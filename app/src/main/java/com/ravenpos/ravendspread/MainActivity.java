package com.ravenpos.ravendspread;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import com.ravenpos.ravendspreadpos.device.PosActivity;
import com.ravenpos.ravendspreadpos.device.RavenActivity;
import com.ravenpos.ravendspreadpos.device.WelcomeActivity;
import com.ravenpos.ravendspreadpos.pos.EmvTransactionHelper;
import com.ravenpos.ravendspreadpos.pos.TransactionResponse;
import com.ravenpos.ravendspreadpos.utils.Constants;
import com.ravenpos.ravendspreadpos.utils.MyHandler;
import com.ravenpos.ravendspreadpos.utils.TransactionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements TransactionListener {
    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private MyHandler handler;
    private MutableLiveData<String> message;
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
    }

    //08033107755

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
        Intent intent = new Intent(this, RavenActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_ACCOUNT_TYPE, "10");
        intent.putExtra(Constants.INTENT_EXTRA_AMOUNT_KEY, amount);
        intent.putExtra(Constants.TERMINAL_ID, "2030LQ01");

        //5849377320EA67F846DC19EA086DCE15
        //  intent.putExtra(Constants.INTENT_CLEAR_MASTER_KEY, "1A6101B94AFDF26B8FAB292A263BF467");
        intent.putExtra(Constants.INTENT_CLEAR_MASTER_KEY, "5849377320EA67F846DC19EA086DCE15");
        //  intent.putExtra(Constants.INTENT_CLEAR_PIN_KEY, "8F0126CD16E64907FD45FD86F7A14661");
        intent.putExtra(Constants.INTENT_CLEAR_PIN_KEY, "52076DC1C194CD97BFA1BC328CB013E9");
        //52076DC1C194CD97BFA1BC328CB013E9

        intent.putExtra(Constants.INTENT_Port, "5015");
        intent.putExtra(Constants.INTENT_IP, "196.6.103.18");
        intent.putExtra(Constants.INTENT_MID, "2030LA000490601");
        intent.putExtra(Constants.INTENT_SN, "98211206905806");
        intent.putExtra(Constants.INTENT_BUSINESS_NAME_KEY, "NETOP BUSINESS SYSTEMS LA           LANG");
        intent.putExtra(Constants.INTENT_CLEAR_SESSION_KEY, "DF3CB7C2F77E8F80530245CD74147ADF");
        //        intent.putExtra(Constants.INTENT_CLEAR_SESSION_KEY, "4F1F648F40409720730EB64C8C4C8C86");
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
    }
}