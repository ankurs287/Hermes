package com.godfather.ankur.travelbuddy.debug;

import android.util.Log;

/**
 * Created by ujjwalchadha8 on 12/4/2017.
 */

public class Debug {
    public static Object[] log(Object... objects){
        String stringToPrint = "";
        for (Object object : objects) {
            stringToPrint += String.valueOf(object) + ", ";
        }
        stringToPrint = stringToPrint.substring(0, stringToPrint.lastIndexOf(","));
        Log.d("DEBUG:: ", stringToPrint);
        return objects;
    }
}
