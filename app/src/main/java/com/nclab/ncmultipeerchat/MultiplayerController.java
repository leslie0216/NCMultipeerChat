package com.nclab.ncmultipeerchat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.nclab.ncmultipeerconnectivity.NCMCCentralService;
import com.nclab.ncmultipeerconnectivity.NCMCCentralServiceCallback;
import com.nclab.ncmultipeerconnectivity.NCMCPeerID;
import com.nclab.ncmultipeerconnectivity.NCMCPeripheralService;
import com.nclab.ncmultipeerconnectivity.NCMCPeripheralServiceCallback;
import com.nclab.ncmultipeerconnectivity.NCMCSession;
import com.nclab.ncmultipeerconnectivity.NCMCSessionCallback;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * control the whole chat
 */
public class MultiplayerController {
    private static final String TAG = "MultiplayerController";

    public static final String TRANSFER_SERVICE_UUID = "ABE00C6E-58F1-44B2-BE41-20E66874B97D";
    private static final char MSG_SERVER_CLIENT_GO_TO_CHAT = 0;
    private static final char MSG_CHAT_MSG = 1;

    public final static String BLE_BROADCAST_FOUND_PEER = "ncchat.nclab.com.BLE_BROADCAST_FOUND_PEER";
    public final static String BLE_BROADCAST_INVITATION = "ncchat.nclab.com.BLE_BROADCAST_INVITATION";
    public final static String BLE_BROADCAST_PEERID = "ncchat.nclab.com.BLE_BROADCAST_PEERID";
    public final static String BLE_BROADCAST_GO_TO_CHATROOM = "ncchat.nclab.com.BLE_BROADCAST_GO_TO_CHATROOM";
    public final static String BLE_BROADCAST_SCAN_TIMEOUT = "ncchat.nclab.com.BLE_BROADCAST_SCAN_TIMEOUT";
    public final static String BLE_BROADCAST_START_FAILED = "ncchat.nclab.com.BLE_BROADCAST_START_FAILED";
    public final static String BLE_BROADCAST_UPDATE_PLAYERLIST = "ncchat.nclab.com.BLE_BROADCAST_UPDATE_PLAYERLIST_";

    public final static String BLE_BROADCAST_RECEIVE_MESSAGE = "ncchat.nclab.com.BLE_BROADCAST_RECEIVE_MESSAGE_";
    public final static String BLE_BROADCAST_RECEIVE_MESSAGE_FROM_NAME = "ncchat.nclab.com.BLE_BROADCAST_RECEIVE_MESSAGE_FROM_NAME";
    public final static String BLE_BROADCAST_RECEIVE_MESSAGE_DATA = "ncchat.nclab.com.BLE_BROADCAST_RECEIVE_MESSAGE_DATA";
    public final static String BLE_BROADCAST_RECEIVE_MESSAGE_TIME = "ncchat.nclab.com.BLE_BROADCAST_RECEIVE_MESSAGE_TIME";

    private NCMCSession currentSession;
    private NCMCCentralService currentCentralService;
    private NCMCPeripheralService currentPeripheralService;
    private List<NCMCPeerID> currentSessionPlayerIDs;
    private boolean isHost;
    private String localName;
    private Context context;

    private NCMCSessionCallback mSessionCallback;
    private NCMCCentralServiceCallback mCentralServiceCallback;
    private NCMCPeripheralServiceCallback mPeripheralServiceCallback;

    private static MultiplayerController ourInstance = new MultiplayerController();

    public static MultiplayerController getInstance() {
        return ourInstance;
    }

    private MultiplayerController() {
    }

    public NCMCSession getCurrentSession() {
        return currentSession;
    }

    public void setCurrentSession(NCMCSession currentSession) {
        this.currentSession = currentSession;
    }

    public NCMCCentralService getCurrentCentralService() {
        return currentCentralService;
    }

    public void setCurrentCentralService(NCMCCentralService currentCentralService) {
        this.currentCentralService = currentCentralService;
    }

    public NCMCPeripheralService getCurrentPeripheralService() {
        return currentPeripheralService;
    }

    public void setCurrentPeripheralService(NCMCPeripheralService currentPeripheralService) {
        this.currentPeripheralService = currentPeripheralService;
    }

    public List<NCMCPeerID> getCurrentSessionPlayerIDs() {
        return currentSessionPlayerIDs;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public void setContext(Context _context) {
        this.context = _context;
        if (this.currentSession != null) {
            this.currentSession.setContext(_context);
        }
    }

    private void broadcastStatus(String action) {
        if (this.context != null) {
            Intent i = new Intent(action);
            this.context.sendBroadcast(i);
        }
    }

    private void broadcastStatus(Intent intent) {
        if (this.context != null) {
            this.context.sendBroadcast(intent);
        }
    }

    private void broadcastStatus(String action, NCMCPeerID peerID) {
        Intent i = new Intent(action);

        i.putExtra(BLE_BROADCAST_PEERID, peerID);
        context.sendBroadcast(i);
    }

    public void initializeControllerForNewMatch() {
        initSessionCallback();

        String uuid = UUID.randomUUID().toString();
        String suuid = uuid.substring(34);
        String displayName = suuid + localName;

        NCMCPeerID peerID = new NCMCPeerID(displayName);
        this.currentSession = new NCMCSession(peerID, TRANSFER_SERVICE_UUID, this.context);
        this.currentSession.callback = mSessionCallback;

        if (this.currentCentralService != null) {
            this.currentCentralService.stopBrowsingForPeers();
            this.currentCentralService.callback = null;
            this.currentCentralService = null;
        }

        if (this.currentPeripheralService != null) {
            this.currentPeripheralService.stopAdvertisingPeer();
            this.currentPeripheralService.callback = null;
            this.currentPeripheralService = null;
        }

        if (this.currentSessionPlayerIDs != null) {
            this.currentSessionPlayerIDs.clear();
        } else {
            this.currentSessionPlayerIDs = new LinkedList<>();
        }

        this.currentSessionPlayerIDs.add(this.currentSession.myPeerID);
    }

    private void initSessionCallback() {
        mSessionCallback = new NCMCSessionCallback() {
            @Override
            public void didReceiveData(NCMCSession session, byte[] data, NCMCPeerID fromPeer) {
                super.didReceiveData(session, data, fromPeer);
                Log.d(TAG, "didReceiveData from " + fromPeer.getDisplayName());
                processMessage(data, fromPeer);
            }

            @Override
            public void didChangeState(NCMCSession session, NCMCPeerID peerID, int state) {
                super.didChangeState(session, peerID, state);
                Log.d(TAG, "didChangeState: " + peerID.getDisplayName() + " state:" + state);

                switch (state) {
                    case NCMCSession.NCMCSessionStateConnected:
                    {
                        currentSessionPlayerIDs.add(peerID);
                        if (!isHost) {
                            if (currentCentralService != null) {
                                currentCentralService.stopBrowsingForPeers();
                                currentCentralService.callback = null;
                                currentCentralService = null;
                            }

                            if (currentPeripheralService != null) {
                                currentPeripheralService.stopAdvertisingPeer();
                                currentPeripheralService.callback = null;
                                currentPeripheralService = null;
                            }
                        }
                        broadcastStatus(BLE_BROADCAST_UPDATE_PLAYERLIST);
                        break;
                    }
                    case NCMCSession.NCMCSessionStateNotConnected:
                    {
                        for (NCMCPeerID pid : currentSessionPlayerIDs) {
                            if (stringForMCPeerDisplayName(pid.getDisplayName()).equalsIgnoreCase(stringForMCPeerDisplayName(peerID.getDisplayName()))) {
                                currentSessionPlayerIDs.remove(pid);
                                broadcastStatus(BLE_BROADCAST_UPDATE_PLAYERLIST);
                                break;
                            }

                        }
                        break;
                    }
                }
            }
        };
    }

    public void createServerHostedGame() {
        initializeControllerForNewMatch();
        initCentralServiceCallback();
        this.isHost = true;
        this.currentCentralService = new NCMCCentralService(this.currentSession);
        this.currentCentralService.callback = mCentralServiceCallback;
        this.currentCentralService.setupCentralEnvironment();
    }

    private void initCentralServiceCallback() {
        mCentralServiceCallback = new NCMCCentralServiceCallback() {
            @Override
            public void didFoundPeer(NCMCCentralService centralService, NCMCPeerID peerID) {
                super.didFoundPeer(centralService, peerID);
                broadcastStatus(BLE_BROADCAST_FOUND_PEER, peerID);
            }

            @Override
            public void didLostPeer(NCMCCentralService centralService, NCMCPeerID peerID) {
                super.didLostPeer(centralService, peerID);
            }

            @Override
            public void didBrowsingTimeout(NCMCCentralService centralService) {
                super.didBrowsingTimeout(centralService);
                broadcastStatus(BLE_BROADCAST_SCAN_TIMEOUT);
            }

            @Override
            public void didNotStartBrowsingForPeers(NCMCCentralService centralService, int reason) {
                super.didNotStartBrowsingForPeers(centralService, reason);
                broadcastStatus(BLE_BROADCAST_START_FAILED);
            }
        };
    }
    
    public void joinServerHostedGame() {
        initializeControllerForNewMatch();
        initPeripheralServiceCallback();

        this.isHost = false;
        this.currentPeripheralService = new NCMCPeripheralService(this.currentSession);
        this.currentPeripheralService.callback = mPeripheralServiceCallback;
        this.currentPeripheralService.setupPeripheralEnvironment();
    }

    private void initPeripheralServiceCallback() {
        mPeripheralServiceCallback = new NCMCPeripheralServiceCallback() {
            @Override
            public void didReceiveInvitationFromPeer(NCMCPeripheralService peripheralService, NCMCPeerID peerID) {
                super.didReceiveInvitationFromPeer(peripheralService, peerID);
                broadcastStatus(BLE_BROADCAST_INVITATION, peerID);

            }

            @Override
            public void didNotStartAdvertising(NCMCPeripheralService peripheralService, int reason) {
                super.didNotStartAdvertising(peripheralService, reason);
                broadcastStatus(BLE_BROADCAST_START_FAILED);
            }
        };
    }

    public void disconnect() {
        this.currentSession.disconnect();
        if (this.currentSessionPlayerIDs != null) {
            this.currentSessionPlayerIDs.clear();
        }
    }

    public void startHost() {
        if (this.isHost && this.currentCentralService != null) {
            this.currentCentralService.startBrowsingForPeers();
        }
    }

    public void startClient() {
        if (!this.isHost && this.currentPeripheralService != null) {
            this.currentPeripheralService.startAdvertisingPeer();
        }
    }

    public void stopHost() {
        if (this.isHost && this.currentCentralService != null) {
            this.currentCentralService.stopBrowsingForPeers();
        }
    }

    public void stopClient() {
        if (!this.isHost && this.currentPeripheralService != null) {
            this.currentPeripheralService.stopAdvertisingPeer();
        }
    }

    public void sendDataToPeer(byte[] msgData, String peerName, int mode) {
        NCMCPeerID peerID = getPeerIDByName(peerName);
        if (peerID != null) {
            Log.d(TAG, "sendDataToPeer: " + peerName);
            byte[] data = packMessageWithType(MSG_CHAT_MSG, msgData);
            List<NCMCPeerID> peerIDs = new LinkedList<>();
            peerIDs.add(peerID);
            this.currentSession.sendData(data, peerIDs, mode);
        }
    }

    public void sendDataToAllPeer(byte[] msgData, int mode) {
        for (NCMCPeerID peerID : this.currentSessionPlayerIDs) {
            if (!peerID.getDisplayName().equalsIgnoreCase(this.currentSession.myPeerID.getDisplayName()))
            {
                Log.d(TAG, "sendDataToPeer: " + peerID.getDisplayName() + " msgData length: " + msgData.length);
                byte[] data = packMessageWithType(MSG_CHAT_MSG, msgData);
                List<NCMCPeerID> peerIDs = new LinkedList<>();
                peerIDs.add(peerID);
                this.currentSession.sendData(data, peerIDs, mode);
            }
        }
    }

    public void enableHighTraffic() {
        for (NCMCPeerID peerID : this.currentSessionPlayerIDs) {
            if (!peerID.getDisplayName().equalsIgnoreCase(this.currentSession.myPeerID.getDisplayName()))
            {
                this.currentSession.enableHighTraffic(peerID);
            }
        }
    }

    public void disableHighTraffic() {
        for (NCMCPeerID peerID : this.currentSessionPlayerIDs) {
            if (!peerID.getDisplayName().equalsIgnoreCase(this.currentSession.myPeerID.getDisplayName()))
            {
                this.currentSession.disableHighTraffic(peerID);
            }
        }
    }

    public void gotoChatRoom() {
        if (this.currentCentralService != null) {
            this.currentCentralService.stopBrowsingForPeers();
            this.currentCentralService.callback = null;
            this.currentCentralService = null;
        }

        if (this.currentPeripheralService != null) {
            this.currentPeripheralService.stopAdvertisingPeer();
            this.currentPeripheralService.callback = null;
            this.currentPeripheralService = null;
        }

        if (this.isHost) {
            byte[] data = packMessageWithType(MSG_SERVER_CLIENT_GO_TO_CHAT);
            this.currentSession.sendData(data, this.currentSessionPlayerIDs, NCMCSession.NCMCSessionSendDataReliable);
        }
    }

    @Nullable
    private NCMCPeerID getPeerIDByName(String name) {
        for (NCMCPeerID peerID : this.currentSessionPlayerIDs) {
            if (stringForMCPeerDisplayName(peerID.getDisplayName()).equalsIgnoreCase(name)) {
                return peerID;
            }
        }

        return null;
    }

    @NonNull
    public String stringForMCPeerDisplayName(String displayName) {
        if (displayName != null && displayName.length() > 2) {
            return displayName.substring(2);
        }

        return "Unknown Player";
    }

    public byte[] packMessageWithType(char msgType) {
        return String.valueOf(msgType).getBytes();
    }

    public byte[] packMessageWithType(char msgType, byte[] msg) {
        byte[] target = packMessageWithType(msgType);

        byte[] result = new byte[target.length + msg.length];

        System.arraycopy(target, 0, result, 0, target.length);
        System.arraycopy(msg, 0, result, target.length, msg.length);

        return result;
    }

    public void processMessage(byte[] message, NCMCPeerID fromPeer) {
        byte target = message[0];
        byte[] data = new byte[message.length - 1];
        System.arraycopy(message, 1, data, 0, message.length - 1);

        switch ((char) target) {
            case MSG_SERVER_CLIENT_GO_TO_CHAT:
            {
                gotoChatRoom();
                broadcastStatus(BLE_BROADCAST_GO_TO_CHATROOM);
                break;
            }
            case MSG_CHAT_MSG:
            {
                Intent i = new Intent(BLE_BROADCAST_RECEIVE_MESSAGE);
                i.putExtra(BLE_BROADCAST_RECEIVE_MESSAGE_FROM_NAME, stringForMCPeerDisplayName(fromPeer.getDisplayName()));
                i.putExtra(BLE_BROADCAST_RECEIVE_MESSAGE_DATA, data);
                i.putExtra(BLE_BROADCAST_RECEIVE_MESSAGE_TIME, System.nanoTime()); // 1 millisecond = 1000000 nanosecond
                broadcastStatus(i);
                break;
            }
        }
    }

    public void invitePeer(NCMCPeerID peerID) {
        if (this.currentCentralService != null) {
            this.currentCentralService.invitePeer(peerID);
        }
    }

    public void sendResponseToInvitation(boolean accept, NCMCPeerID peerID) {
        if (this.currentSession != null) {
            this.currentSession.sendResponseToInvitation(accept, peerID);
        }
    }
}
