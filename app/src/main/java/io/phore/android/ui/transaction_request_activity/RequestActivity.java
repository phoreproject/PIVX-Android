package io.phore.android.ui.transaction_request_activity;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.WriterException;

import org.pivxj.core.Coin;
import org.pivxj.core.NetworkParameters;
import org.pivxj.core.Transaction;
import org.pivxj.uri.PivxURI;

import java.util.ArrayList;
import java.util.List;

import io.phore.android.R;
import io.phore.android.contacts.AddressLabel;
import io.phore.android.ui.base.BaseActivity;
import io.phore.android.ui.base.dialogs.SimpleTextDialog;
import io.phore.android.ui.transaction_send_activity.AmountInputFragment;
import io.phore.android.ui.transaction_send_activity.MyFilterableAdapter;
import io.phore.android.utils.DialogsUtil;
import io.phore.android.utils.NavigationUtils;

import static android.graphics.Color.WHITE;
import static io.phore.android.ui.qr_activity.MyAddressFragment.convertDpToPx;
import static io.phore.android.utils.QrUtils.encodeAsBitmap;

/**
 * Created by Neoperol on 5/11/17.
 */

public class RequestActivity extends BaseActivity implements View.OnClickListener {

    private AmountInputFragment amountFragment;
    private EditText edit_memo;
    private AutoCompleteTextView edit_address;
    private String addressStr;
    private MyFilterableAdapter filterableAdapter;
    private SimpleTextDialog errorDialog;

    private QrDialog qrDialog;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        View root = getLayoutInflater().inflate(R.layout.fragment_transaction_request, container);
        setTitle(R.string.btn_request);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        edit_memo = (EditText) root.findViewById(R.id.edit_memo);
        edit_address = (AutoCompleteTextView) root.findViewById(R.id.edit_address);
        amountFragment = (AmountInputFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_amount);
        root.findViewById(R.id.btnRequest).setOnClickListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
        // todo: This is not updating the filter..
        if (filterableAdapter == null) {
            List<AddressLabel> list = new ArrayList<>(pivxModule.getContacts());
            filterableAdapter = new MyFilterableAdapter(this, list);
            edit_address.setAdapter(filterableAdapter);
        }

        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        NavigationUtils.goBackToHome(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnRequest) {
            try {
                showRequestQr();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                showErrorDialog(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                showErrorDialog(e.getMessage());
            }
        }
    }

    private void showRequestQr() throws Exception {
        // first check amount
        String amountStr = amountFragment.getAmountStr();
        if (amountStr.length() < 1) throw new IllegalArgumentException("Amount not valid");
        if (amountStr.length() == 1 && amountStr.equals("."))
            throw new IllegalArgumentException("Amount not valid");
        if (amountStr.charAt(0) == '.') {
            amountStr = "0" + amountStr;
        }

        Coin amount = Coin.parseCoin(amountStr);
        if (amount.isZero()) throw new IllegalArgumentException("Amount zero, please correct it");
        if (amount.isLessThan(Transaction.MIN_NONDUST_OUTPUT))
            throw new IllegalArgumentException("Amount must be greater than the minimum amount accepted from miners, " + Transaction.MIN_NONDUST_OUTPUT.toFriendlyString());

        // memo
        String memo = edit_memo.getText().toString();

        addressStr = edit_address.getText().toString();
        if (!pivxModule.chechAddress(addressStr))
            throw new IllegalArgumentException("Address not valid");

        NetworkParameters params = pivxModule.getConf().getNetworkParams();

        String pivxURI = PivxURI.convertToBitcoinURI(
                params,
                addressStr,
                amount,
                getString(R.string.btn_request),
                memo
        );

        if (qrDialog != null){
            qrDialog = null;
        }
        qrDialog = QrDialog.newInstance(pivxURI);
        qrDialog.setQrText(pivxURI);
        qrDialog.show(getFragmentManager(),"qr_dialog");

    }

    private void showErrorDialog(int resStr) {
        showErrorDialog(getString(resStr));
    }

    private void showErrorDialog(String message) {
        if (errorDialog == null) {
            errorDialog = DialogsUtil.buildSimpleErrorTextDialog(this, getResources().getString(R.string.invalid_inputs), message);
        } else {
            errorDialog.setBody(message);
        }
        errorDialog.show(getFragmentManager(), getResources().getString(R.string.send_error_dialog_tag));
    }

    public static class QrDialog extends DialogFragment {

        private View root;
        private ImageView img_qr;
        private String qrText;

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            try {
                root = inflater.inflate(R.layout.qr_dialog, container);
                img_qr = (ImageView) root.findViewById(R.id.img_qr);
                root.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });
                updateQr();
            }catch (Exception e){
                Toast.makeText(getActivity(),R.string.error_generic,Toast.LENGTH_SHORT).show();
                dismiss();
                getActivity().onBackPressed();
            }
            return root;
        }

        private void updateQr() throws WriterException {
            if (img_qr != null) {
                Resources r = getResources();
                int px = convertDpToPx(r, 225);
                Log.i("Util", qrText);
                Bitmap qrBitmap = encodeAsBitmap(qrText, px, px, Color.parseColor("#1A1A1A"), WHITE);
                img_qr.setImageBitmap(qrBitmap);
            }
        }


        public void setQrText(String qrText) throws WriterException {
            this.qrText = qrText;
            updateQr();
        }

        public static QrDialog newInstance(String pivxURI) throws WriterException {
            QrDialog qrDialog = new QrDialog();
            qrDialog.setQrText(pivxURI);
            return qrDialog;
        }
    }

}
