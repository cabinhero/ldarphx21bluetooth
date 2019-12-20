package com.macroyau.blue2serial;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.macroyau.blue2serial.structure.AUTO_IGNITION_SEQUENCE;
import com.macroyau.blue2serial.structure.GenerateCalibration;
import com.macroyau.blue2serial.structure.GetCalibration;
import com.macroyau.blue2serial.structure.GetCalibrationResponse;
import com.macroyau.blue2serial.structure.READ_DATA_PARAMS;
import com.macroyau.blue2serial.structure.SetCalibration;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static com.macroyau.blue2serial.StaticCode.CMD_FIDM_AUTO_IGNITION_SEQUENCE;
import static com.macroyau.blue2serial.StaticCode.CMD_FIDM_GENERATE_PPM_CALIBRATION;
import static com.macroyau.blue2serial.StaticCode.CMD_FIDM_GET_PPM_CALIBRATION;
import static com.macroyau.blue2serial.StaticCode.CMD_FIDM_READ_DATA_EXTENDED;
import static com.macroyau.blue2serial.StaticCode.CMD_FIDM_SET_PPM_CALIBRATION;
import static com.macroyau.blue2serial.StaticCode.FIELD_CMD_ID;
import static com.macroyau.blue2serial.StaticCode.FIELD_LENGTH_BYTES;
import static com.macroyau.blue2serial.StaticCode.SYNC_CODE_CMD;


/**
 * Create an instance of this class in your Android application to use the Blue2Serial library. BluetoothSerial creates a Bluetooth serial port using the Serial Port Profile (SPP) and manages its lifecycle.
 *
 * @author Macro Yau
 */
public class BluetoothSerial {

    private static final String TAG = "BluetoothSerial";

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    protected static final int MESSAGE_STATE_CHANGE = 1;
    protected static final int MESSAGE_READ = 2;
    protected static final int MESSAGE_READ_STR = 21;
    protected static final int MESSAGE_WRITE = 3;
    protected static final int MESSAGE_DEVICE_INFO = 4;

    protected static final String KEY_DEVICE_NAME = "DEVICE_NAME";
    protected static final String KEY_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private static final byte[] CRLF = { 0x0D, 0x0A }; // \r\n
    private BluetoothAdapter mAdapter;
    private Set<BluetoothDevice> mPairedDevices;

    private BluetoothSerialListener mListener;
    private static SPPService mService;
    private LocalBroadcastManager mbroadcastManager;
    private String mConnectedDeviceName, mConnectedDeviceAddress;

    private boolean isRaw;

    /**
     * Constructor.
     * @param context The {@link android.content.Context} to use.
     * @param listener The {@link com.macroyau.blue2serial.BluetoothSerialListener} to use.
     */
    public BluetoothSerial(Context context, BluetoothSerialListener listener,LocalBroadcastManager broadcastManager) {
        mAdapter = getAdapter(context);
        mListener = listener;
        isRaw = mListener instanceof BluetoothSerialRawListener;
        mbroadcastManager=broadcastManager;
    }

    public static BluetoothAdapter getAdapter(Context context) {
        BluetoothAdapter bluetoothAdapter = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null)
                bluetoothAdapter = bluetoothManager.getAdapter();
        } else {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return bluetoothAdapter;
    }

    /**
     * Check the presence of a Bluetooth adapter on this device and set up the Bluetooth Serial Port Profile (SPP) service.
     */
    public void setup() {
        if (checkBluetooth()) {
            mPairedDevices = mAdapter.getBondedDevices();
            mService = new SPPService(mHandler, mbroadcastManager);
        }
    }

    /**
     * Return true if Bluetooth is currently enabled and ready for use.
     *
     * @return true if this device's adapter is turned on
     */
    public boolean isBluetoothEnabled() {
        return mAdapter.isEnabled();
    }

    public boolean checkBluetooth() {
        if (mAdapter == null) {
            mListener.onBluetoothNotSupported();
            return false;
        } else {
            if (!mAdapter.isEnabled()) {
                mListener.onBluetoothDisabled();
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Open a Bluetooth serial port and get ready to establish a connection with a remote device.
     */
    public void start() {
        if (mService != null && mService.getState() == STATE_DISCONNECTED) {
            mService.start();
        }
    }

    /**
     * Connect to a remote Bluetooth device with the specified MAC address.
     *
     * @param address The MAC address of a remote Bluetooth device.
     */
    public void connect(String address) {
        BluetoothDevice device = null;
        try {
            device = mAdapter.getRemoteDevice(address);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Device not found!");
        }
        if (device != null)
            connect(device);
    }

    /**
     * Connect to a remote Bluetooth device.
     *
     * @param device A remote Bluetooth device.
     */
    public void connect(BluetoothDevice device) {
        if (mService != null) {
            mService.connect(device);
        }
    }

    /**
     * Write the specified bytes to the Bluetooth serial port.
     *
     * @param data The data to be written.
     */
    public static void write(byte[] data) {
        if (mService.getState() == STATE_CONNECTED) {
            mService.write(data);
        }
    }

    /**
     * Write the specified bytes to the Bluetooth serial port.
     *
     * @param data The data to be written.
     * @param crlf Set true to end the data with a newline (\r\n).
     */
    public void write(String data, boolean crlf) {
        write(data.getBytes());
        if (crlf)
            write(CRLF);
    }
    public void Ignite(boolean onOff, int glowplug) {
        byte nCmd = CMD_FIDM_AUTO_IGNITION_SEQUENCE;
        AUTO_IGNITION_SEQUENCE ignition = BuildAutoIgnitionSequence();
        ignition.use_glow_plug_b = ByteOption.intToByte(glowplug);
        ignition.start_stop = (byte)(onOff ? 1 : 0);
        byte[] bytes = ignition.pack();
        byte nLength = (byte)bytes.length;
        TransmitSerialCmd(nCmd, bytes, nLength, nLength, true);
    }
    Timer timer = new Timer();
    TimerTask task =null;
    Handler handler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ReadDataExtended();
                   // Log.i("时钟","读取任务");
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public void startLog(){
        startTimer();
    }
    public void startTimer(){
        if (timer == null) {
            timer = new Timer();
        }

        if (task == null) {
             task = new TimerTask(){
                public void run() {
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                }
            };
        }
        if(timer != null && task != null )
            timer.schedule(task, 250, 300);

    }
    public void stopTimer(){
        System.out.println("停止时钟");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public static void ReadDataExtended() {
        byte nCmd = CMD_FIDM_READ_DATA_EXTENDED;
        READ_DATA_PARAMS pCmd = new READ_DATA_PARAMS();
        byte[]   bytes = pCmd.pack();
        byte nLength = (byte)bytes.length;
        TransmitSerialCmd(nCmd, bytes, nLength, nLength, true);
    }

    public void GeneratePpmCalibration(int ppm)
    {
        byte nCmd = CMD_FIDM_GENERATE_PPM_CALIBRATION;
        GenerateCalibration cmd = new GenerateCalibration();
        cmd.ppm_tenths = ppm * 10; //to get tenths
        cmd.spare_for_alignment = 0;
        byte[]   bytes = cmd.pack();
        byte nLength = (byte)bytes.length;
        TransmitSerialCmd(nCmd, bytes, nLength, nLength, true);
    }
    public boolean isDeviceIgnited(){
        Phx21Status CurrentStatus=SPPService.CurrentStatus;
        if (CurrentStatus==null||!CurrentStatus.IsIgnited)
            return false;
        else return true;
    }
    public  void SetPpmCalibration(Context context,int indexNumber, int ppmTenths)
    {
        Phx21Status CurrentStatus=SPPService.CurrentStatus;
        if (CurrentStatus!=null){
            SetPpmCalibration(indexNumber, ppmTenths, (int)(CurrentStatus.PicoAmps * 10),
                    (char)(CurrentStatus.TankPressure * 10), true);
        }else{
            Toast.makeText(context,"CurrentStatus是null，请先读数据，进行初始化",Toast.LENGTH_SHORT).show();
        }
    }


    public  void SetPpmCalibration(int indexNumber, int ppmTenths, int picoampsTenths, char H2Pressure,
                                         boolean overwrite)
    {
        SetCalibration cmd = new SetCalibration();

        byte nCmd = CMD_FIDM_SET_PPM_CALIBRATION;

        cmd.index_number = (byte)indexNumber;
        cmd.ppm_tenths = ppmTenths*10;
        cmd.fid_current_tPa = picoampsTenths;
        cmd.H2_pressure_hPSI = H2Pressure;
        cmd.overwrite = overwrite ? (byte)1 : (byte)0;

        byte[] bytes=cmd.pack();
        byte nLength = (byte)bytes.length;

        //  lock (obj)
        {
            //  WriteToPhxLog("SetPpmCalibration");
            TransmitSerialCmd(nCmd, bytes, nLength, nLength, true);
            //  ReceiveCmdResponse<FIDM_STATUS>(nCmd);
        }

    }
    public void GetPpmCalibration(int index)
    {
       // Log.i("获取校准指令",""+index);
        GetCalibration Cmd = new GetCalibration();
        GetCalibrationResponse Rsp;
        byte nCmd = CMD_FIDM_GET_PPM_CALIBRATION;
        Cmd.index_number = (byte)index;
        byte[] bytes=Cmd.pack();
        byte nLength = (byte)bytes.length;
        TransmitSerialCmd(nCmd,bytes, nLength, nLength, true);
    }



    /// <summary>
    /// This is used to build the AUTO_IGNITION_SEQUENCE arguments for igniting a phx21
    /// </summary>
    /// <returns>A fully built AUTO_IGNITION_SEQUENCE</returns>
    private  AUTO_IGNITION_SEQUENCE BuildAutoIgnitionSequence()
    {
        AUTO_IGNITION_SEQUENCE ignition = new AUTO_IGNITION_SEQUENCE();
        ignition.start_stop = 1;
        ignition.target_hPSI = 175;
        ignition.tolerance_hPSI = 5;
        ignition.max_pressure_wait_msec = 10000;
        ignition.min_temperature_rise_tK = 10;
        ignition.max_ignition_wait_msec = 5000;
        ignition.sol_b_delay_msec = 1000;
        ignition.use_glow_plug_b = 0;
        ignition.pre_purge_pump_msec = 5000;
        ignition.pre_purge_sol_A_msec = 5000;
        ignition.param1 = 0;
        ignition.param2 = 0;
        return ignition;
    }

    public static byte TransmitSerialCmd(byte nCmd, byte[] pStream, byte nTotalCmdLength,
                                   byte nHeaderLength, boolean bSendCrc)
    {
        try
        {
            byte nCRC = 0;
            byte[] pData = new byte[nHeaderLength + 1];

            pStream[StaticCode.FIELD_SYNC_CODE] = SYNC_CODE_CMD;
            pStream[FIELD_LENGTH_BYTES] = (byte)(nTotalCmdLength + 1);
            pStream[FIELD_CMD_ID] = nCmd;

            nCRC = ComputeCRC(pStream, nHeaderLength);
            System.arraycopy(pStream, 0,pData,0,nHeaderLength);

            if (bSendCrc)
            {
                pData[nHeaderLength] =nCRC;
                write(pData);
            }
            else
            {
                write(pData);
            }

            return nCRC;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return 0;
    }



    private static byte ComputeCRC(byte[] pStream, byte nLengthBytes)
    {
        byte chksum  = (byte) 0xD5;
        for (int i = 0; i < nLengthBytes; i++)
        {
            int p1 = (chksum & 0xFF) << 1 ;
            int p7 = (chksum & 0xFF) >> 7;
            int p = p1 | p7;
            chksum  =  (byte) (p + (pStream[i] & 0xFF));
        }
        return chksum;
    }



    public void write_test(String data) {
        data+="<CR><LF>";
        write(data.getBytes());
    }

    /**
     * Write the specified string to the Bluetooth serial port.
     *
     * @param data The data to be written.
     */
    public void write(String data) {
        write(data.getBytes());
    }

    /**
     * Write the specified string ended with a new line (\r\n) to the Bluetooth serial port.
     *
     * @param data The data to be written.
     */
    public void writeln(String data) {
        write(data.getBytes());
        write(CRLF);
    }

    /**
     * Disconnect from the remote Bluetooth device and close the active Bluetooth serial port.
     */
    public void stop() {
        if (mService != null) {
            mService.stop();
        }
    }

    /**
     * Get the current state of the Bluetooth serial port.
     *
     * @return the current state
     */
    public int getState() {
        return mService.getState();
    }

    /**
     * Return true if a connection to a remote Bluetooth device is established.
     *
     * @return true if connected to a device
     */
    public boolean isConnected() {
        return (mService.getState() == STATE_CONNECTED);
    }

    /**
     * Get the name of the connected remote Bluetooth device.
     *
     * @return the name of the connected device
     */
    public String getConnectedDeviceName() {
        return mConnectedDeviceName;
    }

    /**
     * Get the MAC address of the connected remote Bluetooth device.
     *
     * @return the MAC address of the connected device
     */
    public String getConnectedDeviceAddress() {
        return mConnectedDeviceAddress;
    }

    /**
     * Get the paired Bluetooth devices of this device.
     *
     * @return the paired devices
     */
    public Set<BluetoothDevice> getPairedDevices() {
        return mPairedDevices;
    }

    /**
     * Get the names of the paired Bluetooth devices of this device.
     *
     * @return the names of the paired devices
     */
    public String[] getPairedDevicesName() {
        if (mPairedDevices != null) {
            String[] name = new String[mPairedDevices.size()];
            int i = 0;
            for (BluetoothDevice d : mPairedDevices) {
                name[i] = d.getName();
                i++;
            }
            return name;
        }
        return null;
    }

    /**
     * Get the MAC addresses of the paired Bluetooth devices of this device.
     *
     * @return the MAC addresses of the paired devices
     */
    public String[] getPairedDevicesAddress() {
        if (mPairedDevices != null) {
            String[] address = new String[mPairedDevices.size()];
            int i = 0;
            for (BluetoothDevice d : mPairedDevices) {
                address[i] = d.getAddress();
                i++;
            }
            return address;
        }
        return null;
    }

    /**
     * Get the name of this device's Bluetooth adapter.
     *
     * @return the name of the local Bluetooth adapter
     */
    public String getLocalAdapterName() {
        return mAdapter.getName();
    }

    /**
     * Get the MAC address of this device's Bluetooth adapter.
     *
     * @return the MAC address of the local Bluetooth adapter
     */
    public String getLocalAdapterAddress() {
        return mAdapter.getAddress();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case STATE_CONNECTED:
                            mListener.onBluetoothDeviceConnected(mConnectedDeviceName, mConnectedDeviceAddress);
                            break;
                        case STATE_CONNECTING:
                            mListener.onConnectingBluetoothDevice();
                            break;
                        case STATE_DISCONNECTED:
                            mListener.onBluetoothDeviceDisconnected();
                            break;
                    }
                    break;
                case MESSAGE_WRITE: //发送命令不在UI显示
                  /* byte[] bufferWrite = (byte[]) msg.obj;
                    String messageWrite = new String(bufferWrite);
                    mListener.onBluetoothSerialWrite(messageWrite);
                    if (isRaw) {
                        ((BluetoothSerialRawListener) mListener).onBluetoothSerialWriteRaw(bufferWrite);
                    }*/
                    break;
                case MESSAGE_READ:
                    byte[] bufferRead = (byte[]) msg.obj;
                    String messageRead = new String(bufferRead);
                    mListener.onBluetoothSerialRead(messageRead);
                    if (isRaw) {
                        ((BluetoothSerialRawListener) mListener).onBluetoothSerialReadRaw(bufferRead);
                    }
                    break;
                case MESSAGE_READ_STR:
                    Phx21Status obj = (Phx21Status) msg.obj;
                    mListener.onBluetoothSerialRead(obj);
                    break;
                case MESSAGE_DEVICE_INFO:
                    mConnectedDeviceName = msg.getData().getString(KEY_DEVICE_NAME);
                    mConnectedDeviceAddress = msg.getData().getString(KEY_DEVICE_ADDRESS);
                    break;
            }
        }
    };

}
