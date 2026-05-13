package com.vidalibarraquer.vibacar.utilitats;

import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UtilitatsAvatar {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private UtilitatsAvatar() {
    }

    public static void mostraAvatar(ShapeableImageView imatge, TextView inicial, String fotoUri, String nom) {
        if (!TextUtils.isEmpty(fotoUri)) {
            if (fotoUri.startsWith("http://") || fotoUri.startsWith("https://")) {
                mostraAvatarRemot(imatge, inicial, fotoUri, nom);
                return;
            }
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

    private static void mostraAvatarRemot(ShapeableImageView imatge, TextView inicial, String fotoUri, String nom) {
        inicial.setText(obteInicial(nom));
        inicial.setVisibility(android.view.View.VISIBLE);
        imatge.setVisibility(android.view.View.GONE);

        EXECUTOR.execute(() -> {
            try (InputStream input = new URL(fotoUri).openStream()) {
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                if (bitmap == null) return;
                imatge.post(() -> {
                    imatge.setImageBitmap(bitmap);
                    imatge.setVisibility(android.view.View.VISIBLE);
                    inicial.setVisibility(android.view.View.GONE);
                });
            } catch (Exception ignored) {
                // Si la imatge remota no carrega, queda la inicial.
            }
        });
    }

    private static String obteInicial(String nom) {
        if (TextUtils.isEmpty(nom)) {
            return "?";
        }
        return String.valueOf(Character.toUpperCase(nom.trim().charAt(0)));
    }
}
