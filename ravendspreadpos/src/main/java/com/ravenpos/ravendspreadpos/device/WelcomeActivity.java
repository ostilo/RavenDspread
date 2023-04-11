package com.ravenpos.ravendspreadpos.device;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;


import com.ravenpos.ravendspreadpos.R;
import com.ravenpos.ravendspreadpos.databinding.ActivityWelcomeBinding;

public class WelcomeActivity extends AppCompatActivity implements View.OnClickListener {
    private Button audio,serial_port,normal_blu,other_blu,print;
    private Intent intent;
    private static final int LOCATION_CODE = 101;
    private LocationManager lm;//【Location management】
    private AppBarConfiguration appBarConfiguration;
    private ActivityWelcomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
       // setSupportActionBar(binding);
     //   getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        //setTitle(getString(R.string.title_welcome));
        audio=(Button) findViewById(R.id.audio);
        serial_port=(Button) findViewById(R.id.serial_port);
        normal_blu=(Button) findViewById(R.id.normal_bluetooth);
        other_blu=(Button) findViewById(R.id.other_bluetooth);
        print = (Button) findViewById(R.id.print);
        audio.setOnClickListener(this);
        serial_port.setOnClickListener(this);
        normal_blu.setOnClickListener(this);
        other_blu.setOnClickListener(this);
        print.setOnClickListener(this);
     //   bluetoothRelaPer();
    }



    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.audio) {
            //Audio
            // intent = new Intent(this,OtherActivity.class);
            //  intent.putExtra("connect_type", 1);
            // startActivity(intent);
        } else if (id == R.id.serial_port) {//Serial Port
//                intent = new Intent(this, OtherActivity.class);
//                intent.putExtra("connect_type", 2);
//                startActivity(intent);
        } else if (id == R.id.normal_bluetooth) {//Normal Bluetooth
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("connect_type", 3);
            startActivity(intent);
        } else if (id == R.id.other_bluetooth) {//Other Bluetooth，such as：BLE，，，
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("connect_type", 4);
            startActivity(intent);
        } else if (id == R.id.print) {
            //                Log.d("pos","print");
//                intent = new Intent(this, PrintSettingActivity.class);
//                startActivity(intent);
        }
    }

    public void bluetoothRelaPer() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && !adapter.isEnabled()) {//if bluetooth is disabled, add one fix
            Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enabler);
        }
        lm = (LocationManager) WelcomeActivity.this.getSystemService(WelcomeActivity.this.LOCATION_SERVICE);
        boolean ok = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (ok) {//Location service is on
            if (ContextCompat.checkSelfPermission(WelcomeActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e("POS_SDK", "Permission Denied");
                // Permission denied
                // Request authorization
                ActivityCompat.requestPermissions(WelcomeActivity.this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_CODE);
//                        Toast.makeText(getActivity(), "Permission Denied", Toast.LENGTH_SHORT).show();
            } else {
                // have permission
                Toast.makeText(WelcomeActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("BRG", "System detects that the GPS location service is not turned on");
            Toast.makeText(WelcomeActivity.this, "System detects that the GPS location service is not turned on", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 1315);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is agreed by the user
                    Toast.makeText(WelcomeActivity.this, getString(R.string.msg_allowed_location_permission), Toast.LENGTH_LONG).show();
                } else {
                    // Permission is denied by the user
                    Toast.makeText(WelcomeActivity.this, getString(R.string.msg_not_allowed_loaction_permission), Toast.LENGTH_LONG).show();
                }
            }
            break;
        }
    }

}