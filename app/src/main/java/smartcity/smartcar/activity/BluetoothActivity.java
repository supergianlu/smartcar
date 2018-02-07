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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.lzyzsd.circleprogress.ArcProgress;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

    private static final int ENABLE_BLUETOOTH_ACTION = 1;
    private static final String savedInstanceFilename = "savedInstance.bin";

    private final MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    private ProgressBar progressBar; // icona di caricamento che viene mostrata durante la connessione ad un dipositivo
    private CheckBox checkBox; // checkBox che indica che si è connessi ad un dispositivo
    private ArcProgress arcProgress;
    private int progressBarTextColor; // colore di default di arcProgress
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
                    checkBox.setText(defaultAddress);
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
                        arcProgress.setBottomText("Bluetooth disabilitato");
                        checkBox.setText("Nessun device");
                        checkBox.setChecked(false);
                        Log.d("AndroidCar", "Bluetooth disabilitato");
                        break;

                    case NO_DEVICES_PAIRED:
                        Log.d("AndroidCar", "Nessun device accoppiato al telefono");
                        arcProgress.setBottomText("Nessun device accoppiato");
                        checkBox.setText("Nessun device");
                        checkBox.setChecked(false);
                        break;

                    case APPLICATION_STOPPED:
                        if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                            arcProgress.setBottomText("Disconnesso");
                        } else {
                            arcProgress.setBottomText("Bluetooth disattivato");
                        }
                        break;

                    case DEVICE_NOT_FOUND:
                        arcProgress.setBottomText("Scegli dispositivo a cui connettersi");
                        connectButton.setEnabled(true);
                        Log.d("AndroidCar", "Device di default non trovato");
                        break;


                    case CAR_NOT_CLOSED:
                        arcProgress.setBottomText("Non hai chiuso la macchina!");
                        break;

                    case TRYING_TO_CONNECT:
                        checkBox.setText(message);
                        connectButton.setEnabled(false);
                        disconnectButton.setEnabled(true);
                        break;

                    case CONNECTION_ESTABLISHED:
                        checkBox.setText(message);
                        connectButton.setEnabled(false);
                        disconnectButton.setEnabled(true);
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
                            arcProgress.setBottomText("Macchina chiusa");
                        } catch (Exception e) {e.printStackTrace();}
                        break;

                    default: Log.d("AndroidCar", event.toString() + " " + message); break;
                }
                modifyGUI(event);
                onSaveInstanceState(Bundle.EMPTY);
            }
        });
    }

    private void modifyGUI(final Event e) {
        /* Mostro/nascondo la checkBox e la connectBar in base all'evento giunto
           Quando ricevo un messaggio/mi connetto ad un dispositivo mostro la checkBar
           Quando provo a connettermi mostro la arcProgress
           In tutti gli altri casi li nascondo entrambi
         */
        if(e.equals(Event.TRYING_TO_CONNECT)) {
            progressBar.setVisibility(View.VISIBLE);
            checkBox.setChecked(false);
        } else if(e.equals(Event.MESSAGE_RECEIVED) || e.equals(Event.CONNECTION_ESTABLISHED)) {
            progressBar.setVisibility(View.INVISIBLE);
            checkBox.setChecked(true);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            checkBox.setChecked(false);
        }
    }

    private void setupGUI() {
        this.progressBar = findViewById(R.id.progressBar);
        this.connectButton = findViewById(R.id.connectButton);
        connectButton.setEnabled(false);
        this.disconnectButton = findViewById(R.id.disconnectButton);
        disconnectButton.setEnabled(false);
        this.checkBox = findViewById(R.id.checkBox);
        this.arcProgress = findViewById(R.id.arc_progress);
        this.arcProgress.setProgress(0);
        this.progressBarTextColor = this.arcProgress.getTextColor();
        this.onRestoreInstanceState(Bundle.EMPTY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        switch (id) {
            case R.id.connectOption:
                if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    showDeviceList();
                } else {
                    // Devo chiedere all'utente di attivare la connessione bluetooth
                    try {
                        // Mi salvo il fatto che ho premuto connect to
                        final FileOutputStream out = openFileOutput("temp.bin", MODE_PRIVATE);
                        out.write(1);
                        out.close();
                    } catch (IOException e) {e.printStackTrace();}
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);
                }
                return true;

            case R.id.disconnectOption:
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(CLOSE_CONNECTION.name()));
                this.showEvent(Event.APPLICATION_STOPPED, "");
                return true;

            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle savedInstanceState) {
        try {
            final ObjectOutputStream objectOutputStream = new ObjectOutputStream(openFileOutput(savedInstanceFilename, MODE_PRIVATE));
            objectOutputStream.writeUTF(this.checkBox.getText().toString());
            objectOutputStream.writeInt(this.arcProgress.getProgress());
            objectOutputStream.writeBoolean(this.checkBox.isChecked());
            objectOutputStream.writeInt(this.progressBar.getVisibility());
            objectOutputStream.close();
        } catch (IOException e) {e.printStackTrace();}
    }

    @Override
    public void onRestoreInstanceState(final Bundle savedInstanceState) {
        boolean notifica = false;
        try {
            final FileInputStream inputStream = openFileInput(Helper.NOTIFICATION_FILENAME);
            notifica = inputStream.read() == 1;
            inputStream.close();
        } catch (IOException e) {e.printStackTrace();}


        try {
            final ObjectInputStream inputStream = new ObjectInputStream(openFileInput(savedInstanceFilename));
            this.checkBox.setText(inputStream.readUTF());
            this.updateProgressBar(inputStream.readInt());
            this.checkBox.setChecked(inputStream.readBoolean());
            this.progressBar.setVisibility(inputStream.readInt() == View.INVISIBLE? View.INVISIBLE : View.VISIBLE);
            inputStream.close();

            if(notifica) {
                this.checkBox.setChecked(false);
                this.progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(this.getApplicationContext(), "Non hai chiuso la macchina!", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {e.printStackTrace();}

        this.getApplicationContext().deleteFile(Helper.NOTIFICATION_FILENAME);
        this.getApplicationContext().deleteFile(savedInstanceFilename);
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        if(requestCode == ENABLE_BLUETOOTH_ACTION) {
            if(resultCode == RESULT_OK) {
                checkBox.setText("");

                /* Controllo se ho cliccato "connect to" dalle opzioni.
                   In tal caso dopo aver attivato il bluetooth devo aprire la lista dei device da scegliere per la connessione.
                 */
                boolean isConnectOptionClicked = false;
                try {
                    final FileInputStream inputStream = openFileInput("temp.bin");
                    isConnectOptionClicked = inputStream.read() == 1;
                    inputStream.close();
                } catch (IOException e) {e.printStackTrace();}

                // Se avevo cliccato 'connect to' apro la lista dei device
                if(isConnectOptionClicked) {
                    startService(new Intent(this, ApplicationService.class));
                    showDeviceList();
                } else {
                    this.startApplication();
                }
            }
        }
        this.getApplicationContext().deleteFile("temp.bin");
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
            }
        });

        builder.setNegativeButton("Cancella", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { }
        });

        builder.create().show();
    }

    private void updateProgressBar(final int progress) {
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
