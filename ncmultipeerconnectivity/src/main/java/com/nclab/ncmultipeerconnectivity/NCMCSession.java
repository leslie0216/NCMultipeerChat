package com.nclab.ncmultipeerconnectivity;

import android.content.Context;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * Session objects (NCMCSession) provide support for communication between connected peer devices.
 * If your app creates a session, it can invite other peers to join it.
 * Otherwise, your app can join a session when invited by another peer.
 *
 * NOTE :
 * 1. Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.
 *
 * 2. An app must hold
 * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION ACCESS_COARSE_LOCATION} or
 * {@link android.Manifest.permission#ACCESS_FINE_LOCATION ACCESS_FINE_LOCATION} permission
 *
 * 3. serviceID must be a 36 bytes UUID. e.g. "E8D52AC1-F0E7-494E-BC27-7D92531E7A30"
 */

public class NCMCSession {
    private static final String TAG = "NCMCSession";

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
    protected List<NCMCDeviceInfo> connectedDevices;

    public NCMCPeerID myPeerID;
    public NCMCSessionCallback callback;

    public NCMCSession(NCMCPeerID _peerID, String _serviceID, Context _context) {
        this.serviceID = _serviceID;
        this.myPeerID = _peerID;
        this.connectedDevices = new LinkedList<>();
        this.callback = null;

        NCMCBluetoothLEManager.getInstance().clear();
        NCMCBluetoothLEManager.getInstance().setSession(this);
        NCMCBluetoothLEManager.getInstance().setContext(_context);
    }

    public void disconnect() {
        NCMCBluetoothLEManager.getInstance().disconnect();
        this.connectedDevices.clear();
    }

    public void setContext(Context _context) {
        NCMCBluetoothLEManager.getInstance().setContext(_context);
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
        for (NCMCDeviceInfo info : this.connectedDevices) {
            NCMCPeerID peerID = new NCMCPeerID(info.name, info.identifier, info.uniqueID);
            peers.add(peerID);
        }

        return peers;
    }

    public void sendResponseToInvitation(boolean accept, NCMCPeerID peerID){
        if (accept) {
            // send accept to central and wait for assign id
            NCMCDeviceInfo device = new NCMCDeviceInfo();
            device.identifier = this.myPeerID.identifier;
            device.uniqueID = (char)-1;
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
                this.connectedDevices.add(centralDevice);
            }
        } else {
            // send refuse to central
            byte[] sysData = packSystemMessage(SYSMSG_PERIPHERAL_CENTRAL_REFUSE_INVITATION, null);
            NCMCBluetoothLEManager.getInstance().sendPeripheralDataToCentral(sysData, peerID.identifier);

            // remove central device from local
            this.connectedDevices.clear();
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
        NCMCDeviceInfo info = getDeviceInfoByIdentifier(identifier);
        if (info != null) {
            // notify this device
            NCMCPeerID peerID = new NCMCPeerID(info.name, info.identifier, info.uniqueID);
            notifyPeerStateChanged(peerID, NCMCSessionStateNotConnected);

            // if central, notify all peripherals
            if (NCMCBluetoothLEManager.getInstance().isCentral()) {
                byte[] deviceData = encodeDeviceInfo(info);
                byte[] sysData = packSystemMessage(SYSMSG_CENTRAL_PERIPHERAL_DEVICE_DISCONNECTED, deviceData);
                for (NCMCDeviceInfo peripheralInfo : this.connectedDevices) {
                    if (peripheralInfo.uniqueID != 0) {
                        NCMCBluetoothLEManager.getInstance().sendCentralDataToPeripheral(sysData, peripheralInfo.identifier, NCMCSessionSendDataReliable);
                    }
                }
            }
            this.connectedDevices.remove(info);
        }
    }

    protected void onCentralDisconnected() {
        for (NCMCDeviceInfo info : this.connectedDevices) {
            NCMCPeerID peerID = new NCMCPeerID(info.name, info.identifier, info.uniqueID);
            notifyPeerStateChanged(peerID, NCMCSessionStateNotConnected);
        }

        this.connectedDevices.clear();
    }

    protected void sendCentralConnectionRequestToPeer(NCMCPeerID peerID) {
        NCMCDeviceInfo centralDevice = new NCMCDeviceInfo();
        centralDevice.identifier = this.myPeerID.identifier; // useless, will reset with real identifier got in peripheral

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
            this.connectedDevices = new LinkedList<>();
        }

        this.myPeerID.uniqueID = 0;
    }

    private NCMCDeviceInfo getDeviceInfoByUniqueID(char uniqueID) {
        for (NCMCDeviceInfo info : this.connectedDevices) {
            if (info.uniqueID == uniqueID) {
                return info;
            }
        }

        return null;
    }

    private NCMCDeviceInfo getDeviceInfoByIdentifier(String identifier) {
        if (identifier != null) {
            for (NCMCDeviceInfo info : this.connectedDevices) {
                if (info.identifier.equalsIgnoreCase(identifier)) {
                    return info;
                }
            }
        }

        return null;
    }

    protected char getDeviceUniqueIDByIdentifier(String identifier) {
        NCMCDeviceInfo info = getDeviceInfoByIdentifier(identifier);
        if (info != null) {
            return info.uniqueID;
        }

        return (char)-1;
    }

    private NCMCDeviceInfo getCentralDeviceInfo() {
        return getDeviceInfoByUniqueID((char)0);
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
        int len = info.name.length() + 1;
        byte[] namebyte = info.name.getBytes();

        byte[] result = new byte[len];

        result[0] = (byte)info.uniqueID;
        System.arraycopy(namebyte, 0, result, 1, namebyte.length);

        return result;
    }

    private NCMCDeviceInfo decodeDeviceInfo(byte[] data, String identifier) {
        NCMCDeviceInfo info = new NCMCDeviceInfo();

        info.identifier = identifier;

        info.uniqueID = (char) data[0];

        byte[] namebyte = new byte[data.length - 1];
        System.arraycopy(data, 1, namebyte, 0, data.length - 1);

        info.name = new String(namebyte);

        return info;
    }

    private byte[] packSystemMessage(char msgType, byte[] msg) {
        int len = (msg != null) ? (msg.length + 3) : 3;
        byte[] result = new byte[len];
        result[0] = 1; // system message
        result[1] = (byte)msgType; // system message type
        result[2] = 0; // nothing, system reserved;

        if (msg != null && msg.length > 0) {
            System.arraycopy(msg, 0, result, 3, msg.length);
        }

        return result;
    }

    private byte[] packUserMessage(byte[] msg, NCMCPeerID peerID) {
        int len = (msg != null) ? (msg.length + 3) : 3;

        byte[] result = new byte[len];
        result[0] = 0; // user message
        result[1] = (byte)peerID.uniqueID; // message to
        result[2] = (byte)this.myPeerID.uniqueID; // message from

        if (msg != null && msg.length > 0) {
            System.arraycopy(msg, 0, result, 3, msg.length);
        }

        return result;
    }

    protected void onDataReceived(byte[] data, String fromIdentifier) {
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
                            NCMCBluetoothLEManager.getInstance().disconnectToPeripheral(fromIdentifier);
                        }
                        break;
                    }
                    case SYSMSG_PERIPHERAL_CENTRAL_ACCEPT_INVITATION:
                    {
                        Log.d(TAG, "onDataReceived: SYSMSG_PERIPHERAL_CENTRAL_ACCEPT_INVITATION");

                        // assign peripheral info to peripheral
                        NCMCDeviceInfo newPeripheralDevice = decodeDeviceInfo(dataMsg, fromIdentifier);
                        newPeripheralDevice.uniqueID = (char)(this.connectedDevices.size() + 1);// id 0 is reserved for central
                        byte[] newDeviceData = encodeDeviceInfo(newPeripheralDevice);
                        byte[] sysData = packSystemMessage(SYSMSG_CENTRAL_PERIPHERAL_ASSIGN_IDENTIFIER, newDeviceData);
                        NCMCBluetoothLEManager.getInstance().sendCentralDataToPeripheral(sysData, fromIdentifier, NCMCSessionSendDataReliable);

                        // update new connected device info to all connected peripherals
                        byte[] sysBroadcastNewDeviceData = packSystemMessage(SYSMSG_CENTRAL_PERIPHERAL_DEVICE_CONNECTED, newDeviceData);
                        for (NCMCDeviceInfo info : this.connectedDevices) {
                            if (info.uniqueID != 0) {
                                NCMCBluetoothLEManager.getInstance().sendCentralDataToPeripheral(sysBroadcastNewDeviceData, info.identifier, NCMCSessionSendDataReliable);
                            }
                        }

                        // update all connected peripherals to new connected device
                        for (NCMCDeviceInfo info : this.connectedDevices) {
                            byte[] connectedPeripheralDeviceData = encodeDeviceInfo(info);
                            byte[] sysBroadcastData = packSystemMessage(SYSMSG_CENTRAL_PERIPHERAL_DEVICE_CONNECTED, connectedPeripheralDeviceData);
                            NCMCBluetoothLEManager.getInstance().sendCentralDataToPeripheral(sysBroadcastData, fromIdentifier, NCMCSessionSendDataReliable);
                        }

                        this.connectedDevices.add(newPeripheralDevice);

                        // send connection status notification
                        NCMCPeerID peerID =  new NCMCPeerID(newPeripheralDevice.name, newPeripheralDevice.identifier, newPeripheralDevice.uniqueID);
                        notifyPeerStateChanged(peerID, NCMCSessionStateConnected);

                        break;
                    }
                    case SYSMSG_CENTRAL_PERIPHERAL_CONNECTION_REQUEST:
                    {
                        Log.d(TAG, "onDataReceived: SYSMSG_CENTRAL_PERIPHERAL_CONNECTION_REQUEST");

                        if (getCentralDeviceInfo() != null) {
                            // refuse connection directly when another central is being processed
                            byte[] sysData = packSystemMessage(SYSMSG_PERIPHERAL_CENTRAL_REFUSE_INVITATION, null);
                            NCMCBluetoothLEManager.getInstance().sendPeripheralDataToCentral(sysData, fromIdentifier);
                        }

                        NCMCDeviceInfo centralDevice = decodeDeviceInfo(dataMsg, fromIdentifier);

                        // save central device
                        this.connectedDevices.add(centralDevice);

                        // broadcast invitation
                        NCMCPeerID peerID = new NCMCPeerID(centralDevice.name, centralDevice.identifier, centralDevice.uniqueID);
                        NCMCBluetoothLEManager.getInstance().notifyDidReceiveInvitationFromPeer(peerID);
                        break;
                    }
                    case SYSMSG_CENTRAL_PERIPHERAL_ASSIGN_IDENTIFIER:
                    {
                        Log.d(TAG, "onDataReceived: SYSMSG_CENTRAL_PERIPHERAL_ASSIGN_IDENTIFIER");
                        NCMCDeviceInfo device = decodeDeviceInfo(dataMsg, fromIdentifier);
                        if (device != null && device.name.equalsIgnoreCase(this.myPeerID.displayName)) {
                            this.myPeerID.identifier = device.identifier; // useless, because a device never send message to itself
                            this.myPeerID.uniqueID = device.uniqueID;

                            // send central connection status notification
                            NCMCDeviceInfo centralDevice = getCentralDeviceInfo();
                            if (centralDevice != null) {
                                NCMCPeerID peerID = new NCMCPeerID(centralDevice.name, centralDevice.identifier, centralDevice.uniqueID);
                                notifyPeerStateChanged(peerID, NCMCSessionStateConnected);
                            }
                        }

                        break;
                    }
                    case SYSMSG_CENTRAL_PERIPHERAL_DEVICE_CONNECTED:
                    {
                        Log.d(TAG, "onDataReceived: SYSMSG_CENTRAL_PERIPHERAL_DEVICE_CONNECTED");
                        NCMCDeviceInfo device = decodeDeviceInfo(dataMsg, fromIdentifier);

                        // we've already added central device
                        if (device.uniqueID != 0 && device.uniqueID != this.myPeerID.uniqueID) {
                            this.connectedDevices.add(device);

                            // send connection status notification
                            NCMCPeerID peerID = new NCMCPeerID(device.name, device.identifier, device.uniqueID);
                            notifyPeerStateChanged(peerID, NCMCSessionStateConnected);
                        }
                        break;
                    }
                    case SYSMSG_CENTRAL_PERIPHERAL_DEVICE_DISCONNECTED:
                    {
                        Log.d(TAG, "onDataReceived: SYSMSG_CENTRAL_PERIPHERAL_DEVICE_DISCONNECTED");
                        NCMCDeviceInfo device = decodeDeviceInfo(dataMsg, fromIdentifier);

                        NCMCDeviceInfo connectedDevice = getDeviceInfoByUniqueID(device.uniqueID);

                        if (connectedDevice != null) {
                            this.connectedDevices.remove(connectedDevice);
                            // send connection status notification
                            NCMCPeerID peerID = new NCMCPeerID(connectedDevice.name, connectedDevice.identifier, connectedDevice.uniqueID);
                            notifyPeerStateChanged(peerID, NCMCSessionStateNotConnected);
                        }
                        break;
                    }
                }
            } else {
                if (NCMCBluetoothLEManager.getInstance().isCentral()) {
                    if (extraInfo == 0) {
                        // data from peripheral to central
                        NCMCDeviceInfo deviceInfo = getDeviceInfoByIdentifier(fromIdentifier);
                        if (deviceInfo != null) {
                            NCMCPeerID peerID = new NCMCPeerID(deviceInfo.name, deviceInfo.identifier, deviceInfo.uniqueID);
                            notifyDidReceiveData(dataMsg, peerID);
                        }
                    } else {
                        // data from peripheral to peripheral
                        NCMCDeviceInfo targetDevice = getDeviceInfoByUniqueID(extraInfo);
                        if (targetDevice != null) {
                            NCMCBluetoothLEManager.getInstance().sendCentralDataToPeripheral(data, targetDevice.identifier, NCMCSessionSendDataUnreliable);
                        }
                    }
                } else {
                    if (this.myPeerID.uniqueID == extraInfo) {
                        NCMCDeviceInfo deviceInfo = getDeviceInfoByUniqueID(extraInfo2);
                        if (deviceInfo != null) {
                            NCMCPeerID peerID = new NCMCPeerID(deviceInfo.name, deviceInfo.identifier, deviceInfo.uniqueID);
                            notifyDidReceiveData(dataMsg, peerID);
                        }
                    }
                }
            }
        }
    }
}
