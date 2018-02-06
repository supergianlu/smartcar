package smartcity.smartcar;


import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import smartcity.smartcar.utility.Helper;

import static smartcity.smartcar.utility.Helper.DEFAULT_PROB;

public class SettingActivity extends MainActivity {

    private TextView text;
    private Button button;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private int deviceIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        setUpMainActivity();

        deviceIndex = 0;
        prefs = getSharedPreferences("MY_PREFS_NAME", MODE_PRIVATE);
        editor = prefs.edit();

        this.text = findViewById(R.id.valueProbabilità);
        final TextView selectionDeviceText = findViewById(R.id.devicesSelectionText);
        this.button = findViewById(R.id.button);
        this.setupSeekBar();

        if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            final List<BluetoothDevice> devices = new ArrayList<>(BluetoothAdapter.getDefaultAdapter().getBondedDevices());

            // Non ci sono device accoppiati con il telefono
            if(devices.isEmpty()) {
                selectionDeviceText.setText("Nessun device accoppiato");
                this.button.setVisibility(View.INVISIBLE);
            } else {
                final String defaultDeviceAddress = Helper.getDefaultDeviceAddress(prefs.getString("myDeviceAddress", ""));
                // Se la stringa è vuota significa che il device di default non è accoppiato con il telefono
                if(defaultDeviceAddress.isEmpty()) {
                    button.setText(devices.get(0).getName());
                } else {
                    // Cerco il nome associato all'indirizzo del device di default
                    for(BluetoothDevice device : devices) {
                        if(device.getAddress().equals(defaultDeviceAddress)) {
                            button.setText(device.getName());
                            deviceIndex = devices.indexOf(device);
                        }
                    }
                }

                this.button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        /* Quando clicco il bottone mostro la lista dei device che posso scegliere.
                           Il secondo parametro è l'opzione già 'cliccata', riprendo dal bundle la posizione salvata in precedenza
                         */
                        showDeviceList(devices);
                    }
                });
            }
        } else {
            selectionDeviceText.setVisibility(View.INVISIBLE);
            this.button.setVisibility(View.INVISIBLE);
        }

    }

    private void showDeviceList(final List<BluetoothDevice> devices) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Scegli device");

        // Creo array di CharSequence da inserire nel Dialog
        final String[] devicesName = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            devicesName[i] = devices.get(i).getName();
        }

        // Inserisco nel dialog la lista dei device
        builder.setSingleChoiceItems(devicesName, deviceIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                deviceIndex = position;
            }
        });

        // Bottone per salvare la scelta
        builder.setPositiveButton("Scegli", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                // Riprendo la posizione salvata nel bundle e cambio il device di default
                final BluetoothDevice device = devices.get(deviceIndex);
                button.setText(device.getName());
                editor.putString("myDeviceAddress", device.getAddress());
                editor.apply();
            }
        });

        // Bottone cancella
        builder.setNegativeButton("Cancella", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                dialog.cancel();
            }
        });

        builder.create().show();
    }

    private void setupSeekBar() {
        final SeekBar seekBarProbabilità = findViewById(R.id.seekBarProbabilità);
        final int defaultProbabilità = prefs.getInt("myProbability", DEFAULT_PROB);

        // Setto la GUI (seekBar e relativo testo che mostra il valore)
        seekBarProbabilità.setProgress(defaultProbabilità / (100 / seekBarProbabilità.getMax()));
        this.text.setText(defaultProbabilità + "/" + seekBarProbabilità.getMax() * (100 / seekBarProbabilità.getMax()));

        seekBarProbabilità.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int value;
            // La seekBar ha come massimo 20 ma io voglio mostrare 100, questo rapporto mi aiuta nei calcoli
            private final int rapporto = 100 / seekBarProbabilità.getMax();

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                this.value = progress;
                text.setText(this.value * rapporto + "/" + seekBar.getMax() * rapporto);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                    final int actualValue = this.value * this.rapporto;
                    editor.putInt("myProbability", actualValue);
                    editor.apply();
                    Log.d("AndroidCar", "Settata probabilità minima a " + actualValue);
            }
        });
    }

}
