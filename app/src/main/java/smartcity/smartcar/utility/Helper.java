package smartcity.smartcar.utility;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Classe che contiene un insieme di metodi statici ai quali è utile accedere in diversi punti dell'applicazione
 */
public class Helper {
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int DEFAULT_PROB = 40;
    public static final int NO_PROB = 0;

    /**
     * Legge da uno stream di dati una stringa. La funzione riconosce come terminatore il carattere '\n',
     * se manca il metodo entra in un loop infinito.
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
