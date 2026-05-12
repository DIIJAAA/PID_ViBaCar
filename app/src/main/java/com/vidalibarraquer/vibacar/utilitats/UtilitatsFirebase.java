package com.vidalibarraquer.vibacar.utilitats;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.Collections;

public final class UtilitatsFirebase {

    public static final String COL_USUARIS = "usuaris";
    public static final String COL_VIATGES = "viatges";
    public static final String COL_RESERVES = "reserves";
    public static final String COL_XATS = "xats";
    public static final String COL_MISSATGES = "missatges";
    public static final String COL_SEGUIMENTS = "seguiments";
    public static final String ESTAT_RESERVA_PENDENT = "pendent";
    public static final String ESTAT_RESERVA_ACCEPTADA = "acceptada";
    public static final String ESTAT_RESERVA_REBUTJADA = "rebutjada";
    public static final String ESTAT_RESERVA_CANCELADA = "cancelada";
    public static final String ESTAT_VIATGE_DISPONIBLE = "disponible";
    public static final String ESTAT_VIATGE_COMPLETAT = "completat";
    public static final String ESTAT_VIATGE_CANCELAT = "cancelat";
    public static final String DOMINI_CENTRE = "vidalibarraquer.net";

    private UtilitatsFirebase() {
    }

    public static boolean esCorreuCentre(String correu) {
        return !TextUtils.isEmpty(correu) && correu.trim().toLowerCase().endsWith("@" + DOMINI_CENTRE);
    }

    public static void enviaVerificacio(@NonNull Activity activity,
                                        @NonNull FirebaseUser usuari,
                                        @NonNull OnCompleteListener<Void> listener) {
        FirebaseAuth.getInstance().setLanguageCode(GestorIdioma.obteIdiomaGuardat(activity));
        usuari.sendEmailVerification().addOnCompleteListener(activity, listener);
    }

    public static void desaTokenMissatgeria(@NonNull String token) {
        FirebaseUser usuari = FirebaseAuth.getInstance().getCurrentUser();
        if (usuari == null) return;
        FirebaseFirestore.getInstance()
                .collection(COL_USUARIS)
                .document(usuari.getUid())
                .set(Collections.singletonMap("fcmToken", token), SetOptions.merge())
                .addOnFailureListener(e -> Log.w("UtilitatsFirebase", "Error desant token FCM", e));
    }

    public static void actualitzaTokenMissatgeria() {
        FirebaseUser usuari = FirebaseAuth.getInstance().getCurrentUser();
        if (usuari == null) return;
        FirebaseMessaging.getInstance()
                .getToken()
                .addOnSuccessListener(UtilitatsFirebase::desaTokenMissatgeria)
                .addOnFailureListener(e -> Log.w("UtilitatsFirebase", "Error obtenint token FCM", e));
    }

    public static String creaIdReserva(String viatgeId, String passatgerId) {
        return viatgeId + "_" + passatgerId;
    }

    public static String creaIdXatUsuaris(String uidA, String uidB) {
        if (uidA.compareTo(uidB) < 0) {
            return "xat_" + uidA + "_" + uidB;
        }
        return "xat_" + uidB + "_" + uidA;
    }

    public static boolean esReservaActiva(String estat) {
        return ESTAT_RESERVA_PENDENT.equals(estat) || ESTAT_RESERVA_ACCEPTADA.equals(estat);
    }
}
