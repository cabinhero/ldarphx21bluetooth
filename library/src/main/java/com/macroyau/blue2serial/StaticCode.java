package com.macroyau.blue2serial;

public class StaticCode {
    public static int MAX_CMD_LENGTH_BYTES = 255;
    public static int CMD_START_TIMEOUT_MS = 300;

    /// <summary>
    /// Sync codes, these signal the start of a new message
    /// </summary>
    public static byte SYNC_CODE_CMD = 0x5A;
    public static byte SYNC_CODE_RES = (byte)0xA5;

    public static byte ERROR_LIST_LENGTH = 4;

    /// <summary>
    /// Field positions common to all messages received
    /// </summary>
    public static byte FIELD_SYNC_CODE = 0;
    public static byte FIELD_LENGTH_BYTES = 1;
    public static byte FIELD_CMD_ID = 2;

    /// <summary>
    /// command bytes
    /// </summary>
    public static byte CMD_FIDM_NO_OP = 0x00;
    public static byte CMD_FIDM_PUMP_CONTROL = 0x01;
    public static byte CMD_FIDM_SOLENOID_CONTROL = 0x02;
    public static byte CMD_FIDM_IGNITE_PULSE = 0x03;
    public static byte CMD_FIDM_SET_SAMPLING_PARAMETERS = 0x04;
    public static byte CMD_FIDM_READ_DATA = 0x05;
    public static byte CMD_FIDM_RESET_FIRMWARE = 0x06;
    public static byte CMD_FIDM_FLASH_WRITE = 0x07;
    public static byte CMD_FIDM_FLASH_READ = 0x08;
    public static byte CMD_FIDM_FLASH_ERASE = 0x09;
    public static byte CMD_FIDM_CONFIGURATION_READ = 0x0A;
    public static byte CMD_FIDM_DEBUG = 0x0B;
    public static byte CMD_FIDM_INTEGRATION_CONTROL = 0x0C;
    public static byte CMD_FIDM_HIGH_VOLTAGE_ON_OFF = 0x0D;
    public static byte CMD_FIDM_FIDM_CONFIGURATION_READ = 0x0E;
    public static byte CMD_FIDM_SET_BT_WATCHDOG = 0x0F;
    public static byte CMD_FIDM_SET_TC_CALIB_LO = 0x10;
    public static byte CMD_FIDM_SET_TC_CALIB_HI = 0x11;
    public static byte CMD_FIDM_SET_TM_CALIB_LO = 0x12;
    public static byte CMD_FIDM_SET_TM_CALIB_HI = 0x13;
    public static byte CMD_FIDM_FLASH_START_STREAM_WRITE = 0x14;
    public static byte CMD_FIDM_FLASH_WRITE_STREAM_DATA = 0x15;
    public static byte CMD_FIDM_FLASH_STOP_STREAM_WRITE = 0x16;
    public static byte CMD_FIDM_FLASH_START_STREAM_READ = 0x17;
    public static byte CMD_FIDM_FLASH_STOP_STREAM_READ = 0x18;
    public static byte CMD_FIDM_NEEDLE_VALVE_STEP = 0x19;
    public static byte CMD_FIDM_GET_SYSTEM_CURRENT = 0x1A;
    public static byte CMD_FIDM_PUMP_AUX_1_CONTROL = 0x1B;
    public static byte CMD_FIDM_SET_PUMPA_CTRL_PARAMS = 0x1C;
    public static byte CMD_FIDM_SET_PUMPA_CLOSED_LOOP = 0x1D;
    public static byte CMD_FIDM_SET_DEADHEAD_PARAMS = 0x1E;
    public static byte CMD_FIDM_GET_ERROR_LIST = 0x1F;
    public static byte CMD_FIDM_AUTO_IGNITION_SEQUENCE = 0x20;
    public static byte CMD_FIDM_GENERATE_PPM_CALIBRATION = 0x21;
    public static byte CMD_FIDM_SET_PPM_CALIBRATION = 0x22;
    public static byte CMD_FIDM_GET_PPM_CALIBRATION = 0x23;
    public static byte CMD_FIDM_SET_CAL_H2PRES_COMPENSATION = 0x24;
    public static byte CMD_FIDM_READ_DATA_EXTENDED = 0x25;
    public static byte LAST_VALID_CMD = 0x25;

    public static byte STATUS_PUMP_A_ON = 0x01;
    public static byte STATUS_PUMP_B_ON = 0x02;
    public static byte STATUS_SOLENOID_A_ON = 0x04;
    public static byte STATUS_SOLENOID_B_ON = 0x08;
    public static byte STATUS_GLOW_PLUG_A_ON = 0x10;
    public static byte STATUS_GLOW_PLUG_B_ON = 0x20;
    public static byte STATUS_HV_ON = 0x40;
   // public static byte STATUS_NEW_ERROR = 0x80;

    public static byte ERROR_NO_ERROR = 0x00;
   /* public static byte ERROR_UNKNOWN_CMD = 0xFF;
    public static byte ERROR_INCORRECT_NUM_PARAMS = 0xFE;
    public static byte ERROR_INVALID_PARAM = 0xFD;
    public static byte ERROR_FLASH_STREAM_SEQUENCE_LOST = 0xFC;
    public static byte ERROR_NEEDLE_VALVE_MOVING = 0xFB;
    public static byte ERROR_BATT_TOO_LOW = 0xFA;
    public static byte ERROR_NO_EMPTY_CAL_SLOTS = 0xF9;*/

    public static byte ERROR_DEAD_HEAD = 1;
    public static byte ERROR_IGN_SEQ_FAILED_PRES = 2;
    public static byte ERROR_IGN_SEQ_FAILED_TEMP = 3;

    public static byte RANGE_MODE_0_LO = 0;
    public static byte RANGE_MODE_1_MID = 1;
    public static byte RANGE_MODE_2_HI = 2;
    public static byte RANGE_MODE_3_MAX = 3;

    public static byte FLAG_RES_RECEIVING_RESPONSE = 0x01;
    public static byte FLAG_RES_COMPLETE = 0x02;
    public static byte FLAG_RES_CRC_VALID = 0x04;
    public static byte FLAG_RES_CORRECT_NUM_RESULTS = 0x08;
    public static byte FLAG_RES_KNOWN_RES = 0x10;

   // public static byte MAX_FLASH_BYTES_PER_OP = 192;

    /// <summary>
    /// States for used while receiving data
    /// </summary>
    public static byte STATE_WAITING_FOR_SYNC_CODE = 0;
    public static byte STATE_WAITING_FOR_LENGTH = 1;
    public static byte STATE_WAITING_FOR_RESPONSE_ID = 2;
    public static byte STATE_WAITING_FOR_RESPONSE_DATA = 3;
    public static byte STATE_RESPONSE_COMPLETE = 4;

    public static byte PID_LOG_SIZE = 5;
    public static int RESPONSE_WAIT = 1100;
    public static int WRITE_RESPONSE_WAIT = 5000;
}
