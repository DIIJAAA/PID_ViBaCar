package com.vidalibarraquer.vibacar.activitats;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.utilitats.PuntsMapaViBaCar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsGeo;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsMapa;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CrearViatgeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextInputEditText campOrigen;
    private TextInputEditText campDesti;
    private TextView txtRutaResum;
    private TextView txtMissatge;
    private TextView txtMapaAlternatiu;
    private TextInputEditText campData;
    private TextInputEditText campHoraSortida;
    private TextView txtArribadaEstimada;
    private TextInputEditText campPlaces;
    private TextInputEditText campPreu;
    private TextInputEditText campModelCotxe;
    private TextInputEditText campColorCotxe;
    private TextInputEditText campObservacions;
    private MaterialButton botoPublicar;
    private LinearProgressIndicator indicadorCarrega;

    private Usuari usuariPerfil;
    private Calendar calendariBase;
    private Long sortidaMillis;
    private Long arribadaMillis;
    private GoogleMap mapa;
    private LatLng origenCoordCalc;
    private LatLng destiCoordCalc;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_viatge);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        calendariBase = Calendar.getInstance();

        campOrigen = findViewById(R.id.campOrigen);
        campDesti = findViewById(R.id.campDesti);
        txtRutaResum = findViewById(R.id.txtRutaResum);
        txtMissatge = findViewById(R.id.txtMissatge);
        txtMapaAlternatiu = findViewById(R.id.txtMapaAlternatiu);
        campData = findViewById(R.id.campData);
        campHoraSortida = findViewById(R.id.campHoraSortida);
        txtArribadaEstimada = findViewById(R.id.txtArribadaEstimada);
        campPlaces = findViewById(R.id.campPlaces);
        campPreu = findViewById(R.id.campPreu);
        campModelCotxe = findViewById(R.id.campModelCotxe);
        campColorCotxe = findViewById(R.id.campColorCotxe);
        campObservacions = findViewById(R.id.campObservacions);
        botoPublicar = findViewById(R.id.botoPublicar);
        indicadorCarrega = findViewById(R.id.indicadorCarrega);

        SupportMapFragment fragmentMapa = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentMapaCrear);
        if (fragmentMapa != null) {
            fragmentMapa.getMapAsync(this);
        } else {
            txtMapaAlternatiu.setVisibility(View.VISIBLE);
        }

        carregaPerfil();

        findViewById(R.id.tabPassatger).setOnClickListener(v -> {
            startActivity(new Intent(this, PantallaPassatgerActivity.class));
            finish();
        });
        botoPublicar.setOnClickListener(v -> publicaViatge());
        campData.setOnClickListener(v -> obreSelectorData());
        campHoraSortida.setOnClickListener(v -> obreSelectorHoraSortida());

        View.OnFocusChangeListener actualitzaPrevisualitzacio = (v, hasFocus) -> {
            if (!hasFocus) {
                actualitzaResumRuta();
                actualitzaMapa();
            }
        };
        campOrigen.setOnFocusChangeListener(actualitzaPrevisualitzacio);
        campDesti.setOnFocusChangeListener(actualitzaPrevisualitzacio);

        actualitzaResumRuta();
        configuraSortidaSegura();
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
                });
    }

    private void actualitzaResumRuta() {
        String origen = obteText(campOrigen);
        String desti = obteText(campDesti);
        if (TextUtils.isEmpty(origen) && TextUtils.isEmpty(desti)) {
            txtRutaResum.setText(R.string.crear_viatge_ruta_pendent);
            return;
        }
        String origenMostrar = TextUtils.isEmpty(origen) ? getString(R.string.text_no_definit) : origen;
        String destiMostrar = TextUtils.isEmpty(desti) ? getString(R.string.text_no_definit) : desti;
        txtRutaResum.setText(getString(R.string.text_ruta_format, origenMostrar, destiMostrar));
    }

    private void actualitzaMapa() {
        if (mapa == null) return;
        String origen = obteText(campOrigen);
        String desti = obteText(campDesti);
        if (TextUtils.isEmpty(origen) && TextUtils.isEmpty(desti)) {
            origenCoordCalc = null;
            destiCoordCalc = null;
            UtilitatsMapa.dibuixaRuta(this, mapa, null, null, null, null, txtMapaAlternatiu);
            return;
        }
        PuntsMapaViBaCar.resolCoordenada(this, origen, puntOrigen ->
                PuntsMapaViBaCar.resolCoordenada(this, desti, puntDesti -> {
                    origenCoordCalc = puntOrigen;
                    destiCoordCalc = puntDesti;
                    UtilitatsMapa.dibuixaRuta(this, mapa, puntOrigen, puntDesti, origen, desti, txtMapaAlternatiu);
                    calculaIEstableixArribada();
                })
        );
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
            actualitzaMapa();
            calculaIEstableixArribada();
        }, initH, initM, true);
        dialog.show();
    }

    private void calculaIEstableixArribada() {
        if (sortidaMillis == null || origenCoordCalc == null || destiCoordCalc == null) return;
        double distM = UtilitatsGeo.distanciaMetres(origenCoordCalc, destiCoordCalc);
        long durMs = (long) (distM * 1.4 / 50000.0 * 3_600_000L);
        long durRounded = ((durMs + 150_000L) / 300_000L) * 300_000L;
        arribadaMillis = sortidaMillis + Math.max(durRounded, 5L * 60_000L);
        txtArribadaEstimada.setText(getString(
                R.string.crear_viatge_arribada_estimacio_format,
                UtilitatsData.formatHora(arribadaMillis)
        ));
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
        calculaIEstableixArribada();
    }

    private void publicaViatge() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            txtMissatge.setText(R.string.error_no_usuari);
            return;
        }

        String origen = obteText(campOrigen);
        String desti = obteText(campDesti);
        if (TextUtils.isEmpty(origen)) {
            txtMissatge.setText(R.string.error_origen_buit);
            return;
        }
        if (TextUtils.isEmpty(desti)) {
            txtMissatge.setText(R.string.error_desti_buit);
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

        mostrarCarrega(true);
        PuntsMapaViBaCar.resolCoordenada(this, origen, puntOrigen ->
                PuntsMapaViBaCar.resolCoordenada(this, desti, puntDesti -> {
                    com.google.android.gms.maps.model.LatLng origenFinal = puntOrigen != null
                            ? puntOrigen : PuntsMapaViBaCar.obteDestiPerDefecte();
                    com.google.android.gms.maps.model.LatLng destiFinal = puntDesti != null
                            ? puntDesti : PuntsMapaViBaCar.obteDestiPerDefecte();

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
                    viatge.put("zonaSortida", origen);
                    viatge.put("origenLat", origenFinal.latitude);
                    viatge.put("origenLng", origenFinal.longitude);
                    viatge.put("destiLat", destiFinal.latitude);
                    viatge.put("destiLng", destiFinal.longitude);
                    viatge.put("sortidaMillis", sortidaMillis);
                    viatge.put("arribadaMillis", arribadaMillis);
                    viatge.put("placesTotals", places);
                    viatge.put("placesDisponibles", places);
                    viatge.put("aportacio", preu);
                    viatge.put("observacions", obteText(campObservacions));
                    viatge.put("estat", "disponible");

                    db.collection(UtilitatsFirebase.COL_VIATGES)
                            .add(viatge)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, R.string.missatge_viatge_creat, Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, PantallaConductorActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                mostrarCarrega(false);
                                txtMissatge.setText(R.string.error_generica);
                            });
                })
        );
    }

    private void configuraSortidaSegura() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (hiHaCanvis()) {
                    mostraDialegSortir();
                } else {
                    finish();
                }
            }
        });
    }

    private boolean hiHaCanvis() {
        return !TextUtils.isEmpty(obteText(campOrigen))
                || !TextUtils.isEmpty(obteText(campDesti))
                || !TextUtils.isEmpty(obteText(campObservacions))
                || sortidaMillis != null;
    }

    private void mostraDialegSortir() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialeg_sortir_titol)
                .setMessage(R.string.dialeg_sortir_missatge)
                .setPositiveButton(R.string.dialeg_sortir_confirmar, (dialog, which) -> finish())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void mostrarCarrega(boolean actiu) {
        indicadorCarrega.setVisibility(actiu ? View.VISIBLE : View.GONE);
        botoPublicar.setEnabled(!actiu);
        if (actiu) txtMissatge.setText("");
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
