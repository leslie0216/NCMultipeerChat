package com.nclab.ncmultipeerconnectivity;

import java.util.UUID;

/**
 * A peerID represents a device
 */
public class NCMCPeerID {
    protected String identifier;
    protected String displayName;

    protected NCMCPeerID(String _displayName, String _identifier) {
        this.displayName = _displayName;
        this.identifier = _identifier;
    }

    public NCMCPeerID(String _displayName) {
        this.displayName = _displayName;
        this.identifier = UUID.randomUUID().toString();
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
