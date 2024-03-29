package me.iscle.notiphone.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import me.iscle.notiphone.NotiPhone;
import me.iscle.notiphone.adapter.BluetoothDeviceAdapter;
import me.iscle.notiphone.R;

public class NewDeviceActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int LOCATION_PERMISSION_REQUEST = 1;

    private BluetoothDeviceAdapter bluetoothDeviceAdapter;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    bluetoothDeviceAdapter.addItem(device);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    unregisterReceiver(receiver);

                    ProgressBar scanningBar = findViewById(R.id.scanning_bar);
                    scanningBar.setVisibility(View.INVISIBLE);
                    LinearLayout buttonPanel = findViewById(R.id.button_panel);
                    buttonPanel.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_device);

        // Set result as canceled in case user doesn't choose a device
        setResult(RESULT_CANCELED);

        // Get the default bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Create a new adapter for the RecyclerView and attach an onClickListener to it
        bluetoothDeviceAdapter = new BluetoothDeviceAdapter(new ArrayList<>(bluetoothAdapter.getBondedDevices()), this);

        RecyclerView btDevicesView = findViewById(R.id.bluetooth_device_list);
        btDevicesView.setLayoutManager(new LinearLayoutManager(btDevicesView.getContext()));
        btDevicesView.addItemDecoration(new DividerItemDecoration(btDevicesView.getContext(), DividerItemDecoration.VERTICAL));
        btDevicesView.setAdapter(bluetoothDeviceAdapter);

        Button startDiscovery = findViewById(R.id.start_discovery);
        startDiscovery.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST);
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST);
                }

                return;
            }

            LinearLayout buttonPanel = findViewById(R.id.button_panel);
            buttonPanel.setVisibility(View.INVISIBLE);
            ProgressBar scanningBar = findViewById(R.id.scanning_bar);
            scanningBar.setVisibility(View.VISIBLE);

            bluetoothDeviceAdapter.setItems(new ArrayList<>(bluetoothAdapter.getBondedDevices()));

            // Register for broadcasts when a device is discovered.
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(receiver, filter);

            bluetoothAdapter.startDiscovery();
        });
    }

    @Override
    protected void onDestroy() {
        bluetoothAdapter.cancelDiscovery();

        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
        }

        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        BluetoothDeviceAdapter.ViewHolder vh = (BluetoothDeviceAdapter.ViewHolder) v.getTag();

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            ProgressBar scanningBar = findViewById(R.id.scanning_bar);
            scanningBar.setVisibility(View.INVISIBLE);
            LinearLayout buttonPanel = findViewById(R.id.button_panel);
            buttonPanel.setVisibility(View.VISIBLE);
        }

        // Get the BluetoothDevice from that item
        BluetoothDevice bd = vh.getDevice();
        ((NotiPhone) getApplication()).getNotiPhoneService().connect(bd.getAddress());

        Intent intent = new Intent();
        intent.putExtra("BluetoothAddress", bd.getAddress());
        setResult(RESULT_OK, intent);
        finish();
    }
}
