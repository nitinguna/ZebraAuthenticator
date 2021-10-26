package com.niting.zebraauthenticator;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.niting.zebraauthenticator.ui.main.SecondFragment;

public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.second_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, SecondFragment.newInstance())
                    .commitNow();
        }
    }
}