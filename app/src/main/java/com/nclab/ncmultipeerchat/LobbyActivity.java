package com.nclab.ncmultipeerchat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.nclab.ncmultipeerconnectivity.NCMCPeerID;

import java.util.List;

public class LobbyActivity extends Activity {

    private TextView lbMsg;
    private TextView txtPlayer1;
    private TextView txtPlayer2;
    private TextView txtPlayer3;
    private TextView txtPlayer4;
    private ImageButton btnStartGame;
    private ImageButton btnBack;


    private final IntentFilter m_intentFilter = new IntentFilter();

    private BroadcastReceiver m_broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case MultiplayerController.BLE_BROADCAST_START_FAILED: {
                    new AlertDialog.Builder(LobbyActivity.this).setTitle(getResources().getString(R.string.title)).setMessage(getResources().getString(R.string.startfailed)).setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            backToSettingActivity();
                        }
                    }).show();
                    break;
                }
                case MultiplayerController.BLE_BROADCAST_FOUND_PEER: {
                    Bundle bundle = intent.getExtras();
                    if (bundle.containsKey(MultiplayerController.BLE_BROADCAST_PEERID)) {
                        final NCMCPeerID peerID = (NCMCPeerID)bundle.getSerializable(MultiplayerController.BLE_BROADCAST_PEERID);
                        if (peerID != null) {
                            String name = MultiplayerController.getInstance().stringForMCPeerDisplayName(peerID.getDisplayName());
                            String msg = " \"" + name + "\" " + getResources().getString(R.string.periphfound);
                            new AlertDialog.Builder(LobbyActivity.this).setTitle(getResources().getString(R.string.title)).setMessage(msg).setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    MultiplayerController.getInstance().invitePeer(peerID);
                                }
                            }).setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //
                                }
                            }).show();
                        }
                    }
                    break;
                }
                case MultiplayerController.BLE_BROADCAST_INVITATION: {
                    Bundle bundle = intent.getExtras();
                    if (bundle.containsKey(MultiplayerController.BLE_BROADCAST_PEERID)) {
                        final NCMCPeerID peerID = (NCMCPeerID)bundle.getSerializable(MultiplayerController.BLE_BROADCAST_PEERID);
                        if (peerID != null) {
                            String name = MultiplayerController.getInstance().stringForMCPeerDisplayName(peerID.getDisplayName());
                            String msg = getResources().getString(R.string.centralfoundp1) + " \"" + name + "\" " + getResources().getString(R.string.centralfoundp2);
                            new AlertDialog.Builder(LobbyActivity.this).setTitle(getResources().getString(R.string.title)).setMessage(msg).setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    MultiplayerController.getInstance().sendResponseToInvitation(true, peerID);
                                }
                            }).setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    MultiplayerController.getInstance().sendResponseToInvitation(false, peerID);
                                }
                            }).show();
                        }
                    }
                    break;
                }
                case MultiplayerController.BLE_BROADCAST_SCAN_TIMEOUT: {
                    String msg;
                    if (MultiplayerController.getInstance().isHost())
                    {
                        msg = getResources().getString(R.string.scanfinish);
                        new AlertDialog.Builder(LobbyActivity.this).setTitle(getResources().getString(R.string.title)).setMessage(msg).setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (MultiplayerController.getInstance().isHost()) {
                                    MultiplayerController.getInstance().startHost();
                                }
                            }
                        }).setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //
                            }
                        }).show();
                    }

                    break;
                }
                case MultiplayerController.BLE_BROADCAST_GO_TO_CHATROOM: {
                    goToChat();
                    break;
                }
                case MultiplayerController.BLE_BROADCAST_UPDATE_PLAYERLIST:{
                    updateUI();
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        btnBack = (ImageButton)this.findViewById(R.id.btnBackToSC);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToSettingActivity();
            }
        });

        btnStartGame = (ImageButton)this.findViewById(R.id.btnStartGame);
        if (MultiplayerController.getInstance().isHost()) {
            btnStartGame.setEnabled(false);
            btnStartGame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MultiplayerController.getInstance().gotoChatRoom();

                    goToChat();
                }
            });
        } else {
            btnStartGame.setEnabled(false);
            btnStartGame.setVisibility(View.INVISIBLE);
        }


        lbMsg = (TextView)this.findViewById(R.id.lbMsg);
        txtPlayer1 = (TextView)this.findViewById(R.id.txtPlayer1);
        txtPlayer2 = (TextView)this.findViewById(R.id.txtPlayer2);
        txtPlayer3 = (TextView)this.findViewById(R.id.txtPlayer3);
        txtPlayer4 = (TextView)this.findViewById(R.id.txtPlayer4);


        if (MultiplayerController.getInstance().isHost()) {
            m_intentFilter.addAction(MultiplayerController.BLE_BROADCAST_FOUND_PEER);
            m_intentFilter.addAction(MultiplayerController.BLE_BROADCAST_SCAN_TIMEOUT);
        } else {
            m_intentFilter.addAction(MultiplayerController.BLE_BROADCAST_INVITATION);
        }

        m_intentFilter.addAction(MultiplayerController.BLE_BROADCAST_GO_TO_CHATROOM);
        m_intentFilter.addAction(MultiplayerController.BLE_BROADCAST_START_FAILED);
        m_intentFilter.addAction(MultiplayerController.BLE_BROADCAST_UPDATE_PLAYERLIST);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            backToSettingActivity();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MultiplayerController.getInstance().setContext(this);
        registerReceiver(m_broadcastReceiver, m_intentFilter);

        if (MultiplayerController.getInstance().isHost()) {
            MultiplayerController.getInstance().startHost();
        } else {
            MultiplayerController.getInstance().startClient();
        }

        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MultiplayerController.getInstance().setContext(null);
        unregisterReceiver(m_broadcastReceiver);

        if (MultiplayerController.getInstance().isHost()) {
            MultiplayerController.getInstance().stopHost();
        } else {
            MultiplayerController.getInstance().stopClient();
        }
    }

    private void backToSettingActivity() {
        MultiplayerController.getInstance().disconnect();

        Intent intent = new Intent();
        intent.setClass(LobbyActivity.this, SettingActivity.class);
        LobbyActivity.this.startActivity(intent);
        LobbyActivity.this.finish();
    }

    private void goToChat() {
        Intent intent = new Intent();
        intent.setClass(LobbyActivity.this, PackageRateActivity.class);
        LobbyActivity.this.startActivity(intent);
        LobbyActivity.this.finish();
    }

    private void updateUI() {
        resetUI();
        List<NCMCPeerID> playerData = MultiplayerController.getInstance().getCurrentSessionPlayerIDs();
        Log.d("LobbyActivity", "updateUI: player count = " + playerData.size());
        for (int i=0; i<playerData.size(); ++i) {
            NCMCPeerID pid = playerData.get(i);
            if (i == 0) {
                txtPlayer1.setText(MultiplayerController.getInstance().stringForMCPeerDisplayName(pid.getDisplayName()));
                txtPlayer1.setTextColor(Color.WHITE);
            }
            if (i == 1) {
                txtPlayer2.setText(MultiplayerController.getInstance().stringForMCPeerDisplayName(pid.getDisplayName()));
                txtPlayer2.setTextColor(Color.WHITE);
            }
            if (i == 2) {
                txtPlayer3.setText(MultiplayerController.getInstance().stringForMCPeerDisplayName(pid.getDisplayName()));
                txtPlayer3.setTextColor(Color.WHITE);
            }
            if (i == 3) {
                txtPlayer4.setText(MultiplayerController.getInstance().stringForMCPeerDisplayName(pid.getDisplayName()));
                txtPlayer4.setTextColor(Color.WHITE);
            }
        }

        if (playerData.size() >= 2) {
            lbMsg.setText(getResources().getString(R.string.readytogame));
            if (MultiplayerController.getInstance().isHost()) {
                btnStartGame.setEnabled(true);
            }
        } else {
            if (MultiplayerController.getInstance().isHost()) {
                btnStartGame.setEnabled(false);
            }
        }
    }

    private void resetUI() {
        if (MultiplayerController.getInstance().isHost()) {
            lbMsg.setText(getResources().getString(R.string.waitingplayer));
            txtPlayer1.setTextColor(Color.WHITE);
            txtPlayer1.setText(MultiplayerController.getInstance().stringForMCPeerDisplayName(MultiplayerController.getInstance().getLocalName()));
            txtPlayer2.setTextColor(Color.GRAY);
            txtPlayer2.setText(getResources().getString(R.string.empty));
            txtPlayer3.setTextColor(Color.GRAY);
            txtPlayer3.setText(getResources().getString(R.string.empty));
            txtPlayer4.setTextColor(Color.GRAY);
            txtPlayer4.setText(getResources().getString(R.string.empty));

            btnStartGame.setEnabled(false);
            btnBack.setEnabled(true);
        } else {
            lbMsg.setText(getResources().getString(R.string.searchinggame));
            txtPlayer1.setTextColor(Color.GRAY);
            txtPlayer1.setText(getResources().getString(R.string.empty));
            txtPlayer2.setTextColor(Color.GRAY);
            txtPlayer2.setText(getResources().getString(R.string.empty));
            txtPlayer3.setTextColor(Color.GRAY);
            txtPlayer3.setText(getResources().getString(R.string.empty));
            txtPlayer4.setTextColor(Color.GRAY);
            txtPlayer4.setText(getResources().getString(R.string.empty));

            btnStartGame.setEnabled(false);
            btnStartGame.setVisibility(View.INVISIBLE);
            btnBack.setEnabled(true);
        }
    }
}
