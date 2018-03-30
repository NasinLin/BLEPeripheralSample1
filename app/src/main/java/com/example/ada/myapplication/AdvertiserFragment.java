package com.example.ada.myapplication;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by ada on 2018/3/29.
 */

public class AdvertiserFragment extends ServiceFragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener,ServiceFragment.ServiceFragmentDelegate {

    BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    BluetoothGattServer mGattServer;

    HashSet<BluetoothDevice> mBluetoothDevices;

    BluetoothGattService mBluetoothService;

    BluetoothGattCharacteristic mBluetoothCharacteristic;

    BluetoothManager mBluetoothManager;

    BluetoothAdapter mBluetoothAdapter;

    ServiceFragmentDelegate mDelegate;

    SeekBar seekBar;

    Button SendData;

    private static final int INITIAL_BATTERY_LEVEL = 50;

    private static final String BATTERY_LEVEL_DESCRIPTION = "The current charge level of a " +
            "battery. 100% represents fully charged while 0% represents fully discharged.";

    private Switch mSwitch;



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_advertiser, container, false);
        mSwitch = (Switch)view.findViewById(R.id.advertise_switch);
        seekBar = (SeekBar)view.findViewById(R.id.seekBar);
        SendData = (Button) view.findViewById(R.id.senddata);
        mSwitch.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);
        setBatteryLevel(INITIAL_BATTERY_LEVEL,null);
        SendData.setOnClickListener(mNotifyButtonListener);
        return view;
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mDelegate = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }

    @Override
    public void onClick(View view) {
        boolean on = ((Switch)view).isChecked();
        if (on){
            Log.e("Onclick","enable");
            startAdvertising();
        }else {
            stopAdvertising();
            Log.e("Onclick","disable");
        }
    }


    private final View.OnClickListener mNotifyButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
                sendNotificationToDevices(mBluetoothCharacteristic);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (running) {
            mSwitch.setChecked(true);
            running = true;
        } else {
            running = false;
            mSwitch.setChecked(false);
        }
    }

    private static final String TAG = AdvertiserFragment.class.getSimpleName();

    public static boolean running = false;

    @Override
    public void onDestroy(){
        running = false;
        stopAdvertising();
        super.onDestroy();
    }

    public void setUp(){
        mBluetoothCharacteristic =
                new BluetoothGattCharacteristic(Constants.BATTERY_LEVEL_UUID,
                        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ);
        mBluetoothCharacteristic.addDescriptor(getClientCharacteristicConfigurationDescriptor());
        mBluetoothCharacteristic.addDescriptor(getCharacteristicUserDescriptionDescriptor(BATTERY_LEVEL_DESCRIPTION));

        mBluetoothService = new BluetoothGattService(Constants.BATTERY_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mBluetoothService.addCharacteristic(mBluetoothCharacteristic);
    }

    private void initialize() {
        mBluetoothDevices = new HashSet<>();
        mBluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        setUp();
    }

    @Override
    public void onStart() {
        super.onStart();
        mGattServer = mBluetoothManager.openGattServer(getActivity(), mGattServerCallback);
        mGattServer.addService(getBluetoothGattService());
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
    }

    private void startAdvertising() {
        Log.d(TAG, "Service: Starting Advertising");
        AdvertiseSettings settings = buildAdvertiseSettings();
        AdvertiseData data = buildAdvertiseData();
        AdvertiseData dataresponse = buildAdvertiserDataResponse();
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.startAdvertising(settings, data, dataresponse,  advercallback);
        }
    }

    private void stopAdvertising() {
        Log.d(TAG, "Service: Stopping Advertising");
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(advercallback);
        }
    }

    private AdvertiseData buildAdvertiseData() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(getServiceUUID());
        dataBuilder.setIncludeTxPowerLevel(true);
        return dataBuilder.build();
    }

    private AdvertiseData buildAdvertiserDataResponse(){
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeDeviceName(true);
        return dataBuilder.build();
    }

    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
        settingsBuilder.setConnectable(true);
        return settingsBuilder.build();
    }


    private final AdvertiseCallback advercallback = new AdvertiseCallback(){
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);

        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            int statusText;
            running = false;
            switch (errorCode) {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    statusText = R.string.status_advertising;
                    Log.w(TAG, "App was already advertising");
                    break;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    statusText = R.string.status_advDataTooLarge;
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    statusText = R.string.status_advFeatureUnsupported;
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    statusText = R.string.status_advInternalError;
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    statusText = R.string.status_advTooManyAdvertisers;
                    break;
                default:
                    statusText = R.string.status_notAdvertising;
                    Log.wtf(TAG, "Unhandled error: " + errorCode);
            }

            Log.v(TAG,getString(statusText));
        }
    };

    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS){
                if (newState == BluetoothGatt.STATE_CONNECTED){
                    mBluetoothDevices.add(device);
                    Log.v(TAG, "Connected to device: " + device.getAddress());
                }else if (newState == BluetoothGatt.STATE_DISCONNECTED){
                    mBluetoothDevices.remove(device);
                    Log.v(TAG, "Disconnected from device");
                }
            }else {
                mBluetoothDevices.remove(device);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
            if (offset != 0) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
            /* value (optional) */ null);
                return;
            }

            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.v(TAG, "Characteristic Write request: " + Arrays.toString(value));
            int status = writeCharacteristic(characteristic, offset, value);
            if (responseNeeded){
                mGattServer.sendResponse(device, requestId, status, 0, null);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            Log.d(TAG, "Device tried to read descriptor: " + descriptor.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(descriptor.getValue()));
            if (offset != 0) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
            /* value (optional) */ null);
                return;
            }
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                    descriptor.getValue());
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            Log.v(TAG, "Descriptor Write Request " + descriptor.getUuid() + " " + Arrays.toString(value));
            int status = BluetoothGatt.GATT_SUCCESS;
            if (descriptor.getUuid() == Constants.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID) {
                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                boolean supportsNotifications = (characteristic.getProperties() &
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
                boolean supportsIndications = (characteristic.getProperties() &
                        BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;

                if (!(supportsNotifications || supportsIndications)) {
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
                } else if (value.length != 2) {
                    status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
                } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    notificationsDisabled(characteristic);
                    descriptor.setValue(value);
                } else if (supportsNotifications &&
                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    notificationsEnabled(characteristic, false /* indicate */);
                    descriptor.setValue(value);
                } else if (supportsIndications &&
                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    notificationsEnabled(characteristic, true /* indicate */);
                    descriptor.setValue(value);
                } else {
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
                }
            } else {
                status = BluetoothGatt.GATT_SUCCESS;
                descriptor.setValue(value);
            }
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, status,
            /* No need to respond with offset */ 0,
            /* No need to respond with a value */ null);
            }
        }
    };

    @Override
    public BluetoothGattService getBluetoothGattService() {
        return mBluetoothService;
    }

    @Override
    public ParcelUuid getServiceUUID() {
        return new ParcelUuid(Constants.BATTERY_SERVICE_UUID);
    }



    ///////////////////////
    ////// Bluetooth //////
    ///////////////////////
    public static BluetoothGattDescriptor getClientCharacteristicConfigurationDescriptor() {
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                Constants.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID,
                (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
        descriptor.setValue(new byte[]{0, 0});
        return descriptor;
    }

    public static BluetoothGattDescriptor getCharacteristicUserDescriptionDescriptor(String defaultValue) {
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                Constants.CHARACTERISTIC_USER_DESCRIPTION_UUID,
                (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
        try {
            descriptor.setValue(defaultValue.getBytes("UTF-8"));
        } finally {
            return descriptor;
        }
    }

    @Override
    public void notificationsEnabled(BluetoothGattCharacteristic characteristic, boolean indicate) {
        if (characteristic.getUuid() != Constants.BATTERY_LEVEL_UUID) {
            return;
        }
        if (indicate) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), R.string.notificationsEnabled, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void notificationsDisabled(BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid() != Constants.BATTERY_LEVEL_UUID) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), R.string.notificationsNotEnabled, Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    @Override
    public void sendNotificationToDevices(BluetoothGattCharacteristic characteristic) {
        boolean indicate = (characteristic.getProperties()
                & BluetoothGattCharacteristic.PROPERTY_INDICATE)
                == BluetoothGattCharacteristic.PROPERTY_INDICATE;
        for (BluetoothDevice device : mBluetoothDevices) {
            mGattServer.notifyCharacteristicChanged(device, characteristic, indicate);
        }
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (b){
            setBatteryLevel(i,seekBar);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void setBatteryLevel(int i, SeekBar seekBar) {
        mBluetoothCharacteristic.setValue(i,BluetoothGattCharacteristic.FORMAT_UINT8,0);

        if (seekBar != seekBar){
            seekBar.setProgress(i);
        }
    }
}
