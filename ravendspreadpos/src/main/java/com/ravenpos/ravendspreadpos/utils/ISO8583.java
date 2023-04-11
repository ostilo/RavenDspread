package com.ravenpos.ravendspreadpos.utils;

import static com.ravenpos.ravendspreadpos.utils.MessagePacker.logISOMsg;
import static com.ravenpos.ravendspreadpos.utils.MessagePacker.logISOMsgMute;

import android.text.TextUtils;
import android.util.Log;

import com.pnsol.sdk.miura.emv.tlv.ISOUtil;

import java.util.Arrays;

public class ISO8583 {
    private static final String TAG = ISO8583.class.getSimpleName();
    public static boolean sec = false;
    public final static int ISO8583_MAX_LENGTH = 128; /*Max Filed Number*/
    public final static int MAXBUFFERLEN = 2048; /*Buffer*/
    public final static int MSGIDLEN = 4; /**/

    public final static byte FIX_LEN = 0; /*(LENgth fix )*/
    public final static byte LLVAR_LEN = 1; /*(LENgth 00~99)*/
    public final static byte LLLVAR_LEN = 2; /*(LENgth 00~999)*/
    public final static byte LLLLVAR_LEN = 2; /*(LENgth 00~9999)*/

    public final static byte L_BCD = 0; /*Left Alignment BCD*/
    public final static byte L_ASC = 1; /*Left Alignment ASC*/
    public final static byte R_BCD = 2; /*Right  Alignment BCD */
    public final static byte R_ASC = 3; /*Right  Alignment ASC*/
    public final static byte D_BIN = 4; /*ֱBCD*/

    public ISO8583Domain[] mISO8583Domain;

    private int mOffset;
    private byte[] mDataBuffer;
    private byte[] mMessageId;

    private class ISO8583Domain { /*8583 Filed Define*/
        public int mMaxLength; /* data element max length */
        public byte mType; /* bit0,bit1 Retain，bit2: 0 Left Alignment ,1 Right Alignment，bit3:0 BCD,1 ASC，type:0,1,2,3三种*/
        public byte mFlag; /* length field length: 0--FIX_LEN型 1--LLVAR_LEN型 2--LLLVAR_LEN型*/
        public String mDomainName; /* 域名*/

        public int mBitf; /*field map if 1 true  0 false*/
        public int mLength; /*field length*/
        public int mStartAddr; /*field data's start address*/

        public void setDomainProperty(int length, byte type, byte flag, String domainName) {
            this.mMaxLength = length;
            this.mType = type;
            this.mFlag = flag;
            this.mDomainName = domainName;
        }
    }

    public ISO8583() {
        mISO8583Domain = new ISO8583Domain[ISO8583_MAX_LENGTH];
        for (int i = 0; i < ISO8583_MAX_LENGTH; i++) {
            mISO8583Domain[i] = new ISO8583Domain();
        }

        initCupISO8583Domain();

        mOffset = 0;
        mDataBuffer = new byte[MAXBUFFERLEN];
        mMessageId = new byte[MSGIDLEN];
    }


    public int setMit(String mit) {
        if (TextUtils.isEmpty(mit) || mit.length() != 4) {
            return -1;
        }
        System.arraycopy(mit.getBytes(), 0, mMessageId, 0, 4);

        return 0;
    }

    public byte[] isotostr() {
        int dataOffset = 0;
        int bitnum = 0;
        int n = 0;
        byte[] data = new byte[MAXBUFFERLEN];
        if (sec == true)
            bitnum = 16;
        else
            bitnum = 8;
        GenericConverter.fromASCIIToBCD(mMessageId, 0, MSGIDLEN, data, 0, false);
        dataOffset = MSGIDLEN / 2 + bitnum; //指向消息类型和位图之后的数据域

        for (int i = 0; i < bitnum; i++) {
            byte bitmap = 0; //表示64位的位图中的一个字节中的8个位图域
            int bitmask = 0x80;
            for (int j = 0; j < 8; j++, bitmask >>= 1) {
                n = (i << 3) + j;
                if (mISO8583Domain[n].mBitf == 0) {//该域没有值
                    continue;
                }
                bitmap |= bitmask;
                int len = mISO8583Domain[n].mLength;

                if (mISO8583Domain[n].mFlag == LLVAR_LEN) {
                    // error Temporary comment for F35 length
                    if (mISO8583Domain[n].mType == D_BIN) {
                        len = (len + 1) / 2;
                    }
                    String lenstr = String.format("%02d", len);

                    byte[] tmp = new byte[MAXBUFFERLEN];
                    byte[] src = lenstr.getBytes();
                    int le = src.length;
                    int l = le;
                    Arrays.fill(tmp, 0, le - l, (byte) ' ');
                    System.arraycopy(src, 0, tmp, le - l, l);
                    System.arraycopy(tmp, 0, data, dataOffset, 2);
                    dataOffset += 2;
                } else if (mISO8583Domain[n].mFlag == LLLVAR_LEN) {
                    if (mISO8583Domain[n].mType == D_BIN) {
                        len = (len + 1) / 2;
                    }
                    String lenstr = String.format("%03d", len);

                    byte[] tmp = new byte[MAXBUFFERLEN];
                    byte[] src = lenstr.getBytes();
                    int le = src.length;
                    int l = le;
                    Arrays.fill(tmp, 0, le - l, (byte) ' ');
                    System.arraycopy(src, 0, tmp, le - l, l);
                    System.arraycopy(tmp, 0, data, dataOffset, 3);
                    dataOffset += 3;
                } else if (mISO8583Domain[n].mFlag == LLLLVAR_LEN) {
                    if (mISO8583Domain[n].mType == D_BIN) {
                        len = (len + 1) / 2;
                    }
                    String lenstr = String.format("%04d", len);

                    byte[] tmp = new byte[MAXBUFFERLEN];
                    byte[] src = lenstr.getBytes();
                    int le = src.length;
                    int l = le;
                    Arrays.fill(tmp, 0, le - l, (byte) ' ');
                    System.arraycopy(src, 0, tmp, le - l, l);
                    System.arraycopy(tmp, 0, data, dataOffset, 4);
                    dataOffset += 4;
                }
                if (mISO8583Domain[n].mType == L_BCD) {
                    len = (len + 1) / 2;
                } else if (mISO8583Domain[n].mType == L_ASC) {

                } else if (mISO8583Domain[n].mType == R_BCD) {
                    len = (len + 1) / 2;
                } else if (mISO8583Domain[n].mType == R_ASC) {

                }
                System.arraycopy(mDataBuffer, mISO8583Domain[n].mStartAddr, data, dataOffset, len);
                dataOffset += len;

            }
            data[i + 2] = bitmap;
        }

        if (mISO8583Domain[127].mBitf == 1 && bitnum == 16) {
        }

        if (bitnum == 16) {
            data[2] |= 0x80;
        }

        if (dataOffset != 0) {
            byte[] BCD = new byte[dataOffset];
            System.arraycopy(data, 0, BCD, 0, dataOffset);
            if (sec == true) {
                byte[] TMP = new byte[18];
                byte[] TMP2 = new byte[dataOffset];
                byte[] TMP3 = new byte[dataOffset + 18];
                System.arraycopy(data, 0, TMP, 0, 18);
                String t = ISOUtil.hexString(TMP);
                TMP2 = t.getBytes();
                System.arraycopy(TMP2, 0, TMP3, 0, 36);
                System.arraycopy(data, 18, TMP3, 36, dataOffset - 18);
                ISO8583 unpackISO8583 = new ISO8583();
                unpackISO8583.strtoiso(TMP3);
                String[] sending = new String[128];
                logISOMsgMute(unpackISO8583, sending);
                String cs = String.format("%04X", TMP3.length);
                byte[] bcdlen = GenericConverter.hexStringToBytes(cs);
                byte[] mPacketMsg = byteArrayAdd(bcdlen, TMP3);
                sec = false;
                return mPacketMsg;
            } else {
                byte[] TMP = new byte[10];
                byte[] TMP2 = new byte[dataOffset];
                byte[] TMP3 = new byte[dataOffset + 10];
                System.arraycopy(data, 0, TMP, 0, 10);
                String t = ISOUtil.hexString(TMP);
                TMP2 = t.getBytes();
                System.arraycopy(TMP2, 0, TMP3, 0, 20);
                System.arraycopy(data, 10, TMP3, 20, dataOffset - 10);
                ISO8583 unpackISO8583 = new ISO8583();
                unpackISO8583.strtoiso(TMP3);
                String[] sending = new String[128];
                logISOMsg(unpackISO8583, sending);
                String cs = String.format("%04X", TMP3.length);
                byte[] bcdlen = GenericConverter.hexStringToBytes(cs);
                byte[] mPacketMsg = byteArrayAdd(bcdlen, TMP3);
                sec = false;
                return mPacketMsg;
            }
        }
        return null;
    }

    public static byte[] byteArrayAdd(byte[] bytesa, byte[] bytesb) {
        int length1 = bytesa.length;
        int length2 = bytesb.length;
        int length = length1 + length2;
        if (length > 0) {
            byte[] bytes = new byte[length];
            System.arraycopy(bytesa, 0, bytes, 0, length1);
            System.arraycopy(bytesb, 0, bytes, length1, length2);
            return bytes;
        } else {
            byte[] bytes = new byte[0];
            return bytes;
        }//BCD
    }


    public byte[] getMacIso() {
        int dataOffset = 0;
        int bitnum = 0;
        int n = 0;
        byte[] data = new byte[MAXBUFFERLEN];
        if (sec == true)
            bitnum = 16;
        else
            bitnum = 8;
        GenericConverter.fromASCIIToBCD(mMessageId, 0, MSGIDLEN, data, 0, false);
        dataOffset = MSGIDLEN / 2 + bitnum;

        for (int i = 0; i < bitnum; i++) {
            byte bitmap = 0;
            int bitmask = 0x80;
            for (int j = 0; j < 8; j++, bitmask >>= 1) {
                n = (i << 3) + j;
                if (mISO8583Domain[n].mBitf == 0) {
                    continue;
                }
                bitmap |= bitmask;
                int len = mISO8583Domain[n].mLength;
                if (mISO8583Domain[n].mFlag == LLVAR_LEN) {
                    if (mISO8583Domain[n].mType == D_BIN) {
                        len = (len + 1) / 2;
                    }
                    String lenstr = String.format("%02d", len);

                    byte[] tmp = new byte[MAXBUFFERLEN];
                    byte[] src = lenstr.getBytes();
                    int le = src.length;
                    int l = le;
                    Arrays.fill(tmp, 0, le - l, (byte) ' ');
                    System.arraycopy(src, 0, tmp, le - l, l);
                    System.arraycopy(tmp, 0, data, dataOffset, 2);
                    dataOffset += 2;
                } else if (mISO8583Domain[n].mFlag == LLLVAR_LEN) {
                    if (mISO8583Domain[n].mType == D_BIN) {
                        len = (len + 1) / 2;
                    }
                    String lenstr = String.format("%03d", len);

                    byte[] tmp = new byte[MAXBUFFERLEN];
                    byte[] src = lenstr.getBytes();
                    int le = src.length;
                    int l = le;
                    Arrays.fill(tmp, 0, le - l, (byte) ' ');
                    System.arraycopy(src, 0, tmp, le - l, l);
                    System.arraycopy(tmp, 0, data, dataOffset, 3);
                    dataOffset += 3;
                } else if (mISO8583Domain[n].mFlag == LLLLVAR_LEN) {
                    if (mISO8583Domain[n].mType == D_BIN) {
                        len = (len + 1) / 2;
                    }
                    String lenstr = String.format("%04d", len);

                    byte[] tmp = new byte[MAXBUFFERLEN];
                    byte[] src = lenstr.getBytes();
                    int le = src.length;
                    int l = le;
                    Arrays.fill(tmp, 0, le - l, (byte) ' ');
                    System.arraycopy(src, 0, tmp, le - l, l);
                    System.arraycopy(tmp, 0, data, dataOffset, 4);
                    dataOffset += 4;
                }
                if (mISO8583Domain[n].mType == L_BCD) {
                    len = (len + 1) / 2;
                } else if (mISO8583Domain[n].mType == L_ASC) {

                } else if (mISO8583Domain[n].mType == R_BCD) {
                    len = (len + 1) / 2;
                } else if (mISO8583Domain[n].mType == R_ASC) {

                }
                System.arraycopy(mDataBuffer, mISO8583Domain[n].mStartAddr, data, dataOffset, len);
                dataOffset += len;

            }
            data[i + 2] = bitmap;
        }

        if (mISO8583Domain[127].mBitf == 1 && bitnum == 16) {
        }

        if (bitnum == 16) {
            data[2] |= 0x80;
        }

        if (dataOffset != 0) {
            byte[] BCD = new byte[dataOffset];
            System.arraycopy(data, 0, BCD, 0, dataOffset);
            if (sec == true) {
                byte[] TMP = new byte[18];
                byte[] TMP2 = new byte[dataOffset];
                byte[] TMP3 = new byte[dataOffset + 18];
                System.arraycopy(data, 0, TMP, 0, 18);
                String t = ISOUtil.hexString(TMP);
                TMP2 = t.getBytes();
                System.arraycopy(TMP2, 0, TMP3, 0, 36);
                System.arraycopy(data, 18, TMP3, 36, dataOffset - 18);
                return TMP3;
            } else {
                byte[] TMP = new byte[10];
                byte[] TMP2 = new byte[dataOffset];
                byte[] TMP3 = new byte[dataOffset + 10];
                System.arraycopy(data, 0, TMP, 0, 10);
                String t = ISOUtil.hexString(TMP);
                TMP2 = t.getBytes();
                System.arraycopy(TMP2, 0, TMP3, 0, 20);
                System.arraycopy(data, 10, TMP3, 20, dataOffset - 10);
                //Log.i(TAG, "FINAL: " + new String(TMP3));
                return TMP3;
            }
        }
        return null;
    }

    public int strtoiso(byte[] src) {
        int bitnum = 0;
        int bitmask = 0x80;
        int srcLen = 0;
        byte[] srcData = null;

        if (sec == true) {
            bitnum = 16;
            bitmask = 0x80;
            clearBit();
            System.arraycopy(src, 0, mMessageId, 0, 4);
            srcLen = src.length;
            srcData = new byte[srcLen - (MSGIDLEN + 32)];
            System.arraycopy(src, MSGIDLEN + bitnum + 16, srcData, 0, srcData.length);
        } else {
            bitnum = 8;
            bitmask = 0x80;
            clearBit();
            System.arraycopy(src, 0, mMessageId, 0, 4);
            srcLen = src.length;
            srcData = new byte[srcLen - (MSGIDLEN + 16)];
            System.arraycopy(src, MSGIDLEN + bitnum + 8, srcData, 0, srcData.length);
        }

        int n = 0;
        int len = 0;
        int srcDataOffset = 0;
        int offset = 0;

        byte[] data1 = GenericConverter.hexStringToBytes(ISOUtil.hexString(src));
        byte[] packData = GenericConverter.hexStringToBytes(new String(data1));

        for (int i = 0; i < bitnum; i++) {
            bitmask = 0x80;
            for (int j = 0; j < 8; j++, bitmask >>= 1) {
                if (i == 0 && bitmask == 0x80) {
                    continue;
                }
                if ((packData[i + 2] & bitmask) == 0) {
                    continue;
                }
                n = (i << 3) + j;
                byte[] temp;
                if (mISO8583Domain[n].mFlag == LLVAR_LEN) {
                    temp = new byte[2];
                    System.arraycopy(srcData, srcDataOffset, temp, 0, 2);
                    String lenSrc = new String(temp);
                    len = Integer.parseInt(lenSrc, 10); // .valueOf(lenSrc);
                    srcDataOffset += 2;
                } else if (mISO8583Domain[n].mFlag == LLLVAR_LEN) {
                    temp = new byte[3];
                    System.arraycopy(srcData, srcDataOffset, temp, 0, 3);
                    String lenSrc = new String(temp);
                    len = Integer.parseInt(lenSrc, 10); // .valueOf(lenSrc);
                    srcDataOffset += 3;
                } else if (mISO8583Domain[n].mFlag == LLLLVAR_LEN) {
                    temp = new byte[4];
                    System.arraycopy(srcData, srcDataOffset, temp, 0, 4);
                    String lenSrc = new String(temp);
                    len = Integer.parseInt(lenSrc, 10); // .valueOf(lenSrc);
                    srcDataOffset += 4;
                } else if (mISO8583Domain[n].mFlag == FIX_LEN) {
                    len = mISO8583Domain[n].mMaxLength;
                }
                mISO8583Domain[n].mLength = len;
                mISO8583Domain[n].mStartAddr = offset;
                if (len + offset >= MAXBUFFERLEN) {
                    return -1;
                }

                byte[] buf = new byte[len];
                if (mISO8583Domain[n].mType == L_BCD) {
                    len = (len + 1) / 2;
                    System.arraycopy(srcData, srcDataOffset, mDataBuffer, offset, len);
                    System.arraycopy(srcData, srcDataOffset, buf, 0, len);
                } else if (mISO8583Domain[n].mType == L_ASC || mISO8583Domain[n].mType == D_BIN) {
                    System.arraycopy(srcData, srcDataOffset, mDataBuffer, offset, len);
                    System.arraycopy(srcData, srcDataOffset, buf, 0, len);
                } else if (mISO8583Domain[n].mType == R_BCD) {
                    len = (len + 1) / 2;
                    System.arraycopy(srcData, srcDataOffset, mDataBuffer, offset, len);
                    System.arraycopy(srcData, srcDataOffset, buf, 0, len);
                } else if (mISO8583Domain[n].mType == R_ASC) {
                    try {
                        System.arraycopy(srcData, srcDataOffset, mDataBuffer, offset, len);
                        System.arraycopy(srcData, srcDataOffset, buf, 0, len);
                    } catch (Exception e) {
                        if (n == 52) {
                            buf = new byte[32];
                            System.arraycopy(srcData, srcDataOffset, mDataBuffer, offset, 32);
                            System.arraycopy(srcData, srcDataOffset, buf, 0, 32);
                        }
                    }
                }
                mISO8583Domain[n].mBitf = 1;
                offset += len;
                srcDataOffset += len;
            }
        }
        mOffset = offset;
        return 0;
    }

    public int setBit(int n, byte[] src, int len) {
        int i = 0, l = 0;
        byte[] pt = null;
        byte[] tmp = new byte[MAXBUFFERLEN];
        if (len == 0 || len > MAXBUFFERLEN) {
            return 0;
        }
        if (n == 0) {
            System.arraycopy(src, 0, mMessageId, 0, MSGIDLEN);
            return 0;
        }
        if (n <= 1 || n > ISO8583_MAX_LENGTH) {
            return -1;
        }
        n--;
        if (len > mISO8583Domain[n].mMaxLength) {
            len = mISO8583Domain[n].mMaxLength;
        }

        l = len;
        if (mISO8583Domain[n].mFlag == FIX_LEN) {
            len = mISO8583Domain[n].mMaxLength;
        }

        mISO8583Domain[n].mBitf = 1;
        mISO8583Domain[n].mLength = len;
        mISO8583Domain[n].mStartAddr = mOffset;
        if ((mOffset + len) >= MAXBUFFERLEN) {
            return -1;
        }
        if (mISO8583Domain[n].mType == L_BCD) {
            System.arraycopy(src, 0, tmp, 0, l);
            Arrays.fill(tmp, l, len, (byte) ' ');
            pt = GenericConverter.fromASCIIToBCD(tmp, 0, len, false);
            System.arraycopy(pt, 0, mDataBuffer, mOffset, (len + 1) / 2);
            mOffset += (len + 1) / 2;
        } else if (mISO8583Domain[n].mType == L_ASC) {
            System.arraycopy(src, 0, tmp, 0, l);
            Arrays.fill(tmp, l, len, (byte) ' ');
            System.arraycopy(tmp, 0, mDataBuffer, mOffset, len);
            mOffset += len;
        } else if (mISO8583Domain[n].mType == R_BCD) {
            Arrays.fill(tmp, 0, len - l, (byte) '0');
            System.arraycopy(src, 0, tmp, len - l, l);
            pt = GenericConverter.fromASCIIToBCD(tmp, 0, len, true);
            System.arraycopy(pt, 0, mDataBuffer, mOffset, (len + 1) / 2);
            mOffset += (len + 1) / 2;
        } else if (mISO8583Domain[n].mType == R_ASC) {
            Arrays.fill(tmp, 0, len - l, (byte) ' ');
            //Log.i(TAG, "R_ASC 1: " + new String(tmp));
            System.arraycopy(src, 0, tmp, len - l, l);
            //Log.i(TAG, "R_ASC 2: " + new String(src));
            System.arraycopy(tmp, 0, mDataBuffer, mOffset, len);
            //Log.i(TAG, "R_ASC 3: " + new String(tmp));
            mOffset += len;
        } else if (mISO8583Domain[n].mType == D_BIN) {
            Log.i(TAG, "  l = " + l);
            System.arraycopy(src, 0, tmp, 0, l);
            Arrays.fill(tmp, l, len, (byte) ' ');
            pt = GenericConverter.fromASCIIToBCD(tmp, 0, len, false);
            System.arraycopy(pt, 0, mDataBuffer, mOffset, (len + 1) / 2);
            mOffset += (len + 1) / 2;
        }
        return 0;
    }

    public byte[] getBit(int n) {
        byte[] domainValue = null;
        if (n == 0) {
            domainValue = new byte[MSGIDLEN];
            System.arraycopy(mMessageId, 0, domainValue, 0, MSGIDLEN);
            //Log.i(TAG, "getBit, n=0," + new String(domainValue));
            return domainValue;
        }

        if (n <= 1 || n > ISO8583_MAX_LENGTH) {
            return null;
        }

        n--;
        if (mISO8583Domain[n].mBitf == 0) {
            return null;
        }

        int len = mISO8583Domain[n].mLength;
        int startAddr = mISO8583Domain[n].mStartAddr;
        byte[] data = null;

        if (mISO8583Domain[n].mType == L_BCD) {
            data = new byte[(len + 1) / 2];
            System.arraycopy(mDataBuffer, startAddr, data, 0, (len + 1) / 2);
            domainValue = GenericConverter.fromBCDToASCII(data, 0, len, false);
        } else if (mISO8583Domain[n].mType == L_ASC || mISO8583Domain[n].mType == D_BIN) {
            data = new byte[len];
            System.arraycopy(mDataBuffer, startAddr, data, 0, len);
            domainValue = data;
        } else if (mISO8583Domain[n].mType == R_BCD) {
            data = new byte[(len + 1) / 2];
            System.arraycopy(mDataBuffer, startAddr, data, 0, (len + 1) / 2);
            domainValue = GenericConverter.fromBCDToASCII(data, 0, len, true);
        } else if (mISO8583Domain[n].mType == R_ASC) {
            data = new byte[len];
            System.arraycopy(mDataBuffer, startAddr, data, 0, len);
            domainValue = data;
        }
        return domainValue;
    }

    public void clearBit() {
        for (int i = 0; i < ISO8583_MAX_LENGTH; i++) {
            mISO8583Domain[i].mBitf = 0;
            mISO8583Domain[i].mLength = 0;
            mISO8583Domain[i].mStartAddr = 0;
        }
        mOffset = 0;
        Arrays.fill(mDataBuffer, (byte) 0);
    }

    private void initCupISO8583Domain() {
        if (mISO8583Domain == null) {
            return;
        }

        mISO8583Domain[0].setDomainProperty(8, L_ASC, FIX_LEN, "BITMAP");    //1
        mISO8583Domain[1].setDomainProperty(19, R_ASC, LLVAR_LEN, "PAN");        //2
        mISO8583Domain[2].setDomainProperty(6, R_ASC, FIX_LEN, "TRANS PROC CODE");    //3
        mISO8583Domain[3].setDomainProperty(12, R_ASC, FIX_LEN, "AMOUNT");//4
        mISO8583Domain[6].setDomainProperty(10, R_ASC, FIX_LEN, "TRANS DATE TIME"); //7
        mISO8583Domain[10].setDomainProperty(6, R_ASC, FIX_LEN, "STAN");        //11
        mISO8583Domain[11].setDomainProperty(6, R_ASC, FIX_LEN, "TIME");    //12
        mISO8583Domain[12].setDomainProperty(4, R_ASC, FIX_LEN, "DATE");    //13
        mISO8583Domain[13].setDomainProperty(4, R_ASC, FIX_LEN, "EXPIRY DATE");    //14
        mISO8583Domain[14].setDomainProperty(4, R_ASC, FIX_LEN, "DATE SETTLEMENT");//15
        mISO8583Domain[17].setDomainProperty(4, R_ASC, FIX_LEN, "MCC");        //18
        mISO8583Domain[21].setDomainProperty(3, R_ASC, FIX_LEN, "POS ENTRY CODE"); //22
        mISO8583Domain[22].setDomainProperty(3, R_ASC, FIX_LEN, "CARD SEQ NUM");//23
        mISO8583Domain[24].setDomainProperty(2, R_ASC, FIX_LEN, "POS CON CODE");//25
        mISO8583Domain[25].setDomainProperty(2, R_ASC, FIX_LEN, "POS PIN CAPTURE CODE");//26
        mISO8583Domain[27].setDomainProperty(9, R_ASC, FIX_LEN, "AMOUNT TRANSACTION FEE");//28
        //mISO8583Domain[29].setDomainProperty(  9, R_ASC, LLVAR_LEN,			"AMOUNT TRANSACTION PROCESSING FEE");//30
        mISO8583Domain[29].setDomainProperty(9, R_ASC, FIX_LEN, "AMOUNT TRANSACTION PROCESSING FEE");//30
        mISO8583Domain[31].setDomainProperty(9, R_ASC, LLVAR_LEN, "ACQ INSTI CODE");    //32
        mISO8583Domain[32].setDomainProperty(9, R_ASC, LLVAR_LEN, "FORWARDING INSTI CODE"); //33
        mISO8583Domain[34].setDomainProperty(37, R_ASC, LLVAR_LEN, "TRACK 2 DATA");        //35
        mISO8583Domain[36].setDomainProperty(12, R_ASC, FIX_LEN, "RRN");        //37
        mISO8583Domain[37].setDomainProperty(6, R_ASC, FIX_LEN, "AUTH CODE");//38 +
        mISO8583Domain[38].setDomainProperty(2, R_ASC, FIX_LEN, "RESPONSE CODE");//39
        mISO8583Domain[39].setDomainProperty(3, R_ASC, FIX_LEN, "SERVICE RES CODE");//40
        mISO8583Domain[40].setDomainProperty(8, R_ASC, FIX_LEN, "TID");    //41
        mISO8583Domain[41].setDomainProperty(15, R_ASC, FIX_LEN, "MID");    //42
        mISO8583Domain[42].setDomainProperty(40, R_ASC, FIX_LEN, "MNL");        //43
        mISO8583Domain[47].setDomainProperty(999, R_ASC, LLLVAR_LEN, "ADDITIONAL DATA");    //48
        mISO8583Domain[48].setDomainProperty(3, R_ASC, FIX_LEN, "CURRENCY CODE");//49
        mISO8583Domain[51].setDomainProperty(16, R_ASC, FIX_LEN, "PINBLOCK");    //52
        mISO8583Domain[52].setDomainProperty(96, R_ASC, FIX_LEN, "SECURITY RELATED INFO");//53//96
        mISO8583Domain[53].setDomainProperty(120, R_ASC, LLLVAR_LEN, "ADDITIONAL AMOUNTS");//54
        mISO8583Domain[54].setDomainProperty(510, R_ASC, LLLVAR_LEN, "ICC");        //55
        mISO8583Domain[55].setDomainProperty(4, R_ASC, LLLVAR_LEN, "MESSAGE REASON CODE");    //56
        mISO8583Domain[58].setDomainProperty(255, R_ASC, LLLVAR_LEN, "ECHO DATA");//59
        mISO8583Domain[59].setDomainProperty(999, R_ASC, LLLVAR_LEN, "PAYMENT INFO"); //60
        mISO8583Domain[61].setDomainProperty(999, R_ASC, LLLVAR_LEN, "MANAGEMENT INFO 1");//62
        mISO8583Domain[62].setDomainProperty(9999, R_ASC, LLLLVAR_LEN, "MANAGEMENT INFO 2"); //63
        mISO8583Domain[63].setDomainProperty(64, R_ASC, FIX_LEN, "(MAC)");//64
        mISO8583Domain[89].setDomainProperty(42, R_ASC, FIX_LEN, "ORIGINAL DATA ELEMENTS");//90
        mISO8583Domain[94].setDomainProperty(42, R_ASC, FIX_LEN, "REPLACEMENT AMOUNT");//95
        mISO8583Domain[101].setDomainProperty(28, R_ASC, LLVAR_LEN, "ACCOUNT IDENT 1");  //102
        mISO8583Domain[102].setDomainProperty(28, R_ASC, LLVAR_LEN, "ACCOUNT IDENT 2"); //103
        mISO8583Domain[122].setDomainProperty(999, R_ASC, LLLVAR_LEN, "POS DATA CODE");     //123
        mISO8583Domain[123].setDomainProperty(9999, R_ASC, LLLLVAR_LEN, "NEAR FIELD COMM");  //124
        mISO8583Domain[127].setDomainProperty(64, R_ASC, FIX_LEN, "SECONDARY MAC");        //128
    }
}


