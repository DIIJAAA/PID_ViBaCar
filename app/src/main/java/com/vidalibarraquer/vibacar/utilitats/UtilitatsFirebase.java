package com.vidalibarraquer.vibacar.utilitats;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vidalibarraquer.vibacar.R;

public final class UtilitatsFirebase {

    public static final String COL_USUARIS = "usuaris";
    public static final String COL_VIATGES = "viatges";
    public static final String COL_RESERVES = "reserves";
    public static final String COL_XATS = "xats";
    public static final String COL_MISSATGES = "missatges";
    public static final String ROL_CONDUCTOR = "conductor";
    public static final String ROL_PASSATGER = "passatger";
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

    public static String creaIdXat(String viatgeId, String passatgerId) {
        return viatgeId + "_" + passatgerId;
    }

    public static boolean esRolConductor(String rol) {
        if (TextUtils.isEmpty(rol)) {
            return false;
        }
        String valor = rol.trim().toLowerCase();
        return ROL_CONDUCTOR.equals(valor) || valor.contains("conductor");
    }

    public static boolean esRolPassatger(String rol) {
        if (TextUtils.isEmpty(rol)) {
            return false;
        }
        String valor = rol.trim().toLowerCase();
        return ROL_PASSATGER.equals(valor)
                || valor.contains("passatg")
                || valor.contains("pasaj")
                || valor.contains("passenger");
    }

    public static String etiquetaRol(@NonNull Context context, String rol) {
        if (esRolConductor(rol)) {
            return context.getString(R.string.rol_conductor);
        }
        if (esRolPassatger(rol)) {
            return context.getString(R.string.rol_passatger);
        }
        return context.getString(R.string.text_no_definit);
    }
}
