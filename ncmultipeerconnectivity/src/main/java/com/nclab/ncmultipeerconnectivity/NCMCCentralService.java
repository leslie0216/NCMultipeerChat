package com.nclab.ncmultipeerconnectivity;

/**
 * Create a ble central role
 */
public class NCMCCentralService {
    public static final  int NCMCCentralService_ERROR_BLUETOOTH_OFF = 0;
    public static final  int NCMCCentralService_ERROR_NO_ACCESS = 1;
    public static final  int NCMCCentralService_ERROR_UNKNOWN = 2;

    protected NCMCSession session;
    public NCMCCentralServiceCallback callback;
    private boolean isInited;

    public NCMCCentralService(NCMCSession _session) {
        this.session = _session;
        this.callback = null;
        this.isInited = false;
    }

    public void setupCentralEnvironment() {
        if (!this.isInited) {
            this.isInited = NCMCBluetoothLEManager.getInstance().setupCentralEnv(NCMCCentralService.this);
        }
    }

    public boolean isInited() {
        return this.isInited;
    }

    public void startBrowsingForPeers() {
        if (this.isInited) {
            NCMCBluetoothLEManager.getInstance().startBrowsing();
        }
    }

    public void stopBrowsingForPeers() {
        NCMCBluetoothLEManager.getInstance().stopBrowsing();
    }

    public void invitePeer(NCMCPeerID peerID) {
        NCMCBluetoothLEManager.getInstance().invitePeer(peerID);
    }

    protected void notifyFoundPeer(NCMCPeerID peerID) {
        if (this.callback != null) {
            this.callback.didFoundPeer(NCMCCentralService.this, peerID);
        }
    }

    protected void notifyLostPeer(NCMCPeerID peerID) {
        if (this.callback != null) {
            this.callback.didLostPeer(NCMCCentralService.this, peerID);
        }
    }

    protected void notifyBrowsingTimeout() {
        if (this.callback != null) {
            this.callback.didBrowsingTimeout(NCMCCentralService.this);
        }
    }

    protected void notifyDidNotStartBrowsingForPeers(int reason) {
        if (this.callback != null) {
            this.callback.didNotStartBrowsingForPeers(NCMCCentralService.this, reason);
        }
    }
}
