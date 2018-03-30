package com.example.ada.myapplication;

import android.os.ParcelUuid;

import java.util.UUID;

/**
 * Created by ada on 2018/3/29.
 */

public class Constants {


    public static final UUID BATTERY_SERVICE_UUID = UUID
            .fromString("0000180F-0000-1000-8000-00805f9b34fb");

    public static final UUID BATTERY_LEVEL_UUID = UUID
            .fromString("00002A19-0000-1000-8000-00805f9b34fb");

    public static final int REQUEST_ENABLE_BT = 1;

    public static final UUID CHARACTERISTIC_USER_DESCRIPTION_UUID = UUID
            .fromString("00002901-0000-1000-8000-00805f9b34fb");
    public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");

}
