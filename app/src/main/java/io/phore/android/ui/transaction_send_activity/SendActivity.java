package io.phore.android.ui.transaction_send_activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.phorej.core.Address;
import org.phorej.core.Coin;
import org.phorej.core.InsufficientMoneyException;
import org.phorej.core.NetworkParameters;
import org.phorej.core.Transaction;
import org.phorej.core.TransactionInput;
import org.phorej.core.TransactionOutput;
import org.phorej.uri.PhoreURI;
import org.phorej.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.phore.android.R;
import io.phore.android.contacts.AddressLabel;
import io.phore.android.module.NoPeerConnectedException;
import io.phore.android.rate.db.PhoreRate;
import io.phore.android.service.PhoreWalletService;
import io.phore.android.ui.base.BaseActivity;
import io.phore.android.ui.base.dialogs.SimpleTextDialog;
import io.phore.android.ui.base.dialogs.SimpleTwoButtonsDialog;
import io.phore.android.ui.transaction_send_activity.custom.ChangeAddressActivity;
import io.phore.android.ui.transaction_send_activity.custom.CustomFeeActivity;
import io.phore.android.ui.transaction_send_activity.custom.CustomFeeFragment;
import io.phore.android.ui.transaction_send_activity.custom.inputs.InputWrapper;
import io.phore.android.ui.transaction_send_activity.custom.inputs.InputsActivity;
import io.phore.android.ui.transaction_send_activity.custom.outputs.OutputWrapper;
import io.phore.android.ui.transaction_send_activity.custom.outputs.OutputsActivity;
import io.phore.android.ui.wallet_activity.TransactionWrapper;
import io.phore.android.utils.CrashReporter;
import io.phore.android.utils.DialogsUtil;
import io.phore.android.utils.NavigationUtils;
import io.phore.android.utils.scanner.ScanActivity;
import wallet.exceptions.InsufficientInputsException;
import wallet.exceptions.TxNotFoundException;

import static android.Manifest.permission_group.CAMERA;
import static io.phore.android.service.IntentsConstants.ACTION_BROADCAST_TRANSACTION;
import static io.phore.android.service.IntentsConstants.DATA_TRANSACTION_HASH;
import static io.phore.android.ui.transaction_detail_activity.FragmentTxDetail.TX;
import static io.phore.android.ui.transaction_detail_activity.FragmentTxDetail.TX_MEMO;
import static io.phore.android.ui.transaction_detail_activity.FragmentTxDetail.TX_WRAPPER;
import static io.phore.android.ui.transaction_send_activity.custom.ChangeAddressActivity.INTENT_EXTRA_CHANGE_ADDRESS;
import static io.phore.android.ui.transaction_send_activity.custom.ChangeAddressActivity.INTENT_EXTRA_CHANGE_SEND_ORIGIN;
import static io.phore.android.ui.transaction_send_activity.custom.CustomFeeFragment.INTENT_EXTRA_CLEAR;
import static io.phore.android.ui.transaction_send_activity.custom.CustomFeeFragment.INTENT_EXTRA_FEE;
import static io.phore.android.ui.transaction_send_activity.custom.CustomFeeFragment.INTENT_EXTRA_IS_FEE_PER_KB;
import static io.phore.android.ui.transaction_send_activity.custom.CustomFeeFragment.INTENT_EXTRA_IS_MINIMUM_FEE;
import static io.phore.android.ui.transaction_send_activity.custom.CustomFeeFragment.INTENT_EXTRA_IS_TOTAL_FEE;
import static io.phore.android.ui.transaction_send_activity.custom.inputs.InputsFragment.INTENT_EXTRA_UNSPENT_WRAPPERS;
import static io.phore.android.ui.transaction_send_activity.custom.outputs.OutputsActivity.INTENT_EXTRA_OUTPUTS_CLEAR;
import static io.phore.android.ui.transaction_send_activity.custom.outputs.OutputsActivity.INTENT_EXTRA_OUTPUTS_WRAPPERS;
import static io.phore.android.utils.scanner.ScanActivity.INTENT_EXTRA_RESULT;

/**
 * Created by Neoperol on 5/4/17.
 */

public class SendActivity extends BaseActivity implements View.OnClickListener {

    public static final String INTENT_EXTRA_TOTAL_AMOUNT = "total_amount";
    private Logger logger = LoggerFactory.getLogger(SendActivity.class);

    private static final int PIN_RESULT = 121;
    private static final int SCANNER_RESULT = 122;
    private static final int CUSTOM_FEE_RESULT = 123;
    private static final int MULTIPLE_ADDRESSES_SEND_RESULT = 124;
    private static final int CUSTOM_INPUTS = 125;
    private static final int SEND_DETAIL = 126;
    private static final int CUSTOM_CHANGE_ADDRESS = 127;

    private View root;
    private Button buttonSend, addAll;
    private AutoCompleteTextView edit_address;
    private TextView txt_local_currency , txt_coin_selection, txt_custom_fee, txt_change_address, txtShow;
    private TextView txt_multiple_outputs, txt_currency_amount;
    private View container_address;
    private EditText edit_amount, editCurrency;
    private EditText edit_memo;
    private MyFilterableAdapter filterableAdapter;
    private String addressStr;
    private PhoreRate phoreRate;
    private SimpleTextDialog errorDialog;
    private ImageButton btnSwap;
    private ViewFlipper amountSwap;

    private boolean inPhrs = true;
    private Transaction transaction;
    /** Several outputs */
    private List<OutputWrapper> outputWrappers;
    /** Custom inputs */
    private Set<InputWrapper> unspent;
    /** Custom fee selector */
    private CustomFeeFragment.FeeSelector customFee;
    /** Clean wallet flag */
    private boolean cleanWallet;
    /** Is multi send */
    private boolean isMultiSend;
    /** Change address */
    private boolean changeToOrigin;
    private Address changeAddress;


    @Override
    protected void onCreateView(Bundle savedInstanceState,ViewGroup container) {
        root = getLayoutInflater().inflate(R.layout.fragment_transaction_send, container);
        setTitle(R.string.btn_send);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        edit_address = (AutoCompleteTextView) findViewById(R.id.edit_address);
        edit_amount = (EditText) findViewById(R.id.edit_amount);
        edit_memo = (EditText) findViewById(R.id.edit_memo);
        container_address = root.findViewById(R.id.container_address);
        txt_local_currency = (TextView) findViewById(R.id.txt_local_currency);
        txt_multiple_outputs = (TextView) root.findViewById(R.id.txt_multiple_outputs);
        txt_multiple_outputs.setOnClickListener(this);
        txt_coin_selection = (TextView) root.findViewById(R.id.txt_coin_selection);
        txt_coin_selection.setOnClickListener(this);
        txt_custom_fee = (TextView) root.findViewById(R.id.txt_custom_fee);
        txt_custom_fee.setOnClickListener(this);
        txt_change_address = (TextView) root.findViewById(R.id.txt_change_address);
        txt_change_address.setOnClickListener(this);
        findViewById(R.id.button_qr).setOnClickListener(this);
        buttonSend = (Button) findViewById(R.id.btnSend);
        buttonSend.setOnClickListener(this);

        //Swap type of ammounts
        amountSwap = (ViewFlipper) findViewById( R.id.viewFlipper );
        amountSwap.setInAnimation(AnimationUtils.loadAnimation(this,
                android.R.anim.slide_in_left));
        amountSwap.setOutAnimation(AnimationUtils.loadAnimation(this,
                android.R.anim.slide_out_right));
        btnSwap = (ImageButton) findViewById(R.id.btn_swap);
        btnSwap.setOnClickListener(this);

        //Sending amount currency
        editCurrency = (EditText) findViewById(R.id.edit_amount_currency);
        txt_currency_amount = (TextView) root.findViewById(R.id.txt_currency_amount);
        txtShow = (TextView) findViewById(R.id.txt_show) ;

        //Sending amount phr
        addAll =  (Button) findViewById(R.id.btn_add_all);
        addAll.setOnClickListener(this);
        phoreRate = phoreModule.getRate(phoreApplication.getAppConf().getSelectedRateCoin());

        editCurrency.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (phoreRate != null) {
                    if (s.length() > 0) {
                        String valueStr = s.toString();
                        if (valueStr.charAt(0) == '.') {
                            valueStr = "0" + valueStr;
                        }
                        BigDecimal result = new BigDecimal(valueStr).divide(phoreRate.getRate(), 6, BigDecimal.ROUND_DOWN);
                        txtShow.setText(result.toPlainString() + getString(R.string.wallet_phr));
                    } else {
                        txtShow.setText("0 " + phoreRate.getCode());
                    }
                }else {
                    txtShow.setText(R.string.no_rate);
                }
                cleanWallet = false;
            }
        });

        edit_amount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length()>0) {
                    if (phoreRate != null) {
                        String valueStr = s.toString();
                        if (valueStr.charAt(0) == '.') {
                            valueStr = "0" + valueStr;
                        }
                        Coin coin = Coin.parseCoin(valueStr);
                        txt_local_currency.setText(
                                phoreApplication.getCentralFormats().format(
                                        new BigDecimal(coin.getValue() * phoreRate.getRate().doubleValue()).movePointLeft(8)
                                )
                                        + " " + phoreRate.getCode()
                        );
                    }else {
                        // rate null -> no connection.
                        txt_local_currency.setText(R.string.no_rate);
                    }
                }else {
                    if (phoreRate!=null)
                        txt_local_currency.setText("0 "+phoreRate.getCode());
                    else
                        txt_local_currency.setText(R.string.no_rate);
                }
                cleanWallet = false;

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
		dismissErrorDialog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.option_fee){
            startCustomFeeActivity(customFee);
            return true;
        }else if(id == R.id.option_multiple_addresses){
            startMultiAddressSendActivity(outputWrappers);
            return true;
        }else if(id == R.id.option_select_inputs){
            startCoinControlActivity(unspent);
        }else if (id == R.id.option_change_address){
            startChangeAddressActivity(changeAddress,changeToOrigin);
        }
        return super.onOptionsItemSelected(item);
    }

    private void startChangeAddressActivity(Address changeAddress,boolean changeToOrigin) {
        Intent intent = new Intent(this, ChangeAddressActivity.class);
        if (changeAddress!=null){
            intent.putExtra(INTENT_EXTRA_CHANGE_ADDRESS,changeAddress.toBase58());
        }
        intent.putExtra(INTENT_EXTRA_CHANGE_SEND_ORIGIN,changeToOrigin);
        startActivityForResult(intent,CUSTOM_CHANGE_ADDRESS);
    }

    private void startCustomFeeActivity(CustomFeeFragment.FeeSelector customFee) {
        Intent intent = new Intent(this, CustomFeeActivity.class);
        if (customFee != null) {
            intent.putExtra(INTENT_EXTRA_IS_FEE_PER_KB, customFee.isFeePerKbSelected());
            intent.putExtra(INTENT_EXTRA_IS_TOTAL_FEE, !customFee.isFeePerKbSelected());
            intent.putExtra(INTENT_EXTRA_IS_MINIMUM_FEE, customFee.isPayMinimum());
            intent.putExtra(INTENT_EXTRA_FEE, customFee.getAmount());
        }
        startActivityForResult(intent,CUSTOM_FEE_RESULT);
    }

    private void startMultiAddressSendActivity(List<OutputWrapper> outputWrappers) {
        Intent intent = new Intent(this, OutputsActivity.class);
        Bundle bundle = new Bundle();
        if (outputWrappers!=null)
            bundle.putSerializable(INTENT_EXTRA_OUTPUTS_WRAPPERS, (Serializable) outputWrappers);
        intent.putExtras(bundle);
        startActivityForResult(intent,MULTIPLE_ADDRESSES_SEND_RESULT);
    }

    private void startCoinControlActivity(Set<InputWrapper> unspent) {
        String amountStr = getAmountStr();
        if (amountStr.length()>0){
            Intent intent = new Intent(this, InputsActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(INTENT_EXTRA_TOTAL_AMOUNT,amountStr);
            if (unspent!=null)
                bundle.putSerializable(INTENT_EXTRA_UNSPENT_WRAPPERS, (Serializable) unspent);
            intent.putExtras(bundle);
            startActivityForResult(intent,CUSTOM_INPUTS);
        }else {
            Toast.makeText(this,R.string.send_amount_input_error,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (transaction!=null)
            outState.putSerializable(TX,transaction.unsafeBitcoinSerialize());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // todo: test this roting the screen..
        if (savedInstanceState.containsKey(TX)){
            transaction = new Transaction(phoreModule.getConf().getNetworkParams(),savedInstanceState.getByteArray(TX));
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        NavigationUtils.goBackToHome(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // todo: This is not updating the filter..
        if (filterableAdapter==null) {
            List<AddressLabel> list = new ArrayList<>(phoreModule.getContacts());
            filterableAdapter = new MyFilterableAdapter(this,list );
            edit_address.setAdapter(filterableAdapter);
        }

        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnSend){
            try {
                if (checkConnectivity()){
                    send(false);
                }
            }catch (IllegalArgumentException e){
                e.printStackTrace();
                showErrorDialog(e.getMessage());
            }catch (Exception e){
                e.printStackTrace();
                showErrorDialog(e.getMessage());
            }
        }else if (id == R.id.button_qr){
            if (!checkPermission(CAMERA)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    int permsRequestCode = 200;
                    String[] perms = {"android.permission.CAMERA"};
                    requestPermissions(perms, permsRequestCode);
                }
            }
            startActivityForResult(new Intent(this, ScanActivity.class),SCANNER_RESULT);
        }else if(id == R.id.btn_add_all){
            if (!isMultiSend) {
                cleanWallet = true;
                Coin coin = phoreModule.getAvailableBalanceCoin();
                if (inPhrs) {
                    edit_amount.setText(coin.toPlainString());
                    txt_local_currency.setText(
                    phoreApplication.getCentralFormats().format(
                            new BigDecimal(coin.getValue() * phoreRate.getRate().doubleValue()).movePointLeft(8)
                    )
                            + " " + phoreRate.getCode()
                    );
                } else {
                    editCurrency.setText(
                            phoreApplication.getCentralFormats().format(
                                    new BigDecimal(coin.getValue() * phoreRate.getRate().doubleValue()).movePointLeft(8)
                            )
                    );
                    txtShow.setText(coin.toFriendlyString());
                }
            }else {
                Toast.makeText(this,R.string.validate_multi_send_enabled,Toast.LENGTH_SHORT).show();
            }
        }else if(id == R.id.btn_swap){
            if (!isMultiSend){
                inPhrs = !inPhrs;
                amountSwap.showNext();
            }else {
                Toast.makeText(this,R.string.validate_multi_send_enabled,Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.txt_coin_selection){
            startCoinControlActivity(unspent);
        }else if(id == R.id.txt_multiple_outputs){
            startMultiAddressSendActivity(outputWrappers);
        }else if(id == R.id.txt_custom_fee){
            startCustomFeeActivity(customFee);
        }else if (id == R.id.txt_change_address){
            startChangeAddressActivity(changeAddress,changeToOrigin);
        }
    }

    private boolean checkConnectivity() {
        if (!isOnline()){
            SimpleTwoButtonsDialog noConnectivityDialog = DialogsUtil.buildSimpleTwoBtnsDialog(
                    this,
                    getString(R.string.error_no_connectivity_title),
                    getString(R.string.error_no_connectivity_body),
                    new SimpleTwoButtonsDialog.SimpleTwoBtnsDialogListener() {
                        @Override
                        public void onRightBtnClicked(SimpleTwoButtonsDialog dialog) {
                            try {
                                send(true);
                            }catch (Exception e){
                                e.printStackTrace();
                                showErrorDialog(e.getMessage());
                            }
                            dialog.dismiss();

                        }

                        @Override
                        public void onLeftBtnClicked(SimpleTwoButtonsDialog dialog) {
                            dialog.dismiss();
                        }
                    }
            );
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                noConnectivityDialog.setRightBtnTextColor(getColor(R.color.lightGreen));
            }else {
                noConnectivityDialog.setRightBtnTextColor(ContextCompat.getColor(this, R.color.lightGreen));
            }
            noConnectivityDialog.setLeftBtnTextColor(Color.WHITE)
                    .setRightBtnTextColor(Color.BLACK)
                    .setRightBtnBackgroundColor(Color.WHITE)
                    .setLeftBtnTextColor(Color.BLACK)
                    .setLeftBtnText(getString(R.string.button_cancel))
                    .setRightBtnText(getString(R.string.button_ok))
                    .show();

            return false;
        }
        return true;
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SCANNER_RESULT){
            if (resultCode==RESULT_OK) {
                String address = "";
                try {
                    address = data.getStringExtra(INTENT_EXTRA_RESULT);
                    String usedAddress;
                    if (phoreModule.chechAddress(address)){
                        usedAddress = address;
                    }else {
                        PhoreURI phoreUri = new PhoreURI(address);
                        usedAddress = phoreUri.getAddress().toBase58();
                    }
                    final String tempPubKey = usedAddress;
                    edit_address.setText(tempPubKey);
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(this,"Bad address "+address,Toast.LENGTH_LONG).show();
                }
            }
        }else if(requestCode == SEND_DETAIL){
            if (resultCode==RESULT_OK) {
                try {
                    // pin ok, send the tx now
                    sendConfirmed();
                }catch (Exception e){
                    e.printStackTrace();
                    CrashReporter.saveBackgroundTrace(e,phoreApplication.getPackageInfo());
                    showErrorDialog(R.string.commit_tx_fail);
                }
            }
        }else if(requestCode == MULTIPLE_ADDRESSES_SEND_RESULT){
            if (resultCode == RESULT_OK){
                if (data.hasExtra(INTENT_EXTRA_OUTPUTS_CLEAR)){
                    outputWrappers = null;
                    txt_multiple_outputs.setVisibility(View.GONE);
                    container_address.setVisibility(View.VISIBLE);
                    unBlockAmount();
                    isMultiSend = false;
                }else {
                    outputWrappers = (List<OutputWrapper>) data.getSerializableExtra(INTENT_EXTRA_OUTPUTS_WRAPPERS);
                    Coin totalAmount = Coin.ZERO;
                    for (OutputWrapper outputWrapper : outputWrappers) {
                        totalAmount = outputWrapper.getAmount().plus(totalAmount);
                    }
                    setAmountAndBlock(totalAmount);
                    txt_multiple_outputs.setText(getString(R.string.multiple_address_send, outputWrappers.size()));
                    txt_multiple_outputs.setVisibility(View.VISIBLE);
                    container_address.setVisibility(View.GONE);
                    isMultiSend = true;
                }
            }
        }else if (requestCode == CUSTOM_INPUTS){
            if (resultCode == RESULT_OK) {
                try {
                    Set<InputWrapper> unspents = (Set<InputWrapper>) data.getSerializableExtra(INTENT_EXTRA_UNSPENT_WRAPPERS);
                    for (InputWrapper inputWrapper : unspents) {
                        inputWrapper.setUnspent(phoreModule.getUnspent(inputWrapper.getParentTxHash(), inputWrapper.getIndex()));
                    }
                    unspent = unspents;
                    txt_coin_selection.setVisibility(View.VISIBLE);
                } catch (TxNotFoundException e) {
                    e.printStackTrace();
                    CrashReporter.saveBackgroundTrace(e,phoreApplication.getPackageInfo());
                    Toast.makeText(this,R.string.load_inputs_fail,Toast.LENGTH_LONG).show();
                } catch (Exception e){
                    CrashReporter.saveBackgroundTrace(e,phoreApplication.getPackageInfo());
                    Toast.makeText(this,R.string.load_inputs_fail,Toast.LENGTH_LONG).show();
                }
            }
        }else if (requestCode == CUSTOM_FEE_RESULT){
            if (resultCode == RESULT_OK){
                if (data.hasExtra(INTENT_EXTRA_CLEAR)){
                    customFee = null;
                    txt_custom_fee.setVisibility(View.GONE);
                }else {
                    boolean isPerKb = data.getBooleanExtra(INTENT_EXTRA_IS_FEE_PER_KB, false);
                    boolean isTotal = data.getBooleanExtra(INTENT_EXTRA_IS_TOTAL_FEE, false);
                    boolean isMinimum = data.getBooleanExtra(INTENT_EXTRA_IS_MINIMUM_FEE, false);
                    Coin feeAmount = (Coin) data.getSerializableExtra(INTENT_EXTRA_FEE);
                    customFee = new CustomFeeFragment.FeeSelector(isPerKb, feeAmount, isMinimum);
                    txt_custom_fee.setVisibility(View.VISIBLE);
                }
            }
        }else if(requestCode == CUSTOM_CHANGE_ADDRESS){
            if (resultCode == RESULT_OK){
                if (data.hasExtra(ChangeAddressActivity.INTENT_EXTRA_CLEAR_CHANGE_ADDRESS)){
                    changeAddress = null;
                    changeToOrigin = false;
                    txt_change_address.setVisibility(View.GONE);
                }else {
                    if (data.hasExtra(INTENT_EXTRA_CHANGE_SEND_ORIGIN)){
                        changeAddress = null;
                        changeToOrigin = true;
                    }else {
                        if (data.hasExtra(INTENT_EXTRA_CHANGE_ADDRESS)) {
                            String address = data.getStringExtra(INTENT_EXTRA_CHANGE_ADDRESS);
                            changeAddress = Address.fromBase58(phoreModule.getConf().getNetworkParams(),address);
                        }
                    }
                    txt_change_address.setVisibility(View.VISIBLE);
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void dismissErrorDialog() {
		if (errorDialog != null) {
			errorDialog.dismiss();
		}
	}

    private void showErrorDialog(int resStr){
        showErrorDialog(getString(resStr));
    }

    private void showErrorDialog(String message) {
        if (errorDialog==null){
            errorDialog = DialogsUtil.buildSimpleErrorTextDialog(this,getResources().getString(R.string.invalid_inputs),message);
        }else {
            errorDialog.setBody(message);
        }
        errorDialog.show(getFragmentManager(),getResources().getString(R.string.send_error_dialog_tag));
    }

    private String getAmountStr(){
        String amountStr = "0";
        if (inPhrs) {
            amountStr = edit_amount.getText().toString();
        }else {
            String valueStr = editCurrency.getText().toString();
            if(valueStr.length()>0) {
                if (valueStr.charAt(0) == '.') {
                    valueStr = "0" + valueStr;
                }
                BigDecimal result = new BigDecimal(valueStr).multiply(phoreRate.getRate());
                amountStr = result.setScale(6, RoundingMode.FLOOR).toPlainString();
            }
        }
        return amountStr;
    }

    public void setAmountAndBlock(Coin amount) {
        if (inPhrs) {
            edit_amount.setText(amount.toPlainString());
            edit_amount.setEnabled(false);
        }else {
            BigDecimal result = new BigDecimal(amount.toPlainString()).multiply(phoreRate.getRate()).setScale(6,RoundingMode.FLOOR);
            editCurrency.setText(result.toPlainString());
            edit_amount.setEnabled(false);
        }
    }

    public void unBlockAmount(){
        if (inPhrs) {
            edit_amount.setEnabled(true);
        }else {
            edit_amount.setEnabled(true);
        }
    }

    private void send(boolean sendOffline) {
        try {

            // check if the wallet is still syncing
            try {
                if(!phoreModule.isSyncWithNode()){
                    throw new IllegalArgumentException(getString(R.string.wallet_is_not_sync));
                }
            } catch (NoPeerConnectedException e) {
                if (!sendOffline) {
                    e.printStackTrace();
                    throw new IllegalArgumentException(getString(R.string.no_peer_connection));
                }
            }

            // first check amount
            String amountStr = getAmountStr();
            if (amountStr.length() < 1) throw new IllegalArgumentException("Amount not valid");
            if (amountStr.length()==1 && amountStr.equals(".")) throw new IllegalArgumentException("Amount not valid");
            if (amountStr.charAt(0)=='.'){
                amountStr = "0"+amountStr;
            }

            Coin amount = Coin.parseCoin(amountStr);
            if (amount.isZero()) throw new IllegalArgumentException("Amount zero, please correct it");
            if (amount.isLessThan(Transaction.MIN_NONDUST_OUTPUT)) throw new IllegalArgumentException("Amount must be greater than the minimum amount accepted from miners, "+Transaction.MIN_NONDUST_OUTPUT.toFriendlyString());
            if (amount.isGreaterThan(Coin.valueOf(phoreModule.getAvailableBalance())))
                throw new IllegalArgumentException("Insufficient balance");

            // memo
            String memo = edit_memo.getText().toString();

            NetworkParameters params = phoreModule.getConf().getNetworkParams();

            if ( (outputWrappers==null || outputWrappers.isEmpty()) && (unspent==null || unspent.isEmpty()) ){
                addressStr = edit_address.getText().toString();
                if (!phoreModule.chechAddress(addressStr))
                    throw new IllegalArgumentException("Address not valid");
                Coin feePerKb = getFee();
                Address changeAddressTemp = null;
                if (changeAddress!=null){
                    changeAddressTemp = changeAddress;
                }else {
                    changeAddressTemp = phoreModule.getReceiveAddress();
                }
                transaction = phoreModule.buildSendTx(addressStr,amount,feePerKb,memo,changeAddressTemp);

                // check if there is a need to change the change address
                if (changeToOrigin){
                    transaction = changeChangeAddressToOriginAddress(transaction,changeAddressTemp);
                    transaction = phoreModule.completeTx(transaction);
                }
            }else {
                transaction = new Transaction(params);
                // then outputs
                if (outputWrappers != null && !outputWrappers.isEmpty()) {
                    for (OutputWrapper outputWrapper : outputWrappers) {
                        transaction.addOutput(
                                outputWrapper.getAmount(),
                                Address.fromBase58(params, outputWrapper.getAddress())
                        );
                    }
                } else {
                    addressStr = edit_address.getText().toString();
                    if (!phoreModule.chechAddress(addressStr))
                        throw new IllegalArgumentException("Address not valid");
                    transaction.addOutput(amount, Address.fromBase58(phoreModule.getConf().getNetworkParams(), addressStr));
                }

                // then check custom inputs if there is any
                if (unspent != null && !unspent.isEmpty()) {
                    for (InputWrapper inputWrapper : unspent) {
                        transaction.addInput(inputWrapper.getUnspent());
                    }
                }
                // satisfy output with inputs if it's neccesary
                Coin ouputsSum = transaction.getOutputSum();
                Coin inputsSum = transaction.getInputSum();

                if (ouputsSum.isGreaterThan(inputsSum)) {
                    List<TransactionOutput> unspent = phoreModule.getRandomUnspentNotInListToFullCoins(transaction.getInputs(), ouputsSum);
                    for (TransactionOutput transactionOutput : unspent) {
                        transaction.addInput(transactionOutput);
                    }
                    // update the input amount
                    inputsSum = transaction.getInputSum();
                }

                // then fee and change address
                Coin feePerKb = getFee();

                if (memo.length()>0)
                    transaction.setMemo(memo);

                Address changeAddressTemp = null;
                if (changeAddress==null){
                    changeAddressTemp = changeAddress;
                }else {
                    changeAddressTemp = phoreModule.getReceiveAddress();
                }

                transaction = phoreModule.completeTx(transaction,changeAddressTemp,feePerKb);

                // check if there is a need to change the change address
                // check if there is a need to change the change address
                if (changeToOrigin){
                    transaction = changeChangeAddressToOriginAddress(transaction,changeAddressTemp);
                    transaction = phoreModule.completeTx(transaction);
                }
            }

            Log.i("APP","tx: "+transaction.toString());

            TransactionWrapper transactionWrapper = new TransactionWrapper(transaction,null,null,amount, TransactionWrapper.TransactionUse.SENT_SINGLE);

            // Confirmation screen
            Intent intent = new Intent(this,SendTxDetailActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(TX_WRAPPER,transactionWrapper);
            bundle.putSerializable(TX,transaction.bitcoinSerialize());
            if (memo.length()>0)
                bundle.putString(TX_MEMO,memo);
            intent.putExtras(bundle);
            startActivityForResult(intent,SEND_DETAIL);

        } catch (InsufficientMoneyException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Insufficient balance\nMissing coins "+e.missing.toFriendlyString());
        } catch (InsufficientInputsException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Insufficient balance\nMissing coins "+e.getMissing().toFriendlyString());
        } catch (Wallet.DustySendRequested e){
            e.printStackTrace();
            throw new IllegalArgumentException("Dusty send output, please increase the value of your outputs");
        }
    }

    private Transaction changeChangeAddressToOriginAddress(Transaction transaction, Address currentChangeAddress) {
        NetworkParameters params = transaction.getParams();
        // origin address is the highest from the inputs.
        TransactionInput origin = null;
        for (TransactionInput input : transaction.getInputs()) {
            if (origin==null)
                origin = input;
            else {
                if (origin.getValue().isLessThan(input.getValue())){
                    origin = input;
                }
            }
        }
        Address originAddress = origin.getConnectedOutput().getScriptPubKey().getToAddress(params,true);
        // check if the address is mine just in case
        if (!phoreModule.isAddressUsed(originAddress)) throw new IllegalStateException("origin address is not on the wallet: "+originAddress);

        // Now i just have to re organize the outputs.
        TransactionOutput changeOutput = null;
        List<TransactionOutput> outputs = new ArrayList<>();
        for (TransactionOutput transactionOutput : transaction.getOutputs()) {
            if(transactionOutput.getScriptPubKey().getToAddress(params,true).equals(currentChangeAddress)){
                changeOutput = transactionOutput;
            }else {
                outputs.add(transactionOutput);
            }
        }
        transaction.clearOutputs();
        for (TransactionOutput output : outputs) {
            transaction.addOutput(output);
        }
        // now the new change address with the same value
        transaction.addOutput(changeOutput.getValue(),originAddress);
        return transaction;
    }

    public Coin getFee() {
        Coin feePerKb;
        // tx size calculation -> (148*inputs)+(34*outputs)+10
        //long txSize = 148 * transaction.getInputs().size() + 34 * transaction.getOutputs().size() + 10;

        if (customFee!=null){
            if (customFee.isPayMinimum()){
                feePerKb = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;
            }else {
                if (customFee.isFeePerKbSelected()){
                    // fee per kB
                    feePerKb = customFee.getAmount();
                }else {
                    // todo: total fee..
                    feePerKb = customFee.getAmount();
                }
            }
        }else {
            feePerKb = Transaction.DEFAULT_TX_FEE;
        }
        return feePerKb;
    }

    private void sendConfirmed(){
        if(transaction==null){
            logger.error("## trying to send a NULL transaction");
            try {
                CrashReporter.appendSavedBackgroundTraces(new StringBuilder().append("ERROR ### sendActivity - sendConfirmed - transaction NULL"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            showErrorDialog(R.string.commit_tx_fail);
            return;
        }
        phoreModule.commitTx(transaction);
        Intent intent = new Intent(SendActivity.this, PhoreWalletService.class);
        intent.setAction(ACTION_BROADCAST_TRANSACTION);
        intent.putExtra(DATA_TRANSACTION_HASH,transaction.getHash().getBytes());
        startService(intent);
        Toast.makeText(SendActivity.this,R.string.sending_tx,Toast.LENGTH_LONG).show();
        finish();
        NavigationUtils.goBackToHome(this);
    }
}
