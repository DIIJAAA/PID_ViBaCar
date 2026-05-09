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
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.models.Viatge;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsBottomNav;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;
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
    private final List<String> viatgesAmbSollicituds = new ArrayList<>();

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

    private void carregaSollicitudsPendents() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            return;
        }

        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("conductorId", usuari.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int pendents = 0;
                    viatgesAmbSollicituds.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (UtilitatsFirebase.ESTAT_RESERVA_PENDENT.equals(doc.getString("estat"))) {
                            pendents++;
                            String viatgeId = doc.getString("viatgeId");
                            if (viatgeId != null && !viatgesAmbSollicituds.contains(viatgeId)) {
                                viatgesAmbSollicituds.add(viatgeId);
                            }
                        }
                    }
                    txtResumSollicituds.setText(String.valueOf(pendents));
                })
                .addOnFailureListener(e -> txtResumSollicituds.setText("0"));
    }

    private void obreSollicitudsPendents() {
        if (viatgesAmbSollicituds.isEmpty()) {
            Toast.makeText(this, R.string.conductor_sense_sollicituds_pendents, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, DetallViatgeActivity.class);
        intent.putExtra(DetallViatgeActivity.EXTRA_ID_VIATGE, viatgesAmbSollicituds.get(0));
        startActivity(intent);
    }
}
