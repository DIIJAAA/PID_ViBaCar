package com.vidalibarraquer.vibacar.utilitats;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public final class UtilitatsNotificacions {

    public static final String COL_NOTIFICACIONS = "notificacions";
    public static final String SUB_ITEMS = "items";

    private UtilitatsNotificacions() {}

    public static void publica(FirebaseFirestore db, String uidDesti, String tipus, String text, String referenciaId) {
        if (uidDesti == null || uidDesti.isEmpty()) return;

        db.collection(COL_NOTIFICACIONS)
                .document(uidDesti)
                .collection(SUB_ITEMS)
                .add(creaDades(tipus, text, referenciaId));
    }

    public static void publicaAmbId(FirebaseFirestore db, String uidDesti, String docId,
                                    String tipus, String text, String referenciaId) {
        if (uidDesti == null || uidDesti.isEmpty() || docId == null || docId.isEmpty()) return;

        db.collection(COL_NOTIFICACIONS)
                .document(uidDesti)
                .collection(SUB_ITEMS)
                .document(docId)
                .set(creaDades(tipus, text, referenciaId));
    }

    private static Map<String, Object> creaDades(String tipus, String text, String referenciaId) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("tipus", tipus);
        notif.put("text", text);
        notif.put("llegida", false);
        notif.put("dataMillis", System.currentTimeMillis());
        notif.put("referenciaId", referenciaId != null ? referenciaId : "");
        return notif;
    }
}
