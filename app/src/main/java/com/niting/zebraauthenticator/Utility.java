package com.niting.zebraauthenticator;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.AuthorizationServiceDiscovery;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.ClientSecretBasic;
import net.openid.appauth.EndSessionRequest;
import net.openid.appauth.RegistrationRequest;
import net.openid.appauth.RegistrationResponse;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;
import net.openid.appauth.browser.AnyBrowserMatcher;
import net.openid.appauth.browser.BrowserMatcher;

import org.joda.time.format.DateTimeFormat;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static android.content.Context.WINDOW_SERVICE;
import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE;


public class Utility {
    private static final String TAG = "ZebraAuthenticator";
    public static final String EXTRA_FAILED = "failed";
    public static final String EXTRA_COMPLETE = "complete";
    public static final String EXTRA_FAILED_SIGNOUT = "failed_signout";
    public static final String EXTRA_COMPLETE_SIGNOUT = "complete_signout";
    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_LOGOUT = "logout";
    public static final String EXTRA_LOGOUT_FULL = "logoutfull";
    public static final String EXTRA_BOOT = "boot";
    Context mContext;
    private AuthorizationService mAuthService;
    private AuthStateManager mAuthStateManager;
    private Configuration mConfiguration;
    @NonNull
    private BrowserMatcher mBrowserMatcher = AnyBrowserMatcher.INSTANCE;
    public static final String ACTION_LOGOUT_USER= "niting.intent.action.LogoutUser";
    public static final String ACTION_LOGOUT_FULL_USER= "niting.intent.action.LogoutUserfull";


    private final AtomicReference<String> mClientId = new AtomicReference<>();
    private final AtomicReference<AuthorizationRequest> mAuthRequest = new AtomicReference<>();
    private final AtomicReference<CustomTabsIntent> mAuthIntent = new AtomicReference<>();

    private final AtomicReference<EndSessionRequest> mSignOutRequest = new AtomicReference<>();
    private final AtomicReference<CustomTabsIntent> mSignOutIntent = new AtomicReference<>();

    private CountDownLatch mAuthIntentLatch = new CountDownLatch(1);
    private boolean mUsePendingIntents = true;
    private static final int RC_AUTH = 100;
    private Boolean isShowing = false;

    public Utility(Context context){
        mContext = context;
        mAuthStateManager = AuthStateManager.getInstance(context);
        mConfiguration = Configuration.getInstance(context);
    }

    public void showWindow(){
        if (!isShowing){
            showWindowInternal();
        }else
        {
            Log.v(TAG, "Cancelling request as Window is already on Top" );
        }
    }
    private void showWindowInternal(){
        Log.v(TAG, "showWindow" );
        isShowing = true;
        final WindowManager wm = (WindowManager)mContext.getSystemService(WINDOW_SERVICE);

        final DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        View mChatHeadView = LayoutInflater.from(mContext).inflate(R.layout.activity_main2, null);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,

                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // TYPE_SYSTEM_ALERT is denied in apiLevel >=19
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.OPAQUE
        );
        mChatHeadView.setFitsSystemWindows(false); // allow us to draw over status bar, navigation bar
        mChatHeadView.setSystemUiVisibility(SYSTEM_UI_FLAG_FULLSCREEN | SYSTEM_UI_FLAG_HIDE_NAVIGATION |SYSTEM_UI_FLAG_IMMERSIVE) ;
        //params.width = params.height = Math.max(metrics.widthPixels, metrics.heightPixels);

        //        params.gravity = Gravity.RIGHT | Gravity.TOP;

        params.x = 0;
        params.y = 0;
        params.setTitle("locktask");
        wm.addView(mChatHeadView, params);


        //Set the close button.
        Button btnReject = (Button) mChatHeadView.findViewById(R.id.button);
        btnReject.setText("Authenticate");
        btnReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //close the service and remove the chat head from the window
                isShowing = false;
                wm.removeView(mChatHeadView);
                doAuth();

                // finishActivity(0);
                // finish();
            }
        });
    }

    public void fullSignOut(){
        Log.i(TAG, "User fullSignOut");
        Intent completionIntent = new Intent(mContext, Authenticator.class);
        completionIntent.putExtra(EXTRA_COMPLETE_SIGNOUT, true);
        Intent cancelIntent = new Intent(mContext, Authenticator.class);
        cancelIntent.putExtra(EXTRA_FAILED_SIGNOUT, true);
        cancelIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        AuthState currentState = mAuthStateManager.getCurrent();
        AuthorizationServiceConfiguration config =
                currentState.getAuthorizationServiceConfiguration();
        if (config.endSessionEndpoint != null) {
            mSignOutRequest.set(
                    new EndSessionRequest.Builder(config)
                            .setIdTokenHint(currentState.getIdToken())
                            .setPostLogoutRedirectUri(mConfiguration.getEndSessionRedirectUri())

                            .build());

            CustomTabsIntent.Builder intentBuilder =
                    mAuthService.createCustomTabsIntentBuilder(mSignOutRequest.get().toUri());
            intentBuilder.setToolbarColor(getColorCompat(R.color.colorPrimary));
            mSignOutIntent.set(intentBuilder.build());

            mAuthService.performEndSessionRequest(
                    mSignOutRequest.get(),
                    PendingIntent.getService(mContext,0,completionIntent,0),
                    PendingIntent.getService(mContext, 0, cancelIntent, 0),
                    //PendingIntent.getActivity(this, 0, completionIntent, 0),
                    //PendingIntent.getActivity(this, 0, cancelIntent, 0),
                    mSignOutIntent.get());

        }


    }

    public void signOut() {
        // discard the authorization and token state, but retain the configuration and
        // dynamic client registration (if applicable), to save from retrieving them again.
        Log.i(TAG, "User Signout");
        AuthState currentState = mAuthStateManager.getCurrent();
        AuthState clearedState =
                new AuthState(currentState.getAuthorizationServiceConfiguration());
        if (currentState.getLastRegistrationResponse() != null) {
            clearedState.update(currentState.getLastRegistrationResponse());
        }
        mAuthStateManager.replace(clearedState);

    }

    private void doAuth() {
        try {
            mAuthIntentLatch.await();
        } catch (InterruptedException ex) {
            Log.w(TAG, "Interrupted while waiting for auth intent");
        }

        if (mUsePendingIntents) {
            Intent completionIntent = new Intent(mContext, Authenticator.class);
            completionIntent.putExtra(EXTRA_COMPLETE, true);
            Intent cancelIntent = new Intent(mContext, Authenticator.class);
            cancelIntent.putExtra(EXTRA_FAILED, true);
            cancelIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            mAuthService.performAuthorizationRequest(
                    mAuthRequest.get(),
                    PendingIntent.getService(mContext,0,completionIntent,0),
                    PendingIntent.getService(mContext, 0, cancelIntent, 0),
                    //PendingIntent.getActivity(this, 0, completionIntent, 0),
                    //PendingIntent.getActivity(this, 0, cancelIntent, 0),
                    mAuthIntent.get());
        } else {
            Intent intent = mAuthService.getAuthorizationRequestIntent(
                    mAuthRequest.get(),
                    mAuthIntent.get());
           // startActivityForResult(intent, RC_AUTH);
        }
    }

    public boolean isAuthenticated(){

        if (mAuthStateManager.getCurrent().isAuthorized()
                && !mConfiguration.hasConfigurationChanged()) {
            Log.i(TAG, "User is already authenticated, proceeding to token activity");
            return true;
        }
        Log.i(TAG, "User Not authenticated");
        return false;
    }

    public void initAuth(){
        if (!isAuthenticated()) {
            if (mConfiguration.hasConfigurationChanged()) {
                // discard any existing authorization state due to the change of configuration
                Log.i(TAG, "Configuration change detected, discarding old state");
                mAuthStateManager.replace(new AuthState());
                mConfiguration.acceptConfiguration();
            }
            initializeAppAuth();
        }
    }

    private void initializeAppAuth() {
        Log.i(TAG, "Initializing AppAuth");
        recreateAuthorizationService();

        if (mAuthStateManager.getCurrent().getAuthorizationServiceConfiguration() != null) {
            // configuration is already created, skip to client initialization
            Log.i(TAG, "auth config already established");
            initializeClient();
            return;
        }

        // if we are not using discovery, build the authorization service configuration directly
        // from the static configuration values.
        if (mConfiguration.getDiscoveryUri() == null) {
            Log.i(TAG, "Creating auth config from res/raw/auth_config.json");
            AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(
                    mConfiguration.getAuthEndpointUri(),
                    mConfiguration.getTokenEndpointUri(),
                    mConfiguration.getRegistrationEndpointUri(),
                    mConfiguration.getEndSessionEndpoint());

            mAuthStateManager.replace(new AuthState(config));
            initializeClient();
            return;
        }

        // WrongThread inference is incorrect for lambdas
        // noinspection WrongThread
        //runOnUiThread(() -> displayLoading("Retrieving discovery document"));
        Log.i(TAG, "Retrieving OpenID discovery doc");
        AuthorizationServiceConfiguration.fetchFromUrl(
                mConfiguration.getDiscoveryUri(),
                this::handleConfigurationRetrievalResult,
                mConfiguration.getConnectionBuilder());
    }

    private void handleConfigurationRetrievalResult(
            AuthorizationServiceConfiguration config,
            AuthorizationException ex) {
        if (config == null) {
            Log.i(TAG, "Failed to retrieve discovery document", ex);
            //displayError("Failed to retrieve discovery document: " + ex.getMessage(), true);
            return;
        }

        Log.i(TAG, "Discovery document retrieved");
        mAuthStateManager.replace(new AuthState(config));
        initializeClient();
    }

    private void recreateAuthorizationService() {
        if (mAuthService != null) {
            Log.i(TAG, "Discarding existing AuthService instance");
            mAuthService.dispose();
        }
        mAuthService = createAuthorizationService();
        mAuthRequest.set(null);
        mAuthIntent.set(null);
    }
    private AuthorizationService createAuthorizationService() {
        Log.i(TAG, "Creating authorization service");
        AppAuthConfiguration.Builder builder = new AppAuthConfiguration.Builder();
        builder.setBrowserMatcher(mBrowserMatcher);
        builder.setConnectionBuilder(mConfiguration.getConnectionBuilder());

        return new AuthorizationService(mContext, builder.build());
    }

    private void initializeClient() {
        if (mConfiguration.getClientId() != null) {
            Log.i(TAG, "Using static client ID: " + mConfiguration.getClientId());
            // use a statically configured client ID
            mClientId.set(mConfiguration.getClientId());
            initializeAuthRequest();
            return;
        }

        RegistrationResponse lastResponse =
                mAuthStateManager.getCurrent().getLastRegistrationResponse();
        if (lastResponse != null) {
            Log.i(TAG, "Using dynamic client ID: " + lastResponse.clientId);
            // already dynamically registered a client ID
            mClientId.set(lastResponse.clientId);
            initializeAuthRequest();
            return;
        }

        // WrongThread inference is incorrect for lambdas
        // noinspection WrongThread
        //runOnUiThread(() -> displayLoading("Dynamically registering client"));
        Log.i(TAG, "Dynamically registering client");

        RegistrationRequest registrationRequest = new RegistrationRequest.Builder(
                mAuthStateManager.getCurrent().getAuthorizationServiceConfiguration(),
                Collections.singletonList(mConfiguration.getRedirectUri()))
                .setTokenEndpointAuthenticationMethod(ClientSecretBasic.NAME)
                .build();

        mAuthService.performRegistrationRequest(
                registrationRequest,
                this::handleRegistrationResponse);
    }

    private void handleRegistrationResponse(
            RegistrationResponse response,
            AuthorizationException ex) {
        mAuthStateManager.updateAfterRegistration(response, ex);
        if (response == null) {
            Log.i(TAG, "Failed to dynamically register client", ex);
            //displayErrorLater("Failed to register client: " + ex.getMessage(), true);
            return;
        }

        Log.i(TAG, "Dynamically registered client: " + response.clientId);
        mClientId.set(response.clientId);
        initializeAuthRequest();
    }

    private void initializeAuthRequest() {
        createAuthRequest(getLoginHint());
        warmUpBrowser();
        displayAuthOptions();
    }

    private void displayAuthOptions() {


        AuthState state = mAuthStateManager.getCurrent();
        AuthorizationServiceConfiguration config = state.getAuthorizationServiceConfiguration();

        String authEndpointStr;
        if (config.discoveryDoc != null) {
            authEndpointStr = "Discovered auth endpoint: \n";
        } else {
            authEndpointStr = "Static auth endpoint: \n";
        }
        authEndpointStr += config.authorizationEndpoint;
        Log.i(TAG, "authEndpointStr: " + authEndpointStr);
        //((TextView)findViewById(R.id.auth_endpoint)).setText(authEndpointStr);

        String clientIdStr;
        if (state.getLastRegistrationResponse() != null) {
            clientIdStr = "Dynamic client ID: \n";
        } else {
            clientIdStr = "Static client ID: \n";
        }
        clientIdStr += mClientId;
        Log.i(TAG, "clientIdStr: " + clientIdStr);
        //((TextView)findViewById(R.id.client_id)).setText(clientIdStr);
    }

    private void createAuthRequest(@Nullable String loginHint) {
        Log.i(TAG, "Creating auth request for login hint: " + loginHint);
        AuthorizationRequest.Builder authRequestBuilder = new AuthorizationRequest.Builder(
                mAuthStateManager.getCurrent().getAuthorizationServiceConfiguration(),
                mClientId.get(),
                ResponseTypeValues.CODE,
                mConfiguration.getRedirectUri())
                .setScope(mConfiguration.getScope());

        if (!TextUtils.isEmpty(loginHint)) {
            authRequestBuilder.setLoginHint(loginHint);
        }

        mAuthRequest.set(authRequestBuilder.build());
    }

    private String getLoginHint() {
        return null;
    }

    private void warmUpBrowser() {
        mAuthIntentLatch = new CountDownLatch(1);

            Log.i(TAG, "Warming up browser instance for auth request");
            CustomTabsIntent.Builder intentBuilder =
                    mAuthService.createCustomTabsIntentBuilder(mAuthRequest.get().toUri());
            intentBuilder.setToolbarColor(getColorCompat(R.color.colorPrimary));
            mAuthIntent.set(intentBuilder.build());
            mAuthIntentLatch.countDown();

    }

    @TargetApi(Build.VERSION_CODES.M)
    @SuppressWarnings("deprecation")
    private int getColorCompat(@ColorRes int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return mContext.getColor(color);
        } else {
            return mContext.getResources().getColor(color);
        }
    }

    public void updateStateAfterAuth(@Nullable AuthorizationResponse response,
                                     @Nullable AuthorizationException ex)
    {
        mAuthStateManager.updateAfterAuthorization(response, ex);
    }

    public void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
       // displayLoading("Exchanging authorization code");
        Log.i(TAG, "Exchanging authorization code");
        performTokenRequest(
                authorizationResponse.createTokenExchangeRequest(),
                this::handleCodeExchangeResponse);
    }


    private void performTokenRequest(
            TokenRequest request,
            AuthorizationService.TokenResponseCallback callback) {
        ClientAuthentication clientAuthentication;
        try {
            clientAuthentication = mAuthStateManager.getCurrent().getClientAuthentication();
        } catch (ClientAuthentication.UnsupportedAuthenticationMethod ex) {
            Log.d(TAG, "Token request cannot be made, client authentication for the token "
                    + "endpoint could not be constructed (%s)", ex);
            displayNotAuthorized("Client authentication method is unsupported");
            return;
        }

        mAuthService.performTokenRequest(
                request,
                clientAuthentication,
                callback);
    }

    private void handleAccessTokenResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {
        mAuthStateManager.updateAfterTokenResponse(tokenResponse, authException);
        displayAuthorized();
    }

    private void handleCodeExchangeResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {

        mAuthStateManager.updateAfterTokenResponse(tokenResponse, authException);
        if (!mAuthStateManager.getCurrent().isAuthorized()) {
            final String message = "Authorization Code exchange failed"
                    + ((authException != null) ? authException.error : "");

            // WrongThread inference is incorrect for lambdas
            //noinspection WrongThread
            displayNotAuthorized(message);
        } else {
            displayAuthorized();
        }
    }

    public static void displayAuthCancelled() {
        Log.v(TAG, "Auth Cancelled" );
    }

    public static void displayNotAuthorized(String explanation) {
        Log.v(TAG, explanation );
    }

    private void displayAuthorized() {


        AuthState state = mAuthStateManager.getCurrent();


        if(state.getRefreshToken() == null)
        {
            Log.v(TAG, "No Refresh Token Returned" );
        }
        else{
            Log.v(TAG, "Refresh Token Returned" );
        }

        if(state.getIdToken() == null)
        {
            Log.v(TAG, "No ID Token Returned" );
        }
        else{
            Log.v(TAG, "ID Token Returned" );
        }

        if (state.getAccessToken() == null) {
            Log.v(TAG, "No Access Token Returned" );

        } else {
            Long expiresAt = state.getAccessTokenExpirationTime();
            if (expiresAt == null) {
                Log.v(TAG, "No Access Token Expiry" );

            } else if (expiresAt < System.currentTimeMillis()) {
                Log.v(TAG, "Access Token Expired" );
            } else {
                Log.v(TAG, "Refresh Token Expires at " + DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss ZZ").print(expiresAt));

            }
        }

        AuthorizationServiceDiscovery discoveryDoc =
                state.getAuthorizationServiceConfiguration().discoveryDoc;
        if ((discoveryDoc == null || discoveryDoc.getUserinfoEndpoint() == null)
                && mConfiguration.getUserInfoEndpointUri() == null) {
            Log.v(TAG, "No User info" );
        } else {
            Log.v(TAG, "User info Present" );
        }


    }

}
