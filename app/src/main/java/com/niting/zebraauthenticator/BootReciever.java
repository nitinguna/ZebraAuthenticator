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
            Log.v(TAG, "Boot Reciever, Action: " + intent.getAction());
            if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                    intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)
                    || intent.getAction().equals(Intent.ACTION_USER_PRESENT))
            {
                Log.v(TAG, "Boot Reciever : " + Intent.ACTION_BOOT_COMPLETED);
                startSecondActivity(context);
                //context.startService(new Intent(context, Authenticator.class).putExtra(EXTRA_BOOT,true));
            }
        }
    }

    private void startSecondActivity(Context context) {
        Intent activityIntent = new Intent(context, SecondActivity.class);
        activityIntent.putExtra(EXTRA_BOOT,true);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activityIntent);
    }
}