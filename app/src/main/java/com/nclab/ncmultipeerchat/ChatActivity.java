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
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.nclab.ncmultipeerconnectivity.NCMCPeerID;
import com.nclab.ncmultipeerconnectivity.NCMCSession;

import java.util.List;

public class ChatActivity extends Activity {

    private TextView txtChatHist;
    private TextView txtLocalPlayer;
    private TextView txtPlayer1;
    private TextView txtPlayer2;
    private TextView txtPlayer3;
    private Button btnSendTo1;
    private Button btnSendTo2;
    private Button btnSendTo3;

    private final IntentFilter m_intentFilter = new IntentFilter();

    private BroadcastReceiver m_broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case MultiplayerController.BLE_BROADCAST_RECEIVE_MESSAGE: {
                    Bundle bundle = intent.getExtras();
                    if (bundle.containsKey(MultiplayerController.BLE_BROADCAST_RECEIVE_MESSAGE_FROM_NAME) && bundle.containsKey(MultiplayerController.BLE_BROADCAST_RECEIVE_MESSAGE_DATA)) {
                        String fromName = bundle.getString(MultiplayerController.BLE_BROADCAST_RECEIVE_MESSAGE_FROM_NAME);
                        byte[] data = bundle.getByteArray(MultiplayerController.BLE_BROADCAST_RECEIVE_MESSAGE_DATA);
                        if (data != null) {
                            String message = new String(data);
                            String chat = fromName + " : " + message;
                            String hist = txtChatHist.getText().toString();
                            String newHist = hist + "\n" + chat;
                            txtChatHist.setText(newHist);
                        }
                    }
                    break;
                }
                case MultiplayerController.BLE_BROADCAST_UPDATE_PLAYERLIST: {
                    setPlayerList();
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        txtChatHist = (TextView)this.findViewById(R.id.txtChatHist);
        txtChatHist.setText("");
        txtPlayer1 = (TextView)this.findViewById(R.id.txtSendToPlayer1);
        txtPlayer2 = (TextView)this.findViewById(R.id.txtSendToPlayer2);
        txtPlayer3 = (TextView)this.findViewById(R.id.txtSendToPlayer3);
        txtLocalPlayer = (TextView)this.findViewById(R.id.txtPlayerLocal);

        btnSendTo1 = (Button)this.findViewById(R.id.btnSendTo1);
        btnSendTo1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = getChatMsg();
                if (msg.length() > 0) {
                    MultiplayerController.getInstance().sendDataToPeer(msg.getBytes(), txtPlayer1.getText().toString(), NCMCSession.NCMCSessionSendDataUnreliable);
                    MultiplayerController.getInstance().sendDataToPeer(msg.getBytes(), txtPlayer1.getText().toString(), NCMCSession.NCMCSessionSendDataUnreliable);
                    MultiplayerController.getInstance().sendDataToPeer(msg.getBytes(), txtPlayer1.getText().toString(), NCMCSession.NCMCSessionSendDataUnreliable);
                    MultiplayerController.getInstance().sendDataToPeer(msg.getBytes(), txtPlayer1.getText().toString(), NCMCSession.NCMCSessionSendDataUnreliable);
                    MultiplayerController.getInstance().sendDataToPeer(msg.getBytes(), txtPlayer1.getText().toString(), NCMCSession.NCMCSessionSendDataUnreliable);
                }
            }
        });

        btnSendTo2 = (Button)this.findViewById(R.id.btnSendTo2);
        btnSendTo2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = getChatMsg();
                if (msg.length() > 0) {
                    MultiplayerController.getInstance().sendDataToPeer(msg.getBytes(), txtPlayer2.getText().toString(), NCMCSession.NCMCSessionSendDataUnreliable);
                }
            }
        });

        btnSendTo3 = (Button)this.findViewById(R.id.btnSendTo3);
        btnSendTo3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = getChatMsg();
                if (msg.length() > 0) {
                    MultiplayerController.getInstance().sendDataToPeer(msg.getBytes(), txtPlayer3.getText().toString(), NCMCSession.NCMCSessionSendDataUnreliable);
                }
            }
        });

        m_intentFilter.addAction(MultiplayerController.BLE_BROADCAST_RECEIVE_MESSAGE);
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
        setPlayerList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MultiplayerController.getInstance().setContext(null);
        unregisterReceiver(m_broadcastReceiver);
    }

    private void backToSettingActivity() {
        MultiplayerController.getInstance().disconnect();

        Intent intent = new Intent();
        intent.setClass(ChatActivity.this, SettingActivity.class);
        ChatActivity.this.startActivity(intent);
        ChatActivity.this.finish();
    }

    public String getChatMsg() {
        TextView user = (TextView)this.findViewById(R.id.txtChatMsg);
        return  user.getText().toString();
    }

    private void resetUI() {
        txtPlayer1.setTextColor(Color.GRAY);
        txtPlayer1.setText(getResources().getString(R.string.empty));
        btnSendTo1.setEnabled(false);

        txtPlayer2.setTextColor(Color.GRAY);
        txtPlayer2.setText(getResources().getString(R.string.empty));
        btnSendTo2.setEnabled(false);

        txtPlayer3.setTextColor(Color.GRAY);
        txtPlayer3.setText(getResources().getString(R.string.empty));
        btnSendTo3.setEnabled(false);

        txtLocalPlayer.setText(MultiplayerController.getInstance().getLocalName());
        txtLocalPlayer.setTextColor(Color.WHITE);
    }

    private void setPlayerList(){
        resetUI();
        List<NCMCPeerID> playerData = MultiplayerController.getInstance().getCurrentSessionPlayerIDs();
        int i = 0;
        for (NCMCPeerID pid : playerData) {
            if (!MultiplayerController.getInstance().stringForMCPeerDisplayName(pid.getDisplayName()).equalsIgnoreCase(MultiplayerController.getInstance().getLocalName())) {
                if (i == 0) {
                    txtPlayer1.setText(MultiplayerController.getInstance().stringForMCPeerDisplayName(pid.getDisplayName()));
                    txtPlayer1.setTextColor(Color.WHITE);
                    btnSendTo1.setEnabled(true);
                }
                if (i == 1) {
                    txtPlayer2.setText(MultiplayerController.getInstance().stringForMCPeerDisplayName(pid.getDisplayName()));
                    txtPlayer2.setTextColor(Color.WHITE);
                    btnSendTo2.setEnabled(true);
                }
                if (i == 2) {
                    txtPlayer3.setText(MultiplayerController.getInstance().stringForMCPeerDisplayName(pid.getDisplayName()));
                    txtPlayer3.setTextColor(Color.WHITE);
                    btnSendTo3.setEnabled(true);
                }
                ++i;
            }
        }
    }
}
