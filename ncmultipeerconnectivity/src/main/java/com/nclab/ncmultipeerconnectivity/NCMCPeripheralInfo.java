package com.nclab.ncmultipeerconnectivity;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Used by central to save connected peripheral information
 */
class NCMCPeripheralInfo {
    public BluetoothGatt bluetoothGatt;
    public BluetoothGattCharacteristic readCharacteristic; // message from peripheral to central
    public BluetoothGattCharacteristic writeWithResponseCharacteristic; // message from central to peripheral
    public BluetoothGattCharacteristic writeWithoutResponseCharacteristic;
    public String name;
    public int mtu;
}
