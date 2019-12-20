package com.macroyau.blue2serial;

import java.util.List;

public class OptionUtils {
    public static int BytesToWord(byte b1, byte b0)
    {
        return 0xFFFF & (b1 &0xff) << 8 | b0 &0xff;
    }

    public static int BytesToDword(byte b3, byte b2, byte b1, byte b0)
    {
        return (b3 & 0xff) << 24 | (b2 & 0xff) << 16 | (b1 & 0xff) << 8 | b0 & 0xff;
    }

    public static float ConvertKelvinToFahrenheit(float kelvin)
    {
        return (float)Math.round((kelvin - 273.15f) * 1.8f + 32 );
    }

    public static byte DwordToByte0(int dword)
    {
        return (byte)(0xFF & (dword));
    }

    public static byte DwordToByte1(int dword)
    {
        return (byte)(0xFF & ((dword) >> 8));
    }

    public static byte DwordToByte2(int dword)
    {
        return (byte)(0xFF & ((dword) >> 16));
    }

    public static byte DwordToByte3(int dword)
    {
        return (byte)(0xFF & ((dword) >> 24));
    }

    public  static Double getAvg(List<Double> list){
        Double avg=0.0;
        if (list != null&& list.size()>0){
            for (int i =0;i<list.size();i++){
                avg+=list.get(i);
            }
            avg=avg/list.size();
        }
        return avg;
    }

    public static boolean All(List<Double> list,Double LongAveragePpm,int UseAvgPerc){
        boolean isAll=true;
        for (Double p:list){
            isAll=((p / LongAveragePpm) * 100 >= 100 - UseAvgPerc
                    && (p / LongAveragePpm) * 100 <= 100 + UseAvgPerc);
            if (!isAll){
                break;
            }
        }
        return isAll;
    }

    public static boolean All(List<Double> list){
        boolean isAll=true;
        for (Double p:list){
            isAll=p>10;
            if (!isAll){
                break;
            }
        }
        return isAll;
    }
}
