package com.example.lifestyleapp.repositories;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.MutableLiveData;

import com.example.lifestyleapp.MainDrawerActivity;
import com.example.lifestyleapp.models.AppDatabase;
import com.example.lifestyleapp.models.User;
import com.example.lifestyleapp.models.UserDao;
import com.example.lifestyleapp.ui.weather.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class WeatherRepository {

    private MutableLiveData<String> city = new MutableLiveData<>();
    private MutableLiveData<User> mUser = new MutableLiveData<>();
    private static UserDao mUserDao;
    private static MutableLiveData<Weather> userLocationWeather;

    public WeatherRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        mUserDao = db.userDao();
        loadData();
    }

    public MutableLiveData<User> getUser() {
        return mUser;
    }

    public MutableLiveData<Weather> getUserLocationWeather() {
        return userLocationWeather;
    }

    public MutableLiveData<String> getUserCity() {
        return city;
    }

    private static class getUserAsyncTask extends AsyncTask<Long, Void, User> {
        private WeakReference<WeatherRepository> mRepoWReference;

        getUserAsyncTask(WeatherRepository repo) {
            mRepoWReference = new WeakReference<WeatherRepository>(repo);
        }

        @Override
        protected User doInBackground(Long... longs) {
            return mUserDao.getUser(longs[0]);
        }

        @SuppressLint("StaticFieldLeak")
        @Override
        protected void onPostExecute(User returnedUser) {
            WeatherRepository localWRvar = mRepoWReference.get();
            localWRvar.mUser.setValue(returnedUser);
            String city = returnedUser.getCity();
            localWRvar.city.setValue(returnedUser.getCity());


            new AsyncTask<String, Void, Weather>() {

                @Override
                protected Weather doInBackground(String... strings) {
                    String city = strings[0];

                    try {
                        return getWeather(city);
                    } catch (IOException | JSONException e) {
                        return new Weather("--", 0);
                    }
                }


                @Override
                protected void onPostExecute(Weather weather) {
                    super.onPostExecute(weather);
                    try {
                        userLocationWeather.setValue(weather);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.execute(city);
        }
    }

    @SuppressLint("StaticFieldLeak")
    void loadData() {
        userLocationWeather = new MutableLiveData<>();
        city = new MutableLiveData<>();
        new getUserAsyncTask(this).execute(MainDrawerActivity.userPrimaryKey);
    }

    private static URL buildOpenWeatherAPIURL(String cityName) throws MalformedURLException {
        String apiKey = "&appid=63e730362b278faf6db7254c1f3837d8";
        String urlBuild = "https://api.openweathermap.org/data/2.5/weather?q=";
        urlBuild += cityName + apiKey;
        URL url = new URL(urlBuild);
        System.out.println(url);
        return url;
    }

    private static InputStream sendAPIHTTPRequest(URL openWeatherFormattedURL) throws IOException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) openWeatherFormattedURL.openConnection();
        InputStream in;
        try {
            in = new BufferedInputStream(urlConnection.getInputStream());
            return in;
        } catch (IOException e) {
            System.out.println(e);
            String failureToGetWeather = "--";
            in = new ByteArrayInputStream(failureToGetWeather.getBytes());
            return in;
        } finally {
            urlConnection.disconnect();
        }
    }

    private static String readInputStream(InputStream in) throws IOException {
        int i;
        char c;
        String inString = "";
        while ((i = in.read()) != -1) {
            c = (char) i;
            inString += c;
        }

        return inString;
    }

    private static Weather JSONToWeather(String weatherData) throws JSONException {
        JSONObject weatherDataJSON = new JSONObject(weatherData);

        JSONArray weatherArr = weatherDataJSON.getJSONArray("weather");
        JSONObject weatherObjects = (JSONObject) weatherArr.get(0);
        String conditions = weatherObjects.getString("description");

        JSONObject main = weatherDataJSON.getJSONObject("main");
        String temp = main.getString("temp");
        float tempKelvin = Float.parseFloat(temp);
        Weather userLocationWeather = new Weather(conditions, (int) tempKelvin);
        return userLocationWeather;
    }


    // 63e730362b278faf6db7254c1f3837d8
    private static Weather getWeather(String city) throws IOException, JSONException {
        URL url = buildOpenWeatherAPIURL(city);

        InputStream in = sendAPIHTTPRequest(url);

        String weatherData = readInputStream(in);

        if (weatherData.length() <= 2)
            return new Weather("--", 0);
        Weather userLocationWeather = JSONToWeather(weatherData);
        return userLocationWeather;
    }
}
