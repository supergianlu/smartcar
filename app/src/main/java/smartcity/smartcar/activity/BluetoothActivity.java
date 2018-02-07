package smartcity.smartcar.activity;


import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
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


public class BluetoothActivity extends MainActivity implements ServiceConnection{

    private ApplicationService service;
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
                service.stopComputing();
                modifyGUI(Event.DISCONNECT, 0);
                //TODO stop service
            }
        });

        Intent intent= new Intent(this, ApplicationService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
        //on destroy unbind?? bo
    }

    /*private void startApplication() {

           // Casi:
            //- Bluetooth disattivo: chiedo all'utente di attivarlo attraverso il metodo startActivityForResult
            //- Lista dei device paired vuota: mostro l'informazione all'utente
            //- Device di default non accoppiato: indico all'utente di scegliere un device con cui connettersi dalle impostazioni
            //- Device di default trovato: provo a connettermi

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
    }*/

    private void modifyGUI(final Event event, final int progress) {
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
                        connectButton.setEnabled(false);
                        disconnectButton.setEnabled(true);
                        break;

                    case CONNECTION_ESTABLISHED:
                        arcProgress.setBottomText("Connessione stabilita");
                        progressBar.setVisibility(View.INVISIBLE);
                        checkBox.setChecked(true);
                        disconnectButton.setEnabled(true);
                        //updateArcProgress(0);
                        break;

                    case MESSAGE_RECEIVED:
                        progressBar.setVisibility(View.INVISIBLE);
                        checkBox.setChecked(true);
                        try {
                            arcProgress.setProgress(progress);
                        } catch (Exception e) { e.printStackTrace();}
                        break;

                    case CAR_CLOSED:
                        try {
                            arcProgress.setProgress(progress);
                            arcProgress.setBottomText("Macchina chiusa");
                        } catch (Exception e) {e.printStackTrace();}
                        break;

                    default: Log.d("AndroidCar", event.toString()); break;
                }
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
                service.startApplicationService(device.getAddress());
                connectButton.setEnabled(false);
            }
        });

        builder.setNegativeButton("Cancella", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { }
        });

        builder.create().show();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
        ApplicationService.MyBinder b = (ApplicationService.MyBinder) binder;
        service = b.getService();
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(true);
        //modifyGUI(service.getEvent(), service.getProgress());
        /*parto mettendo l'evento di provarmi a connettere e poi lo cambio direttamente dal service*/
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

}
