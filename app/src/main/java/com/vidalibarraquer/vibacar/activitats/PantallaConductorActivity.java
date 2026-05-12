package com.vidalibarraquer.vibacar.activitats;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.adaptadors.AdaptadorViatges;
import com.vidalibarraquer.vibacar.models.Notificacio;
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.models.Viatge;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsBottomNav;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsNotificacions;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsRecordatoris;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PantallaConductorActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private AdaptadorViatges adaptadorViatges;
    private TextView txtBuit;
    private TextView txtNomConductor;
    private TextView txtValoracioConductor;
    private TextView txtResumPublicats;
    private TextView txtResumSollicituds;
    private TextView txtInicialAvatar;
    private MaterialCardView cardSollicituds;
    private ShapeableImageView imatgePerfil;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_conductor);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txtBuit = findViewById(R.id.txtBuit);
        txtNomConductor = findViewById(R.id.txtNomConductor);
        txtValoracioConductor = findViewById(R.id.txtValoracioConductor);
        txtResumPublicats = findViewById(R.id.txtResumPublicats);
        txtResumSollicituds = findViewById(R.id.txtResumSollicituds);
        txtInicialAvatar = findViewById(R.id.txtInicialAvatar);
        cardSollicituds = findViewById(R.id.cardSollicituds);
        imatgePerfil = findViewById(R.id.imatgePerfil);

        RecyclerView llistaViatges = findViewById(R.id.llistaViatges);
        adaptadorViatges = new AdaptadorViatges(this, viatge -> {
            Intent intent = new Intent(this, DetallViatgeActivity.class);
            intent.putExtra(DetallViatgeActivity.EXTRA_ID_VIATGE, viatge.getId());
            startActivity(intent);
        });
        llistaViatges.setLayoutManager(new LinearLayoutManager(this));
        llistaViatges.setAdapter(adaptadorViatges);

        findViewById(R.id.tabPassatger).setOnClickListener(v -> {
            startActivity(new Intent(this, PantallaPassatgerActivity.class));
            finish();
        });
        findViewById(R.id.botoPerfil).setOnClickListener(v -> startActivity(new Intent(this, PerfilActivity.class)));
        findViewById(R.id.botoCrearViatge).setOnClickListener(v -> startActivity(new Intent(this, CrearViatgeActivity.class)));
        cardSollicituds.setOnClickListener(v -> obreSollicitudsPendents());

        configuraBotomNav();
    }

    private void configuraBotomNav() {
        UtilitatsBottomNav.marcaSeleccionada(this, UtilitatsBottomNav.SECCIO_BUSCAR);

        android.view.View navBuscar = findViewById(R.id.navBuscar);
        android.view.View navNotificacions = findViewById(R.id.navNotificacions);
        android.view.View navMissatges = findViewById(R.id.navMissatges);

        if (navBuscar != null) {
            navBuscar.setOnClickListener(v -> {
                startActivity(new Intent(this, PantallaPassatgerActivity.class));
                finish();
            });
        }
        if (navNotificacions != null) {
            navNotificacions.setOnClickListener(v ->
                    startActivity(new Intent(this, NotificacionsActivity.class)));
        }
        if (navMissatges != null) {
            navMissatges.setOnClickListener(v ->
                    startActivity(new Intent(this, BustiaXatsActivity.class)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregaCapcalera();
        carregaViatgesPropis();
        carregaSollicitudsPendents();
        UtilitatsBottomNav.actualitzaBadgeNotificacions(this);
        UtilitatsRecordatoris.comprova(this, db, auth.getCurrentUser());
    }

    private void carregaCapcalera() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            startActivity(new Intent(this, PantallaBenvingudaActivity.class));
            finish();
            return;
        }

        db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(usuari.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Usuari perfil = documentSnapshot.toObject(Usuari.class);
                    String nom = perfil != null && perfil.getNom() != null ? perfil.getNom() : getString(R.string.nom_marca);
                    txtNomConductor.setText(nom);

                    if (perfil != null && perfil.getTotalValoracions() > 0) {
                        txtValoracioConductor.setText(getString(
                                R.string.text_valoracio_format,
                                perfil.getValoracioMitjana(),
                                (int) perfil.getTotalValoracions()
                        ));
                    } else {
                        txtValoracioConductor.setText(R.string.conductor_sense_valoracions);
                    }

                    UtilitatsAvatar.mostraAvatar(
                            imatgePerfil,
                            txtInicialAvatar,
                            perfil != null ? perfil.getFotoUri() : null,
                            nom
                    );
                });
    }

    private void carregaViatgesPropis() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            return;
        }

        db.collection(UtilitatsFirebase.COL_VIATGES)
                .whereEqualTo("conductorId", usuari.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Viatge> viatges = new ArrayList<>();
                    int publicats = 0;
                    long ara = System.currentTimeMillis();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Viatge viatge = document.toObject(Viatge.class);
                        viatge.setId(document.getId());
                        if (UtilitatsFirebase.ESTAT_VIATGE_CANCELAT.equalsIgnoreCase(viatge.getEstat())) {
                            continue;
                        }
                        if (UtilitatsFirebase.ESTAT_VIATGE_COMPLETAT.equalsIgnoreCase(viatge.getEstat())) {
                            continue;
                        }
                        if (viatge.getSortidaMillis() < ara - 2L * 60L * 60L * 1000L) {
                            marcaCompletat(viatge);
                            continue;
                        }
                        viatges.add(viatge);
                        if (UtilitatsFirebase.ESTAT_VIATGE_DISPONIBLE.equalsIgnoreCase(viatge.getEstat())) {
                            publicats++;
                        }
                    }

                    viatges.sort(Comparator.comparingLong(Viatge::getSortidaMillis));
                    adaptadorViatges.actualitzaDades(viatges);
                    txtResumPublicats.setText(String.valueOf(publicats));
                    txtBuit.setVisibility(viatges.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                })
                .addOnFailureListener(e -> {
                    txtResumPublicats.setText("0");
                    txtBuit.setVisibility(android.view.View.VISIBLE);
                });
    }

    private void marcaCompletat(Viatge viatge) {
        if (viatge == null || android.text.TextUtils.isEmpty(viatge.getId())) return;

        db.collection(UtilitatsFirebase.COL_VIATGES)
                .document(viatge.getId())
                .update(
                        "estat", UtilitatsFirebase.ESTAT_VIATGE_COMPLETAT,
                        "completatMillis", System.currentTimeMillis()
                );

        String origen = viatge.getOrigen() != null ? viatge.getOrigen() : "";
        String desti = viatge.getDesti() != null ? viatge.getDesti() : "";
        String text = getString(R.string.notif_viatge_completat, origen, desti);
        String prefix = "fi_" + viatge.getId();

        UtilitatsNotificacions.publicaAmbId(db, viatge.getConductorId(),
                prefix + "_cond",
                Notificacio.TIPUS_VIATGE_COMPLETAT,
                text,
                viatge.getId());

        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("viatgeId", viatge.getId())
                .whereEqualTo("estat", UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA)
                .get()
                .addOnSuccessListener(reserves -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot reserva : reserves) {
                        String passatgerId = reserva.getString("passatgerId");
                        UtilitatsNotificacions.publicaAmbId(db, passatgerId,
                                prefix + "_" + passatgerId,
                                Notificacio.TIPUS_VIATGE_COMPLETAT,
                                text,
                                viatge.getId());
                    }
                });
    }

    private void carregaSollicitudsPendents() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            return;
        }

        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("conductorId", usuari.getUid())
                .whereEqualTo("estat", UtilitatsFirebase.ESTAT_RESERVA_PENDENT)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        txtResumSollicituds.setText("0");
                        return;
                    }
                    final int[] pendents = {0};
                    final int[] restants = {queryDocumentSnapshots.size()};
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String viatgeId = doc.getString("viatgeId");
                        if (android.text.TextUtils.isEmpty(viatgeId)) {
                            restants[0]--;
                            if (restants[0] == 0) txtResumSollicituds.setText(String.valueOf(pendents[0]));
                            continue;
                        }
                        db.collection(UtilitatsFirebase.COL_VIATGES).document(viatgeId).get()
                                .addOnSuccessListener(viatgeDoc -> {
                                    String estat = viatgeDoc.getString("estat");
                                    if (viatgeDoc.exists()
                                            && UtilitatsFirebase.ESTAT_VIATGE_DISPONIBLE.equals(estat)) {
                                        pendents[0]++;
                                    }
                                    restants[0]--;
                                    if (restants[0] == 0) txtResumSollicituds.setText(String.valueOf(pendents[0]));
                                })
                                .addOnFailureListener(e -> {
                                    restants[0]--;
                                    if (restants[0] == 0) txtResumSollicituds.setText(String.valueOf(pendents[0]));
                                });
                    }
                })
                .addOnFailureListener(e -> txtResumSollicituds.setText("0"));
    }

    private void obreSollicitudsPendents() {
        if ("0".contentEquals(txtResumSollicituds.getText())) {
            Toast.makeText(this, R.string.conductor_sense_sollicituds_pendents, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, SollicitudsActivity.class));
    }
}
