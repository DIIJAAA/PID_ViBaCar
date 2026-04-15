package com.vidalibarraquer.vibacar;

import android.app.Application;

import com.vidalibarraquer.vibacar.utilitats.GestorIdioma;

public class ViBaCarApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        GestorIdioma.aplicaIdiomaGuardat(this);
    }
}
