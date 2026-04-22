package com.vidalibarraquer.vibacar.utilitats;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PuntsMapaViBaCar {

    public static final LatLng INSTITUT_VIDAL_I_BARRAQUER = new LatLng(41.1187, 1.2452);

    private static final Map<String, LatLng> PUNTS = new LinkedHashMap<>();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler HANDLER_PRINCIPAL = new Handler(Looper.getMainLooper());
    private static final String SUFIX_GEOCODER = ", Tarragona, Spain";

    static {
        PUNTS.put("tarragona centre", new LatLng(41.1189, 1.2445));
        PUNTS.put("sant pere i sant pau", new LatLng(41.1327, 1.2442));
        PUNTS.put("torreforta", new LatLng(41.1145, 1.2156));
        PUNTS.put("campclar", new LatLng(41.1090, 1.2049));
        PUNTS.put("bonavista", new LatLng(41.1117, 1.2172));
        PUNTS.put("la canonja", new LatLng(41.1202, 1.1787));
        PUNTS.put("vila-seca", new LatLng(41.1112, 1.1453));
        PUNTS.put("vilaseca", new LatLng(41.1112, 1.1453));
        PUNTS.put("cambrils", new LatLng(41.0668, 1.0552));
        PUNTS.put("reus", new LatLng(41.1540, 1.1086));
        PUNTS.put("valls", new LatLng(41.2864, 1.2491));
        PUNTS.put("salou", new LatLng(41.0768, 1.1417));
        PUNTS.put("institut vidal i barraquer", INSTITUT_VIDAL_I_BARRAQUER);
        PUNTS.put("vidal i barraquer", INSTITUT_VIDAL_I_BARRAQUER);
        PUNTS.put("institut", INSTITUT_VIDAL_I_BARRAQUER);
    }

    public interface CallbackCoordenada {
        void onResolt(@Nullable LatLng coordenada);
    }

    private PuntsMapaViBaCar() {
    }

    @Nullable
    public static LatLng obteCoordenada(@Nullable String textLliure) {
        String textNormalitzat = normalitza(textLliure);
        if (textNormalitzat.isEmpty()) {
            return null;
        }

        for (Map.Entry<String, LatLng> entrada : PUNTS.entrySet()) {
            if (textNormalitzat.contains(entrada.getKey())) {
                return entrada.getValue();
            }
        }

        return null;
    }

    public static LatLng obteDestiPerDefecte() {
        return INSTITUT_VIDAL_I_BARRAQUER;
    }

    public static void resolCoordenada(@NonNull Context context,
                                       @Nullable String textLliure,
                                       @NonNull CallbackCoordenada callback) {
        LatLng directe = obteCoordenada(textLliure);
        if (directe != null || TextUtils.isEmpty(textLliure) || !Geocoder.isPresent()) {
            callback.onResolt(directe);
            return;
        }

        Context contextAplicacio = context.getApplicationContext();
        String consulta = textLliure.trim();

        EXECUTOR.execute(() -> {
            LatLng trobada = geocodifica(contextAplicacio, consulta);
            if (trobada == null) {
                trobada = geocodifica(contextAplicacio, consulta + SUFIX_GEOCODER);
            }
            LatLng resultat = trobada;
            HANDLER_PRINCIPAL.post(() -> callback.onResolt(resultat));
        });
    }

    @Nullable
    private static LatLng geocodifica(@NonNull Context context, @NonNull String consulta) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> resultats = geocoder.getFromLocationName(consulta, 1);
            if (resultats != null && !resultats.isEmpty()) {
                Address adresa = resultats.get(0);
                return new LatLng(adresa.getLatitude(), adresa.getLongitude());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String normalitza(@Nullable String text) {
        if (text == null) {
            return "";
        }
        String senseAccents = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return senseAccents.toLowerCase(Locale.ROOT).trim();
    }
}
