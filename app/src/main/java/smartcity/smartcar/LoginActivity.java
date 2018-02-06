package smartcity.smartcar;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

import smartcity.smartcar.utility.Helper;


public class LoginActivity extends AppCompatActivity implements UrlConnectionAsyncTask.UrlConnectionListener {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;

    private EditText usernameText;
    private EditText passwordText;
    private Button loginButton;
    private TextView signupLink;
    private String username;
    private String password;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        usernameText = findViewById(R.id.input_username);
        passwordText = findViewById(R.id.input_password);
        loginButton = findViewById(R.id.btn_login);
        signupLink = findViewById(R.id.link_signup);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
                loginButton.setEnabled(false);
            }
        });

        signupLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
            }
        });

        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        if(!Helper.hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        SharedPreferences prefs = getSharedPreferences("MY_PREFS_NAME", MODE_PRIVATE);
        String username = prefs.getString("username", null);
        String password = prefs.getString("password", null);
        if (username != null && password != null) {
            usernameText.setText(username);
            passwordText.setText(password);
        }
    }

    public void login() {
        Log.d(TAG, "login");

        username = usernameText.getText().toString();
        password = passwordText.getText().toString();

        checkLogin(username,password);
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
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
                    SharedPreferences.Editor editor = getSharedPreferences("MY_PREFS_NAME", MODE_PRIVATE).edit();
                    editor.putString("username", username);
                    editor.putString("password", password);
                    editor.apply();
                    startActivity(new Intent(this, MapActivity.class));
                    finish();
                } else if(code == LOGIN_FAILED) {
                    loginButton.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "Username e/o password errati", Toast.LENGTH_LONG).show();
                } else {
                    loginButton.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "Errore sconosciuto, riprovare", Toast.LENGTH_LONG).show();
                }

            } catch (JSONException e) {
                loginButton.setEnabled(true);
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Errore durante il login", Toast.LENGTH_LONG).show();
            }

        } else {
            loginButton.setEnabled(true);
            Toast.makeText(getApplicationContext(), "JSON vuoto", Toast.LENGTH_LONG).show();
        }
    }


}
