package com.example.android.sunshine.app;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by jshrader on 2/27/16.
 */
public class Util {
    public static void close(Closeable closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Log.e("ForecastFragment", "Error closing stream ", e);
                // oh well
            }
        }
    }
}
