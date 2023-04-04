package com.ravenpos.ravendspreadpos;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;


import com.dspread.xpos.CQPOSService;
import com.dspread.xpos.QPOSService;
import com.ravenpos.ravendspreadpos.databinding.ActivityPosBinding;
import com.ravenpos.ravendspreadpos.pos.TransactionResponse;
import com.ravenpos.ravendspreadpos.utils.Constants;
import com.ravenpos.ravendspreadpos.utils.TRACE;
import com.ravenpos.ravendspreadpos.utils.TransactionListener;
import com.ravenpos.ravendspreadpos.utils.USBClass;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import Decoder.BASE64Decoder;
import Decoder.BASE64Encoder;
import pl.droidsonroids.gif.GifImageView;

/**
 * Created by AYODEJI on 05/15/2023.
 */
public class PosActivity extends BaseActivity implements TransactionListener {
    private ActivityPosBinding binding;
    private Double totalAmount = 0.0;
    private QPOSService pos;
    public static Double totalAmountPrint = 0.0;
    private static final int WORK_KEY_INDEX = 0;
    private static String TAG = PosActivity.class.getName();
    private String accountType;
    private TextView posViewUpdate;
    private GifImageView spinKit;
    private String terminalId;
    private String clearPinKey;
    private String clearMasterKey;
    private String clearSessionKey;
    private String host;
    private String port;
    private String Ip;
    private String Mid;
    private String snKey;
    private String businessName;
    private MutableLiveData<String> mutableLiveData;
    private boolean isSupport;
    private UsbDevice usbDevice;
    private EditText statusEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Intent intent = getIntent();
        binding.spinKit.setImageResource(R.drawable.insert_card_one);
        mutableLiveData = new MutableLiveData();
        if (intent != null) {
            totalAmount = intent.getDoubleExtra(Constants.INTENT_EXTRA_AMOUNT_KEY, 0.0);
            accountType = intent.getStringExtra(Constants.INTENT_EXTRA_ACCOUNT_TYPE);
            terminalId = intent.getStringExtra(Constants.TERMINAL_ID);
            clearPinKey = intent.getStringExtra(Constants.INTENT_CLEAR_PIN_KEY);
            clearMasterKey = intent.getStringExtra(Constants.INTENT_CLEAR_MASTER_KEY);
            port = intent.getStringExtra(Constants.INTENT_Port);
            Ip = intent.getStringExtra(Constants.INTENT_IP);
            businessName = intent.getStringExtra(Constants.INTENT_BUSINESS_NAME_KEY);
            clearSessionKey = intent.getStringExtra(Constants.INTENT_CLEAR_SESSION_KEY);
            totalAmountPrint = totalAmount;
            if (!TextUtils.isEmpty(intent.getStringExtra(Constants.TERMINAL_ID))
                    && !TextUtils.isEmpty(intent.getStringExtra(Constants.INTENT_CLEAR_MASTER_KEY))
                    && !TextUtils.isEmpty(intent.getStringExtra(Constants.INTENT_CLEAR_PIN_KEY))
                    && !TextUtils.isEmpty(intent.getStringExtra(Constants.INTENT_CLEAR_SESSION_KEY))
                    && !TextUtils.isEmpty(intent.getStringExtra(Constants.INTENT_IP))
                    && !TextUtils.isEmpty(intent.getStringExtra(Constants.INTENT_Port))
                    && !TextUtils.isEmpty(String.valueOf(intent.getDoubleExtra(Constants.INTENT_EXTRA_AMOUNT_KEY, 0.0)))
                    && !TextUtils.isEmpty(intent.getStringExtra(Constants.INTENT_EXTRA_ACCOUNT_TYPE))
            ) {
                totalAmount = intent.getDoubleExtra(Constants.INTENT_EXTRA_AMOUNT_KEY, 0.0);
                accountType = intent.getStringExtra(Constants.INTENT_EXTRA_ACCOUNT_TYPE);
                terminalId = intent.getStringExtra(Constants.TERMINAL_ID);
                clearPinKey = intent.getStringExtra(Constants.INTENT_CLEAR_PIN_KEY);
                clearMasterKey = intent.getStringExtra(Constants.INTENT_CLEAR_MASTER_KEY);
                port = intent.getStringExtra(Constants.INTENT_Port);
                Ip = intent.getStringExtra(Constants.INTENT_IP);
                clearSessionKey = intent.getStringExtra(Constants.INTENT_CLEAR_SESSION_KEY);
                Mid = intent.getStringExtra(Constants.INTENT_MID);
                snKey = intent.getStringExtra(Constants.INTENT_SN);

                totalAmountPrint = totalAmount;
            } else {
                incompleteParameters();
            }
        } else {
            incompleteParameters();
        }
        viewObserver();
    }

    private void  checkIFOTGConnected(){
        USBClass usb = new USBClass();
        ArrayList<String> deviceList = usb.GetUSBDevices(getBaseContext());
        if (deviceList == null) {
            this.toastPendingSheet();
            return;
        }
        final CharSequence[] items = deviceList.toArray(new CharSequence[deviceList.size()]);
        String selectedDevice = (String) items[0];


        usbDevice = USBClass.getMdevices().get(selectedDevice);
        open(QPOSService.CommunicationMode.USB_OTG_CDC_ACM);
        //posType = POS_TYPE.OTG;
        pos.openUsb(usbDevice);


    }

    /**
     * close device
     */
    /*
      private void close() {
        TRACE.d("close");
        if (pos == null) {
            return;
        } else if (posType == POS_TYPE.AUDIO) {
            pos.closeAudio();
        } else if (posType == POS_TYPE.BLUETOOTH) {
            pos.disconnectBT();
//			pos.disConnectBtPos();
        } else if (posType == POS_TYPE.BLUETOOTH_BLE) {
            pos.disconnectBLE();
        } else if (posType == POS_TYPE.UART) {
            pos.closeUart();
        } else if (posType == POS_TYPE.USB) {
            pos.closeUsb();
        } else if (posType == POS_TYPE.OTG) {
            pos.closeUsb();
        }
    }
     */

    /**
     * open and init bluetooth
     *
     * @param mode
     */
    private void open(QPOSService.CommunicationMode mode) {
        TRACE.d("open");
        //pos=null;
//        MyPosListener listener = new MyPosListener();
    //    MyQposClass listener = new MyQposClass();
        pos = QPOSService.getInstance(mode);
        if (pos == null) {
            statusEditText.setText("CommunicationMode unknow");
            return;
        }
        if (mode == QPOSService.CommunicationMode.USB_OTG_CDC_ACM) {
            pos.setUsbSerialDriver(QPOSService.UsbOTGDriver.CDCACM);
        }
      //  pos.setConext(MainActivity.this);
        //init handler
        Handler handler = new Handler(Looper.myLooper());
       // pos.initListener(handler, listener);
        String sdkVersion = pos.getSdkVersion();
       // Toast.makeText(MainActivity.this, "sdkVersion--" + sdkVersion, Toast.LENGTH_SHORT).show();
    }

    /**
     * @author qianmengChen
     * @ClassName: MyPosListener
     * @date: 2016-11-10 6:35:06pm
     */


    private void viewObserver() {
        mutableLiveData.observe(this, s -> {
            if (!TextUtils.isEmpty(s)) {
                showResult(posViewUpdate, s);
            }
        });
    }


    private void incompleteParameters() {
        Intent intent = new Intent();
        intent.putExtra(Constants.PRINTER_DATA_TRANSACTION_STATUS_CODE, "-1");
        intent.putExtra(Constants.PRINTER_DATA_TRXN_STATUS_KEY, "Incomplete Parameters");
        setResult(AppCompatActivity.RESULT_OK, intent);
        finish();
    }

    public static TransactionResponse getOnBackPressResponse(Double totalAmount) {
        TransactionResponse response = new TransactionResponse();
        response.responseCode = "407";
        response.responseDescription = "Transaction could not complete";
        response.amount = String.valueOf(totalAmount);
        return response;
    }

    @Override
    public void onBackPressed() {
        onCompleteTransaction(getOnBackPressResponse(totalAmount));
    }

    @Override
    public void onProcessingError(RuntimeException message, int errorcode) {
    }

    @Override
    public void onCompleteTransaction(TransactionResponse response) {

    }
}
