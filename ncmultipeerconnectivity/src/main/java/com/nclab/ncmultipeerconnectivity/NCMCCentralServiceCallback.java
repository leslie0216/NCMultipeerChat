package com.nclab.ncmultipeerconnectivity;

/**
 * This abstract class is used to implement {@link NCMCCentralService} callbacks.
 */
public abstract class NCMCCentralServiceCallback {

    public void didFoundPeer(NCMCCentralService centralService, NCMCPeerID peerID) {

    }

    public void didLostPeer(NCMCCentralService centralService, NCMCPeerID peerID) {

    }

    public void didBrowsingTimeout(NCMCCentralService centralService) {

    }

    public void didNotStartBrowsingForPeers(NCMCCentralService centralService, int reason) {

    }

}
