package smartcity.smartcar.utility;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Helper {

    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Colore da mostrare nella progressBar quando è probabile che la macchina sia chiusa
    public static final int CAR_CLOSED_COLOR = Color.rgb(19, 166, 13);
    // Colore da mostrare nella progressBar quando la macchina non è chiusa
    public static final int CAR_UNCLOSED_COLOR = Color.rgb(217, 0, 0);

    /**
     * Funzione che trova un device accoppiato a partire dal suo nome.
     * Occore prima effettuare l'operazione di pairing, questa funzione non effettua discovery di device nelle vicinanze per trovare
     * quello cercato!
     * @param name Nome del device che si vuole cercare
     * @return device trovato, null altrimenti
     */
    public static BluetoothDevice getDeviceByName(final String name) {
        for(BluetoothDevice b : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            if(b.getName().equals(name)) {
                return b;
            }
        }

        return null;
    }

    public static BluetoothDevice getDeviceByAddress(final String address){
        for(BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            if(device.getAddress().equals(address)) {
                return device;
            }
        }
        return null;
    }

    /**
     * Legge da uno stream di dati una stringa. La funzione riconosce come terminatore il carattere '\n',
     * se manca il metodo entra in un loop infinito.
     * @param input stream di dati da cui leggere la stringa
     * @return la stringa letta
     * @throws IOException
     */
    public static String readFromStream(final InputStream input) throws IOException{
        final int lenght = 1024;
        final byte[] bytes = new byte[lenght];
        boolean exit = false;
        final StringBuilder string = new StringBuilder();

        do {
            int receiveData = input.read(bytes, 0, lenght);

            if(receiveData > 0) {
                final char c = (char) bytes[receiveData - 1];

                if(c == '\n' || c == 13) {
                    receiveData--;
                    exit = true;
                }

                string.append(new String(bytes, 0, receiveData));

            } else {
                exit = true;
            }
        } while(!exit);

        return string.toString();
    }

    /**
     * Restituisce l'indirizzo del device impostato come default.
     * Quando l'applicazione è installata il device di default è HC-05, poi l'utente potrà cambiarlo dalla schermata impostazioni.
     * La funzione esegue le seguenti operazioni:
     * - Controlla se è presente un'impostazione salvata dall'utente
     * - Se è presente controlla se l'indirizzo salvato appartiene ad un device che è accoppiato con il telefono
     * - Se si, restituisce l'indirizzo
     * - Altrimenti significa che l'utente ha disaccoppiato il device, quindi
     *   carica i nomi dei dispositivi di default e controlla se sono accoppiati
     * - Se anche questa ricerca fallisce, resituisce stringa vuota, altrimenti resituisce l'indirizzo del device.
     *
     * @param context
     * @return
     */
    public static String getDefaultDeviceAddress(final String address) {
        if(!address.isEmpty()) {
            if (getDeviceByAddress(address) != null) {
                return address;
            }
        }

        for (String s : getDefaultDeviceNames()) {
            BluetoothDevice device = getDeviceByName(s);
            if(device != null) {
                return device.getAddress();
            }
        }

        return "";
    }

    private static List<String> getDefaultDeviceNames() {
        final List<String> list = new ArrayList<>();
        Collections.addAll(list, "HC-05", "HC-06");
        return Collections.unmodifiableList(list);
    }
}
