package com.vidalibarraquer.vibacar.utilitats;

import android.net.Uri;
import android.text.TextUtils;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;

public final class UtilitatsAvatar {

    private UtilitatsAvatar() {
    }

    public static void mostraAvatar(ShapeableImageView imatge, TextView inicial, String fotoUri, String nom) {
        if (!TextUtils.isEmpty(fotoUri)) {
            try {
                imatge.setImageURI(Uri.parse(fotoUri));
                imatge.setVisibility(android.view.View.VISIBLE);
                inicial.setVisibility(android.view.View.GONE);
                return;
            } catch (Exception ignored) {
                // Si la uri falla, es mostra la inicial.
            }
        }

        inicial.setText(obteInicial(nom));
        inicial.setVisibility(android.view.View.VISIBLE);
        imatge.setVisibility(android.view.View.GONE);
    }

    private static String obteInicial(String nom) {
        if (TextUtils.isEmpty(nom)) {
            return "?";
        }
        return String.valueOf(Character.toUpperCase(nom.trim().charAt(0)));
    }
}
