package com.vidalibarraquer.vibacar.utilitats;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public final class GestorIdioma {

    private static final String NOM_PREFS = "pref_vibacar";
    private static final String CLAU_IDIOMA = "idioma_app";
    private static final String IDIOMA_PER_DEFECTE = "ca";

    private GestorIdioma() {
    }

    public static void aplicaIdiomaGuardat(Context context) {
        String codi = obteIdiomaGuardat(context);
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(codi));
    }

    public static void guardaIAplica(Context context, String codiIdioma) {
        SharedPreferences prefs = context.getSharedPreferences(NOM_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(CLAU_IDIOMA, codiIdioma).apply();
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(codiIdioma));
    }

    public static String obteIdiomaGuardat(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(NOM_PREFS, Context.MODE_PRIVATE);
        return prefs.getString(CLAU_IDIOMA, IDIOMA_PER_DEFECTE);
    }
}
