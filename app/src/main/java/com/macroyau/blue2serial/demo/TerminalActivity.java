package com.macroyau.blue2serial.demo;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.macroyau.blue2serial.BluetoothDeviceListDialog;
import com.macroyau.blue2serial.BluetoothSerial;
import com.macroyau.blue2serial.BluetoothSerialListener;
import com.macroyau.blue2serial.Phx21Status;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This is an example Bluetooth terminal application built using the Blue2Serial library.
 *
 * @author Macro Yau
 */
public class TerminalActivity extends AppCompatActivity
        implements BluetoothSerialListener, BluetoothDeviceListDialog.OnDeviceSelectedListener, OnClickListener {

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;

    private BluetoothSerial bluetoothSerial;

    private ScrollView svTerminal;
    private TextView tvTerminal,tvBattery,tv_battery_p,tv_sysinfo,tv_cot,tv_sp,tv_airp,tv_tc,tv_pa,tv_sc,tv_LPH2,tv_tp,tv_tp_p,tv_pp,tv_ppm,tv_peak;
    private EditText etSend;
    private Button pumpOn,pumpOff,ignite,logStart,logStop,FID,getCal;
    private MenuItem actionConnect, actionDisconnect;
    private LocalBroadcastManager broadcastManager;
    SetCalReceiver SCR;
    CheckEnabledReceiver CER;
    ShowCalReceiver ShowCR;
    private boolean crlf = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        // Find UI views and set listeners
        svTerminal = (ScrollView) findViewById(R.id.terminal);
        tvTerminal = (TextView) findViewById(R.id.tv_terminal);
        tvBattery = (TextView) findViewById(R.id.tv_battery);
        tv_battery_p= (TextView) findViewById(R.id.tv_battery_p);
        tv_sysinfo= (TextView) findViewById(R.id.tv_sysinfo);
        tv_cot= (TextView) findViewById(R.id.tv_cot);
        tv_sp= (TextView) findViewById(R.id.tv_sp);
        tv_airp= (TextView) findViewById(R.id.tv_airp);
        tv_tc= (TextView) findViewById(R.id.tv_tc);
        tv_pa= (TextView) findViewById(R.id.tv_pa);
        tv_sc= (TextView) findViewById(R.id.tv_sc);

        tv_tp= (TextView) findViewById(R.id.tv_tp);
        tv_tp_p= (TextView) findViewById(R.id.tv_tp_p);
        tv_pp= (TextView) findViewById(R.id.tv_pp);
        tv_ppm= (TextView) findViewById(R.id.tv_ppm);
        tv_peak=(TextView) findViewById(R.id.tv_peak);
        pumpOn = (Button) findViewById(R.id.btn_pump_on);
        pumpOff = (Button) findViewById(R.id.btn_pump_off);
        ignite = (Button) findViewById(R.id.btn_pump_ignite);
        logStart = (Button) findViewById(R.id.btn_log_start);
        logStop = (Button) findViewById(R.id.btn_log_stop);
        FID = (Button) findViewById(R.id.btn_detector_fid);
        getCal = (Button) findViewById(R.id.btn_get_cal);

        pumpOn.setOnClickListener(this);
        pumpOff.setOnClickListener(this);
        ignite.setOnClickListener(this);
        logStart.setOnClickListener(this);
        logStop.setOnClickListener(this);
        getCal.setOnClickListener(this);
        etSend = (EditText) findViewById(R.id.et_send);
        etSend.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    String send = etSend.getText().toString().trim();
                    if (send.length() > 0) {
                        bluetoothSerial.write(send, crlf);
                        etSend.setText("");
                    }
                }
                return false;
            }
        });


        //使用LocalBroadcastManager注册广播
        broadcastManager = LocalBroadcastManager.getInstance(this);
        // Create a new instance of BluetoothSerial
        bluetoothSerial = new BluetoothSerial(this, this,broadcastManager);
        /*SCR =new SetCalReceiver();
        IntentFilter setcalfilter = new IntentFilter("com.cabin.bluetooth.setcaculation");
        broadcastManager.registerReceiver(SCR,setcalfilter);*/

        ShowCR=new ShowCalReceiver();
        IntentFilter showcalfilter = new IntentFilter("com.cabin.bluetooth.showcaculation");
        broadcastManager.registerReceiver(ShowCR,showcalfilter);
        CER=new CheckEnabledReceiver();
        IntentFilter checkEnabledfilter = new IntentFilter("com.cabin.bluetooth.checkbtnenabled");
        broadcastManager.registerReceiver(CER,checkEnabledfilter);
    }
    Handler handler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    try{
                        for (int i =0;i<6;i++){
                            System.out.println("获取校准值...."+i);
                            Thread.sleep(200);
                            bluetoothSerial.GetPpmCalibration(i);
                       }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    break;
                case 2:
                    final int v=msg.arg2;
                    final int index=msg.arg1;
                   Thread t = new Thread(new Runnable(){
                        @Override
                        public void run(){
                            bluetoothSerial.GeneratePpmCalibration(v);
                           long startTime= System.currentTimeMillis();
                          while (true){
                               long currentTime=System.currentTimeMillis();
                               long differ=currentTime-startTime;
                              System.out.print("检查"+differ);
                               if (differ>5000) {
                                   System.out.print("检查UI");
                                   Intent intent=new Intent("com.cabin.bluetooth.checkbtnenabled");
                                   intent.putExtra("index",index);
                                   broadcastManager.sendBroadcast(intent);
                                   break;
                               }

                           }
                        }
                    });
                    t.start();
                    break;
            }
            super.handleMessage(msg);
        }
    };
    @Override
    protected void onStart() {
        super.onStart();

        // Check Bluetooth availability on the device and set up the Bluetooth adapter
        bluetoothSerial.setup();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Open a Bluetooth serial port and get ready to establish a connection
        if (bluetoothSerial.checkBluetooth() && bluetoothSerial.isBluetoothEnabled()) {
            if (!bluetoothSerial.isConnected()) {
                bluetoothSerial.start();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Disconnect from the remote device and close the serial port
     //   bluetoothSerial.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_terminal, menu);

        actionConnect = menu.findItem(R.id.action_connect);
        actionDisconnect = menu.findItem(R.id.action_disconnect);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_connect) {
            showDeviceListDialog();
            return true;
        } else if (id == R.id.action_disconnect) {
            bluetoothSerial.stop();
            return true;
        } else if (id == R.id.action_crlf) {
            crlf = !item.isChecked();
            item.setChecked(crlf);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void invalidateOptionsMenu() {
        if (bluetoothSerial == null)
            return;

        // Show or hide the "Connect" and "Disconnect" buttons on the app bar
        if (bluetoothSerial.isConnected()) {
            if (actionConnect != null)
                actionConnect.setVisible(false);
            if (actionDisconnect != null)
                actionDisconnect.setVisible(true);
        } else {
            if (actionConnect != null)
                actionConnect.setVisible(true);
            if (actionDisconnect != null)
                actionDisconnect.setVisible(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                // Set up Bluetooth serial port when Bluetooth adapter is turned on
                if (resultCode == Activity.RESULT_OK) {
                    bluetoothSerial.setup();
                }
                break;
        }
    }

    private void updateBluetoothState() {
        // Get the current Bluetooth state
        final int state;
        if (bluetoothSerial != null)
            state = bluetoothSerial.getState();
        else
            state = BluetoothSerial.STATE_DISCONNECTED;

        // Display the current state on the app bar as the subtitle
        String subtitle;
        switch (state) {
            case BluetoothSerial.STATE_CONNECTING:
                subtitle = getString(R.string.status_connecting);
                break;
            case BluetoothSerial.STATE_CONNECTED:
                subtitle = getString(R.string.status_connected, bluetoothSerial.getConnectedDeviceName());
                break;
            default:
                subtitle = getString(R.string.status_disconnected);
                break;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subtitle);
        }
    }

    private void showDeviceListDialog() {
        // Display dialog for selecting a remote Bluetooth device
        BluetoothDeviceListDialog dialog = new BluetoothDeviceListDialog(this);
        dialog.setOnDeviceSelectedListener(this);
        dialog.setTitle(R.string.paired_devices);
        dialog.setDevices(bluetoothSerial.getPairedDevices());
        dialog.showAddress(true);
        dialog.show();
    }

    /* Implementation of BluetoothSerialListener */

    @Override
    public void onBluetoothNotSupported() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.no_bluetooth)
                .setPositiveButton(R.string.action_quit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onBluetoothDisabled() {
        Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBluetooth, REQUEST_ENABLE_BLUETOOTH);
    }

    @Override
    public void onBluetoothDeviceDisconnected() {
        invalidateOptionsMenu();
        updateBluetoothState();
    }

    @Override
    public void onConnectingBluetoothDevice() {
        updateBluetoothState();
    }

    @Override
    public void onBluetoothDeviceConnected(String name, String address) {
        invalidateOptionsMenu();
        updateBluetoothState();
    }

    @Override
    public void onBluetoothSerialRead(String message) {
        // Print the incoming message on the terminal screen
        tvTerminal.append(getString(R.string.terminal_message_template,
                bluetoothSerial.getConnectedDeviceName(),
                message));
        svTerminal.post(scrollTerminalToBottom);
    }
    @Override
    public void onBluetoothSerialRead(Phx21Status status){
        Phx21OnDataPolled(status);
    }

    private Double peakValue=0.0;
    private void Phx21OnDataPolled(Phx21Status Status)
    {
        //Log.Info("phxdemo_BatteryVoltage11", "hello");
      try{
            float BatteryVoltage = Status.BatteryVoltage;
            float BatteryVoltagePrecent = ((BatteryVoltage - 5.5f)/3.4f)*100;
            //ppm
           String Ppm = Status.PpmStr;
            if(Ppm == "N/A")
            {
                Ppm = "0";
            }
            if(peakValue<Double.parseDouble(Ppm))
            {
                peakValue = Double.parseDouble(Ppm);
            }
            float ChamberOuterTemp = Status.ChamberOuterTemp;
            float SamplePressure = Status.SamplePressure;
            float AirPressure = Status.AirPressure;
            float TankPressure = Status.TankPressure;
            float TankPressurePrecent = ((TankPressure - 200f) / 1600f) * 100;
            float ThermoCouple = Status.ThermoCouple;
            double PicoAmps = Status.PicoAmps;
            float  SystemCurrent = Status.SystemCurrent;


            float PumpPower = Status.PumpPower;
            boolean IsSolenoidAOn = Status.IsSolenoidAOn;
            boolean IsSolenoidBOn = Status.IsSolenoidBOn;

            tvBattery.setText(String.valueOf(BatteryVoltage));
            tv_battery_p.setText(String.valueOf(BatteryVoltagePrecent)+"%");
            tv_cot.setText(String.valueOf(ChamberOuterTemp));
            tv_sp.setText(String.valueOf(SamplePressure));
            tv_airp.setText(String.valueOf(AirPressure));
            tv_tc.setText(String.valueOf(ThermoCouple));
            tv_pa.setText(String.valueOf(PicoAmps));
            tv_sc.setText(String.valueOf(SystemCurrent));
            // tv_LPH2.setText(String.valueOf(status.ThermoCouple));
            tv_tp.setText(String.valueOf(TankPressure));
            tv_tp_p.setText(String.valueOf(TankPressurePrecent)+"%");
            tv_pp.setText(String.valueOf(PumpPower));
            tv_ppm.setText(String.valueOf(Ppm));
            tv_peak.setText(String.valueOf(peakValue));
            String sysinfo="";
            if (PumpPower > 85)
            {
                sysinfo="泵已达到最大输出限制！请查看探头是否阻塞，或者需要更新探头或过滤器";
            }
            else
            {
                if(PumpPower > 85)
                {
                    sysinfo="取样空气压力较低！请查看探头是否阻塞，或者需要更新探头或过滤器";
                }
                else
                {
                    if(BatteryVoltage<10)
                    {
                        sysinfo="电池电压低，请充电";
                    }
                    else
                    {
                        if( ChamberOuterTemp > 500)
                        {
                            sysinfo="燃烧室内部温度过高";
                        }
                        else
                        {
                            if(TankPressure<400)
                            {
                                sysinfo="氢气量即将用完，请及时充气";
                            }
                        }
                    }
                }
            }
          tv_sysinfo.setTextColor(Color.rgb(221, 82, 70));
          tv_sysinfo.setText(sysinfo);
      }
      catch(Exception e){

      }

    }
    @Override
    public void onBluetoothSerialWrite(String message) {
        // Print the outgoing message on the terminal screen
        tvTerminal.append(getString(R.string.terminal_message_template,
                bluetoothSerial.getLocalAdapterName(),
                message));
        svTerminal.post(scrollTerminalToBottom);
    }

    /* Implementation of BluetoothDeviceListDialog.OnDeviceSelectedListener */

    @Override
    public void onBluetoothDeviceSelected(BluetoothDevice device) {
        // Connect to the selected remote Bluetooth device
        bluetoothSerial.connect(device);
    }

    /* End of the implementation of listeners */

    private final Runnable scrollTerminalToBottom = new Runnable() {
        @Override
        public void run() {
            // Scroll the terminal screen to the bottom
            svTerminal.fullScroll(ScrollView.FOCUS_DOWN);
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_pump_on:
                bluetoothSerial.write("pump on",true);
                break;
            case R.id.btn_pump_off:
                bluetoothSerial.write("pump off",true);
                break;
            case R.id.btn_pump_ignite:
                // bluetoothSerial.write("ignite",true);
                bluetoothSerial.Ignite(true,0);
                break;
            case R.id.btn_log_start:
                //bluetoothSerial.ReadDataExtended();
                bluetoothSerial.startLog();
               // bluetoothSerial.write("log start",true);
                break;
            case R.id.btn_log_stop:
                bluetoothSerial.write("log stop",true);
                break;
            case R.id.btn_detector_fid:
                bluetoothSerial.write("detector fid",true);
                break;
            case R.id.btn_get_cal:
                if (bluetoothSerial.isDeviceIgnited()){
                    showCalDialog(null);
                }else{
                    Toast.makeText(this,"仪器没点火无法进行校准操作！！",Toast.LENGTH_LONG).show();
                }
                break;

        }
    }
    private void multiThreadgetCal(){
      //  bluetoothSerial.stopTimer();
        MyRejectedExecutionHandler handler=new MyRejectedExecutionHandler();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                1, 5, 30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(6), sysThreadFactory,handler);
        for (int i =0;i<6;i++){
            final int iValue=i;
            Runnable runnable=new Runnable(){
                @Override
                public void run(){
                    SystemClock.sleep(200);
                    bluetoothSerial.GetPpmCalibration(iValue);
                    Log.i("ansen","当前线程id:"+android.os.Process.myTid()+" iValue:"+iValue);
                }
            };
            threadPoolExecutor.execute(runnable);
        }
    }
    private void sendMsgtoGetCal(){
       // bluetoothSerial.stopTimer();
        Message message = new Message();
        message.what = 1;
        handler.sendMessage(message);
    }
    private Dialog mDialog;
    private EditText et_cal_1,et_cal_2,et_cal_3,et_cal_4,et_cal_5,et_cal_6;
    private Button btn_set_1,btn_set_2,btn_set_3,btn_set_4,btn_set_5,btn_set_6,btn_set_cal,btn_get_cal,btn_close;
    public void showCalDialog(View view){
        //1.创建一个Dialog对象，如果是AlertDialog对象的话，弹出的自定义布局四周会有一些阴影，效果不好
        mDialog = new Dialog(this);
        //去除标题栏
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //2.填充布局
        LayoutInflater inflater = LayoutInflater.from(this);
        View  dialogView     = inflater.inflate(R.layout.activity_cal, null);
        //将自定义布局设置进去
        mDialog.setContentView(dialogView);
        //3.设置指定的宽高,如果不设置的话，弹出的对话框可能不会显示全整个布局，当然在布局中写死宽高也可以
        WindowManager.LayoutParams lp     = new WindowManager.LayoutParams();
        Window   window = mDialog.getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        //注意要在Dialog show之后，再将宽高属性设置进去，才有效果
        mDialog.show();
        window.setAttributes(lp);

        //设置点击其它地方不让消失弹窗
        mDialog.setCancelable(false);
        initDialogView(dialogView);
        initDialogListener();
        sendMsgtoGetCal();
      // multiThreadgetCal();
    }

    private void initDialogView(View view) {
        et_cal_1 = (EditText) view.findViewById(R.id.cal_1);
        et_cal_2 = (EditText) view.findViewById(R.id.cal_2);
        et_cal_3 = (EditText) view.findViewById(R.id.cal_3);
        et_cal_4 = (EditText) view.findViewById(R.id.cal_4);
        et_cal_5 = (EditText) view.findViewById(R.id.cal_5);
        et_cal_6 = (EditText) view.findViewById(R.id.cal_6);
        btn_set_1 = (Button) view.findViewById(R.id.btn_set_1);
        btn_set_2 = (Button) view.findViewById(R.id.btn_set_2);
        btn_set_3 = (Button) view.findViewById(R.id.btn_set_3);
        btn_set_4 = (Button) view.findViewById(R.id.btn_set_4);
        btn_set_5 = (Button) view.findViewById(R.id.btn_set_5);
        btn_set_6 = (Button) view.findViewById(R.id.btn_set_6);
        btn_set_cal = (Button) view.findViewById(R.id.btn_set_cal);
        btn_get_cal = (Button) view.findViewById(R.id.btn_get_cal);
        btn_close= (Button) view.findViewById(R.id.btn_close);
    }

    private final void checkEnabled(int index){
        Button[] BtnList={btn_set_1,btn_set_2,btn_set_3,btn_set_4,btn_set_5,btn_set_6};
        EditText[] ETList={et_cal_1,et_cal_2,et_cal_3,et_cal_4,et_cal_5,et_cal_6};
        int nextIndex=index+1;
        if (nextIndex<=BtnList.length){
            BtnList[nextIndex-1].setText("校准");
            BtnList[nextIndex].setEnabled(true);
            ETList[nextIndex].requestFocus();
        }
    }

    private void commonSetCal(EditText et,Button btn,int index){
        int v=Integer.valueOf(et.getText().toString());
        Message message = new Message();
        message.what = 2;
        message.arg1=index;
        message.arg2=v;
        handler.sendMessage(message);
    }
    private static final ThreadFactory sysThreadFactory =Executors.defaultThreadFactory();
    private void initDialogListener() {
        btn_set_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_set_1.setEnabled(false);
                btn_set_1.setText("校准中");
            MyRejectedExecutionHandler handler=new MyRejectedExecutionHandler();
            //只能设置成1个核心线程，否则仪器处理不过来
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                    1, 5, 30, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(6), sysThreadFactory,handler);
                Runnable runnable=new Runnable(){
                    @Override
                    public void run(){
                        for (int j = 0; j < 3; j++)
                        {
                            for (int i = 0; i < 6; i++)
                            {
                                SystemClock.sleep(200);
                                bluetoothSerial.SetPpmCalibration(i, 0, 0, (char)0, false);
                            }
                        }
                        commonSetCal(et_cal_1,btn_set_1,0);
                    }
                };
                threadPoolExecutor.execute(runnable);
            }
        });
        btn_set_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_set_2.setEnabled(false);
                btn_set_2.setText("校准中");
                commonSetCal(et_cal_2,btn_set_2,1);
            }
        });
        btn_set_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_set_3.setEnabled(false);
                btn_set_3.setText("校准中");
                commonSetCal(et_cal_3,btn_set_3,2);
            }
        });
        btn_set_4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_set_4.setEnabled(false);
                btn_set_4.setText("校准中");
                commonSetCal(et_cal_4,btn_set_4,3);
            }
        });
        btn_set_5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_set_5.setEnabled(false);
                btn_set_5.setText("校准中");
                commonSetCal(et_cal_5,btn_set_5,4);
            }
        });
        btn_set_6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_set_6.setEnabled(false);
                btn_set_6.setText("校准中");
                commonSetCal(et_cal_6,btn_set_6,5);
            }
        });

        btn_get_cal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("获取校准参数","------------------");
                multiThreadgetCal();
            }
        });

        btn_set_cal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // bluetoothSerial.stopTimer();
                Log.i("设置校准参数","------------------");
                int v1=Integer.valueOf(et_cal_1.getText().toString());
                int v2=Integer.valueOf(et_cal_2.getText().toString());
                int v3=Integer.valueOf(et_cal_3.getText().toString());
                int v4=Integer.valueOf(et_cal_4.getText().toString());
                int v5=Integer.valueOf(et_cal_5.getText().toString());
                int v6=Integer.valueOf(et_cal_6.getText().toString());
                final int[] values={v1,v2,v3,v4,v5,v6};
                MyRejectedExecutionHandler handler=new MyRejectedExecutionHandler();
                //只能设置成1个核心线程，否则仪器处理不过来
                ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                        1, 5, 30, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>(6), sysThreadFactory,handler);
                for (int i =0;i<values.length;i++){
                    final int iValue=i;
                    Runnable runnable=new Runnable(){
                        @Override
                        public void run(){
                            SystemClock.sleep(200);
                            bluetoothSerial.SetPpmCalibration(getApplicationContext(),iValue,values[iValue]);
                            Log.i("ansen","当前线程id:"+android.os.Process.myTid()+" iValue:"+iValue);
                        }
                    };
                    threadPoolExecutor.execute(runnable);
                }
            }
        });
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        broadcastManager.unregisterReceiver(SCR);
    }
    public static class MyRejectedExecutionHandler implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("任务被拒绝");
        }
    }
    private class CheckEnabledReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            try {
                int index=intent.getIntExtra("index",-1);
                if (index>-1) checkEnabled(index);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    private class SetCalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            try {
                int v1=intent.getIntExtra("v1",0);
                int v2=intent.getIntExtra("v2",0);
                int v3=intent.getIntExtra("v3",0);
                int v4=intent.getIntExtra("v4",0);
                int v5=intent.getIntExtra("v5",0);
                int v6=intent.getIntExtra("v6",0);
                bluetoothSerial.SetPpmCalibration(context,0,v1);
                bluetoothSerial.SetPpmCalibration(context,1,v2);
                bluetoothSerial.SetPpmCalibration(context,2,v3);
                bluetoothSerial.SetPpmCalibration(context,3,v4);
                bluetoothSerial.SetPpmCalibration(context,4,v5);
                bluetoothSerial.SetPpmCalibration(context,5,v6);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class ShowCalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            try {
//                PpmCalibrationInfo ppmCalibrationInfo=(PpmCalibrationInfo)intent.getSerializableExtra("calinfo");
                List<Integer> ppms =( List<Integer>)intent.getSerializableExtra("calinfo");
                EditText[] etcs={et_cal_1,et_cal_2,et_cal_3,et_cal_4,et_cal_5,et_cal_6};
               // int index=ppmCalibrationInfo.getIndex();
                for (int i =0;i<etcs.length;i++){
                    etcs[i].setText(""+ppms.get(i));
                }
               // int value=ppmCalibrationInfo.getPpm();
              //  etcs[index].setText(""+value);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
