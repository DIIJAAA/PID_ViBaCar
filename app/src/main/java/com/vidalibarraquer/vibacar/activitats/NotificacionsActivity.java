package com.vidalibarraquer.vibacar.activitats;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.adaptadors.AdaptadorNotificacions;
import com.vidalibarraquer.vibacar.models.Notificacio;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsNotificacions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NotificacionsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private AdaptadorNotificacions adaptador;
    private TextView txtBuit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificacions);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txtBuit = findViewById(R.id.txtBuit);

        RecyclerView llista = findViewById(R.id.llistaNotificacions);
        adaptador = new AdaptadorNotificacions(this::marcaLlegidaIObre);
        llista.setLayoutManager(new LinearLayoutManager(this));
        llista.setAdapter(adaptador);

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        findViewById(R.id.botoMarcarTotes).setOnClickListener(v -> marcaTotesLlegides());

        configuraBotomNav();
    }

    private void configuraBotomNav() {
        android.view.View navBuscar = findViewById(R.id.navBuscar);
        android.view.View navMissatges = findViewById(R.id.navMissatges);

        if (navBuscar != null) {
            navBuscar.setOnClickListener(v -> {
                startActivity(new android.content.Intent(this, PantallaPassatgerActivity.class));
                finish();
            });
        }
        if (navMissatges != null) {
            navMissatges.setOnClickListener(v -> {
                startActivity(new android.content.Intent(this, BustiaXatsActivity.class));
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregaNotificacions();
    }

    private void carregaNotificacions() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) return;

        db.collection(UtilitatsNotificacions.COL_NOTIFICACIONS)
                .document(usuari.getUid())
                .collection(UtilitatsNotificacions.SUB_ITEMS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Notificacio> llista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Notificacio n = doc.toObject(Notificacio.class);
                        n.setId(doc.getId());
                        llista.add(n);
                    }
                    llista.sort(Comparator.comparingLong(Notificacio::getDataMillis).reversed());
                    adaptador.actualitzaDades(llista);
                    txtBuit.setVisibility(llista.isEmpty() ? View.VISIBLE : View.GONE);
                    findViewById(R.id.llistaNotificacions).setVisibility(llista.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void marcaLlegidaIObre(Notificacio notif) {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            return;
        }

        if (notif.isLlegida()) {
            obreDetallNotificacio(notif);
            return;
        }

        db.collection(UtilitatsNotificacions.COL_NOTIFICACIONS)
                .document(usuari.getUid())
                .collection(UtilitatsNotificacions.SUB_ITEMS)
                .document(notif.getId())
                .update("llegida", true)
                .addOnSuccessListener(unused -> {
                    carregaNotificacions();
                    obreDetallNotificacio(notif);
                });
    }

    private void marcaTotesLlegides() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) return;

        db.collection(UtilitatsNotificacions.COL_NOTIFICACIONS)
                .document(usuari.getUid())
                .collection(UtilitatsNotificacions.SUB_ITEMS)
                .whereEqualTo("llegida", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().update("llegida", true);
                    }
                    carregaNotificacions();
                });
    }

    private void obreDetallNotificacio(Notificacio notif) {
        if (notif == null) {
            return;
        }
        if (Notificacio.TIPUS_RESERVA_ACCEPTADA.equals(notif.getTipus())
                && !TextUtils.isEmpty(notif.getReferenciaId())) {
            android.content.Intent intent = new android.content.Intent(this, XatActivity.class);
            intent.putExtra(XatActivity.EXTRA_ID_XAT, notif.getReferenciaId());
            intent.putExtra(XatActivity.EXTRA_NOM_XAT, getString(R.string.bustia_titol));
            startActivity(intent);
            return;
        }
        if (Notificacio.TIPUS_NOVA_RESERVA.equals(notif.getTipus())) {
            startActivity(new android.content.Intent(this, PerfilActivity.class));
        }
    }
}
