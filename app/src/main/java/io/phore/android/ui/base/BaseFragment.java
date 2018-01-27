package io.phore.android.ui.base;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

import io.phore.android.PhoreApplication;
import io.phore.android.module.PhoreModule;

/**
 * Created by furszy on 6/29/17.
 */

public class BaseFragment extends Fragment {

    protected PhoreApplication phoreApplication;
    protected PhoreModule phoreModule;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        phoreApplication = PhoreApplication.getInstance();
        phoreModule = phoreApplication.getModule();
    }

    protected boolean checkPermission(String permission) {
        int result = ContextCompat.checkSelfPermission(getActivity(),permission);
        return result == PackageManager.PERMISSION_GRANTED;
    }
}
