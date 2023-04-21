package com.ravenpos.ravendspreadpos.utils;

import android.util.Log;

import com.pnsol.sdk.miura.emv.tlv.ISOUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MessagePacker {

    private static final String TAG = MessagePacker.class.getSimpleName();

    public static String generateHashData(TransactionMessage message) {
        ISO8583 packISO8583 = new ISO8583();
        packISO8583.setMit(message.getField0());
        packISO8583.clearBit();
        if (message.getField2() != null) {
            byte[] field2 = message.getField2().getBytes();
            packISO8583.setBit(2, field2, field2.length);
        }
        if (message.getField3() != null) {
            byte[] field3 = message.getField3().getBytes();
            packISO8583.setBit(3, field3, field3.length);
        }
        if (message.getField4() != null) {
            byte[] field4 = message.getField4().getBytes();
            packISO8583.setBit(4, field4, field4.length);
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMddhhmmss");
        String datetime = simpleDateFormat.format(new Date());

        byte[] field7 = message.getField7().getBytes();
        packISO8583.setBit(7, field7, field7.length);
        simpleDateFormat = new SimpleDateFormat("hhmmss");
        String stan = simpleDateFormat.format(new Date());

        byte[] field11 = message.getField11().getBytes();
        packISO8583.setBit(11, field11, field11.length);

        byte[] field12 = message.getField12().getBytes();
        packISO8583.setBit(12, field12, field12.length);
        simpleDateFormat = new SimpleDateFormat("MMdd");
        String date = simpleDateFormat.format(new Date());

        byte[] field13 = message.getField13().getBytes();
        packISO8583.setBit(13, field13, field13.length);
        if (message.getField14() != null) {
            byte[] field14 = message.getField14().getBytes();
            packISO8583.setBit(14, field14, field14.length);
        }

        byte[] field18 = message.getField18().getBytes();
        packISO8583.setBit(18, field18, field18.length);

        if (message.getField22() != null) {
            byte[] field22 = message.getField22().getBytes();
            packISO8583.setBit(22, field22, field22.length);
        }
        if (message.getField23() != null) {
            byte[] field23 = message.getField23().getBytes();
            packISO8583.setBit(23, field23, field23.length);
        }
        if (message.getField25() != null) {
            byte[] field25 = message.getField25().getBytes();
            packISO8583.setBit(25, field25, field25.length);
        }
        if (message.getField26() != null) {
            byte[] field26 = message.getField26().getBytes();
            packISO8583.setBit(26, field26, field26.length);
        }
        if (message.getField28() != null) {
            byte[] field28 = message.getField28().getBytes();
            packISO8583.setBit(28, field28, field28.length);
        }
        if (message.getField32() != null) {
            byte[] field32 = message.getField32().getBytes();
            packISO8583.setBit(32, field32, field32.length);
        }
        if (message.getField35() != null) {
            byte[] field35 = message.getField35().getBytes();
            packISO8583.setBit(35, field35, field35.length);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String pre = dateFormat.format(new Date());
        String rrn = pre.substring(2);
        byte[] field37 = message.getField37().getBytes();
        packISO8583.setBit(37, field37, field37.length);


//        if(message.getField38() != null)
//        {
//            byte[] field38 = ProfileParser.field38.getBytes();
//            packISO8583.setBit(38, field38, field38.length);
//        }

        if (message.getField40() != null) {
            byte[] field40 = message.getField40().getBytes();
            packISO8583.setBit(40, field40, field40.length);
        }
        byte[] field41 = message.getField41().getBytes();
        packISO8583.setBit(41, field41, field41.length);

        byte[] field42 = message.getField42().getBytes();
        packISO8583.setBit(42, field42, field42.length);

        byte[] field43 = message.getField43().getBytes();
        packISO8583.setBit(43, field43, field43.length);

        byte[] field49 = message.getField49().getBytes();
        packISO8583.setBit(49, field49, field49.length);

        if (message.getField52() != null && message.getField52().length() > 4) {
            byte[] field52 = message.getField52().getBytes();
            packISO8583.setBit(52, field52, field52.length);
        }
        if (message.getField55() != null) {
            byte[] field55 = message.getField55().getBytes();
            packISO8583.setBit(55, field55, field55.length);
        }
//        if(message.getField56() != null)
//        {
//            byte[] field56 = ProfileParser.field56.getBytes();
//            packISO8583.setBit(56, field56, field56.length);
//        }

        byte[] field123 = message.getField123().getBytes();
        packISO8583.setBit(123, field123, field123.length);
        byte use = 0x0;
        char ch = (char) use;
        byte[] field128 = Character.toString(ch).getBytes();
        packISO8583.setBit(128, field128, field128.length);
        ISO8583.sec = true;


        byte[] preUnmac = packISO8583.getMacIso();
        byte[] unMac = new byte[preUnmac.length - 64];
        System.arraycopy(preUnmac, 0, unMac, 0, preUnmac.length - 64);

        //byte[] unMac =  packISO8583.getMacIso();
        Log.i(TAG, "ISO BEFORE MAC: " + new String(unMac));
        AlgoUtil enc = new AlgoUtil();
        String gotten = null;
        try {
            Log.i(TAG, "CLEAR SESSION KEY USED: " + message.getClrsesskey());
            gotten = enc.getMacNibss(message.getClrsesskey(), unMac);
            Log.i(TAG, "MAC: " + gotten);
        } catch (Exception e) {
            e.printStackTrace();
        }
        field128 = gotten.getBytes();
        packISO8583.setBit(128, field128, field128.length);
        ISO8583.sec = true;
        byte[] packData = packISO8583.isotostr();


        Log.i(TAG, "ISO TO HOST: " + ISOUtil.hexString(packData));


        Log.i(TAG, "IP: " + message.getHost());
        Log.i(TAG, "PORT: " + message.getPort());
        Log.i(TAG, "SSL: " + message.getSsl());

        Log.i(TAG, "Storing for database sake");
        byte[] getSending = new byte[packData.length - 2];
        System.arraycopy(packData, 2, getSending, 0, packData.length - 2);
        ISO8583.sec = true;
        ISO8583 unpackISO8583 = new ISO8583();
        unpackISO8583.strtoiso(getSending);
        String[] sending = new String[128];
        logISOMsgMute(unpackISO8583, sending);
        return gotten;
    }

    public static void logISOMsgMute(ISO8583 msg, String[] storage) {
        Log.i(TAG, "----ISO LOGGER-----");
        try {
            for (int i = 0; i < 129; i++) {
                try {
                    String log = new String(msg.getBit(i));
                    if (log != null) {
                        storage[i] = log;
                    }
                } catch (Exception e) {
                    //Do nothing about it
                }
            }
        } finally {
            Log.i(TAG, "--------------------");
        }

    }

    public static void logISOMsg(ISO8583 msg, String[] storage) {
        Log.i(TAG, "----ISO LOGGER-----");
        try {
            for (int i = 0; i < 129; i++) {
                try {
                    String log = new String(msg.getBit(i));
                    if (log != null) {
                        storage[i] = log;
                    }
                } catch (Exception e) {
                    //Do nothing about it
                }
            }
        } finally {
            Log.i(TAG, "--------------------");
        }

    }


}
