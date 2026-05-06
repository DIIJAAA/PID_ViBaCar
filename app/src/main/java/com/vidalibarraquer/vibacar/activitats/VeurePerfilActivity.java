package com.vidalibarraquer.vibacar.activitats;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

public class VeurePerfilActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "uid_usuari";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextView txtNom;
    private TextView txtValoracio;
    private TextView txtDades;
    private TextView txtInicialAvatar;
    private ShapeableImageView imatgePerfil;
    private RatingBar barraReputacio;
    private MaterialButton botoObrirXat;

    // Elements per a les seccions
    private View seccioSobreMi;
    private View seccioComentaris;
    private MaterialButton botoSeccioSobreMi;
    private MaterialButton botoSeccioComentaris;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_veure_perfil);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txtNom = findViewById(R.id.txtNom);
        txtValoracio = findViewById(R.id.txtValoracio);
        txtDades = findViewById(R.id.txtDades);
        txtInicialAvatar = findViewById(R.id.txtInicialAvatar);
        imatgePerfil = findViewById(R.id.imatgePerfil);
        barraReputacio = findViewById(R.id.barraReputacio);
        botoObrirXat = findViewById(R.id.botoObrirXat);

        // Inicialització de seccions i botons
        seccioSobreMi = findViewById(R.id.seccioSobreMi);
        seccioComentaris = findViewById(R.id.seccioComentaris);
        botoSeccioSobreMi = findViewById(R.id.botoSeccioSobreMi);
        botoSeccioComentaris = findViewById(R.id.botoSeccioComentaris);

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());

        // Listeners per canviar de secció
        botoSeccioSobreMi.setOnClickListener(v -> mostraSeccioSobreMi());
        botoSeccioComentaris.setOnClickListener(v -> mostraSeccioComentaris());

        String uid = getIntent().getStringExtra(EXTRA_UID);
        if (TextUtils.isEmpty(uid)) {
            finish();
            return;
        }
        carregaPerfil(uid);
    }

    private void mostraSeccioSobreMi() {
        seccioSobreMi.setVisibility(View.VISIBLE);
        seccioComentaris.setVisibility(View.GONE);

        // Botó Sobre Mi actiu
        botoSeccioSobreMi.setBackgroundResource(R.drawable.fons_boto_principal);
        botoSeccioSobreMi.setTextColor(getColor(R.color.color_text_clar));
        botoSeccioSobreMi.setStrokeWidth(0);
        botoSeccioSobreMi.setBackgroundTintList(null);

        // Botó Comentaris inactiu
        botoSeccioComentaris.setBackgroundResource(0);
        botoSeccioComentaris.setStrokeWidth(2);
        botoSeccioComentaris.setStrokeColor(getColorStateList(R.color.color_text_principal));
        botoSeccioComentaris.setTextColor(getColor(R.color.color_text_principal));
        botoSeccioComentaris.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
    }

    private void mostraSeccioComentaris() {
        seccioSobreMi.setVisibility(View.GONE);
        seccioComentaris.setVisibility(View.VISIBLE);

        // Botó Comentaris actiu
        botoSeccioComentaris.setBackgroundResource(R.drawable.fons_boto_principal);
        botoSeccioComentaris.setTextColor(getColor(R.color.color_text_clar));
        botoSeccioComentaris.setStrokeWidth(0);
        botoSeccioComentaris.setBackgroundTintList(null);

        // Botó Sobre Mi inactiu
        botoSeccioSobreMi.setBackgroundResource(0);
        botoSeccioSobreMi.setStrokeWidth(2);
        botoSeccioSobreMi.setStrokeColor(getColorStateList(R.color.color_text_principal));
        botoSeccioSobreMi.setTextColor(getColor(R.color.color_text_principal));
        botoSeccioSobreMi.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
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
                    configuraXat(perfil);
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
            barraReputacio.setRating((float) perfil.getValoracioMitjana());
        } else {
            txtValoracio.setText(R.string.conductor_sense_valoracions);
            barraReputacio.setRating(0);
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
        if (!TextUtils.isEmpty(perfil.getBio())) {
            if (dades.length() > 0) dades.append("\n\n");
            dades.append(perfil.getBio());
        }
        txtDades.setText(dades.toString());
    }

    private void configuraXat(Usuari perfil) {
        FirebaseUser usuariActual = auth.getCurrentUser();
        if (usuariActual == null || usuariActual.getUid().equals(perfil.getUid())) {
            botoObrirXat.setVisibility(View.GONE);
            return;
        }

        botoObrirXat.setVisibility(View.VISIBLE);
        botoObrirXat.setOnClickListener(v -> {
             Toast.makeText(this, R.string.missatge_primer_reserva, Toast.LENGTH_SHORT).show();
        });
    }

    private String valorPerMostrar(String valor) {
        return valor == null || valor.trim().isEmpty() ? getString(R.string.text_no_definit) : valor.trim();
    }
}
