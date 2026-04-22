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

    private static final int MINUTS_TRAJECTE = 25;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private MaterialButtonToggleGroup grupSentit;
    private MaterialAutoCompleteTextView campZona;
    private TextView txtRutaResum;
    private TextView txtArribadaResum;
    private TextView txtMissatge;
    private TextView txtMapaAlternatiu;
    private TextView txtAvisCotxe;
    private TextInputEditText campData;
    private TextInputEditText campHoraSortida;
    private TextInputEditText campPlaces;
    private TextInputEditText campPreu;
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
        txtArribadaResum = findViewById(R.id.txtArribadaResum);
        txtMissatge = findViewById(R.id.txtMissatge);
        txtMapaAlternatiu = findViewById(R.id.txtMapaAlternatiu);
        txtAvisCotxe = findViewById(R.id.txtAvisCotxe);
        campData = findViewById(R.id.campData);
        campHoraSortida = findViewById(R.id.campHoraSortida);
        campPlaces = findViewById(R.id.campPlaces);
        campPreu = findViewById(R.id.campPreu);
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
        campHoraSortida.setOnClickListener(v -> obreSelectorHora());

        actualitzaResumRuta();
        actualitzaResumArribada();
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
                    validaCotxeAlPerfil();
                });
    }

    private void validaCotxeAlPerfil() {
        if (usuariPerfil == null || TextUtils.isEmpty(usuariPerfil.getModelCotxe())) {
            botoPublicar.setEnabled(false);
            txtAvisCotxe.setVisibility(View.VISIBLE);
            txtAvisCotxe.setText(R.string.error_falta_model_cotxe);
            txtAvisCotxe.setOnClickListener(v -> startActivity(new Intent(this, ConfiguraPerfilActivity.class)));
        } else {
            botoPublicar.setEnabled(true);
            txtAvisCotxe.setVisibility(View.GONE);
            txtAvisCotxe.setOnClickListener(null);
        }
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
            recalculaSortidaIArribada();
        }, calendariBase.get(Calendar.YEAR), calendariBase.get(Calendar.MONTH), calendariBase.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void obreSelectorHora() {
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            calendariBase.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendariBase.set(Calendar.MINUTE, minute);
            calendariBase.set(Calendar.SECOND, 0);
            calendariBase.set(Calendar.MILLISECOND, 0);
            sortidaMillis = calendariBase.getTimeInMillis();
            campHoraSortida.setText(UtilitatsData.formatHora(sortidaMillis));
            recalculaArribada();
            actualitzaResumArribada();
        }, 8, 0, true);
        dialog.show();
    }

    private void recalculaSortidaIArribada() {
        if (sortidaMillis == null) {
            return;
        }
        Calendar copia = (Calendar) calendariBase.clone();
        Calendar antic = Calendar.getInstance();
        antic.setTimeInMillis(sortidaMillis);
        copia.set(Calendar.HOUR_OF_DAY, antic.get(Calendar.HOUR_OF_DAY));
        copia.set(Calendar.MINUTE, antic.get(Calendar.MINUTE));
        copia.set(Calendar.SECOND, 0);
        copia.set(Calendar.MILLISECOND, 0);
        sortidaMillis = copia.getTimeInMillis();
        recalculaArribada();
        actualitzaResumArribada();
    }

    private void recalculaArribada() {
        if (sortidaMillis == null) {
            arribadaMillis = null;
            return;
        }
        arribadaMillis = sortidaMillis + MINUTS_TRAJECTE * 60_000L;
    }

    private void actualitzaResumArribada() {
        if (arribadaMillis == null) {
            txtArribadaResum.setText(getString(R.string.crear_viatge_arribada_estimada_pendent, MINUTS_TRAJECTE));
            return;
        }
        txtArribadaResum.setText(getString(
                R.string.crear_viatge_arribada_estimada_format,
                UtilitatsData.formatHora(arribadaMillis),
                MINUTS_TRAJECTE
        ));
    }

    private void publicaViatge() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            txtMissatge.setText(R.string.error_no_usuari);
            return;
        }
        if (usuariPerfil == null || TextUtils.isEmpty(usuariPerfil.getModelCotxe())) {
            txtMissatge.setText(R.string.error_falta_model_cotxe);
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

        String origen = obteOrigen(zona);
        String desti = obteDesti(zona);
        String zonaSortida = tornadaCasa ? getString(R.string.desti_per_defecte) : zona;

        Map<String, Object> viatge = new HashMap<>();
        viatge.put("conductorId", usuari.getUid());
        viatge.put("conductorNom", usuariPerfil.getNom() != null ? usuariPerfil.getNom() : getString(R.string.nom_marca));
        viatge.put("conductorFotoUri", usuariPerfil.getFotoUri());
        viatge.put("modelCotxeConductor", usuariPerfil.getModelCotxe());
        viatge.put("conductorValoracio", usuariPerfil.getValoracioMitjana());
        viatge.put("conductorValoracions", usuariPerfil.getTotalValoracions());
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
