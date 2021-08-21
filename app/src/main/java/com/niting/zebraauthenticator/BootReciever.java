package com.niting.zebraauthenticator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static com.niting.zebraauthenticator.Utility.EXTRA_BOOT;
import static com.niting.zebraauthenticator.Utility.EXTRA_SOURCE;

public class BootReciever extends BroadcastReceiver {

    private static final String TAG = "ZebraAuthenticator";
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() != null)
        {
            if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
            {
                Log.v(TAG, "Boot Reciever" );
                context.startService(new Intent(context, Authenticator.class).putExtra(EXTRA_BOOT,true));
            }
        }
    }
}