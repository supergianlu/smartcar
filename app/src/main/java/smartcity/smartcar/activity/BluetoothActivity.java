package smartcity.smartcar.activity;


import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.lzyzsd.circleprogress.ArcProgress;

import smartcity.smartcar.R;
import smartcity.smartcar.model.ApplicationService;
import smartcity.smartcar.model.Event;
import smartcity.smartcar.utility.Helper;

import static smartcity.smartcar.utility.Helper.NO_PROB;


public class BluetoothActivity extends MainActivity {

    private ProgressBar progressBar;
    private CheckBox checkBox;
    private ArcProgress arcProgress;
    private Button connectButton;
    private Button disconnectButton;
    private SharedPreferences prefs;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String eventName = intent.getAction();

            for(Event event : Event.values()) {
                if(event.name().equals(eventName)) {
                    modifyGUI(Event.valueOf(intent.getAction()), intent.getIntExtra("probability", NO_PROB));
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        setUpMainActivity();
        prefs = getSharedPreferences("MY_PREFS_NAME", MODE_PRIVATE);

        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        for(Event e : Event.values())
            localBroadcastManager.registerReceiver(this.broadcastReceiver, new IntentFilter(e.name()));

        progressBar = findViewById(R.id.progressBar);
        connectButton = findViewById(R.id.connectButton);
        disconnectButton = findViewById(R.id.disconnectButton);
        checkBox = findViewById(R.id.checkBox);
        arcProgress = findViewById(R.id.arc_progress);

        String deviceName = prefs.getString("myDeviceName", null);
        if(deviceName != null){
            checkBox.setText(deviceName);
        } else {
            checkBox.setText("Nessun device");
            arcProgress.setBottomText("Selezionare un device dalle impostazioni");
        }

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    String deviceAddress = prefs.getString("myDeviceAddress", null);
                    if(deviceAddress != null){
                        startService(new Intent(getApplicationContext(), ApplicationService.class));
                        connectButton.setEnabled(false);
                        //modifyGUI(Event.TRYING_TO_CONNECT, NO_PROB);
                    } else {
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.drawer_layout), "Selezionare un device dalle impostazioni", Snackbar.LENGTH_LONG);
                        snackbar.setAction("VAI", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(BluetoothActivity.this, SettingActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                        snackbar.show();
                    }
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
                stopService(new Intent(getApplicationContext(), ApplicationService.class));
                modifyGUI(Event.DISCONNECT, NO_PROB);
            }
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        if(requestCode == 1) {
            if(resultCode == RESULT_OK) {
                recreate();
            }
        }
    }

    private void modifyGUI(final Event event, final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (event) {
                    case BLUETOOTH_DISABLED:
                        arcProgress.setBottomText("Bluetooth disabilitato");
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

                    case CAR_NOT_CLOSED:
                        arcProgress.setBottomText("MACCHINA NON CHIUSA!!");
                        arcProgress.setTextColor(Color.RED);
                        break;

                    case TRYING_TO_CONNECT:
                        arcProgress.setBottomText("Provo a connettermi...");
                        progressBar.setVisibility(View.VISIBLE);
                        checkBox.setChecked(false);
                        connectButton.setEnabled(false);
                        disconnectButton.setEnabled(true);
                        break;

                    case CONNECTION_ESTABLISHED:
                        arcProgress.setBottomText("Connesso");
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
                            arcProgress.setBottomText("Macchina chiusa ;)");
                            arcProgress.setTextColor(Color.GREEN);
                        } catch (Exception e) {e.printStackTrace();}
                        break;

                    default: Log.d("AndroidCar", event.toString()); break;
                }
            }
        });
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
        ApplicationService.MyBinder b = (ApplicationService.MyBinder) binder;
        service = b.getService();
        if(service.isRunning() && service.getEvent() != null){
            modifyGUI(service.getEvent(), NO_PROB);
        } else {
            modifyGUI(Event.DISCONNECT, NO_PROB);
        }
    }

    private void modifyNoConnectionGUI(){
        progressBar.setVisibility(View.INVISIBLE);
        checkBox.setChecked(false);
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
    }

}
