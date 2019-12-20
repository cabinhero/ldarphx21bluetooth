package com.macroyau.blue2serial.demo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.macroyau.blue2serial.domain.PpmCalibrationInfo;

public class CalActivity extends Activity implements View.OnClickListener {
    private EditText et_cal_1,et_cal_2,et_cal_3,et_cal_4,et_cal_5,et_cal_6;
    private Button btn_set_cal,btn_get_cal;
    private GetCalReceiver GCR;
    private LocalBroadcastManager broadcastManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cal);
        initView();
        broadcastManager = LocalBroadcastManager.getInstance(this);
        GCR=new GetCalReceiver();
        IntentFilter getcalfilter = new IntentFilter("com.cabin.bluetooth.getcaculation");
        broadcastManager.registerReceiver(GCR,getcalfilter);
    }

    private void initView(){
        et_cal_1 = (EditText) findViewById(R.id.cal_1);
        et_cal_2 = (EditText) findViewById(R.id.cal_2);
        et_cal_3 = (EditText) findViewById(R.id.cal_3);
        et_cal_4 = (EditText) findViewById(R.id.cal_4);
        et_cal_5 = (EditText) findViewById(R.id.cal_5);
        et_cal_6 = (EditText) findViewById(R.id.cal_6);
        btn_set_cal = (Button) findViewById(R.id.btn_set_cal);
        btn_get_cal = (Button) findViewById(R.id.btn_get_cal);
        btn_get_cal.setOnClickListener(this);
        btn_set_cal.setOnClickListener(this);
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        broadcastManager.unregisterReceiver(GCR);
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_set_cal:
                Intent intent=new Intent("com.cabin.bluetooth.setcaculation");
                intent.putExtra("v1",Integer.valueOf(et_cal_1.getText().toString()));
                intent.putExtra("v2",Integer.valueOf(et_cal_2.getText().toString()));
                intent.putExtra("v3",Integer.valueOf(et_cal_3.getText().toString()));
                intent.putExtra("v4",Integer.valueOf(et_cal_4.getText().toString()));
                intent.putExtra("v5",Integer.valueOf(et_cal_5.getText().toString()));
                intent.putExtra("v6",Integer.valueOf(et_cal_6.getText().toString()));
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//                sendBroadcast(intent);
                break;
            case R.id.btn_get_cal:
                Intent intent2=new Intent("com.cabin.bluetooth.getcaculation");
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent2);
                break;
        }

    }

    private class GetCalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            try {
                PpmCalibrationInfo ppmCalibrationInfo=(PpmCalibrationInfo)intent.getSerializableExtra("calinfo");

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
