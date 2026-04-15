package com.vidalibarraquer.vibacar.activitats;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.Locale;

public class PantallaPrincipalActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private final List<Viatge> totsElsViatges = new ArrayList<>();
    private AdaptadorViatges adaptadorViatges;
    private TextView txtSalutacio;
    private TextView txtBuit;
    private TextView txtInicialAvatar;
    private com.google.android.material.imageview.ShapeableImageView imatgePerfil;
    private com.google.android.material.textfield.TextInputEditText campCerca;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_principal);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txtSalutacio = findViewById(R.id.txtSalutacio);
        txtBuit = findViewById(R.id.txtBuit);
        txtInicialAvatar = findViewById(R.id.txtInicialAvatar);
        imatgePerfil = findViewById(R.id.imatgePerfil);
        campCerca = findViewById(R.id.campCerca);

        RecyclerView llistaViatges = findViewById(R.id.llistaViatges);
        adaptadorViatges = new AdaptadorViatges(this, viatge -> {
            Intent intent = new Intent(this, DetallViatgeActivity.class);
            intent.putExtra(DetallViatgeActivity.EXTRA_ID_VIATGE, viatge.getId());
            startActivity(intent);
        });
        llistaViatges.setLayoutManager(new LinearLayoutManager(this));
        llistaViatges.setAdapter(adaptadorViatges);

        findViewById(R.id.botoPerfil).setOnClickListener(v -> startActivity(new Intent(this, PerfilActivity.class)));
        findViewById(R.id.botoCrearViatge).setOnClickListener(v -> startActivity(new Intent(this, CrearViatgeActivity.class)));

        campCerca.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtraViatges(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregaUsuariCapcalera();
        carregaViatges();
    }

    private void carregaUsuariCapcalera() {
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
                    txtSalutacio.setText(getString(R.string.principal_salutacio_format, nom));
                    UtilitatsAvatar.mostraAvatar(
                            imatgePerfil,
                            txtInicialAvatar,
                            perfil != null ? perfil.getFotoUri() : null,
                            nom
                    );
                });
    }

    private void carregaViatges() {
        db.collection(UtilitatsFirebase.COL_VIATGES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totsElsViatges.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Viatge viatge = document.toObject(Viatge.class);
                        viatge.setId(document.getId());
                        if (!"disponible".equalsIgnoreCase(viatge.getEstat())) {
                            continue;
                        }
                        if (viatge.getPlacesDisponibles() <= 0) {
                            continue;
                        }
                        totsElsViatges.add(viatge);
                    }

                    totsElsViatges.sort(Comparator.comparingLong(Viatge::getSortidaMillis));
                    filtraViatges(campCerca.getText() == null ? "" : campCerca.getText().toString());
                });
    }

    private void filtraViatges(String text) {
        String filtre = text.toLowerCase(Locale.ROOT).trim();
        List<Viatge> filtrats = new ArrayList<>();

        for (Viatge viatge : totsElsViatges) {
            String resum = (
                    viatge.getOrigen() + " " +
                    viatge.getDesti() + " " +
                    viatge.getZonaSortida()
            ).toLowerCase(Locale.ROOT);

            if (filtre.isEmpty() || resum.contains(filtre)) {
                filtrats.add(viatge);
            }
        }

        adaptadorViatges.actualitzaDades(filtrats);
        txtBuit.setVisibility(filtrats.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}
