package com.nclab.ncmultipeerchat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.test.suitebuilder.TestMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.nclab.ncmultipeerconnectivity.NCMCSession;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

public class PackageSizeActivity extends Activity {
    public static final String TAG = "PackageSizeActivity";
    public static final int MaxPingCount = 30;
    private Button m_pintBtn;
    private TextView m_txtCurrentPing;
    private TextView m_txtReceivedCount;
    private TextView m_txtTotalCount;

    private boolean m_isPing;
    private boolean m_isPingEnabled;
    private boolean m_isLogEnabled = true;

    private int m_messageSize;
    private int m_totalCount;
    private int m_receivedCount;
    private Hashtable<Integer, PingInfo> m_pingDict = null;

    private NetworkLogger m_logger;

    private final IntentFilter m_intentFilter = new IntentFilter();

    private BroadcastReceiver m_broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case MultiplayerController.BLE_BROADCAST_RECEIVE_MESSAGE: {
                    Bundle bundle = intent.getExtras();
                    if (bundle.containsKey(MultiplayerController.BLE_BROADCAST_RECEIVE_MESSAGE_FROM_NAME) &&
                            bundle.containsKey(MultiplayerController.BLE_BROADCAST_RECEIVE_MESSAGE_DATA) &&
                            bundle.containsKey(MultiplayerController.BLE_BROADCAST_RECEIVE_MESSAGE_TIME)) {
                        String fromName = bundle.getString(MultiplayerController.BLE_BROADCAST_RECEIVE_MESSAGE_FROM_NAME);
                        long receiveTime = bundle.getLong(MultiplayerController.BLE_BROADCAST_RECEIVE_MESSAGE_TIME);
                        byte[] data = bundle.getByteArray(MultiplayerController.BLE_BROADCAST_RECEIVE_MESSAGE_DATA);
                        handleMessage(data, fromName, receiveTime);
                    }
                    break;
                }
                case MultiplayerController.BLE_BROADCAST_UPDATE_PLAYERLIST: {
                    updateStatus();
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_package_size);

        String isHost = MultiplayerController.getInstance().isHost() ? "Yes" : "No";
        ((TextView)findViewById(R.id.lbpsIsHost)).setText(isHost);

        m_isPing = false;

        m_pintBtn = (Button)findViewById(R.id.btnpsStart);

        m_pintBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_isPing) {
                    m_isPing = false;
                    m_pintBtn.setText(getResources().getString(R.string.start));
                    stopPing();
                } else {
                    m_isPing = true;
                    m_pintBtn.setText(getResources().getString(R.string.stop));
                    startPing();
                }
            }
        });

        m_messageSize = 0;

        updatePackageSize();

        m_txtCurrentPing = ((TextView)findViewById(R.id.lbpsCurrentPing));
        m_txtReceivedCount = ((TextView)findViewById(R.id.lbpsReceivedCount));
        m_txtTotalCount = ((TextView)findViewById(R.id.lbpsTotalCount));

        updateStatus();


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
        updateStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MultiplayerController.getInstance().setContext(null);
        unregisterReceiver(m_broadcastReceiver);
    }

    private void updateStatus() {
        int connectedCnt = MultiplayerController.getInstance().getCurrentSession().getConnectedPeers().size();
        ((TextView)findViewById(R.id.lbpsNetworkStatus)).setText(String.valueOf(connectedCnt));
    }

    private void updatePackageSize() {
        ((TextView)findViewById(R.id.lbpsPackageSize)).setText(String.valueOf(getPackageSize()));
    }

    private int getPackageSize() {
        int size = m_messageSize + 10;
        if (m_messageSize >= 128) {
            size += 1;
        }

        if (m_messageSize >= 499) {
            size += 2;
        }

        return size;
    }

    private void backToSettingActivity() {
        MultiplayerController.getInstance().disconnect();

        Intent intent = new Intent();
        intent.setClass(PackageSizeActivity.this, SettingActivity.class);
        PackageSizeActivity.this.startActivity(intent);
        PackageSizeActivity.this.finish();
    }

    private void handleMessage(byte[] data, String from, long receiveTime) {
        if (data != null && data.length != 0) {
            try {
                Message.PingMessage message = Message.PingMessage.parseFrom(data);

                if (message.getMessageType() == Message.PingMessage.MsgType.RESPONSE) {
                    int token = message.getToken();

                    PingInfo info = m_pingDict.get(token);
                    if (info.m_totalCount == info.m_currentCount) {
                        Log.d(TAG, "handleMessage: token over received");
                        return;
                    }

                    double timeInterval = ((double)receiveTime) / 1000000.0 - ((double)info.m_startTime)/1000000.0 - message.getResponseTime();
                    Log.d(TAG, "handleMessage token : " + token + ", timeInterval : " + timeInterval);

                    info.m_timeIntervals.add(timeInterval);
                    info.m_currentCount += 1;
                    m_pingDict.put(token, info);
                    m_receivedCount += 1;

                    if (m_isPing) {
                        updatePackageSize();
                        m_txtCurrentPing.setText(String.valueOf(timeInterval));
                        m_txtReceivedCount.setText(String.valueOf(m_receivedCount));
                        m_txtTotalCount.setText(String.valueOf(m_totalCount));

                        if (info.m_totalCount == info.m_currentCount) {
                            if (m_totalCount >= MaxPingCount && m_receivedCount >= MaxPingCount) {
                                m_isPingEnabled = false;
                                calculateResult();
                            } else {
                                doPing();
                            }
                        }
                    }

                } else if (message.getMessageType() == Message.PingMessage.MsgType.PING) {
                    Message.PingMessage.Builder mb = Message.PingMessage.newBuilder();
                    mb.setToken(message.getToken());
                    mb.setMessageType(Message.PingMessage.MsgType.RESPONSE);
                    mb.setIsReliable(message.getIsReliable());
                    mb.setMessage("");

                    double responseTime = ((double)System.nanoTime())/1000000.0 - ((double)receiveTime) / 1000000.0; // to ms
                    mb.setResponseTime(responseTime);

                    Message.PingMessage msg = mb.build();
                    Log.d(TAG, "handleMessage: send response with token : " + msg.getToken() + " response time : " + responseTime);
                    int mode = message.getIsReliable() ? NCMCSession.NCMCSessionSendDataReliable : NCMCSession.NCMCSessionSendDataUnreliable;
                    MultiplayerController.getInstance().sendDataToPeer(msg.toByteArray(), from, mode);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    private void startPing() {
        m_isPing = true;

        if (m_pingDict == null) {
            m_pingDict = new Hashtable<>();
        } else {
            m_pingDict.clear();
        }

        m_totalCount = 0;
        m_receivedCount = 0;
        m_isPingEnabled = true;
        m_messageSize = 1;

        if (m_isLogEnabled) {
            m_logger = new NetworkLogger(this, "");
        }

        MultiplayerController.getInstance().enableHighTraffic();

        doPing();
    }

    private void stopPing() {
        m_isPing = false;
        m_isPingEnabled = false;

        if (m_isLogEnabled && m_logger != null) {
            m_logger.flush();
            m_logger.close();
            m_logger = null;
        }

        MultiplayerController.getInstance().disableHighTraffic();
    }

    private void doPing() {
        Message.PingMessage.Builder mb = Message.PingMessage.newBuilder();

        String tmp = "";
        for (int i=0; i<m_messageSize; ++i) {
            tmp += "a";
        }

        int token = m_totalCount+1;

        mb.setMessage(tmp);

        mb.setToken(token);
        mb.setMessageType(Message.PingMessage.MsgType.PING);
        mb.setResponseTime(0.0);
        mb.setIsReliable(false);

        Message.PingMessage msg = mb.build();

        long startTime = System.nanoTime();
        Log.d(TAG, "doPing: message size: " + m_messageSize + ", total size: " + msg.toByteArray().length + ", packageSize: " + getPackageSize());
        MultiplayerController.getInstance().sendDataToAllPeer(msg.toByteArray(), NCMCSession.NCMCSessionSendDataUnreliable);

        PingInfo info = new PingInfo();
        info.m_startTime = startTime;
        info.m_token = token;
        info.m_totalCount = MultiplayerController.getInstance().getCurrentSession().getConnectedPeers().size();
        info.m_currentCount = 0;
        //info.m_number = m_totalCount + 1;
        m_totalCount += info.m_totalCount;

        m_pingDict.put(token, info);

        if (m_totalCount >= MaxPingCount) {
            m_isPingEnabled = false;
        }
    }

    private void calculateResult() {
        double totalTime = 0.0;
        double min = 10000.0;
        double max = 0.0;
        List<Double> allTimes = new ArrayList<>();
        Enumeration<PingInfo> values = m_pingDict.elements();
        while (values.hasMoreElements()) {
            PingInfo info = values.nextElement();

            for (Double time : info.m_timeIntervals) {
                totalTime += time;
                if (time > max) {
                    max = time;
                }
                if (time < min) {
                    min = time;
                }
                allTimes.add(time);
            }
        }

        double average = totalTime / allTimes.size();

        double sumOfSquaredDifferences = 0.0;
        for (Double time : allTimes) {
            double difference = time - average;
            sumOfSquaredDifferences += difference * difference;
        }
        double std = Math.sqrt(sumOfSquaredDifferences / allTimes.size());

        if (m_isLogEnabled && m_logger != null) {
            m_logger.write(getPackageSize() + "," + average + "," + std, true);
        }

        if (m_isPing) {
            m_pingDict.clear();
            m_receivedCount = 0;
            m_totalCount = 0;
            m_messageSize += 1;
            m_isPingEnabled = true;
            doPing();
        }
    }
}
