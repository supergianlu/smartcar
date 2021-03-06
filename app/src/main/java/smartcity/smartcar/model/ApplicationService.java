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
 * Una volta avviato continua a lavorare in background finchè non viene disattivato.
 * Contiene un campo ConnectionHandlerThread che rappresenta il thread che gestisce
 * il collegamento con Arduino.
 * Permette di mostrare una notifica che avvisa l'utente sullo stato di
 * chiusura o apertura della macchina
 */
public class ApplicationService extends Service {


    private ConnectionHandlerThread connectionHandlerThread;
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
        String notificationString;
        if (closed) {
            Log.d("AndroidCar", "Hai chiuso la macchina al " + this.probability + "%");
            notificationString = "Hai chiuso la macchina al " + this.probability + "%";
            this.saveAndSendEvent(CAR_CLOSED, this.probability);
        } else {
            Log.d("AndroidCar", "Non hai chiuso la macchina!");
            notificationString = "Non hai chiuso la macchina!";
            this.saveAndSendEvent(CAR_NOT_CLOSED, this.probability);
        }
        this.sendNotification(notificationString);
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

    private void sendNotification(final String contentText) {
        Notification.Builder builder;
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String CHANNEL_ID = "smartcar_channel_id";// The id of the channel.
            CharSequence name = "smartcar";// The user-visible name of the channel.
            int notifyID = (int) System.currentTimeMillis();
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            builder = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle("Smart Car")
                    .setContentText(contentText)
                    .setAutoCancel(true)
                    .setChannelId(CHANNEL_ID);
            notificationManager.createNotificationChannel(mChannel);
            notificationManager.notify(notifyID, builder.build());
        } else {
            builder = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle("Smart Car")
                    .setContentText(contentText)
                    .setAutoCancel(true);
            notificationManager.notify(1, builder.build());
        }
    }

    public Event getEvent() {
        return event;
    }

    public int getProbability() {
        return probability;
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
