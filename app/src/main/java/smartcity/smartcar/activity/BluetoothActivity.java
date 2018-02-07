package smartcity.smartcar.activity;


import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;

import com.github.lzyzsd.circleprogress.ArcProgress;

import java.util.ArrayList;
import java.util.List;

import smartcity.smartcar.R;
import smartcity.smartcar.model.ApplicationService;
import smartcity.smartcar.model.Event;
import smartcity.smartcar.utility.Helper;

import static smartcity.smartcar.model.MyIntentFilter.CLOSE_CONNECTION;
import static smartcity.smartcar.model.MyIntentFilter.SET_DEVICE;
import static smartcity.smartcar.utility.Helper.DEFAULT_PROB;


public class BluetoothActivity extends MainActivity {

    private final MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    private ProgressBar progressBar;
    private CheckBox checkBox;
    private ArcProgress arcProgress;
    private Button connectButton;
    private Button disconnectButton;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        setUpMainActivity();
        this.prefs = getSharedPreferences("MY_PREFS_NAME", MODE_PRIVATE);
        this.setupBroadcastReceiver();
        this.setupGUI();

        if(prefs.getBoolean("restore", false)){
            checkBox.setText(prefs.getString("checkboxText", ""));
            arcProgress.setProgress(prefs.getInt("arcNumber", 0));
            arcProgress.setBottomText(prefs.getString("arcText", ""));
            checkBox.setChecked(prefs.getBoolean("checkboxChecked", false));
            connectButton.setEnabled(prefs.getBoolean("connectEnabled", false));
            disconnectButton.setEnabled(prefs.getBoolean("disconnectEnabled", false));
            progressBar.setVisibility(prefs.getInt("progressbarVisibility", View.VISIBLE) == View.VISIBLE? View.VISIBLE : View.INVISIBLE);
        }// else {
            this.startApplication();
        //}
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
                final BluetoothDevice device = Helper.getDeviceByAddress(prefs.getString("myDeviceAddress", ""));
                if(device == null) {
                    // Device di default non accoppiato
                    this.showEvent(Event.DEVICE_NOT_FOUND, "");
                } else {
                    intent.putExtra("address", device.getAddress()); // Inserisco nell'Intent l'indirizzo del dispositivo di default
                    checkBox.setText(device.getName());
                }
            }
            startService(intent); // Faccio partire il service. Se era già partito non succede niente
        } else {
            // Chiedo all'utente di attivare il bluetooth
            this.showEvent(Event.BLUETOOTH_DISABLED, "");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }

    private void showEvent(final Event event, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (event) {
                    case BLUETOOTH_DISABLED:
                        arcProgress.setBottomText("Bluetooth disabilitato");
                        modifyNoConnectionGUI();
                        break;

                    case NO_DEVICES_PAIRED:
                        arcProgress.setBottomText("Nessun device accoppiato");
                        modifyNoConnectionGUI();
                        break;

                    case DISCONNECT:
                        if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                            arcProgress.setBottomText("Disconnesso");
                        } else {
                            arcProgress.setBottomText("Bluetooth disattivato");
                        }
                        modifyNoConnectionGUI();
                        break;

                    case DEVICE_NOT_FOUND:
                        arcProgress.setBottomText("Connettersi a un device");
                        modifyNoConnectionGUI();
                        break;

                    case CAR_NOT_CLOSED:
                        arcProgress.setBottomText("Non hai chiuso la macchina!");
                        break;

                    case TRYING_TO_CONNECT:
                        arcProgress.setBottomText("Provo a connettermi...");
                        progressBar.setVisibility(View.VISIBLE);
                        checkBox.setChecked(false);
                        checkBox.setText(message);
                        connectButton.setEnabled(false);
                        disconnectButton.setEnabled(true);
                        break;

                    case CONNECTION_ESTABLISHED:
                        arcProgress.setBottomText("Connessione stabilita");
                        progressBar.setVisibility(View.INVISIBLE);
                        checkBox.setChecked(true);
                        checkBox.setText(message);
                        disconnectButton.setEnabled(true);
                        //updateArcProgress(0);
                        break;

                    case MESSAGE_RECEIVED:
                        progressBar.setVisibility(View.INVISIBLE);
                        checkBox.setChecked(true);
                        try {
                            final int progress = Integer.parseInt(message);
                            updateArcProgress(progress);
                        } catch (Exception e) { e.printStackTrace();}
                        break;

                    case CAR_CLOSED:
                        try {
                            final int probability = Integer.parseInt(message);
                            updateArcProgress(probability);
                            arcProgress.setBottomText("Macchina chiusa");
                        } catch (Exception e) {e.printStackTrace();}
                        break;

                    default: Log.d("AndroidCar", event.toString() + " " + message); break;
                }
                saveValues(true);
            }
        });
    }

    private void modifyNoConnectionGUI(){
        progressBar.setVisibility(View.INVISIBLE);
        checkBox.setText("Nessun device");
        checkBox.setChecked(false);
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
    }

    private void setupGUI() {
        this.progressBar = findViewById(R.id.progressBar);
        this.connectButton = findViewById(R.id.connectButton);
        connectButton.setEnabled(false);
        this.disconnectButton = findViewById(R.id.disconnectButton);
        disconnectButton.setEnabled(false);
        this.checkBox = findViewById(R.id.checkBox);
        this.arcProgress = findViewById(R.id.arc_progress);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    showDeviceList();
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 1);
                }
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(CLOSE_CONNECTION.name()));
                showEvent(Event.DISCONNECT, "");
            }
        });
    }

    public void saveValues(final boolean restore) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("checkboxText", (String) checkBox.getText());
        editor.putInt("arcNumber", arcProgress.getProgress());
        editor.putString("arcText", arcProgress.getBottomText());
        editor.putBoolean("checkboxChecked", checkBox.isChecked());
        editor.putBoolean("connectEnabled", connectButton.isEnabled());
        editor.putBoolean("disconnectEnabled", disconnectButton.isEnabled());
        editor.putInt("progressbarVisibility", progressBar.getVisibility());
        editor.putBoolean("restore", restore);
        editor.apply();
    }

    /*@Override
    public void onRestoreInstanceState(final Bundle savedInstanceState) {
        boolean notifica = false;
        try {
            final FileInputStream inputStream = openFileInput(Helper.NOTIFICATION_FILENAME);
            notifica = inputStream.read() == 1;
            inputStream.close();
            if(notifica) {
                this.checkBox.setChecked(false);
                this.progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(this.getApplicationContext(), "Non hai chiuso la macchina!", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {e.printStackTrace();}
    }*/

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        if(requestCode == 1) {
            if(resultCode == RESULT_OK) {
                this.recreate();
            }
        }
    }

    private void showDeviceList() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Scegli device");

        // Bundle per ricordare posizione del device scelto
        final Bundle bundle = new Bundle();
        bundle.putInt("selected", 0);

        // Creo array di CharSequence da inserire nel Dialog
        final List<BluetoothDevice> devices = new ArrayList<>(BluetoothAdapter.getDefaultAdapter().getBondedDevices());
        final CharSequence[] array = new CharSequence[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            array[i] = devices.get(i).getName();
        }

        // Inserisco nel dialog la lista dei device
        builder.setSingleChoiceItems(array, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Quando seleziono un device dalla lista memorizzo la sua posizione nel bundle
                bundle.putInt("selected", which);
            }
        });

        builder.setPositiveButton("Connetti", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /* Grazie alla variabile salvata nel bundle posso recuperare il device selezionato dalla lista
                   e comunicarlo al Controller. Stopppo l'applicazione perchè deve sapere che l'ho terminata forzatamente
                 */
                final int position = bundle.getInt("selected");
                final BluetoothDevice device = devices.get(position);
                final Intent intent = new Intent(SET_DEVICE.name());
                intent.putExtra("address", device.getAddress());
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                connectButton.setEnabled(false);
            }
        });

        builder.setNegativeButton("Cancella", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { }
        });

        builder.create().show();
    }

    private void updateArcProgress(final int progress) {
        arcProgress.setProgress(progress);

        if (progress > prefs.getInt("myProbability", DEFAULT_PROB)) {
            arcProgress.setFinishedStrokeColor(Helper.CAR_CLOSED_COLOR);
            arcProgress.setTextColor(Helper.CAR_CLOSED_COLOR);
        } else {
            arcProgress.setFinishedStrokeColor(Helper.CAR_UNCLOSED_COLOR);
            arcProgress.setTextColor(Helper.CAR_UNCLOSED_COLOR);
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
