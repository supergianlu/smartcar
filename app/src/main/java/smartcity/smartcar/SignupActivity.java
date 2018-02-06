package smartcity.smartcar;

import android.app.ProgressDialog;
import android.os.Bundle;
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


public class SignupActivity extends AppCompatActivity implements UrlConnectionAsyncTask.UrlConnectionListener {
    private static final String TAG = "SignupActivity";

    private EditText nameText;
    private EditText surnameText;
    private EditText usernameText;
    private EditText passwordText;
    private Button signupButton;
    private TextView loginLink;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        nameText = (EditText) findViewById(R.id.input_name);
        surnameText = (EditText) findViewById(R.id.input_surname);
        usernameText = (EditText) findViewById(R.id.input_username);
        passwordText = (EditText) findViewById(R.id.input_password);
        signupButton = (Button) findViewById(R.id.btn_signup);
        loginLink = (TextView) findViewById(R.id.link_login);

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the login activity
                finish();
            }
        });
    }

    public void signup() {
        Log.d(TAG, "Signup");

        if (!validate()) {
            onSignupFailed("");
            return;
        }

        signupButton.setEnabled(false);

        progressDialog = new ProgressDialog(SignupActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creazione account...");
        progressDialog.show();

        String name = nameText.getText().toString();
        String surname = surnameText.getText().toString();
        String username = usernameText.getText().toString();
        String password = passwordText.getText().toString();

        try {
            URL url = new URL(getString(R.string.add_new_user));

            final Bundle data = new Bundle();
            data.putString("username", username);
            data.putString("password", password);
            data.putString("nome", name);
            data.putString("cognome", surname);

            new UrlConnectionAsyncTask(url, SignupActivity.this, getApplicationContext()).execute(data);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }


    public void onSignupSuccess(String s) {
        Toast.makeText(getBaseContext(), s, Toast.LENGTH_LONG).show();
        signupButton.setEnabled(true);
        setResult(RESULT_OK, null);
        finish();
    }

    public void onSignupFailed(String s) {
        Toast.makeText(getBaseContext(), s, Toast.LENGTH_LONG).show();
        signupButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String name = nameText.getText().toString();
        String username = usernameText.getText().toString();
        String password = passwordText.getText().toString();

        if (name.isEmpty() || name.length() < 3) {
            nameText.setError("Minimo 3 caratteri");
            valid = false;
        } else {
            nameText.setError(null);
        }

        if (username.isEmpty() || username.length() < 4) {
            usernameText.setError("Inserire un username valido");
            valid = false;
        } else {
            usernameText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 16) {
            passwordText.setError("Inserire una password compresa tra 4 e 16 caratteri");
            valid = false;
        } else {
            passwordText.setError(null);
        }

        return valid;
    }

    @Override
    public void handleResponse(final JSONObject response, Bundle extra) {
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        String msgResponse;
                        try {
                            msgResponse = response.getString("message");
                        } catch (JSONException e) {
                            msgResponse = "failed";
                        }
                        if(msgResponse.equals("Utente aggiunto correttamente")) {
                            onSignupSuccess(msgResponse);
                        } else {
                            onSignupFailed(msgResponse);
                        }
                        progressDialog.dismiss();
                    }
                }, 10);
    }
}
