package com.niting.zebraauthenticator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static com.niting.zebraauthenticator.Utility.ACTION_LOGOUT_FULL_USER;
import static com.niting.zebraauthenticator.Utility.ACTION_LOGOUT_USER;
import static com.niting.zebraauthenticator.Utility.EXTRA_LOGOUT;
import static com.niting.zebraauthenticator.Utility.EXTRA_LOGOUT_FULL;
import static com.niting.zebraauthenticator.Utility.EXTRA_SOURCE;


public class ScreenReceiver extends BroadcastReceiver {
    private static final String TAG = "ZebraAuthenticator";
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() != null)
        {
            if(intent.getAction().equals(Intent.ACTION_USER_PRESENT))
            {
                Log.v(TAG, "User Present" );
                context.startService(new Intent(context, Authenticator.class).putExtra(EXTRA_SOURCE,true));

            }else if(intent.getAction().equals(ACTION_LOGOUT_USER))
            {
                Log.v(TAG, "ACTION_LOGOUT_USER" );
                context.startService(new Intent(context, Authenticator.class).putExtra(EXTRA_LOGOUT,true));

            }
            else if (intent.getAction().equals(ACTION_LOGOUT_FULL_USER)){
                Log.v(TAG, "ACTION_LOGOUT_FULL_USER" );
                context.startService(new Intent(context, Authenticator.class).putExtra(EXTRA_LOGOUT_FULL,true));
            }
        }
    }
}