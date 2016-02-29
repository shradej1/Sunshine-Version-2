package com.example.android.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    public static final int NUM_DAYS = 7;
    private ArrayAdapter<String> mForecastAdapter;
    private WeatherDataParser weatherDataParser = new WeatherDataParser();

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // indicates that this fragment has a menu
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Create some dummy data for the ListView.  Here's a sample weekly forecast
        String[] data = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
                "Sun 6/29 - Sunny - 20/7"
        };
        List<String> weekForecast = new ArrayList<>(Arrays.asList(data));

        // Now that we have some dummy forecast data, create an ArrayAdapter.
        // The ArrayAdapter will take data from a source (like our dummy forecast) and
        // use it to populate the ListView it's attached to.
        mForecastAdapter =
                new ArrayAdapter<>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_forecast, // The name of the layout ID.
                        R.id.list_item_forecast_textview, // The ID of the textview to populate.
                        weekForecast);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = mForecastAdapter.getItem(position);
                Toast toast = Toast.makeText(getActivity(), item, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();
            }
        });

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.  The action bar will automatically
        // handle clicks on the Home / Up button, so long as you specifity a parent
        // activity in AndroidManifest.xml.

        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            new FetchWeatherTask().execute("94043");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        private String FORECAST_BASE_URL;

        /**
         * @param args Arguments.  First argument (required) should be postal code.
         */
        @Override
        protected String[] doInBackground(String... args) {
            if (args.length == 0) return null;
            String postalCode = args[0];

            // These two need to be declared outside the try/catch so that they can be closed
            // in the finally block
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily";
                Uri.Builder uriBuilder = Uri.parse(FORECAST_BASE_URL)
                        .buildUpon()
                        .appendQueryParameter("q", postalCode)
                        .appendQueryParameter("mode", "json")
                        .appendQueryParameter("units", "metric")
                        .appendQueryParameter("cnt", Integer.toString(NUM_DAYS))
                        .appendQueryParameter("APPID", BuildConfig.OPEN_WEATHER_MAP_API_KEY);

                URL url = new URL(uriBuilder.build().toString());
                Log.v(LOG_TAG, url.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();
                if (inputStream == null) {
                    // Nothing to do
                    return empty();
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n"); // for debugging purposes if we need to print it.
                }

                if (buffer.length() == 0) {
                    return empty();
                }
                forecastJsonStr = buffer.toString();

                Log.v(LOG_TAG, forecastJsonStr);

                return weatherDataParser.getWeatherDataFromJson(forecastJsonStr, NUM_DAYS);

            } catch (IOException | JSONException e) {
                Log.e(LOG_TAG, "Error ", e);
                return empty();
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
                Util.close(reader);
            }
        }

        @Override
        protected void onPostExecute(String[] strings) {
            mForecastAdapter.clear();
            for (String s : strings) {
                // TODO: calls notifyDatasetChanged() on every add.  >= honeycomb use addAll();
                mForecastAdapter.add(s);
            }
        }

        @NonNull
        private String[] empty() {
            return new String[]{};
        }
    }
}
