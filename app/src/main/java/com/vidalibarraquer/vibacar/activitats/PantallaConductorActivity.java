package com.vidalibarraquer.vibacar.activitats;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.adaptadors.AdaptadorViatges;
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.models.Viatge;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

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
    private TextView txtResumReservats;
    private TextView txtResumSollicituds;
    private TextView txtInicialAvatar;
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
        txtResumReservats = findViewById(R.id.txtResumReservats);
        txtResumSollicituds = findViewById(R.id.txtResumSollicituds);
        txtInicialAvatar = findViewById(R.id.txtInicialAvatar);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregaCapcalera();
        carregaViatgesPropis();
        carregaReservatsComPassatger();
        carregaSollicitudsPendents();
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

                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Viatge viatge = document.toObject(Viatge.class);
                        viatge.setId(document.getId());
                        viatges.add(viatge);
                        if ("disponible".equalsIgnoreCase(viatge.getEstat())) {
                            publicats++;
                        }
                    }

                    viatges.sort(Comparator.comparingLong(Viatge::getSortidaMillis));
                    adaptadorViatges.actualitzaDades(viatges);
                    txtResumPublicats.setText(getString(R.string.conductor_resum_publicats_format, publicats));
                    txtBuit.setVisibility(viatges.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                })
                .addOnFailureListener(e -> {
                    txtResumPublicats.setText(R.string.conductor_resum_publicats_buit);
                    txtBuit.setVisibility(android.view.View.VISIBLE);
                });
    }

    private void carregaReservatsComPassatger() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            return;
        }

        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("passatgerId", usuari.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int reservats = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String estat = doc.getString("estat");
                        if (UtilitatsFirebase.ESTAT_RESERVA_PENDENT.equals(estat)
                                || UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA.equals(estat)) {
                            reservats++;
                        }
                    }
                    txtResumReservats.setText(getString(R.string.conductor_resum_reservats_format, reservats));
                })
                .addOnFailureListener(e -> txtResumReservats.setText(R.string.conductor_resum_reservats_buit));
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
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (UtilitatsFirebase.ESTAT_RESERVA_PENDENT.equals(doc.getString("estat"))) {
                            pendents++;
                        }
                    }
                    txtResumSollicituds.setText(getString(R.string.conductor_resum_sollicituds_format, pendents));
                })
                .addOnFailureListener(e -> txtResumSollicituds.setText(R.string.conductor_resum_sollicituds_buit));
    }
}
