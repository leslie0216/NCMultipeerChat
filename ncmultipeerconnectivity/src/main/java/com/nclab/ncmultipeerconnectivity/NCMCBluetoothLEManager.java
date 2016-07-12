package com.nclab.ncmultipeerconnectivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Manage the ble connection
 */
 class NCMCBluetoothLEManager {
    //region CONSTANTS
    private static final String TAG = "NCMCBluetoothLEManager";
    private static final long SCAN_PERIOD = 20000;
    private static final String TRANSFER_CHARACTERISTIC_MSG_FROM_PERIPHERAL_UUID = "B7020F32-5170-4F62-B078-E5C231B71B3F";
    private static final String TRANSFER_CHARACTERISTIC_MSG_FROM_CENTRAL_WITH_RESPONSE_UUID = "0E182478-7DC7-43D2-9B52-06FE34B325CE";
    private static final String TRANSFER_CHARACTERISTIC_MSG_FROM_CENTRAL_WITHOUT_RESPONSE_UUID = "541300E2-99C2-4319-A5B9-98E96053D2C9";
    private static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    private static final int DEFAULT_MTU = 23;
    private static final int MAX_MTU = 512;
    private static final int SYSTEM_RESERVED_MTU = 3;
    //endregion

    //region COMMON VARS
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private NCMCSession mSession;
    private Context mContext;
    private boolean mIsCentral;
    private boolean mIsBrowsingOrAdvertising;
    //private boolean mIsInit = false;
    private List<NCMCMessageData> mReceivedMsgArray = new ArrayList<>();
    private final List<NCMCMessageData> mMessageSendQueue = new LinkedList<>();
    //endregion

    //region CENTRAL VARS
    private NCMCCentralService mCentralService;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings mScanSettings;
    private List<ScanFilter> mFilters;
    private Handler mScanHandler;
    private ScanCallback mScanCallback;
    private BluetoothGattCallback mGattCallback;
    private Hashtable<String, NCMCPeripheralInfo> mDiscoveredPeripherals = null; // key:device address/identifier
    //endregion

    //region PERIPHERAL VARS
    private NCMCPeripheralService mPeripheralService;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothGattServerCallback mGattServerCallback;
    private BluetoothLeAdvertiser mAdvertiser;
    private AdvertiseCallback mAdvertisingCallback;
    private AdvertiseSettings mAdSettings;
    private AdvertiseData mAdData;
    private Hashtable<String, BluetoothDevice> mConnectedCentrals = null; // key:device address/identifier
    private Hashtable<String, Integer> mCentralMTUs;
    private BluetoothGattCharacteristic mSendCharacteristic; // message from peripheral to central
    private BluetoothGattCharacteristic mReceiveWithResponseCharacteristic; // message from central to peripheral
    private BluetoothGattCharacteristic mReceiveWithoutResponseCharacteristic;
    private List<NCMCPeripheralWriteRequestData> mWriteList = new ArrayList<>();
    //endregion

    private static NCMCBluetoothLEManager ourInstance = new NCMCBluetoothLEManager();

    public static NCMCBluetoothLEManager getInstance() {
        return ourInstance;
    }

    private NCMCBluetoothLEManager() {
    }

    public NCMCSession getSession() {
        return this.mSession;
    }

    public void setSession(NCMCSession session) {
        this.mSession = session;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public boolean isCentral() {
        return mIsCentral;
    }

    public boolean isBrowsingOrAdvertising() {
        return mIsBrowsingOrAdvertising;
    }

    public void clear() {
        // destroy all connections and reset variables
        disconnect();

        if (this.mDiscoveredPeripherals != null) {
            this.mDiscoveredPeripherals.clear();
        }

        if (this.mConnectedCentrals != null) {
            this.mConnectedCentrals.clear();
        }

        if (this.mCentralMTUs != null) {
            this.mCentralMTUs.clear();
        }

        synchronized (this.mMessageSendQueue) {
            this.mMessageSendQueue.clear();
        }

        this.mCentralService = null;
        this.mPeripheralService = null;
        this.mSession = null;

        this.mGattCallback = null;

        if (this.mAdvertiser != null) {
            this.setIsAdvertise(false);
            this.mAdvertiser = null;
        }

        if (this.mAdData != null) {
            this.mAdData  = null;
        }

        if (this.mBluetoothGattServer != null) {
            this.mBluetoothGattServer.clearServices();
            this.mBluetoothGattServer.close();
            this.mBluetoothGattServer = null;
        }

        if (this.mGattServerCallback != null) {
            this.mGattServerCallback = null;
        }

        this.mIsBrowsingOrAdvertising = false;
    }

    public void disconnect() {
        if (isCentral()) {
            stopBrowsing();
            for (NCMCPeripheralInfo info : this.mDiscoveredPeripherals.values()) {
                disconnectToPeripheralByInfo(info);
            }
        } else {
            stopAdvertising();
            if (this.mBluetoothGattServer != null) {
                this.mBluetoothGattServer.clearServices();
            }
        }
    }

    private boolean initBluetoothManager() {
        if (mBluetoothManager == null) {

            if (mContext != null) {
                mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            }
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        return true;
    }

    public boolean setup() {
        boolean rt = false;
        //mIsInit = true;
        if (initBluetoothManager()) {
            this.mBluetoothAdapter = this.mBluetoothManager.getAdapter();
        }

        if (this.mBluetoothAdapter != null && this.mBluetoothAdapter.isEnabled()) {
            this.mBluetoothAdapter.setName(this.mSession.myPeerID.displayName);
            if (Build.VERSION.SDK_INT> Build.VERSION_CODES.LOLLIPOP_MR1) {
                boolean isCoarseLocGranted = this.mContext.checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_GRANTED;
                boolean isFineLocGranted = this.mContext.checkSelfPermission("android.permission.ACCESS_FINE_LOCATION")  == PackageManager.PERMISSION_GRANTED;
                if (!isCoarseLocGranted || !isFineLocGranted) {
                    if (this.mPeripheralService != null) {
                        this.mPeripheralService.notifyDidNotStartAdvertising(NCMCPeripheralService.NCMCPeripheralService_ERROR_NO_ACCESS);
                    }
                    return rt;
                }
            }

            if (!isCentral()) {
                if (!this.mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                    Log.d(TAG, "isMultipleAdvertisementSupported : false");
                    if (this.mPeripheralService != null) {
                        this.mPeripheralService.notifyDidNotStartAdvertising(NCMCPeripheralService.NCMCPeripheralService_ERROR_NOT_SUPPORT);
                    }
                } else {
                    rt = true;
                }
            } else {
                rt = true;
            }

        } else {
            Log.d(TAG, "setupNetwork: bluetooth is not enabled");
            // broadcast message
            if (isCentral()) {
                if (this.mCentralService != null) {
                    this.mCentralService.notifyDidNotStartBrowsingForPeers(NCMCCentralService.NCMCCentralService_ERROR_BLUETOOTH_OFF);
                }
            } else {
                if (this.mPeripheralService != null) {
                    this.mPeripheralService.notifyDidNotStartAdvertising(NCMCPeripheralService.NCMCPeripheralService_ERROR_BLUETOOTH_OFF);
                }
            }
        }

        return rt;
    }

    //region MESSAGE   FUNCTIONS
/***********************************************************************/
/**                          MESSAGE   FUNCTIONS                      **/
/***********************************************************************/
    private List<byte[]> makeMsg(byte[] message, int capacity) {
        Log.d(TAG, "makeMsg: msg length = " + message.length + ", capacity = " + capacity);
        int limitation = capacity - SYSTEM_RESERVED_MTU - 2;

        List<byte[]> msgArray = new ArrayList<>();
        if (message.length < limitation) {
            msgArray.add(packMsg(message, true, true));
        } else {
            int index = 0;
            boolean isCompleted = false;

            while (!isCompleted) {
                int amountToSend = message.length - index;

                if (amountToSend >  limitation) {
                    amountToSend = limitation;
                    isCompleted = false;
                } else {
                    isCompleted = true;
                }

                byte[] chunk = new byte[amountToSend];
                System.arraycopy(message, index, chunk, 0, amountToSend);
                msgArray.add(packMsg(chunk, (index == 0), isCompleted));

                index += amountToSend;
            }
        }

        return msgArray;
    }

    private void processMsg(byte[] message, String deviceUUID) {
        byte newMsg =  message[0];
        byte isCompleted =  message[1];

        byte[] data = new byte[message.length - 2];
        System.arraycopy(message, 2, data, 0, message.length-2);

        if (newMsg != 0) {
            if (isCompleted != 0) {
                if (this.mSession != null) {
                    this.mSession.onDataReceived(data, deviceUUID);
                }
            } else {
                NCMCMessageData msgData = getMessageData(deviceUUID);
                if (msgData == null) {
                    msgData = new NCMCMessageData(deviceUUID, true);
                } else {
                    msgData.clearData();
                }

                msgData.addData(data);

                this.mReceivedMsgArray.add(msgData);
            }
        } else {
            NCMCMessageData msgData = getMessageData(deviceUUID);
            if (msgData != null) {
                msgData.addData(data);
                if (isCompleted != 0) {
                    if (this.mSession != null) {
                        this.mSession.onDataReceived(msgData.getFullData(), deviceUUID);
                    }
                    msgData.clearData();
                    this.mReceivedMsgArray.remove(msgData);
                }
            }
        }
    }

    private NCMCMessageData getMessageData(String deviceUUID) {
        NCMCMessageData data = null;
        for (NCMCMessageData d : this.mReceivedMsgArray) {
            if (d.getDeviceUUID().equalsIgnoreCase(deviceUUID)) {
                data = d;
                break;
            }
        }

        return data;
    }

    private byte[] packMsg(byte[] message, boolean isNew, boolean isCompleted) {
        byte isNewb = (byte) (isNew ? 1 :0);
        byte isCompletedb = (byte) (isCompleted ? 1 :0);

        byte[] result = new byte[message.length + 2];

        result[0] = isNewb;
        result[1] = isCompletedb;
        System.arraycopy(message, 0, result, 2, message.length);

        return result;
    }
    //endregion

    //region CENTRAL   FUNCTIONS
/***********************************************************************/
/**                          CENTRAL   FUNCTIONS                      **/
/***********************************************************************/
    public boolean setupCentralEnv(NCMCCentralService service) {
        this.mCentralService = service;
        this.mIsCentral = true;

        if (!setup()) {
            if (this.mCentralService != null) {
                this.mCentralService.notifyDidNotStartBrowsingForPeers(NCMCCentralService.NCMCCentralService_ERROR_UNKNOWN);
            }
            return false;
        }

        if (this.mDiscoveredPeripherals != null) {
            this.mDiscoveredPeripherals.clear();
        } else {
            mDiscoveredPeripherals = new Hashtable<>();
        }

        this.mSession.setSelfAsCentral();

        mScanHandler = new Handler();

        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mScanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        mFilters = new ArrayList<>();
        initScanCallback();
        initGattCallback();

        return true;
    }

    private void initScanCallback() {
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                //Log.i("callbackType", String.valueOf(callbackType));
                //Log.i("result", result.toString());

                BluetoothDevice device = result.getDevice();

                if (mDiscoveredPeripherals.containsKey(device.getAddress())) {
                    return;
                }

                ScanRecord scanRecord = result.getScanRecord();
                try {
                    List<ParcelUuid> serviceUUIDs = scanRecord != null ? scanRecord.getServiceUuids() : null;
                    if (serviceUUIDs == null) {
                        Log.d(TAG, "onScanResult: no services found!");
                        return;
                    }

                    boolean isFound = false;
                    for (ParcelUuid uuid : serviceUUIDs) {
                        Log.d(TAG, "UUUUID: " + uuid.toString());
                        if (uuid.toString().equalsIgnoreCase(mSession.serviceID)) {
                            isFound = true;
                            break;
                        }
                    }

                    if (isFound && !mDiscoveredPeripherals.containsKey(device.getAddress())) {
                        Log.d(TAG, "onScanResult: new device found : " + device.getName() + " with rssi : " + result.getRssi());
                        NCMCPeripheralInfo info = new NCMCPeripheralInfo();
                        info.bluetoothGatt = null;
                        info.name = device.getName();
                        info.readCharacteristic = null;
                        info.writeWithResponseCharacteristic = null;
                        info.writeWithoutResponseCharacteristic = null;
                        info.mtu = DEFAULT_MTU;
                        info.device = device;
                        mDiscoveredPeripherals.put(device.getAddress(), info);

                        NCMCPeerID peerID = new NCMCPeerID(device.getName(), device.getAddress());
                        if (mCentralService != null) {
                            mCentralService.notifyFoundPeer(peerID);
                        }
                        // stop scan
                        mIsBrowsingOrAdvertising = false;
                        stopBrowsing();
                        //broadcastStatus(BLE_CONNECTION_AUTO_STOP_SCAN_ACTION);

                        // connect to peripheral
                        //broadcastStatus(BLE_CONNECTION_UPDATE_ACTION, "connecting to : " + device.getName());
                        //connectToPeripheral(device, info);
                    }
                } catch (NullPointerException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult sr : results) {
                    Log.i("ScanResult - Results", sr.toString());
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "Scan Failed Error Code: " + errorCode);
                mIsBrowsingOrAdvertising = false;
                mLEScanner.stopScan(mScanCallback);
                if (mCentralService != null) {
                    mCentralService.notifyDidNotStartBrowsingForPeers(NCMCCentralService.NCMCCentralService_ERROR_UNKNOWN);
                }
            }
        };
    }

    private void notifyConnectionError(BluetoothGatt gatt) {
        NCMCPeripheralInfo info = mDiscoveredPeripherals.get(gatt.getDevice().getAddress());
        if (mCentralService != null) {
            NCMCPeerID peerID = new NCMCPeerID(info.name, gatt.getDevice().getAddress());
            mCentralService.notifyLostPeer(peerID);
        }
        mDiscoveredPeripherals.remove(gatt.getDevice().getAddress());
        gatt.disconnect();
        gatt.close();
    }

    private void initGattCallback() {
        mGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to GATT peripheral " + gatt.getDevice().getAddress());
                    // Attempts to discover services after successful connection.
                    Log.d(TAG, "Attempting to start service discovery:" + gatt.discoverServices());

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from GATT peripheral : " + gatt.getDevice().getAddress());
                    NCMCPeripheralInfo info = mDiscoveredPeripherals.get(gatt.getDevice().getAddress());
                    if (info != null) {

                        if (mSession != null) {
                            mSession.onPeripheralDisconnected(gatt.getDevice().getAddress());
                        }

                        mDiscoveredPeripherals.remove(gatt.getDevice().getAddress());
                        gatt.disconnect();
                        gatt.close();
                    }
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.d(TAG, "onServicesDiscovered: status " + status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    boolean isReadCharFound = false;
                    boolean isWriteWithResponseCharFound = false;
                    boolean isWriteWithoutResponseCharFound = false;

                    NCMCPeripheralInfo info = mDiscoveredPeripherals.get(gatt.getDevice().getAddress());
                    List<BluetoothGattService> services = gatt.getServices();
                    for (BluetoothGattService service : services) {
                        List<BluetoothGattCharacteristic> chars =  service.getCharacteristics();
                        for (BluetoothGattCharacteristic ch : chars) {
                            if (ch.getUuid().toString().equalsIgnoreCase(TRANSFER_CHARACTERISTIC_MSG_FROM_PERIPHERAL_UUID)) {
                                Log.d(TAG, "onServicesDiscovered: TRANSFER_CHARACTERISTIC_MSG_FROM_PERIPHERAL_UUID found! ");
                                Log.d(TAG, "onServicesDiscovered: characteristic properties : " + ch.getProperties());

                                if ((ch.getProperties() &
                                        BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
                                    Log.d(TAG, "onServicesDiscovered: cannot read the characteristic" );
                                }

                                isReadCharFound = true;
                                info.readCharacteristic = ch;
                                Log.d(TAG, "onServicesDiscovered: setNotification : " + gatt.setCharacteristicNotification(ch, true));
                                BluetoothGattDescriptor descriptor = ch.getDescriptor(
                                        UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                Log.d(TAG, "onServicesDiscovered: setDescriptor : " + gatt.writeDescriptor(descriptor));
                            } else if (ch.getUuid().toString().equalsIgnoreCase(TRANSFER_CHARACTERISTIC_MSG_FROM_CENTRAL_WITH_RESPONSE_UUID)) {
                                Log.d(TAG, "onServicesDiscovered: TRANSFER_CHARACTERISTIC_MSG_FROM_CENTRAL_WITH_RESPONSE_UUID found!");
                                Log.d(TAG, "onServicesDiscovered: characteristic properties : " + ch.getProperties());
                                isWriteWithResponseCharFound = true;
                                info.writeWithResponseCharacteristic = ch;
                            } else if (ch.getUuid().toString().equalsIgnoreCase(TRANSFER_CHARACTERISTIC_MSG_FROM_CENTRAL_WITHOUT_RESPONSE_UUID)) {
                                Log.d(TAG, "onServicesDiscovered: TRANSFER_CHARACTERISTIC_MSG_FROM_CENTRAL_WITHOUT_RESPONSE_UUID found!");
                                Log.d(TAG, "onServicesDiscovered: characteristic properties : " + ch.getProperties());
                                isWriteWithoutResponseCharFound = true;
                                info.writeWithoutResponseCharacteristic = ch;
                            }
                        }
                    }
                    if (isReadCharFound && isWriteWithResponseCharFound && isWriteWithoutResponseCharFound) {
                        mDiscoveredPeripherals.put(gatt.getDevice().getAddress(), info);
                        Log.d(TAG, "onServicesDiscovered: required services are found start to subscribe notification");
                    } else {
                        notifyConnectionError(gatt);
                    }
                } else {
                    notifyConnectionError(gatt);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                Log.d(TAG, "onCharacteristicRead: " + characteristic.getUuid().toString() + " status : " + status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                Log.d(TAG, "onCharacteristicWrite: " + characteristic.getUuid().toString() + " device = " + gatt.getDevice().getAddress() + " status = " + status);
                try {

                    synchronized (mMessageSendQueue) {
                        mMessageSendQueue.remove(0);
                        if (mMessageSendQueue.size() != 0) {
                            executeSendCentralData();
                        }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                Log.d(TAG, "onCharacteristicChanged: " + characteristic.getUuid().toString() + " , size : " + characteristic.getValue().length);

                if (characteristic.getUuid().toString().equalsIgnoreCase(TRANSFER_CHARACTERISTIC_MSG_FROM_PERIPHERAL_UUID)) {
                    processMsg(characteristic.getValue(), gatt.getDevice().getAddress());
                }
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                super.onReliableWriteCompleted(gatt, status);
                Log.d(TAG, "onReliableWriteCompleted: status : " + status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                Log.d(TAG, "onDescriptorWrite: " + descriptor.getUuid().toString() + " status : " + status);
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    notifyConnectionError(gatt);
                } else {
                    gatt.requestMtu(MAX_MTU); // request max MTU once all stuff have been done
                }
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                super.onMtuChanged(gatt, mtu, status);
                //status=GATT_SUCCESS(0) if the MTU has been changed successfully
                Log.d(TAG, "onMtuChanged: peripheral name = " + gatt.getDevice().getName() + ", MTU = " + mtu + ", status = " + status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    NCMCPeripheralInfo info = mDiscoveredPeripherals.get(gatt.getDevice().getAddress());
                    if (info != null) {
                        info.mtu = mtu;
                        mDiscoveredPeripherals.put(gatt.getDevice().getAddress(), info);

                        // send central info to peripheral and wait for peripheral confirm the connection
                        NCMCPeerID peerID = new NCMCPeerID(info.name, gatt.getDevice().getAddress());
                        if (mSession != null) {
                            mSession.sendCentralConnectionRequestToPeer(peerID);
                        }
                    }
                } else {
                    notifyConnectionError(gatt);
                }
            }
        };
    }

    public void startBrowsing() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(true);
        }
    }

    public void stopBrowsing() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
            mScanHandler.removeCallbacks(mScanRunnable);
        }
    }

    private Runnable mScanRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsBrowsingOrAdvertising) {
                mIsBrowsingOrAdvertising = false;
                mLEScanner.stopScan(mScanCallback);
                if (mCentralService != null) {
                    mCentralService.notifyBrowsingTimeout();
                }
            }
        }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable && !mIsBrowsingOrAdvertising) {
            // Stops scanning after a pre-defined scan period.
            mScanHandler.postDelayed(mScanRunnable, SCAN_PERIOD);

            // android.os.Build.VERSION.SDK_INT> 21
            mIsBrowsingOrAdvertising = true;
            mLEScanner.startScan(mFilters, mScanSettings, mScanCallback);
        } else {
            mIsBrowsingOrAdvertising = false;
            mLEScanner.stopScan(mScanCallback);
        }
    }

    public void invitePeer(NCMCPeerID peerID) {
        NCMCPeripheralInfo info = mDiscoveredPeripherals.get(peerID.identifier);
        if (info != null) {
            connectToPeripheral(info);
        }
    }

    private void connectToPeripheral(NCMCPeripheralInfo info) {
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.M) {
            info.bluetoothGatt = info.device.connectGatt(mContext, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            try {
                Method m = info.device.getClass().getDeclaredMethod("connectGatt", Context.class, boolean.class, BluetoothGattCallback.class, int.class);
                int transport = info.device.getClass().getDeclaredField("TRANSPORT_LE").getInt(null);     // LE = 2, BREDR = 1, AUTO = 0
                info.bluetoothGatt = (BluetoothGatt) m.invoke(info.device, mContext, false, mGattCallback, transport);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        mDiscoveredPeripherals.put(info.bluetoothGatt.getDevice().getAddress(), info);
    }

    public void disconnectToPeripheral(String deviceAddress) {
        NCMCPeripheralInfo info = this.mDiscoveredPeripherals.get(deviceAddress);
        if (info != null) {
            disconnectToPeripheralByInfo(info);
        }
    }

    private void disconnectToPeripheralByInfo(NCMCPeripheralInfo info) {
        if (info != null) {
            if (info.bluetoothGatt != null) {
                if (info.readCharacteristic != null) {
                    // unsubscribe
                    info.bluetoothGatt.setCharacteristicNotification(info.readCharacteristic, false);

                    BluetoothGattDescriptor descriptor = info.readCharacteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                    descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    info.bluetoothGatt.writeDescriptor(descriptor);
                }
                info.bluetoothGatt.disconnect();
                info.bluetoothGatt.close();
                info.bluetoothGatt = null;
            }
        }
    }

    public void sendCentralDataToPeripheral(byte[] message, String address, int mode) {
        NCMCPeripheralInfo info = this.mDiscoveredPeripherals.get(address);
        if (info != null) {
            if (info.bluetoothGatt != null) {
                synchronized (this.mMessageSendQueue) {
                    List<byte[]> msgs = makeMsg(message, MAX_MTU);

                    for (byte[] msg : msgs) {
                        NCMCMessageData msgData = new NCMCMessageData(info.bluetoothGatt.getDevice().getAddress(), mode == NCMCSession.NCMCSessionSendDataReliable);
                        msgData.addData(msg);
                        this.mMessageSendQueue.add(msgData);
                    }
                }
                executeSendCentralData();
            }
        }
    }

    private void executeSendCentralData() {
        synchronized (this.mMessageSendQueue) {
            if (this.mMessageSendQueue.size() != 0) {
                NCMCMessageData msgInfo = this.mMessageSendQueue.get(0);
                NCMCPeripheralInfo targetInfo = this.mDiscoveredPeripherals.get(msgInfo.getDeviceUUID());
                if (targetInfo != null && targetInfo.bluetoothGatt != null) {
                    byte[] dataToSend = msgInfo.getFullData();

                    if (msgInfo.isReliable && targetInfo.writeWithResponseCharacteristic != null) {
                        targetInfo.writeWithoutResponseCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                        targetInfo.writeWithResponseCharacteristic.setValue(dataToSend);
                        targetInfo.bluetoothGatt.writeCharacteristic(targetInfo.writeWithResponseCharacteristic);
                        Log.d(TAG, "sendDataToPeripheralsWithResponse: byteMsg size :" + dataToSend.length);

                    } else if (!msgInfo.isReliable && targetInfo.writeWithoutResponseCharacteristic != null) {
                        targetInfo.writeWithoutResponseCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                        targetInfo.writeWithoutResponseCharacteristic.setValue(dataToSend);
                        targetInfo.bluetoothGatt.writeCharacteristic(targetInfo.writeWithoutResponseCharacteristic);
                        Log.d(TAG, "sendDataToPeripheralsWithoutResponse: byteMsg size :" + dataToSend.length);
                        // TODO: 16-07-08 test if onCharacteristicWrite() would be called with WRITE_TYPE_NO_RESPONSE
                    }
                }
            }
        }
    }
    //endregion

    //region PERIPHERAL FUNCTIONS
/***********************************************************************/
/**                          PERIPHERAL   FUNCTIONS                   **/
/***********************************************************************/
    public boolean setupPeripheralEnv(NCMCPeripheralService service) {
        this.mPeripheralService = service;
        if (!setup()) {
            if (this.mPeripheralService != null) {
                this.mPeripheralService.notifyDidNotStartAdvertising(NCMCPeripheralService.NCMCPeripheralService_ERROR_UNKNOWN);
            }
            return false;
        }

        this.mAdvertiser = this.mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mAdvertiser == null) {
            Log.d(TAG, "setupPeripheralEnv: can not get ble advertiser!!!");
            return false;
        }
        this.mAdSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();

        ParcelUuid pUuid = new ParcelUuid( UUID.fromString( this.mSession.serviceID ) );
        this.mAdData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(pUuid)
                .build();

        initAdCallback();
        initGattServerCallback();
        initGattServer();

        return true;
    }

    private void initAdCallback() {
        mAdvertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d(TAG, "Start advertising...");
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e("BLE", "Advertising onStartFailure: " + errorCode);
                super.onStartFailure(errorCode);
                mIsBrowsingOrAdvertising = false;
                if (mPeripheralService != null) {
                    mPeripheralService.notifyDidNotStartAdvertising(NCMCPeripheralService.NCMCPeripheralService_ERROR_UNKNOWN);
                }
            }
        };
    }

    private void initGattServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);
        BluetoothGattService service = new BluetoothGattService(UUID.fromString(mSession.serviceID), BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mReceiveWithResponseCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(TRANSFER_CHARACTERISTIC_MSG_FROM_CENTRAL_WITH_RESPONSE_UUID),
                BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE );

        mReceiveWithoutResponseCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(TRANSFER_CHARACTERISTIC_MSG_FROM_CENTRAL_WITHOUT_RESPONSE_UUID),
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE );

        mSendCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(TRANSFER_CHARACTERISTIC_MSG_FROM_PERIPHERAL_UUID),
                BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ );
        BluetoothGattDescriptor gD = new BluetoothGattDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG), BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ);
        mSendCharacteristic.addDescriptor(gD);

        if (!service.addCharacteristic(mReceiveWithResponseCharacteristic)) {
            Log.d(TAG, "initGattServer: cannot add receive with response characteristic!!!");
        }
        if (!service.addCharacteristic(mReceiveWithoutResponseCharacteristic)) {
            Log.d(TAG, "initGattServer: cannot add receive without response characteristic!!!");
        }
        if (!service.addCharacteristic(mSendCharacteristic)) {
            Log.d(TAG, "initGattServer: cannot add send characteristic!!!");
        }

        mBluetoothGattServer.addService(service);
        Log.d(TAG, "initGattServer: done!");
    }

    private void initGattServerCallback() {
        mGattServerCallback = new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                Log.d(TAG, "gatt server connection state changed, new state " + newState);
                super.onConnectionStateChange(device, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (mConnectedCentrals == null) {
                        mConnectedCentrals = new Hashtable<>();
                    }

                    if (mCentralMTUs == null) {
                        mCentralMTUs = new Hashtable<>();
                    }

                    if (!mConnectedCentrals.containsKey(device.getAddress())) {
                        mConnectedCentrals.put(device.getAddress(), device);
                    }

                    if (!mCentralMTUs.containsKey(device.getAddress())) {
                        mCentralMTUs.put(device.getAddress(), DEFAULT_MTU);
                    }
                    // Do nothing, waiting for central information message

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from central:" + device.getName() + " ,address:" + device.getAddress());
                    if (mConnectedCentrals.containsKey(device.getAddress())) {
                        mConnectedCentrals.remove(device.getAddress());
                        mCentralMTUs.remove(device.getAddress());

                        if (mSession != null) {
                            mSession.onCentralDisconnected();
                        }
                    }
                }
            }

            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                Log.d(TAG, "service id = " + service.getUuid() + ", added with status = " + status);
                super.onServiceAdded(status, service);
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                Log.d(TAG, "received a read request from " + device.getName() + " to characteristic " + characteristic.getUuid());
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Log.d(TAG, "onCharacteristicWriteRequest requestId = " + requestId + " " + "received a write request from " + device + " to characteristic " + characteristic.getUuid() + " valueLength = " + value.length + " offset = " + offset + " preparedWrite = " + preparedWrite + " responseNeeded = " + responseNeeded);
                //super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                if (mBluetoothGattServer != null && responseNeeded) {
                    mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }

                if (characteristic.getUuid().toString().equalsIgnoreCase(TRANSFER_CHARACTERISTIC_MSG_FROM_CENTRAL_WITH_RESPONSE_UUID) ||
                        characteristic.getUuid().toString().equalsIgnoreCase(TRANSFER_CHARACTERISTIC_MSG_FROM_CENTRAL_WITHOUT_RESPONSE_UUID)) {
                    if (preparedWrite) {
                        addWriteItemByteBuffer(device.getAddress(), characteristic.getUuid().toString(), value, true);
                    } else {
                        processMsg(characteristic.getValue(), device.getAddress());
                    }
                }
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                Log.d(TAG, "Our gatt server descriptor was read.");
                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Log.d(TAG, "Our gatt server descriptor was written.");
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }

            @Override
            public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                Log.d(TAG, "gatt server on execute write device = " + device + " requestId = " + requestId + " execute = " + execute);
                super.onExecuteWrite(device, requestId, execute);
                if (mBluetoothGattServer != null) {
                    mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[]{});
                }

                executeWriteRequest(device.getAddress(), execute);
            }

            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                super.onNotificationSent(device, status);
                Log.d(TAG, "onNotificationSent to " + device.getName() + " status : " + status);
                synchronized (mMessageSendQueue) {
                    if (status == BluetoothGatt.GATT_SUCCESS && mMessageSendQueue.size() != 0) {
                        mMessageSendQueue.remove(0);
                    }
                }
                if (mMessageSendQueue.size() != 0) {
                    executeSendPeripheralData();
                }
            }

            @Override
            public void onMtuChanged(BluetoothDevice device, int mtu) {
                super.onMtuChanged(device, mtu);
                Log.d(TAG, "onMtuChanged: device : " + device.getName() + ", mtu : " + mtu);
                if (!mCentralMTUs.containsKey(device.getAddress())) {
                    mCentralMTUs.put(device.getAddress(), mtu);
                }
            }
        };
    }

    private void executeWriteRequest(String deviceAddress, boolean execute){
        for(int i=mWriteList.size() - 1; i >= 0;i--){
            NCMCPeripheralWriteRequestData storage  = mWriteList.get(i);
            if(storage != null && storage.getDeviceAddress().equalsIgnoreCase(deviceAddress)){
                if(execute){//if its not for executing, its then for cancelling it
                    if(storage.isCharacter()){
                        processMsg(storage.getFullData(), storage.getDeviceAddress());
                    }
                }

                mWriteList.remove(storage);
                //we are done with this item now.
                storage.clearData();
            }
        }
    }

    private void addWriteItemByteBuffer(String deviceAddress, String uuid,byte[] buffer, boolean isCharacter){
        NCMCPeripheralWriteRequestData  data = getWriteItem(deviceAddress,uuid);
        if(data != null){
            data.addData(buffer);
        }else{
            NCMCPeripheralWriteRequestData newItem = new NCMCPeripheralWriteRequestData(deviceAddress,uuid,isCharacter);
            newItem.addData(buffer);
            mWriteList.add(newItem);
        }
    }


    private NCMCPeripheralWriteRequestData getWriteItem(String deviceAddress, String uuid) {
        NCMCPeripheralWriteRequestData ret = null;
        for (NCMCPeripheralWriteRequestData data : mWriteList){
            if(data != null && data.getUUID().equalsIgnoreCase(uuid) && data.getDeviceAddress().equalsIgnoreCase(deviceAddress)){
                ret = data;
                break;
            }
        }
        return ret;
    }

    public void startAdvertising() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            setIsAdvertise(true);
        }
    }

    public void stopAdvertising() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
            mScanHandler.removeCallbacks(mScanRunnable);
        }
    }

    private void setIsAdvertise(boolean enable) {
        if (enable) {
            mIsBrowsingOrAdvertising = true;
            mAdvertiser.startAdvertising(mAdSettings, mAdData, mAdvertisingCallback);
        } else {
            mIsBrowsingOrAdvertising = false;
            if (mAdvertiser != null) {
                mAdvertiser.stopAdvertising(mAdvertisingCallback);
            }
        }
    }

    public void sendPeripheralDataToCentral(byte[] message, String centralAddress) {
        BluetoothDevice centralDevice = this.mConnectedCentrals.get(centralAddress);
        Integer centralMtu  = this.mCentralMTUs.get(centralAddress);
        if (mSendCharacteristic != null && centralDevice != null && centralMtu != null) {
            List<byte[]> msgs = makeMsg(message, centralMtu);
            synchronized (mMessageSendQueue) {
                for (byte[] msg : msgs) {
                    NCMCMessageData msgData = new NCMCMessageData(centralAddress, true);
                    msgData.addData(msg);
                    this.mMessageSendQueue.add(msgData);
                }
            }

            executeSendPeripheralData();
        }
    }

    private void executeSendPeripheralData() {
        synchronized (mMessageSendQueue) {
            if (mMessageSendQueue.size() != 0 && mBluetoothGattServer != null) {
                NCMCMessageData msgInfo = this.mMessageSendQueue.get(0);
                BluetoothDevice centralDevice = this.mConnectedCentrals.get(msgInfo.getDeviceUUID());
                if (centralDevice != null) {
                    byte[] dataToSend = msgInfo.getFullData();
                    mSendCharacteristic.setValue(dataToSend);
                    mBluetoothGattServer.notifyCharacteristicChanged(centralDevice, mSendCharacteristic, false);
                    Log.d(TAG, "sendDataToCentral: byteMsg size :" + dataToSend.length);
                } else {
                    Log.d(TAG, "sendDataToCentral: failed, centralDevice = null");
                }
            } else {
                Log.d(TAG, "sendDataToCentral: failed, PeripheralMessageQueue.size() = " + mMessageSendQueue.size() +
                        "m_bluetoothGattServer = " + (mBluetoothGattServer != null));
            }
        }
    }

    protected void notifyDidReceiveInvitationFromPeer(NCMCPeerID peerID) {
        if (this.mDiscoveredPeripherals != null) {
            this.mPeripheralService.notifyDidReceiveInvitationFromPeer(peerID);
        }
    }
    //endregion
}
