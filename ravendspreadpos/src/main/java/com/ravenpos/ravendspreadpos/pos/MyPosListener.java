package com.ravenpos.ravendspreadpos.pos;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;

import com.chaos.view.PinView;
import com.dspread.xpos.CQPOSService;
import com.dspread.xpos.QPOSService;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.pnsol.sdk.miura.emv.EmvTags;
import com.pnsol.sdk.miura.emv.tlv.Tag;
import com.ravenpos.ravendspreadpos.R;
import com.ravenpos.ravendspreadpos.utils.EmvCardData;
import com.ravenpos.ravendspreadpos.utils.MyCallBackListener;
import com.ravenpos.ravendspreadpos.utils.TransactionListener;
import com.ravenpos.ravendspreadpos.utils.TransactionType;
import com.ravenpos.ravendspreadpos.utils.utils.DUKPK2009_CBC;
import com.ravenpos.ravendspreadpos.utils.utils.ParseASN1Util;
import com.ravenpos.ravendspreadpos.utils.utils.QPOSUtil;
import com.ravenpos.ravendspreadpos.utils.utils.TRACE;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;

import Decoder.BASE64Decoder;
import Decoder.BASE64Encoder;

/**
 * Created by ADEOLU on 3/13/2018.
 */

public class MyPosListener extends CQPOSService implements MyCallBackListener {
    private static QPOSService pos;
    private static TransactionListener listener;
    private final String cashBackAmount = "";
    private final String currencyCode = "566";
    private Context context;
    private boolean isPinCanceled = false;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1001;

    private String deviceSignCert;
    private String amount;
    private String terminalTime;
    private final int timeout = 30;
    private Hashtable<String, String> tagList;
    private String PINBLOCK, KSN, CARDHOLDERNAME, ICCDATA, ID;
    private EmvCardData emvCardData;
    private static  MyPosListener myPosListener;
    private TransactionType transactionType;
    private TransactionResponse transactionResponse;
    private POS_TYPE posType = POS_TYPE.BLUETOOTH;

    private MutableLiveData<String> messageCarrier;
    private String nfcLog = "";

    private BottomSheetDialog pinDialog;
    private PinView txtUserPin;

    private Activity activity;
    private void initializeSheet(Context context){
        pinDialog = new BottomSheetDialog(context);
        pinDialog.setContentView(R.layout.transaction_pinview);
        txtUserPin = pinDialog.findViewById(R.id.txtTaxPayerIdPIn);
        assert txtUserPin != null;
    }

    public void startTransaction(Activity activity, String bluetoothAddress, Double amount, TransactionType transactionType, TransactionResponse transactionResponse, MutableLiveData<String> listenerMutable, TransactionListener vlistener){
        this.amount = String.valueOf(amount.intValue());
        this.transactionType = transactionType;
        this.transactionResponse = transactionResponse;
        listener = vlistener;
        messageCarrier = listenerMutable;
        this.activity = activity;
        pos.connectBluetoothDevice(true, timeout, bluetoothAddress);
    }
    private MyPosListener(Context context){
        pos = QPOSService.getInstance(QPOSService.CommunicationMode.BLUETOOTH);
        pos.setConext(context);
        pos.initListener(new Handler(Looper.myLooper()), this);
        this.context = context;
        initializeSheet(context);
    }
    public static MyPosListener initialize(Context context){
        return myPosListener == null ? new MyPosListener(context) : myPosListener;
    }


        @Override
        public void onRequestWaitingUser() {
            TRACE.d("onRequestWaitingUser()");
            messageCarrier.postValue(context.getString(R.string.waiting_for_card));
        }

        @Override
        public void onDoTradeResult(QPOSService.DoTradeResult result, Hashtable<String, String> decodeData) {
            TRACE.d("(DoTradeResult result, Hashtable<String, String> decodeData) " + result.toString() + TRACE.NEW_LINE + "decodeData:" + decodeData);
            String cardNo = "";
            if (result == QPOSService.DoTradeResult.NONE) {
                messageCarrier.postValue(context.getString(R.string.no_card_detected));
            } else if (result == QPOSService.DoTradeResult.TRY_ANOTHER_INTERFACE) {
                messageCarrier.postValue(context.getString(R.string.try_another_interface));
            } else if (result == QPOSService.DoTradeResult.ICC) {
                //messageCarrier.postValue(context.getString(R.string.icc_card_inserted));
                TRACE.d("EMV ICC Start");
                pos.doEmvApp(QPOSService.EmvOption.START);
            } else if (result == QPOSService.DoTradeResult.NOT_ICC) {
                messageCarrier.postValue(context.getString(R.string.card_inserted));
            } else if (result == QPOSService.DoTradeResult.BAD_SWIPE) {
                messageCarrier.postValue(context.getString(R.string.bad_swipe));
            } else if (result == QPOSService.DoTradeResult.CARD_NOT_SUPPORT) {
                messageCarrier.postValue("GPO NOT SUPPORT");
            } else if (result == QPOSService.DoTradeResult.PLS_SEE_PHONE) {
                messageCarrier.postValue("PLS SEE PHONE");
            } else if (result == QPOSService.DoTradeResult.MCR) {//Magnetic card
                String content = context.getString(R.string.card_swiped);
                String formatID = decodeData.get("formatID");
                if (formatID.equals("31") || formatID.equals("40") || formatID.equals("37") || formatID.equals("17") || formatID.equals("11") || formatID.equals("10")) {
                    String maskedPAN = decodeData.get("maskedPAN");
                    String expiryDate = decodeData.get("expiryDate");
                    String cardHolderName = decodeData.get("cardholderName");
                    String serviceCode = decodeData.get("serviceCode");
                    String trackblock = decodeData.get("trackblock");
                    String psamId = decodeData.get("psamId");
                    String posId = decodeData.get("posId");
                    String pinblock = decodeData.get("pinblock");
                    String macblock = decodeData.get("macblock");
                    String activateCode = decodeData.get("activateCode");
                    String trackRandomNumber = decodeData.get("trackRandomNumber");
                    content += formatID + "\n";
                    content +=maskedPAN + "\n";
                    content +=  expiryDate + "\n";
                    content += cardHolderName + "\n";
                    content += serviceCode + "\n";
                    content += "trackblock: " + trackblock + "\n";
                    content += "psamId: " + psamId + "\n";
                    content += "posId: " + posId + "\n";
                    content +=  pinblock + "\n";
                    content += "macblock: " + macblock + "\n";
                    content += "activateCode: " + activateCode + "\n";
                    content += "trackRandomNumber: " + trackRandomNumber + "\n";
                    cardNo = maskedPAN;
                } else if (formatID.equals("FF")) {
                    String type = decodeData.get("type");
                    String encTrack1 = decodeData.get("encTrack1");
                    String encTrack2 = decodeData.get("encTrack2");
                    String encTrack3 = decodeData.get("encTrack3");
                    content += "cardType:" + " " + type + "\n";
                    content += "track_1:" + " " + encTrack1 + "\n";
                    content += "track_2:" + " " + encTrack2 + "\n";
                    content += "track_3:" + " " + encTrack3 + "\n";
                } else {
                    String orderID = decodeData.get("orderId");
                    String maskedPAN = decodeData.get("maskedPAN");
                    String expiryDate = decodeData.get("expiryDate");
                    String cardHolderName = decodeData.get("cardholderName");
//					String ksn = decodeData.get("ksn");
                    String serviceCode = decodeData.get("serviceCode");
                    String track1Length = decodeData.get("track1Length");
                    String track2Length = decodeData.get("track2Length");
                    String track3Length = decodeData.get("track3Length");
                    String encTracks = decodeData.get("encTracks");
                    String encTrack1 = decodeData.get("encTrack1");
                    String encTrack2 = decodeData.get("encTrack2");
                    String encTrack3 = decodeData.get("encTrack3");
                    String partialTrack = decodeData.get("partialTrack");
                    String pinKsn = decodeData.get("pinKsn");
                    String trackksn = decodeData.get("trackksn");
                    String pinBlock = decodeData.get("pinBlock");
                    String encPAN = decodeData.get("encPAN");
                    String trackRandomNumber = decodeData.get("trackRandomNumber");
                    String pinRandomNumber = decodeData.get("pinRandomNumber");
                    if (orderID != null && !"".equals(orderID)) {
                        content += "orderID:" + orderID;
                    }
                    content += formatID + "\n";
                    content += maskedPAN + "\n";
                    content += expiryDate + "\n";
                    content += cardHolderName + "\n";
//					content += getString(R.string.ksn) + " " + ksn + "\n";
                    content +=  "\n";
                    content += trackksn + "\n";
                    content +=  serviceCode + "\n";
                    content += track1Length + "\n";
                    content += track2Length + "\n";
                    content +=track3Length + "\n";
                    content +=  encTracks + "\n";
                    content +=  encTrack1 + "\n";
                    content +=encTrack2 + "\n";
                    content +=  encTrack3 + "\n";
                    content +=  partialTrack + "\n";
                    content += pinBlock + "\n";
                    content += "encPAN: " + encPAN + "\n";
                    content += "trackRandomNumber: " + trackRandomNumber + "\n";
                    content += "pinRandomNumber:" + " " + pinRandomNumber + "\n";
                    cardNo = maskedPAN;
                    String realPan = null;
                    if (!TextUtils.isEmpty(trackksn) && !TextUtils.isEmpty(encTrack2)) {
                        String clearPan = DUKPK2009_CBC.getDate(trackksn, encTrack2, DUKPK2009_CBC.Enum_key.DATA, DUKPK2009_CBC.Enum_mode.CBC);
                        content += "encTrack2:" + " " + clearPan + "\n";
                        realPan = clearPan.substring(0, maskedPAN.length());
                        content += "realPan:" + " " + realPan + "\n";
                    }
                    if (!TextUtils.isEmpty(pinKsn) && !TextUtils.isEmpty(pinBlock) && !TextUtils.isEmpty(realPan)) {
                        String date = DUKPK2009_CBC.getDate(pinKsn, pinBlock, DUKPK2009_CBC.Enum_key.PIN, DUKPK2009_CBC.Enum_mode.CBC);
                        String parsCarN = "0000" + realPan.substring(realPan.length() - 13, realPan.length() - 1);
                        String s = DUKPK2009_CBC.xor(parsCarN, date);
                        content += "PIN:" + " " + s + "\n";
                    }
                }
                messageCarrier.postValue(content);
            } else if ((result == QPOSService.DoTradeResult.NFC_ONLINE) || (result == QPOSService.DoTradeResult.NFC_OFFLINE)) {
                nfcLog = decodeData.get("nfcLog");
                String content = context.getString(R.string.tap_card);
                String formatID = decodeData.get("formatID");
                if (formatID.equals("31") || formatID.equals("40")
                        || formatID.equals("37") || formatID.equals("17")
                        || formatID.equals("11") || formatID.equals("10")) {
                    String maskedPAN = decodeData.get("maskedPAN");
                    String expiryDate = decodeData.get("expiryDate");
                    String cardHolderName = decodeData.get("cardholderName");
                    String serviceCode = decodeData.get("serviceCode");
                    String trackblock = decodeData.get("trackblock");
                    String psamId = decodeData.get("psamId");
                    String posId = decodeData.get("posId");
                    String pinblock = decodeData.get("pinblock");
                    String macblock = decodeData.get("macblock");
                    String activateCode = decodeData.get("activateCode");
                    String trackRandomNumber = decodeData
                            .get("trackRandomNumber");

                    content +=formatID
                            + "\n";
                    content +=  maskedPAN
                            + "\n";
                    content +=" "
                            + expiryDate + "\n";
                    content += " "
                            + cardHolderName + "\n";

                    content +=  " "
                            + serviceCode + "\n";
                    content += "trackblock: " + trackblock + "\n";
                    content += "psamId: " + psamId + "\n";
                    content += "posId: " + posId + "\n";
                    content +=  pinblock
                            + "\n";
                    content += "macblock: " + macblock + "\n";
                    content += "activateCode: " + activateCode + "\n";
                    content += "trackRandomNumber: " + trackRandomNumber + "\n";
                    cardNo = maskedPAN;
                } else {
                    String maskedPAN = decodeData.get("maskedPAN");
                    String expiryDate = decodeData.get("expiryDate");
                    String cardHolderName = decodeData.get("cardholderName");
                    String serviceCode = decodeData.get("serviceCode");
                    String track1Length = decodeData.get("track1Length");
                    String track2Length = decodeData.get("track2Length");
                    String track3Length = decodeData.get("track3Length");
                    String encTracks = decodeData.get("encTracks");
                    String encTrack1 = decodeData.get("encTrack1");
                    String encTrack2 = decodeData.get("encTrack2");
                    String encTrack3 = decodeData.get("encTrack3");
                    String partialTrack = decodeData.get("partialTrack");
                    String pinKsn = decodeData.get("pinKsn");
                    String trackksn = decodeData.get("trackksn");
                    String pinBlock = decodeData.get("pinBlock");
                    String encPAN = decodeData.get("encPAN");
                    String trackRandomNumber = decodeData
                            .get("trackRandomNumber");
                    String pinRandomNumber = decodeData.get("pinRandomNumber");

                    content += formatID
                            + "\n";
                    content += maskedPAN
                            + "\n";
                    content += " "
                            + expiryDate + "\n";
                    content += " "
                            + cardHolderName + "\n";
                    content += pinKsn + "\n";
                    content += trackksn
                            + "\n";
                    content += " "
                            + serviceCode + "\n";
                    content += " "
                            + track1Length + "\n";
                    content +=  " "
                            + track2Length + "\n";
                    content += " "
                            + track3Length + "\n";
                    content += " "
                            + encTracks + "\n";
                    content +=  " "
                            + encTrack1 + "\n";
                    content +=  " "
                            + encTrack2 + "\n";
                    content += " "
                            + encTrack3 + "\n";
                    content +=partialTrack + "\n";
                    content += pinBlock
                            + "\n";
                    content += "encPAN: " + encPAN + "\n";
                    content += "trackRandomNumber: " + trackRandomNumber + "\n";
                    content += "pinRandomNumber:" + " " + pinRandomNumber
                            + "\n";
                    cardNo = maskedPAN;
                }
                // pos.getICCTag(QPOSService.EncryptType.PLAINTEXT,1,1,"5F20") // get plaintext or ciphertext 5F20 tag
                // pos.getICCTag(QPOSService.EncryptType.PLAINTEXT,1,2,"5F205F24") // get plaintext or ciphertext 5F20 and 5F24 tag

               // messageCarrier.postValue(content);
               // sendMsg(8003);
            } else if ((result == QPOSService.DoTradeResult.NFC_DECLINED)) {
                messageCarrier.postValue(context.getString(R.string.transaction_declined));
            } else if (result == QPOSService.DoTradeResult.NO_RESPONSE) {
                messageCarrier.postValue(context. getString(R.string.card_no_response));
            }
        }

        @Override
        public void onQposInfoResult(Hashtable<String, String> posInfoData) {
            TRACE.d("onQposInfoResult" + posInfoData.toString());
            String isSupportedTrack1 = posInfoData.get("isSupportedTrack1") == null ? "" : posInfoData.get("isSupportedTrack1");
            String isSupportedTrack2 = posInfoData.get("isSupportedTrack2") == null ? "" : posInfoData.get("isSupportedTrack2");
            String isSupportedTrack3 = posInfoData.get("isSupportedTrack3") == null ? "" : posInfoData.get("isSupportedTrack3");
            String bootloaderVersion = posInfoData.get("bootloaderVersion") == null ? "" : posInfoData.get("bootloaderVersion");
            String firmwareVersion = posInfoData.get("firmwareVersion") == null ? "" : posInfoData.get("firmwareVersion");
            String isUsbConnected = posInfoData.get("isUsbConnected") == null ? "" : posInfoData.get("isUsbConnected");
            String isCharging = posInfoData.get("isCharging") == null ? "" : posInfoData.get("isCharging");
            String batteryLevel = posInfoData.get("batteryLevel") == null ? "" : posInfoData.get("batteryLevel");
            String batteryPercentage = posInfoData.get("batteryPercentage") == null ? ""
                    : posInfoData.get("batteryPercentage");
            String hardwareVersion = posInfoData.get("hardwareVersion") == null ? "" : posInfoData.get("hardwareVersion");
            String SUB = posInfoData.get("SUB") == null ? "" : posInfoData.get("SUB");
            String pciFirmwareVersion = posInfoData.get("PCI_firmwareVersion") == null ? ""
                    : posInfoData.get("PCI_firmwareVersion");
            String pciHardwareVersion = posInfoData.get("PCI_hardwareVersion") == null ? ""
                    : posInfoData.get("PCI_hardwareVersion");
            String content = "";
            content += bootloaderVersion + "\n";
            content += firmwareVersion + "\n";
            content +=isUsbConnected + "\n";
            content += isCharging + "\n";
            content += batteryLevel + "\n";
//			}else {
            content += batteryPercentage + "\n";
//			}
            content +=  hardwareVersion + "\n";
            content += "SUB : " + SUB + "\n";
            content += isSupportedTrack1 + "\n";
            content += isSupportedTrack2 + "\n";
            content +=  isSupportedTrack3 + "\n";
            content += "PCI FirmwareVresion:" + pciFirmwareVersion + "\n";
            content += "PCI HardwareVersion:" + pciHardwareVersion + "\n";
            //statusEditText.setText(content);
            listener.onCompleteTransaction(new TransactionResponse());
        }
        /**
         * @see QPOSService.QPOSServiceListener#onRequestTransactionResult(QPOSService.TransactionResult)
         */
        @Override
        public void onRequestTransactionResult(QPOSService.TransactionResult transactionResult) {
            TRACE.d("onRequestTransactionResult()" + transactionResult.toString());
            if (transactionResult == QPOSService.TransactionResult.CARD_REMOVED) {
             //   clearDisplay();
            }

            if (transactionResult == QPOSService.TransactionResult.APPROVED) {
                TRACE.d("TransactionResult.APPROVED");
               // String message = context32.getString(R.string.transaction_approved) + "\n" + context.getString(R.string.amount) + ": $" + amount + "\n";
//                if (!cashBackAmount.equals("")) {
//                    message += ": INR" + cashBackAmount;
//                }

                //TODO Processing
                        messageCarrier.postValue("Transaction Processing...");
                      listener.onCompleteTransaction(new TransactionResponse());
//                    deviceShowDisplay("APPROVED");
            } else if (transactionResult == QPOSService.TransactionResult.TERMINATED) {
             //   clearDisplay();
                messageCarrier.postValue(context. getString(R.string.transaction_terminated));
            } else if (transactionResult == QPOSService.TransactionResult.DECLINED) {
                messageCarrier.postValue(context.getString(R.string.transaction_declined));
                listener.onProcessingError(new RuntimeException(context.getString(R.string.transaction_declined)),101);
//                    deviceShowDisplay("DECLINED");
            } else if (transactionResult == QPOSService.TransactionResult.CANCEL) {
             //   clearDisplay();
                messageCarrier.postValue(context.getString(R.string.transaction_cancel));
            } else if (transactionResult == QPOSService.TransactionResult.CAPK_FAIL) {
                messageCarrier.postValue(context.getString(R.string.transaction_capk_fail));
            } else if (transactionResult == QPOSService.TransactionResult.NOT_ICC) {
                messageCarrier.postValue(context.getString(R.string.transaction_not_icc));
            } else if (transactionResult == QPOSService.TransactionResult.SELECT_APP_FAIL) {
                messageCarrier.postValue(context.getString(R.string.transaction_app_fail));
            } else if (transactionResult == QPOSService.TransactionResult.DEVICE_ERROR) {
                messageCarrier.postValue(context.getString(R.string.transaction_device_error));
            } else if (transactionResult == QPOSService.TransactionResult.TRADE_LOG_FULL) {
                messageCarrier.postValue("pls clear the trace log and then to begin do trade");
                messageCarrier.postValue("the trade log has fulled!pls clear the trade log!");
            } else if (transactionResult == QPOSService.TransactionResult.CARD_NOT_SUPPORTED) {
                messageCarrier.postValue(context.getString(R.string.card_not_supported));
            } else if (transactionResult == QPOSService.TransactionResult.MISSING_MANDATORY_DATA) {
                messageCarrier.postValue(context.getString(R.string.missing_mandatory_data));
            } else if (transactionResult == QPOSService.TransactionResult.CARD_BLOCKED_OR_NO_EMV_APPS) {
                messageCarrier.postValue(context.getString(R.string.card_blocked_or_no_evm_apps));
            } else if (transactionResult == QPOSService.TransactionResult.INVALID_ICC_DATA) {
                messageCarrier.postValue(context.getString(R.string.invalid_icc_data));
            } else if (transactionResult == QPOSService.TransactionResult.FALLBACK) {
                messageCarrier.postValue("trans fallback");
            } else if (transactionResult == QPOSService.TransactionResult.NFC_TERMINATED) {
              //  clearDisplay();
                messageCarrier.postValue("NFC Terminated");
            } else if (transactionResult == QPOSService.TransactionResult.CARD_REMOVED) {
               // clearDisplay();
                messageCarrier.postValue("CARD REMOVED");
            } else if (transactionResult == QPOSService.TransactionResult.CONTACTLESS_TRANSACTION_NOT_ALLOW) {
               // clearDisplay();
                messageCarrier.postValue("TRANS NOT ALLOW");
            } else if (transactionResult == QPOSService.TransactionResult.CARD_BLOCKED) {
               // clearDisplay();
                messageCarrier.postValue("CARD BLOCKED");
            } else if (transactionResult == QPOSService.TransactionResult.TRANS_TOKEN_INVALID) {
              //  clearDisplay();
                messageCarrier.postValue("TOKEN INVALID");
            }
        }

        @Override
        public void onRequestBatchData(String tlv) {
            TRACE.d("ICC trade finished");
            String content = context. getString(R.string.batch_data);
            TRACE.d("onRequestBatchData(String tlv):" + tlv);
            pos.getQposId();
        }

        @Override
        public void onRequestTransactionLog(String tlv) {
            TRACE.d("onRequestTransactionLog(String tlv):" + tlv);

            String content = context.getString(R.string.transaction_log);
            content += tlv;
           // statusEditText.setText(content);
        }

        @Override
        public void onQposIdResult(Hashtable<String, String> posIdTable) {
            TRACE.w("onQposIdResult():" + posIdTable.toString());
            String posId = posIdTable.get("posId") == null ? "" : posIdTable.get("posId");
            String csn = posIdTable.get("csn") == null ? "" : posIdTable.get("csn");
            String psamId = posIdTable.get("psamId") == null ? "" : posIdTable
                    .get("psamId");
            String NFCId = posIdTable.get("nfcID") == null ? "" : posIdTable
                    .get("nfcID");
            String content = "";
            content +=  posId + "\n";
            content += "csn: " + csn + "\n";
            content += "conn: " + pos.getBluetoothState() + "\n";
            content += "psamId: " + psamId + "\n";
            content += "NFCId: " + NFCId + "\n";
           // statusEditText.setText(content);
            listener.onCompleteTransaction(new TransactionResponse());
        }

        @Override
        public void onRequestSelectEmvApp(ArrayList<String> appList) {
            String s = "";
        }

        @Override
        public void onRequestSetAmount() {
            TRACE.d("input amount -- S");
            TRACE.d("onRequestSetAmount()");
            pos.setAmount(amount.concat("00"), cashBackAmount, currencyCode, QPOSService.TransactionType.PAYMENT);
            pos.setAmountIcon("N");
        }

        /**
         * @see QPOSService.QPOSServiceListener#onRequestIsServerConnected()
         */
        @Override
        public void onRequestIsServerConnected() {
            TRACE.d("onRequestIsServerConnected()");
            pos.isServerConnected(true);
        }

        @Override
        public void onRequestOnlineProcess(final String tlv) {
            TRACE.d("onRequestOnlineProcess" + tlv);
            tagList = getTags();
            ICCDATA = getICCTags();
            getPinBlockAndKsn(tlv);

            Hashtable<String, String> decodeData = pos.anlysEmvIccData(tlv);
            TRACE.d("anlysEmvIccData(tlv):" + decodeData.toString());

            if (isPinCanceled) {
                pos.sendOnlineProcessResult(null);
            } else {
                pos.sendOnlineProcessResult("8A023030");
              //  pos.sendOnlineProcessResult("8A023030" + tlv);
            }
//            pos.getICCTag(QPOSService.EncryptType.PLAINTEXT,0,1,"5F20") // get plaintext or ciphertext tag
//            pos.getICCTag(QPOSService.EncryptType.PLAINTEXT,0,2,"5F205F24") // get plaintext or ciphertext 5F20 and 5F24 tag
        }

        @Override
        public void onRequestTime() {
            TRACE.d("onRequestTime");
            pos.sendTime(terminalTime);
        }

        @Override
        public void onRequestDisplay(QPOSService.Display displayMsg) {
            TRACE.d("onRequestDisplay(Display displayMsg):" + displayMsg.toString());
            String msg = "";
            if (displayMsg == QPOSService.Display.CLEAR_DISPLAY_MSG) {
                msg = "";
            } else if (displayMsg == QPOSService.Display.MSR_DATA_READY) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Audio");
                builder.setMessage("Success,Contine ready");
                builder.setPositiveButton("Confirm", null);
                builder.show();
            } else if (displayMsg == QPOSService.Display.PLEASE_WAIT) {
                msg = context .getString(R.string.wait);
            } else if (displayMsg == QPOSService.Display.REMOVE_CARD) {
                msg = context .getString(R.string.remove_card);
            } else if (displayMsg == QPOSService.Display.TRY_ANOTHER_INTERFACE) {
                msg = context .getString(R.string.try_another_interface);
            } else if (displayMsg == QPOSService.Display.PROCESSING) {
                msg = context .getString(R.string.processing);

            } else if (displayMsg == QPOSService.Display.INPUT_PIN_ING) {
                msg = "please input pin on pos";

            } else if (displayMsg == QPOSService.Display.INPUT_OFFLINE_PIN_ONLY || displayMsg == QPOSService.Display.INPUT_LAST_OFFLINE_PIN) {
                msg = "please input offline pin on pos";

            } else if (displayMsg == QPOSService.Display.MAG_TO_ICC_TRADE) {
                msg = "please insert chip card on pos";
            } else if (displayMsg == QPOSService.Display.CARD_REMOVED) {
                msg = "card removed";
            }
            //TODO here -> Processsing
            messageCarrier.postValue(msg);
        }

        @Override
        public void onRequestFinalConfirm() {
            TRACE.d("onRequestFinalConfirm() ");
            TRACE.d("onRequestFinalConfirm - S");
           /*
            dismissDialog();
            if (!isPinCanceled) {
                dialog = new Dialog(MainActivity.this);
                dialog.setContentView(R.layout.confirm_dialog);
                dialog.setTitle(getString(R.string.confirm_amount));

                String message = getString(R.string.amount) + ": $" + amount;
                if (!cashbackAmount.equals("")) {
                    message += "\n" + getString(R.string.cashback_amount) + ": $" + cashbackAmount;
                }
                ((TextView) dialog.findViewById(R.id.messageTextView)).setText(message);
                dialog.findViewById(R.id.confirmButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pos.finalConfirm(true);
                        dialog.dismiss();
                    }
                });
                dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pos.finalConfirm(false);
                        dialog.dismiss();
                    }
                });
                dialog.show();
            } else {
                pos.finalConfirm(false);
            }
            */
        }

        @Override
        public void onRequestNoQposDetected() {
            TRACE.d("onRequestNoQposDetected()");
            listener.onProcessingError(new RuntimeException("Could not connect to the device"), 507);
        }

        @Override
        public void onRequestQposConnected() {
            TRACE.d("onRequestQposConnected()");
            startEmvTransaction();
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PERMISSION_GRANTED) {
                //申请权限
               // ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }

        @Override
        public void onRequestQposDisconnected() {
                TRACE.d("onRequestQposDisconnected()");

        }

        @Override
        public void onError(QPOSService.Error errorState) {
            TRACE.d("onError" + errorState.toString());

            String errorMessage = "";
            if (errorState == QPOSService.Error.CMD_NOT_AVAILABLE) {
                errorMessage = context.getString(R.string.command_not_available);
            } else if (errorState == QPOSService.Error.TIMEOUT) {
                errorMessage  = (context.getString(R.string.device_no_response));
            } else if (errorState == QPOSService.Error.DEVICE_RESET) {
                errorMessage  = (context.getString(R.string.device_reset));
            } else if (errorState == QPOSService.Error.UNKNOWN) {
                errorMessage  = (context.getString(R.string.unknown_error));
            } else if (errorState == QPOSService.Error.DEVICE_BUSY) {
                errorMessage  = (context.getString(R.string.device_busy));
            } else if (errorState == QPOSService.Error.INPUT_OUT_OF_RANGE) {
                errorMessage  = (context.getString(R.string.out_of_range));
            } else if (errorState == QPOSService.Error.INPUT_INVALID_FORMAT) {
                errorMessage  = (context.getString(R.string.invalid_format));
            } else if (errorState == QPOSService.Error.INPUT_ZERO_VALUES) {
                errorMessage  = (context.getString(R.string.zero_values));
            } else if (errorState == QPOSService.Error.INPUT_INVALID) {
                errorMessage  = (context.getString(R.string.input_invalid));
            } else if (errorState == QPOSService.Error.CASHBACK_NOT_SUPPORTED) {
                errorMessage  = (context.getString(R.string.cashback_not_supported));
            } else if (errorState == QPOSService.Error.CRC_ERROR) {
                errorMessage  = (context.getString(R.string.crc_error));
            } else if (errorState == QPOSService.Error.COMM_ERROR) {
                errorMessage  = (context.getString(R.string.comm_error));
            } else if (errorState == QPOSService.Error.MAC_ERROR) {
                errorMessage  = (context.getString(R.string.mac_error));
            } else if (errorState == QPOSService.Error.APP_SELECT_TIMEOUT) {
                errorMessage  = (context.getString(R.string.app_select_timeout_error));
            } else if (errorState == QPOSService.Error.CMD_TIMEOUT) {
                errorMessage  = (context.getString(R.string.cmd_timeout));
            } else if (errorState == QPOSService.Error.ICC_ONLINE_TIMEOUT) {
                if (pos == null) {
                    return;
                }
                pos.resetPosStatus();
              //  messageCarrier.postValue(context. getString(R.string.device_reset));
            }
            listener.onProcessingError(new RuntimeException(errorMessage),1001);
        }
        @Override
        public void onReturnReversalData(String tlv) {
            String content = context. getString(R.string.reversal_data);
            content += tlv;
            TRACE.d("onReturnReversalData(): " + tlv);
            //statusEditText.setText(content);
        }

        @Override
        public void onReturnupdateKeyByTR_31Result(boolean result, String keyType) {
            super.onReturnupdateKeyByTR_31Result(result, keyType);
            if (result) {
                //statusEditText.setText("send TR31 key success! The keyType is " + keyType);
            } else {
             //   statusEditText.setText("send TR31 key fail");
            }
        }

        @Override
        public void onReturnServerCertResult(String serverSignCert, String serverEncryptCert) {
            super.onReturnServerCertResult(serverSignCert, serverEncryptCert);
//            String pedkResponse = "[AOPEDK;ANY;KN0;KB30819D0201013181973081941370413031313242315458303045303130304B5331384646464630303030303030303031453030303030414332313038323435443442443733373445443932464142363838373438314544363034344137453635433239463132393739383931384441394434353631443235324143414641020102040AFFFF0000000001E00000020100130A4473707265616442444B04021D58;KA0D0B2F2F3178D4045C1363274890494664B23D32BABEA47E5DB42F15C06816107FD293BAFF7371119F0B11B685A29D40DE78D397F9629A56112629452A9525F5261F8BDCA168328C49ACCFF0133C90E91AFCCA1E18178EBBA5E0BFA054B09514BA87EE05F2E4837D2C74E00BFD3B14EB708598517F357F79AA34C89DFEA9F59B6D3CECABA6C211809400DE9D0B0CA09384FDD834B8BFD416C4B09D32B3F5E45001F18E5C3116A0FFD8E0C6ACE567FCCE1AC909FD038FB58F16BB32163866CD9DCB4B131A394757638111B2CF3DC968D58CBAA95279BEFF697C0D92C6A42248B53A3E56E595AD128EDB50710BDBFFCB113A7DC4ECBCE8668482CBFD22CD7B2E42;RDB077A03C07C94F161842AA0C4831E0EF;CE1;AP308205A906092A864886F70D010702A082059A30820596020149310D300B06096086480165030402013082032506092A864886F70D010703A082031604820312020100318201FB308201F70201003081A830819C310B3009060355040613025553310B300906035504080C0254583114301206035504070C0B53414E20414E544F4E494F31133011060355040A0C0A56697274754372797074311B3019060355040C0C12447370726561642044657669636573204341311B301906035504410C12447370726561642044657669636573204341311B301906035504030C1244737072656164204465766963657320434102074EB0D600009880304306092A864886F70D0101073036300B0609608648016503040201301806092A864886F70D010108300B0609608648016503040201300D06092A864886F70D01010904000482010092583A07F6280625EE4CA043E3245F2CD6CCA8BAE6E198F4046A5DDE055723D2591A84DDCA4D7F7BB1B179881FD9EC4E33ED22333A9008DAEB3C3B1D7143D1953F2363BEA4C0D2592667C3468F228F856A95A6DCA1FA9CA0AB05D25DC612E7E2BF2AE3012D22C78BB7224C8C8E02146929937C3DF9FA3589B2A486C132477ACFA50BE09528FCBFDA43079AF54C050843BE4BDE701D246D8D8A4C947F12AFD97A66010459BBAE4ED627F687CC3E6DC30B5B35FE3564D9FB07F501B57A73A70AB9C3398E14391B16A5FE45C374984219F0B3A3265A82D3F5A48CEEF3998DCEA59F1CC5821B51605C66C8FD2687778C84B51CCE51C1FBFA876F978E0A9546C425FF3082010C06092A864886F70D010701301406082A864886F70D03070408C8FA8F2094E103118081E85816DF38AEC7C0E569C011DB7212278A767C8934770C7E994E9508E256B693973FBB4B47A78A9F6B1AB2D326CC2A76A53E3731B8A8128B1DE4BEDCCA51E0E740C1A474C21C8CF4A4726F4FBE0DC5CE41C4DB7A2CDBB2EF7B2C0F61B50E34A1A327A5069EB23524DB0D8119C4C407B90277B806288ECAC2826AF8AF6D092B29E90C03554986F38345B6BB247BC1498C2185661BDE318ADECAF199E798D70A058305F686ECC3A267D28EED6052483401EB5B5B84F897CAEA7968B8EEAB23F465CE3F1E7F7F7E402D1AA681D76D34CF9EC0B6BBBE9A513B8C42E5EA5319E218AC996F87767966DBD8F8318202573082025302014930819C308190310B3009060355040613025553310B300906035504080C0254583114301206035504070C0B53414E20414E544F4E494F31133011060355040A0C0A5669727475437279707431173015060355040C0C0E44737072656164204B44482043413117301506035504410C0E44737072656164204B44482043413117301506035504030C0E44737072656164204B444820434102074EB0D60000987E300B0609608648016503040201A0818E301806092A864886F70D010903310B06092A864886F70D0107033020060A2A864886F70D01091903311204104CDCEDD916AAACEEAE548A1C5B0A0EAA301F06092A864886F70D0107013112041041303031364B30544E30304530303030302F06092A864886F70D01090431220420A0E06A133DA8D4A5EC5A2E51E468B470B19E13834019A0C2563BA39308660A1F300D06092A864886F70D0101010500048201003BA0F51DC5B3400E5CD29429663008713C3B61DE0C053590296421635218AEB228A1802C971B18CCF0A137D66FE07B08A0B2A592F11557CC401C353C859E1B82C4BAE146F8AC2955BD1326A3482B173E5589B321FBA0517DCA071F120D0940DC7B8CD33C861E1403CCBD7C3203F1609D261D38B415A0BF234CC9370D18B1004D89BE4C7C4631C7A5D3A1010F0371E25F70B8000D5B94C946571D0F6A730DEF57950AED18839B38B0FF6497D03E960194CF3F113C57575F62E8299FCDE855A1BD36ECE5CAF3DC9F942387A76A329715EC09FDBED3C4FACA06160D538EC00D0166D46152D61F6C665F749E91A0E70E532CE726525B946ACD81510FF47146F00994;]";
//            String KA = ParseASN1Util.parseToken(pedkResponse,"KA");
//            String AP = ParseASN1Util.parseToken(pedkResponse,"AP");
//            KB =  ParseASN1Util.parseToken(pedkResponse,"KB");
//            ParseASN1Util.getServerPubkey(serverSignCert);
//            String publickStr = ParseASN1Util.getPublicKey();
//            String raSe = ParseASN1Util.getPublicKey().substring(publickStr.length()-6);
//            publickStr = publickStr.substring(29);
//            PublicKey publicKey = QPOSUtil.getPublicKey(publickStr,raSe);
//            String signatureData = "";
//            boolean verifyResult = pos.verifySignWithPubKey(publicKey,QPOSUtil.HexStringToByteArray(KA),signatureData);
//            verifyResult = true;
//            if(verifyResult) {
//                ParseASN1Util.parseASN1new(AP.replace("A081", "3081"));
//                String nonce = ParseASN1Util.getNonce();
//                String header = ParseASN1Util.getHeader();
//                String digist = ParseASN1Util.getDigest();
//                String encryptData = ParseASN1Util.getEncryptData();
//                ParseASN1Util.parseASN1new(encryptData.substring(6));
//                String signData = ParseASN1Util.getSignData();
//                String encryptDataWith3des = ParseASN1Util.getEncryptDataWith3Des();
//                String IV = ParseASN1Util.getIVStr();
//                String clearData = "A0818e301806092a864886f70d010903310b06092a864886f70d0107033020060a2a864886f70d01091903311204104cdcedd916aaaceeae548a1c5b0a0eaa301f06092a864886f70d0107013112041041303031364b30544e30304530303030302f06092a864886f70d01090431220420a0e06a133da8d4a5ec5a2e51e468b470b19e13834019a0c2563ba39308660a1f";
//                String envelop = getDigitalEnvelopStr(encryptData,encryptDataWith3des,"01",clearData,signData,IV);
//                pos.loadSessionKeyByTR_34(envelop);
//            }else {
//                statusEditText.setText("PEDK signature verification failed.");
//            }
        }

        @Override
        public void onReturnGetPinResult(Hashtable<String, String> result) {
            TRACE.d("onReturnGetPinResult(Hashtable<String, String> result):" + result.toString());
            String pinBlock = result.get("pinBlock");
            String pinKsn = result.get("pinKsn");
            String content = "get pin result\n";
            content +=  pinKsn + "\n";
            content +=  pinBlock + "\n";
           // statusEditText.setText(content);
            TRACE.i(content);

            emvCardData = prepareTagsForOnlineProcess(ID);
            processRequestOnline(emvCardData);
        }

        @Override
        public void onReturnApduResult(boolean arg0, String arg1, int arg2) {
            TRACE.d("onReturnApduResult(boolean arg0, String arg1, int arg2):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2);
        }

        @Override
        public void onReturnPowerOffIccResult(boolean arg0) {
            TRACE.d("onReturnPowerOffIccResult(boolean arg0):" + arg0);
        }

        @Override
        public void onReturnPowerOnIccResult(boolean arg0, String arg1, String arg2, int arg3) {
            TRACE.d("onReturnPowerOnIccResult(boolean arg0, String arg1, String arg2, int arg3) :" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2 + TRACE.NEW_LINE + arg3);
            if (arg0) {
                pos.sendApdu("123456");
            }
        }

        @Override
        public void onReturnSetSleepTimeResult(boolean isSuccess) {
            TRACE.d("onReturnSetSleepTimeResult(boolean isSuccess):" + isSuccess);
            String content = "";
            if (isSuccess) {
                content = "set the sleep time success.";
            } else {
                content = "set the sleep time failed.";
            }
          //  statusEditText.setText(content);
        }

        @Override
        public void onGetCardNoResult(String cardNo) {
            TRACE.d("onGetCardNoResult(String cardNo):" + cardNo);
           // statusEditText.setText("cardNo: " + cardNo);
        }

        @Override
        public void onRequestCalculateMac(String calMac) {
            TRACE.d("onRequestCalculateMac(String calMac):" + calMac);
            if (calMac != null && !"".equals(calMac)) {
                calMac = QPOSUtil.byteArray2Hex(calMac.getBytes());
            }
           // statusEditText.setText("calMac: " + calMac);
            TRACE.d("calMac_result: calMac=> e: " + calMac);
        }

        @Override
        public void onRequestSignatureResult(byte[] arg0) {
            TRACE.d("onRequestSignatureResult(byte[] arg0):" + arg0.toString());
        }

        @Override
        public void onRequestUpdateWorkKeyResult(QPOSService.UpdateInformationResult result) {
            TRACE.d("onRequestUpdateWorkKeyResult(UpdateInformationResult result):" + result);
            if (result == QPOSService.UpdateInformationResult.UPDATE_SUCCESS) {
               // statusEditText.setText("update work key success");
            } else if (result == QPOSService.UpdateInformationResult.UPDATE_FAIL) {
               // statusEditText.setText("update work key fail");
            } else if (result == QPOSService.UpdateInformationResult.UPDATE_PACKET_VEFIRY_ERROR) {
              //  statusEditText.setText("update work key packet vefiry error");
            } else if (result == QPOSService.UpdateInformationResult.UPDATE_PACKET_LEN_ERROR) {
              //  statusEditText.setText("update work key packet len error");
            }
        }

        @Override
        public void onReturnCustomConfigResult(boolean isSuccess, String result) {
            TRACE.d("onReturnCustomConfigResult(boolean isSuccess, String result):" + isSuccess + TRACE.NEW_LINE + result);
          //  statusEditText.setText("result: " + isSuccess + "\ndata: " + result);
        }

        @Override
        public void onRequestSetPin() {
            TRACE.i("onRequestSetPin()");
            messageCarrier.postValue(context. getString(R.string.enter_pin));
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pinDialog.show();

                    txtUserPin.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            String pin = s.toString();
                            if (pin.length() == 4) {
                                pos.sendPin(pin);
                                pinDialog.dismiss();
                                txtUserPin.setText("");
                            }
                        }

                        @Override
                        public void afterTextChanged(Editable s) {

                        }
                    });
                }
            });

            /*
            isPinCanceled = true;
                    pos.cancelPin();
             */
        }

        @Override
        public void onReturnSetMasterKeyResult(boolean isSuccess) {
            TRACE.d("onReturnSetMasterKeyResult(boolean isSuccess) : " + isSuccess);
          //  statusEditText.setText("result: " + isSuccess);
        }

        @Override
        public void onReturnBatchSendAPDUResult(LinkedHashMap<Integer, String> batchAPDUResult) {
            TRACE.d("onReturnBatchSendAPDUResult(LinkedHashMap<Integer, String> batchAPDUResult):" + batchAPDUResult.toString());
            StringBuilder sb = new StringBuilder();
            sb.append("APDU Responses: \n");
            for (HashMap.Entry<Integer, String> entry : batchAPDUResult.entrySet()) {
                sb.append("[" + entry.getKey() + "]: " + entry.getValue() + "\n");
            }
         //   statusEditText.setText("\n" + sb.toString());
        }

        @Override
        public void onBluetoothBondFailed() {
            TRACE.d("onBluetoothBondFailed()");
            //statusEditText.setText("bond failed");
        }

        @Override
        public void onBluetoothBondTimeout() {
            TRACE.d("onBluetoothBondTimeout()");
          //  statusEditText.setText("bond timeout");
        }

        @Override
        public void onBluetoothBonded() {
            TRACE.d("onBluetoothBonded()");
          //  statusEditText.setText("bond success");
        }

        @Override
        public void onBluetoothBonding() {
            TRACE.d("onBluetoothBonding()");
           // statusEditText.setText("bonding .....");
        }

        @Override
        public void onReturniccCashBack(Hashtable<String, String> result) {
            TRACE.d("onReturniccCashBack(Hashtable<String, String> result):" + result.toString());
            String s = "serviceCode: " + result.get("serviceCode");
            s += "\n";
            s += "trackblock: " + result.get("trackblock");
          //  statusEditText.setText(s);
        }

        @Override
        public void onLcdShowCustomDisplay(boolean arg0) {
            TRACE.d("onLcdShowCustomDisplay(boolean arg0):" + arg0);
        }

        @Override
        public void onUpdatePosFirmwareResult(QPOSService.UpdateInformationResult arg0) {
            TRACE.d("onUpdatePosFirmwareResult(UpdateInformationResult arg0):" + arg0.toString());
         //   isUpdateFw = false;
            if (arg0 != QPOSService.UpdateInformationResult.UPDATE_SUCCESS) {
              //  updateThread.concelSelf();
            } else {
             //   mhipStatus.setText("");
            }
          //  statusEditText.setText("onUpdatePosFirmwareResult" + arg0.toString());
        }

        @Override
        public void onReturnDownloadRsaPublicKey(HashMap<String, String> map) {
            TRACE.d("onReturnDownloadRsaPublicKey(HashMap<String, String> map):" + map.toString());
            if (map == null) {
                TRACE.d("MainActivity++++++++++++++map == null");
                return;
            }
            String randomKeyLen = map.get("randomKeyLen");
            String randomKey = map.get("randomKey");
            String randomKeyCheckValueLen = map.get("randomKeyCheckValueLen");
            String randomKeyCheckValue = map.get("randomKeyCheckValue");
            TRACE.d("randomKey" + randomKey + "    \n    randomKeyCheckValue" + randomKeyCheckValue);
//            statusEditText.setText("randomKeyLen:" + randomKeyLen + "\nrandomKey:" + randomKey + "\nrandomKeyCheckValueLen:" + randomKeyCheckValueLen + "\nrandomKeyCheckValue:"
//                    + randomKeyCheckValue);
        }

        @Override
        public void onGetPosComm(int mod, String amount, String posid) {
            TRACE.d("onGetPosComm(int mod, String amount, String posid):" + mod + TRACE.NEW_LINE + amount + TRACE.NEW_LINE + posid);
        }

        @Override
        public void onPinKey_TDES_Result(String arg0) {
            TRACE.d("onPinKey_TDES_Result(String arg0):" + arg0);
           // statusEditText.setText("result:" + arg0);
        }

        @Override
        public void onUpdateMasterKeyResult(boolean arg0, Hashtable<String, String> arg1) {
            TRACE.d("onUpdateMasterKeyResult(boolean arg0, Hashtable<String, String> arg1):" + arg0 + TRACE.NEW_LINE + arg1.toString());
        }

        @Override
        public void onEmvICCExceptionData(String arg0) {
            TRACE.d("onEmvICCExceptionData(String arg0):" + arg0);
        }

        @Override
        public void onSetParamsResult(boolean arg0, Hashtable<String, Object> arg1) {
            TRACE.d("onSetParamsResult(boolean arg0, Hashtable<String, Object> arg1):" + arg0 + TRACE.NEW_LINE + arg1.toString());
        }

        @Override
        public void onGetInputAmountResult(boolean arg0, String arg1) {
            TRACE.d("onGetInputAmountResult(boolean arg0, String arg1):" + arg0 + TRACE.NEW_LINE + arg1.toString());
        }

        @Override
        public void onReturnNFCApduResult(boolean arg0, String arg1, int arg2) {
            TRACE.d("onReturnNFCApduResult(boolean arg0, String arg1, int arg2):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2);
         //   statusEditText.setText("onReturnNFCApduResult(boolean arg0, String arg1, int arg2):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2);
        }

        @Override
        public void onReturnPowerOffNFCResult(boolean arg0) {
            TRACE.d(" onReturnPowerOffNFCResult(boolean arg0) :" + arg0);
            //statusEditText.setText(" onReturnPowerOffNFCResult(boolean arg0) :" + arg0);
        }

        @Override
        public void onReturnPowerOnNFCResult(boolean arg0, String arg1, String arg2, int arg3) {
            TRACE.d("onReturnPowerOnNFCResult(boolean arg0, String arg1, String arg2, int arg3):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2 + TRACE.NEW_LINE + arg3);
           // statusEditText.setText("onReturnPowerOnNFCResult(boolean arg0, String arg1, String arg2, int arg3):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2 + TRACE.NEW_LINE + arg3);
        }

        @Override
        public void onCbcMacResult(String result) {
            TRACE.d("onCbcMacResult(String result):" + result);
            if (result == null || "".equals(result)) {
             //   statusEditText.setText("cbc_mac:false");
            } else {
               // statusEditText.setText("cbc_mac: " + result);
            }
        }

        @Override
        public void onReadBusinessCardResult(boolean arg0, String arg1) {
            TRACE.d(" onReadBusinessCardResult(boolean arg0, String arg1):" + arg0 + TRACE.NEW_LINE + arg1);
        }

        @Override
        public void onWriteBusinessCardResult(boolean arg0) {
            TRACE.d(" onWriteBusinessCardResult(boolean arg0):" + arg0);
        }

        @Override
        public void onConfirmAmountResult(boolean arg0) {
            TRACE.d("onConfirmAmountResult(boolean arg0):" + arg0);
        }

        @Override
        public void onQposIsCardExist(boolean cardIsExist) {
            TRACE.d("onQposIsCardExist(boolean cardIsExist):" + cardIsExist);
            if (cardIsExist) {
              //  statusEditText.setText("cardIsExist:" + cardIsExist);
            } else {
               // statusEditText.setText("cardIsExist:" + cardIsExist);
            }
        }

        @Override
        public void onSearchMifareCardResult(Hashtable<String, String> arg0) {
            if (arg0 != null) {
                TRACE.d("onSearchMifareCardResult(Hashtable<String, String> arg0):" + arg0.toString());
                String statuString = arg0.get("status");
                String cardTypeString = arg0.get("cardType");
                String cardUidLen = arg0.get("cardUidLen");
                String cardUid = arg0.get("cardUid");
                String cardAtsLen = arg0.get("cardAtsLen");
                String cardAts = arg0.get("cardAts");
                String ATQA = arg0.get("ATQA");
                String SAK = arg0.get("SAK");
//                statusEditText.setText("statuString:" + statuString + "\n" + "cardTypeString:" + cardTypeString + "\ncardUidLen:" + cardUidLen
//                        + "\ncardUid:" + cardUid + "\ncardAtsLen:" + cardAtsLen + "\ncardAts:" + cardAts
//                        + "\nATQA:" + ATQA + "\nSAK:" + SAK);
            } else {
               // statusEditText.setText("poll card failed");
            }
        }

        @Override
        public void onBatchReadMifareCardResult(String msg, Hashtable<String, List<String>> cardData) {
            if (cardData != null) {
                TRACE.d("onBatchReadMifareCardResult(boolean arg0):" + msg + cardData.toString());
            }
        }

        @Override
        public void onBatchWriteMifareCardResult(String msg, Hashtable<String, List<String>> cardData) {
            if (cardData != null) {
                TRACE.d("onBatchWriteMifareCardResult(boolean arg0):" + msg + cardData.toString());
            }
        }

        @Override
        public void onSetBuzzerResult(boolean arg0) {
            TRACE.d("onSetBuzzerResult(boolean arg0):" + arg0);
            if (arg0) {
               // statusEditText.setText("Set buzzer success");
            } else {
               // statusEditText.setText("Set buzzer failed");
            }
        }

        @Override
        public void onSetBuzzerTimeResult(boolean b) {
            TRACE.d("onSetBuzzerTimeResult(boolean b):" + b);
        }

        @Override
        public void onSetBuzzerStatusResult(boolean b) {
            TRACE.d("onSetBuzzerStatusResult(boolean b):" + b);
        }

        @Override
        public void onGetBuzzerStatusResult(String s) {
            TRACE.d("onGetBuzzerStatusResult(String s):" + s);
        }

        @Override
        public void onSetManagementKey(boolean arg0) {
            TRACE.d("onSetManagementKey(boolean arg0):" + arg0);
            if (arg0) {
              //  statusEditText.setText("Set master key success");
            } else {
             //   statusEditText.setText("Set master key failed");
            }
        }

        @Override
        public void onReturnUpdateIPEKResult(boolean arg0) {
            TRACE.d("onReturnUpdateIPEKResult(boolean arg0):" + arg0);
            if (arg0) {
              //  statusEditText.setText("update IPEK success");
            } else {
              //  statusEditText.setText("update IPEK fail");
            }
        }

        @Override
        public void onReturnUpdateEMVRIDResult(boolean arg0) {
            TRACE.d("onReturnUpdateEMVRIDResult(boolean arg0):" + arg0);
        }

        @Override
        public void onReturnUpdateEMVResult(boolean arg0) {
            TRACE.d("onReturnUpdateEMVResult(boolean arg0):" + arg0);
        }

        @Override
        public void onBluetoothBoardStateResult(boolean arg0) {
            TRACE.d("onBluetoothBoardStateResult(boolean arg0):" + arg0);
        }

        @Override
        public void onDeviceFound(BluetoothDevice arg0) {
            if (arg0 != null && arg0.getName() != null) {
            }
            else {
                messageCarrier.postValue("Don't found new device");
                TRACE.d("Don't found new device");
            }
        }

        @Override
        public void onSetSleepModeTime(boolean arg0) {
            TRACE.d("onSetSleepModeTime(boolean arg0):" + arg0);
            if (arg0) {
              //  statusEditText.setText("set the Sleep timee Success");
            } else {
                //statusEditText.setText("set the Sleep timee unSuccess");
            }
        }

        @Override
        public void onReturnGetEMVListResult(String arg0) {
            TRACE.d("onReturnGetEMVListResult(String arg0):" + arg0);
            if (arg0 != null && arg0.length() > 0) {
           //     statusEditText.setText("The emv list is : " + arg0);
            }
        }

        @Override
        public void onWaitingforData(String arg0) {
            TRACE.d("onWaitingforData(String arg0):" + arg0);
        }

        @Override
        public void onRequestDeviceScanFinished() {
            TRACE.d("onRequestDeviceScanFinished()");
           // Toast.makeText(MainActivity.this, R.string.scan_over, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onRequestUpdateKey(String arg0) {
            TRACE.d("onRequestUpdateKey(String arg0):" + arg0);
            /*
            mhipStatus.setText("update checkvalue : " + arg0);
            if(isUpdateFw){
                updateFirmware();
            }
             */
        }

        @Override
        public void onReturnGetQuickEmvResult(boolean arg0) {
            TRACE.d("onReturnGetQuickEmvResult(boolean arg0):" + arg0);
            if (arg0) {
              //  statusEditText.setText("emv configed");
                pos.setQuickEmv(true);
            } else {
              //  statusEditText.setText("emv don't configed");
            }
        }

        @Override
        public void onQposDoGetTradeLogNum(String arg0) {
            TRACE.d("onQposDoGetTradeLogNum(String arg0):" + arg0);
           /*
            int a = Integer.parseInt(arg0, 16);
            if (a >= 188) {
                statusEditText.setText("the trade num has become max value!!");
                return;
            }
            statusEditText.setText("get log num:" + a);
            */
        }

        @Override
        public void onQposDoTradeLog(boolean arg0) {
            TRACE.d("onQposDoTradeLog(boolean arg0) :" + arg0);
           /*
            if (arg0) {
                statusEditText.setText("clear log success!");
            } else {
                statusEditText.setText("clear log fail!");
            }
            */
        }

        @Override
        public void onAddKey(boolean arg0) {
            TRACE.d("onAddKey(boolean arg0) :" + arg0);
          /*
            if (arg0) {
                statusEditText.setText("ksn add 1 success");
            } else {
                statusEditText.setText("ksn add 1 failed");
            }
           */
        }

        @Override
        public void onEncryptData(Hashtable<String, String> resultTable) {
            if (resultTable != null) {
                TRACE.d("onEncryptData(String arg0) :" + resultTable);
            }
        }

        @Override
        public void onQposKsnResult(Hashtable<String, String> arg0) {
            TRACE.d("onQposKsnResult(Hashtable<String, String> arg0):" + arg0.toString());
            String pinKsn = arg0.get("pinKsn");
            String trackKsn = arg0.get("trackKsn");
            String emvKsn = arg0.get("emvKsn");
            TRACE.d("get the ksn result is :" + "pinKsn" + pinKsn + "\ntrackKsn" + trackKsn + "\nemvKsn" + emvKsn);
        }

        @Override
        public void onQposDoGetTradeLog(String arg0, String arg1) {
            TRACE.d("onQposDoGetTradeLog(String arg0, String arg1):" + arg0 + TRACE.NEW_LINE + arg1);
            arg1 = QPOSUtil.convertHexToString(arg1);
           // statusEditText.setText("orderId:" + arg1 + "\ntrade log:" + arg0);
        }

        @Override
        public void onRequestDevice() {
            List<UsbDevice> deviceList = getPermissionDeviceList();
            UsbManager mManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            for (int i = 0; i < deviceList.size(); i++) {
                UsbDevice usbDevice = deviceList.get(i);
                if (usbDevice.getVendorId() == 2965 || usbDevice.getVendorId() == 0x03EB) {

                    if (mManager.hasPermission(usbDevice)) {
                        pos.setPermissionDevice(usbDevice);
                    } else {
                        devicePermissionRequest(mManager, usbDevice);
                    }
                }
            }
        }

        @Override
        public void onGetKeyCheckValue(List<String> checkValue) {
            if (checkValue != null) {
                StringBuffer buffer = new StringBuffer();
                buffer.append("{");
                for (int i = 0; i < checkValue.size(); i++) {
                    buffer.append(checkValue.get(i)).append(",");
                }
                buffer.append("}");
               // statusEditText.setText(buffer.toString());
            }
        }

        @Override
        public void onGetDevicePubKey(String clearKeys) {
            TRACE.d("onGetDevicePubKey(clearKeys):" + clearKeys);
         //   statusEditText.setText(clearKeys);
            String lenStr = clearKeys.substring(0, 4);
            int sum = 0;
            for (int i = 0; i < 4; i++) {
                int bit = Integer.parseInt(lenStr.substring(i, i + 1));
                sum += bit * Math.pow(16, (3 - i));
            }
           // pubModel = clearKeys.substring(4, 4 + sum * 2);
        }

//        @Override
//        public void onSetPosBlePinCode(boolean b) {
//            TRACE.d("onSetPosBlePinCode(b):" + b);
//            if (b) {
//                statusEditText.setText("onSetPosBlePinCode success");
//            } else {
//                statusEditText.setText("onSetPosBlePinCode fail");
//            }
//        }

        @Override
        public void onTradeCancelled() {
            TRACE.d("onTradeCancelled");
           // dismissDialog();
        }

        @Override
        public void onReturnSignature(boolean b, String signaturedData) {
            if (b) {
                BASE64Encoder base64Encoder = new BASE64Encoder();
                String encode = base64Encoder.encode(signaturedData.getBytes());
              //  statusEditText.setText("signature data (Base64 encoding):" + encode);
            }
        }

        @Override
        public void onReturnConverEncryptedBlockFormat(String result) {
           // statusEditText.setText(result);
        }

        @Override
        public void onFinishMifareCardResult(boolean arg0) {
            TRACE.d("onFinishMifareCardResult(boolean arg0):" + arg0);
            /*
             if (arg0) {
                statusEditText.setText("finish success");
            } else {
                statusEditText.setText("finish fail");
            }
             */
        }

        @Override
        public void onVerifyMifareCardResult(boolean arg0) {
            TRACE.d("onVerifyMifareCardResult(boolean arg0):" + arg0);
         /*
            if (arg0) {
                statusEditText.setText(" onVerifyMifareCardResult success");
            } else {
                statusEditText.setText("onVerifyMifareCardResult fail");
            }
          */
        }

        @Override
        public void onReadMifareCardResult(Hashtable<String, String> arg0) {
            if (arg0 != null) {
                TRACE.d("onReadMifareCardResult(Hashtable<String, String> arg0):" + arg0.toString());
                String addr = arg0.get("addr");
                String cardDataLen = arg0.get("cardDataLen");
                String cardData = arg0.get("cardData");
                //statusEditText.setText("addr:" + addr + "\ncardDataLen:" + cardDataLen + "\ncardData:" + cardData);
            } else {
              //  statusEditText.setText("onReadWriteMifareCardResult fail");
            }
        }

        @Override
        public void onWriteMifareCardResult(boolean arg0) {
            TRACE.d("onWriteMifareCardResult(boolean arg0):" + arg0);
            if (arg0) {
              //  statusEditText.setText("write data success!");
            } else {
             //   statusEditText.setText("write data fail!");
            }
        }

        @Override
        public void onOperateMifareCardResult(Hashtable<String, String> arg0) {
            if (arg0 != null) {
                TRACE.d("onOperateMifareCardResult(Hashtable<String, String> arg0):" + arg0.toString());
                String cmd = arg0.get("Cmd");
                String blockAddr = arg0.get("blockAddr");
              //  statusEditText.setText("Cmd:" + cmd + "\nBlock Addr:" + blockAddr);
            } else {
                //statusEditText.setText("operate failed");
            }
        }

        @Override
        public void getMifareCardVersion(Hashtable<String, String> arg0) {
            if (arg0 != null) {
                TRACE.d("getMifareCardVersion(Hashtable<String, String> arg0):" + arg0.toString());

                String verLen = arg0.get("versionLen");
                String ver = arg0.get("cardVersion");
              //  statusEditText.setText("versionLen:" + verLen + "\nverison:" + ver);
            } else {
               // statusEditText.setText("get mafire UL version failed");
            }
        }

        @Override
        public void getMifareFastReadData(Hashtable<String, String> arg0) {
            if (arg0 != null) {
                TRACE.d("getMifareFastReadData(Hashtable<String, String> arg0):" + arg0.toString());
                String startAddr = arg0.get("startAddr");
                String endAddr = arg0.get("endAddr");
                String dataLen = arg0.get("dataLen");
                String cardData = arg0.get("cardData");
//                statusEditText.setText("startAddr:" + startAddr + "\nendAddr:" + endAddr + "\ndataLen:" + dataLen
//                        + "\ncardData:" + cardData);
            } else {
               // statusEditText.setText("read fast UL failed");
            }
        }

        @Override
        public void getMifareReadData(Hashtable<String, String> arg0) {
            if (arg0 != null) {
                TRACE.d("getMifareReadData(Hashtable<String, String> arg0):" + arg0.toString());
                String blockAddr = arg0.get("blockAddr");
                String dataLen = arg0.get("dataLen");
                String cardData = arg0.get("cardData");
              //  statusEditText.setText("blockAddr:" + blockAddr + "\ndataLen:" + dataLen + "\ncardData:" + cardData);
            } else {
             //   statusEditText.setText("read mafire UL failed");
            }
        }

        @Override
        public void writeMifareULData(String arg0) {
            if (arg0 != null) {
                TRACE.d("writeMifareULData(String arg0):" + arg0);
              //  statusEditText.setText("addr:" + arg0);
            } else {
                //statusEditText.setText("write UL failed");
            }
        }

        @Override
        public void verifyMifareULData(Hashtable<String, String> arg0) {
            if (arg0 != null) {
                TRACE.d("verifyMifareULData(Hashtable<String, String> arg0):" + arg0.toString());
                String dataLen = arg0.get("dataLen");
                String pack = arg0.get("pack");
              //  statusEditText.setText("dataLen:" + dataLen + "\npack:" + pack);
            } else {
               // statusEditText.setText("verify UL failed");
            }
        }

        @Override
        public void onGetSleepModeTime(String arg0) {
            if (arg0 != null) {
                TRACE.d("onGetSleepModeTime(String arg0):" + arg0.toString());

                int time = Integer.parseInt(arg0, 16);
               // statusEditText.setText("time is ： " + time + " seconds");
            } else {
               // statusEditText.setText("get the time is failed");
            }
        }

        @Override
        public void onGetShutDownTime(String arg0) {
            if (arg0 != null) {
                TRACE.d("onGetShutDownTime(String arg0):" + arg0.toString());
             //   statusEditText.setText("shut down time is : " + Integer.parseInt(arg0, 16) + "s");
            } else {
               // statusEditText.setText("get the shut down time is fail!");
            }
        }

        @Override
        public void onQposDoSetRsaPublicKey(boolean arg0) {
            TRACE.d("onQposDoSetRsaPublicKey(boolean arg0):" + arg0);
            if (arg0) {
              //  statusEditText.setText("set rsa is successed!");
            } else {
                //statusEditText.setText("set rsa is failed!");
            }
        }

        @Override
        public void onQposGenerateSessionKeysResult(Hashtable<String, String> arg0) {
            if (arg0 != null) {
                TRACE.d("onQposGenerateSessionKeysResult(Hashtable<String, String> arg0):" + arg0.toString());
                String rsaFileName = arg0.get("rsaReginString");
                String enPinKeyData = arg0.get("enPinKey");
                String enKcvPinKeyData = arg0.get("enPinKcvKey");
                String enCardKeyData = arg0.get("enDataCardKey");
                String enKcvCardKeyData = arg0.get("enKcvDataCardKey");
//                statusEditText.setText("rsaFileName:" + rsaFileName + "\nenPinKeyData:" + enPinKeyData + "\nenKcvPinKeyData:" +
//                        enKcvPinKeyData + "\nenCardKeyData:" + enCardKeyData + "\nenKcvCardKeyData:" + enKcvCardKeyData);
//
            } else {
               // statusEditText.setText("get key failed,pls try again!");
            }
        }

        @Override
        public void transferMifareData(String arg0) {
            TRACE.d("transferMifareData(String arg0):" + arg0.toString());

            // TODO Auto-generated method stub
            if (arg0 != null) {
             //   statusEditText.setText("response data:" + arg0);
            } else {
              //  statusEditText.setText("transfer data failed!");
            }
        }

        @Override
        public void onReturnRSAResult(String arg0) {
            TRACE.d("onReturnRSAResult(String arg0):" + arg0.toString());

            if (arg0 != null) {
                //statusEditText.setText("rsa data:\n" + arg0);
            } else {
               // statusEditText.setText("get the rsa failed");
            }
        }

        @Override
        public void onRequestNoQposDetectedUnbond() {
            // TODO Auto-generated method stub
            TRACE.d("onRequestNoQposDetectedUnbond()");
        }

        @Override
        public  void onReturnDeviceCSRResult(String re) {
            TRACE.d("onReturnDeviceCSRResult:"+re);
           // statusEditText.setText("onReturnDeviceCSRResult:"+re);
        }

        @Override
        public  void onReturnStoreCertificatesResult(boolean re) {
            TRACE.d("onReturnStoreCertificatesResult:"+re);
           /*
            if(isInitKey){
                statusEditText.setText("Init key result is :"+re);
                isInitKey = false;
            }else {
                statusEditText.setText("Exchange Certificates result is :"+re);
            }
            */

        }

        @Override
        public  void onReturnDeviceSigningCertResult(String certificates, String certificatesTree) {
            TRACE.d("onReturnDeviceSigningCertResult:"+certificates+"\n"+certificatesTree);
            deviceSignCert = certificates;
            String command = context. getString(R.string.pedi_command,certificates,"1","oeap");
            command = ParseASN1Util.addTagToCommand(command,"CD",certificates);
            TRACE.i("request the RKMS command is "+command);
            String pediRespose = "[AOPEDI;ANY;CC308203B33082029BA00302010202074EB0D60000987E300D06092A864886F70D01010B0500308190310B3009060355040613025553310B300906035504080C0254583114301206035504070C0B53414E20414E544F4E494F31133011060355040A0C0A5669727475437279707431173015060355040C0C0E44737072656164204B44482043413117301506035504410C0E44737072656164204B44482043413117301506035504030C0E44737072656164204B4448204341301E170D3231303330363030303030305A170D3330303330373030303030305A3081A2310B3009060355040613025553310B300906035504080C0254583114301206035504070C0B53414E20414E544F4E494F31133011060355040A0C0A56697274754372797074311D301B060355040C0C14447370726561645F417573524B4D533130312D56311D301B06035504410C14447370726561645F417573524B4D533130312D56311D301B06035504030C14447370726561645F417573524B4D533130312D5630820122300D06092A864886F70D01010105000382010F003082010A0282010100D7FD40DD513EE82491FABA3EB734C3FE69C79973797007A2183EC9C468F73D8E1CB669DDA6DC32CA125F9FAEAC0C0556893C9196FB123B06BC9B880EEF367CD17000C7E0ECF7313DD2D396F29C8D977A65946258BE5A4133462F0675161407EED3D263BC20E9271B9070DCC1A6376F89E7E9E2B304BC756E3E3B61B869A2E39F11067D00B5BA3817673A730F42DC4C037FC214207C70A1E3E43F7D7494E71EBDD5BB0E9AFAE32E422DB90B85E230DF406FB12470AD0360FD7BDFDD1A29BCE91655A835129858A0E9EB04845A80F1E9F8EAA20C67C6B8A61113D6FFDD7DF5719778A03A30F69B0DD9033D5E975F723CC18792CC6988250A7DBD20901450651A810203010001300D06092A864886F70D01010B050003820101008F002AE3AFB49C2E7D99CC7B933617D180CB4E8EA13CBCBE7469FC4E5124033F06E4C3B0DAB3C6CA4625E3CD53E7B86C247CDF100E266059366F8FEEC746507E1B0D0924029805AAB89FCE1482946B8B0C1F546DD56B399AB48891B731148C878EF4D02AE641717A3D381C7B62011B76A6FFBF20846217EB68149C96B4B134F980060A542DBE2F32BF7AD308F26A279B41C65E32D4E260AE68B3010685CE36869EFF09D211CE64401F417A72F29F49A2EE713ACC37C29AECBFEBE571EF11D883815F54FA3E52A917CC3D6B008A3E3C52164FF5591D869026D248873F15DE531104F329C279FC5B6BC28ABC833F8C31BEF47783A5D5B9C534A57530D9AE463DC3;CD308203B33082029BA00302010202074EB0D60000987C300D06092A864886F70D01010B0500308190310B3009060355040613025553310B300906035504080C0254583114301206035504070C0B53414E20414E544F4E494F31133011060355040A0C0A5669727475437279707431173015060355040C0C0E44737072656164204B44482043413117301506035504410C0E44737072656164204B44482043413117301506035504030C0E44737072656164204B4448204341301E170D3231303330373030303030305A170D3330303330383030303030305A3081A2310B3009060355040613025553310B300906035504080C0254583114301206035504070C0B53414E20414E544F4E494F31133011060355040A0C0A56697274754372797074311D301B060355040C0C14447370726561645F417573524B4D533130312D45311D301B06035504410C14447370726561645F417573524B4D533130312D45311D301B06035504030C14447370726561645F417573524B4D533130312D4530820122300D06092A864886F70D01010105000382010F003082010A0282010100A62A4935B57BA478F41B6C8B3F79E84DB61E516FEC8D5BE3E86FD296C6906625E0316A77F59D6D5075811BA7BB0801366BA7E370B758E3E1DCE005008C13D368536C2216FAF8AF70EBC6B5D1D231AFD19D6270DDBEA6535B46135D1DE11F374978A655FAA8C2A0DDC933CF82E9DC69DABF8676D0E81762D9B01799C83A8DF3EE70584AA4543EBBDAB02A0EFCA6A276588893DD28BD096400E315ECF5FE91EC210EEC2BE8763FEFB57D1448CC7D0FCDC3BDCE4B7BAAD546E0E5E99281B4F1AB052E1B0361977406B6B57B32353E9F338BED29E55E2D1F65C4322B5850D45146D5A66BFE8323C0D3E78E55A8945B622E15295B9176454A868399990B31D7B104CF0203010001300D06092A864886F70D01010B05000382010100296101AC1ED80EF9DD845D03F2D1F822B4AEFD50E0A1F47FA98105155327FDA6CE52BCD650BE1EB6DCD7F3CDF73325E85EE979EF0364970ADF6ED3A247B2E3E2D83D877BEBD66B20F3983E8DF8932F82F30C3FAF980ADF72E9FEE30EBAFC42B19FB1EAEC74BAE16E2D4EF245D18B58FB560A64C9B515EA065ECA7AE81D6ED0B97A24636E1E70EE3F2F3A3364C17C6B36BE82588BBED79F23914D4E4E7E1E3FC2A5438FAB0535D37D6FA52009ACD37B6F413700BBF440B6B94E4F12C7F465B8AAC2A03776AAB9AFBAE42FE19664DC0B4E3D8A90EB185529CABE39335AEC58295E1E073A765733410FD769345E9B99C0AA0CBE3FA815661857DCF7EA3BD35EFB4C;RD04916CCC6289600A55118FC37AF0999E;]";
            String cc = ParseASN1Util.parseToken(pediRespose,"CC");
            String cd = ParseASN1Util.parseToken(pediRespose,"CD");
            BASE64Decoder base64Decoder = new BASE64Decoder();
            try {
                String caChain= QPOSUtil.byteArray2Hex(base64Decoder.decodeBuffer(QPOSUtil.readRSANStream(context.getAssets().open("FX-Dspread-CA-Tree.pem"))));
                //the api callback is onReturnStoreCertificatesResult
                pos.loadCertificates(cc,cd,caChain);
//                statusEditText.setText("is load the server cert to device...");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onReturnAnalyseDigEnvelop(String result) {
            super.onReturnAnalyseDigEnvelop(result);
            /*
              verifySignatureCommand = ParseASN1Util.addTagToCommand(verifySignatureCommand,"KA",result);
            if(pedvVerifySignatureCommand != null){
                pedvVerifySignatureCommand = ParseASN1Util.addTagToCommand(pedvVerifySignatureCommand,"KA",result);
                TRACE.i("send key encryption command to RMKS is "+ pedvVerifySignatureCommand);
            }
            TRACE.i("send key encryption command to RMKS is "+ verifySignatureCommand);
            String response = "[AOPEDK;ANY;KN0;KB30819D0201013181973081941370413031313242315458303045303130304B5331384646464630303030303030303031453030303030414332313038323435443442443733373445443932464142363838373438314544363034344137453635433239463132393739383931384441394434353631443235324143414641020102040AFFFF0000000001E00000020100130A4473707265616442444B04021D58;KA0D0B2F2F3178D4045C1363274890494664B23D32BABEA47E5DB42F15C06816107FD293BAFF7371119F0B11B685A29D40DE78D397F9629A56112629452A9525F5261F8BDCA168328C49ACCFF0133C90E91AFCCA1E18178EBBA5E0BFA054B09514BA87EE05F2E4837D2C74E00BFD3B14EB708598517F357F79AA34C89DFEA9F59B6D3CECABA6C211809400DE9D0B0CA09384FDD834B8BFD416C4B09D32B3F5E45001F18E5C3116A0FFD8E0C6ACE567FCCE1AC909FD038FB58F16BB32163866CD9DCB4B131A394757638111B2CF3DC968D58CBAA95279BEFF697C0D92C6A42248B53A3E56E595AD128EDB50710BDBFFCB113A7DC4ECBCE8668482CBFD22CD7B2E42;RDB077A03C07C94F161842AA0C4831E0EF;CE1;AP308205A906092A864886F70D010702A082059A30820596020149310D300B06096086480165030402013082032506092A864886F70D010703A082031604820312020100318201FB308201F70201003081A830819C310B3009060355040613025553310B300906035504080C0254583114301206035504070C0B53414E20414E544F4E494F31133011060355040A0C0A56697274754372797074311B3019060355040C0C12447370726561642044657669636573204341311B301906035504410C12447370726561642044657669636573204341311B301906035504030C1244737072656164204465766963657320434102074EB0D600009880304306092A864886F70D0101073036300B0609608648016503040201301806092A864886F70D010108300B0609608648016503040201300D06092A864886F70D01010904000482010092583A07F6280625EE4CA043E3245F2CD6CCA8BAE6E198F4046A5DDE055723D2591A84DDCA4D7F7BB1B179881FD9EC4E33ED22333A9008DAEB3C3B1D7143D1953F2363BEA4C0D2592667C3468F228F856A95A6DCA1FA9CA0AB05D25DC612E7E2BF2AE3012D22C78BB7224C8C8E02146929937C3DF9FA3589B2A486C132477ACFA50BE09528FCBFDA43079AF54C050843BE4BDE701D246D8D8A4C947F12AFD97A66010459BBAE4ED627F687CC3E6DC30B5B35FE3564D9FB07F501B57A73A70AB9C3398E14391B16A5FE45C374984219F0B3A3265A82D3F5A48CEEF3998DCEA59F1CC5821B51605C66C8FD2687778C84B51CCE51C1FBFA876F978E0A9546C425FF3082010C06092A864886F70D010701301406082A864886F70D03070408C8FA8F2094E103118081E85816DF38AEC7C0E569C011DB7212278A767C8934770C7E994E9508E256B693973FBB4B47A78A9F6B1AB2D326CC2A76A53E3731B8A8128B1DE4BEDCCA51E0E740C1A474C21C8CF4A4726F4FBE0DC5CE41C4DB7A2CDBB2EF7B2C0F61B50E34A1A327A5069EB23524DB0D8119C4C407B90277B806288ECAC2826AF8AF6D092B29E90C03554986F38345B6BB247BC1498C2185661BDE318ADECAF199E798D70A058305F686ECC3A267D28EED6052483401EB5B5B84F897CAEA7968B8EEAB23F465CE3F1E7F7F7E402D1AA681D76D34CF9EC0B6BBBE9A513B8C42E5EA5319E218AC996F87767966DBD8F8318202573082025302014930819C308190310B3009060355040613025553310B300906035504080C0254583114301206035504070C0B53414E20414E544F4E494F31133011060355040A0C0A5669727475437279707431173015060355040C0C0E44737072656164204B44482043413117301506035504410C0E44737072656164204B44482043413117301506035504030C0E44737072656164204B444820434102074EB0D60000987E300B0609608648016503040201A0818E301806092A864886F70D010903310B06092A864886F70D0107033020060A2A864886F70D01091903311204104CDCEDD916AAACEEAE548A1C5B0A0EAA301F06092A864886F70D0107013112041041303031364B30544E30304530303030302F06092A864886F70D01090431220420A0E06A133DA8D4A5EC5A2E51E468B470B19E13834019A0C2563BA39308660A1F300D06092A864886F70D0101010500048201003BA0F51DC5B3400E5CD29429663008713C3B61DE0C053590296421635218AEB228A1802C971B18CCF0A137D66FE07B08A0B2A592F11557CC401C353C859E1B82C4BAE146F8AC2955BD1326A3482B173E5589B321FBA0517DCA071F120D0940DC7B8CD33C861E1403CCBD7C3203F1609D261D38B415A0BF234CC9370D18B1004D89BE4C7C4631C7A5D3A1010F0371E25F70B8000D5B94C946571D0F6A730DEF57950AED18839B38B0FF6497D03E960194CF3F113C57575F62E8299FCDE855A1BD36ECE5CAF3DC9F942387A76A329715EC09FDBED3C4FACA06160D538EC00D0166D46152D61F6C665F749E91A0E70E532CE726525B946ACD81510FF47146F00994;]";
            String KA = ParseASN1Util.parseToken(response,"KA");

            KB =  ParseASN1Util.parseToken(response,"KB");
            String signatureData = "a57e821386de1038b1a12dc22fa59ce317625680c523bd66bf2b9f840aebe52d020e07105d4107eeb05edd560d0345cd73ce2b68dbf19c61f9d56fbd1ddf9222c47956595b773c88eb7ec4577fb17053d42acf64f3e5c38ff325cdac7b689df029299087b69211e61bdfc22e329eb287456f83ef6c25e84fe1324e36ee85ba7e3accb79eb8ab7b270916a28a42a867e0e050c6950100c90daddb1f421444d16accb6005a312c3273c2f1b28f0c77456ae875081ae594d26139efd267c8dafa15e1b6cf961f3acdb92b26777127f474d24d57611b29f01dec062c02d720c4e759e1757f85ee39e74e05e23aa0aed53d62d05a902a6539a3e986e6dd237888ff92";
            boolean verifyResult = pos.authenticServerResponse(QPOSUtil.HexStringToByteArray(KA),signatureData);
            verifyResult = true;
            if(verifyResult) {
                if(response.contains("AP")) {
                    String AP = ParseASN1Util.parseToken(response, "AP");
                    ParseASN1Util.parseASN1new(AP.replace("A081", "3081"));
                    String nonce = ParseASN1Util.getNonce();
                    String header = ParseASN1Util.getHeader();
                    String digist = ParseASN1Util.getDigest();
                    String encryptData = ParseASN1Util.getEncryptData();
                    ParseASN1Util.parseASN1new(encryptData.substring(6));
                    String signData = ParseASN1Util.getSignData();
                    String encryptDataWith3des = ParseASN1Util.getEncryptDataWith3Des();
                    String IV = ParseASN1Util.getIVStr();
                    String clearData = "A0818e301806092a864886f70d010903310b06092a864886f70d0107033020060a2a864886f70d01091903311204104cdcedd916aaaceeae548a1c5b0a0eaa301f06092a864886f70d0107013112041041303031364b30544e30304530303030302f06092a864886f70d01090431220420a0e06a133da8d4a5ec5a2e51e468b470b19e13834019a0c2563ba39308660a1f";
                    String envelop = getDigitalEnvelopStr(encryptData, encryptDataWith3des, "01", clearData, signData, IV);
                    //the api callback is onRequestUpdateWorkKeyResult
                    pos.loadSessionKeyByTR_34(envelop);
                }else {
                    statusEditText.setText("signature verification successful.");
                    ParseASN1Util.parseASN1new(KB);
                    String data =  ParseASN1Util.getTr31Data();
                    //the api callback is onReturnupdateKeyByTR_31Result
                    pos.updateKeyByTR_31(data,30);
                }
            }else {
                statusEditText.setText("signature verification failed.");
            }
             */
        }

    private void getPinBlockAndKsn(String str) {
        Hashtable<String, String> tlvList = pos.anlysEmvIccData(str);
        PINBLOCK = tlvList.get("pinBlock" );
        KSN = tlvList.get("pinKsn");
        CARDHOLDERNAME = tlvList.get("cardholderName" );

        if(!TextUtils.isEmpty(PINBLOCK) && !TextUtils.isEmpty(CARDHOLDERNAME)){
            byte [] byte_ksn = DUKPTUtil.parseHexStr2Byte(KSN);
            byte [] byte_bdk = DUKPTUtil.parseHexStr2Byte(context.getString(R.string.bdk));
            byte[] ipek = DUKPTUtil.GenerateIPEK(byte_ksn,byte_bdk);
            byte[] pinKey = DUKPTUtil.GetPinKeyVariant(byte_ksn,ipek);
            String pinKeyStr = DUKPTUtil.parseByte2HexStr(pinKey);
            PINBLOCK = DUKPTUtil.parseByte2HexStr(DUKPTUtil.TriDesDecryption(DUKPTUtil.parseHexStr2Byte(pinKeyStr), DUKPTUtil.parseHexStr2Byte(PINBLOCK)));
        }
    }

    private Hashtable<String,String> getTags() {
        Hashtable<String, String> decodeData = new Hashtable<>();
        decodeData.put(EmvTags.APPLICATION_IDENTIFIER_TERMINAL.toString(), pos.getICCTag(0, 1, EmvTags.APPLICATION_IDENTIFIER_TERMINAL.toString()).get("tlv"));
        decodeData.put(EmvTags.TRANSACTION_TIME.toString(), "9A03".concat(terminalTime.substring(2,8)));
        decodeData.put(EmvTags.APPLICATION_PRIMARY_ACCOUNT_NUMBER_SEQUENCE_NUMBER.toString(), pos.getICCTag(0, 1, EmvTags.APPLICATION_PRIMARY_ACCOUNT_NUMBER_SEQUENCE_NUMBER.toString()).get("tlv"));
        decodeData.put(EmvTags.AMOUNT_AUTHORISED_NUMERIC.toString(), pos.getICCTag(0, 1, EmvTags.AMOUNT_AUTHORISED_NUMERIC.toString()).get("tlv"));
        decodeData.put(EmvTags.APPLICATION_PRIMARY_ACCOUNT_NUMBER.toString(), pos.getICCTag(0, 1, EmvTags.APPLICATION_PRIMARY_ACCOUNT_NUMBER.toString()).get("tlv"));
        decodeData.put(EmvTags.AMOUNT_OTHER_NUMERIC.toString(), pos.getICCTag(0, 1, EmvTags.AMOUNT_OTHER_NUMERIC.toString()).get("tlv"));
        decodeData.put(EmvTags.APPLICATION_EXPIRATION_DATE.toString(), pos.getICCTag(0, 1, EmvTags.APPLICATION_EXPIRATION_DATE.toString()).get("tlv"));
        decodeData.put(EmvTags.TRACK_2_EQUIVALENT_DATA.toString(), pos.getICCTag(0, 1, EmvTags.TRACK_2_EQUIVALENT_DATA.toString()).get("tlv"));
        decodeData.put(EmvTags.CARDHOLDER_VERIFICATION_METHOD_RESULTS.toString(), pos.getICCTag(0, 1, EmvTags.CARDHOLDER_VERIFICATION_METHOD_RESULTS.toString()).get("tlv"));
        decodeData.put(EmvTags.CARD_HOLDER_NAME.toString(), pos.getICCTag(0, 1, EmvTags.CARD_HOLDER_NAME.toString()).get("tlv"));
        return decodeData;
    }
    private String getICCTags(){
        StringBuilder builder = new StringBuilder();
        try {
            String[] tags = new String[]{"9F26", "9F27", "9F10", "9F37", "9F36", "95", "9A", "9C", "9F02", "5F2A", "5F34", "82", "9F1A", "9F03", "9F33", "9F34", "9F35"};
            for(String s : tags){
                String tag = pos.getICCTag(0,1,s).get("tlv");
                builder.append(tag);            }
            return builder.toString();
        }catch (Exception e){
            return  null;
        }
    }
    private   String padLeft(String data, int length, char padChar) {
        int remaining = length - data.length();

        String newData = data;
        for (int i = 0; i < remaining; i++)
            newData = padChar + newData;
        return newData;
    }


    @Override
    public void onProcessComplete(String s, int i) {
        TransactionResponse response = new Gson().fromJson(s, TransactionResponse.class);
        try {
            byte[] paras = response.responseDescription == null ? context.getString(R.string.unable_to_proceed).getBytes("GBK") : response.responseDescription.getBytes("GBK");
            String customDisplayString = DUKPTUtil.parseByte2HexStr(paras);
            pos.lcdShowCustomDisplay(QPOSService.LcdModeAlign.LCD_MODE_ALIGNCENTER, customDisplayString,5);
            listener.onCompleteTransaction(response);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public static String getDigitalEnvelopStr(String encryptData, String encryptDataWith3des, String keyType, String clearData, String signData, String IV) {
        int encryptDataLen = (encryptData.length() / 2);
        int encryptDataWith3desLen = (encryptDataWith3des.length() / 2);
        int clearDataLen = (clearData.length() / 2);
        int signDataLen = (signData.length() / 2);
        int ivLen = IV.length() / 2;
        int len = 2 + 1 + 2 + 2 + encryptDataLen + 2 + encryptDataWith3desLen + 1 + ivLen + 1 + 2 + clearDataLen + 2 + signDataLen;
        String len2 = QPOSUtil.byteArray2Hex(QPOSUtil.intToBytes(len));
        String result = len2 + "010000" + QPOSUtil.intToHex2(encryptDataLen) + encryptData + QPOSUtil.intToHex2(encryptDataWith3desLen) + encryptDataWith3des
                + "0" + Integer.toString(ivLen, 16) + IV
                + keyType + QPOSUtil.intToHex2(clearDataLen) + clearData + QPOSUtil.intToHex2(signDataLen) + signData;
        System.out.println("sys = " + result);
        return result;
    }
    @Override
    public void onErrorOccur(String s, int i) {
        listener.onProcessingError(new RuntimeException(s), i);
    }
    public void close() {
        if (pos == null) {
            return;
        }
        if (pos == null) {
            return;
        } else if (posType == POS_TYPE.AUDIO) {
            pos.closeAudio();
        } else if (posType ==POS_TYPE.BLUETOOTH) {
            pos.disconnectBT();
//			pos.disConnectBtPos();
        } else if (posType ==POS_TYPE.BLUETOOTH_BLE) {
            pos.disconnectBLE();
        } else if (posType == POS_TYPE.UART) {
            pos.closeUart();
        } else if (posType == POS_TYPE.USB) {
            pos.closeUsb();
        } else if (posType == POS_TYPE.OTG) {
            pos.closeUsb();
        }
        pos.disconnectBT();
    }

    private List getPermissionDeviceList() {
        UsbManager mManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        List deviceList = new ArrayList<UsbDevice>();
        // check for existing devices
        for (UsbDevice device : mManager.getDeviceList().values()) {
            deviceList.add(device);
        }
        return deviceList;
    }
    private void devicePermissionRequest(UsbManager mManager, UsbDevice usbDevice) {
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(
                "com.android.example.USB_PERMISSION"), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(mUsbReceiver, filter);
        mManager.requestPermission(usbDevice, mPermissionIntent);
    }
    private String extractTag(Tag tag){
        try {
            return pos.getTag(tagList.get(tag.toString())).get(0).getValue();
        }catch (Exception e){
            return "";
        }
    }

    private void processRequestOnline(final EmvCardData emvCardData) {
            String rt = "";
            String finalev= "";
      /*
        new Thread(new Runnable() {
            @Override
            public void run() {
                NetworkUtils.postRequest(context.getString(R.string.nibbs_address),
                        "TransactionRequest",
                        "jsonRequest",
                        new Gson().toJson(emvCardData),
                        MyPosListener.this,
                        404);
            }
        }).start();
       */
    }
    private EmvCardData prepareTagsForOnlineProcess(String macAddress){
        EmvCardData emvcardData = new EmvCardData();
        emvcardData.aid = extractTag(EmvTags.APPLICATION_IDENTIFIER_TERMINAL);
        emvcardData.track2 = extractTag(EmvTags.TRACK_2_EQUIVALENT_DATA);
        emvcardData.cardHolderName = CARDHOLDERNAME;
        emvcardData.iccData = ICCDATA;
        emvcardData.pindata = PINBLOCK;
        emvcardData.otheramount = extractTag(EmvTags.AMOUNT_OTHER_NUMERIC);
        emvcardData.realamount = amount;
        emvcardData.transactiontype = transactionType.toString();
        emvcardData.pan = extractTag(EmvTags.APPLICATION_PRIMARY_ACCOUNT_NUMBER);
        emvcardData.cardSequenceNo = padLeft(extractTag(EmvTags.APPLICATION_PRIMARY_ACCOUNT_NUMBER_SEQUENCE_NUMBER),3, '0');
        emvcardData.transactionamount = extractTag(EmvTags.AMOUNT_AUTHORISED_NUMERIC);
        emvcardData.cardExpiryDate = extractTag(EmvTags.APPLICATION_EXPIRATION_DATE).substring(0,4);
        return  emvcardData;
    }

    private enum POS_TYPE {
        BLUETOOTH, AUDIO, UART, USB, OTG, BLUETOOTH_BLE
    }
    private void startEmvTransaction(){
        terminalTime = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime());
        PINBLOCK = null; KSN = null; CARDHOLDERNAME = null; ICCDATA = null; emvCardData = null;
        pos.setCardTradeMode(QPOSService.CardTradeMode.ONLY_INSERT_CARD);
        pos.doTrade(timeout);
    }

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent
                            .getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // call method to set up device communication
                            TRACE.i("usb" + "permission granted for device "
                                    + device);
                            pos.setPermissionDevice(device);
                        }
                    } else {
                        TRACE.i("usb" + "permission denied for device " + device);

                    }
                    context.unregisterReceiver(mUsbReceiver);
                }
            }
        }
    };
}
