package smartcity.smartcar;


import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.DonutProgress;

import smartcity.smartcar.model.ApplicationService;
import smartcity.smartcar.model.Event;
import smartcity.smartcar.utility.Helper;


public class BluetoothActivity extends MainActivity {

    private static final int ENABLE_BLUETOOTH_ACTION = 1;
    private final MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    private ProgressBar connecting; // icona di caricamento che viene mostrata durante la connessione ad un dipositivo
    private TextView eventLogger; // Campo testuale per mostrare alcune frasi all'utente
    private CheckBox checkBox; // checkBox che indica che si è connessi ad un dispositivo
    private DonutProgress progressBar;
    private int progressBarTextColor; // colore di default di progressBar
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        setUpMainActivity();
        this.setupBroadcastReceiver();
        this.prefs = getSharedPreferences("MY_PREFS_NAME", MODE_PRIVATE);
        this.connecting = findViewById(R.id.progressBar);
        this.eventLogger = findViewById(R.id.eventLogger);
        this.checkBox = findViewById(R.id.checkBox);
        this.checkBox.setEnabled(false);
        this.checkBox.setChecked(true);
        this.progressBar = findViewById(R.id.donut_progress);
        this.progressBarTextColor = this.progressBar.getTextColor();
        this.onRestoreInstanceState(Bundle.EMPTY);
        this.startApplication();
    }

    private void startApplication() {
        /*
            Casi:

            - Bluetooth disattivo: chiedo all'utente di attivarlo attraverso il metodo startActivityForResult
            - Lista dei device paired vuota: mostro l'informazione all'utente
            - Device di default non accoppiato: indico all'utente di scegliere un device con cui connettersi dalle impostazioni
            - Device di default trovato: provo a connettermi
         */
        if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            final Intent intent = new Intent(this, ApplicationService.class);

            if(BluetoothAdapter.getDefaultAdapter().getBondedDevices().isEmpty()) {
                this.showEvent(Event.NO_DEVICES_PAIRED, "");
            } else {
                final String defaultAddress = Helper.getDefaultDeviceAddress(prefs.getString("myDeviceAddress", ""));

                if(defaultAddress.isEmpty()) {
                    // Device di default non accoppiato
                    this.showEvent(Event.DEVICE_NOT_FOUND, "");
                } else {
                    intent.putExtra("address", defaultAddress); // Inserisco nell'Intent l'indirizzo del dispositivo di default
                }
            }
            startService(intent); // Faccio partire il service. Se era già partito non succede niente
        } else {
            // Chiedo all'utente di attivare il bluetooth
            this.showEvent(Event.BLUETOOTH_DISABLED, "");
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), ENABLE_BLUETOOTH_ACTION);
        }
    }

    private void showEvent(final Event event, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (event) {
                    case BLUETOOTH_DISABLED:
                        eventLogger.setText("Bluetooth disabilitato");
                        Log.d("AndroidCar", "Bluetooth disabilitato");
                        break;

                    case NO_DEVICES_PAIRED:
                        Log.d("AndroidCar", "Nessun device accoppiato al telefono");
                        eventLogger.setText("Nessun device accoppiato");
                        progressBar.setTextColor(progressBarTextColor);
                        break;

                    case APPPLICATION_STOPPED:
                        if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                            eventLogger.setText("Disconnesso");
                        } else {
                            eventLogger.setText("Bluetooth disattivato");
                        }
                        break;

                    case DEVICE_NOT_FOUND:
                        eventLogger.setText("Scegli dispositivo a cui connettersi");
                        Log.d("AndroidCar", "Device di default non trovato");
                        break;


                    case CAR_NOT_CLOSED:
                        eventLogger.setText("Non hai chiuso la macchina!");
                        break;

                    case TRYING_TO_CONNECT:
                        eventLogger.setText(message);
                        break;

                    case CONNECTION_ESTABLISHED:
                        eventLogger.setText(message);
                        updateProgressBar(0);
                        break;

                    case MESSAGE_RECEIVED:
                        try {
                            final int progress = Integer.parseInt(message);
                            updateProgressBar(progress);
                        } catch (Exception e) { e.printStackTrace();}
                        break;

                    case CAR_CLOSED:
                        try {
                            final int probability = Integer.parseInt(message);
                            updateProgressBar(probability);
                            eventLogger.setText("Macchina chiusa");
                        } catch (Exception e) {e.printStackTrace();}
                        break;

                    default: Log.d("AndroidCar", event.toString() + " " + message); break;
                }
                modifyGUI(event);
                onSaveInstanceState(Bundle.EMPTY);
            }
        });
    }

    private void updateProgressBar(final int progress) {
        progressBar.setProgress(progress);

        if (progress > prefs.getInt("myProbability", 40)) {
            progressBar.setFinishedStrokeColor(Helper.CAR_CLOSED_COLOR);
            progressBar.setTextColor(Helper.CAR_CLOSED_COLOR);
        } else {
            progressBar.setFinishedStrokeColor(Helper.CAR_UNCLOSED_COLOR);
            progressBar.setTextColor(Helper.CAR_UNCLOSED_COLOR);
        }
    }

    private void modifyGUI(final Event e) {
        /* Mostro/nascondo la checkBox e la connectBar in base all'evento giunto
           Quando ricevo un messaggio/mi connetto ad un dispositivo mostro la checkBar
           Quando provo a connettermi mostro la progressBar
           In tutti gli altri casi li nascondo entrambi
         */
        if(e.equals(Event.TRYING_TO_CONNECT)) {
            connecting.setVisibility(View.VISIBLE);
            checkBox.setVisibility(View.INVISIBLE);
        } else if(e.equals(Event.MESSAGE_RECEIVED) || e.equals(Event.CONNECTION_ESTABLISHED)) {
            connecting.setVisibility(View.INVISIBLE);
            checkBox.setVisibility(View.VISIBLE);
        } else {
            connecting.setVisibility(View.INVISIBLE);
            checkBox.setVisibility(View.INVISIBLE);
        }
    }



    private void setupBroadcastReceiver() {
        // Uso un LocalBroadcastReceiver per poter gestire gli intent all'interno dell'applicazione
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        for(Event e : Event.values()) {
            localBroadcastManager.registerReceiver(this.myBroadcastReceiver, new IntentFilter(e.name()));
        }
    }

    private final class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            for(Event e : Event.values()) {
                if(e.name().equals(action)) {
                    showEvent(Event.valueOf(intent.getAction()), intent.getStringExtra("message"));
                }
            }
        }
    }

}
