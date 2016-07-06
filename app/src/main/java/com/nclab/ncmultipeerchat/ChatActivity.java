package com.nclab.ncmultipeerchat;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class ChatActivity extends Activity {

    private TextView txtChatHist;
    private TextView txtLocalPlayer;
    private TextView txtPlayer1;
    private TextView txtPlayer2;
    private TextView txtPlayer3;
    private Button btnSendTo1;
    private Button btnSendTo2;
    private Button btnSendTo3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        txtChatHist = (TextView)this.findViewById(R.id.txtChatHist);
        txtPlayer1 = (TextView)this.findViewById(R.id.txtSendToPlayer1);
        txtPlayer2 = (TextView)this.findViewById(R.id.txtSendToPlayer2);
        txtPlayer3 = (TextView)this.findViewById(R.id.txtSendToPlayer3);
        txtLocalPlayer = (TextView)this.findViewById(R.id.txtPlayerLocal);
        btnSendTo1 = (Button)this.findViewById(R.id.btnSendTo1);
        btnSendTo2 = (Button)this.findViewById(R.id.btnSendTo2);
        btnSendTo3 = (Button)this.findViewById(R.id.btnSendTo3);
    }

    public String getChatMsg() {
        TextView user = (TextView)this.findViewById(R.id.txtChatMsg);
        return  user.getText().toString();
    }
}
