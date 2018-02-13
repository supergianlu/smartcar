package smartcity.smartcar.utility;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Helper {
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int DEFAULT_PROB = 40;
    public static final int NO_PROB = 0;

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
        return "";
    }

    public static BluetoothDevice getDeviceByAddress(final String address){
        for(BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            if(device.getAddress().equals(address)) {
                return device;
            }
        }
        return null;
    }


    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Legge una stringa da uno stream di dati.
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static String readStringFromStream(final InputStream inputStream) throws IOException {
        int byteRead = 1;
        byte bytes[] = new byte[1024];
        final StringBuilder stringBuilder = new StringBuilder();

        while(byteRead > 0) {
            byteRead = inputStream.read(bytes, 0, bytes.length);

            if(byteRead > 0) {
                stringBuilder.append(new String(bytes, 0, byteRead));
            }
        }
        return stringBuilder.toString();
    }
}
