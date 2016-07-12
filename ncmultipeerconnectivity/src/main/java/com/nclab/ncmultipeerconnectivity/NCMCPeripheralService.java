package com.nclab.ncmultipeerconnectivity;

/**
 * Create a Bluetooth low energy peripheral role
 * Tell nearby peers that your app is willing to join sessions of a specified type.
 */
public class NCMCPeripheralService {
    public static final  int NCMCPeripheralService_ERROR_BLUETOOTH_OFF = 0;
    public static final  int NCMCPeripheralService_ERROR_NO_ACCESS = 1;
    public static final  int NCMCPeripheralService_ERROR_UNKNOWN = 2;
    public static final  int NCMCPeripheralService_ERROR_NOT_SUPPORT = 3;

    protected NCMCSession session;
    public NCMCPeripheralServiceCallback callback;
    private boolean isInited;

    public NCMCPeripheralService(NCMCSession _session) {
        this.session = _session;
        this.callback = null;
        this.isInited = false;
    }

    public void setupPeripheralEnvironment() {
        if (!this.isInited) {
            this.isInited = NCMCBluetoothLEManager.getInstance().setupPeripheralEnv(NCMCPeripheralService.this);
        }
    }

    public boolean isInited() {
        return this.isInited;
    }

    public void startAdvertisingPeer() {
        if (isInited) {
            NCMCBluetoothLEManager.getInstance().startAdvertising();
        }
    }

    public void stopAdvertisingPeer() {
        NCMCBluetoothLEManager.getInstance().stopAdvertising();
    }

    protected void notifyDidNotStartAdvertising(int reason)  {
        if (this.callback != null) {
            this.callback.didNotStartAdvertising(NCMCPeripheralService.this, reason);
        }
    }

    protected void notifyDidReceiveInvitationFromPeer(NCMCPeerID peerID) {
        if (this.callback != null) {
            this.callback.didReceiveInvitationFromPeer(NCMCPeripheralService.this, peerID);
        }
    }
}
