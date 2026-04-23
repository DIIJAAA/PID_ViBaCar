package com.vidalibarraquer.vibacar.activitats;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ConfiguraPerfilActivity extends AppCompatActivity {

    public static final String EXTRA_PRIMER_COP = "primer_cop";
    public static final String EXTRA_NOM_INICIAL = "nom_inicial";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextInputEditText campNom;
    private TextInputEditText campDataNaixement;
    private MaterialAutoCompleteTextView campSexe;
    private TextInputEditText campModelCotxe;
    private MaterialAutoCompleteTextView campPlacesHabituals;
    private TextInputEditText campBio;
    private LinearLayout layoutDadesConductor;
    private TextView txtDadesConductor;
    private ShapeableImageView imatgePerfil;
    private TextView txtInicialAvatar;
    private TextView txtMissatge;
    private boolean primerCop;
    private String fotoUri;
    private Calendar calendariNaixement = Calendar.getInstance();

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
        campDataNaixement = findViewById(R.id.campDataNaixement);
        campSexe = findViewById(R.id.campSexe);
        campModelCotxe = findViewById(R.id.campModelCotxe);
        campPlacesHabituals = findViewById(R.id.campPlacesHabituals);
        campBio = findViewById(R.id.campBio);
        layoutDadesConductor = findViewById(R.id.layoutDadesConductor);
        txtDadesConductor = findViewById(R.id.txtDadesConductor);
        imatgePerfil = findViewById(R.id.imatgePerfil);
        txtInicialAvatar = findViewById(R.id.txtInicialAvatar);
        txtMissatge = findViewById(R.id.txtMissatge);

        configuraLlistes();
        configuraSelectorData();
        carregaPerfilSiExisteix();

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());

        FloatingActionButton botoTriaFoto = findViewById(R.id.botoTriaFoto);
        if (botoTriaFoto != null) {
            botoTriaFoto.setOnClickListener(v -> selectorFoto.launch("image/*"));
        }

        ((MaterialButton) findViewById(R.id.botoDesarPerfil)).setOnClickListener(v -> desaPerfil());
    }

    private void configuraLlistes() {
        String[] places = getResources().getStringArray(R.array.places_habituals_conductor);
        if (campPlacesHabituals != null) {
            campPlacesHabituals.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, places));
        }

        String[] sexes = {getString(R.string.sexe_home), getString(R.string.sexe_dona), getString(R.string.sexe_no_dir), getString(R.string.sexe_altre)};
        if (campSexe != null) {
            campSexe.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sexes));
        }
    }

    private void configuraSelectorData() {
        DatePickerDialog.OnDateSetListener date = (view, year, month, day) -> {
            calendariNaixement.set(Calendar.YEAR, year);
            calendariNaixement.set(Calendar.MONTH, month);
            calendariNaixement.set(Calendar.DAY_OF_MONTH, day);
            actualitzaEtiquetaData();
        };

        campDataNaixement.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(ConfiguraPerfilActivity.this, date,
                    calendariNaixement.get(Calendar.YEAR),
                    calendariNaixement.get(Calendar.MONTH),
                    calendariNaixement.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            dialog.show();
        });
    }

    private void actualitzaEtiquetaData() {
        String format = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        campDataNaixement.setText(sdf.format(calendariNaixement.getTime()));
    }

    private void carregaPerfilSiExisteix() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) return;

        db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(usuari.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Usuari perfil = documentSnapshot.toObject(Usuari.class);
                    if (perfil == null) return;

                    if (!TextUtils.isEmpty(perfil.getNom())) campNom.setText(perfil.getNom());
                    if (!TextUtils.isEmpty(perfil.getDataNaixement())) campDataNaixement.setText(perfil.getDataNaixement());
                    if (!TextUtils.isEmpty(perfil.getSexe())) campSexe.setText(perfil.getSexe(), false);
                    campBio.setText(perfil.getBio());
                    campModelCotxe.setText(perfil.getModelCotxe());
                    if (perfil.getPlacesHabituals() > 0) {
                        campPlacesHabituals.setText(String.valueOf(perfil.getPlacesHabituals()), false);
                    }
                    fotoUri = perfil.getFotoUri();
                    actualitzaBlocConductor(perfil.getRol());
                    actualitzaAvatar();
                });
    }

    private void desaPerfil() {
        txtMissatge.setText("");

        String nom = obteText(campNom);
        String dataNaixement = obteText(campDataNaixement);
        String sexe = campSexe.getText().toString();
        String modelCotxe = obteText(campModelCotxe);
        int placesHabituals = parseInt(campPlacesHabituals.getText().toString());
        String bio = obteText(campBio);

        if (TextUtils.isEmpty(nom)) {
            txtMissatge.setText(getString(R.string.error_nom_buit));
            return;
        }
        if (TextUtils.isEmpty(dataNaixement)) {
            txtMissatge.setText(getString(R.string.error_data_naixement_buida));
            return;
        }
        if (TextUtils.isEmpty(sexe)) {
            txtMissatge.setText(getString(R.string.error_sexe_buit));
            return;
        }

        String rol = !TextUtils.isEmpty(modelCotxe) ? UtilitatsFirebase.ROL_CONDUCTOR : UtilitatsFirebase.ROL_PASSATGER;

        if (UtilitatsFirebase.esRolConductor(rol)) {
            if (TextUtils.isEmpty(modelCotxe)) {
                txtMissatge.setText(getString(R.string.error_model_cotxe_buit));
                return;
            }
            if (placesHabituals <= 0) {
                txtMissatge.setText(getString(R.string.error_places_habituals_buides));
                return;
            }
        }

        Map<String, Object> dades = new HashMap<>();
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) return;

        dades.put("uid", usuari.getUid());
        dades.put("nom", nom);
        dades.put("dataNaixement", dataNaixement);
        dades.put("sexe", sexe);
        dades.put("rol", rol);
        dades.put("bio", bio);
        dades.put("fotoUri", fotoUri);
        dades.put("perfilCompletat", true);
        dades.put("modelCotxe", rol.equals(UtilitatsFirebase.ROL_CONDUCTOR) ? modelCotxe : "");
        dades.put("placesHabituals", rol.equals(UtilitatsFirebase.ROL_CONDUCTOR) ? placesHabituals : 0);

        db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(usuari.getUid())
                .set(dades, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Class<?> desti = UtilitatsFirebase.esRolConductor(rol)
                            ? PantallaConductorActivity.class
                            : PantallaPassatgerActivity.class;
                    startActivity(new Intent(this, desti));
                    finish();
                })
                .addOnFailureListener(e -> txtMissatge.setText(getString(R.string.error_generica)));
    }

    private void actualitzaAvatar() {
        UtilitatsAvatar.mostraAvatar(imatgePerfil, txtInicialAvatar, fotoUri, obteText(campNom));
    }

    private void actualitzaBlocConductor(String rol) {
        boolean esConductor = UtilitatsFirebase.esRolConductor(rol);
        int visibilidad = esConductor ? android.view.View.VISIBLE : android.view.View.GONE;
        if (layoutDadesConductor != null) layoutDadesConductor.setVisibility(visibilidad);
        if (txtDadesConductor != null) txtDadesConductor.setVisibility(visibilidad);
    }

    private int parseInt(String text) {
        try { return Integer.parseInt(text); } catch (Exception e) { return 0; }
    }

    private String obteText(TextInputEditText camp) {
        return (camp != null && camp.getText() != null) ? camp.getText().toString().trim() : "";
    }
}
