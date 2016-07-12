package com.nclab.ncmultipeerconnectivity;

/**
 * This abstract class is used to implement {@link NCMCPeripheralService} callbacks.
 */
public abstract class NCMCPeripheralServiceCallback {

    /**
     * Notify received an invitation from a remote central device
     * * <p>An application must call {@link NCMCSession#sendResponseToInvitation}
     * to complete the request</p>.
     * @param peripheralService the peripheral service running on this device
     * @param peerID the central device that sent the invitation
     */
    public void didReceiveInvitationFromPeer(NCMCPeripheralService peripheralService, NCMCPeerID peerID) {

    }

    public void didNotStartAdvertising(NCMCPeripheralService peripheralService, int reason) {

    }
}
