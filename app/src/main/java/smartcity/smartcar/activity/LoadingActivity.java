package smartcity.smartcar.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

import smartcity.smartcar.AccountManager;
import smartcity.smartcar.R;
import smartcity.smartcar.UrlConnectionAsyncTask;
import smartcity.smartcar.Utente;
import smartcity.smartcar.UtenteImpl;
import smartcity.smartcar.model.ParkingContent;

public class LoadingActivity extends Activity implements UrlConnectionAsyncTask.UrlConnectionListener {
    private String username;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        final SharedPreferences prefs = getSharedPreferences("MY_PREFS_NAME", MODE_PRIVATE);
        username = prefs.getString("username", null);
        final String password = prefs.getString("password", null);
        if(username != null && password != null){
            checkLogin(username, password);
        } else {
            startLoginActivity();
        }
    }

    private void checkLogin(String username, String password) {
        final Bundle data = new Bundle();
        data.putString("username", username);
        data.putString("password", password);
        try {
            new UrlConnectionAsyncTask(new URL(getString(R.string.login_url)), this, getApplicationContext()).execute(data);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleResponse(JSONObject response, Bundle extra) {
        if(response.length() != 0) {
            try {
                final int code = response.getInt("code");
                if(code == LOGIN_SUCCESS) {
                    final Utente utente = new UtenteImpl(response.getJSONObject("extra").getJSONObject("utente"));
                    AccountManager.saveUser(utente, getApplicationContext());
                    startActivity(new Intent(this, MapActivity.class));
                    finish();
                } else {
                    startLoginActivity();
                }
            } catch (JSONException e) {
                startLoginActivity();
                e.printStackTrace();
            }
        } else {
            startLoginActivity();
        }
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
