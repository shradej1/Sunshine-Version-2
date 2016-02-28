package com.example.android.sunshine.app;

import android.text.format.Time;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Given a string of the form returned by the api call:
 * http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7
 * retrieve the maximum temperature for the day indicated by dayIndex
 * (Note: 0-indexed, so 0 would refer to the first day).
 */
public class WeatherDataParser {

    private static final String LOG_TAG = WeatherDataParser.class.getSimpleName();

    private String getReadableDateString(long time) {
        // Because the API returns a UNIX timestamp in seconds, it must be converted
        // to milliseconds in order to be converted to a valid date.

        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd", Locale.getDefault());
        return shortenedDateFormat.format(time);
    }

    /**
     * Prepares the weather high/low for presentation.
     *
     * For presentation, assume the user doesn't care about tenths of a degree
     */
    private String formatHighLows(double high, double low) {
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        return roundedHigh + "/" + roundedLow;
    }

    /**
     * Take the String representing the complete forecase in JSON format and pull out the data we
     * need to construct the Strings needed for the wireframes.
     *
     */
    public String[] getWeatherDataFromJson(String forecatJsonStr, int numDays) throws JSONException {
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecatJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        // OWM returns daily forecasts based upon the local time of the city that is being asked
        // for, which means that we need to know the GMT offset to translate this data properly.

        // Since this data is also sent in-order and the first day is always the current day,
        // we're going to take advantage of that to get a nice normalized UTC date for
        // all of our weather

        Time dayTime = new Time();
        dayTime.setToNow();

        // we start at the day returned by lcal time.  Otherwise, this is a mess.
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        // now we work exculsively in UTC
        dayTime = new Time();

        String[] resultStrs = new String[numDays];
        for(int i = 0; i < weatherArray.length(); i++) {
            JSONObject dayForecast = weatherArray.getJSONObject(i);
            long dateTime;
            dateTime = dayTime.setJulianDay(julianStartDay + i);
            String day = getReadableDateString(dateTime);

            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            String description = weatherObject.getString(OWM_DESCRIPTION);

            JSONObject tempObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = tempObject.getDouble(OWM_MAX);
            double low = tempObject.getDouble(OWM_MIN);

            String highAndLow = formatHighLows(high, low);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }

        for (String s : resultStrs) {
            Log.v(LOG_TAG, "Forecast entry: " + s);
        }
        return resultStrs;
    }

    public static double getMaxTemperatureForDay(String weatherData, int dayIndex) throws JSONException {
        JSONObject array = new JSONObject(weatherData);
        return array.getJSONArray("list")
                .getJSONObject(dayIndex)
                .getJSONObject("temp")
                .getDouble("max");
    }
}
