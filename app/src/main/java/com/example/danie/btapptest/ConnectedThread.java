package com.example.danie.btapptest;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private final String Tag = "Connected";
    private Handler mHandler;

    public ConnectedThread(BluetoothSocket socket, Handler handler) {
        mHandler = handler;
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(Tag, "bad things happend during initializing streams");
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        Log.i(Tag, "streams connected");
    }

    public void run() {
        byte[] buffer;
        int bytes;
        Log.i("Connected", "running...");

        while (true) {
            try {
                buffer = new byte[1024];
                bytes = mmInStream.read(buffer);
                mHandler.obtainMessage(Utils.RECEIVING, bytes, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e("Connected", "bad things happend during streaming");
                break;
            }
        }
    }

    public void write(String string) {
        Log.i(Tag, "writing a msg");
        try {
            mmOutStream.write(string.getBytes());
        } catch (IOException e) {
            Log.e(Tag, "probably server connection dropped");
        }
    }

    public void cancel() {
        Log.i(Tag, "trying to close socket");
        try {
            mmSocket.close();
            Log.i(Tag, "socket successfuly closed");
        } catch (IOException e) {
            Log.e(Tag, "error during closing a socket");
        }
    }

}