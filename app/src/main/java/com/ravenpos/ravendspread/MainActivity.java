package com.ravenpos.ravendspread;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;

import com.ravenpos.ravendspreadpos.device.RavenActivity;
import com.ravenpos.ravendspreadpos.device.WelcomeActivity;
import com.ravenpos.ravendspreadpos.pos.TransactionResponse;
import com.ravenpos.ravendspreadpos.utils.AppLog;
import com.ravenpos.ravendspreadpos.utils.Constants;
import com.ravenpos.ravendspreadpos.utils.MyHandler;
import com.ravenpos.ravendspreadpos.utils.RavenEmv;
import com.ravenpos.ravendspreadpos.utils.TransactionListener;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity implements TransactionListener {
    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private MyHandler handler;
    private MutableLiveData<String> message;

    private static final String[] BLE_PERMISSIONS = new String[]{
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
    };

    private static final String[] ANDROID_12_BLE_PERMISSIONS = new String[]{
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
    };

    public static void requestBlePermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ActivityCompat.requestPermissions(activity, ANDROID_12_BLE_PERMISSIONS, requestCode);
        else
            ActivityCompat.requestPermissions(activity, BLE_PERMISSIONS, requestCode);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        message = new MutableLiveData<>();
        handler = new MyHandler(this, false);
        findViewById(R.id.btnTest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAccountSelectionActivity(10.0);
            }
        });
        findViewById(R.id.keyDownload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // String tlv = "5F201A5052415645454E204B554D41522042204E20202020202020202F4F07A00000000310105F24032311309F160F4243544553543132333435363738009F21031244089A031907109F02060000000000059F03060000000000009F34034203009F120A564953412044454249549F0607A00000000310105F300202269F4E0F616263640000000000000000000000C408414367FFFFFF0912C10A10218083100492E0000CC70836D3E567845F788FC00A10218083100492E0000CC2820198BBA22DE72324CD77FBFE7BCA8343BC2F26719BBC1F4633FB0E10329E35018CB35077D634CD3A84F998F52DFAC4F0442E2CD03A85D89BFF630D8A85727132E12C88664FBE5A664BB8AA21FF0D10A2D79E324D87B4225A5B9AAC68BD1FFCF5DD334B38D128B02E983DBBD32EC35DBE26CFFA01C11C272F99D8095107DE981818534873828880F1091B8BC62FD39C8394B19E7A410CF9C870CF27986D0CB251E0B6B2D364DE7F3EF1453B397B9FD2D181668510BA16DE250BEC7C1C6A3C12F7006B6B7660D7B331D326D2EA4990F899B4D11AC17D3C0FF63AEF482A349CD8849D906F60B320832E41D8349316E55DE764F8C0AF6ACE3AACA43B3994536A231BE2E790471EB559F4B9FAA5370067B7A0EA3FE59421B7AC17FA5383C6BB3159EBDE3718FEC72CC20EC1AE178386B4F7B3948C97A439AB0F70A386B392276B9B30D8398BAFE3D01AEAB03079368EEF05248E5FAE7BAB070E527981BB25F441A9224AC66DAE623BECDD9B0D1BB05A6EBCAE1E9151FB7AE3E5034B57BD6C3D609276B7743176179A801AD1B378B4629D08263148859ADDE1687CB5E9D0104D84851E5733F4C95D71E880EF20607C";
                //   List<TLV> parse = parse(tlv);
           /*
                try {
                    //encryptPinData();
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
            */
                boolean isEnabled = RavenActivity.isUSBDetected();
                String tt = "";
              //  requestBlePermissions(MainActivity.this,100);
            }
        });
    }

    //08033107755


/*
    private void encryptPinData() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {
        try {
            String clearpinblock = "04319DCBB86B7B6E";
            String pinkey = "9DFB23DC0EE3899B26DFBA372570A151";

            byte[] clearBytes = hexStringToByteArray(clearpinblock);
            byte[] keyBytes = hexStringToByteArray(pinkey);

            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
            cipher.init(1,new SecretKeySpec(keyBytes,"DESede"));

            byte[] encryptedBytes = cipher.doFinal(clearBytes);
            String encryptedPinblock = byteArrayToHexString(encryptedBytes);

            System.out.println(encryptedPinblock);
        }catch (Exception e){
            AppLog.e("byteArrayToHexString",e.getLocalizedMessage());
        }
    }

 */
    private void proceedToPayment() {
        try {
            if(!btAdapter.isEnabled()){
                btAdapter.enable();
                Thread.sleep(2000);
            }
            selectBluetoothDevice();
        }catch (Exception e){}
    }
    private void selectBluetoothDevice() {
        final Set<BluetoothDevice> pairedBTDevices =  btAdapter.getBondedDevices();
        List<String> deviceNames = getDeviceNames(pairedBTDevices);
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames);
        AlertDialog.Builder alertDialog =  new AlertDialog.Builder(this);
        alertDialog.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                String btDevice =  ((BluetoothDevice)pairedBTDevices.toArray()[which]).getAddress();
                startEmV(btDevice);
            }
        });
        alertDialog.setTitle("Select Bluetooth Device");
        alertDialog.show();

    }

    private List<String> getDeviceNames(Set<BluetoothDevice> pairedBTDevices) {
        List<String> temp = new ArrayList<>();
        for(BluetoothDevice device: pairedBTDevices){
            temp.add(device.getName());
        }
        return temp;
    }

    private void startEmV( final String btMacAddress){
        handler.sendEmptyMessage(0);
        //EmvTransactionHelper.initialize(this);
      //  EmvTransactionHelper.startTransaction(this, btMacAddress,10.0, message,this);
    }

    private void startAccountSelectionActivity(Double amount) {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_ACCOUNT_TYPE, "10");
        intent.putExtra(Constants.INTENT_EXTRA_AMOUNT_KEY, amount);
        intent.putExtra(Constants.TERMINAL_ID, "2030LQ01");

        //5849377320EA67F846DC19EA086DCE15
        //  intent.putExtra(Constants.INTENT_CLEAR_MASTER_KEY, "1A6101B94AFDF26B8FAB292A263BF467");
        intent.putExtra(Constants.INTENT_CLEAR_MASTER_KEY, "549DEC3898977CC243A415DCC1BF6457");
        intent.putExtra(Constants.INTENT_CLEAR_PIN_KEY, "9DFB23DC0EE3899B26DFBA372570A151");

        intent.putExtra(Constants.INTENT_Port, "5013");
        intent.putExtra(Constants.INTENT_IP, "196.6.103.18");
        intent.putExtra(Constants.INTENT_MID, "2030LA0C0199436");
        intent.putExtra(Constants.INTENT_SN, "98211206905806");
        intent.putExtra(Constants.INTENT_BUSINESS_NAME_KEY, "RAVENPAY LIMITED       LA           LANG");
        intent.putExtra(Constants.INTENT_CLEAR_SESSION_KEY, "97BCC4618F323BF119103E9E161C589E");
        startActivityForResult(intent, 100);
//Todo for push
    }

    @Override
    public void onProcessingError(RuntimeException message, int errorcode) {
        handler.obtainMessage(1, message.getMessage()).sendToTarget();
    }

    @Override
    public void onCompleteTransaction(TransactionResponse response) {
        handler.obtainMessage(3, response.responseCode + " : " + response.responseDescription).sendToTarget();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
          String re = "";
        if (data != null && data.hasExtra("data")) {
            RavenEmv response = (RavenEmv) data.getSerializableExtra("data");
            new AlertDialog.Builder(this)
                    .setTitle(response.dataModel.RRN)
                    .setMessage(response.dataModel.responseCode)
                    .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
    }
    //
}