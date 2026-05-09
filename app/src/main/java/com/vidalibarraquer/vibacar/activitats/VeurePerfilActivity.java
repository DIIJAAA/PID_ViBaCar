package com.vidalibarraquer.vibacar.activitats;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.content.Intent;
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
import com.google.firebase.firestore.SetOptions;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

import java.util.HashMap;
import java.util.Map;

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
    private MaterialButton botoSeguir;
    private MaterialButton botoVeureTrajectes;
    private String uidPerfil;
    private String nomPerfil;
    private String xatConductorId;
    private String xatPassatgerId;
    private boolean seguint;

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
        botoSeguir = findViewById(R.id.botoSeguir);
        botoVeureTrajectes = findViewById(R.id.botoVeureTrajectes);

        seccioSobreMi = findViewById(R.id.seccioSobreMi);
        seccioComentaris = findViewById(R.id.seccioComentaris);
        botoSeccioSobreMi = findViewById(R.id.botoSeccioSobreMi);
        botoSeccioComentaris = findViewById(R.id.botoSeccioComentaris);

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());

        botoSeccioSobreMi.setOnClickListener(v -> mostraSeccioSobreMi());
        botoSeccioComentaris.setOnClickListener(v -> mostraSeccioComentaris());

        uidPerfil = getIntent().getStringExtra(EXTRA_UID);
        if (TextUtils.isEmpty(uidPerfil)) {
            finish();
            return;
        }
        carregaPerfil(uidPerfil);
        configuraSeguiment();
        botoVeureTrajectes.setOnClickListener(v -> {
            Intent intent = new Intent(this, PantallaPassatgerActivity.class);
            intent.putExtra(PantallaPassatgerActivity.EXTRA_CONDUCTOR_ID, uidPerfil);
            startActivity(intent);
        });
    }

    private void mostraSeccioSobreMi() {
        seccioSobreMi.setVisibility(View.VISIBLE);
        seccioComentaris.setVisibility(View.GONE);

        botoSeccioSobreMi.setBackgroundResource(R.drawable.fons_boto_principal);
        botoSeccioSobreMi.setTextColor(getColor(R.color.color_text_clar));
        botoSeccioSobreMi.setStrokeWidth(0);
        botoSeccioSobreMi.setBackgroundTintList(null);

        botoSeccioComentaris.setBackgroundResource(0);
        botoSeccioComentaris.setStrokeWidth(2);
        botoSeccioComentaris.setStrokeColor(getColorStateList(R.color.color_text_principal));
        botoSeccioComentaris.setTextColor(getColor(R.color.color_text_principal));
        botoSeccioComentaris.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
    }

    private void mostraSeccioComentaris() {
        seccioSobreMi.setVisibility(View.GONE);
        seccioComentaris.setVisibility(View.VISIBLE);

        botoSeccioComentaris.setBackgroundResource(R.drawable.fons_boto_principal);
        botoSeccioComentaris.setTextColor(getColor(R.color.color_text_clar));
        botoSeccioComentaris.setStrokeWidth(0);
        botoSeccioComentaris.setBackgroundTintList(null);

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
        nomPerfil = nom;
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

        txtDades.setText(TextUtils.isEmpty(perfil.getBio())
                ? getString(R.string.perfil_bio_buida)
                : perfil.getBio());
    }

    private void configuraXat(Usuari perfil) {
        FirebaseUser usuariActual = auth.getCurrentUser();
        if (usuariActual == null || usuariActual.getUid().equals(perfil.getUid())) {
            botoObrirXat.setVisibility(View.GONE);
            return;
        }

        botoObrirXat.setVisibility(View.VISIBLE);
        bloquejaXat();
        comprovaXatDesbloquejat(usuariActual.getUid(), perfil.getUid());
    }

    private void comprovaXatDesbloquejat(String uidActual, String uidAltre) {
        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("passatgerId", uidActual)
                .whereEqualTo("conductorId", uidAltre)
                .whereEqualTo("estat", UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA)
                .get()
                .addOnSuccessListener(docs -> {
                    if (!docs.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot reserva = docs.getDocuments().get(0);
                        desbloquejaXat(reserva.getString("conductorId"), reserva.getString("passatgerId"));
                        return;
                    }
                    comprovaXatDesbloquejatInvers(uidActual, uidAltre);
                })
                .addOnFailureListener(e -> bloquejaXat());
    }

    private void comprovaXatDesbloquejatInvers(String uidActual, String uidAltre) {
        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("passatgerId", uidAltre)
                .whereEqualTo("conductorId", uidActual)
                .whereEqualTo("estat", UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA)
                .get()
                .addOnSuccessListener(docs -> {
                    if (!docs.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot reserva = docs.getDocuments().get(0);
                        desbloquejaXat(reserva.getString("conductorId"), reserva.getString("passatgerId"));
                    } else {
                        bloquejaXat();
                    }
                })
                .addOnFailureListener(e -> bloquejaXat());
    }

    private void bloquejaXat() {
        botoObrirXat.setEnabled(false);
        botoObrirXat.setText(R.string.boto_xat_bloquejat);
        botoObrirXat.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.color_linia)));
        botoObrirXat.setTextColor(getColor(R.color.color_text_secundari));
        botoObrirXat.setOnClickListener(null);
    }

    private void desbloquejaXat(String conductorId, String passatgerId) {
        xatConductorId = conductorId;
        xatPassatgerId = passatgerId;
        botoObrirXat.setEnabled(true);
        botoObrirXat.setText(R.string.boto_obrir_xat);
        botoObrirXat.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.color_principal)));
        botoObrirXat.setTextColor(getColor(R.color.color_text_clar));
        botoObrirXat.setOnClickListener(v -> obreXatDesbloquejat());
    }

    private void obreXatDesbloquejat() {
        if (TextUtils.isEmpty(xatConductorId) || TextUtils.isEmpty(xatPassatgerId)) return;
        String idXat = UtilitatsFirebase.creaIdXatUsuaris(xatConductorId, xatPassatgerId);
        Map<String, Object> dades = new HashMap<>();
        dades.put("conductorId", xatConductorId);
        dades.put("passatgerId", xatPassatgerId);
        dades.put("darreraActualitzacio", System.currentTimeMillis());
        db.collection(UtilitatsFirebase.COL_XATS)
                .document(idXat)
                .set(dades, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Intent intent = new Intent(this, XatActivity.class);
                    intent.putExtra(XatActivity.EXTRA_ID_XAT, idXat);
                    intent.putExtra(XatActivity.EXTRA_NOM_XAT, nomPerfil);
                    intent.putExtra(XatActivity.EXTRA_UID_ALTRE, uidPerfil);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show());
    }

    private void configuraSeguiment() {
        FirebaseUser actual = auth.getCurrentUser();
        if (actual == null || actual.getUid().equals(uidPerfil)) {
            botoSeguir.setVisibility(View.GONE);
            return;
        }
        String id = actual.getUid() + "_" + uidPerfil;
        db.collection(UtilitatsFirebase.COL_SEGUIMENTS)
                .document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    seguint = doc.exists();
                    actualitzaBotoSeguir();
                });
        botoSeguir.setOnClickListener(v -> {
            if (seguint) {
                db.collection(UtilitatsFirebase.COL_SEGUIMENTS).document(id).delete()
                        .addOnSuccessListener(unused -> {
                            seguint = false;
                            actualitzaBotoSeguir();
                        });
                return;
            }
            confirmaSeguiment(id, actual.getUid());
        });
    }

    private void confirmaSeguiment(String id, String uidActual) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirmar_seguir_titol)
                .setMessage(getString(R.string.confirmar_seguir_missatge, nomPerfil))
                .setPositiveButton(R.string.boto_seguir_usuari, (dialog, which) -> {
                    Map<String, Object> dades = new HashMap<>();
                    dades.put("seguidorId", uidActual);
                    dades.put("seguitId", uidPerfil);
                    dades.put("creatMillis", System.currentTimeMillis());
                    db.collection(UtilitatsFirebase.COL_SEGUIMENTS)
                            .document(id)
                            .set(dades, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                seguint = true;
                                actualitzaBotoSeguir();
                            });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void actualitzaBotoSeguir() {
        botoSeguir.setText(seguint ? R.string.boto_seguidor_actiu : R.string.boto_seguir_usuari);
        if (seguint) {
            botoSeguir.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.color_principal)));
            botoSeguir.setTextColor(getColor(R.color.color_text_clar));
            botoSeguir.setStrokeColor(ColorStateList.valueOf(getColor(R.color.color_principal)));
        } else {
            botoSeguir.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            botoSeguir.setTextColor(getColor(R.color.color_principal));
            botoSeguir.setStrokeColor(ColorStateList.valueOf(getColor(R.color.color_principal)));
        }
    }

    private String valorPerMostrar(String valor) {
        return valor == null || valor.trim().isEmpty() ? getString(R.string.text_no_definit) : valor.trim();
    }
}
