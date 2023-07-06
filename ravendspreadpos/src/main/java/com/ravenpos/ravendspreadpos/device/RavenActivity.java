package com.ravenpos.ravendspreadpos.device;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.ravenpos.ravendspreadpos.pos.EncryptUtil.byteArrayToHexString;
import static com.ravenpos.ravendspreadpos.pos.EncryptUtil.hexStringToByteArray;
import static com.ravenpos.ravendspreadpos.utils.StringUtils.getTransactionTesponse;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.dspread.xpos.CQPOSService;
import com.dspread.xpos.QPOSService;
import com.dspread.xpos.QPOSService.CommunicationMode;

import com.dspread.xpos.Tlv;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.pnsol.sdk.miura.emv.EmvTags;
import com.ravenpos.ravendspreadpos.BaseActivity;
import com.ravenpos.ravendspreadpos.BaseApplication;
import com.ravenpos.ravendspreadpos.R;
import com.ravenpos.ravendspreadpos.databinding.ActivityRavenBinding;
import com.ravenpos.ravendspreadpos.model.BaseData;
import com.ravenpos.ravendspreadpos.model.BluetoothModel;
import com.ravenpos.ravendspreadpos.model.BluetoothResponse;
import com.ravenpos.ravendspreadpos.network.Baas;

import com.ravenpos.ravendspreadpos.network.RetrofitClient;
import com.ravenpos.ravendspreadpos.pos.BluetoothDeviceAdapter;
import com.ravenpos.ravendspreadpos.pos.IBluetooth;
import com.ravenpos.ravendspreadpos.pos.KSNUtilities;
import com.ravenpos.ravendspreadpos.pos.TransactionResponse;
import com.ravenpos.ravendspreadpos.utils.AppLog;
import com.ravenpos.ravendspreadpos.utils.BluetoothSearch;
import com.ravenpos.ravendspreadpos.utils.Constants;
import com.ravenpos.ravendspreadpos.utils.DeviceType;
import com.ravenpos.ravendspreadpos.utils.MessagePacker;
import com.ravenpos.ravendspreadpos.utils.RavenEmv;
import com.ravenpos.ravendspreadpos.utils.RavenExtensions;
import com.ravenpos.ravendspreadpos.utils.TransactionListener;
import com.ravenpos.ravendspreadpos.utils.TransactionMessage;
import com.ravenpos.ravendspreadpos.utils.TransactionType;
import com.ravenpos.ravendspreadpos.utils.USBClass;
import com.ravenpos.ravendspreadpos.utils.utils.FileUtils;
import com.ravenpos.ravendspreadpos.utils.utils.ParseASN1Util;
import com.ravenpos.ravendspreadpos.utils.utils.QPOSUtil;
import com.ravenpos.ravendspreadpos.utils.utils.SharedPreferencesUtils;
import com.ravenpos.ravendspreadpos.utils.utils.TLVParser;
import com.ravenpos.ravendspreadpos.utils.utils.TRACE;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import Decoder.BASE64Decoder;
import Decoder.BASE64Encoder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class RavenActivity extends BaseActivity implements TransactionListener, IBluetooth, TextWatcher {
    private MutableLiveData<String> message;
    private final String currencyCode = "566";
    private ActivityRavenBinding binding;

    public QPOSService pos;
    private UsbDevice usbDevice;
    private Dialog dialog;
    private Hashtable<String, String> tagList;
    private String verifySignatureCommand, pedvVerifySignatureCommand;
    private String KB;
    private boolean isInitKey;
    private boolean isUpdateFw = false;

    private String nfcLog = "";
    private String pubModel = "";
    private String amount = "";
    private String cashbackAmount = "";
    private String blueTootchAddress = "";
    private String blueTitle;
    private String title;
    private boolean isPinCanceled = false;
    private boolean isNormalBlu = false;//to judge if is normal bluetooth
    private int type;
    private String ICCDATA, ID, PINBLOCK, TLV;
    private String _responseCode;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1001;
    private String deviceSignCert;
    private BottomSheetDialog pinDialog;

    private Button btnContinue;
    // private PinView txtUserPin;

    private RavenActivity.POS_TYPE posType = RavenActivity.POS_TYPE.BLUETOOTH;
    int flags = 0;

    private String serialNo = "";
    private String terminalTime;


    @Override
    public void getSelectedDevice(@NonNull BluetoothModel model) {
        this.model = model;
        serialNo = model.serialNo;
        // stateDialog.dismiss();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String pin = s.toString();

        if (pin.length() == 4) {
            clearPinText = pin;


            binding.pinOne.setBackgroundResource(R.drawable.pin_circle_checked);
            binding.pinTwo.setBackgroundResource(R.drawable.pin_circle_checked);
            binding.pinThree.setBackgroundResource(R.drawable.pin_circle_checked);
            binding.pinFour.setBackgroundResource(R.drawable.pin_circle_checked);
        } else if (s.length() == 3) {
            binding.pinOne.setBackgroundResource(R.drawable.pin_circle_checked);
            binding.pinTwo.setBackgroundResource(R.drawable.pin_circle_checked);
            binding.pinThree.setBackgroundResource(R.drawable.pin_circle_checked);
            binding.pinFour.setBackgroundResource(R.drawable.pin_circle);
        } else if (s.length() == 2) {
            binding.pinOne.setBackgroundResource(R.drawable.pin_circle_checked);
            binding.pinTwo.setBackgroundResource(R.drawable.pin_circle_checked);
            binding.pinThree.setBackgroundResource(R.drawable.pin_circle);
            binding.pinFour.setBackgroundResource(R.drawable.pin_circle);
        } else if (s.length() == 1) {
            binding.pinOne.setBackgroundResource(R.drawable.pin_circle_checked);
            binding.pinTwo.setBackgroundResource(R.drawable.pin_circle);
            binding.pinThree.setBackgroundResource(R.drawable.pin_circle);
            binding.pinFour.setBackgroundResource(R.drawable.pin_circle);
        } else if (TextUtils.isEmpty(s)) {
            binding.pinOne.setBackgroundResource(R.drawable.pin_circle);
            binding.pinTwo.setBackgroundResource(R.drawable.pin_circle);
            binding.pinThree.setBackgroundResource(R.drawable.pin_circle);
            binding.pinFour.setBackgroundResource(R.drawable.pin_circle);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    private enum POS_TYPE {
        BLUETOOTH, AUDIO, UART, USB, OTG, BLUETOOTH_BLE
    }

    private Double totalAmount = 0.0;
    public static Double totalAmountPrint = 0.0;
    private String terminalId;
    private String keyId;

    private String clearPinKey;
    private String clearMasterKey;
    private String clearSessionKey;
    private String host;
    private String port;
    private String Ip;
    private String Mid;
    private String snKey;
    private String businessName;

    private String accountType;
    private Baas mApiService;

    private KSNUtilities ksnUtilities;

    private String clearPinText;
    private  BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

    private ArrayList<BluetoothModel> bluetoothModelArrayList = new ArrayList<>();

    private DeviceType deviceTypeA = DeviceType.BLUETOOTH;

//    private static final int DISCOVERABLE_REQUEST = 2;
//    private static final int DISCOVERABLE_DURATION = 10;


   // private static BluetoothListener listener;

/*
  static   BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice bd = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (bd != null && bd.getName() != null) {
                    if(bd.getName().contains("MPOS")){
                        listener.onSuccess(true);
                    }
                }
            } else {
                listener.onError(false,1003);
            }
        }
    };
 */

//    public static void isBluetoothDetected(Activity activity, BluetoothListener _listener){
//            listener = _listener;
//            deviceDiscovery(activity);
//            //makeDiscoverable(activity);
//    }
/*
    private static void deviceDiscovery(Activity activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            String[] list =   new String[]{
                    Manifest.permission.BLUETOOTH_SCAN
            };
            ActivityCompat.requestPermissions(activity, list, 123);
            return;
        }
        if (btAdapter.startDiscovery()) {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

            activity.registerReceiver(br, filter);
            Toast.makeText(activity, "Discovering other bluetooth devices", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, "Discovery failed to start", Toast.LENGTH_SHORT).show();
        }
    }

 */
    /*
    private static void makeDiscoverable(Activity activity) {
        Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            String[] list =   new String[]{
                    Manifest.permission.BLUETOOTH_ADVERTISE
            };
            ActivityCompat.requestPermissions(activity, list, 123);
            return;
        }
        activity.startActivityForResult(i, DISCOVERABLE_REQUEST);
    }
     */


    private void clearDisplay() {
        message.postValue("");
    }

    private void incompleteParameters() {
        Intent intent = new Intent();
        intent.putExtra(Constants.PRINTER_DATA_TRANSACTION_STATUS_CODE, "-1");
        intent.putExtra(Constants.PRINTER_DATA_TRXN_STATUS_KEY, "Incomplete Parameters");
        setResult(RESULT_OK, intent);
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
            onCompleteTransaction(new RuntimeException("Transaction not complete"),10);
    }

    @SuppressLint("MissingPermission")
    private void proceedToPayment(DeviceType deviceType) {
        try {
            message.postValue(getString(R.string.connect));
            if(deviceType == DeviceType.BLUETOOTH){

                RavenExtensions.INSTANCE.gone(binding.refreshLoad);
                RavenExtensions.INSTANCE.visible(binding.normalLoad);

                RavenExtensions.INSTANCE.gone(binding.posTranParent);
                RavenExtensions.INSTANCE.gone(binding.posPinParent);
                RavenExtensions.INSTANCE.visible(binding.posBluetoothParent);
                detectBluetoothConnect();
            }else{
                initListener();
              //  selectOTGDevice();
            }
        }catch (Exception e){}
    }

    final Handler handler = new Handler();

    private void detectBluetoothConnect(){
        bluetoothModelArrayList = new ArrayList<>();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                RavenExtensions.INSTANCE.gone(binding.refreshLoad);
                RavenExtensions.INSTANCE.visible(binding.normalLoad);
            }
        }, 12000);

        open(CommunicationMode.BLUETOOTH);
        posType = POS_TYPE.BLUETOOTH;
        pos.clearBluetoothBuffer();
        if (isNormalBlu) {
            TRACE.d("begin scan====");
            pos.scanQPos2Mode(RavenActivity.this, 20);
        }
    }
    private BluetoothDeviceAdapter adapter;

    private RecyclerView recyclerView;


    @SuppressLint("MissingPermission")
    private void selectBluetoothDevice(ArrayList<BluetoothModel> bluetoothModelArrayList) {
        //new ArrayList<>(RavenExtensions.INSTANCE.sortNonPos(bluetoothModelArrayList));
        adapter.swapData(bluetoothModelArrayList);
       // stateDialog.show();

        RavenExtensions.INSTANCE.gone(binding.refreshLoad);
        RavenExtensions.INSTANCE.visible(binding.normalLoad);

        RavenExtensions.INSTANCE.gone(binding.posTranParent);
        RavenExtensions.INSTANCE.gone(binding.posPinParent);
        RavenExtensions.INSTANCE.visible(binding.posBluetoothParent);

     //   pinDialog.show();


        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, bluetoothModelArrayList){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(bluetoothModelArrayList.get(position).title);
                return view;
            }
        };

        AlertDialog.Builder alertDialog =  new AlertDialog.Builder(this);
        alertDialog.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BluetoothModel btDevice = bluetoothModelArrayList.get(which);
                String btDeviceT =  btDevice.address;

                dialog.dismiss();
                onBTPosSelected(btDeviceT);

            }
        });
        alertDialog.setTitle("Select Bluetooth Device");
     //   alertDialog.show();
    }

    private void onBTPosSelected(String blueTootchAdd){
        if (isNormalBlu) {
            pos.stopScanQPos2Mode();
        }
        pos.connectBluetoothDevice(true, 25, blueTootchAdd);
        pos.setForceCVMNotRequired(true);
    }

    private BottomSheetDialog stateDialog;

    private BluetoothModel model;

    private void initBluetoothView(View view){
        try {
//            stateDialog =  new BottomSheetDialog(this);
//            stateDialog.setContentView(R.layout.bluetooth_connection);
//            stateDialog.setCancelable(false);


//            pinDialog =  new BottomSheetDialog(this);
//            pinDialog.setContentView(R.layout.pin_connection);
//            pinDialog.setCancelable(false);

//            pinDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
//            pinDialog.setOnShowListener(new DialogInterface.OnShowListener() {
//                @Override
//                public void onShow(DialogInterface dialog) {
//                    BottomSheetDialog d = (BottomSheetDialog) dialog;
//                    FrameLayout sheet =
//                            d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
//                    assert sheet != null;
//                    BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View)sheet);
//                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//                    behavior.setPeekHeight(700);
//                    behavior.setFitToContents(false);
//                   // behavior.setExpandedOffset(100);
//
//                   // int newHeight = ((Activity) getApplicationContext()).getWindow().getDecorView().getMeasuredHeight();
////
////                    ViewGroup.LayoutParams viewGroupLayoutParams = sheet.getLayoutParams();
////                    int height = getResources().getDisplayMetrics().heightPixels;
////                    float density = getResources().getDisplayMetrics().density;
////
////                    float finals =  height/density;
//
//
//                    //viewGroupLayoutParams.height =
//                  //  sheet.setLayoutParams( viewGroupLayoutParams);
//
//                }
//            });
           /// recyclerView = stateDialog.findViewById(R.id.guarantor_recyclerView);
           // btnContinue = stateDialog.findViewById(R.id.btnContinue);
          binding.btnContinueBlue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(model == null){
                       Toast.makeText(BaseApplication.getInstance(), R.string.kindly_select_a_bluetooth_model,Toast.LENGTH_SHORT).show();
                    }else {
                        String btDeviceT = model.address;
                        onBTPosSelected(btDeviceT);
                        RavenExtensions.INSTANCE.visible(binding.spinKit);
                        RavenExtensions.INSTANCE.visible(binding.posTranParent);
                        RavenExtensions.INSTANCE.gone(binding.posPinParent);
                        RavenExtensions.INSTANCE.gone(binding.posBluetoothParent);
                    }
                }
            });

          binding.blueRefreshAction.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  RavenExtensions.INSTANCE.visible(binding.refreshLoad);
                  RavenExtensions.INSTANCE.gone(binding.normalLoad);
                  detectBluetoothConnect();
              }
          });


            binding.amountText.addTextChangedListener(this);
            binding.btnContinuePin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String pinSet = clearPinText;
                            //binding.amountText.getText().toString();

                    //String pin = "21232";
                    if (pinSet.length() == 4) {
                        clearPinText = pinSet;
//                        if (pin.equals("000000")) {
//                            pos.sendEncryptPin("5516422217375116");
//
//                        } else {
//                            pos.sendPin(pin);
//                        }
                        RavenExtensions.INSTANCE.visible(binding.posTranParent);
                        RavenExtensions.INSTANCE.gone(binding.posPinParent);
                        RavenExtensions.INSTANCE.gone(binding.posBluetoothParent);
                        pos.sendPin(pinSet);
                        dismissDialog();
                    }
                    else {
                        Toast.makeText(RavenActivity.this, "The length just can input 4digits", Toast.LENGTH_LONG).show();
                    }

                }
            });
            /*
            LinearLayout parentCell = stateDialog.findViewById(R.id.back_arrow);
            if (parentCell != null) {
                LinearLayout btnBack = parentCell.findViewById(R.id.backArrowPost);
                btnBack.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onCompleteTransaction(new RuntimeException("Transaction not complete"),10);
                    }
                });
            }
             */
/*

            stateDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            stateDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    BottomSheetDialog d = (BottomSheetDialog) dialog;
                    FrameLayout sheet =
                            d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                    assert sheet != null;
                    BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View)sheet);
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            });
 */
        }catch (Exception e){
            AppLog.e("setOnShowListener",e.getLocalizedMessage());
        }
    }

    private static void setMainContext(Context context){
        BaseApplication.setContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QPOSService.TransactionType tt = QPOSService.TransactionType.PAYMENT;
        String gg = tt.name();
        String ggi = tt.toString();
        binding = ActivityRavenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        BaseApplication.setContext(this.getApplicationContext());
        adapter = new BluetoothDeviceAdapter(this, this);
        initBluetoothView(binding.getRoot());
         binding.recyclerView.setAdapter(adapter);

        binding.keyPad.registerDisplayDelegate(binding.amountText);
        Intent intent = getIntent();
        _responseCode = "00";
        ksnUtilities = new KSNUtilities();

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
            keyId = intent.getStringExtra(Constants.KEY_ID);
            boolean deviceTypeP = intent.getBooleanExtra(Constants.INTENT_BLUETOOTH_DEVICE_TYPE,false);
            if(deviceTypeP){
                deviceTypeA = DeviceType.BLUETOOTH;
            }else {
                deviceTypeA = DeviceType.OTG_CORD;
            }
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
                keyId = intent.getStringExtra(Constants.KEY_ID);
                this.amount = String.valueOf(totalAmount.intValue());
                totalAmountPrint = totalAmount;
            } else {
                incompleteParameters();
            }
        } else {
            incompleteParameters();
        }
      //  requestBlePermissions();
        message = new MutableLiveData<>();
        binding.spinKit.setImageResource(R.drawable.insert_card_two);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            flags = FLAG_IMMUTABLE;
        }else{
            flags = FLAG_UPDATE_CURRENT;
        }
//        if(SharedPreferencesUtils.getInstance().getBooleanValue(BaseApplication.getINSTANCE().getString(R.string.loadedDevice),false)){
//          //  initAID_CAPK();
//        }
        viewObserver();
        initIntent();
        proceedToPayment(deviceTypeA);
      // initListener();
        RavenExtensions.INSTANCE.gone(binding.spinKit);
        message.postValue(getString(R.string.connecting_bt_pos));
        try {
          // String gg = encryptedPinData("04319DCBB86B7B6E");
           //String gRRg = gg;
        } catch (Exception e) {
            AppLog.e("encryptedPinData",e.getLocalizedMessage());
        }

        binding.closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    onCompleteTransaction(new RuntimeException("Transaction Cancelled"),200);
            }
        });

        binding.btnCloseSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCompleteTransaction(new RuntimeException("Transaction Cancelled"),200);
            }
        });

        binding.txtCloseSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCompleteTransaction(new RuntimeException("Transaction Cancelled"),200);
            }
        });
    }

    public static boolean isUSBDetected(){
        USBClass usb = new USBClass();
        ArrayList<String> deviceList = usb.GetUSBDevices(BaseApplication.getInstance());
        if(deviceList == null) return  false;
        final ArrayList<String> items = deviceList;
        if(items.size() == 0)return  false;else return true;
    }

    private void initListener(){
        USBClass usb = new USBClass();
        ArrayList<String> deviceList = usb.GetUSBDevices(getBaseContext());
        if (deviceList == null) {
            Toast.makeText(RavenActivity.this, "No Permission", Toast.LENGTH_SHORT).show();
            return;
        }
        final CharSequence[] items = deviceList.toArray(new CharSequence[deviceList.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(RavenActivity.this);
        builder.setTitle("Select an OTG Reader");
        builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                String selectedDevice = (String) items[item];
                dialog.dismiss();
                usbDevice = USBClass.getMdevices().get(selectedDevice);
                open(CommunicationMode.USB_OTG_CDC_ACM);
               // getInitTermConfig();
                posType = RavenActivity.POS_TYPE.OTG;
                pos.openUsb(usbDevice);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private ArrayList list = new ArrayList();

    private void getInitTermConfig(){
       // initAID_CAPK();
        try{
            pos.updateEMVConfigByXml(new String(FileUtils.readAssetsLine("emv_profile_tlv.xml",RavenActivity.this)));
        }catch (Exception ex){
            AppLog.e("getInitTermConfig", ex.getLocalizedMessage());
        }
     // injectDevice();
    }

    private byte[] readLine(String Filename) {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            android.content.ContextWrapper contextWrapper = new ContextWrapper(this);
            AssetManager assetManager = contextWrapper.getAssets();
            InputStream inputStream = assetManager.open(Filename);
            byte[] data = new byte[512];
            int current = 0;
            while ((current = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, current);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }
        return buffer.toByteArray();
    }

    public void updateEmvConfig() {
       try {
           String emvAppCfg = QPOSUtil.byteArray2Hex(readLine("emv_app.bin"));
           String emvCapkCfg = QPOSUtil.byteArray2Hex(readLine("emv_capk.bin"));
           pos.updateEmvConfig(emvAppCfg, emvCapkCfg);
       }catch (Exception e){
           Log.e("updateEmvConfig",e.getLocalizedMessage());
       }
    }

    private  void injectDevice() {
        try {
            updateEmvConfig();
        }catch (Exception e){
            Log.e("injectDevice",e.getLocalizedMessage());
        }
    }
    private void  loadClearMasterKeyA(String clearPinKey, String clearMasterKey){
       // int keyIndex = getKeyIndex();
       // pos.setMasterKey("1A4D672DCA6CB3351FD1B02B237AF9AE", "08D7B4FB629D0885", keyIndex);

    }

    private void initIntent() {
        Intent intent = getIntent();
        type = intent.getIntExtra("connect_type", 3);
        switch (type) {
            case 3://normal bluetooth
                this.isNormalBlu = true;
                title = getString(R.string.title_blu);
                break;
            case 4://Ble
                isNormalBlu = false;
                title = getString(R.string.title_ble);
                break;
        }
        setTitle(title);
    }

    private void viewObserver() {
        message.observe(this, s -> {
            if (!TextUtils.isEmpty(s)) {
                showResult(binding.posViewUpdate, s);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED){
           // initListener();
        }
    }

//    private void initializeSheet(Context context){
//        pinDialog = new BottomSheetDialog(context);
//        pinDialog.setContentView(R.layout.transaction_pinview);
//        txtUserPin = pinDialog.findViewById(R.id.txtTaxPayerIdPIn);
//        assert txtUserPin != null;
//    }


    @Override
    public void onProcessingError(RuntimeException message, int errorcode) {
        try {
            if (!isFinishing()) {
                onCompleteTransaction(message,errorcode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getField4(String amountStr) {
        int index = amountStr.indexOf(".");
        if (amountStr.substring(index + 1, amountStr.length()).length() < 2) {
            amountStr = amountStr + "0";
        }
        amountStr = amountStr.replace(".", "");
        int amtlen = amountStr.length();
        StringBuilder amtBuilder = new StringBuilder();
        if (amtlen < 12) {
            for (int i = 0; i < (12 - amtlen); i++) {
                amtBuilder.append("0");
            }
        }
        amtBuilder.append(amountStr);
        amountStr = amtBuilder.toString();
        return amountStr;
    }

    public static String getServiceCode(String track2Data) {
        int indexOfToken = track2Data.indexOf("D");
        int indexOfServiceCode = indexOfToken + 5;
        int lengthOfServiceCode = 3;
        return track2Data.substring(indexOfServiceCode, indexOfServiceCode + lengthOfServiceCode);
    }

    public static String getExpiryDate(String track2Data) {
        int indexOfToken = track2Data.indexOf("D");
        int indexOfExpiryDate = indexOfToken + 1;
        int lengthOfExpiryDate = 4;
        return track2Data.substring(indexOfExpiryDate, indexOfExpiryDate + lengthOfExpiryDate);
    }

    private String encryptPinData(String clearpinblock,String pinkey) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {
        try {
            byte[] clearBytes = hexStringToByteArray(clearpinblock);
            byte[] keyBytes = hexStringToByteArray(pinkey);
            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
            cipher.init(1,new SecretKeySpec(keyBytes,"DESede"));

            byte[] encryptedBytes = cipher.doFinal(clearBytes);
            return byteArrayToHexString(encryptedBytes);
            //System.out.println(encryptedPinblock);
        }catch (Exception e){
            AppLog.e("byteArrayToHexString",e.getLocalizedMessage());
        }
        return "";
    }
    @Override
    public void onCompleteTransaction(TransactionResponse response)  {
        try {
            RavenEmv ravenEmv = new RavenEmv();
            response.TerminaID = terminalId;
            response.totalAmoount = String.valueOf(totalAmount.intValue());
            response.amount = String.valueOf(totalAmount.intValue());
            ravenEmv.dataModel = response;
            ravenEmv.dataModel.poseidonSerialNumber = serialNo;
            String fullPay = new Gson().toJson(response);
            showResult(binding.posViewUpdate, "");
            Log.e("TRANS DONE", new Gson().toJson(response));
            Gson gson = new Gson();

            TransactionMessage msg = new TransactionMessage();
            msg.setField0("0200");
            msg.setField2(response.CardNo);
            msg.setField3("00"+accountType+"00");
            msg.setTotalamount(String.valueOf(totalAmount.intValue()));
            msg.setField4(getField4(String.valueOf(totalAmount.intValue())+ "00"));

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMddhhmmss");
            String datetime = simpleDateFormat.format(new Date());
            msg.setField7(datetime);


            SimpleDateFormat dateFormatStan = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            //simpleDateFormat = new SimpleDateFormat("hhmmss");
            String stan = dateFormatStan.format(new Date());
            String newStan = stan.substring(stan.length() - 6);


            msg.setField11(newStan);
            msg.setField12(newStan);

            simpleDateFormat = new SimpleDateFormat("MMdd");
            String date = simpleDateFormat.format(new Date());
            msg.setField13(date);

            msg.setField14(getExpiryDate(response.Track2));
            msg.setField18("5251");
            msg.setField22("051");
            msg.setField23(response.CardSequenceNumber);
            msg.setField25("00");
            msg.setField26("06");//12;
            msg.setField28("D00000000");
            msg.setField32(response.Track2.substring(0, 6));
            msg.setField35(response.Track2);
            // SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            ///String pre = dateFormat.format(new Date());
            String newPre = stan.substring(stan.length() - 12);
            ravenEmv.dataModel.RRN = newPre;
            msg.setField37(newPre);
            msg.setField40(getServiceCode(response.Track2));
            msg.setField41(response.TerminaID);
            msg.setField42(Mid);
            //  msg.setField42("2030LA000490601");
            msg.setField43(businessName);
            msg.setField49("566");

            if(response.PinBlock != null){
                if (!response.PinBlock.isEmpty()) {
                    String plainPin = ksnUtilities.encryptPinBlock(response.CardNo,clearPinText);
                    try {
                        response.PinBlock  = encryptPinData(plainPin,clearPinKey);
                        msg.setField52(response.PinBlock);
                        msg.setPinblock(response.PinBlock);
                    } catch (InvalidKeyException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchPaddingException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalBlockSizeException e) {
                        throw new RuntimeException(e);
                    } catch (BadPaddingException e) {
                        throw new RuntimeException(e);
                    } catch (InvalidKeySpecException e) {
                        throw new RuntimeException(e);
                    }

                    if(!response.PinBlock.equals("31393937")){
                        // msg.setField52(response.PinBlock);
                    }
                }
            }
            msg.setField55(response.IccData);

            msg.setField123("510101511344101");

            msg.setClrsesskey(clearSessionKey);
            msg.setPort(port);
            msg.setHost(Ip);
            msg.setSsl(true);
            msg.setTotalamount(String.valueOf(totalAmount.intValue()));
            msg.setRrn(newPre);
            msg.setStan(newStan);
            msg.setTrack(response.Track2);
            msg.setExpirydate(getExpiryDate(response.Track2));
            msg.setPan(response.CardNo);
            msg.setTid(terminalId);

            msg.setFilename(SharedPreferencesUtils.getInstance().getStringValue("84", ""));
            msg.setUnpredictable(SharedPreferencesUtils.getInstance().getStringValue("9F37", ""));
            msg.setCapabilities(SharedPreferencesUtils.getInstance().getStringValue("9F33", ""));
            msg.setCryptogram(SharedPreferencesUtils.getInstance().getStringValue("9F26", ""));
            msg.setTvr(SharedPreferencesUtils.getInstance().getStringValue("95", ""));

            msg.setIad(SharedPreferencesUtils.getInstance().getStringValue("9F10", ""));

            msg.setCvm(SharedPreferencesUtils.getInstance().getStringValue("9F34", ""));

            msg.setCip("");
            msg.setAmount(String.valueOf(totalAmount.intValue()));

            msg.setAtc(SharedPreferencesUtils.getInstance().getStringValue("9F36", ""));

            msg.setAip(SharedPreferencesUtils.getInstance().getStringValue("82", ""));

            msg.setPanseqno(response.CardSequenceNumber);

            msg.setClrpin(clearPinKey);

            msg.setAccount(getAccountTypeString(accountType));

            msg.setSn(snKey);

            msg.setMid(Mid);

            msg.setFilename(SharedPreferencesUtils.getInstance().getStringValue("84", ""));
            msg.setField128(MessagePacker.generateHashData(msg));

            String msgToIso = new Gson().toJson(msg);
            AppLog.e("msgToIso",msgToIso);
            ravenEmv.nibbsEmv = msg;
            String fullRes = new Gson().toJson(ravenEmv);
            Intent intent = new Intent();
            // intent.putExtra(getString(R.string.data), ravenEmv);
            intent.putExtra(getString(R.string.data), fullRes);
            setResult(Activity.RESULT_OK, intent);
            finish();
 /*
        Call<Object> userCall = mApiService.performTransaction("98220514989004", "Horizonpay", "K11", "1.0.0", msg);
        userCall.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> res) {
                if (res.code() == 200) {
                    Intent intent = new Intent();
                    intent.putExtra(getString(R.string.data), ravenEmv);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
//                        SharedPreferencesUtils.getInstance().setValue(SharedPreferencesUtils.KEYS, new Gson().toJson(response.body()));
                }
            }
            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Intent intent = new Intent();
                intent.putExtra(getString(R.string.data), response);
                setResult(Activity.RESULT_OK, intent);
                finish();
                t.printStackTrace();
            }
        });
  */
        }catch (Exception e){
            AppLog.e("onCompleteTransaction",e.getLocalizedMessage());
        }
    }

    public void onCompleteTransaction(RuntimeException message, int errorcode) {
        TransactionResponse response = getTransactionTesponse(message.getMessage(), errorcode);
        RavenEmv ravenEmv = new RavenEmv();
        response.TerminaID = terminalId;
        response.RRN = terminalId;
        response.totalAmoount = String.valueOf(totalAmount.intValue());
        response.amount = String.valueOf(totalAmount.intValue());
        ravenEmv.dataModel = response;
        String fullRes = new Gson().toJson(ravenEmv);
        Intent intent = new Intent();
        intent.putExtra(getString(R.string.data), ravenEmv);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private  String getAccountTypeString(String code){
        String codeMsg = "Savings";
        switch (code){
            case "00":
                codeMsg =  "Default";
                break;
            case "10":
                codeMsg =  "Savings";
                break;
            case "20":
                codeMsg =  "Current";
                break;
            case "30":
                codeMsg =  "Credit";
                break;
        }

        return  codeMsg;
    }

    /**
     * open and init bluetooth
     *
     * @param mode
     */
    private void open(CommunicationMode mode) {
        TRACE.d("open");
            MyQposClass listener = new MyQposClass();
        pos = QPOSService.getInstance(mode);
        if (pos == null) {
            message.postValue("CommunicationMode unknow");
            return;
        }
        if (mode == CommunicationMode.USB_OTG_CDC_ACM) {
            pos.setUsbSerialDriver(QPOSService.UsbOTGDriver.CDCACM);
        }
        pos.setConext(RavenActivity.this);
        //init handler
        Handler handler = new Handler(Looper.myLooper());
        pos.initListener(handler, listener);
        String sdkVersion = pos.getSdkVersion();
    }

    /**
     * close device
     */
    private void close() {
        TRACE.d("close");
        if (pos == null) {
            return;
        } else if (posType == RavenActivity.POS_TYPE.AUDIO) {
            pos.closeAudio();
        } else if (posType == RavenActivity.POS_TYPE.BLUETOOTH) {
            pos.disconnectBT();
//			pos.disConnectBtPos();
        } else if (posType == RavenActivity.POS_TYPE.BLUETOOTH_BLE) {
            pos.disconnectBLE();
        } else if (posType == POS_TYPE.UART) {
            pos.closeUart();
        } else if (posType == RavenActivity.POS_TYPE.USB) {
            pos.closeUsb();
        } else if (posType == RavenActivity.POS_TYPE.OTG) {
            pos.closeUsb();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TRACE.d("onDestroy");
        if (pos != null) {
            close();
            pos = null;
        }
//        try {
//            unregisterNetworkChanges();
//        }catch (Exception ex){
//        AppLog.e("unregisterNetworkChanges", ex.getLocalizedMessage());
//        }
    }

  /*
    private void unregisterNetworkChanges() {
        try {
            unregisterReceiver(br);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
   */

    private void devicePermissionRequest(UsbManager mManager, UsbDevice usbDevice) {
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(RavenActivity.this, 0, new Intent(
                "com.android.example.USB_PERMISSION"), flags);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        mManager.requestPermission(usbDevice, mPermissionIntent);
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
                    RavenActivity.this.unregisterReceiver(mUsbReceiver);
                }
            }
        }
    };

    private List getPermissionDeviceList() {
        UsbManager mManager = (UsbManager) RavenActivity.this.getSystemService(Context.USB_SERVICE);
        List deviceList = new ArrayList<UsbDevice>();
        // check for existing devices
        for (UsbDevice device : mManager.getDeviceList().values()) {
            deviceList.add(device);
        }
        return deviceList;
    }
    private QPOSService.TransactionType transactionType = QPOSService.TransactionType.PAYMENT;
    /**
     * @author qianmengChen
     * @ClassName: MyPosListener
     * @date: 2016-11-10 6:35:06pm
     */
    class MyQposClass extends CQPOSService {

        @Override
        public void onRequestWaitingUser() {//wait user to insert/swipe/tap card
            TRACE.d("onRequestWaitingUser()");
          //  dismissDialog();

            message.postValue(getString(R.string.waiting_for_card));
        }

        @Override
        public void onDoTradeResult(QPOSService.DoTradeResult result, Hashtable<String, String> decodeData) {
            TRACE.d("(DoTradeResult result, Hashtable<String, String> decodeData) " + result.toString() + TRACE.NEW_LINE + "decodeData:" + decodeData);
           // dismissDialog();
            String cardNo = "";
            if (result == QPOSService.DoTradeResult.NONE) {
                message.postValue(getString(R.string.no_card_detected));
            } else if (result == QPOSService.DoTradeResult.TRY_ANOTHER_INTERFACE) {
                message.postValue(getString(R.string.try_another_interface));
            } else if (result == QPOSService.DoTradeResult.ICC) {
                message.postValue(getString(R.string.icc_card_inserted));
                TRACE.d("EMV ICC Start");
                pos.doEmvApp(QPOSService.EmvOption.START);
            } else if (result == QPOSService.DoTradeResult.NOT_ICC) {
                message.postValue(getString(R.string.card_inserted));
            } else if (result == QPOSService.DoTradeResult.BAD_SWIPE) {
                message.postValue(getString(R.string.bad_swipe));
            } else if (result == QPOSService.DoTradeResult.CARD_NOT_SUPPORT) {
                message.postValue("GPO NOT SUPPORT");
            } else if (result == QPOSService.DoTradeResult.PLS_SEE_PHONE) {
                message.postValue("PLS SEE PHONE");
            } else if (result == QPOSService.DoTradeResult.MCR) {//Magnetic card
                String content = getString(R.string.card_swiped);
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
                    content += getString(R.string.format_id) + " " + formatID + "\n";
                    content += getString(R.string.masked_pan) + " " + maskedPAN + "\n";
                    content += getString(R.string.expiry_date) + " " + expiryDate + "\n";
                    content += getString(R.string.cardholder_name) + " " + cardHolderName + "\n";
                    content += getString(R.string.service_code) + " " + serviceCode + "\n";
                    content += "trackblock: " + trackblock + "\n";
                    content += "psamId: " + psamId + "\n";
                    content += "posId: " + posId + "\n";
                    content += getString(R.string.pinBlock) + " " + pinblock + "\n";
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
                    content += getString(R.string.format_id) + " " + formatID + "\n";
                    content += getString(R.string.masked_pan) + " " + maskedPAN + "\n";
                    content += getString(R.string.expiry_date) + " " + expiryDate + "\n";
                    content += getString(R.string.cardholder_name) + " " + cardHolderName + "\n";
//					content += getString(R.string.ksn) + " " + ksn + "\n";
                    content += getString(R.string.pinKsn) + " " + pinKsn + "\n";
                    content += getString(R.string.trackksn) + " " + trackksn + "\n";
                    content += getString(R.string.service_code) + " " + serviceCode + "\n";
                    content += getString(R.string.track_1_length) + " " + track1Length + "\n";
                    content += getString(R.string.track_2_length) + " " + track2Length + "\n";
                    content += getString(R.string.track_3_length) + " " + track3Length + "\n";
                    content += getString(R.string.encrypted_tracks) + " " + encTracks + "\n";
                    content += getString(R.string.encrypted_track_1) + " " + encTrack1 + "\n";
                    content += getString(R.string.encrypted_track_2) + " " + encTrack2 + "\n";
                    content += getString(R.string.encrypted_track_3) + " " + encTrack3 + "\n";
                    content += getString(R.string.partial_track) + " " + partialTrack + "\n";
                    content += getString(R.string.pinBlock) + " " + pinBlock + "\n";
                    content += "encPAN: " + encPAN + "\n";
                    content += "trackRandomNumber: " + trackRandomNumber + "\n";
                    content += "pinRandomNumber:" + " " + pinRandomNumber + "\n";
                    cardNo = maskedPAN;
                    String realPan = null;
                    if (!TextUtils.isEmpty(trackksn) && !TextUtils.isEmpty(encTrack2)) {
//                        String clearPan = DUKPK2009_CBC.getDate(trackksn, encTrack2, DUKPK2009_CBC.Enum_key.DATA, DUKPK2009_CBC.Enum_mode.CBC);
//                        content += "encTrack2:" + " " + clearPan + "\n";
//                        realPan = clearPan.substring(0, maskedPAN.length());
//                        content += "realPan:" + " " + realPan + "\n";
                    }
                    if (!TextUtils.isEmpty(pinKsn) && !TextUtils.isEmpty(pinBlock) && !TextUtils.isEmpty(realPan)) {
//                        String date = DUKPK2009_CBC.getDate(pinKsn, pinBlock, DUKPK2009_CBC.Enum_key.PIN, DUKPK2009_CBC.Enum_mode.CBC);
//                        String parsCarN = "0000" + realPan.substring(realPan.length() - 13, realPan.length() - 1);
//                        String s = DUKPK2009_CBC.xor(parsCarN, date);
//                        content += "PIN:" + " " + s + "\n";
                    }
                }
                message.postValue(content);
            }
            else if ((result == QPOSService.DoTradeResult.NFC_ONLINE) || (result == QPOSService.DoTradeResult.NFC_OFFLINE)) {
                nfcLog = decodeData.get("nfcLog");
                String content = getString(R.string.tap_card);
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

                    content += getString(R.string.format_id) + " " + formatID
                            + "\n";
                    content += getString(R.string.masked_pan) + " " + maskedPAN
                            + "\n";
                    content += getString(R.string.expiry_date) + " "
                            + expiryDate + "\n";
                    content += getString(R.string.cardholder_name) + " "
                            + cardHolderName + "\n";

                    content += getString(R.string.service_code) + " "
                            + serviceCode + "\n";
                    content += "trackblock: " + trackblock + "\n";
                    content += "psamId: " + psamId + "\n";
                    content += "posId: " + posId + "\n";
                    content += getString(R.string.pinBlock) + " " + pinblock
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

                    content += getString(R.string.format_id) + " " + formatID
                            + "\n";
                    content += getString(R.string.masked_pan) + " " + maskedPAN
                            + "\n";
                    content += getString(R.string.expiry_date) + " "
                            + expiryDate + "\n";
                    content += getString(R.string.cardholder_name) + " "
                            + cardHolderName + "\n";
                    content += getString(R.string.pinKsn) + " " + pinKsn + "\n";
                    content += getString(R.string.trackksn) + " " + trackksn
                            + "\n";
                    content += getString(R.string.service_code) + " "
                            + serviceCode + "\n";
                    content += getString(R.string.track_1_length) + " "
                            + track1Length + "\n";
                    content += getString(R.string.track_2_length) + " "
                            + track2Length + "\n";
                    content += getString(R.string.track_3_length) + " "
                            + track3Length + "\n";
                    content += getString(R.string.encrypted_tracks) + " "
                            + encTracks + "\n";
                    content += getString(R.string.encrypted_track_1) + " "
                            + encTrack1 + "\n";
                    content += getString(R.string.encrypted_track_2) + " "
                            + encTrack2 + "\n";
                    content += getString(R.string.encrypted_track_3) + " "
                            + encTrack3 + "\n";
                    content += getString(R.string.partial_track) + " "
                            + partialTrack + "\n";
                    content += getString(R.string.pinBlock) + " " + pinBlock
                            + "\n";
                    content += "encPAN: " + encPAN + "\n";
                    content += "trackRandomNumber: " + trackRandomNumber + "\n";
                    content += "pinRandomNumber:" + " " + pinRandomNumber
                            + "\n";
                    cardNo = maskedPAN;
                }
                // pos.getICCTag(QPOSService.EncryptType.PLAINTEXT,1,1,"5F20") // get plaintext or ciphertext 5F20 tag
                // pos.getICCTag(QPOSService.EncryptType.PLAINTEXT,1,2,"5F205F24") // get plaintext or ciphertext 5F20 and 5F24 tag
                message.postValue(content);
              //  sendMsg(8003);
            } else if ((result == QPOSService.DoTradeResult.NFC_DECLINED)) {
                message.postValue(getString(R.string.transaction_declined));
            } else if (result == QPOSService.DoTradeResult.NO_RESPONSE) {
                message.postValue(getString(R.string.card_no_response));
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
            content += getString(R.string.firmware_version) + firmwareVersion + "\n";
            content += getString(R.string.usb) + isUsbConnected + "\n";
            content += getString(R.string.charge) + isCharging + "\n";
//			if (batteryPercentage==null || "".equals(batteryPercentage)) {
            content += getString(R.string.battery_level) + batteryLevel + "\n";
//			}else {
            content += getString(R.string.battery_percentage) + batteryPercentage + "\n";
//			}
            content += getString(R.string.hardware_version) + hardwareVersion + "\n";
            content += "SUB : " + SUB + "\n";
            content += getString(R.string.track_1_supported) + isSupportedTrack1 + "\n";
            content += getString(R.string.track_2_supported) + isSupportedTrack2 + "\n";
            content += getString(R.string.track_3_supported) + isSupportedTrack3 + "\n";
            content += "PCI FirmwareVresion:" + pciFirmwareVersion + "\n";
            content += "PCI HardwareVersion:" + pciHardwareVersion + "\n";
            message.postValue(content);
        }

        /**
         * @see QPOSService.QPOSServiceListener#onRequestTransactionResult(QPOSService.TransactionResult)
         */
        @Override
        public void onRequestTransactionResult(QPOSService.TransactionResult transactionResult) {
            TRACE.d("onRequestTransactionResult()" + transactionResult.toString());
            if (transactionResult == QPOSService.TransactionResult.CARD_REMOVED) {
                clearDisplay();
            }
            dismissDialog();
            dialog = new Dialog(RavenActivity.this);
            dialog.setContentView(R.layout.alert_dialog);
            dialog.setTitle(R.string.transaction_result);
            TextView messageTextView = (TextView) dialog.findViewById(R.id.messageTextView);
            if (transactionResult == QPOSService.TransactionResult.APPROVED) {
                TRACE.d("TransactionResult.APPROVED");
                String message = getString(R.string.transaction_approved) + "\n" + getString(R.string.amount) + ": $" + amount + "\n";
                if (!cashbackAmount.equals("")) {
                    message += getString(R.string.cashback_amount) + ": INR" + cashbackAmount;
                }
                messageTextView.setText(message);
//                    deviceShowDisplay("APPROVED");
            } else if (transactionResult == QPOSService.TransactionResult.TERMINATED) {
                clearDisplay();
                messageTextView.setText(getString(R.string.transaction_terminated));
                onProcessingError(new RuntimeException("Transaction Terminated"),100);
            } else if (transactionResult == QPOSService.TransactionResult.DECLINED) {
                messageTextView.setText(getString(R.string.transaction_declined));
//                    deviceShowDisplay("DECLINED");
            } else if (transactionResult == QPOSService.TransactionResult.CANCEL) {
                clearDisplay();
                messageTextView.setText(getString(R.string.transaction_cancel));
            } else if (transactionResult == QPOSService.TransactionResult.CAPK_FAIL) {
                messageTextView.setText(getString(R.string.transaction_capk_fail));
            } else if (transactionResult == QPOSService.TransactionResult.NOT_ICC) {
                messageTextView.setText(getString(R.string.transaction_not_icc));
            } else if (transactionResult == QPOSService.TransactionResult.SELECT_APP_FAIL) {
                messageTextView.setText(getString(R.string.transaction_app_fail));
            } else if (transactionResult == QPOSService.TransactionResult.DEVICE_ERROR) {
                messageTextView.setText(getString(R.string.transaction_device_error));
            } else if (transactionResult == QPOSService.TransactionResult.TRADE_LOG_FULL) {
                message.postValue("pls clear the trace log and then to begin do trade");
                messageTextView.setText("the trade log has fulled!pls clear the trade log!");
            } else if (transactionResult == QPOSService.TransactionResult.CARD_NOT_SUPPORTED) {
                messageTextView.setText(getString(R.string.card_not_supported));
            } else if (transactionResult == QPOSService.TransactionResult.MISSING_MANDATORY_DATA) {
                messageTextView.setText(getString(R.string.missing_mandatory_data));
            } else if (transactionResult == QPOSService.TransactionResult.CARD_BLOCKED_OR_NO_EMV_APPS) {
                messageTextView.setText(getString(R.string.card_blocked_or_no_evm_apps));
            } else if (transactionResult == QPOSService.TransactionResult.INVALID_ICC_DATA) {
                messageTextView.setText(getString(R.string.invalid_icc_data));
            } else if (transactionResult == QPOSService.TransactionResult.FALLBACK) {
                messageTextView.setText("trans fallback");
            } else if (transactionResult == QPOSService.TransactionResult.NFC_TERMINATED) {
                clearDisplay();
                messageTextView.setText("NFC Terminated");
            } else if (transactionResult == QPOSService.TransactionResult.CARD_REMOVED) {
                clearDisplay();
                messageTextView.setText("CARD REMOVED");
            } else if (transactionResult == QPOSService.TransactionResult.CONTACTLESS_TRANSACTION_NOT_ALLOW) {
                clearDisplay();
                messageTextView.setText("TRANS NOT ALLOW");
            } else if (transactionResult == QPOSService.TransactionResult.CARD_BLOCKED) {
                clearDisplay();
                messageTextView.setText("CARD BLOCKED");
            } else if (transactionResult == QPOSService.TransactionResult.TRANS_TOKEN_INVALID) {
                clearDisplay();
                messageTextView.setText("TOKEN INVALID");
            }else {
                messageTextView.setText("FALL BACK");
                onProcessingError(new RuntimeException("FALL BACK"),100);
            }
            dialog.findViewById(R.id.confirmButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissDialog();
                }
            });
           // dialog.show();
            amount = "";
            cashbackAmount = "";
            //Todo This is where transaction flow ends
        }

        @Override
        public void onRequestBatchData(String tlv) {
            TRACE.d("ICC trade finished");
            String content = getString(R.string.batch_data);
            TRACE.d("onRequestBatchData(String tlv):" + tlv);
            content += tlv;
            TLV = tlv;
            message.postValue(getString(R.string.connecting_bt_pos));
          //  message.postValue(content);
            pos.getQposId();
        }

        @Override
        public void onRequestTransactionLog(String tlv) {
            TRACE.d("onRequestTransactionLog(String tlv):" + tlv);
          dismissDialog();
            String content = getString(R.string.transaction_log);
            content += tlv;
            message.postValue(content);
        }

        @Override
        public void onQposIdResult(Hashtable<String, String> posIdTable) {
            ID = posIdTable.get("posId") == null ? "" : posIdTable.get("posId");
            TRACE.w("onQposIdResult():" + posIdTable.toString());
            String posId = posIdTable.get("posId") == null ? "" : posIdTable.get("posId");
            String csn = posIdTable.get("csn") == null ? "" : posIdTable.get("csn");
            String psamId = posIdTable.get("psamId") == null ? "" : posIdTable
                    .get("psamId");
            String NFCId = posIdTable.get("nfcID") == null ? "" : posIdTable
                    .get("nfcID");
            String content = "";
            content += getString(R.string.posId) + posId + "\n";
            content += "csn: " + csn + "\n";
            content += "conn: " + pos.getBluetoothState() + "\n";
            content += "psamId: " + psamId + "\n";
            content += "NFCId: " + NFCId + "\n";
            message.postValue(getString(R.string.defaultloading));
            TransactionResponse  transactionResponse =   showEmvTransResult();
            onCompleteTransaction(transactionResponse);
        }
        @Override
        public void onRequestSelectEmvApp(ArrayList<String> appList) {
            TRACE.d("onRequestSelectEmvApp():" + appList.toString());
            TRACE.d("Please select App -- S，emv card config");
           dismissDialog();
            dialog = new Dialog(RavenActivity.this);
            dialog.setContentView(R.layout.emv_app_dialog);
            dialog.setTitle(R.string.please_select_app);
            String[] appNameList = new String[appList.size()];
            for (int i = 0; i < appNameList.length; ++i) {

                appNameList[i] = appList.get(i);
            }
         /*
            appListView = (ListView) dialog.findViewById(R.id.appList);
            appListView.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, appNameList));
            appListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    pos.selectEmvApp(position);
                    TRACE.d("select emv app position = " + position);
                    dismissDialog();
                }

            });
            dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    pos.cancelSelectEmvApp();
                    dismissDialog();
                }
            });
            dialog.show();
          */
        }
        @Override
        public void onRequestSetAmount() {
            TRACE.d("input amount -- S");
            TRACE.d("onRequestSetAmount()");
            pos.setAmount(amount, cashbackAmount, "566", QPOSService.TransactionType.PAYMENT);
///            pos.setAmount(amount.concat("00"), cashbackAmount, "566", QPOSService.TransactionType.PAYMENT)
        }

        /**
         * @see QPOSService.QPOSServiceListener#onRequestIsServerConnected()
         */
        @Override
        public void onRequestIsServerConnected() {
            TRACE.d("onRequestIsServerConnected()");
            pos.isServerConnected(true);
        }

        private void getPinBlock(String str){
            HashMap<Integer, Tlv> tlvList2 = pos.getTag(str);
            for(Tlv tlv : tlvList2.values()){
                if(tlv.getTag().equals("C7"))
                    PINBLOCK = tlv.getValue();
            }
        }
        @Override
        public void onRequestOnlineProcess(final String tlv) {
            TRACE.d("onRequestOnlineProcess" + tlv);
            TLV = tlv;
            tagList = getTags();
            ICCDATA = getICCTags();
            getPinBlock(tlv);
            if (isPinCanceled) {
                pos.sendOnlineProcessResult(null);
            }
            else {
//				String str = "5A0A6214672500000000056F5F24032307315F25031307085F2A0201565F34010182027C008407A00000033301018E0C000000000000000002031F009505088004E0009A031406179C01009F02060000000000019F03060000000000009F0702AB009F080200209F0902008C9F0D05D86004A8009F0E0500109800009F0F05D86804F8009F101307010103A02000010A010000000000CE0BCE899F1A0201569F1E0838333230314943439F21031826509F2608881E2E4151E527899F2701809F3303E0F8C89F34030203009F3501229F3602008E9F37042120A7189F4104000000015A0A6214672500000000056F5F24032307315F25031307085F2A0201565F34010182027C008407A00000033301018E0C000000000000000002031F00";
//				str = "9F26088930C9018CAEBCD69F2701809F101307010103A02802010A0100000000007EF350299F370415B4E5829F360202179505000004E0009A031504169C01009F02060000000010005F2A02015682027C009F1A0201569F03060000000000009F330360D8C89F34030203009F3501229F1E0838333230314943438408A0000003330101019F090200209F410400000001";
                String str = "8A023030";//Currently the default value,
                // should be assigned to the server to return data,
                // the data format is TLV
                pos.sendOnlineProcessResult(str);//Script notification/55domain/ICCDATA
            }
            dismissDialog();
            dialog = new Dialog(RavenActivity.this);
            dialog.setContentView(R.layout.alert_dialog);
            dialog.setTitle(R.string.request_data_to_server);
            Hashtable<String, String> decodeData = pos.anlysEmvIccData(tlv);
            TRACE.d("anlysEmvIccData(tlv):" + decodeData.toString());

            message.postValue(getString(R.string.connecting_bt_pos));

            if (isPinCanceled) {
                ((TextView) dialog.findViewById(R.id.messageTextView))
                        .setText(R.string.replied_failed);
            } else {
                ((TextView) dialog.findViewById(R.id.messageTextView))
                        .setText(R.string.replied_success);
            }
            try {
//                    analyData(tlv);// analy tlv ,get the tag you need
            } catch (Exception e) {
                e.printStackTrace();
            }
            dialog.findViewById(R.id.confirmButton).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (isPinCanceled) {
                                pos.sendOnlineProcessResult(null);
                            }
                            else {
//									String str = "5A0A6214672500000000056F5F24032307315F25031307085F2A0201565F34010182027C008407A00000033301018E0C000000000000000002031F009505088004E0009A031406179C01009F02060000000000019F03060000000000009F0702AB009F080200209F0902008C9F0D05D86004A8009F0E0500109800009F0F05D86804F8009F101307010103A02000010A010000000000CE0BCE899F1A0201569F1E0838333230314943439F21031826509F2608881E2E4151E527899F2701809F3303E0F8C89F34030203009F3501229F3602008E9F37042120A7189F4104000000015A0A6214672500000000056F5F24032307315F25031307085F2A0201565F34010182027C008407A00000033301018E0C000000000000000002031F00";
//									str = "9F26088930C9018CAEBCD69F2701809F101307010103A02802010A0100000000007EF350299F370415B4E5829F360202179505000004E0009A031504169C01009F02060000000010005F2A02015682027C009F1A0201569F03060000000000009F330360D8C89F34030203009F3501229F1E0838333230314943438408A0000003330101019F090200209F410400000001";
                                String str = "8A023030";//Currently the default value,
                                // should be assigned to the server to return data,
                                // the data format is TLV
                                pos.sendOnlineProcessResult(str);//Script notification/55domain/ICCDATA

                            }
                            //dismissDialog();
                        }
                    });
          //  dialog.show();
        }

        @Override
        public void onRequestTime() {
            TRACE.d("onRequestTime");
            pos.sendTime(terminalTime);
            //message.postValue(getString(R.string.request_terminal_time) + " " + terminalTime);
        }
//579160EE59A49BE7
        @Override
        public void onRequestDisplay(QPOSService.Display displayMsg) {
            TRACE.d("onRequestDisplay(Display displayMsg):" + displayMsg.toString());
            dismissDialog();
            String msg = "";
            if (displayMsg == QPOSService.Display.CLEAR_DISPLAY_MSG) {
                msg = "";
            } else if (displayMsg == QPOSService.Display.MSR_DATA_READY) {
                AlertDialog.Builder builder = new AlertDialog.Builder(RavenActivity.this);
                builder.setTitle("Audio");
                builder.setMessage("Success,Contine ready");
                builder.setPositiveButton("Confirm", null);
                builder.show();
            } else if (displayMsg == QPOSService.Display.PLEASE_WAIT) {
                msg = getString(R.string.wait);
            } else if (displayMsg == QPOSService.Display.REMOVE_CARD) {
                msg = getString(R.string.remove_card);
            } else if (displayMsg == QPOSService.Display.TRY_ANOTHER_INTERFACE) {
                msg = getString(R.string.try_another_interface);
            } else if (displayMsg == QPOSService.Display.PROCESSING) {
                msg = getString(R.string.processing);
            } else if (displayMsg == QPOSService.Display.INPUT_PIN_ING) {
                msg = "please input pin on pos";

            } else if (displayMsg == QPOSService.Display.INPUT_OFFLINE_PIN_ONLY || displayMsg == QPOSService.Display.INPUT_LAST_OFFLINE_PIN) {
                msg = "please input offline pin on pos";

            } else if (displayMsg == QPOSService.Display.MAG_TO_ICC_TRADE) {
                msg = "please insert chip card on pos";
            } else if (displayMsg == QPOSService.Display.CARD_REMOVED) {
                msg = "card removed";
            }
            message.postValue(msg);
        }

        @Override
        public void onRequestFinalConfirm() {
            TRACE.d("onRequestFinalConfirm() ");
            TRACE.d("onRequestFinalConfirm - S");
            dismissDialog();
            if (!isPinCanceled) {
                dialog = new Dialog(RavenActivity.this);
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
        }

        @Override
        public void onRequestNoQposDetected() {
            TRACE.d("onRequestNoQposDetected()");
            dismissDialog();
            message.postValue(getString(R.string.no_device_detected));
            onProcessingError(new RuntimeException(getString(R.string.no_device_detected)),100);
        }

        @Override
        public void onRequestQposConnected() {
            TRACE.d("onRequestQposConnected()");
            dismissDialog();
            if(!SharedPreferencesUtils.getInstance().getBooleanValue(BaseApplication.getInstance().getString(R.string.loadedDevice),false)){
                message.postValue("Card Injection Processing...");
                pos.updateEMVConfigByXml(new String(FileUtils.readAssetsLine("emv_profile_tlv.xml",BaseApplication.getInstance())));
            }else {
                 //terminalTime = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime());
                 terminalTime = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime());

                pos.setCardTradeMode(QPOSService.CardTradeMode.ONLY_INSERT_CARD);
                //pos.setFormatId("0000");
                pos.doTrade();//start do trade
                //pos.doTrade(30);//start do trade
            }
        }

        @Override
        public void onRequestQposDisconnected() {
            //dismissDialog();
            setTitle(title);
            TRACE.d("onRequestQposDisconnected()");
            message.postValue(getString(R.string.device_unplugged));
            onProcessingError(new RuntimeException(getString(R.string.device_unplugged)),100);
        }

        @Override
        public void onError(QPOSService.Error errorState) {

            TRACE.d("onError" + errorState.toString());
           // dismissDialog();
            if (errorState == QPOSService.Error.CMD_NOT_AVAILABLE) {
                message.postValue(getString(R.string.command_not_available));
            } else if (errorState == QPOSService.Error.TIMEOUT) {
                message.postValue(getString(R.string.device_no_response));
            } else if (errorState == QPOSService.Error.DEVICE_RESET) {
                message.postValue(getString(R.string.device_reset));
            } else if (errorState == QPOSService.Error.UNKNOWN) {
                message.postValue(getString(R.string.unknown_error));
            } else if (errorState == QPOSService.Error.DEVICE_BUSY) {
                message.postValue(getString(R.string.device_busy));
            } else if (errorState == QPOSService.Error.INPUT_OUT_OF_RANGE) {
                message.postValue(getString(R.string.out_of_range));
            } else if (errorState == QPOSService.Error.INPUT_INVALID_FORMAT) {
                message.postValue(getString(R.string.invalid_format));
            } else if (errorState == QPOSService.Error.INPUT_ZERO_VALUES) {
                message.postValue(getString(R.string.zero_values));
            } else if (errorState == QPOSService.Error.INPUT_INVALID) {
                message.postValue(getString(R.string.input_invalid));
            } else if (errorState == QPOSService.Error.CASHBACK_NOT_SUPPORTED) {
                message.postValue(getString(R.string.cashback_not_supported));
            } else if (errorState == QPOSService.Error.CRC_ERROR) {
                message.postValue(getString(R.string.crc_error));
            } else if (errorState == QPOSService.Error.COMM_ERROR) {
                message.postValue(getString(R.string.comm_error));
            } else if (errorState == QPOSService.Error.MAC_ERROR) {
                message.postValue(getString(R.string.mac_error));
            } else if (errorState == QPOSService.Error.APP_SELECT_TIMEOUT) {
                message.postValue(getString(R.string.app_select_timeout_error));
            } else if (errorState == QPOSService.Error.CMD_TIMEOUT) {
                message.postValue(getString(R.string.cmd_timeout));
            } else if (errorState == QPOSService.Error.ICC_ONLINE_TIMEOUT) {
                if (pos == null) {
                    return;
                }
                pos.resetPosStatus();
                message.postValue(getString(R.string.device_reset));
            }
            onProcessingError(new RuntimeException("Oops!, An error occured"),100);
        }

        @Override
        public void onReturnReversalData(String tlv) {
            String content = getString(R.string.reversal_data);
            content += tlv;
            TRACE.d("onReturnReversalData(): " + tlv);
            message.postValue(content);
        }

        @Override
        public void onReturnupdateKeyByTR_31Result(boolean result, String keyType) {
            super.onReturnupdateKeyByTR_31Result(result, keyType);
            if (result) {
                message.postValue("send TR31 key success! The keyType is " + keyType);
            } else {
                message.postValue("send TR31 key fail");
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
            content += getString(R.string.pinKsn) + " " + pinKsn + "\n";
            content += getString(R.string.pinBlock) + " " + pinBlock + "\n";
            message.postValue(content);
            TRACE.i(content);
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
            message.postValue(content);
        }

        @Override
        public void onGetCardNoResult(String cardNo) {
            TRACE.d("onGetCardNoResult(String cardNo):" + cardNo);
            message.postValue("cardNo: " + cardNo);
        }

        @Override
        public void onRequestCalculateMac(String calMac) {
            TRACE.d("onRequestCalculateMac(String calMac):" + calMac);
            if (calMac != null && !"".equals(calMac)) {
                calMac = QPOSUtil.byteArray2Hex(calMac.getBytes());
            }
            message.postValue("calMac: " + calMac);
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
                message.postValue("update work key success");
            } else if (result == QPOSService.UpdateInformationResult.UPDATE_FAIL) {
                message.postValue("update work key fail");
            } else if (result == QPOSService.UpdateInformationResult.UPDATE_PACKET_VEFIRY_ERROR) {
                message.postValue("update work key packet vefiry error");
            } else if (result == QPOSService.UpdateInformationResult.UPDATE_PACKET_LEN_ERROR) {
                message.postValue("update work key packet len error");
            }
        }
//XML does not match device version
        @Override
        public void onReturnCustomConfigResult(boolean isSuccess, String result) {
            TRACE.d("onReturnCustomConfigResult(boolean isSuccess, String result):" + isSuccess + TRACE.NEW_LINE + result);
            message.postValue("result: " + isSuccess + "\ndata: " + result);
            if(isSuccess){
                SharedPreferencesUtils.getInstance().setValue(getResources().getString(R.string.loadedDevice),true);
                terminalTime = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime());
                pos.doTrade();
            }else {
                onProcessingError(new RuntimeException("Emv failed to  inject"),1010);
            }
        }

        @Override
        public void onRequestSetPin() {
            TRACE.i("onRequestSetPin()");

            if(keyId != null){
                if(!TextUtils.isEmpty(keyId)){
                    //binding.amountText.setText(keyId);
                    if (keyId.length() == 4) {
                        clearPinText = keyId;

                        RavenExtensions.INSTANCE.visible(binding.posTranParent);
                        RavenExtensions.INSTANCE.gone(binding.posPinParent);
                        RavenExtensions.INSTANCE.gone(binding.posBluetoothParent);
                        pos.sendPin(keyId);
                        dismissDialog();
                    }
                    else {
                        onCompleteTransaction(new RuntimeException("The length just can input 4digits"),101);
                       Toast.makeText(RavenActivity.this, "The length just can input 4digits", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    RavenExtensions.INSTANCE.gone(binding.posTranParent);
                    RavenExtensions.INSTANCE.visible(binding.posPinParent);
                    RavenExtensions.INSTANCE.gone(binding.posBluetoothParent);
                }
            }else {
                RavenExtensions.INSTANCE.gone(binding.posTranParent);
                RavenExtensions.INSTANCE.visible(binding.posPinParent);
                RavenExtensions.INSTANCE.gone(binding.posBluetoothParent);
            }


            dialog = new Dialog(RavenActivity.this);
            dialog.setContentView(R.layout.pin_dialog);
            dialog.setTitle(getString(R.string.enter_pin));
            dialog.findViewById(R.id.confirmButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String pin = ((EditText) dialog.findViewById(R.id.pinEditText)).getText().toString();
                    if (pin.length() == 4) {
                        clearPinText = pin;
//                        if (pin.equals("000000")) {
//                            pos.sendEncryptPin("5516422217375116");
//
//                        } else {
//                            pos.sendPin(pin);
//                        }
                        pos.sendPin(pin);
                         dismissDialog();
                    } else {
                        Toast.makeText(RavenActivity.this, "The length just can input 4digits", Toast.LENGTH_LONG).show();
                    }
                }
            });
            dialog.findViewById(R.id.bypassButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//					pos.bypassPin();
                    pos.sendPin("");
                 dismissDialog();
                }
            });
            dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    isPinCanceled = true;
                    pos.cancelPin();
                    dismissDialog();
                    onProcessingError(new RuntimeException("PIN Cancelled"),100);
                }
            });
         //  dialog.show();

        }

        @Override
        public void onReturnSetMasterKeyResult(boolean isSuccess) {
            TRACE.d("onReturnSetMasterKeyResult(boolean isSuccess) : " + isSuccess);
            message.postValue("result: " + isSuccess);
        }

        @Override
        public void onReturnBatchSendAPDUResult(LinkedHashMap<Integer, String> batchAPDUResult) {
            TRACE.d("onReturnBatchSendAPDUResult(LinkedHashMap<Integer, String> batchAPDUResult):" + batchAPDUResult.toString());
            StringBuilder sb = new StringBuilder();
            sb.append("APDU Responses: \n");
            for (HashMap.Entry<Integer, String> entry : batchAPDUResult.entrySet()) {
                sb.append("[" + entry.getKey() + "]: " + entry.getValue() + "\n");
            }
            message.postValue("\n" + sb.toString());
        }

        @Override
        public void onBluetoothBondFailed() {
            TRACE.d("onBluetoothBondFailed()");
            message.postValue("bond failed");
        }

        @Override
        public void onBluetoothBondTimeout() {
            TRACE.d("onBluetoothBondTimeout()");
            message.postValue("bond timeout");
        }

        @Override
        public void onBluetoothBonded() {
            TRACE.d("onBluetoothBonded()");
            message.postValue("bond success");
        }

        @Override
        public void onBluetoothBonding() {
            TRACE.d("onBluetoothBonding()");
            message.postValue("bonding .....");
        }

        @Override
        public void onReturniccCashBack(Hashtable<String, String> result) {
            TRACE.d("onReturniccCashBack(Hashtable<String, String> result):" + result.toString());
            String s = "serviceCode: " + result.get("serviceCode");
            s += "\n";
            s += "trackblock: " + result.get("trackblock");
            message.postValue(s);
        }

        @Override
        public void onLcdShowCustomDisplay(boolean arg0) {
            TRACE.d("onLcdShowCustomDisplay(boolean arg0):" + arg0);
        }

        @Override
        public void onUpdatePosFirmwareResult(QPOSService.UpdateInformationResult arg0) {
            TRACE.d("onUpdatePosFirmwareResult(UpdateInformationResult arg0):" + arg0.toString());
            isUpdateFw = false;
//            if (arg0 != QPOSService.UpdateInformationResult.UPDATE_SUCCESS) {
//                updateThread.concelSelf();
//            } else {
//                mhipStatus.setText("");
//            }
//            statusEditText.setText("onUpdatePosFirmwareResult" + arg0.toString());
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
            message.postValue("result:" + arg0);
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
            message.postValue("onReturnNFCApduResult(boolean arg0, String arg1, int arg2):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2);
        }

        @Override
        public void onReturnPowerOffNFCResult(boolean arg0) {
            TRACE.d(" onReturnPowerOffNFCResult(boolean arg0) :" + arg0);
            message.postValue(" onReturnPowerOffNFCResult(boolean arg0) :" + arg0);
        }

        @Override
        public void onReturnPowerOnNFCResult(boolean arg0, String arg1, String arg2, int arg3) {
            TRACE.d("onReturnPowerOnNFCResult(boolean arg0, String arg1, String arg2, int arg3):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2 + TRACE.NEW_LINE + arg3);
            message.postValue("onReturnPowerOnNFCResult(boolean arg0, String arg1, String arg2, int arg3):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2 + TRACE.NEW_LINE + arg3);
        }

        @Override
        public void onCbcMacResult(String result) {
            TRACE.d("onCbcMacResult(String result):" + result);
//            if (result == null || "".equals(result)) {
//                statusEditText.setText("cbc_mac:false");
//            } else {
//                statusEditText.setText("cbc_mac: " + result);
//            }
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
                message.postValue("cardIsExist:" + cardIsExist);
            } else {
                message.postValue("cardIsExist:" + cardIsExist);
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
//            if (arg0) {
//                statusEditText.setText("Set buzzer success");
//            } else {
//                statusEditText.setText("Set buzzer failed");
//            }
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
//            if (arg0) {
//                statusEditText.setText("Set master key success");
//            } else {
//                statusEditText.setText("Set master key failed");
//            }
        }

        @Override
        public void onReturnUpdateIPEKResult(boolean arg0) {
            TRACE.d("onReturnUpdateIPEKResult(boolean arg0):" + arg0);
//            if (arg0) {
//                statusEditText.setText("update IPEK success");
//            } else {
//                statusEditText.setText("update IPEK fail");
//            }
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

            @SuppressLint("MissingPermission")
            @Override
            public void onDeviceFound(BluetoothDevice arg0) {
                if (arg0 != null && arg0.getName() != null) {
                    int icon =
                    arg0.getBondState() == BluetoothDevice.BOND_BONDED ? Integer
                            .valueOf(R.drawable.bluetooth_blue) : Integer
                            .valueOf(R.drawable.bluetooth_blue_unbond);
                        String tt = arg0.getName();
                        String ttrr = arg0.getAddress();
                    //Extract SN and get business name;

                        if(arg0.getName().contains("MPOS")){
                            serialNo = arg0.getName().replace("MPOS","");
                            RetrofitClient.getAPIService().performBluSearch(new BluetoothSearch(serialNo)).enqueue(new Callback<BaseData<BluetoothResponse>>() {
                                @Override
                                public void onResponse(Call<BaseData<BluetoothResponse>> call, Response<BaseData<BluetoothResponse>> response) {
                                    if (response.isSuccessful()) {
                                        BluetoothResponse response1 = null;
                                        if (response.body() != null) {
                                            response1 = response.body().getData();
                                        }
                                        if(response1 != null){
                                            bluetoothModelArrayList.add(new BluetoothModel(arg0.getAddress(),response1.business_name,icon,serialNo));
                                            selectBluetoothDevice(bluetoothModelArrayList);
                                        }
                                    }else{
                                        bluetoothModelArrayList.add(new BluetoothModel(arg0.getAddress(),arg0.getName()+ "(" + arg0.getAddress() + ")",icon,serialNo));
                                        selectBluetoothDevice(bluetoothModelArrayList);
                                    }
                                }
                                @Override
                                public void onFailure(Call<BaseData<BluetoothResponse>> call, Throwable t) {
                                    AppLog.e("performBluSearch",t.getLocalizedMessage());
                                        //Show that there's was no conncetion
                                }
                            });
                        }
                        //Send to the API for business Name
            }
            else {
               // statusEditText.setText("Don't found new device");
                TRACE.d("Don't found new device");
            }
        }

        @Override
        public void onSetSleepModeTime(boolean arg0) {
            TRACE.d("onSetSleepModeTime(boolean arg0):" + arg0);
//            if (arg0) {
//                statusEditText.setText("set the Sleep timee Success");
//            } else {
//                statusEditText.setText("set the Sleep timee unSuccess");
//            }
        }

        @Override
        public void onReturnGetEMVListResult(String arg0) {
            TRACE.d("onReturnGetEMVListResult(String arg0):" + arg0);
            if (arg0 != null && arg0.length() > 0) {
              //  statusEditText.setText("The emv list is : " + arg0);
            }
        }

        @Override
        public void onWaitingforData(String arg0) {
            TRACE.d("onWaitingforData(String arg0):" + arg0);
        }

        @Override
        public void onRequestDeviceScanFinished() {
            TRACE.d("onRequestDeviceScanFinished()");
            //Toast.makeText(RavenActivity.this, R.string.scan_over, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onRequestUpdateKey(String arg0) {
            TRACE.d("onRequestUpdateKey(String arg0):" + arg0);
//            mhipStatus.setText("update checkvalue : " + arg0);
//            if(isUpdateFw){
//                updateFirmware();
//            }
        }

        @Override
        public void onReturnGetQuickEmvResult(boolean arg0) {
            TRACE.d("onReturnGetQuickEmvResult(boolean arg0):" + arg0);
            if (arg0) {
               // statusEditText.setText("emv configed");
                pos.setQuickEmv(true);
            } else {
              //  statusEditText.setText("emv don't configed");
            }
        }

        @Override
        public void onQposDoGetTradeLogNum(String arg0) {
            TRACE.d("onQposDoGetTradeLogNum(String arg0):" + arg0);
            int a = Integer.parseInt(arg0, 16);
            if (a >= 188) {
               // statusEditText.setText("the trade num has become max value!!");
                return;
            }
          //  statusEditText.setText("get log num:" + a);
        }

        @Override
        public void onQposDoTradeLog(boolean arg0) {
            TRACE.d("onQposDoTradeLog(boolean arg0) :" + arg0);
            if (arg0) {
             //   statusEditText.setText("clear log success!");
            } else {
               // statusEditText.setText("clear log fail!");
            }
        }

        @Override
        public void onAddKey(boolean arg0) {
            TRACE.d("onAddKey(boolean arg0) :" + arg0);
//            if (arg0) {
//                statusEditText.setText("ksn add 1 success");
//            } else {
//                statusEditText.setText("ksn add 1 failed");
//            }
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
            //statusEditText.setText("orderId:" + arg1 + "\ntrade log:" + arg0);
        }

        @Override
        public void onRequestDevice() {
            List<UsbDevice> deviceList = getPermissionDeviceList();
            UsbManager mManager = (UsbManager) RavenActivity.this.getSystemService(Context.USB_SERVICE);
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
          //  statusEditText.setText(clearKeys);
            String lenStr = clearKeys.substring(0, 4);
            int sum = 0;
            for (int i = 0; i < 4; i++) {
                int bit = Integer.parseInt(lenStr.substring(i, i + 1));
                sum += bit * Math.pow(16, (3 - i));
            }
            pubModel = clearKeys.substring(4, 4 + sum * 2);
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
          //  statusEditText.setText(result);
        }

        @Override
        public void onFinishMifareCardResult(boolean arg0) {
            TRACE.d("onFinishMifareCardResult(boolean arg0):" + arg0);
//            if (arg0) {
//                statusEditText.setText("finish success");
//            } else {
//                statusEditText.setText("finish fail");
//            }
        }

        @Override
        public void onVerifyMifareCardResult(boolean arg0) {
            TRACE.d("onVerifyMifareCardResult(boolean arg0):" + arg0);
//            if (arg0) {
//                statusEditText.setText(" onVerifyMifareCardResult success");
//            } else {
//                statusEditText.setText("onVerifyMifareCardResult fail");
//            }
        }

        @Override
        public void onReadMifareCardResult(Hashtable<String, String> arg0) {
            if (arg0 != null) {
                TRACE.d("onReadMifareCardResult(Hashtable<String, String> arg0):" + arg0.toString());
                String addr = arg0.get("addr");
                String cardDataLen = arg0.get("cardDataLen");
                String cardData = arg0.get("cardData");
              //  statusEditText.setText("addr:" + addr + "\ncardDataLen:" + cardDataLen + "\ncardData:" + cardData);
            } else {
              //  statusEditText.setText("onReadWriteMifareCardResult fail");
            }
        }

        @Override
        public void onWriteMifareCardResult(boolean arg0) {
            TRACE.d("onWriteMifareCardResult(boolean arg0):" + arg0);
//            if (arg0) {
//                statusEditText.setText("write data success!");
//            } else {
//                statusEditText.setText("write data fail!");
//            }
        }

        @Override
        public void onOperateMifareCardResult(Hashtable<String, String> arg0) {
//            if (arg0 != null) {
//                TRACE.d("onOperateMifareCardResult(Hashtable<String, String> arg0):" + arg0.toString());
//                String cmd = arg0.get("Cmd");
//                String blockAddr = arg0.get("blockAddr");
//                statusEditText.setText("Cmd:" + cmd + "\nBlock Addr:" + blockAddr);
//            } else {
//                statusEditText.setText("operate failed");
//            }
        }

        @Override
        public void getMifareCardVersion(Hashtable<String, String> arg0) {
            if (arg0 != null) {
                TRACE.d("getMifareCardVersion(Hashtable<String, String> arg0):" + arg0.toString());

                String verLen = arg0.get("versionLen");
                String ver = arg0.get("cardVersion");
                //statusEditText.setText("versionLen:" + verLen + "\nverison:" + ver);
            } else {
                //statusEditText.setText("get mafire UL version failed");
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
                //statusEditText.setText("blockAddr:" + blockAddr + "\ndataLen:" + dataLen + "\ncardData:" + cardData);
            } else {
              //  statusEditText.setText("read mafire UL failed");
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
                //statusEditText.setText("dataLen:" + dataLen + "\npack:" + pack);
            } else {
              //  statusEditText.setText("verify UL failed");
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
                //statusEditText.setText("shut down time is : " + Integer.parseInt(arg0, 16) + "s");
            } else {
               // statusEditText.setText("get the shut down time is fail!");
            }
        }

        @Override
        public void onQposDoSetRsaPublicKey(boolean arg0) {
            TRACE.d("onQposDoSetRsaPublicKey(boolean arg0):" + arg0);
            if (arg0) {
               // statusEditText.setText("set rsa is successed!");
            } else {
               // statusEditText.setText("set rsa is failed!");
            }
        }

        @Override
        public void onQposGenerateSessionKeysResult(Hashtable<String, String> arg0) {
//            if (arg0 != null) {
//                TRACE.d("onQposGenerateSessionKeysResult(Hashtable<String, String> arg0):" + arg0.toString());
//                String rsaFileName = arg0.get("rsaReginString");
//                String enPinKeyData = arg0.get("enPinKey");
//                String enKcvPinKeyData = arg0.get("enPinKcvKey");
//                String enCardKeyData = arg0.get("enDataCardKey");
//                String enKcvCardKeyData = arg0.get("enKcvDataCardKey");
//                statusEditText.setText("rsaFileName:" + rsaFileName + "\nenPinKeyData:" + enPinKeyData + "\nenKcvPinKeyData:" +
//                        enKcvPinKeyData + "\nenCardKeyData:" + enCardKeyData + "\nenKcvCardKeyData:" + enKcvCardKeyData);
//            } else {
//                statusEditText.setText("get key failed,pls try again!");
//            }
        }

        @Override
        public void transferMifareData(String arg0) {
            TRACE.d("transferMifareData(String arg0):" + arg0.toString());

            // TODO Auto-generated method stub
//            if (arg0 != null) {
//                statusEditText.setText("response data:" + arg0);
//            } else {
//                statusEditText.setText("transfer data failed!");
//            }
        }

        @Override
        public void onReturnRSAResult(String arg0) {
            TRACE.d("onReturnRSAResult(String arg0):" + arg0.toString());

//            if (arg0 != null) {
//                statusEditText.setText("rsa data:\n" + arg0);
//            } else {
//                statusEditText.setText("get the rsa failed");
//            }
        }

        @Override
        public void onRequestNoQposDetectedUnbond() {
            // TODO Auto-generated method stub
            TRACE.d("onRequestNoQposDetectedUnbond()");
        }

        @Override
        public  void onReturnDeviceCSRResult(String re) {
            TRACE.d("onReturnDeviceCSRResult:"+re);
            //statusEditText.setText("onReturnDeviceCSRResult:"+re);
        }

        @Override
        public  void onReturnStoreCertificatesResult(boolean re) {
            TRACE.d("onReturnStoreCertificatesResult:"+re);
            if(isInitKey){
               // statusEditText.setText("Init key result is :"+re);
                isInitKey = false;
            }else {
              //  statusEditText.setText("Exchange Certificates result is :"+re);
            }

        }

        @Override
        public  void onReturnDeviceSigningCertResult(String certificates, String certificatesTree) {
            TRACE.d("onReturnDeviceSigningCertResult:"+certificates+"\n"+certificatesTree);
            deviceSignCert = certificates;
            String command = getString(R.string.pedi_command,certificates,"1","oeap");
            command = ParseASN1Util.addTagToCommand(command,"CD",certificates);
            TRACE.i("request the RKMS command is "+command);
            String pediRespose = "[AOPEDI;ANY;CC308203B33082029BA00302010202074EB0D60000987E300D06092A864886F70D01010B0500308190310B3009060355040613025553310B300906035504080C0254583114301206035504070C0B53414E20414E544F4E494F31133011060355040A0C0A5669727475437279707431173015060355040C0C0E44737072656164204B44482043413117301506035504410C0E44737072656164204B44482043413117301506035504030C0E44737072656164204B4448204341301E170D3231303330363030303030305A170D3330303330373030303030305A3081A2310B3009060355040613025553310B300906035504080C0254583114301206035504070C0B53414E20414E544F4E494F31133011060355040A0C0A56697274754372797074311D301B060355040C0C14447370726561645F417573524B4D533130312D56311D301B06035504410C14447370726561645F417573524B4D533130312D56311D301B06035504030C14447370726561645F417573524B4D533130312D5630820122300D06092A864886F70D01010105000382010F003082010A0282010100D7FD40DD513EE82491FABA3EB734C3FE69C79973797007A2183EC9C468F73D8E1CB669DDA6DC32CA125F9FAEAC0C0556893C9196FB123B06BC9B880EEF367CD17000C7E0ECF7313DD2D396F29C8D977A65946258BE5A4133462F0675161407EED3D263BC20E9271B9070DCC1A6376F89E7E9E2B304BC756E3E3B61B869A2E39F11067D00B5BA3817673A730F42DC4C037FC214207C70A1E3E43F7D7494E71EBDD5BB0E9AFAE32E422DB90B85E230DF406FB12470AD0360FD7BDFDD1A29BCE91655A835129858A0E9EB04845A80F1E9F8EAA20C67C6B8A61113D6FFDD7DF5719778A03A30F69B0DD9033D5E975F723CC18792CC6988250A7DBD20901450651A810203010001300D06092A864886F70D01010B050003820101008F002AE3AFB49C2E7D99CC7B933617D180CB4E8EA13CBCBE7469FC4E5124033F06E4C3B0DAB3C6CA4625E3CD53E7B86C247CDF100E266059366F8FEEC746507E1B0D0924029805AAB89FCE1482946B8B0C1F546DD56B399AB48891B731148C878EF4D02AE641717A3D381C7B62011B76A6FFBF20846217EB68149C96B4B134F980060A542DBE2F32BF7AD308F26A279B41C65E32D4E260AE68B3010685CE36869EFF09D211CE64401F417A72F29F49A2EE713ACC37C29AECBFEBE571EF11D883815F54FA3E52A917CC3D6B008A3E3C52164FF5591D869026D248873F15DE531104F329C279FC5B6BC28ABC833F8C31BEF47783A5D5B9C534A57530D9AE463DC3;CD308203B33082029BA00302010202074EB0D60000987C300D06092A864886F70D01010B0500308190310B3009060355040613025553310B300906035504080C0254583114301206035504070C0B53414E20414E544F4E494F31133011060355040A0C0A5669727475437279707431173015060355040C0C0E44737072656164204B44482043413117301506035504410C0E44737072656164204B44482043413117301506035504030C0E44737072656164204B4448204341301E170D3231303330373030303030305A170D3330303330383030303030305A3081A2310B3009060355040613025553310B300906035504080C0254583114301206035504070C0B53414E20414E544F4E494F31133011060355040A0C0A56697274754372797074311D301B060355040C0C14447370726561645F417573524B4D533130312D45311D301B06035504410C14447370726561645F417573524B4D533130312D45311D301B06035504030C14447370726561645F417573524B4D533130312D4530820122300D06092A864886F70D01010105000382010F003082010A0282010100A62A4935B57BA478F41B6C8B3F79E84DB61E516FEC8D5BE3E86FD296C6906625E0316A77F59D6D5075811BA7BB0801366BA7E370B758E3E1DCE005008C13D368536C2216FAF8AF70EBC6B5D1D231AFD19D6270DDBEA6535B46135D1DE11F374978A655FAA8C2A0DDC933CF82E9DC69DABF8676D0E81762D9B01799C83A8DF3EE70584AA4543EBBDAB02A0EFCA6A276588893DD28BD096400E315ECF5FE91EC210EEC2BE8763FEFB57D1448CC7D0FCDC3BDCE4B7BAAD546E0E5E99281B4F1AB052E1B0361977406B6B57B32353E9F338BED29E55E2D1F65C4322B5850D45146D5A66BFE8323C0D3E78E55A8945B622E15295B9176454A868399990B31D7B104CF0203010001300D06092A864886F70D01010B05000382010100296101AC1ED80EF9DD845D03F2D1F822B4AEFD50E0A1F47FA98105155327FDA6CE52BCD650BE1EB6DCD7F3CDF73325E85EE979EF0364970ADF6ED3A247B2E3E2D83D877BEBD66B20F3983E8DF8932F82F30C3FAF980ADF72E9FEE30EBAFC42B19FB1EAEC74BAE16E2D4EF245D18B58FB560A64C9B515EA065ECA7AE81D6ED0B97A24636E1E70EE3F2F3A3364C17C6B36BE82588BBED79F23914D4E4E7E1E3FC2A5438FAB0535D37D6FA52009ACD37B6F413700BBF440B6B94E4F12C7F465B8AAC2A03776AAB9AFBAE42FE19664DC0B4E3D8A90EB185529CABE39335AEC58295E1E073A765733410FD769345E9B99C0AA0CBE3FA815661857DCF7EA3BD35EFB4C;RD04916CCC6289600A55118FC37AF0999E;]";
            String cc = ParseASN1Util.parseToken(pediRespose,"CC");
            String cd = ParseASN1Util.parseToken(pediRespose,"CD");
            BASE64Decoder base64Decoder = new BASE64Decoder();
            try {
                String caChain= QPOSUtil.byteArray2Hex(base64Decoder.decodeBuffer(QPOSUtil.readRSANStream(getAssets().open("FX-Dspread-CA-Tree.pem"))));
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
                 //   statusEditText.setText("signature verification successful.");
                    ParseASN1Util.parseASN1new(KB);
                    String data =  ParseASN1Util.getTr31Data();
                    //the api callback is onReturnupdateKeyByTR_31Result
                    pos.updateKeyByTR_31(data,30);
                }
            }else {
                //statusEditText.setText("signature verification failed.");
            }
        }
    }

/*
    private int getKeyIndex() {
        String s = mKeyIndex.getText().toString();
        if (TextUtils.isEmpty(s)) {
            return 0;
        }
        int i = 0;
        try {
            i = Integer.parseInt(s);
            if (i > 9 || i < 0) {
                i = 0;
            }
        } catch (Exception e) {
            i = 0;
            return i;
        }
        return i;
    }

 */

    public void dismissDialog(){
        if (dialog != null){
            dialog.dismiss();
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

    protected  String getPanFromTrack2() {
        String track2 = readTrack2();
        if (track2 != null) {
            for (int i = 0; i < track2.length(); i++) {
                if (track2.charAt(i) == '=' || track2.charAt(i) == 'D') {
                    int endIndex = Math.min(i, 19);
                    return track2.substring(0, endIndex);
                }
            }
        }
        return null;
    }

    public  String readTrack2() {
        String track2 = null;
        try {
            track2 = pos.getICCTag(0,1,"57").get("tlv");
            if (track2 == null || track2.isEmpty()) {
                track2 = pos.getICCTag(0,1,"9F20").get("tlv");
            }
            if (track2 == null || track2.isEmpty()) {
                track2 = pos.getICCTag(0,1,"9F6B").get("tlv");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!TextUtils.isEmpty(track2) && track2.endsWith("F")) {
            return track2.substring(0, track2.length() - 1);
        }
        return track2;
    }
    public  String readPan() {
        String pan = null;
        try {
            pan = pos.getICCTag(0,1,"5A").get("tlv");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(pan)) {
            return getPanFromTrack2();
        }
        if (pan.endsWith("F")) {
            return pan.substring(0, pan.length() - 1);
        }
        return pan;
    }
    private static String getCardHolderFromTrack1(String track1) {
        if (track1 != null && track1.length() > 20) {
            int idx = track1.indexOf('^');
            String temp = track1.substring(idx + 1);
            return temp.substring(0, temp.indexOf('^'));
        }
        return null;
    }

    public  String readCardHolder() {
        String cardHolderName = null;
        try {
            cardHolderName = pos.getICCTag(0,1,"5F20").get("tlv");
            if (cardHolderName == null || cardHolderName.isEmpty()) {
                String track1 =pos.getICCTag(0,1,"56").get("tlv");
                cardHolderName = getCardHolderFromTrack1(track1);
            }
            return cardHolderName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public  TransactionResponse showEmvTransResult() {
        TransactionResponse mResponse = new TransactionResponse();

        try {
            mResponse.amount = String.valueOf(totalAmountPrint);
        //    readPan();
            mResponse.IccData = ICCDATA;
                if(pos.getICCTag(0,1,"4F") != null){
                    mResponse.CardOrg = pos.getICCTag(0,1,"4F").get("tlv");
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(pos.getICCTag(0,1,"5F24") != null){
            mResponse.ExpireDate = pos.getICCTag(0,1,"5F24").get("tlv");
        }
        if(pos.getICCTag(0,1,"5F20") != null){
            mResponse.CardHolderName = pos.getICCTag(0,1,"5F20").get("tlv");
        }else{
            String name = readCardHolder();
            mResponse.CardHolderName = name;
        }

        if(pos.getICCTag(0,1,"9F12") != null){
            mResponse.CardType = pos.getICCTag(0,1,"9F12").get("tlv");
        }

        mResponse.TransactionType = "00"; //POS TRANSACTION
        mResponse.AccountType = accountType;
        if (PINBLOCK != null) {
            mResponse.PinBlock = PINBLOCK;
        }
        if(ID != null){
            mResponse.MacAddress = ID;
        }

        mResponse.Track2 =   extractTag(EmvTags.TRACK_2_EQUIVALENT_DATA);
        mResponse.CardNo =  extractTag(EmvTags.APPLICATION_PRIMARY_ACCOUNT_NUMBER);
        mResponse.CardSequenceNumber = padLeft(extractTag(EmvTags.APPLICATION_PRIMARY_ACCOUNT_NUMBER_SEQUENCE_NUMBER),3, '0');



      String cardSeques=  extractTag(EmvTags.CRYPTOGRAM_INFORMATION_DATA);


        StringBuilder builder = new StringBuilder();
        for(String s : tags){
            List<com.ravenpos.ravendspreadpos.utils.utils.TLV> parse = TLVParser.parse(TLV);
            //String tagR = pos.getICCTag(0,1,s).get("tlv");
            String tag =  Objects.requireNonNull(TLVParser.searchTLV(parse, s)).value;
            if(tag != null){
                builder.append(s);
               SharedPreferencesUtils.getInstance().setValue(s, tag.toUpperCase());
            }
        }

        //readTrack2();
        mResponse.responseCode = _responseCode;
        return mResponse;
    }


    private String extractTag(com.pnsol.sdk.miura.emv.tlv.Tag tag){
        try {
            return pos.getTag(tagList.get(tag.toString())).get(0).getValue();
        }catch (Exception e){
            return "";
        }
    }
    String[] tags = new String[]{
             "9F26", "9F27", "9F10", "9F37", "9F36", "95", "9A", "9C", "9F02", "5F2A", "5F34", "82", "9F1A", "9F03", "9F33", "84", "9F34", "9F35", "9F41","9F12","4F"
            // "9F26", "9F27", "9F10", "9F37", "9F36", "95", "9A", "9C", "9F02", "5F2A", "5F34", "82", "9F1A", "9F03", "9F33", "9F34", "9F35"
    };


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


//        decodeData.put(EmvTags.APPLICATION_CRYPTOGRAM.toString(), pos.getICCTag(0, 1, EmvTags.APPLICATION_CRYPTOGRAM.toString()).get("tlv"));
//        decodeData.put(EmvTags.CRYPTOGRAM_INFORMATION_DATA.toString(), pos.getICCTag(0, 1, EmvTags.CRYPTOGRAM_INFORMATION_DATA.toString()).get("tlv"));
//        decodeData.put(EmvTags.ISSUER_APPLICATION_DATA.toString(), pos.getICCTag(0, 1, EmvTags.ISSUER_APPLICATION_DATA.toString()).get("tlv"));
//        decodeData.put(EmvTags.UNPREDICTABLE_NUMBER.toString(), pos.getICCTag(0, 1, EmvTags.UNPREDICTABLE_NUMBER.toString()).get("tlv"));
//        decodeData.put(EmvTags.APPLICATION_TRANSACTION_COUNTER.toString(), pos.getICCTag(0, 1, EmvTags.APPLICATION_TRANSACTION_COUNTER.toString()).get("tlv"));
//        decodeData.put(EmvTags.TERMINAL_VERIFICATION_RESULTS.toString(), pos.getICCTag(0, 1, EmvTags.TERMINAL_VERIFICATION_RESULTS.toString()).get("tlv"));
//        decodeData.put(EmvTags.TRANSACTION_DATE.toString(), pos.getICCTag(0, 1, EmvTags.TRANSACTION_DATE.toString()).get("tlv"));

        //TERMINAL_VERIFICATION_RESULTS
        return decodeData;
    }
    private String getICCTags(){
        StringBuilder builder = new StringBuilder();
        try {
            for(String s : tags){
                String tag = pos.getICCTag(0,1,s).get("tlv");
                builder.append(tag);
            }
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

}