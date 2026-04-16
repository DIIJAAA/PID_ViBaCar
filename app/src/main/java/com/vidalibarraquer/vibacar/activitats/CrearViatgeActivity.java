package com.vidalibarraquer.vibacar.activitats;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CrearViatgeActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextInputEditText campOrigen;
    private TextInputEditText campDesti;
    private MaterialAutoCompleteTextView campZona;
    private TextInputEditText campData;
    private TextInputEditText campHoraSortida;
    private TextInputEditText campHoraArribada;
    private TextInputEditText campPlaces;
    private TextInputEditText campAportacio;
    private TextInputEditText campObservacions;
    private TextView txtMissatge;
    private Usuari usuariPerfil;
    private Calendar calendariBase;
    private Long sortidaMillis;
    private Long arribadaMillis;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_viatge);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        calendariBase = Calendar.getInstance();

        campOrigen = findViewById(R.id.campOrigen);
        campDesti = findViewById(R.id.campDesti);
        campZona = findViewById(R.id.campZona);
        campData = findViewById(R.id.campData);
        campHoraSortida = findViewById(R.id.campHoraSortida);
        campHoraArribada = findViewById(R.id.campHoraArribada);
        campPlaces = findViewById(R.id.campPlaces);
        campAportacio = findViewById(R.id.campAportacio);
        campObservacions = findViewById(R.id.campObservacions);
        txtMissatge = findViewById(R.id.txtMissatge);

        configuraZones();
        carregaPerfil();

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        findViewById(R.id.botoPublicar).setOnClickListener(v -> publicaViatge());
        campData.setOnClickListener(v -> obreSelectorData());
        campHoraSortida.setOnClickListener(v -> obreSelectorHora(true));
        campHoraArribada.setOnClickListener(v -> obreSelectorHora(false));
    }

    private void configuraZones() {
        campZona.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.zones_trobada)));
    }

    private void carregaPerfil() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            finish();
            return;
        }

        db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(usuari.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    usuariPerfil = documentSnapshot.toObject(Usuari.class);
                    if (usuariPerfil != null && !TextUtils.isEmpty(usuariPerfil.getZona())) {
                        campZona.setText(usuariPerfil.getZona(), false);
                    }
                    if (usuariPerfil != null && !TextUtils.isEmpty(usuariPerfil.getHoraSortidaHabitual())) {
                        campHoraSortida.setText(usuariPerfil.getHoraSortidaHabitual());
                    }
                    if (usuariPerfil != null && usuariPerfil.getPlacesHabituals() > 0) {
                        campPlaces.setText(String.valueOf(usuariPerfil.getPlacesHabituals()));
                    }
                    if (usuariPerfil != null && !TextUtils.isEmpty(usuariPerfil.getPuntTrobadaHabitual())) {
                        campObservacions.setText(getString(
                                R.string.text_observacions_punt_trobada_format,
                                usuariPerfil.getPuntTrobadaHabitual()
                        ));
                    }
                });
    }

    private void obreSelectorData() {
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendariBase.set(Calendar.YEAR, year);
            calendariBase.set(Calendar.MONTH, month);
            calendariBase.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            campData.setText(UtilitatsData.formatData(calendariBase.getTimeInMillis()));
        }, calendariBase.get(Calendar.YEAR), calendariBase.get(Calendar.MONTH), calendariBase.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void obreSelectorHora(boolean esSortida) {
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            Calendar copia = (Calendar) calendariBase.clone();
            copia.set(Calendar.HOUR_OF_DAY, hourOfDay);
            copia.set(Calendar.MINUTE, minute);
            copia.set(Calendar.SECOND, 0);
            copia.set(Calendar.MILLISECOND, 0);

            if (esSortida) {
                sortidaMillis = copia.getTimeInMillis();
                campHoraSortida.setText(UtilitatsData.formatHora(sortidaMillis));
            } else {
                arribadaMillis = copia.getTimeInMillis();
                campHoraArribada.setText(UtilitatsData.formatHora(arribadaMillis));
            }
        }, 8, 0, true);
        dialog.show();
    }

    private void publicaViatge() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            txtMissatge.setText(R.string.error_no_usuari);
            return;
        }

        if (usuariPerfil != null && !UtilitatsFirebase.esRolConductor(usuariPerfil.getRol())) {
            txtMissatge.setText(R.string.missatge_nomes_conductors);
            return;
        }

        String origen = obteText(campOrigen);
        String desti = obteText(campDesti);
        String zona = campZona.getText() == null ? "" : campZona.getText().toString().trim();
        String observacions = obteText(campObservacions);
        int places = parseInt(obteText(campPlaces));
        double aportacio = parseDouble(obteText(campAportacio));

        if (TextUtils.isEmpty(origen)) {
            txtMissatge.setText(R.string.error_origen_buit);
            return;
        }
        if (TextUtils.isEmpty(desti)) {
            txtMissatge.setText(R.string.error_desti_buit);
            return;
        }
        if (TextUtils.isEmpty(zona)) {
            txtMissatge.setText(R.string.error_zona_buida);
            return;
        }
        if (TextUtils.isEmpty(obteText(campData))) {
            txtMissatge.setText(R.string.error_data_buida);
            return;
        }

        if (sortidaMillis == null && !TextUtils.isEmpty(obteText(campHoraSortida))) {
            sortidaMillis = combinaDataHora(obteText(campHoraSortida));
        }
        if (sortidaMillis == null || arribadaMillis == null) {
            txtMissatge.setText(R.string.error_hora_buida);
            return;
        }
        if (places <= 0) {
            txtMissatge.setText(R.string.error_places_buides);
            return;
        }
        if (aportacio <= 0) {
            txtMissatge.setText(R.string.error_aportacio_buida);
            return;
        }

        Map<String, Object> viatge = new HashMap<>();
        viatge.put("conductorId", usuari.getUid());
        viatge.put("conductorNom", usuariPerfil != null ? usuariPerfil.getNom() : getString(R.string.nom_marca));
        viatge.put("conductorFotoUri", usuariPerfil != null ? usuariPerfil.getFotoUri() : null);
        viatge.put("modelCotxeConductor", usuariPerfil != null ? usuariPerfil.getModelCotxe() : "");
        viatge.put("conductorValoracio", usuariPerfil != null ? usuariPerfil.getValoracioMitjana() : 0d);
        viatge.put("conductorValoracions", usuariPerfil != null ? usuariPerfil.getTotalValoracions() : 0L);
        viatge.put("origen", origen);
        viatge.put("desti", desti);
        viatge.put("zonaSortida", zona);
        viatge.put("sortidaMillis", sortidaMillis);
        viatge.put("arribadaMillis", arribadaMillis);
        viatge.put("placesTotals", places);
        viatge.put("placesDisponibles", places);
        viatge.put("aportacio", aportacio);
        viatge.put("observacions", observacions);
        viatge.put("estat", "disponible");

        db.collection(UtilitatsFirebase.COL_VIATGES)
                .add(viatge)
                .addOnSuccessListener(documentReference -> {
                    startActivity(new Intent(this, PantallaPrincipalActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> txtMissatge.setText(R.string.error_generica));
    }

    private String obteText(TextInputEditText camp) {
        return camp.getText() == null ? "" : camp.getText().toString().trim();
    }

    private int parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private double parseDouble(String text) {
        try {
            return Double.parseDouble(text.replace(",", "."));
        } catch (Exception ignored) {
            return 0d;
        }
    }

    private Long combinaDataHora(String horaText) {
        try {
            String[] parts = horaText.split(":");
            if (parts.length != 2) {
                return null;
            }
            Calendar copia = (Calendar) calendariBase.clone();
            copia.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            copia.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
            copia.set(Calendar.SECOND, 0);
            copia.set(Calendar.MILLISECOND, 0);
            return copia.getTimeInMillis();
        } catch (Exception ignored) {
            return null;
        }
    }
}
