package com.macroyau.blue2serial;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.macroyau.blue2serial.domain.PpmCalibrationInfo;
import com.macroyau.blue2serial.structure.DEFAULT_RESPONSE_EXTENDED;
import com.macroyau.blue2serial.structure.FIDM_STATUS_EXTENDED;
import com.macroyau.blue2serial.structure.GetCalibrationResponse;
import com.macroyau.blue2serial.structure.IntegrationControlParams;
import com.macroyau.blue2serial.structure.PumpAux1ControlParams;
import com.macroyau.blue2serial.structure.PumpClosedLoop;
import com.macroyau.blue2serial.structure.SetSamplingParams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.macroyau.blue2serial.BluetoothSerial.ReadDataExtended;
import static com.macroyau.blue2serial.BluetoothSerial.TransmitSerialCmd;
import static com.macroyau.blue2serial.OptionUtils.All;
import static com.macroyau.blue2serial.OptionUtils.BytesToDword;
import static com.macroyau.blue2serial.OptionUtils.BytesToWord;
import static com.macroyau.blue2serial.OptionUtils.ConvertKelvinToFahrenheit;
import static com.macroyau.blue2serial.OptionUtils.DwordToByte0;
import static com.macroyau.blue2serial.OptionUtils.DwordToByte1;
import static com.macroyau.blue2serial.OptionUtils.getAvg;
import static com.macroyau.blue2serial.StaticCode.CMD_FIDM_AUTO_IGNITION_SEQUENCE;
import static com.macroyau.blue2serial.StaticCode.CMD_FIDM_GET_PPM_CALIBRATION;
import static com.macroyau.blue2serial.StaticCode.CMD_FIDM_INTEGRATION_CONTROL;
import static com.macroyau.blue2serial.StaticCode.CMD_FIDM_PUMP_AUX_1_CONTROL;
import static com.macroyau.blue2serial.StaticCode.CMD_FIDM_READ_DATA_EXTENDED;
import static com.macroyau.blue2serial.StaticCode.CMD_FIDM_SET_PUMPA_CLOSED_LOOP;
import static com.macroyau.blue2serial.StaticCode.CMD_FIDM_SET_SAMPLING_PARAMETERS;
import static com.macroyau.blue2serial.StaticCode.RANGE_MODE_0_LO;
import static com.macroyau.blue2serial.StaticCode.RANGE_MODE_3_MAX;
import static com.macroyau.blue2serial.StaticCode.STATE_WAITING_FOR_SYNC_CODE;
import static com.macroyau.blue2serial.StaticCode.STATUS_PUMP_A_ON;
import static com.macroyau.blue2serial.StaticCode.STATUS_SOLENOID_A_ON;
import static com.macroyau.blue2serial.StaticCode.STATUS_SOLENOID_B_ON;
import static com.macroyau.blue2serial.StaticCode.SYNC_CODE_RES;

/**
 * Encapsulated service class for implementing the Bluetooth Serial Port Profile (SPP).
 *
 * @author Macro Yau
 */
public class SPPService {

    private static final String TAG = "SPPService";

    private static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    public static  Phx21Status CurrentStatus;
    private LocalBroadcastManager mbroadcastManager;
    public SPPService(Handler handler,LocalBroadcastManager broadcastManager) {
        mState = BluetoothSerial.STATE_DISCONNECTED;
        mHandler = handler;
        mbroadcastManager=broadcastManager;
    }

    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);

        mState = state;
        mHandler.obtainMessage(BluetoothSerial.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void start() {
        Log.d(TAG, "start()");

        resetThreads();
        setState(BluetoothSerial.STATE_DISCONNECTED);
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect(" + device + ")");

        if (mState == BluetoothSerial.STATE_CONNECTING) {
            resetConnectThread();
        }

        if (mState == BluetoothSerial.STATE_CONNECTED) {
            resetConnectedThread();
        }

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(BluetoothSerial.STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "Connected to " + device + "!");

        resetThreads();
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        Message msg = mHandler.obtainMessage(BluetoothSerial.MESSAGE_DEVICE_INFO);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothSerial.KEY_DEVICE_NAME, device.getName());
        bundle.putString(BluetoothSerial.KEY_DEVICE_ADDRESS, device.getAddress());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(BluetoothSerial.STATE_CONNECTED);
    }

    public synchronized void stop() {
        Log.d(TAG, "stop()");

        resetThreads();
        setState(BluetoothSerial.STATE_DISCONNECTED);
    }

    public void write(byte[] data) {
        ConnectedThread t;
        synchronized (this) {
            if (mState == BluetoothSerial.STATE_CONNECTED)
                t = mConnectedThread;
            else
                return;
        }
        t.write(data);
    }

    private synchronized void resetThreads() {
        resetConnectThread();
        resetConnectedThread();
    }

    private synchronized void resetConnectThread() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
    }

    private synchronized void resetConnectedThread() {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    private void reconnect() {
        SPPService.this.start();
    }

    private class ConnectThread extends Thread {

        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;

        public ConnectThread(BluetoothDevice device) {
            Log.d(TAG, "ConnectThread(" + device + ")");
            mDevice = device;
            BluetoothSocket tempSocket = null;
            try {
                tempSocket = device.createRfcommSocketToServiceRecord(UUID_SPP);
            } catch (IOException e1) {
                Log.e(TAG, "Failed to create a secure socket!");
                try {
                    tempSocket = device.createInsecureRfcommSocketToServiceRecord(UUID_SPP);
                } catch (IOException e2) {
                    Log.e(TAG, "Failed to create an insecure socket!");
                }
            }
            mSocket = tempSocket;
        }

        public void run() {
            try {
                mSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "Failed to connect to the socket!");
                cancel();
                reconnect(); // Connection failed
                return;
            }

            synchronized (SPPService.this) {
                mConnectThread = null;
            }

            connected(mSocket, mDevice);
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Unable to close the socket!");
            }
        }

    }

    private class ConnectedThread extends Thread {

        private final BluetoothSocket mSocket;
        private final InputStream mInputStream;
        private final OutputStream mOutputStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread()");

            mSocket = socket;
            InputStream tempInputStream = null;
            OutputStream tempOutputStream = null;


            try {
                tempInputStream = socket.getInputStream();
                tempOutputStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "I/O streams cannot be created from the socket!");
            }

            mInputStream = tempInputStream;
            mOutputStream = tempOutputStream;
        }
        //传统的读数据方法
        public void run_old() {
            byte[] data = new byte[1024];
            int length;
            Log.i("读取数据","。。。");
            while (true) {
                try {
                    length = mInputStream.read(data);
                    byte[] read = new byte[length];
                    System.arraycopy(data, 0, read, 0, length);
                    Log.i("读取的数据",read.toString());
                    mHandler.obtainMessage(BluetoothSerial.MESSAGE_READ, length, -1, read).sendToTarget();
                } catch (IOException e) {
                    Log.i("读取的数据异常","。。。。");
                    reconnect(); // Connection lost
                    SPPService.this.start();
                    break;
                }
            }
        }
        int calcount=0;//校准读取次数
        boolean newread=true;
        //phx21读数据方法
        public void run() {
            byte[] data = new byte[1024];
            int length;
            int dataCount =0;//数据长度
            byte dataCmd=0;//反馈的命令
            byte currentState = STATE_WAITING_FOR_SYNC_CODE;
            boolean newResponse=false;
            byte[] readData=null;
            int readCount=0;
            int index=0;//目标位置
            byte currentCmd=0;
            while (true) {
                try {
                    length = mInputStream.read(data);
                    /*byte[] read = new byte[length];
                    System.arraycopy(data, 0, read, 0, length);
                    printReceiveData(read,"raw数据");*/
                       //发送点火命令、读数据命令后，初始化数据
                    if (!newResponse&&data[0]==SYNC_CODE_RES&&(data[2]==CMD_FIDM_AUTO_IGNITION_SEQUENCE
                            ||data[2]==CMD_FIDM_READ_DATA_EXTENDED//0x25 读取ppm数值
                            ||data[2]==CMD_FIDM_GET_PPM_CALIBRATION //0x23 读取校准
                        )){
                        dataCount=data[1];
                        readData=new byte[dataCount];
                        newResponse=true;
                        currentCmd=data[2];
                        if (newread&&data[2]==CMD_FIDM_GET_PPM_CALIBRATION){ //只在calcount=0时进行初始化
                            initPpmCaliInfo();
                        }
                    }
                    if (readData!=null) {
                        if (readCount+length>dataCount) length=dataCount-readCount;
                        System.arraycopy(data, 0, readData, index, length);
                        index+=length;
                        readCount+=length;
                      if (readCount==dataCount){
                            newResponse=false;
                            dataCount=0;
                            readCount=0;
                            index=0;
                          // printReceiveData(readData,currentCmd+"组装后数据");
                            try
                            {
                                switch(currentCmd){
                                    case 0x25 : //读取ppm数值
                                        Phx21Status Rsp = parseReadData(readData);
                                        if (Rsp!=null){
                                            CurrentStatus=Rsp;
                                           mHandler.obtainMessage(BluetoothSerial.MESSAGE_READ_STR, length, -1, Rsp).sendToTarget();
                                        }
                                        break;
                                    case 0x23 ://读取校准
                                        GetCalibrationResponse Grsp=ReceiveCmdResponse_getCal(readData);
                                        if (Grsp!=null){
                                        PpmCalibrationInfo ppmCalibrationInfo=new PpmCalibrationInfo();
                                        ppmCalibrationInfo.setIndex(Grsp.index_number);
                                        ppmCalibrationInfo.setPpm((int)(0.1 * Grsp.ppm_tenths));
                                        ppmCalibrationInfo.setFidCurrent(Grsp.fid_current_tPa);
                                        ppmCalibrationInfo.setH2Pressure(Grsp.H2_pressure_hPSI / 100.0f);
                                        ppmCalibrationInfo.setValid(Grsp.valid > 0);
                                            System.out.println("校准值...."+ppmCalibrationInfo.getIndex()+"-------"+ppmCalibrationInfo.getPpm());
                                        processCalResult(ppmCalibrationInfo);
                                        }
                                        break;
                                }
                            }
                            catch (Exception ex){
                                ex.printStackTrace();
                                readData=null;
                            }
                            readData=null;
                            currentCmd=0;
                        }
                    }


                  //  mHandler.obtainMessage(BluetoothSerial.MESSAGE_READ, length, -1, read).sendToTarget();
                } catch (IOException e) {
                    Log.i("读取的数据异常","。。。。");
                    reconnect(); // Connection lost
                    SPPService.this.start();
                    break;
                }
            }
        }
        List<PpmCalibrationInfo> savedPpms ;
        List<Integer> ppms ;
        private void initPpmCaliInfo(){
            newread=false;
            savedPpms= new ArrayList<PpmCalibrationInfo>();
            ppms = new ArrayList<Integer>();
            for (int i = 0; i < 6; i++)
            {
                ppms.add(0);
            }
        }

        private void destroyPpmCaliInfo(){
            newread=true;
            savedPpms=null;
            ppms=null;
            calcount=0;
        }

        private void processCalResult( PpmCalibrationInfo ppmCalibrationInfo){
            calcount++;//已读取个数计数
            if (ppmCalibrationInfo.isValid()) savedPpms.add(ppmCalibrationInfo);

            if(calcount==6){ //如果已读完就发事件
                Collections.sort(savedPpms,new Comparator<PpmCalibrationInfo>() {
                    @Override
                    public int compare(PpmCalibrationInfo o1, PpmCalibrationInfo o2) {
                        return o1.getPpm()-o2.getPpm();
                    }} );

                for (int i = 0; i < savedPpms.size(); i++)
                {
                    PpmCalibrationInfo ppminfo=savedPpms.get(i);
                    ppms.set(i,ppminfo.getPpm());

                }

                Intent intent=new Intent("com.cabin.bluetooth.showcaculation");
                intent.putExtra("calinfo",(Serializable)ppms);
                mbroadcastManager.sendBroadcast(intent);
                Log.i("ppmCalibrationInfo---","index:"+ppmCalibrationInfo.getIndex()+"   ppm:"+ppmCalibrationInfo.getPpm());

                destroyPpmCaliInfo();

            }

        }
        private void printReceiveData(byte[] readData,String msg){
             String ttt="";
            for (byte ab : readData){
                ttt+=ab;
                ttt+=",";
            }
            Log.i(msg,ttt);
        }
        private  GetCalibrationResponse ReceiveCmdResponse_getCal(byte[] bytes)
        {

//            byte[] bytes =  new byte[]{ -91,81,37,-44,-59,
//                    12,0,-111,14,72,
//                    27,-29,11,-77,0,
//                    -119,2,-92,4,0,
//                    112,17,1,0,124,
//                    2,0,0,-16,-2,
//                    -1,-1,0,0,0,
//                    0,67,2,67,0,
//                    25,-61,0,0,-5,-1,24,-4,-112,2,
//                    40,-61,1,0,-6,-1,24,-4,-118,2,
//                    55,-61,-2,-1,-4,-1,52,-4,-102,2,
//                    0,0,0,0,0,0,0,0,0,0,
//                    -114};

            try
            {
                GetCalibrationResponse rsp = new GetCalibrationResponse();
                rsp.unpack(bytes);
               // System.out.println(rsp.nCmdID);
                return rsp;
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }

            return null;
        }

        public  void TurnOffPump()
        {
            SetPumpACtrlLoop(false, 0);
            ControlPumpAux1((byte)0, 0, (byte)0);
        }
        private  void ControlPumpAux1(byte nId, int nPowerLevelTenthsPercent, byte nKickStartDurationSec)
        {
            PumpAux1ControlParams pCmd = new PumpAux1ControlParams();
            byte nCmd = CMD_FIDM_PUMP_AUX_1_CONTROL;
            byte[]   bytes = pCmd.pack();
            byte nLength = (byte)bytes.length;
            pCmd.nID = nId;
            pCmd.nPowerTenthsPercent0 = DwordToByte0(nPowerLevelTenthsPercent);
            pCmd.nPowerTenthsPercent1 = DwordToByte1(nPowerLevelTenthsPercent);
            pCmd.nKickStartDurationSec = nKickStartDurationSec;
            //lock (obj)
            {
                // WriteToPhxLog("ControlPumpAux1");
                TransmitSerialCmd(nCmd, bytes, nLength, nLength, true);
            }

        }
        private  void SetPumpACtrlLoop(boolean enable, long target)
        {


            byte nCmd = CMD_FIDM_SET_PUMPA_CLOSED_LOOP;
            PumpClosedLoop Cmd = new PumpClosedLoop();
            byte[]   bytes = Cmd.pack();
            byte nLength = (byte)bytes.length;


            Cmd.enable = enable ? (byte)1 : (byte)0;
            Cmd.target_hPSI = (short)target;

            // lock (obj)
            {
                //WriteToPhxLog("SetPumpACtrlLoop");
                TransmitSerialCmd(nCmd, bytes, nLength, nLength, true);
            }

        }

        private  void SetSamplingParameters(byte nFIDMRange)
        {
            SetSamplingParams pCmd = new SetSamplingParams();
            pCmd.nRange = nFIDMRange;
            byte nCmd = CMD_FIDM_SET_SAMPLING_PARAMETERS;
            byte[] bytes =pCmd.pack();

            byte nLength = (byte)bytes.length;
            //lock (obj)
            {
                //  WriteToPhxLog("SetSamplingParameters");
                TransmitSerialCmd(nCmd, bytes, nLength, nLength, true);
                // ReceiveCmdResponse<FIDM_STATUS>(nCmd);
            }
        }

        //从byte[]构建strutre
        public  Phx21Status parseReadData(byte[] bytes){
          /* byte[] bytes = new byte[]{
                    -91,81,37,-44,-59,12,0,-111,14,72,
                    27,-29,11,-77,0, -119,2,-92,4,0,
                    112,17,1,0,124,2,0,0,-16,-2,
                    -1,-1,0,0,0,0,67,2,67,0,
                    25,-61,0,0,-5,-1,24,-4,-112,2,
                    40,-61,1,0,-6,-1,24,-4,-118,2,
                    55,-61,-2,-1,-4,-1,52,-4,-102,2,
                    0,0,0,0,0,0,0,0,0,0,
                    -114};*/
           /* String as="";
            for (byte adata:bytes){
                as+=adata+",";
            }
            Log.i("发送的数据---",as);*/
            DEFAULT_RESPONSE_EXTENDED rsp = new DEFAULT_RESPONSE_EXTENDED();

            rsp.unpack(bytes);

           // System.out.println(rsp.nCmdID);

            Phx21Status phx21Status =  GetStatusFromFidmStatusExtended(rsp.status);
            CurrentStatus=phx21Status;
            return phx21Status;
//            System.out.println(phx21Status.AirPressure);
        }

        private  int num0s = 0;
        private  int ignitedChagedCount = 0;
        private  boolean prevIgnite = false;
        private  int junkDataCount = 0;
        private  int changeCount = 0;
        private  List<Double> pastPpms = new ArrayList<>() ;
        private  int maxPastPpms = 50;
        private  int UseAvgPerc = 10;
        private  int LongAverageCount = 25;
        private  int ShortAverageCount = 5;
        private  int AverageCutoffPpm = 40;
        private  byte currentHardwareAvg = 10;
        private  SimpleDateFormat aDate=new SimpleDateFormat("yyyy-MM-dd hh:MM:ss");
        private boolean CheckIfIgnited(Phx21Status status)
        {
            return status.ThermoCouple > 75 && status.IsSolenoidAOn && status.IsPumpAOn;
        }
        private  Phx21Status GetStatusFromFidmStatusExtended(FIDM_STATUS_EXTENDED status)
        {
            double ppm =
                    Math.round(
                            0.1f *
                                    BytesToDword(status.nFIDTenthsPPM3, status.nFIDTenthsPPM2, status.nFIDTenthsPPM1,
                                            status.nFIDTenthsPPM0) );
            if (ppm >= 100)
                ppm = Math.round(ppm);
            if (ppm < 0)
                ppm = 0;

            if (ppm == 0)
            {
                num0s++;
                if (num0s > 5)
                {
                    num0s = -5;
                }
                if (num0s < 0)
                {
                    ppm = 0.1;
                }
            }

            Phx21Status phx21Status = new Phx21Status();
            phx21Status. IsPumpAOn = (status.nStatusFlags & STATUS_PUMP_A_ON) > 0;
            phx21Status.      AirPressure = BytesToWord(status.nAirPressure_HPSI1, status.nAirPressure_HPSI0) / 100.0f;
            phx21Status.          BatteryVoltage = BytesToWord(status.nBatt_mV1, status.nBatt_mV0) / 1000.0f;
            phx21Status. ChamberOuterTemp =
                    ConvertKelvinToFahrenheit(BytesToWord(status.nChamberOuterTemp_TK1, status.nChamberOuterTemp_TK0) /
                            10.0f);
            phx21Status. RawPpm = ppm;
            phx21Status. SamplePressure = BytesToWord(status.nSamplePressure_HPSI1, status.nSamplePressure_HPSI0) / 100.0f;
            phx21Status. TankPressure = 10.0f * (BytesToWord(status.nH2Pressure_PSI1, status.nH2Pressure_PSI0) / 10);
            //this is copied... losing a fraction looks intentional
            phx21Status. ThermoCouple =
                    ConvertKelvinToFahrenheit(BytesToWord(status.nThermocouple_TK1, status.nThermocouple_TK0) / 10.0f);
            phx21Status. PicoAmps =
                    (double)
                            BytesToDword(status.nFIDTenthsPicoA_In13, status.nFIDTenthsPicoA_In12,
                                    status.nFIDTenthsPicoA_In11, status.nFIDTenthsPicoA_In10) / 10.0;
            phx21Status. SystemCurrent = BytesToWord(status.nSystemCurrentMa1, status.nSystemCurrentMa0);
            phx21Status. PumpPower = status.nPumpA_power_pct;
            phx21Status. IsSolenoidAOn = (status.nStatusFlags & STATUS_SOLENOID_A_ON) > 0;
            phx21Status. IsSolenoidBOn = (status.nStatusFlags & STATUS_SOLENOID_B_ON) > 0;
            phx21Status. FIDRange = status.nFIDRange;
            phx21Status. Timestamp = aDate.format(new Date());

            //check for ignition
            boolean isIgnited = CheckIfIgnited(phx21Status);;
            if (isIgnited != prevIgnite)
            {
                ignitedChagedCount++;

                if (ignitedChagedCount >= 3)
                {
                    prevIgnite = isIgnited;
                }
            }
            else
            {
                ignitedChagedCount = 0;
            }
            phx21Status.IsIgnited = prevIgnite;
            if (phx21Status.IsIgnited && phx21Status.PumpPower >= 85.0)
            {
                // WriteToPhxLog("Pump power is above 85% (" + phx21Status.PumpPower + "%), shutting off pump!");
                TurnOffPump();
            }
            //Check for junk data
            //Reread if junk data
            if ((phx21Status.BatteryVoltage > 15 || phx21Status.PicoAmps < -10000 || phx21Status.ThermoCouple < -400) && junkDataCount < 10)
            {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                junkDataCount++;
                ReadDataExtended();
                return null;
            }
            junkDataCount = 0;
            //This is where the ppm range is switched
            if (phx21Status.FIDRange == RANGE_MODE_0_LO && phx21Status.PicoAmps >= 6500)
            {
                changeCount++;
                if (changeCount >= 1)
                {
                    changeCount = 0;
                     SetSamplingParameters(RANGE_MODE_3_MAX);
                    try {
                        Thread.sleep(250);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (phx21Status.FIDRange == RANGE_MODE_3_MAX && phx21Status.PicoAmps <= 6000)
            {
                changeCount++;
                if (changeCount >= 1)
                {
                    changeCount = 0;
                     SetSamplingParameters(RANGE_MODE_0_LO);
                    try {
                        Thread.sleep(250);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            pastPpms.add(phx21Status.RawPpm);
            if (pastPpms.size() > maxPastPpms)
                pastPpms.remove(0);
            //apply averaging to the ppm value
            int start =Math.max(pastPpms.size() - LongAverageCount, 0);
            int end =pastPpms.size();
            phx21Status.LongAveragePpm = getAvg(pastPpms.subList(start,end));
            phx21Status.LongAveragePpm = phx21Status.LongAveragePpm >= 100
                    ? Math.round(phx21Status.LongAveragePpm )
                    : Math.round(phx21Status.LongAveragePpm );
            start =Math.max(pastPpms.size() - ShortAverageCount, 0);
            end =pastPpms.size();
        List<Double> shortAveragePpms = pastPpms.subList(start,end);
        phx21Status.ShortAveragePpm = getAvg(shortAveragePpms);
        phx21Status.ShortAveragePpm = phx21Status.ShortAveragePpm >= 100
                ? Math.round(phx21Status.ShortAveragePpm)
                : Math.round(phx21Status.ShortAveragePpm);
        phx21Status.UseAverage = All(shortAveragePpms,phx21Status.LongAveragePpm,UseAvgPerc);
            if (phx21Status.UseAverage)
            {
                phx21Status.Ppm = phx21Status.FIDRange == RANGE_MODE_3_MAX ? phx21Status.LongAveragePpm : phx21Status.ShortAveragePpm;
            }
            else
            {
                phx21Status.Ppm = phx21Status.RawPpm;
            }
            phx21Status.PpmStr = phx21Status.IsIgnited ? phx21Status.Ppm+"" : "N/A";
            if (phx21Status.PicoAmps <= 200 && currentHardwareAvg == 10)
            {
                currentHardwareAvg = 50;
                 SetIntegrationControlParams((byte)0, (byte)1, (byte)7, 50000, currentHardwareAvg, (byte)0);
            }
            else if (phx21Status.PicoAmps > 200 && currentHardwareAvg == 50)
            {
                currentHardwareAvg = 10;
                  SetIntegrationControlParams((byte)0, (byte)1, (byte)7, 50000, currentHardwareAvg, (byte)0);
            }
            // AppendToFile(LogFilePath, GetLineForLog(phx21Status));
            return phx21Status;
        }
        private   void SetIntegrationControlParams(byte nMode, byte nChargeMultiplier, byte nRange,
                                                        int nIntegrationTimeUs,
                                                        byte nSamplesToAvg, byte nReportMode)
        {
            IntegrationControlParams pCmd = new IntegrationControlParams();
            boolean result = false;
            byte nCmd = CMD_FIDM_INTEGRATION_CONTROL;
            pCmd.nMode = nMode;
            pCmd.nChargeMultiplier = nChargeMultiplier;
            pCmd.nRange = nRange;
            pCmd.nIntegrationTimeUs0 = DwordToByte0(nIntegrationTimeUs);
            pCmd.nIntegrationTimeUs1 = DwordToByte1(nIntegrationTimeUs);
            pCmd.nSamplesToAvg = nSamplesToAvg;
            pCmd.nReportMode = nReportMode;
            byte[] bytes=pCmd.pack();
            byte nLength = (byte)bytes.length;
            //  lock (obj)
            {
                // WriteToPhxLog("SetIntegrationControlParams");
                TransmitSerialCmd(nCmd, bytes, nLength, nLength, true);
                //  ReceiveCmdResponse<FIDM_STATUS>(nCmd);
            }
        }
        public void write(byte[] data) {
            try {
                mOutputStream.write(data);
                mHandler.obtainMessage(BluetoothSerial.MESSAGE_WRITE, -1, -1, data).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Unable to write the socket!");
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Unable to close the socket!");
            }
        }

    }

}
