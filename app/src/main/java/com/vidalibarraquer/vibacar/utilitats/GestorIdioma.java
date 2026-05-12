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
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tagLocale(codi)));
    }

    public static void guardaIAplica(Context context, String codiIdioma) {
        String codiNet = normalitza(codiIdioma);
        SharedPreferences prefs = context.getSharedPreferences(NOM_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(CLAU_IDIOMA, codiNet).commit();
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tagLocale(codiNet)));
    }

    public static String obteIdiomaGuardat(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(NOM_PREFS, Context.MODE_PRIVATE);
        return normalitza(prefs.getString(CLAU_IDIOMA, IDIOMA_PER_DEFECTE));
    }

    private static String normalitza(String codiIdioma) {
        if ("es".equals(codiIdioma) || "es-ES".equals(codiIdioma)) return "es";
        if ("en".equals(codiIdioma) || "en-US".equals(codiIdioma) || "en-GB".equals(codiIdioma)) return "en";
        return "ca";
    }

    private static String tagLocale(String codiIdioma) {
        if ("es".equals(codiIdioma)) return "es-ES";
        if ("en".equals(codiIdioma)) return "en";
        return "ca-ES";
    }
}
