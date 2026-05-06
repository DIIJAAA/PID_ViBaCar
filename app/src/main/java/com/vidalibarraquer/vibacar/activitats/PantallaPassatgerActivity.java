package com.vidalibarraquer.vibacar.activitats;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
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

public class PantallaPassatgerActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private final List<Viatge> totsElsViatges = new ArrayList<>();
    private AdaptadorViatges adaptadorViatges;
    private TextView txtBuit;
    private TextView txtInicialAvatar;
    private ShapeableImageView imatgePerfil;
    private TextInputEditText campCerca;
    private MaterialAutoCompleteTextView campFiltreZona;
    private String zonaFiltre = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_passatger);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txtBuit = findViewById(R.id.txtBuit);
        txtInicialAvatar = findViewById(R.id.txtInicialAvatar);
        imatgePerfil = findViewById(R.id.imatgePerfil);
        campCerca = findViewById(R.id.campCerca);
        campFiltreZona = findViewById(R.id.campFiltreZona);

        RecyclerView llistaViatges = findViewById(R.id.llistaViatges);
        adaptadorViatges = new AdaptadorViatges(this, viatge -> {
            Intent intent = new Intent(this, DetallViatgeActivity.class);
            intent.putExtra(DetallViatgeActivity.EXTRA_ID_VIATGE, viatge.getId());
            startActivity(intent);
        });
        llistaViatges.setLayoutManager(new LinearLayoutManager(this));
        llistaViatges.setAdapter(adaptadorViatges);

        findViewById(R.id.tabConductor).setOnClickListener(v -> {
            startActivity(new Intent(this, PantallaConductorActivity.class));
            finish();
        });
        findViewById(R.id.botoPerfil).setOnClickListener(v -> startActivity(new Intent(this, PerfilActivity.class)));

        configuraBotomNav();
        configuraFiltreZones();

        campCerca.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtraViatges();
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

    private void configuraFiltreZones() {
        String[] zonesOriginals = getResources().getStringArray(R.array.zones_trobada);
        List<String> zones = new ArrayList<>();
        zones.add(getString(R.string.zones_totes));
        for (String zona : zonesOriginals) {
            zones.add(zona);
        }

        campFiltreZona.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, zones));
        campFiltreZona.setText(getString(R.string.zones_totes), false);
        campFiltreZona.setOnItemClickListener((parent, view, position, id) -> {
            String valor = zones.get(position);
            zonaFiltre = getString(R.string.zones_totes).equalsIgnoreCase(valor) ? "" : valor;
            filtraViatges();
        });
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
                    UtilitatsAvatar.mostraAvatar(
                            imatgePerfil,
                            txtInicialAvatar,
                            perfil != null ? perfil.getFotoUri() : null,
                            nom
                    );

                    if (perfil != null && !TextUtils.isEmpty(perfil.getZona()) && TextUtils.isEmpty(zonaFiltre)) {
                        zonaFiltre = perfil.getZona();
                        campFiltreZona.setText(zonaFiltre, false);
                    }
                });
    }

    private void carregaViatges() {
        FirebaseUser usuariActual = auth.getCurrentUser();
        String uidActual = usuariActual == null ? "" : usuariActual.getUid();

        db.collection(UtilitatsFirebase.COL_VIATGES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totsElsViatges.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Viatge viatge = document.toObject(Viatge.class);
                        viatge.setId(document.getId());
                        if (uidActual.equals(viatge.getConductorId())) {
                            continue;
                        }
                        if (!"disponible".equalsIgnoreCase(viatge.getEstat())) {
                            continue;
                        }
                        if (viatge.getPlacesDisponibles() <= 0) {
                            continue;
                        }
                        totsElsViatges.add(viatge);
                    }

                    totsElsViatges.sort(Comparator.comparingLong(Viatge::getSortidaMillis));
                    filtraViatges();
                });
    }

    private void filtraViatges() {
        String text = campCerca.getText() == null ? "" : campCerca.getText().toString();
        String filtreText = text.toLowerCase(Locale.ROOT).trim();
        String filtreZona = zonaFiltre.toLowerCase(Locale.ROOT).trim();
        List<Viatge> filtrats = new ArrayList<>();

        for (Viatge viatge : totsElsViatges) {
            String resum = (
                    valorPerBuit(viatge.getOrigen()) + " " +
                    valorPerBuit(viatge.getDesti()) + " " +
                    valorPerBuit(viatge.getZonaSortida())
            ).toLowerCase(Locale.ROOT);

            boolean compleixText = filtreText.isEmpty() || resum.contains(filtreText);
            boolean compleixZona = filtreZona.isEmpty()
                    || valorPerBuit(viatge.getZonaSortida()).toLowerCase(Locale.ROOT).contains(filtreZona)
                    || valorPerBuit(viatge.getDesti()).toLowerCase(Locale.ROOT).contains(filtreZona);

            if (compleixText && compleixZona) {
                filtrats.add(viatge);
            }
        }

        adaptadorViatges.actualitzaDades(filtrats);
        txtBuit.setVisibility(filtrats.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void configuraBotomNav() {
        android.view.View navBuscar = findViewById(R.id.navBuscar);
        android.view.View navNotificacions = findViewById(R.id.navNotificacions);
        android.view.View navMissatges = findViewById(R.id.navMissatges);

        if (navBuscar != null) {
            navBuscar.setAlpha(1f);
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

    private String valorPerBuit(String text) {
        return text == null ? "" : text;
    }
}
