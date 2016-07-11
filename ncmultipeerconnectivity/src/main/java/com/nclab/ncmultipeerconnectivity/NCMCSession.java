package com.nclab.ncmultipeerconnectivity;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * A session is used to control the connection between devices
 * android.permission.ACCESS_COARSE_LOCATION and android.permission.ACCESS_FINE_LOCATION must be granted
 */

public class NCMCSession {
    private static final String TAG = "NCMCBluetoothLEManager";

    public static final  int NCMCSessionStateNotConnected = 0;
    public static final  int NCMCSessionStateConnected = 1;
    public static final  int NCMCSessionSendDataReliable = 0;
    public static final  int NCMCSessionSendDataUnreliable = 1;

    private static final char SYSMSG_PERIPHERAL_CENTRAL_REFUSE_INVITATION = 0;
    private static final char SYSMSG_PERIPHERAL_CENTRAL_ACCEPT_INVITATION = 1;
    private static final char SYSMSG_CENTRAL_PERIPHERAL_CONNECTION_REQUEST = 2;
    private static final char SYSMSG_CENTRAL_PERIPHERAL_ASSIGN_IDENTIFIER = 3;
    private static final char SYSMSG_CENTRAL_PERIPHERAL_DEVICE_CONNECTED = 4;
    private static final char SYSMSG_CENTRAL_PERIPHERAL_DEVICE_DISCONNECTED = 5;

    protected String serviceID;
    protected char myUniqueID;
    protected HashMap<String, NCMCDeviceInfo> connectedDevices;

    public NCMCPeerID myPeerID;
    public NCMCSessionCallback callback;

    public NCMCSession(NCMCPeerID _peerID, String _serviceID, Context _context) {
        this.serviceID = _serviceID;
        this.myPeerID = _peerID;
        this.myUniqueID = 0;
        this.connectedDevices = new HashMap<>();
        this.callback = null;

        NCMCBluetoothLEManager.getInstance().clear();
        NCMCBluetoothLEManager.getInstance().setSession(this);
        NCMCBluetoothLEManager.getInstance().setContext(_context);
    }

    public void diconnect() {
        NCMCBluetoothLEManager.getInstance().disconnect();
        this.connectedDevices.clear();
    }

    public void sendData(byte[] data, List<NCMCPeerID> peerIDs, int mode) {
        for (NCMCPeerID peerID : peerIDs) {
            byte[] msg = packUserMessage(data, peerID);
            if (NCMCBluetoothLEManager.getInstance().isCentral()) {
                NCMCBluetoothLEManager.getInstance().sendCentralDataToPeripheral(msg, peerID.identifier, mode);
            } else {
                NCMCBluetoothLEManager.getInstance().sendPeripheralDataToCentral(msg, getCentralDeviceIdentifier());
            }
        }
    }

    public List<NCMCPeerID> getConnectedPeers(){
        List<NCMCPeerID> peers = new LinkedList<>();
        for (NCMCDeviceInfo info : this.connectedDevices.values()) {
            NCMCPeerID peerID = new NCMCPeerID(info.name, info.identifier);
            peers.add(peerID);
        }

        return peers;
    }

    public void sendResponseToInvitation(boolean accept, NCMCPeerID peerID){
        if (accept) {
            // send accept to central and wait for assign id
            NCMCDeviceInfo device = new NCMCDeviceInfo();
            device.identifier = this.myPeerID.identifier;
            device.uniqueID = 0;
            device.name = this.myPeerID.displayName;
            byte[] deviceData = encodeDeviceInfo(device);
            byte[] sysData = packSystemMessage(SYSMSG_PERIPHERAL_CENTRAL_ACCEPT_INVITATION, deviceData);
            NCMCBluetoothLEManager.getInstance().sendPeripheralDataToCentral(sysData, peerID.identifier);

            // clear and init local connected information
            NCMCDeviceInfo centralDevice = new NCMCDeviceInfo();
            centralDevice.identifier = peerID.identifier;
            centralDevice.uniqueID = 0;
            centralDevice.name = peerID.displayName;

            if (this.connectedDevices != null) {
                this.connectedDevices.clear();
                this.connectedDevices.put(peerID.identifier, centralDevice);
            }
        } else {
            // send refuse to central
            byte[] sysData = packSystemMessage(SYSMSG_PERIPHERAL_CENTRAL_REFUSE_INVITATION, null);
            NCMCBluetoothLEManager.getInstance().sendPeripheralDataToCentral(sysData, peerID.identifier);

            // remove central device from local
            this.connectedDevices.remove(peerID.identifier);
        }
    }

    protected void notifyPeerStateChanged(NCMCPeerID peerID, int state) {
        if (this.callback != null) {
            this.callback.didChangeState(NCMCSession.this, peerID, state);
        }
    }

    protected void notifyDidReceiveData(byte[] data, NCMCPeerID fromPeer) {
        if (this.callback != null) {
            this.callback.didReceiveData(NCMCSession.this, data, fromPeer);
        }
    }

    protected void onPeripheralDisconnected(String identifier) {
        NCMCDeviceInfo info = this.connectedDevices.get(identifier);
        if (info != null) {
            // notify this device
            NCMCPeerID peerID = new NCMCPeerID(info.name, info.identifier);
            notifyPeerStateChanged(peerID, NCMCSessionStateNotConnected);

            // if central, notify all peripherals
            if (NCMCBluetoothLEManager.getInstance().isCentral()) {
                byte[] deviceData = encodeDeviceInfo(info);
                byte[] sysData = packSystemMessage(SYSMSG_CENTRAL_PERIPHERAL_DEVICE_DISCONNECTED, deviceData);
                for (NCMCDeviceInfo peripheralInfo : this.connectedDevices.values()) {
                    if (peripheralInfo.uniqueID != 0) {
                        NCMCBluetoothLEManager.getInstance().sendCentralDataToPeripheral(sysData, peripheralInfo.identifier, NCMCSessionSendDataReliable);
                    }
                }
            }
            this.connectedDevices.remove(identifier);
        }
    }

    protected void onCentralDisconnected() {
        for (NCMCDeviceInfo info : this.connectedDevices.values()) {
            if (!info.identifier.equalsIgnoreCase(myPeerID.identifier)) {
                NCMCPeerID peerID = new NCMCPeerID(info.name, info.identifier);
                notifyPeerStateChanged(peerID, NCMCSessionStateNotConnected);
            }
        }
    }

    protected void sendCentralConnectionRequestToPeer(NCMCPeerID peerID) {
        NCMCDeviceInfo centralDevice = new NCMCDeviceInfo();
        centralDevice.identifier = this.myPeerID.identifier;
        centralDevice.uniqueID = 0;
        centralDevice.name = this.myPeerID.displayName;

        byte[] centralDeviceData = encodeDeviceInfo(centralDevice);
        byte[] sysData = packSystemMessage(SYSMSG_CENTRAL_PERIPHERAL_CONNECTION_REQUEST, centralDeviceData);
        NCMCBluetoothLEManager.getInstance().sendCentralDataToPeripheral(sysData, peerID.identifier, NCMCSessionSendDataReliable);
    }

    protected void setSelfAsCentral() {
        if (this.connectedDevices != null) {
            this.connectedDevices.clear();
        } else {
            this.connectedDevices = new HashMap<>();
        }

        this.myUniqueID = 0;
    }

    private NCMCDeviceInfo getDeviceInfoByUniqueID(char uniqueID) {
        for (String key : this.connectedDevices.keySet()) {
            if (this.connectedDevices.get(key).uniqueID == uniqueID) {
                return this.connectedDevices.get(key);
            }
        }

        return null;
    }
    protected String getCentralDeviceIdentifier() {
        NCMCDeviceInfo centralDevice = getDeviceInfoByUniqueID((char)0);
        if (centralDevice != null) {
            return centralDevice.identifier;
        } else {
            return "";
        }
    }

    private byte[] encodeDeviceInfo(NCMCDeviceInfo info) {
        int len = info.identifier.length() + info.name.length() + 1;
        byte[] idbyte = info.identifier.getBytes();
        byte[] namebyte = info.name.getBytes();

        byte[] result = new byte[len];

        System.arraycopy(idbyte, 0, result, 0, idbyte.length);
        result[idbyte.length] = (byte)info.uniqueID;
        System.arraycopy(namebyte, 0, result, idbyte.length+1, namebyte.length);

        return result;
    }

    private NCMCDeviceInfo decodeDeviceInfo(byte[] data) {
        NCMCDeviceInfo info = new NCMCDeviceInfo();
        byte[] idbyte = new byte[36];
        System.arraycopy(data, 0, idbyte, 0, 36);

        info.identifier = new String(idbyte);

        info.uniqueID = (char) data[36];

        byte[] namebyte = new byte[data.length - 37];
        System.arraycopy(data, 37, namebyte, 0, data.length - 37);

        info.name = new String(namebyte);

        return info;
    }

    private byte[] packSystemMessage(char msgType, byte[] msg) {
        int len = (msg != null) ? (msg.length + 3) : 3;
        byte[] result = new byte[len];
        result[0] = 1; // system message
        result[1] = (byte)msgType; // system message type
        result[2] = 0; // nothing, system reserved;

        if (msg != null) {
            System.arraycopy(msg, 0, result, 3, msg.length);
        }

        return result;
    }

    private byte[] packUserMessage(byte[] msg, NCMCPeerID peerID) {
        NCMCDeviceInfo targetDevice = this.connectedDevices.get(peerID.identifier);
        if (targetDevice != null) {
            byte[] result = new byte[msg.length + 3];
            result[0] = 0; // user message
            result[1] = (byte)targetDevice.uniqueID; // message to
            result[2] = (byte)this.myUniqueID; // message from

            System.arraycopy(msg, 0, result, 3, msg.length);

            return result;
        }

        return null;
    }

    protected void onDataReceived(byte[] data, String fromIdentifer) {
        if (data != null && data.length >= 3) {
            char isSysMsg = (char)data[0];
            char extraInfo = (char)data[1];
            char extraInfo2 = (char)data[2];

            byte[] dataMsg = new byte[data.length - 3];
            System.arraycopy(data, 3, dataMsg, 0, data.length-3);

            if (isSysMsg == 1) {
                switch (extraInfo) {
                    case SYSMSG_PERIPHERAL_CENTRAL_REFUSE_INVITATION:
                    {
                        Log.d(TAG, "onDataReceived: SYSMSG_PERIPHERAL_CENTRAL_REFUSE_INVITATION");

                        // disconnect to peripheral
                        if (NCMCBluetoothLEManager.getInstance().isCentral()) {
                            NCMCBluetoothLEManager.getInstance().disconnectToPeripheral(fromIdentifer);
                            this.connectedDevices.remove(fromIdentifer);// may be we don't need to do this
                        }
                        break;
                    }
                    case SYSMSG_PERIPHERAL_CENTRAL_ACCEPT_INVITATION:
                    {
                        Log.d(TAG, "onDataReceived: SYSMSG_PERIPHERAL_CENTRAL_ACCEPT_INVITATION");

                        // assign peripheral info to peripheral
                        NCMCDeviceInfo peripheralDevice = decodeDeviceInfo(dataMsg);
                        peripheralDevice.identifier = fromIdentifer;
                        peripheralDevice.uniqueID = (char)(this.connectedDevices.size() + 1);// id 0 is reserved for central
                        byte[] deviceData = encodeDeviceInfo(peripheralDevice);
                        byte[] sysData = packSystemMessage(SYSMSG_CENTRAL_PERIPHERAL_ASSIGN_IDENTIFIER, deviceData);
                        NCMCBluetoothLEManager.getInstance().sendCentralDataToPeripheral(sysData, fromIdentifer, NCMCSessionSendDataReliable);

                        // update new connected device info to all connected peripherals
                        byte[] sysBroadcastNewDeviceData = packSystemMessage(SYSMSG_CENTRAL_PERIPHERAL_DEVICE_CONNECTED, deviceData);
                        for (NCMCDeviceInfo info : this.connectedDevices.values()) {
                            if (info.uniqueID != 0) {
                                NCMCBluetoothLEManager.getInstance().sendCentralDataToPeripheral(sysBroadcastNewDeviceData, info.identifier, NCMCSessionSendDataReliable);
                            }
                        }

                        // update all connected peripherals to new connected device
                        for (NCMCDeviceInfo peripheralDeviceInfo : this.connectedDevices.values()) {
                            if (peripheralDeviceInfo.uniqueID != 0) {
                                byte[] peripheralDeviceData = encodeDeviceInfo(peripheralDeviceInfo);
                                byte[] sysBroadcastData = packSystemMessage(SYSMSG_CENTRAL_PERIPHERAL_DEVICE_CONNECTED, peripheralDeviceData);
                                NCMCBluetoothLEManager.getInstance().sendCentralDataToPeripheral(sysBroadcastData, fromIdentifer, NCMCSessionSendDataReliable);
                            }
                        }

                        this.connectedDevices.put(fromIdentifer, peripheralDevice);

                        // send connection status notification
                        NCMCPeerID peerID =  new NCMCPeerID(peripheralDevice.name, peripheralDevice.identifier);
                        notifyPeerStateChanged(peerID, NCMCSessionStateConnected);

                        break;
                    }
                    case SYSMSG_CENTRAL_PERIPHERAL_CONNECTION_REQUEST:
                    {
                        Log.d(TAG, "onDataReceived: SYSMSG_CENTRAL_PERIPHERAL_CONNECTION_REQUEST");

                        if (!getCentralDeviceIdentifier().equalsIgnoreCase("")) {
                            // refuse connection directly when another central is being processed
                            byte[] sysData = packSystemMessage(SYSMSG_PERIPHERAL_CENTRAL_REFUSE_INVITATION, null);
                            NCMCBluetoothLEManager.getInstance().sendPeripheralDataToCentral(sysData, fromIdentifer);
                        }

                        NCMCDeviceInfo centralDevice = decodeDeviceInfo(dataMsg);
                        if (centralDevice.uniqueID == 0) {
                            centralDevice.identifier = fromIdentifer; // set with its real identifier
                        }

                        // save central device
                        this.connectedDevices.put(centralDevice.identifier, centralDevice);

                        // broadcast invitation
                        NCMCPeerID peerID = new NCMCPeerID(centralDevice.name, centralDevice.identifier);
                        NCMCBluetoothLEManager.getInstance().notifyDidReceiveInvitationFromPeer(peerID);
                        break;
                    }
                    case SYSMSG_CENTRAL_PERIPHERAL_ASSIGN_IDENTIFIER:
                    {
                        Log.d(TAG, "onDataReceived: SYSMSG_CENTRAL_PERIPHERAL_ASSIGN_IDENTIFIER");
                        NCMCDeviceInfo device = decodeDeviceInfo(dataMsg);
                        if (device != null && device.name.equalsIgnoreCase(this.myPeerID.displayName)) {
                            this.myPeerID.identifier = device.identifier;
                            this.myUniqueID = device.uniqueID;

                            // send central connection status notification
                            NCMCDeviceInfo centralDevice = getDeviceInfoByUniqueID((char)0);
                            if (centralDevice != null) {
                                NCMCPeerID peerID = new NCMCPeerID(centralDevice.name, centralDevice.identifier);
                                notifyPeerStateChanged(peerID, NCMCSessionStateConnected);
                            }
                        }

                        break;
                    }
                    case SYSMSG_CENTRAL_PERIPHERAL_DEVICE_CONNECTED:
                    {
                        Log.d(TAG, "onDataReceived: SYSMSG_CENTRAL_PERIPHERAL_DEVICE_CONNECTED");
                        NCMCDeviceInfo device = decodeDeviceInfo(dataMsg);

                        // we've already added central device
                        if (device.uniqueID != 0 && !device.identifier.equalsIgnoreCase(this.myPeerID.identifier)) {
                            this.connectedDevices.put(device.identifier, device);

                            // send connection status notification
                            NCMCPeerID peerID = new NCMCPeerID(device.name, device.identifier);
                            notifyPeerStateChanged(peerID, NCMCSessionStateConnected);
                        }
                        break;
                    }
                    case SYSMSG_CENTRAL_PERIPHERAL_DEVICE_DISCONNECTED:
                    {
                        Log.d(TAG, "onDataReceived: SYSMSG_CENTRAL_PERIPHERAL_DEVICE_DISCONNECTED");
                        NCMCDeviceInfo device = decodeDeviceInfo(dataMsg);

                        if (this.connectedDevices.containsKey(device.identifier)) {
                            this.connectedDevices.remove(device.identifier);
                            // send connection status notification
                            NCMCPeerID peerID = new NCMCPeerID(device.name, device.identifier);
                            notifyPeerStateChanged(peerID, NCMCSessionStateNotConnected);
                        }
                        break;
                    }
                }
            } else {
                if (NCMCBluetoothLEManager.getInstance().isCentral()) {
                    if (extraInfo == 0) {
                        // data from peripheral to central
                        NCMCDeviceInfo deviceInfo = this.connectedDevices.get(fromIdentifer);
                        if (deviceInfo != null) {
                            NCMCPeerID peerID = new NCMCPeerID(deviceInfo.name, fromIdentifer);
                            notifyDidReceiveData(dataMsg, peerID);
                        }
                    } else {
                        // data from peripheral to peripheral
                        NCMCDeviceInfo targetDevice = getDeviceInfoByUniqueID(extraInfo);
                        if (targetDevice != null) {
                            NCMCBluetoothLEManager.getInstance().sendCentralDataToPeripheral(data, targetDevice.identifier, NCMCSessionSendDataReliable);
                        }
                    }
                } else {
                    if (this.myUniqueID == extraInfo) {
                        NCMCDeviceInfo deviceInfo = getDeviceInfoByUniqueID(extraInfo2);
                        if (deviceInfo != null) {
                            NCMCPeerID peerID = new NCMCPeerID(deviceInfo.name, deviceInfo.identifier);
                            notifyDidReceiveData(dataMsg, peerID);
                        }
                    }
                }
            }
        }
    }
}
