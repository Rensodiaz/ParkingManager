package com.parking.management;


import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.Models.Car;
import com.bobekos.bobek.scanner.BarcodeView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.WriterException;
import com.helpers.AppDataBase;
import com.helpers.BarcodeGenerator;
import com.helpers.Constants;
import com.helpers.PrinterService;
import com.helpers.TicketStatus;
import com.helpers.TotalCost;
import com.reactiveandroid.ReActiveAndroid;
import com.reactiveandroid.ReActiveConfig;
import com.reactiveandroid.internal.database.DatabaseConfig;
import com.reactiveandroid.query.Select;
import com.reactiveandroid.query.Update;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import pub.devrel.easypermissions.EasyPermissions;
import spencerstudios.com.ezdialoglib.EZDialog;
import spencerstudios.com.ezdialoglib.EZDialogListener;

//TODO:Printer for this app http://tinyw.in/WAIh

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    final private String TAG = MainActivity.class.getSimpleName();
    private BarcodeView barcode;
    final private int REQUEST_CODE = 200;
    private LinearLayout historyLayout;
    private Button pay;
    private ImageView barcodeImage;
    private TextView date_in;
    private TextView plate;
    private TextView color;
    private TextView make;
    private TextView scanHistory;
    private TextView total;
    private String barcodeToPay;
    private double searchByPlateCharge = 20;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        historyLayout = findViewById(R.id.historyLayout);
        barcode = findViewById(R.id.barcodeView);
        pay = findViewById(R.id.payTicket);
        barcodeImage = findViewById(R.id.barcodeImage);
        plate = findViewById(R.id.plateHistory);
        date_in = findViewById(R.id.dateHistory);
        color = findViewById(R.id.colorHistory);
        make = findViewById(R.id.makeHistory);
        scanHistory = findViewById(R.id.scanHistory);
        total = findViewById(R.id.totalHistory);

        //TODO: Need to check devices version so it doesn't crash the application
        //Let's check if the device is good for this
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= Build.VERSION_CODES.N){
            //Lets' start our DB
            DatabaseConfig appDatabase = new DatabaseConfig.Builder(AppDataBase.class)
                    .addModelClasses(Car.class)
                    .disableMigrationsChecking()
                    .build();
            ReActiveAndroid.init(new ReActiveConfig.Builder(this)
                    .addDatabaseConfigs(appDatabase)
                    .build());

            //Button to add a car
            FloatingActionButton fab = findViewById(R.id.addCar);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent addCar = new Intent(MainActivity.this, AddCar.class);
                    //Delete.from(Car.class).execute();
                    startActivity(addCar);
                }
            });

            //Let's get our permissions for use
            requiresPermissions();
        } else{
            Toast.makeText(this, R.string.device_os_too_low, Toast.LENGTH_LONG).show();
        }
        //mService = new BluetoothService(this, new BluetoothHandler(this));
        //BluetoothDevice mDevice = mService.getDevByMac("66:12:7B:1A:40:A6");
        //mService.connect(mDevice);
    }


    @Override
    public void onResume(){
        super.onResume();
        //start our reader
        startReader();
    }

    private void startReader(){
        barcode.setVibration(500l);
        barcode.getObservable().observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        barcode1 ->
                                checkCarHistory(barcode1.rawValue, false),
                        throwable ->
                                Log.e(TAG, "Something is not cool with us!!!"+throwable.getMessage()));

    }

    private void checkCarHistory(String bc, boolean extraCharge){
        //Check if the car is in the data base first
        //Car checkCar = Select.from(Car.class).where("barcode = ?", bc).fetchSingle();
        Car checkCar = Select.from(Car.class).where("barcode='"+bc+"' AND status='"+TicketStatus.MODE_2+"'").fetchSingle();
        if (checkCar==null) {
            Toast.makeText(this, R.string.no_car_message, Toast.LENGTH_LONG).show();
        }else{
            //Toast.makeText(this, "Car History! "+checkCar.getStatus(), Toast.LENGTH_LONG).show();
            //TODO: Let us know the total cost
            SimpleDateFormat formatterCost = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String outDateCost = formatterCost.format(new Date());
            double totalCost0 = TotalCost.calculateCost(checkCar.getIn(), outDateCost);
            if(extraCharge && totalCost0>0){
                totalCost0 += searchByPlateCharge;
            }
            final double totalCost = totalCost0;
            historyLayout.setVisibility(View.VISIBLE);
            date_in.setText(checkCar.getIn());
            plate.setText(checkCar.getPlate());
            color.setText(checkCar.getColor());
            make.setText(checkCar.getMake());
            total.setText("$"+totalCost);

            try {
                barcodeImage.setImageBitmap(BarcodeGenerator.createBarcode(checkCar.getBarcode(), Constants.BARCODE_TYPE_2D));
            } catch (WriterException e) {
                Log.e(TAG, e.toString());
            }
            //hide scan message
            scanHistory.setVisibility(View.INVISIBLE);
            //Let's pay our ticket
            pay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //tgf123Acura
                    new EZDialog.Builder(MainActivity.this)
                            .setTitle(getString(R.string.pay_dialog_title))
                            .setMessage(getString(R.string.pay_confirmation))
                            .setPositiveBtnText(getString(R.string.yes))
                            .setNegativeBtnText(getString(R.string.no))
                            .setCancelableOnTouchOutside(false)
                            .OnPositiveClicked(new EZDialogListener() {
                                @Override
                                public void OnClick() {
                                    Toast.makeText(MainActivity.this, "paying", Toast.LENGTH_LONG).show();
                                    //updating record to mark the ticket as paid
                                    //Update the date that is coming out
                                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    String out = formatter.format(new Date());
                                    if (totalCost==0){
                                        Update.table(Car.class).set("status = ?", TicketStatus.MODE_3).where("id = ?", checkCar.getId()).execute();
                                    }else {
                                        Update.table(Car.class).set("status = ?", TicketStatus.MODE_1).where("id = ?", checkCar.getId()).execute();
                                        //Only print the receipt if the ticket cost something
                                        Intent startIntent = new Intent(MainActivity.this, PrinterService.class);
                                        startIntent.setAction(Constants.BT_ACTION_PRINT_RECEIPT);
                                        startIntent.putExtra(Constants.BT_BARCODE_DATA, checkCar.getBarcode());
                                        startIntent.putExtra(Constants.BT_DATE_DATA, out);
                                        startService(startIntent);
                                    }
                                    //update the cost column
                                    Update.table(Car.class).set("total = ?", totalCost).where("id = ?", checkCar.getId()).execute();
                                    Update.table(Car.class).set("date_out = ?", out).where("id = ?", checkCar.getId()).execute();

                                }
                            })
                            .OnNegativeClicked(new EZDialogListener() {
                                @Override
                                public void OnClick() {

                                }
                            })
                            .build();
                }
            });
            //TODO: this doesn't go here just for testing the date part only
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String stringDate = checkCar.getIn();
            try {
               Date in = formatter.parse(stringDate);

                System.out.println(formatter.format(in));


                //Give it some time before going away and turn everything back
                new CountDownTimer(10000, 1000) {
                    public void onTick(long millisUntilFinished) {

                    }
                    public void onFinish() {
                        historyLayout.setVisibility(View.INVISIBLE);
                        scanHistory.setVisibility(View.VISIBLE);

                        Date out = new Date();
                        long diff = out.getTime() - in.getTime();
                        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                        long hours = TimeUnit.MILLISECONDS.toHours(diff);
                        long days = TimeUnit.MILLISECONDS.toDays(diff);
                        SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
                        String day = sdf.format(in);
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String out2 = formatter.format(out);

                        Log.w(TAG, "day:: "+day);
                        Log.w(TAG, "hour:: "+hours);
                        Log.w(TAG, "minutes:: "+minutes);
                        Log.w(TAG, "Date:: "+stringDate);
                        Log.w(TAG, "Date out:: "+out2);

                        //This is to know the hour that the car came in
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(in);
                        int hourIn = cal.get(Calendar.HOUR_OF_DAY);
                        Log.w(TAG, "hour in:: "+hourIn);

                    }
                }.start();
            } catch (ParseException e) {
                e.printStackTrace();
            }

        }

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Log.i(TAG, "Admin only!!!");
            Intent settings = new Intent(this, Settings2.class);
            startActivity(settings);
            return true;

        }
        else if (id == R.id.action_search_printer){
            Intent findPrinter = new Intent(MainActivity.this, PrinterConnection.class);
            startActivity(findPrinter);
        }
        else if (id == R.id.action_search_plate){
            Log.i(TAG, "plate Searching!");
            final AlertDialog.Builder alert = new AlertDialog.Builder(this);
            final EditText inputPlate = new EditText(this);
            inputPlate.setFilters(new InputFilter[] { new InputFilter.LengthFilter(10) });

            alert.setTitle(getString(R.string.search_dialog_title));
            alert.setView(inputPlate);
            alert.setPositiveButton(getString(R.string.search), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String plate = inputPlate.getText().toString().trim();
                    //Toast.makeText(getApplicationContext(), value, Toast.LENGTH_SHORT).show();
                    if (plate.isEmpty()){
                        Toast.makeText(MainActivity.this, getString(R.string.search_dialog_empty), Toast.LENGTH_SHORT).show();
                    }else{
                        //get our car and send it to history
                        Car checkPlate = Select.from(Car.class).where("plate='"+plate+"' AND status='"+TicketStatus.MODE_2+"'").fetchSingle();
                        if (checkPlate==null) {
                            Toast.makeText(MainActivity.this, getString(R.string.no_car_message), Toast.LENGTH_LONG).show();
                        }else {
                            checkCarHistory(checkPlate.getBarcode(), true);
                        }
                    }

                }
            });

            alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });
            alert.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void requiresPermissions() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN};
        if (EasyPermissions.hasPermissions(this, perms)) {
            Log.w(TAG, "Have this permission already");
            startBTService();
        }else{
            EasyPermissions.requestPermissions(this, getString(R.string.permission_message), REQUEST_CODE , perms);
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);

        Log.w(TAG, "Scanning Printers!");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE:
                Log.i(TAG, "onActivityResult: bt device is not ready for use");
                break;
        }

    }

    //Let's get our printer!
    private void startBTService(){
        Intent startIntent = new Intent(MainActivity.this, PrinterService.class);
        startIntent.setAction(Constants.BT_START);
        startService(startIntent);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        startBTService();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Log.w(TAG, "Permissions Denied!!!!!");
        Toast.makeText(MainActivity.this, getString(R.string.permission_message), Toast.LENGTH_LONG).show();
        //Let's force the user to give us permissions, LOL
        requiresPermissions();
    }
}
