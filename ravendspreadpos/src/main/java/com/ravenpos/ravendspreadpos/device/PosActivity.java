package com.ravenpos.ravendspreadpos.device;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.ravenpos.ravendspreadpos.BaseActivity;
import com.ravenpos.ravendspreadpos.R;
import com.ravenpos.ravendspreadpos.databinding.PosLoadingBinding;
import com.ravenpos.ravendspreadpos.pos.EmvTransactionHelper;
import com.ravenpos.ravendspreadpos.pos.TransactionResponse;
import com.ravenpos.ravendspreadpos.utils.MyHandler;
import com.ravenpos.ravendspreadpos.utils.TransactionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PosActivity  extends BaseActivity implements TransactionListener {
    private PosLoadingBinding binding;
    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private MyHandler handler;
    private MutableLiveData<String> message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PosLoadingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        handler = new MyHandler(this,false);
        message = new MutableLiveData<>();
        binding.spinKit.setImageResource(R.drawable.insert_card_one);
        viewObserver();
        proceedToPayment();
    }

    private void proceedToPayment() {
        try {
            showResult(binding.posViewUpdate, getString(R.string.connect));
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
        //handler.sendEmptyMessage(0);
        EmvTransactionHelper.initialize(this);
        EmvTransactionHelper.startTransaction(this, btMacAddress,10.0, message,this);
    }

    @Override
    public void onProcessingError(RuntimeException message, int errorcode) {
        handler.obtainMessage(1, message.getMessage()).sendToTarget();
        finish();
    }

    @Override
    public void onCompleteTransaction(TransactionResponse response) {
        handler.obtainMessage(3, response.responseCode + " : " + response.responseDescription).sendToTarget();
            finish();
    }


    private void viewObserver() {

        message.observe(this, s -> {
            if (!TextUtils.isEmpty(s)) {
                showResult(binding.posViewUpdate, s);
            }
        });
    }



}