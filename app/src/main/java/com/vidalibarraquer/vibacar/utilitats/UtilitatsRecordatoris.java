package com.vidalibarraquer.vibacar.utilitats;

import android.content.Context;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Notificacio;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class UtilitatsRecordatoris {

    private UtilitatsRecordatoris() {}

    public static void comprova(Context context, FirebaseFirestore db, FirebaseUser usuari) {
        if (usuari == null) return;

        Calendar dema = Calendar.getInstance();
        dema.add(Calendar.DAY_OF_YEAR, 1);
        dema.set(Calendar.HOUR_OF_DAY, 0);
        dema.set(Calendar.MINUTE, 0);
        dema.set(Calendar.SECOND, 0);
        dema.set(Calendar.MILLISECOND, 0);

        long iniciDema = dema.getTimeInMillis();
        long fiDema = iniciDema + 24L * 60L * 60L * 1000L;
        String dataDema = new SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(new Date(iniciDema));
        String uid = usuari.getUid();

        db.collection(UtilitatsFirebase.COL_VIATGES)
                .whereEqualTo("conductorId", uid)
                .whereEqualTo("estat", UtilitatsFirebase.ESTAT_VIATGE_DISPONIBLE)
                .get()
                .addOnSuccessListener(docs -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : docs) {
                        Long sortida = doc.getLong("sortidaMillis");
                        if (sortida == null || sortida < iniciDema || sortida >= fiDema) continue;
                        publicaRecordatori(context, db, uid, "record_cond_" + doc.getId() + "_" + dataDema,
                                doc.getId(), doc.getString("origen"), doc.getString("desti"));
                    }
                });

        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("passatgerId", uid)
                .whereEqualTo("estat", UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA)
                .get()
                .addOnSuccessListener(docs -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot reserva : docs) {
                        String viatgeId = reserva.getString("viatgeId");
                        if (viatgeId == null) continue;
                        db.collection(UtilitatsFirebase.COL_VIATGES).document(viatgeId)
                                .get()
                                .addOnSuccessListener(viatge -> {
                                    Long sortida = viatge.getLong("sortidaMillis");
                                    if (sortida == null || sortida < iniciDema || sortida >= fiDema) return;
                                    publicaRecordatori(context, db, uid, "record_pass_" + viatgeId + "_" + dataDema,
                                            viatgeId, viatge.getString("origen"), viatge.getString("desti"));
                                });
                    }
                });
    }

    private static void publicaRecordatori(Context context, FirebaseFirestore db, String uid, String docId,
                                           String viatgeId, String origen, String desti) {
        db.collection(UtilitatsNotificacions.COL_NOTIFICACIONS)
                .document(uid)
                .collection(UtilitatsNotificacions.SUB_ITEMS)
                .document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) return;
                    UtilitatsNotificacions.publicaAmbId(db, uid, docId,
                            Notificacio.TIPUS_RECORDATORI_VIATGE,
                            context.getString(R.string.notif_recordatori_viatge,
                                    origen == null ? "" : origen,
                                    desti == null ? "" : desti),
                            viatgeId);
                });
    }
}
