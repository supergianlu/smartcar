package smartcity.smartcar;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Implementazione dell'interfaccia Utente
 */
public class UtenteImpl implements Utente, Parcelable {

    public static final Creator<Utente> CREATOR = new Creator<Utente>() {
        @Override
        public Utente createFromParcel(Parcel in) {
            return new UtenteImpl(in);
        }

        @Override
        public Utente[] newArray(int size) {
            return new UtenteImpl[size];
        }
    };

    private static final String JSON_USERNAME = "username";
    private static final String JSON_PASSWORD = "password";
    private static final String JSON_NOME = "nome";
    private static final String JSON_COGNOME = "cognome";

    private String username;
    private String password;
    private String nome;
    private String cognome;

    public UtenteImpl(final JSONObject jsonObject) throws JSONException {
        this.nome = jsonObject.getString(JSON_NOME);
        this.cognome = jsonObject.getString(JSON_COGNOME);
        this.username = jsonObject.getString(JSON_USERNAME);
        this.password = jsonObject.getString(JSON_PASSWORD);
    }

    public UtenteImpl() {
        this.username = "";
        this.password = "";
        this.nome = "";
        this.cognome = "";
    }

    public UtenteImpl(final String username, final String password, final String nome, final String cognome) {
        this.username = username;
        this.password = password;
        this.nome = nome;
        this.cognome = cognome;
    }

    protected UtenteImpl(Parcel in) {
        username = in.readString();
        password = in.readString();
        nome = in.readString();
        cognome = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(username);
        dest.writeString(password);
        dest.writeString(nome);
        dest.writeString(cognome);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void setNome(String nome) {
        this.nome = nome;
    }

    @Override
    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getNome() {
        return this.nome;
    }

    @Override
    public String getCognome() {
        return this.cognome;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public JSONObject generateJSon() {
        final JSONObject jsonObject =  new JSONObject();
        try {
            jsonObject.put(JSON_COGNOME, this.cognome)
                    .put(JSON_NOME, this.nome)
                    .put(JSON_PASSWORD, this.password)
                    .put(JSON_USERNAME, this.username);
        } catch (JSONException e) {
            return new JSONObject();
        }

        return jsonObject;
    }
}
