package com.helpers;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.Models.Car;
import com.google.zxing.WriterException;
import com.parking.management.PrinterConnection;
import com.parking.management.R;
import com.reactiveandroid.query.Select;
import com.zj.btsdk.BluetoothService;

import java.util.List;

public class PrinterService extends Service implements BluetoothHandler.HandlerInterface {

    private final String TAG = PrinterService.class.getSimpleName();
    public static final int RC_BLUETOOTH = 0;
    public static final int RC_CONNECT_DEVICE = 1;
    public static final int RC_ENABLE_BLUETOOTH = 2;
    final private int REQUEST_CODE = 200;
    private BluetoothService printService = null;
    private boolean isPrinterReady = false;
    private LocalBroadcastManager localBroadcastManager;


    @Override
    public void onCreate() {
        super.onCreate();

        //Init our broadcast to let the activity know what's happening to the printer
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent!=null) {
            if (intent.getAction().equals(Constants.BT_START)) {//Start our service and make sure the printer is reachable
                if (!isPrinterReady) {
                    String address = intent.getStringExtra(Constants.BT_DEVICE_ADDRESS);
                    Log.i(TAG, "Start printer service...");
                    //setup our BT service for printing
                    printService = new BluetoothService(this, new BluetoothHandler(this));
                    if (address != null) {
                        connectToPrinter(address);
                    } else {
                        if (Constants.WORK_WITHOUT_PRINTER){
                            Toast.makeText(this, "Working without printer", Toast.LENGTH_SHORT).show();
                        }else {
                            Log.e(TAG, "No address for printer");
                            requestPrinterActivity();
                        }
                    }
                } else {
                    sendResult(Constants.BT_CONNECTED);
                }
            } else if (intent.getAction().equals(Constants.BT_STOPS)) {
                Log.e(TAG, "Stop printer service...");
            } else if (intent.getAction().equals(Constants.BT_ACTION_PRINT)) {//Print ticket here
                String date = intent.getStringExtra(Constants.BT_DATE_DATA);
                String barC = intent.getStringExtra(Constants.BT_BARCODE_DATA);
                printTicket(barC, date);
            }else if (intent.getAction().equals(Constants.BT_ACTION_PRINT_HISTORY)){//Print history here
                String date = intent.getStringExtra(Constants.BT_DATE_DATA);
                printHistory(date);
            }else if (intent.getAction().equals(Constants.BT_ACTION_PRINT_RECEIPT)){//Print receipt here
                String barcode = intent.getStringExtra(Constants.BT_BARCODE_DATA);
                String out = intent.getStringExtra(Constants.BT_DATE_DATA);
                printReceipt(barcode, out);
            }
        }
        return START_STICKY;
    }

    private void connectToPrinter(String address){
        //String address = data.getExtras().getString(PrinterConnection.EXTRA_DEVICE_ADDRESS);
        Log.i(TAG, "connectToPrinter");
        try {
            BluetoothDevice btDevice = printService.getDevByMac(address);
            printService.connect(btDevice);
        }catch (IllegalArgumentException e){
            Log.e(TAG, e.toString());
        }

    }

    private void sendResult(String message) {
        Log.w(TAG, "SendResult:: "+message);
        Intent intent = new Intent(Constants.BT_SERVICE);
        intent.putExtra(Constants.BT_STATUS, message);
        //send our stuff
        localBroadcastManager.sendBroadcast(intent);
    }

    private void requestPrinterActivity() {
        if (printService != null) {
            try {
                if (printService.isBTopen()) {
                    Intent findPrinter = new Intent(this, PrinterConnection.class);
                    startActivity(findPrinter);
                    Toast.makeText(this, getString(R.string.bt_printer_service_message), Toast.LENGTH_LONG).show();
                } else {
                    Log.e(TAG, "We need bluetooth on! for this");
                }
            }catch (NullPointerException ex){
                Log.e(TAG, ""+ex.toString());
            }
        }
    }


    public void printTicket(String barcode, String inDate) {
        if (isPrinterReady || Constants.WORK_WITHOUT_PRINTER) {
            Log.i(TAG, "Printing ticket in service!!");
            //Print garage name
            printService.write(PrinterCommands.ESC_ALIGN_CENTER);
            printService.write(PrinterCommands.BOLD_LARGE);
            printService.sendMessage(getString(R.string.app_name), "");
            printService.write(PrinterCommands.NORMAL_SIZE);
            //Print date
            printService.sendMessage(inDate, "");
            //printService.write(PrinterCommands.ESC_ENTER);
            //print QRcode

            byte[] sendData = new byte[0];
            try {
                sendData = Utils.decodeBitmap(BarcodeGenerator.createBarcode(barcode, Constants.BARCODE_TYPE_QR));
            } catch (WriterException e) {
                e.printStackTrace();
            }
            printService.write(PrinterCommands.ESC_ALIGN_CENTER);
            printService.write(sendData);
            printService.sendMessage(getString(R.string.lost_ticket_message),"");
            printService.write(PrinterCommands.ESC_ENTER);
            printService.write(PrinterCommands.ESC_ENTER);

        } else {
            if (printService.isBTopen()) {
                Log.w(TAG, "Let's get our printer");//startActivityForResult(new Intent(this, DeviceActivity.class), RC_CONNECT_DEVICE);
                requestPrinterActivity();
            }


        }
    }

    public void printHistory(String out) {
        if (isPrinterReady || Constants.WORK_WITHOUT_PRINTER) {
            //Log.i(TAG, "Printing ticket in service!!");
            //Print garage name
            printService.write(PrinterCommands.ESC_ALIGN_CENTER);
            printService.write(PrinterCommands.BOLD_LARGE);
            printService.sendMessage(getString(R.string.app_name), "");
            printService.sendMessage(out, "");
            printService.write(PrinterCommands.NORMAL_SIZE);
            printService.write(PrinterCommands.ESC_ALIGN_LEFT);
            //printService.write(PrinterCommands.ESC_ENTER);
            //printService.write(PrinterCommands.ESC_ENTER);
            printService.sendMessage("Plate         "+"Make         "+"Amount", "");
            List<Car> result = Select.from(Car.class).where(" strftime('%Y-%m-%d', date_out)='"+out+"'"+" AND status='"+TicketStatus.MODE_1+"'").fetch();
            double total = 0;
            for (int i=0 ; i<result.size() ; i++){
                Car c = result.get(i);
                total+=c.getTotal();
                //Log.i("Make: ", c.getMake() + " plate: " + c.getPlate());
                printService.sendMessage(c.getPlate()+"         "+c.getMake()+"         "+c.getTotal(), "");
                //printService.write(PrinterCommands.ESC_ENTER);
            }
            //Print total of all the cars that came out that date
            printService.write(PrinterCommands.ESC_ENTER);
            printService.write(PrinterCommands.BOLD_LARGE);
            printService.write(PrinterCommands.ESC_ALIGN_CENTER);
            printService.sendMessage("Total Amount: "+total, "");
            printService.write(PrinterCommands.NORMAL_SIZE);
            //printService.write(PrinterCommands.ESC_ENTER);

            //printService.write(PrinterCommands.ESC_ALIGN_CENTER);
            printService.write(PrinterCommands.ESC_ENTER);
            printService.write(PrinterCommands.ESC_ENTER);

        } else {
            if (printService.isBTopen()) {
                Log.w(TAG, "Let's get our printer");//startActivityForResult(new Intent(this, DeviceActivity.class), RC_CONNECT_DEVICE);
                requestPrinterActivity();
            }


        }
    }

    //TODO: ticket receipt
    public void printReceipt(String barcode, String out){
        if (isPrinterReady || Constants.WORK_WITHOUT_PRINTER) {
            Log.i(TAG, "Printing ticket receipt!!");
            printService.write(PrinterCommands.ESC_ALIGN_CENTER);
            printService.write(PrinterCommands.BOLD_LARGE);
            printService.sendMessage(getString(R.string.app_name), "");
            printService.write(PrinterCommands.NORMAL_SIZE);
            printService.sendMessage(out, "");
            printService.sendMessage(getString(R.string.receipt), "");
            Car checkCar = Select.from(Car.class).where("barcode='"+barcode+"'").fetchSingle();
            printService.sendMessage("\u0024"+checkCar.getTotal(), "");
            printService.sendMessage(checkCar.getPlate()+"  "+checkCar.getMake()+"   "+checkCar.getColor(), "");
            printService.write(PrinterCommands.ESC_ALIGN_CENTER);
            printService.write(PrinterCommands.BOLD_LARGE);
            printService.sendMessage(getString(R.string.paid), "");
            //printService.write(PrinterCommands.ESC_ALIGN_CENTER);
            printService.write(PrinterCommands.ESC_ENTER);
            printService.write(PrinterCommands.ESC_ENTER);

        } else {
            if (printService.isBTopen()) {
                Log.w(TAG, "Let's get our printer");//startActivityForResult(new Intent(this, DeviceActivity.class), RC_CONNECT_DEVICE);
                requestPrinterActivity();
            }


        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "In onDestroy");
    }

    @Override
    public void onDeviceConnected() {
        isPrinterReady = true;
        sendResult(Constants.BT_CONNECTED);
        Log.i(TAG, "Connected to printer");
        Toast.makeText(this, getString(R.string.bt_printer_connected),Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDeviceConnecting() {
        Log.i(TAG, "Connecting to printer");
    }

    @Override
    public void onDeviceConnectionLost() {
        isPrinterReady = false;
        sendResult(Constants.BT_CONNECTION_LOST);
        Toast.makeText(this, getString(R.string.bt_no_printer_message),Toast.LENGTH_LONG).show();
        Log.e(TAG, "Connection to printer lost");
    }

    @Override
    public void onDeviceUnableToConnect() {
        isPrinterReady = false;
        Toast.makeText(this, getString(R.string.bt_no_printer_message),Toast.LENGTH_LONG).show();
        Log.e(TAG, "Can't connect to printer!");
    }

}
