package com.nclab.ncmultipeerconnectivity;

/**
 * This abstract class is used to implement {@link NCMCSession} callbacks.
 */
public abstract class NCMCSessionCallback {

    public void didReceiveData(NCMCSession session, byte[] data, NCMCPeerID fromPeer) {
    }

    public void didChangeState(NCMCSession session, NCMCPeerID peerID, int state) {
    }
}
