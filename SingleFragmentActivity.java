package com.harryven.appai;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

public abstract class SingleFragmentActivity extends FragmentActivity {
    protected abstract Fragment createFragment();
    private static final String TAG = "SingleFragmentActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "On saved instance state");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);

        if (fragment == null) {
            fragment = createFragment();

            fm.beginTransaction()
                    .add(R.id.fragmentContainer, fragment)
                    .commit();

        }
    }

}

