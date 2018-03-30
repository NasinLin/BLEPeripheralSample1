package com.example.ada.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

import java.util.HashSet;


public class MainActivity extends FragmentActivity{

    BluetoothAdapter mBluetoothAdapter;

    private static String TAG = MainActivity.class.getName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothAdapter = ((BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (mBluetoothAdapter != null){
            if (mBluetoothAdapter.isEnabled()){
                if (mBluetoothAdapter.isMultipleAdvertisementSupported()){
                    setupFragments();
                }else {
                    showErrorText(R.string.bt_ads_not_supported);
                }
            }else {
                Intent enableBtIntent = new Intent();
                startActivityForResult(enableBtIntent,Constants.REQUEST_ENABLE_BT);
            }
        }else {
            showErrorText(R.string.bt_not_supported);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case Constants.REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK){
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()){
                        setupFragments();
                    }else {
                        showErrorText(R.string.bt_ads_not_supported);
                    }
                }else {
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
        }
    }
    private static final String CURRENT_FRAGMENT_TAG = "CURRENT_FRAGMENT";
    private void setupFragments() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        AdvertiserFragment advertiserFragment =
                new AdvertiserFragment();

        transaction.replace(R.id.advertiser_fragment_container,advertiserFragment);
        transaction.commit();}

    private void showErrorText(int messageId) {
        Log.v(TAG,getString(messageId));
    }


}
