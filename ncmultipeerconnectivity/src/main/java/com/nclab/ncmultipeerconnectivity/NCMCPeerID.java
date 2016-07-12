package com.nclab.ncmultipeerconnectivity;

import java.io.Serializable;
import java.util.UUID;

/**
 * Peer IDs uniquely identify an app running on a device to nearby peers.
 */
public class NCMCPeerID implements Serializable{
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
