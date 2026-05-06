package com.vidalibarraquer.vibacar.utilitats;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UtilitatsGeo {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface CallbackText {
        void onResultat(@Nullable String text);
    }

    private UtilitatsGeo() {
    }

    public static float distanciaMetres(@Nullable LatLng puntA, @Nullable LatLng puntB) {
        if (puntA == null || puntB == null) {
            return Float.MAX_VALUE;
        }
        float[] resultat = new float[1];
        Location.distanceBetween(
                puntA.latitude,
                puntA.longitude,
                puntB.latitude,
                puntB.longitude,
                resultat
        );
        return resultat[0];
    }

    public static int calculaMinutsEstimats(@Nullable LatLng puntA, @Nullable LatLng puntB) {
        float metres = distanciaMetres(puntA, puntB);
        if (metres == Float.MAX_VALUE) {
            return 25;
        }
        double km = metres / 1000d;
        int minuts = (int) Math.round((km / 40d) * 60d);
        if (minuts < 10) {
            return 10;
        }
        if (minuts > 90) {
            return 90;
        }
        return minuts;
    }

    public static void obteLocalitat(@NonNull Context context,
                                     @Nullable LatLng punt,
                                     @NonNull CallbackText callback) {
        if (punt == null || !Geocoder.isPresent()) {
            callback.onResultat(null);
            return;
        }

        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            String localitat = null;
            try {
                Geocoder geocoder = new Geocoder(app, Locale.getDefault());
                List<Address> resultats = geocoder.getFromLocation(punt.latitude, punt.longitude, 1);
                if (resultats != null && !resultats.isEmpty()) {
                    Address adresa = resultats.get(0);
                    localitat = valorUtil(adresa.getLocality());
                    if (TextUtils.isEmpty(localitat)) {
                        localitat = valorUtil(adresa.getSubAdminArea());
                    }
                }
            } catch (Exception ignored) {
            }
            String finalLocalitat = localitat;
            MAIN.post(() -> callback.onResultat(finalLocalitat));
        });
    }

    @NonNull
    public static String normalitza(@Nullable String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ROOT);
    }

    @Nullable
    private static String valorUtil(@Nullable String text) {
        if (text == null) {
            return null;
        }
        String net = text.trim();
        return net.isEmpty() ? null : net;
    }
}
