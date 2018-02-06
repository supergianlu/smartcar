package smartcity.smartcar;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import smartcity.smartcar.utility.Helper;

/**
 * Classe singleton che tiene traccia del login/logout dell'account dell'utente.
 */
public class AccountManager {

    private static final String PATH = "utente.xml";

    private static boolean login;
    private static boolean error;

    public static final boolean saveUser(final Utente utente, final Context context) {
        try {
            final OutputStream outputStream = context.openFileOutput(PATH, Context.MODE_PRIVATE);
            final String s = utente.generateJSon().toString();
            outputStream.write(s.getBytes());
            outputStream.close();
            Log.d("applicazione", "Salvato utente : " + utente.getUsername());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static final Utente getLoggedUser(final Context context) {
        try {
            final InputStream inputStream = context.openFileInput(PATH);
            final JSONObject jsonObject = new JSONObject(Helper.readStringFromStream(inputStream));
            final Utente utente = new UtenteImpl(jsonObject);
            inputStream.close();
            return utente;
        } catch (IOException | JSONException e) {
            Log.e("applicazione", e.getMessage());
        }

        return null;
    }

    public static boolean reset(final Context context) {
        return context.deleteFile(PATH);
    }

    public static void setFlag (Boolean flag) {
        login = flag;
    }

    public static Boolean isLogged () {
        return login;
    }

    public static void setError (Boolean flag) {
        error = flag;
    }

    public static Boolean isPresentError () {
        return error;
    }
}
