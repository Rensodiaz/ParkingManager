package com.helpers;

import android.icu.text.SimpleDateFormat;
import android.util.Log;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TotalCost {
    private static final String TAG = TotalCost.class.getSimpleName();

    public static int calculateCost(String inDate, String outDate){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date in = formatter.parse(inDate);
            Date out = formatter.parse(outDate);
            int eightPM = 20;
            long diff = out.getTime() - in.getTime();
            long passedMinutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            long passedHours = TimeUnit.MILLISECONDS.toHours(diff);
            long passedDays = TimeUnit.MILLISECONDS.toDays(diff);

            //Let's get minute down to 60 per hour
            if (passedHours>=1){
                passedMinutes = passedMinutes -(passedHours*60);
            }
            //Let's ge the hours in 24 or less.
            if (passedDays>=1){
                passedHours = passedHours-(passedDays*24);
            }
            //This is to know the day of the week when the vehicle came in
            SimpleDateFormat daysFormat = new SimpleDateFormat("EEEE");
            String dayOfTheWeekIn = daysFormat.format(in);
            //This is to know the hour that the car came in
            Calendar cal = Calendar.getInstance();
            cal.setTime(in);
            int hourIn = cal.get(Calendar.HOUR_OF_DAY);

            Log.i(TAG, "in: "+in);
            Log.i(TAG, "ot: "+out);
            Log.i(TAG, "Hour in: "+hourIn);
            Log.i(TAG, "diff: "+diff);
            Log.i(TAG, "Day: "+passedDays);
            Log.i(TAG, "Hour: "+passedHours);
            Log.i(TAG, "Minute: "+passedMinutes);
            Log.i(TAG, "day of the week: "+dayOfTheWeekIn);

            //Let's see how much is the total
            if (passedMinutes<5 && passedDays==0 && passedHours==0){//If time is less than 5 minutes cost is $0.
                Log.i(TAG, "0 pesos");
                return 0;
            }else if((hourIn>=eightPM || hourIn<=8) && passedDays==0 && passedHours<12 && (dayOfTheWeekIn.equals("Friday") || dayOfTheWeekIn.equals("Saturday") || dayOfTheWeekIn.equals("Sunday"))){//Here we will charge $20 flat if is a weekend after 8pm
                //have to check because people can be coming in at 2am and is friday thinking that is thursday
                if(dayOfTheWeekIn.equals("Friday") && (hourIn<=eightPM)){
                    return 12;
                }
                Log.i(TAG, "Vente pesos weekend");
                return 20;
            }else if(passedHours>=12 && passedDays==0){//If time is more than 12 hours and less than 36 hours cost is $20
                Log.i(TAG, "Mas de 12 horas Vente pesos");
                return 20;
            }else if(passedDays>=1 && passedHours>=12){//if the car have n days and 12+ hours charge the extra 20 after 12 hours
                Log.i(TAG, "mas de un dia y 12 horas 40 dollar");
                return (20*(int)passedDays)+20;
            }else if(passedDays>=1 && (passedMinutes>=1 || passedHours>=1)){//Charge the day plus the next twelve hour
                Log.i(TAG, "Dias mas 12 horas");
                return (20*(int)passedDays)+12;
            }else if (passedDays>=1 && passedHours==0 && passedMinutes==0){//charge only the days if the car goes out at exactly 12am
                Log.i(TAG, "Vente pesos");
                return (20*(int)passedDays);
            }else if(passedDays==0 && ((passedHours>=1 && passedHours<12) || passedMinutes>=5)){
                Log.i(TAG, "12 pesos, mas de 5 minutos y menos de 12 horas");
                if (dayOfTheWeekIn.equals("Monday") && hourIn<=6){
                    return 20;
                }
                return 12;
            }
            return 0;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

}
