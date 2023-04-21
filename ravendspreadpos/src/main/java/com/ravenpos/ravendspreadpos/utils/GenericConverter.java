package com.ravenpos.ravendspreadpos.utils;

public class GenericConverter {
    private static final String TAG = GenericConverter.class.getSimpleName();
    public final static byte ALPHA_A_ASCII_VALUE = 0x41;
    public final static byte ALPHA_a_ASCII_VALUE = 0x61;
    public final static byte DIGITAL_0_ASCII_VALUE = 0x30;

    private GenericConverter() {
    }

    public static byte[] hexStringToBytes(String s) {
        byte[] ret;

        if (s == null) return null;

        int sz = s.length();

        char c;
        for (int i = 0; i < sz; ++i) {
            c = s.charAt(i);
            if (!((c >= '0') && (c <= '9'))
                    && !((c >= 'A') && (c <= 'F'))
                    && !((c >= 'a') && (c <= 'f'))) {
                s = s.replaceAll("[^[0-9][A-F][a-f]]", "");
                sz = s.length();
                break;
            }
        }

        ret = new byte[sz / 2];

        for (int i = 0; i < sz - 1; i += 2) {
            ret[i / 2] = (byte) ((hexCharToInt(s.charAt(i)) << 4)
                    | hexCharToInt(s.charAt(i + 1)));
        }

        return ret;
    }


    static int hexCharToInt(char c) {
        if (c >= '0' && c <= '9') return (c - '0');
        if (c >= 'A' && c <= 'F') return (c - 'A' + 10);
        if (c >= 'a' && c <= 'f') return (c - 'a' + 10);

        throw new RuntimeException("invalid hex char '" + c + "'");
    }

    public static byte[] fromBCDToASCII(byte[] bcdBuf, int bcdOffset, int asciiLen, boolean rightAlignFlag) {
        byte[] asciiBuf = new byte[asciiLen];
        fromBCDToASCII(bcdBuf, bcdOffset, asciiBuf, 0, asciiLen, rightAlignFlag);

        return asciiBuf;
    }


    public static void fromBCDToASCII(byte[] bcdBuf, int bcdOffset, byte[] asciiBuf, int asciiOffset, int asciiLen,
                                      boolean rightAlignFlag) {
        int cnt;

        if (((asciiLen & 1) == 1) && rightAlignFlag) {
            cnt = 1;
            asciiLen++;
        } else {
            cnt = 0;
        }

        for (; cnt < asciiLen; cnt++, asciiOffset++) {
            asciiBuf[asciiOffset] = (byte) ((((cnt) & 1) == 1) ? (bcdBuf[bcdOffset++] & 0x0f)
                    : ((bcdBuf[bcdOffset] >> 4) & 0x0f));
            asciiBuf[asciiOffset] = (byte) (asciiBuf[asciiOffset] + ((asciiBuf[asciiOffset] > 9) ? (ALPHA_A_ASCII_VALUE - 10)
                    : DIGITAL_0_ASCII_VALUE));
        }
    }

    public static void fromASCIIToBCD(byte[] asciiBuf, int asciiOffset, int asciiLen, byte[] bcdBuf, int bcdOffset,
                                      boolean rightAlignFlag) {
        int cnt;
        byte ch, ch1;

        if (((asciiLen & 1) == 1) && rightAlignFlag) {
            ch1 = 0;
        } else {
            ch1 = 0x55;
        }

        for (cnt = 0; cnt < asciiLen; cnt++, asciiOffset++) {
            if (asciiBuf[asciiOffset] >= ALPHA_a_ASCII_VALUE)
                ch = (byte) (asciiBuf[asciiOffset] - ALPHA_a_ASCII_VALUE + 10);
            else if (asciiBuf[asciiOffset] >= ALPHA_A_ASCII_VALUE)
                ch = (byte) (asciiBuf[asciiOffset] - ALPHA_A_ASCII_VALUE + 10);
            else if (asciiBuf[asciiOffset] >= DIGITAL_0_ASCII_VALUE)
                ch = (byte) (asciiBuf[asciiOffset] - DIGITAL_0_ASCII_VALUE);
            else
                ch = 0x00;

            if (ch1 == 0x55)
                ch1 = ch;
            else {
                bcdBuf[bcdOffset] = (byte) (ch1 << 4 | ch);
                bcdOffset++;
                ch1 = 0x55;
            }
        }

        if (ch1 != 0x55)
            bcdBuf[bcdOffset] = (byte) (ch1 << 4);
    }

    public static byte[] fromASCIIToBCD(byte[] asciiBuf, int asciiOffset, int asciiLen, boolean rightAlignFlag) {
        byte[] bcdBuf = new byte[(asciiLen + 1) / 2];
        fromASCIIToBCD(asciiBuf, asciiOffset, asciiLen, bcdBuf, 0, rightAlignFlag);
        return bcdBuf;
    }
}

