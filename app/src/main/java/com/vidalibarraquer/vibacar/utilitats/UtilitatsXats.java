package com.vidalibarraquer.vibacar.utilitats;

import android.text.TextUtils;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UtilitatsXats {

    public interface Callback {
        void onPreparat(String idXat);

        void onError(Exception e);
    }

    private UtilitatsXats() {
    }

    public static void preparaXatEntreUsuaris(FirebaseFirestore db,
                                              String uidA,
                                              String uidB,
                                              Map<String, Object> dades,
                                              Callback callback) {
        preparaXatEntreUsuaris(db, uidA, uidB, null, dades, callback);
    }

    public static void preparaXatEntreUsuaris(FirebaseFirestore db,
                                              String uidA,
                                              String uidB,
                                              String idLlegat,
                                              Map<String, Object> dades,
                                              Callback callback) {
        if (TextUtils.isEmpty(uidA) || TextUtils.isEmpty(uidB)) {
            callback.onError(new IllegalArgumentException("Usuaris del xat buits"));
            return;
        }

        List<DocumentSnapshot> candidats = new ArrayList<>();
        Set<String> idsVistos = new HashSet<>();
        final int[] consultesPendents = {TextUtils.isEmpty(idLlegat) ? 2 : 3};
        final boolean[] hiHaError = {false};

        Runnable acaba = () -> {
            if (hiHaError[0]) return;
            consultesPendents[0]--;
            if (consultesPendents[0] > 0) return;

            String idXat = triaMillorXat(candidats, uidA, uidB);
            dades.put("darreraActualitzacio", System.currentTimeMillis());

            db.collection(UtilitatsFirebase.COL_XATS)
                    .document(idXat)
                    .set(dades, SetOptions.merge())
                    .addOnSuccessListener(unused -> callback.onPreparat(idXat))
                    .addOnFailureListener(callback::onError);
        };

        db.collection(UtilitatsFirebase.COL_XATS)
                .whereEqualTo("conductorId", uidA)
                .whereEqualTo("passatgerId", uidB)
                .get()
                .addOnSuccessListener(docs -> {
                    for (DocumentSnapshot doc : docs.getDocuments()) {
                        if (idsVistos.add(doc.getId())) candidats.add(doc);
                    }
                    acaba.run();
                })
                .addOnFailureListener(e -> {
                    hiHaError[0] = true;
                    callback.onError(e);
                });

        db.collection(UtilitatsFirebase.COL_XATS)
                .whereEqualTo("conductorId", uidB)
                .whereEqualTo("passatgerId", uidA)
                .get()
                .addOnSuccessListener(docs -> {
                    for (DocumentSnapshot doc : docs.getDocuments()) {
                        if (idsVistos.add(doc.getId())) candidats.add(doc);
                    }
                    acaba.run();
                })
                .addOnFailureListener(e -> {
                    hiHaError[0] = true;
                    callback.onError(e);
                });

        if (!TextUtils.isEmpty(idLlegat)) {
            db.collection(UtilitatsFirebase.COL_XATS)
                    .document(idLlegat)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && idsVistos.add(doc.getId())) {
                            candidats.add(doc);
                        }
                        acaba.run();
                    })
                    .addOnFailureListener(e -> {
                        hiHaError[0] = true;
                        callback.onError(e);
                    });
        }
    }

    private static String triaMillorXat(List<DocumentSnapshot> candidats, String uidA, String uidB) {
        if (candidats.isEmpty()) {
            return UtilitatsFirebase.creaIdXatUsuaris(uidA, uidB);
        }

        DocumentSnapshot millor = candidats.get(0);
        for (DocumentSnapshot candidat : candidats) {
            if (valorActivitat(candidat) > valorActivitat(millor)) {
                millor = candidat;
            }
        }
        return millor.getId();
    }

    private static long valorActivitat(DocumentSnapshot doc) {
        Long darrera = doc.getLong("darreraActualitzacio");
        if (darrera != null && darrera > 0) return darrera;
        Long sortida = doc.getLong("sortidaMillis");
        return sortida != null ? sortida : 0L;
    }
}
