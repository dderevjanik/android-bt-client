package com.example.danie.btapptest;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class ConnectThread extends Thread {

    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final String Tag = "Connect";
    private Handler mHandler;
    private ShowToast mShowToast;

    public ConnectThread(BluetoothDevice device, UUID uuid, Handler handler, ShowToast showToast) {
        mHandler = handler;
        BluetoothSocket tmp = null;
        mmDevice = device;
        mShowToast = showToast;
        Log.i(Tag, device.getName());

        try {
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.e(Tag, "Cannot connect to device: " + device.getName());
        }
        mmSocket = tmp;
    }

    public void run() {
        Log.i("Connect", "running...");
        try {
            Log.i(Tag, "conneecc");
            mmSocket.connect();
            Log.i(Tag, "connect");
        } catch (IOException connectException) {
            mShowToast.loong("Looks like server isn't running");
            try {
                mmSocket.close();
            } catch (IOException closeException) { }
            return;
        }

        mHandler.obtainMessage(Utils.IS_CONNECTED, mmSocket).sendToTarget();
    }

    public void cancel() {
        Log.i(Tag, "closing connection");
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(Tag, "cannot close connection");
        }
    }
}