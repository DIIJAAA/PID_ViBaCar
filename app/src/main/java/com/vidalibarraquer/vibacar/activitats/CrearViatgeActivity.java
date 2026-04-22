package com.vidalibarraquer.vibacar.activitats;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.utilitats.PuntsMapaViBaCar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsMapa;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CrearViatgeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private MaterialButtonToggleGroup grupSentit;
    private MaterialAutoCompleteTextView campZona;
    private TextView txtRutaResum;
    private TextView txtMissatge;
    private TextView txtMapaAlternatiu;
    private TextInputEditText campData;
    private TextInputEditText campHoraSortida;
    private TextInputEditText campHoraArribada;
    private TextInputEditText campPlaces;
    private TextInputEditText campPreu;
    private TextInputEditText campModelCotxe;
    private TextInputEditText campColorCotxe;
    private TextInputEditText campObservacions;
    private MaterialButton botoPublicar;

    private Usuari usuariPerfil;
    private Calendar calendariBase;
    private Long sortidaMillis;
    private Long arribadaMillis;
    private boolean tornadaCasa;
    private GoogleMap mapa;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_viatge);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        calendariBase = Calendar.getInstance();

        grupSentit = findViewById(R.id.grupSentit);
        campZona = findViewById(R.id.campZona);
        txtRutaResum = findViewById(R.id.txtRutaResum);
        txtMissatge = findViewById(R.id.txtMissatge);
        txtMapaAlternatiu = findViewById(R.id.txtMapaAlternatiu);
        campData = findViewById(R.id.campData);
        campHoraSortida = findViewById(R.id.campHoraSortida);
        campHoraArribada = findViewById(R.id.campHoraArribada);
        campPlaces = findViewById(R.id.campPlaces);
        campPreu = findViewById(R.id.campPreu);
        campModelCotxe = findViewById(R.id.campModelCotxe);
        campColorCotxe = findViewById(R.id.campColorCotxe);
        campObservacions = findViewById(R.id.campObservacions);
        botoPublicar = findViewById(R.id.botoPublicar);

        SupportMapFragment fragmentMapa = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentMapaCrear);
        if (fragmentMapa != null) {
            fragmentMapa.getMapAsync(this);
        } else {
            txtMapaAlternatiu.setVisibility(View.VISIBLE);
        }

        configuraSelectorZones();
        configuraSentit();
        carregaPerfil();

        findViewById(R.id.tabPassatger).setOnClickListener(v -> {
            startActivity(new Intent(this, PantallaPassatgerActivity.class));
            finish();
        });
        botoPublicar.setOnClickListener(v -> publicaViatge());
        campData.setOnClickListener(v -> obreSelectorData());
        campHoraSortida.setOnClickListener(v -> obreSelectorHoraSortida());
        campHoraArribada.setOnClickListener(v -> obreSelectorHoraArribada());

        actualitzaResumRuta();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapa = googleMap;
        mapa.getUiSettings().setMapToolbarEnabled(false);
        mapa.getUiSettings().setZoomControlsEnabled(true);
        mapa.getUiSettings().setRotateGesturesEnabled(false);
        mapa.getUiSettings().setTiltGesturesEnabled(false);
        actualitzaMapa();
    }

    private void configuraSelectorZones() {
        String[] zones = getResources().getStringArray(R.array.zones_trobada);
        campZona.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, zones));
        campZona.setOnItemClickListener((parent, view, position, id) -> {
            actualitzaResumRuta();
            actualitzaMapa();
        });
    }

    private void configuraSentit() {
        grupSentit.check(R.id.botoSentitInstitut);
        tornadaCasa = false;

        grupSentit.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            tornadaCasa = checkedId == R.id.botoSentitCasa;
            actualitzaResumRuta();
            actualitzaMapa();
        });
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
                    if (usuariPerfil != null && !TextUtils.isEmpty(usuariPerfil.getModelCotxe())) {
                        campModelCotxe.setText(usuariPerfil.getModelCotxe());
                    }
                });
    }

    private void actualitzaResumRuta() {
        String zona = obteZonaSeleccionada();
        if (TextUtils.isEmpty(zona)) {
            txtRutaResum.setText(R.string.crear_viatge_ruta_pendent);
            return;
        }
        String origen = obteOrigen(zona);
        String desti = obteDesti(zona);
        txtRutaResum.setText(getString(R.string.text_ruta_format, origen, desti));
    }

    private void actualitzaMapa() {
        if (mapa == null) {
            return;
        }
        String zona = obteZonaSeleccionada();
        if (TextUtils.isEmpty(zona)) {
            UtilitatsMapa.dibuixaRuta(this, mapa, null, null, null, null, txtMapaAlternatiu);
            return;
        }
        String origen = obteOrigen(zona);
        String desti = obteDesti(zona);
        PuntsMapaViBaCar.resolCoordenada(this, origen, puntOrigen ->
                PuntsMapaViBaCar.resolCoordenada(this, desti, puntDesti ->
                        UtilitatsMapa.dibuixaRuta(this, mapa, puntOrigen, puntDesti, origen, desti, txtMapaAlternatiu)
                )
        );
    }

    private String obteOrigen(String zona) {
        return tornadaCasa ? getString(R.string.desti_per_defecte) : zona;
    }

    private String obteDesti(String zona) {
        return tornadaCasa ? zona : getString(R.string.desti_per_defecte);
    }

    private String obteZonaSeleccionada() {
        return campZona.getText() == null ? "" : campZona.getText().toString().trim();
    }

    private void obreSelectorData() {
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendariBase.set(Calendar.YEAR, year);
            calendariBase.set(Calendar.MONTH, month);
            calendariBase.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            campData.setText(UtilitatsData.formatData(calendariBase.getTimeInMillis()));
            recalculaMillisAmbNovaDada();
        }, calendariBase.get(Calendar.YEAR), calendariBase.get(Calendar.MONTH), calendariBase.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void obreSelectorHoraSortida() {
        int initH = 8, initM = 0;
        if (sortidaMillis != null) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(sortidaMillis);
            initH = c.get(Calendar.HOUR_OF_DAY);
            initM = c.get(Calendar.MINUTE);
        }
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            Calendar c = (Calendar) calendariBase.clone();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            sortidaMillis = c.getTimeInMillis();
            campHoraSortida.setText(UtilitatsData.formatHora(sortidaMillis));
        }, initH, initM, true);
        dialog.show();
    }

    private void obreSelectorHoraArribada() {
        int initH = 8, initM = 25;
        if (arribadaMillis != null) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(arribadaMillis);
            initH = c.get(Calendar.HOUR_OF_DAY);
            initM = c.get(Calendar.MINUTE);
        } else if (sortidaMillis != null) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(sortidaMillis + 25 * 60_000L);
            initH = c.get(Calendar.HOUR_OF_DAY);
            initM = c.get(Calendar.MINUTE);
        }
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            Calendar c = (Calendar) calendariBase.clone();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            arribadaMillis = c.getTimeInMillis();
            campHoraArribada.setText(UtilitatsData.formatHora(arribadaMillis));
        }, initH, initM, true);
        dialog.show();
    }

    private void recalculaMillisAmbNovaDada() {
        if (sortidaMillis != null) {
            Calendar antic = Calendar.getInstance();
            antic.setTimeInMillis(sortidaMillis);
            Calendar nou = (Calendar) calendariBase.clone();
            nou.set(Calendar.HOUR_OF_DAY, antic.get(Calendar.HOUR_OF_DAY));
            nou.set(Calendar.MINUTE, antic.get(Calendar.MINUTE));
            nou.set(Calendar.SECOND, 0);
            nou.set(Calendar.MILLISECOND, 0);
            sortidaMillis = nou.getTimeInMillis();
        }
        if (arribadaMillis != null) {
            Calendar antic = Calendar.getInstance();
            antic.setTimeInMillis(arribadaMillis);
            Calendar nou = (Calendar) calendariBase.clone();
            nou.set(Calendar.HOUR_OF_DAY, antic.get(Calendar.HOUR_OF_DAY));
            nou.set(Calendar.MINUTE, antic.get(Calendar.MINUTE));
            nou.set(Calendar.SECOND, 0);
            nou.set(Calendar.MILLISECOND, 0);
            arribadaMillis = nou.getTimeInMillis();
        }
    }

    private void publicaViatge() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            txtMissatge.setText(R.string.error_no_usuari);
            return;
        }

        String zona = obteZonaSeleccionada();
        if (TextUtils.isEmpty(zona)) {
            txtMissatge.setText(R.string.error_zona_buida);
            return;
        }
        if (TextUtils.isEmpty(obteText(campData))) {
            txtMissatge.setText(R.string.error_data_buida);
            return;
        }
        if (sortidaMillis == null || arribadaMillis == null) {
            txtMissatge.setText(R.string.error_hora_buida);
            return;
        }

        int places = parseInt(obteText(campPlaces));
        if (places <= 0) {
            txtMissatge.setText(R.string.error_places_buides);
            return;
        }
        double preu = parseDouble(obteText(campPreu));
        if (preu <= 0) {
            txtMissatge.setText(R.string.error_preu_buit);
            return;
        }

        String modelCotxe = obteText(campModelCotxe);
        if (TextUtils.isEmpty(modelCotxe)) {
            txtMissatge.setText(R.string.error_model_cotxe_buit);
            return;
        }

        String nomConductor = (usuariPerfil != null && usuariPerfil.getNom() != null)
                ? usuariPerfil.getNom() : getString(R.string.nom_marca);
        String fotoConductor = usuariPerfil != null ? usuariPerfil.getFotoUri() : null;
        double valoracio = usuariPerfil != null ? usuariPerfil.getValoracioMitjana() : 0d;
        long totalValoracios = usuariPerfil != null ? usuariPerfil.getTotalValoracions() : 0L;

        String origen = obteOrigen(zona);
        String desti = obteDesti(zona);
        String zonaSortida = tornadaCasa ? getString(R.string.desti_per_defecte) : zona;

        Map<String, Object> viatge = new HashMap<>();
        viatge.put("conductorId", usuari.getUid());
        viatge.put("conductorNom", nomConductor);
        viatge.put("conductorFotoUri", fotoConductor);
        viatge.put("modelCotxeConductor", modelCotxe);
        viatge.put("colorCotxeConductor", obteText(campColorCotxe));
        viatge.put("conductorValoracio", valoracio);
        viatge.put("conductorValoracions", totalValoracios);
        viatge.put("origen", origen);
        viatge.put("desti", desti);
        viatge.put("zonaSortida", zonaSortida);
        viatge.put("sentitTrajecte", tornadaCasa ? "tornada" : "anada");
        viatge.put("sortidaMillis", sortidaMillis);
        viatge.put("arribadaMillis", arribadaMillis);
        viatge.put("placesTotals", places);
        viatge.put("placesDisponibles", places);
        viatge.put("aportacio", preu);
        viatge.put("observacions", obteText(campObservacions));
        viatge.put("estat", "disponible");

        botoPublicar.setEnabled(false);
        db.collection(UtilitatsFirebase.COL_VIATGES)
                .add(viatge)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, R.string.missatge_viatge_creat, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, PantallaConductorActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    botoPublicar.setEnabled(true);
                    txtMissatge.setText(R.string.error_generica);
                });
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
}
