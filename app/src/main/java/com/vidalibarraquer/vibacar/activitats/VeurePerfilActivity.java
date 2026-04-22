package com.vidalibarraquer.vibacar.activitats;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

public class VeurePerfilActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "uid_usuari";

    private FirebaseFirestore db;
    private TextView txtNom;
    private TextView txtValoracio;
    private TextView txtDades;
    private TextView txtInicialAvatar;
    private ShapeableImageView imatgePerfil;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_veure_perfil);

        db = FirebaseFirestore.getInstance();

        txtNom = findViewById(R.id.txtNom);
        txtValoracio = findViewById(R.id.txtValoracio);
        txtDades = findViewById(R.id.txtDades);
        txtInicialAvatar = findViewById(R.id.txtInicialAvatar);
        imatgePerfil = findViewById(R.id.imatgePerfil);

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());

        String uid = getIntent().getStringExtra(EXTRA_UID);
        if (TextUtils.isEmpty(uid)) {
            finish();
            return;
        }
        carregaPerfil(uid);
    }

    private void carregaPerfil(String uid) {
        db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Usuari perfil = documentSnapshot.toObject(Usuari.class);
                    if (perfil == null) {
                        finish();
                        return;
                    }
                    mostraDades(perfil);
                })
                .addOnFailureListener(e -> finish());
    }

    private void mostraDades(Usuari perfil) {
        String nom = valorPerMostrar(perfil.getNom());
        txtNom.setText(nom);

        if (perfil.getTotalValoracions() > 0) {
            txtValoracio.setText(getString(
                    R.string.text_valoracio_format,
                    perfil.getValoracioMitjana(),
                    (int) perfil.getTotalValoracions()
            ));
        } else {
            txtValoracio.setText(R.string.conductor_sense_valoracions);
        }

        UtilitatsAvatar.mostraAvatar(imatgePerfil, txtInicialAvatar, perfil.getFotoUri(), nom);

        StringBuilder dades = new StringBuilder();

        if (!TextUtils.isEmpty(perfil.getZona())) {
            dades.append(getString(R.string.text_zona_sortida_format, perfil.getZona()));
        }
        if (!TextUtils.isEmpty(perfil.getPuntTrobadaHabitual())) {
            if (dades.length() > 0) dades.append("\n");
            dades.append(getString(R.string.text_observacions_punt_trobada_format, perfil.getPuntTrobadaHabitual()));
        }
        if (!TextUtils.isEmpty(perfil.getModelCotxe())) {
            if (dades.length() > 0) dades.append("\n");
            dades.append(getString(R.string.text_model_cotxe_format, perfil.getModelCotxe()));
        }
        if (!TextUtils.isEmpty(perfil.getBio())) {
            if (dades.length() > 0) dades.append("\n");
            dades.append(perfil.getBio());
        }

        txtDades.setText(dades.toString());
    }

    private String valorPerMostrar(String valor) {
        return valor == null || valor.trim().isEmpty() ? getString(R.string.text_no_definit) : valor.trim();
    }
}
