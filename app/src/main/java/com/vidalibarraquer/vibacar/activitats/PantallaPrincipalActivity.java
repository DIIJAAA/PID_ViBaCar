package com.vidalibarraquer.vibacar.activitats;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

public class PantallaPrincipalActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextView txtSalutacio;
    private TextView txtInfoPassatger;
    private TextView txtInfoConductor;
    private TextView txtInicialAvatar;
    private ShapeableImageView imatgePerfil;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_principal);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txtSalutacio = findViewById(R.id.txtSalutacio);
        txtInfoPassatger = findViewById(R.id.txtInfoPassatger);
        txtInfoConductor = findViewById(R.id.txtInfoConductor);
        txtInicialAvatar = findViewById(R.id.txtInicialAvatar);
        imatgePerfil = findViewById(R.id.imatgePerfil);

        MaterialCardView targetaPassatger = findViewById(R.id.targetaPassatger);
        MaterialCardView targetaConductor = findViewById(R.id.targetaConductor);

        findViewById(R.id.botoPerfil).setOnClickListener(v -> startActivity(new Intent(this, PerfilActivity.class)));
        targetaPassatger.setOnClickListener(v -> startActivity(new Intent(this, PantallaPassatgerActivity.class)));
        targetaConductor.setOnClickListener(v -> startActivity(new Intent(this, PantallaConductorActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregaCapcaleraIResums();
    }

    private void carregaCapcaleraIResums() {
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
                    String nom = perfil != null && perfil.getNom() != null && !perfil.getNom().trim().isEmpty()
                            ? perfil.getNom().trim()
                            : getString(R.string.nom_marca);

                    txtSalutacio.setText(getString(R.string.principal_hub_salutacio_format, nom));
                    UtilitatsAvatar.mostraAvatar(
                            imatgePerfil,
                            txtInicialAvatar,
                            perfil != null ? perfil.getFotoUri() : null,
                            nom
                    );
                });

        carregaResumPassatger(usuari.getUid());
        carregaResumConductor(usuari.getUid());
    }

    private void carregaResumPassatger(String uid) {
        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("passatgerId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int comptador = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String estat = doc.getString("estat");
                        if (UtilitatsFirebase.ESTAT_RESERVA_PENDENT.equals(estat)
                                || UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA.equals(estat)) {
                            comptador++;
                        }
                    }
                    txtInfoPassatger.setText(getString(R.string.principal_hub_passatger_info_format, comptador));
                })
                .addOnFailureListener(e -> txtInfoPassatger.setText(R.string.principal_hub_passatger_info_buit));
    }

    private void carregaResumConductor(String uid) {
        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("conductorId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int pendents = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (UtilitatsFirebase.ESTAT_RESERVA_PENDENT.equals(doc.getString("estat"))) {
                            pendents++;
                        }
                    }
                    txtInfoConductor.setText(getString(R.string.principal_hub_conductor_info_format, pendents));
                })
                .addOnFailureListener(e -> txtInfoConductor.setText(R.string.principal_hub_conductor_info_buit));
    }
}
