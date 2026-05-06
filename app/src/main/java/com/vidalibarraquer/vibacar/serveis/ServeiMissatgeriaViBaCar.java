package com.vidalibarraquer.vibacar.serveis;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.vidalibarraquer.vibacar.utilitats.GestorNotificacions;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

@SuppressWarnings({"SpellCheckingInspection", "unused"})
public class ServeiMissatgeriaViBaCar extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            UtilitatsFirebase.desaTokenMissatgeria(token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String titol = null;
        String cos = null;
        if (message.getNotification() != null) {
            titol = message.getNotification().getTitle();
            cos = message.getNotification().getBody();
        }
        if (titol == null) {
            titol = message.getData().get("titol");
        }
        if (cos == null) {
            cos = message.getData().get("cos");
        }

        GestorNotificacions.mostra(this, titol, cos);
    }
}
