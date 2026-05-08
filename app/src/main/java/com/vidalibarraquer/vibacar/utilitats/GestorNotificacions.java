package com.vidalibarraquer.vibacar.utilitats;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.activitats.PantallaBenvingudaActivity;

@SuppressWarnings({"SpellCheckingInspection", "unused"})
public final class GestorNotificacions {

    public static final String CANAL_VIBACAR = "vibacar_notificacions";
    private static final int CODI_PERMIS_NOTIFICACIONS = 2104;

    private GestorNotificacions() {
    }

    public static void creaCanals(@NonNull Context context) {
        NotificationChannel canal = new NotificationChannel(
                CANAL_VIBACAR,
                context.getString(R.string.notificacions_canal_nom),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        canal.setDescription(context.getString(R.string.notificacions_canal_descripcio));

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(canal);
        }
    }

    public static void demanaPermisSiCal(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                CODI_PERMIS_NOTIFICACIONS
        );
    }

    public static void mostra(@NonNull Context context, String titol, String cos) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        creaCanals(context);

        Intent intent = new Intent(context, PantallaBenvingudaActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String titolFinal = TextUtils.isEmpty(titol) ? context.getString(R.string.app_name) : titol;
        String cosFinal = TextUtils.isEmpty(cos) ? context.getString(R.string.notificacio_generica) : cos;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_VIBACAR)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(titolFinal)
                .setContentText(cosFinal)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(cosFinal))
                .setAutoCancel(true)
                .setContentIntent(pendent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(context)
                .notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
    }
}
