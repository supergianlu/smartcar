package smartcity.smartcar.model;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;

import smartcity.smartcar.BluetoothActivity;
import smartcity.smartcar.R;
import smartcity.smartcar.utility.Helper;

import static smartcity.smartcar.model.Event.*;
import static smartcity.smartcar.model.MyIntentFilter.*;
import static smartcity.smartcar.utility.Helper.DEFAULT_PROB;


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
public class ApplicationService extends IntentService {

    private ConnectionHandlerThread connectionHandlerThread;
    private final MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    private int actualProbability = -1;
    private long lastUpdateTime;
    private boolean stop;

    public ApplicationService() {
        super("ApplicationService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        Log.d("AndroidCar", "service partito");
        this.setupBroadcastReceiver();

        final String address = intent.getStringExtra("address");

        if(address != null && !address.isEmpty()){
            this.startApplicationService(address);
        }

        while (!stop) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(this.myBroadcastReceiver);
        this.stopComputing();
        stopSelf();
    }

    public void notifyEvent(Event event, String message) {
        switch (event) {
            case MESSAGE_RECEIVED:
                try {
                    this.actualProbability = Integer.parseInt(message);
                }catch (Exception e) {e.printStackTrace();}

                this.lastUpdateTime = System.currentTimeMillis();
                this.sendBroadcast(event, message);
                break;

            case DISCONNECTED: this.valutaChiusuraMacchina(); break;
            default: this.sendBroadcast(event, message); break;
        }
    }

    private void startApplicationService(final String address) {
        // Se sono già connesso al dispositivo non interrompo la connessione
        if(this.connectionHandlerThread != null && this.connectionHandlerThread.isConnectedWith(address)) {
            Log.d("AndroidCar", "Già connesso al dispositivo");
        } else {
            this.stopComputing();
            final BluetoothDevice device = Helper.getDeviceByAddress(address);
            this.connectionHandlerThread = new ConnectionHandlerThread(device, this);
            this.connectionHandlerThread.start();
        }
    }

    /* Per mandare un Intent implicito attraverso il LocalBroadcastManager in modo più semplice. */
    private void sendBroadcast(final Event event, final String s) {
        final Intent intent = new Intent(event.name());
        final Bundle bundle = new Bundle();
        bundle.putString("message", s);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void stopComputing() {
        if(this.connectionHandlerThread != null) {
            Log.d("AndroidCar", "Termino connectionHandler");
            this.connectionHandlerThread.stopComputing();
        }
    }

    private void valutaChiusuraMacchina() {
        Log.d("AndroidCar", "Valuto chiusura macchina");

        // Probabilità attuale minore di quella minima ---> lancio allarme
        SharedPreferences prefs = getSharedPreferences("MY_PREFS_NAME", MODE_PRIVATE);
        if(this.actualProbability <= prefs.getInt("myProbability", DEFAULT_PROB)) {
            Log.d("AndroidCar", "Non hai chiuso la macchina!");
            this.notifyEvent(CAR_NOT_CLOSED, Integer.toString(this.actualProbability));
            this.sendNotification();

        } else {
            final long time = System.currentTimeMillis() - this.lastUpdateTime;

            if(time < 15000) {
                this.actualProbability += 35;
            } else if(time > 15000 && time < 30000){
                this.actualProbability += 25;
            } else if(time > 30000 && time < 45000){
                this.actualProbability += 15;
            } else {
                this.actualProbability += 5;
            }

            Log.d("AndroidCar", "Hai chiuso la macchina al " + this.actualProbability + "%");
            this.notifyEvent(CAR_CLOSED, this.actualProbability + "");
        }
    }

    private void setupBroadcastReceiver() {
        // Uso un LocalBroadcastReceiver per non mandare i miei intent fuori dall'applicazione
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.registerReceiver(this.myBroadcastReceiver, new IntentFilter(SET_DEVICE.name()));
        localBroadcastManager.registerReceiver(this.myBroadcastReceiver, new IntentFilter(CLOSE_CONNECTION.name()));
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

        // Scrivo su file il fatto che ho mandato la notifica
        try {
            final FileOutputStream outputStream = openFileOutput(Helper.NOTIFICATION_FILENAME, MODE_PRIVATE);
            outputStream.write(1);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopService() {
        this.stop = true;
    }

    private final class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(SET_DEVICE.name())) {
                final String address = intent.getStringExtra("address");
                startApplicationService(address);

            } else if(action.equals(CLOSE_CONNECTION.name())) {
                stopComputing();
            } else if(action.equals(STOP_SERVICE.name())) {
                stopService();
            }
        }
    }
}
