package com.parking.management;

import android.os.Bundle;
import android.util.Log;

import java.util.Calendar;

import androidx.appcompat.app.AppCompatActivity;
import ru.slybeaver.slycalendarview.SlyCalendarDialog;
import ru.slybeaver.slycalendarview.SlyCalendarView;

public class Records extends AppCompatActivity implements SlyCalendarDialog.Callback{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        SlyCalendarView view = findViewById(R.id.slyCalendarView);
        view.setCallback(this);


    }

    @Override
    public void onCancelled() {

    }

    @Override
    public void onDataSelected(Calendar firstDate, Calendar secondDate, int hours, int minutes) {
        if (firstDate != null) {
            if (secondDate == null) {
                firstDate.set(Calendar.HOUR_OF_DAY, hours);
                firstDate.set(Calendar.MINUTE, minutes);
                Log.w("Records","Date:: "+firstDate.toString());
//                Toast.makeText(
//                        this,
//                        new SimpleDateFormat(getString(R.string.timeFormat), Locale.getDefault()).format(firstDate.getTime()),
//                        Toast.LENGTH_LONG
//
//                ).show();
            } else {
//                Toast.makeText(
//                        this,
//                        getString(
//                                "periodo",
//                                new SimpleDateFormat(getString(R.string.dateFormat), Locale.getDefault()).format(firstDate.getTime()),
//                                new SimpleDateFormat(getString(R.string.timeFormat), Locale.getDefault()).format(secondDate.getTime())
//                        ),
//                        Toast.LENGTH_LONG
//
//                ).show();
            }
        }
    }
}
