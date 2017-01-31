package com.example.danie.btapptest;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.bluetooth.BluetoothAdapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // layout related
    private Button searchBtn;
    private Button testBtn;
    private TextView txtDevices;
    private ListView devicesList;
    private Switch accSwitch;
    private Switch geoSwitch;

    private ArrayAdapter<String> devicesAdapter;
    private ArrayList<BluetoothDevice> availableDevices;
    private ArrayList<BluetoothDevice> foundedDevices;
    private Set<BluetoothDevice> bondedDevices;
    private BluetoothDevice connectedToDevice;

    // bt related
    private BluetoothAdapter btAdapter;
    private BroadcastReceiver btReceiver;

    // connection
    private ConnectedThread connectedThread;

    private final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;

    // NEW
    public static final UUID uuid = UUID.fromString("72764c49-a85c-4780-8767-b20da73e1bd4");

    // Sensors
    private SensorManager mSensorManager;
    private Sensor accSensor;
    private Sensor othSensor;

    // Utils
    private ShowToast showToast;
    private Utils utils;

    // GuiState
    private boolean geoChecked = true;
    private boolean accChecked = false;

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case Utils.IS_CONNECTED:
                    connectedThread = new ConnectedThread((BluetoothSocket)msg.obj, mHandler);
                    showToast.quick(MSG.BT_SUCC_CONNECTED);
                    connectedThread.write("connected");
                    if (connectedToDevice != null) {
                        txtDevices.setText("connected to device: " + connectedToDevice.getName());
                    }
                    break;
                case Utils.RECEIVING:
                    byte[] readBuf = (byte[])msg.obj;
                    String string = new String(readBuf);
                    showToast.quick(string);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        initGUI();
        showToast = new ShowToast(getApplicationContext());
        utils = new Utils(this);

        foundedDevices = new ArrayList<>();
        devicesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, 0);
        availableDevices = new ArrayList<>();

        devicesList.setAdapter(devicesAdapter);

        // init bt
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        othSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);

        if (btAdapter == null) {
            showToast.quick(MSG.BT_NOT_SUPPORTED);
        } else {
            if (!btAdapter.isEnabled()) {
                Intent btEnableReq = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(btEnableReq, 1);
                btAdapter.startDiscovery();
            }
            btReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                        BluetoothDevice foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (bondedDevices.contains(foundDevice)) {
                            foundedDevices.add(foundDevice);
                            devicesAdapter.add(foundDevice.getName() + '\n' + foundDevice.getAddress());
                        }
                        Log.i("bt", "device found: " + foundDevice.getName());
                    } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                        Log.i("bt", "search started");
                        foundedDevices.clear();
                        devicesAdapter.clear();
                        searchBtn.setEnabled(false);
                        searchBtn.setText("Searching...");
                        showToast.quick(MSG.BT_SEARCH_START);
                    } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                        Log.i("bt", "search ended");
                        searchBtn.setEnabled(true);
                        searchBtn.setText("search for devices");
                        showToast.quick(MSG.BT_SEARCH_END);
                    }
                }
            };
            utils.applyBroadcastFilters(btReceiver);
        }
    }

    private void initGUI() {
//        testBtn = (Button) findViewById(R.id.btnTest);
//        testBtn.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (connectedThread != null) {
//                    connectedThread.write("YEAH");
//                } else {
//                    Log.e("GUI", "No connected devices");
//                    showToast.quick("You aren't connect to any device");
//                }
//            }
//        });

        searchBtn = (Button) findViewById(R.id.btnSearch);
        searchBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btAdapter != null) {
                    if (!btAdapter.isDiscovering()) {
                        bondedDevices = btAdapter.getBondedDevices();
                        btAdapter.startDiscovery();
                        devicesAdapter.clear();
                    } else {
                        showToast.loong(MSG.BT_IS_SEARCHING);
                    }
                } else {
                    showToast.quick(MSG.BT_NOT_SUPPORTED);
                }
            }
        });
        devicesList = (ListView) findViewById(R.id.foundDevLst);
        devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i("GUI", "user clicked on item: ");
                BTCancelDiscovery();
                BluetoothDevice device = foundedDevices.get(i);
                if (device == null) {
                    Log.i("GUI", "device isn't initialized, null :-(");
                    finish();
                } else {
                    Log.i("GUI", device.getName());
                    showToast.quick("connecting to : " + device.getName());
                    ConnectThread connect = new ConnectThread(device, uuid, mHandler, showToast);
                    connect.start();
                    connectedToDevice = device;
                    devicesAdapter.clear();
                    searchBtn.setEnabled(false);
                }
            }
        });

        txtDevices = (TextView) findViewById(R.id.txtDevices);

        geoSwitch = (Switch) findViewById(R.id.swGyr);
        geoSwitch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (geoSwitch.isChecked()) {
                    geoChecked = true;
                    accChecked = false;
                    accSwitch.setChecked(false);
                } else {
                    geoChecked = false;
                    accChecked = true;
                    accSwitch.setChecked(true);
                }
                Log.i("GUI", "gyroscope switched to: " + geoChecked);
            }
        });

        accSwitch = (Switch) findViewById(R.id.swAcc);
        accSwitch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (accSwitch.isChecked()) {
                    accChecked = true;
                    geoChecked = false;
                    geoSwitch.setChecked(false);
                } else {
                    accChecked = false;
                    geoChecked = true;
                    geoSwitch.setChecked(true);
                }
                Log.i("GUI", "accellerator switched to: " + accChecked);
            }
        });
    }

    private void BTCancelDiscovery() {
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            showToast.loong(MSG.BT_NOT_SUPPORTED);
            finish();
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }
    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, othSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String dataToSend = ">";
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                if (accChecked && (connectedThread != null)) {
                    dataToSend = dataToSend + "A" + Float.toString(event.values[0]);
                    dataToSend = dataToSend + "," + Float.toString(event.values[1]);
                    dataToSend = dataToSend + "," + Float.toString(event.values[2]);
                    connectedThread.write(dataToSend);
                }
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                if (geoChecked && (connectedThread != null)) {
                    dataToSend = dataToSend + "G" + Float.toString(event.values[0]);
                    dataToSend = dataToSend + "," + Float.toString(event.values[1]);
                    dataToSend = dataToSend + "," + Float.toString(event.values[2]);
                    connectedThread.write(dataToSend);
                }
        }
    }
}
