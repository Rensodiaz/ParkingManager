package com.parking.management;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.helpers.Constants;
import com.helpers.PrinterService;
import com.zj.btsdk.BluetoothService;

import java.util.Date;
import java.util.Set;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class PrinterConnection extends AppCompatActivity {

    private final String TAG = PrinterConnection.class.getSimpleName();
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    private BluetoothService mService = null;
    private ArrayAdapter<String> newDeviceAdapter;
    private ListView lvPairedDevice;
    private ListView lvNewDevice;
    private TextView tvNewDevice;
    private TextView tvPairedDevice;
    private Button searchBT;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    newDeviceAdapter.add(device.getName() + "\n" + device.getAddress());
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    setTitle(getString(R.string.bt_search_new_device));
                    if (newDeviceAdapter.getCount() == 0) {
                        newDeviceAdapter.add(getString(R.string.bt_no_device_found_message));
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_printer_connection);
        setTitle(getString(R.string.bt_activity_title));
        //setupActionBar();
        lvPairedDevice = findViewById(R.id.paired_devices);
        lvNewDevice = findViewById(R.id.new_devices);
        tvNewDevice = findViewById(R.id.title_new_devices);
        tvPairedDevice = findViewById(R.id.title_paired_devices);
        searchBT = findViewById(R.id.bt_search_button);

        ArrayAdapter<String> pairedDeviceAdapter = new ArrayAdapter<>(this, R.layout.printer_name);
        lvPairedDevice.setAdapter(pairedDeviceAdapter);
        lvPairedDevice.setOnItemClickListener(mDeviceClickListener);

        newDeviceAdapter = new ArrayAdapter<>(this, R.layout.printer_name);
        lvNewDevice.setAdapter(newDeviceAdapter);
        lvNewDevice.setOnItemClickListener(mDeviceClickListener);

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, intentFilter);

        mService = new BluetoothService(this, null);

        Set<BluetoothDevice> pairedDevice = mService.getPairedDev();

        if (pairedDevice.size() > 0) {
            tvPairedDevice.setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevice) {
                pairedDeviceAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedDeviceAdapter.add(getString(R.string.bt_no_device_message));
        }
        searchBT.setOnClickListener(v -> scan());

    }

    public void scan() {
        doDiscovery();
        tvPairedDevice.setVisibility(View.GONE);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mService.cancelDiscovery();

            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);

            Intent intentService = new Intent(PrinterConnection.this, PrinterService.class);
            intentService.setAction(Constants.BT_START);
            intentService.putExtra(Constants.BT_DEVICE_ADDRESS, address);

            startService(intentService);
            finish();
        }
    };

    //Let's get our printer!
    private void sendDataToService(String barcode, Date date){
        Intent startIntent = new Intent(this, PrinterService.class);
        startIntent.setAction(Constants.BT_START);
        startIntent.putExtra(Constants.BT_BARCODE_DATA, barcode);
        startIntent.putExtra(Constants.BT_DATE_DATA, date.toString());
        startService(startIntent);
    }
    private void doDiscovery() {
        setTitle(getString(R.string.bt_search_title));
        tvNewDevice.setVisibility(View.VISIBLE);

        if (mService.isDiscovering()) {
            mService.cancelDiscovery();
        }

        mService.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            mService.cancelDiscovery();
        }
        mService = null;
        unregisterReceiver(mReceiver);
    }
}
