package com.parking.management;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.Models.Car;
import com.helpers.Constants;
import com.helpers.PrinterService;
import com.helpers.TicketStatus;
import com.reactiveandroid.query.Select;

import java.util.Date;

public class AddCar extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private final String TAG = AddCar.class.getSimpleName();
    private Spinner colorSpinner;
    private Spinner makeSpinner;
    private Button addCar;
    private Button cancel;
    private LinearLayout addCarForm;
    private TextView plateTextView;
    private String barcode;
    private boolean isPrinterReady = false;
    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car);

        initEverything();
        initSelectionSpinner();
        checkPrinterService();
        //Let's open the keyboard immediately
        InputMethodManager imm = (InputMethodManager)getSystemService(this.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,InputMethodManager.HIDE_IMPLICIT_ONLY);

        addCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (plateTextView.getText().toString().trim().equals("")) {
                    Toast.makeText(AddCar.this, R.string.no_empty_plate, Toast.LENGTH_LONG).show();
                } else {
                    String selectedColor = colorSpinner.getSelectedItem().toString();
                    String selectedMake = makeSpinner.getSelectedItem().toString();
                    String plateString = plateTextView.getText().toString();
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String in = formatter.format(new Date());
                    barcode = plateString + selectedMake + in;

                    //Check if the car is in the data base first
                    Car checkCar = Select.from(Car.class).where("plate = ?", plateString).fetchSingle();
                    if (checkCar == null || (checkCar!=null && checkCar.getStatus().equals(TicketStatus.MODE_1))) {
                        if (isPrinterReady || Constants.WORK_WITHOUT_PRINTER) {
                            Log.i("addCar", "Creating record...."+in);
                            Car newCar = new Car(plateTextView.getText().toString(), selectedColor, selectedMake, barcode, in, TicketStatus.MODE_2);
                            newCar.save();
                            Toast.makeText(AddCar.this, getString(R.string.addCar_creating_ticket), Toast.LENGTH_LONG).show();
                            //TODO:Here we have to print the ticket
                            sendDataToService(barcode, in);
                            hideKeyboard();
                            finish();
                        } else {
                            Log.e(TAG, "Printer is not ready yet!!!!");
                            Toast.makeText(AddCar.this, getString(R.string.bt_no_printer_message),Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(AddCar.this, getString(R.string.car_in_already_message), Toast.LENGTH_LONG).show();
                        Log.e("addCar", "Record in database already!!!!");
                    }
                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                finish();
            }
        });

        //register our local broadcast receiver to see the status of the printer
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    String status = intent.getStringExtra(Constants.BT_STATUS);
                    if (status != null) {
                        Log.i(TAG, "data from service:: " + status);
                        if (status.equals(Constants.BT_CONNECTED)) {
                            isPrinterReady = true;
                        } else if (status.equals(Constants.BT_CONNECTION_LOST)) {
                            isPrinterReady = false;
                        }
                    } else {
                        Log.i(TAG, "No data from service yet");
                    }
                }
            }
        };
    }

    //Let's get our printer!
    private void sendDataToService(String barcode, String date){
        Intent startIntent = new Intent(this, PrinterService.class);
        startIntent.setAction(Constants.BT_ACTION_PRINT);
        startIntent.putExtra(Constants.BT_BARCODE_DATA, barcode);
        startIntent.putExtra(Constants.BT_DATE_DATA, date);
        startService(startIntent);
    }

    private void checkPrinterService(){
        Intent startIntent = new Intent(this, PrinterService.class);
        startIntent.setAction(Constants.BT_START);
        startService(startIntent);
    }
    private void hideKeyboard(){
        //let's close the activity and hide the keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(AddCar.this.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(plateTextView.getWindowToken(), 0);
    }

    private void initEverything(){
        addCarForm = findViewById(R.id.linearLayoutForm);
        plateTextView = findViewById(R.id.addPlate);
        cancel = findViewById(R.id.btnCancel);
        addCar = findViewById(R.id.btnAddCar);
        colorSpinner = findViewById(R.id.add_color);
        makeSpinner = findViewById(R.id.add_make);
    }


    private void initSelectionSpinner() {

        // Set SpinnerActivity as the item selected listener
        //colorSpinner.setOnItemSelectedListener(this);
        //makeSpinner.setOnItemSelectedListener(this);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Toast.makeText(this, colorSpinner.getSelectedItem() + " selected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((broadcastReceiver), new IntentFilter(Constants.BT_SERVICE));
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onStop();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }
}
