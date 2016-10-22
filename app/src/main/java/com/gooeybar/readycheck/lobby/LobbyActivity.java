package com.gooeybar.readycheck.lobby;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.gooeybar.readycheck.R;
import com.gooeybar.readycheck.base.BaseActivity;

public class LobbyActivity extends BaseActivity implements
        View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
    }

    private void signOut() {
        Log.d("LOGIN", "Sign out clicked!!!");


        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_out_button:
                signOut();
                break;
        }
    }
}
