package com.niting.zebraauthenticator;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.google.android.material.snackbar.Snackbar;
import com.symbol.mxmf.IMxFrameworkService;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE;
import static com.niting.zebraauthenticator.Utility.ACTION_LOGOUT_FULL_USER;
import static com.niting.zebraauthenticator.Utility.ACTION_LOGOUT_USER;
import static com.niting.zebraauthenticator.Utility.EXTRA_BOOT;
import static com.niting.zebraauthenticator.Utility.EXTRA_COMPLETE;
import static com.niting.zebraauthenticator.Utility.EXTRA_COMPLETE_SIGNOUT;
import static com.niting.zebraauthenticator.Utility.EXTRA_FAILED;
import static com.niting.zebraauthenticator.Utility.EXTRA_LOGOUT;
import static com.niting.zebraauthenticator.Utility.EXTRA_LOGOUT_FULL;
import static com.niting.zebraauthenticator.Utility.EXTRA_SOURCE;
import static com.niting.zebraauthenticator.Utility.displayAuthCancelled;
import static com.niting.zebraauthenticator.Utility.displayNotAuthorized;


public class Authenticator extends Service {
    private static final String TAG = "ZebraAuthenticator";

    private Boolean mSignoutRequested = false;

    private Utility utils;
    public Authenticator() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate" );
        utils = new Utility(getApplicationContext());

        utils.initAuth();
        ScreenReceiver mScreenReceiver = new ScreenReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        filter.addAction(ACTION_LOGOUT_USER);
        filter.addAction(ACTION_LOGOUT_FULL_USER);
        registerReceiver(mScreenReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.v(TAG, "onStartCommand" );

        //DialogFragment newFragment = new AuthenticatorFragment();
        //newFragment.show(getSupportFragmentManager(), "missiles");

        if (intent!=null && intent.getBooleanExtra(EXTRA_BOOT, false)) {
            //utils.bindMXMFServices("config.xml");
            if (!utils.isAuthenticated()){
                utils.showWindow();
            }
        }
        if (intent!=null && intent.getBooleanExtra(EXTRA_SOURCE, false)) {
            if (!utils.isAuthenticated()){
                utils.showWindow();
            }
        }

        if (intent!=null && intent.getBooleanExtra(EXTRA_LOGOUT, false)) {
            if (utils.isAuthenticated()){
                utils.signOut();
            }
        }

        if (intent!=null && intent.getBooleanExtra(EXTRA_LOGOUT_FULL, false)) {
            if (utils.isAuthenticated()){
                mSignoutRequested = true;
                utils.fullSignOut();
            }
        }

        if (intent!=null && intent.getBooleanExtra(EXTRA_FAILED, false)) {
            displayAuthCancelled();
            if (!utils.isAuthenticated()){
                utils.showWindow();
            }
        }
        if (intent!=null && intent.getBooleanExtra(EXTRA_COMPLETE_SIGNOUT, false)) {
            // for now leave interpreting end session response
            Log.v(TAG, "EXTRA_COMPLETE_SIGNOUT" );
            utils.signOut();
        }


        if (intent!=null && intent.getBooleanExtra(EXTRA_COMPLETE, false)) {
            Log.v(TAG, "EXTRA_COMPLETE" );
            // the stored AuthState is incomplete, so check if we are currently receiving the result of
            // the authorization flow from the browser.
            AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
            AuthorizationException ex = AuthorizationException.fromIntent(intent);

            if (response != null || ex != null) {
                utils.updateStateAfterAuth(response, ex);
            }

            if (response != null && response.authorizationCode != null) {
                // authorization code exchange is required
                utils.updateStateAfterAuth(response, ex);
                utils.exchangeAuthorizationCode(response);
            } else if (ex != null) {
                displayNotAuthorized("Authorization flow failed: " + ex.getMessage());
                if (!utils.isAuthenticated()){
                    utils.showWindow();
                }
            } else {
                displayNotAuthorized("No authorization state retained - reauthorization required");
                if (mSignoutRequested){
                    mSignoutRequested = false;
                    utils.signOut();
                }
                if (!utils.isAuthenticated()){
                    utils.showWindow();
                }
            }
        }
        return START_STICKY;
    }


}