package com.harryven.appai;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class HomePageActivity extends SingleFragmentActivity {

    protected Fragment createFragment() {
        return new HomePageFragment();
    }

}
