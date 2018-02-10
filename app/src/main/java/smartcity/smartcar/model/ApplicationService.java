package smartcity.smartcar.model;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import smartcity.smartcar.activity.BluetoothActivity;
import smartcity.smartcar.R;
import smartcity.smartcar.utility.Helper;

import static smartcity.smartcar.model.Event.*;
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
        if(deviceAddress == null) {
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
        if(this.connectionHandlerThread != null && this.connectionHandlerThread.isConnectedWith(address)) {
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
        if(this.connectionHandlerThread != null) {
            Log.d("AndroidCar", "Termino connectionHandler");
            this.connectionHandlerThread.stopComputing();
        }
    }

    private void valutaChiusuraMacchina() {
        Log.d("AndroidCar", "Valuto chiusura macchina");

        // Probabilità attuale minore di quella minima ---> lancio allarme
        if(this.probability <= prefs.getInt("myProbability", DEFAULT_PROB)) {
            Log.d("AndroidCar", "Non hai chiuso la macchina!");
            this.saveAndSendEvent(CAR_NOT_CLOSED, this.probability);
            this.sendNotification();

        } else {
            final long time = System.currentTimeMillis() - this.lastUpdateTime;

            if(time < 15000) {
                this.probability += 35;
            } else if(time > 15000 && time < 30000){
                this.probability += 25;
            } else if(time > 30000 && time < 45000){
                this.probability += 15;
            } else {
                this.probability += 5;
            }

            Log.d("AndroidCar", "Hai chiuso la macchina al " + this.probability + "%");
            this.saveAndSendEvent(CAR_CLOSED, this.probability);
        }
        //TODO in ognuno dei due casi salvo nel db il parcheggio
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
