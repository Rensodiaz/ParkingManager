package com.parking.management;

import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.helpers.Constants;
import com.helpers.PrinterService;
import com.savvi.rangedatepicker.CalendarPickerView;
import com.savvi.rangedatepicker.SubTitle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

public class Settings2 extends AppCompatActivity {

    CalendarPickerView calendar;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings2);

        final java.util.Calendar nextYear = java.util.Calendar.getInstance();
        nextYear.add(Calendar.YEAR, 10);

        final java.util.Calendar lastYear = java.util.Calendar.getInstance();
        lastYear.add(Calendar.YEAR, - 10);

        calendar = findViewById(R.id.calendar_view);
        button = findViewById(R.id.get_selected_dates);

        calendar.init(lastYear.getTime(),
                nextYear.getTime(),
                new SimpleDateFormat("MMMM, YYYY", Locale.getDefault())) //
                .inMode(CalendarPickerView.SelectionMode.SINGLE);

        calendar.scrollToDate(new Date());


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Date cDate = calendar.getSelectedDate();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                String out = formatter.format(cDate);
                //Toast.makeText(Settings2.this, "" + cDate.toString(), Toast.LENGTH_LONG).show();
                Log.i("Settings: ", "date1: "+out);
                sendDataToService(out);

                //Log.i("Settings: ", "date2: "+new Date().toString());
                //where CAST(delivery_date as date) = CAST(getdate() as date)
                //where strftime('%Y-%m-%d', mydate) = '2018-03-18'
                //List<Car> result = Select.from(Car.class).where(" strftime('%Y-%m-%d', date_in)='"+in+"'").fetch();
                /*List<Car> result = Select.from(Car.class).where(" strftime('%Y-%m-%d', date_out)='"+out+"'"+" AND status='"+ TicketStatus.MODE_1+"'").fetch();
                for (int i=0 ; i<result.size() ; i++){
                    Car c = result.get(i);
                    Log.i("Settings: ", c.getMake() + " plate: " + c.getPlate());
                }*/
            }
        });
    }

    //Let's get our printer!
    private void sendDataToService(String date){
        Intent startIntent = new Intent(this, PrinterService.class);
        startIntent.setAction(Constants.BT_ACTION_PRINT_HISTORY);
        startIntent.putExtra(Constants.BT_DATE_DATA, date);
        startService(startIntent);
    }

    private ArrayList<SubTitle> getSubTitles() {
        final ArrayList<SubTitle> subTitles = new ArrayList<>();
        final Calendar tmrw = Calendar.getInstance();
        tmrw.add(Calendar.DAY_OF_MONTH, 1);
        subTitles.add(new SubTitle(tmrw.getTime(), "₹1000"));
        return subTitles;
    }
}
