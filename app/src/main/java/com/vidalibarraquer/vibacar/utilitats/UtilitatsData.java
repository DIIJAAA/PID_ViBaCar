package com.vidalibarraquer.vibacar.utilitats;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class UtilitatsData {

    private UtilitatsData() {
    }

    public static String formatData(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(millis));
    }

    public static String formatHora(long millis) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(millis));
    }

    public static String formatPreu(double valor) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ca-ES"));
        return format.format(valor);
    }

    public static boolean esPassat(long millis) {
        return millis < System.currentTimeMillis();
    }
}
