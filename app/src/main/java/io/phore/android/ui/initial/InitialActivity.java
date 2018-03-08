package io.phore.android.ui.initial;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import io.phore.android.PhoreApplication;
import io.phore.android.ui.start_activity.StartActivity;
import io.phore.android.ui.wallet_activity.WalletActivity;
import io.phore.android.utils.AppConf;

/**
 * Created by furszy on 8/19/17.
 */

public class InitialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PhoreApplication phoreApplication = PhoreApplication.getInstance();
        AppConf appConf = phoreApplication.getAppConf();
        // show report dialog if something happen with the previous process
        Intent intent;
        if (!appConf.isAppInit()){
            intent = new Intent(this, StartActivity.class);
        }else {
            intent = new Intent(this, WalletActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
