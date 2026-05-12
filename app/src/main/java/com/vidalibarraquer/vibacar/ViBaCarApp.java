package com.vidalibarraquer.vibacar;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowInsetsController;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.vidalibarraquer.vibacar.activitats.PantallaBenvingudaActivity;
import com.vidalibarraquer.vibacar.activitats.PantallaConductorActivity;
import com.vidalibarraquer.vibacar.utilitats.GestorIdioma;
import com.vidalibarraquer.vibacar.utilitats.GestorNotificacions;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsNotificacions;

public class ViBaCarApp extends Application {

    private ListenerRegistration registreNotificacions;
    private String uidNotificacions;
    private long iniciEscoltaNotificacions;

    @Override
    public void onCreate() {
        super.onCreate();
        GestorIdioma.aplicaIdiomaGuardat(this);
        GestorNotificacions.creaCanals(this);
        configuraSessioINotificacions();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                ajustaBarresSistema(activity);
            }

            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    private void configuraSessioINotificacions() {
        FirebaseAuth.getInstance().addAuthStateListener(auth -> {
            if (auth.getCurrentUser() == null) {
                aturaNotificacions();
                return;
            }
            UtilitatsFirebase.actualitzaTokenMissatgeria();
            escoltaNotificacions(auth.getCurrentUser().getUid());
        });
    }

    private void escoltaNotificacions(String uid) {
        if (uid.equals(uidNotificacions) && registreNotificacions != null) {
            return;
        }
        aturaNotificacions();
        uidNotificacions = uid;
        iniciEscoltaNotificacions = System.currentTimeMillis();
        registreNotificacions = FirebaseFirestore.getInstance()
                .collection(UtilitatsNotificacions.COL_NOTIFICACIONS)
                .document(uid)
                .collection(UtilitatsNotificacions.SUB_ITEMS)
                .whereEqualTo("llegida", false)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    for (DocumentChange canvi : snapshots.getDocumentChanges()) {
                        if (canvi.getType() != DocumentChange.Type.ADDED) continue;
                        Long dataMillis = canvi.getDocument().getLong("dataMillis");
                        if (dataMillis == null || dataMillis < iniciEscoltaNotificacions - 2000L) continue;
                        String text = canvi.getDocument().getString("text");
                        GestorNotificacions.mostra(this, getString(R.string.app_name), text);
                    }
                });
    }

    private void aturaNotificacions() {
        if (registreNotificacions != null) {
            registreNotificacions.remove();
            registreNotificacions = null;
        }
        uidNotificacions = null;
    }

    private void ajustaBarresSistema(Activity activity) {
        Window window = activity.getWindow();
        window.setDecorFitsSystemWindows(true);

        boolean pantallaFosca = activity instanceof PantallaBenvingudaActivity
                || activity instanceof PantallaConductorActivity;
        int colorStatus = getColor(pantallaFosca ? R.color.color_conductor_fons : R.color.color_fons);
        int colorNav = getColor(R.color.color_targeta);
        window.setStatusBarColor(colorStatus);
        window.setNavigationBarColor(colorNav);

        window.getDecorView().post(() -> {
            WindowInsetsController controller = window.getInsetsController();
            if (controller == null) return;

            int aparenca = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            if (!pantallaFosca) {
                aparenca |= WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
            }
            controller.setSystemBarsAppearance(
                    aparenca,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            );
        });
    }
}
