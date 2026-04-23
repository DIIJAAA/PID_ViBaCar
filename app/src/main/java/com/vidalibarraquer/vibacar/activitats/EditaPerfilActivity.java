package com.vidalibarraquer.vibacar.activitats;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.utilitats.GestorIdioma;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

import java.util.HashMap;
import java.util.Map;

public class EditaPerfilActivity extends AppCompatActivity {

    public static final String EXTRA_PRIMER_COP = "primer_cop";
    public static final String EXTRA_NOM_INICIAL = "nom_inicial";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextInputEditText campNom;
    private TextInputEditText campTelefon;
    private MaterialAutoCompleteTextView campZona;
    private MaterialAutoCompleteTextView campHoraHabitual;
    private TextInputEditText campPuntTrobada;
    private TextInputEditText campModelCotxe;
    private MaterialAutoCompleteTextView campPlacesHabituals;
    private TextInputEditText campBio;
    private RadioGroup grupRol;
    private LinearLayout layoutDadesConductor;
    private TextView txtDadesConductor;
    private ShapeableImageView imatgePerfil;
    private TextView txtInicialAvatar;
    private TextView txtMissatge;
    private boolean primerCop;
    private String fotoUri;

    private final ActivityResultLauncher<String> selectorFoto = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    fotoUri = uri.toString();
                    actualitzaAvatar();
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configura_perfil);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        primerCop = getIntent().getBooleanExtra(EXTRA_PRIMER_COP, false);

        campNom = findViewById(R.id.campNom);
        campTelefon = findViewById(R.id.campTelefon);
        campZona = findViewById(R.id.campZona);
        campHoraHabitual = findViewById(R.id.campHoraHabitual);
        campPuntTrobada = findViewById(R.id.campPuntTrobada);
        campModelCotxe = findViewById(R.id.campModelCotxe);
        campPlacesHabituals = findViewById(R.id.campPlacesHabituals);
        campBio = findViewById(R.id.campBio);
        grupRol = findViewById(R.id.grupRol);
        layoutDadesConductor = findViewById(R.id.layoutDadesConductor);
        txtDadesConductor = findViewById(R.id.txtDadesConductor);
        imatgePerfil = findViewById(R.id.imatgePerfil);
        txtInicialAvatar = findViewById(R.id.txtInicialAvatar);
        txtMissatge = findViewById(R.id.txtMissatge);

        configuraLlistes();
        grupRol.setOnCheckedChangeListener((group, checkedId) -> actualitzaBlocConductor());
        carregaPerfilSiExisteix();

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        findViewById(R.id.botoTriaFoto).setOnClickListener(v -> selectorFoto.launch("image/*"));
        ((MaterialButton) findViewById(R.id.botoDesarPerfil)).setOnClickListener(v -> desaPerfil());
    }

    private void configuraLlistes() {
        String[] zones = getResources().getStringArray(R.array.zones_trobada);
        String[] hores = getResources().getStringArray(R.array.hores_habituals_sortida);
        String[] places = getResources().getStringArray(R.array.places_habituals_conductor);
        campZona.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, zones));
        campHoraHabitual.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, hores));
        campPlacesHabituals.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, places));
    }

    private void carregaPerfilSiExisteix() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            finish();
            return;
        }

        String nomInicial = getIntent().getStringExtra(EXTRA_NOM_INICIAL);
        if (!TextUtils.isEmpty(nomInicial)) {
            campNom.setText(nomInicial);
            actualitzaAvatar();
        }

        db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(usuari.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Usuari perfil = documentSnapshot.toObject(Usuari.class);
                    if (perfil == null) return;

                    if (!TextUtils.isEmpty(perfil.getNom())) campNom.setText(perfil.getNom());
                    campTelefon.setText(perfil.getTelefon());
                    campZona.setText(perfil.getZona(), false);
                    campHoraHabitual.setText(perfil.getHoraSortidaHabitual(), false);
                    campPuntTrobada.setText(perfil.getPuntTrobadaHabitual());
                    campBio.setText(perfil.getBio());
                    campModelCotxe.setText(perfil.getModelCotxe());
                    if (perfil.getPlacesHabituals() > 0) {
                        campPlacesHabituals.setText(String.valueOf(perfil.getPlacesHabituals()), false);
                    }
                    fotoUri = perfil.getFotoUri();

                    if (UtilitatsFirebase.esRolConductor(perfil.getRol())) {
                        grupRol.check(R.id.radioConductor);
                    } else if (UtilitatsFirebase.esRolPassatger(perfil.getRol())) {
                        grupRol.check(R.id.radioPassatger);
                    }

                    actualitzaBlocConductor();
                    actualitzaAvatar();
                });
    }

    private void desaPerfil() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            txtMissatge.setText(R.string.error_no_usuari);
            return;
        }

        String nom = obteText(campNom);
        String telefon = obteText(campTelefon);
        String zona = campZona.getText() == null ? "" : campZona.getText().toString().trim();
        String horaHabitual = campHoraHabitual.getText() == null ? "" : campHoraHabitual.getText().toString().trim();
        String puntTrobada = obteText(campPuntTrobada);
        String modelCotxe = obteText(campModelCotxe);
        int placesHabituals = parseInt(campPlacesHabituals.getText() == null ? "" : campPlacesHabituals.getText().toString().trim());
        String bio = obteText(campBio);
        String rol = obteRolSeleccionat();

        if (TextUtils.isEmpty(nom)) {
            txtMissatge.setText(R.string.error_nom_buit);
            return;
        }
        if (TextUtils.isEmpty(zona)) {
            txtMissatge.setText(R.string.error_zona_buida);
            return;
        }
        if (TextUtils.isEmpty(horaHabitual)) {
            txtMissatge.setText(R.string.error_hora_habitual_buida);
            return;
        }
        if (TextUtils.isEmpty(puntTrobada)) {
            txtMissatge.setText(R.string.error_punt_trobada_buit);
            return;
        }
        if (TextUtils.isEmpty(rol)) {
            txtMissatge.setText(R.string.error_rol_buit);
            return;
        }
        if (UtilitatsFirebase.esRolConductor(rol) && TextUtils.isEmpty(modelCotxe)) {
            txtMissatge.setText(R.string.error_model_cotxe_buit);
            return;
        }
        if (UtilitatsFirebase.esRolConductor(rol) && placesHabituals <= 0) {
            txtMissatge.setText(R.string.error_places_habituals_buides);
            return;
        }

        Map<String, Object> dades = new HashMap<>();
        dades.put("uid", usuari.getUid());
        dades.put("nom", nom);
        dades.put("correu", usuari.getEmail());
        dades.put("telefon", telefon);
        dades.put("rol", rol);
        dades.put("zona", zona);
        dades.put("horaSortidaHabitual", horaHabitual);
        dades.put("puntTrobadaHabitual", puntTrobada);
        dades.put("bio", bio);
        dades.put("fotoUri", fotoUri);
        dades.put("idioma", GestorIdioma.obteIdiomaGuardat(this));
        dades.put("perfilCompletat", true);
        dades.put("emailVerified", usuari.isEmailVerified());
        if (UtilitatsFirebase.esRolConductor(rol)) {
            dades.put("modelCotxe", modelCotxe);
            dades.put("placesHabituals", placesHabituals);
        } else {
            dades.put("modelCotxe", "");
            dades.put("placesHabituals", 0);
        }

        db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(usuari.getUid())
                .set(dades, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (primerCop) {
                        UtilitatsFirebase.enviaVerificacio(this, usuari, task -> {
                            startActivity(new Intent(this, VerificaCorreuActivity.class));
                            finish();
                        });
                    } else if (usuari.isEmailVerified()) {
                        Class<?> desti = UtilitatsFirebase.esRolConductor(rol)
                                ? PantallaConductorActivity.class
                                : PantallaPassatgerActivity.class;
                        startActivity(new Intent(this, desti));
                        finish();
                    } else {
                        startActivity(new Intent(this, VerificaCorreuActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> txtMissatge.setText(R.string.error_generica));
    }

    private String obteRolSeleccionat() {
        int id = grupRol.getCheckedRadioButtonId();
        if (id == R.id.radioConductor) return UtilitatsFirebase.ROL_CONDUCTOR;
        if (id == R.id.radioPassatger) return UtilitatsFirebase.ROL_PASSATGER;
        return "";
    }

    private void actualitzaAvatar() {
        UtilitatsAvatar.mostraAvatar(imatgePerfil, txtInicialAvatar, fotoUri, obteText(campNom));
    }

    private void actualitzaBlocConductor() {
        boolean esConductor = grupRol.getCheckedRadioButtonId() == R.id.radioConductor;
        int visibilitat = esConductor ? android.view.View.VISIBLE : android.view.View.GONE;
        layoutDadesConductor.setVisibility(visibilitat);
        txtDadesConductor.setVisibility(visibilitat);
    }

    private int parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String obteText(TextInputEditText camp) {
        return camp.getText() == null ? "" : camp.getText().toString().trim();
    }
}
