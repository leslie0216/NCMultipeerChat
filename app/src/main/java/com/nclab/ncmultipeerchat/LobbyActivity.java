package com.nclab.ncmultipeerchat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class LobbyActivity extends Activity {

    private TextView lbMsg;
    private TextView txtPlayer1;
    private TextView txtPlayer2;
    private TextView txtPlayer3;
    private TextView txtPlayer4;
    private ImageButton btnStartGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        ImageButton btnBack = (ImageButton)this.findViewById(R.id.btnBackToSC);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToSettingActivity();
            }
        });

        btnStartGame = (ImageButton)this.findViewById(R.id.btnStartGame);
        lbMsg = (TextView)this.findViewById(R.id.lbMsg);
        txtPlayer1 = (TextView)this.findViewById(R.id.txtPlayer1);
        txtPlayer2 = (TextView)this.findViewById(R.id.txtPlayer2);
        txtPlayer3 = (TextView)this.findViewById(R.id.txtPlayer3);
        txtPlayer4 = (TextView)this.findViewById(R.id.txtPlayer4);
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

    private void backToSettingActivity() {
        Intent intent = new Intent();
        intent.setClass(LobbyActivity.this, SettingActivity.class);
        LobbyActivity.this.startActivity(intent);
        LobbyActivity.this.finish();
    }
}
