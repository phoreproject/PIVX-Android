package io.phore.android.ui.transaction_send_activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import io.phore.android.R;
import io.phore.android.ui.base.BaseActivity;
import io.phore.android.ui.pincode_activity.PincodeActivity;

/**
 * Created by furszy on 8/7/17.
 * //
 */

public class SendTxDetailActivity extends BaseActivity {

    private static final int PIN_RESULT = 121;

    private View root;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        super.onCreateView(savedInstanceState, container);
        setTitle("Transaction Information");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        root = getLayoutInflater().inflate(R.layout.send_tx_detail_main,container);
        root.findViewById(R.id.txt_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start pin
                Intent intent = new Intent(SendTxDetailActivity.this, PincodeActivity.class);
                intent.putExtra(PincodeActivity.CHECK_PIN,true);
                startActivityForResult(intent,PIN_RESULT);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PIN_RESULT){
            if (resultCode == RESULT_OK){
                setResult(RESULT_OK,null);
                finish();
            }
        }
    }
}
