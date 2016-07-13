package com.nclab.ncmultipeerconnectivity;

import java.io.Serializable;
import java.util.UUID;

/**
 * Peer IDs uniquely identify an app running on a device to nearby peers.
 */
public class NCMCPeerID implements Serializable{
    protected String identifier; // device address
    protected String displayName; // device user define name
    protected char uniqueID; // uniqueID in current session, assigned by central

    protected NCMCPeerID(String _displayName, String _identifier, char _uniqueID) {
        this.displayName = _displayName;
        this.identifier = _identifier;
        this.uniqueID = _uniqueID;
    }

    public NCMCPeerID(String _displayName) {
        this.displayName = _displayName;
        this.identifier = UUID.randomUUID().toString();
        this.uniqueID = (char)-1;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
