package com.niting.zebraauthenticator.ui.main;

import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.niting.zebraauthenticator.Authenticator;
import com.niting.zebraauthenticator.R;

import static com.niting.zebraauthenticator.Utility.EXTRA_BOOT;

public class SecondFragment extends Fragment {

    private static final String TAG = "ZebraAuthenticator";
    private MainViewModel mViewModel;
    private TextView data;

    public static SecondFragment newInstance() {
        return new SecondFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.v(TAG, "SecondFrag, onCreateView");
        View root = inflater.inflate(R.layout.main_fragment, container, false);
        data = root.findViewById(R.id.data);
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        // TODO: Use the ViewModel
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "SecondFrag, onResume");
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            boolean extra = intent.getBooleanExtra(EXTRA_BOOT, false);
            updateData("Received intent with EXTRA_BOOT : " + extra);
            Log.v(TAG, "Received intent with EXTRA_BOOT : " + extra);
            getActivity().startService(
                    new Intent(getActivity(), Authenticator.class).putExtra(EXTRA_BOOT,true));
            getActivity().finish();
        }
    }

    private void updateData(String msg) {
        data.setText(msg);
    }
}