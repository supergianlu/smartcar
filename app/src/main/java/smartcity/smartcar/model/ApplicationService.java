package smartcity.smartcar.model;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;

import smartcity.smartcar.UrlConnectionAsyncTask;
import smartcity.smartcar.activity.BluetoothActivity;
import smartcity.smartcar.R;
import smartcity.smartcar.activity.SignupActivity;
import smartcity.smartcar.utility.Helper;

import static smartcity.smartcar.model.Event.*;
import static smartcity.smartcar.model.ParkingContent.ITEMS;
import static smartcity.smartcar.utility.Helper.DEFAULT_PROB;
import static smartcity.smartcar.utility.Helper.NO_PROB;


/**
 * Service che implementa la logica dell'applicazione.
 * Una volta avviato continua a lavorare in background finchè l'applicazione non viene chiusa.
 * Come ogni service viene attivato con un Intent esplicito, nell'Intent può essere inserito un indirizzo fisico di un device
 * attraverso il metodo Intent.putStringExtra("address", stringaIndirizzo). Il Service si connetterà quindi a quell'indirizzo
 * Inoltre resta in ascolto di tre Intent attraverso un LocalBroadcastReceiver:
 *
 * - SET_DEVICE, permette connettersi ad un altro dispositivo. Nell'Intent va inserito l'indirizzo del dispositivo usando il metodo
 * Intent.putStringExtra("address", stringaIndirizzo)
 *
 * - CLOSE_CONNECTION, chiude la connessione bluetooth e interrompe l'applicazione.
 *
 * - STOP_SERVICE, interrompe la computazione, chiude la connessione e termina il service.
 *
 * N.B : Questi Intent devono essere mandati usando il metodo LocalBroadcastManager.sendBroadcast(intent) in quanto
 *       questo Service utilizza un receiver locale
 */
public class ApplicationService extends Service {


    private ConnectionHandlerThread connectionHandlerThread;
    private long lastUpdateTime; //TODO così utile sta roba del tempo?

    private final IBinder mBinder = new MyBinder();
    private SharedPreferences prefs;
    private String deviceAddress;
    private int probability = NO_PROB;
    private Event event;
    private boolean running;

    public class MyBinder extends Binder {
        public ApplicationService getService() {
            return ApplicationService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        prefs = getSharedPreferences("MY_PREFS_NAME", MODE_PRIVATE);
        deviceAddress = prefs.getString("myDeviceAddress", null);
        if (deviceAddress == null) {
            stopComputing();
            stopSelf();
            Toast.makeText(this, "Selezionare un device bluetooth dalle impostazioni", Toast.LENGTH_SHORT).show();
        } else {
            startApplicationService(deviceAddress);
        }
        return Service.START_STICKY;
    }

    public void saveAndSendEvent(Event event, int probability) {
        this.event = event;
        switch (event) {
            case MESSAGE_RECEIVED:
                this.probability = probability;
                this.lastUpdateTime = System.currentTimeMillis();
                this.sendBroadcast(event, probability);
                break;

            case DISCONNECTED:
                this.valutaChiusuraMacchina();
                break;

            default:
                this.probability = probability;
                this.sendBroadcast(event, probability);
                break;
        }
    }

    public void startApplicationService(final String address) {
        // Se sono già connesso al dispositivo non interrompo la connessione
        if (this.connectionHandlerThread != null && this.connectionHandlerThread.isConnectedWith(address)) {
            Log.d("AndroidCar", "Già connesso al dispositivo");
        } else {
            this.stopComputing();
            running = true;
            final BluetoothDevice device = Helper.getDeviceByAddress(address);
            this.connectionHandlerThread = new ConnectionHandlerThread(device, this);
            this.connectionHandlerThread.start();
        }
    }

    public void stopComputing() {
        running = false;
        if (this.connectionHandlerThread != null) {
            Log.d("AndroidCar", "Termino connectionHandler");
            this.connectionHandlerThread.stopComputing();
        }
    }

    private void valutaChiusuraMacchina() {
        Log.d("AndroidCar", "Valuto chiusura macchina");

        final boolean closed = this.probability >= prefs.getInt("myProbability", DEFAULT_PROB);
        if (closed) {
            Log.d("AndroidCar", "Hai chiuso la macchina al " + this.probability + "%");
            this.saveAndSendEvent(CAR_CLOSED, this.probability);
        } else {
            Log.d("AndroidCar", "Non hai chiuso la macchina!");
            this.saveAndSendEvent(CAR_NOT_CLOSED, this.probability);
            this.sendNotification();
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            addParking(0, 0, closed);
        }
        LocationServices.getFusedLocationProviderClient(this).getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            addParking(location.getLatitude(), location.getLongitude(), closed);
                        } else {
                            addParking(0, 0, closed);
                        }
                    }
                });
    }

    private void addParking(double latitude, double longitude, boolean closed) {
        final String username = prefs.getString("username", "");
        try {
            URL url = new URL(getString(R.string.add_parking));

            final Bundle data = new Bundle();
            data.putString("user", username);
            data.putString("lat", ""+latitude);
            data.putString("lon", ""+longitude);
            data.putString("closed", closed ? "1" : "0");

            new UrlConnectionAsyncTask(url, null, getApplicationContext()).execute(data);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /* Per mandare un Intent implicito attraverso il LocalBroadcastManager in modo più semplice. */
    private void sendBroadcast(final Event event, final int probability) {
        final Intent intent = new Intent(event.name());
        intent.putExtra("probability", probability);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void sendNotification() {
        final Intent i = new Intent(this, BluetoothActivity.class);
        final PendingIntent pi = PendingIntent.getActivity(this, 1, i, 0);
        Notification.Builder builder;
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String CHANNEL_ID = "smartcar_channel_id";// The id of the channel.
            CharSequence name = "smartcar";// The user-visible name of the channel.
            int notifyID = (int) System.currentTimeMillis();
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            builder = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Smart Car")
                    .setContentText("Non hai chiuso la macchina!")
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .setChannelId(CHANNEL_ID);
            notificationManager.createNotificationChannel(mChannel);
            notificationManager.notify(notifyID, builder.build());
        } else {
            builder = new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Smart Car")
                    .setContentText("Non hai chiuso la macchina!")
                    .setAutoCancel(true)
                    .setContentIntent(pi);
            notificationManager.notify(1, builder.build());
        }
    }

    public Event getEvent() {
        return event;
    }

    @Override
    public boolean stopService(Intent intent) {
        stopComputing();
        return super.stopService(intent);
    }

    public boolean isRunning() {
        return running;
    }
}
