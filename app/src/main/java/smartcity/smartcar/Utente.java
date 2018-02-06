package smartcity.smartcar;

import org.json.JSONObject;

/**
 * Interfaccia di un Utente autenticato con l'applicazione
 */
public interface Utente {

    void setNome(final String nome);
    void setCognome(final String cognome);
    void setUsername(final String username);
    void setPassword(final String password);
    String getNome();
    String getCognome();
    String getPassword();
    String getUsername();
    JSONObject generateJSon();
}
