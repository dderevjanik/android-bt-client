package com.example.danie.btapptest;
import android.content.Context;
import android.widget.Toast;

public class ShowToast {

    private final Context appContext;

    public ShowToast(Context context) {
        appContext = context;
    }

    public void quick(String text) {
        Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show();
    }

    public void loong(String text) {
        Toast.makeText(appContext, text, Toast.LENGTH_LONG).show();
    }

}
