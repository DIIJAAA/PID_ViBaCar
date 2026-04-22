package com.vidalibarraquer.vibacar.activitats;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Viatge;
import com.vidalibarraquer.vibacar.utilitats.PuntsMapaViBaCar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsMapa;

import java.util.HashMap;
import java.util.Map;

public class DetallViatgeActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_ID_VIATGE = "id_viatge";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String idViatge;
    private Viatge viatgeActual;
    private GoogleMap mapa;
    private TextView txtNomConductor;
    private TextView txtValoracioConductor;
    private TextView txtRuta;
    private TextView txtInfoGeneral;
    private TextView txtObservacions;
    private TextView txtInicialConductor;
    private TextView txtMapaAlternatiu;
    private ShapeableImageView imatgeConductor;
    private MaterialButton botoReservar;
    private MaterialButton botoObrirXat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detall_viatge);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        idViatge = getIntent().getStringExtra(EXTRA_ID_VIATGE);

        txtNomConductor = findViewById(R.id.txtNomConductor);
        txtValoracioConductor = findViewById(R.id.txtValoracioConductor);
        txtRuta = findViewById(R.id.txtRuta);
        txtInfoGeneral = findViewById(R.id.txtInfoGeneral);
        txtObservacions = findViewById(R.id.txtObservacions);
        txtInicialConductor = findViewById(R.id.txtInicialConductor);
        txtMapaAlternatiu = findViewById(R.id.txtMapaAlternatiu);
        imatgeConductor = findViewById(R.id.imatgeConductor);
        botoReservar = findViewById(R.id.botoReservar);
        botoObrirXat = findViewById(R.id.botoObrirXat);

        SupportMapFragment fragmentMapa = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentMapa);
        if (fragmentMapa != null) {
            fragmentMapa.getMapAsync(this);
        } else {
            txtMapaAlternatiu.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        botoReservar.setOnClickListener(v -> reservaViatge());
        botoObrirXat.setOnClickListener(v -> obreXatSiExisteix());
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregaViatge();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapa = googleMap;
        mapa.getUiSettings().setMapToolbarEnabled(false);
        mapa.getUiSettings().setZoomControlsEnabled(true);
        mapa.getUiSettings().setRotateGesturesEnabled(false);
        mapa.getUiSettings().setTiltGesturesEnabled(false);
        mostraMapaSiCal();
    }

    private void carregaViatge() {
        if (TextUtils.isEmpty(idViatge)) {
            finish();
            return;
        }

        db.collection(UtilitatsFirebase.COL_VIATGES)
                .document(idViatge)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    viatgeActual = documentSnapshot.toObject(Viatge.class);
                    if (viatgeActual == null) {
                        finish();
                        return;
                    }
                    viatgeActual.setId(documentSnapshot.getId());
                    mostraDades();
                    actualitzaAccions();
                    mostraMapaSiCal();
                });
    }

    private void mostraDades() {
        txtNomConductor.setText(viatgeActual.getConductorNom());
        txtValoracioConductor.setText(getString(
                R.string.text_valoracio_format,
                viatgeActual.getConductorValoracio(),
                (int) viatgeActual.getConductorValoracions()
        ));
        txtRuta.setText(getString(R.string.text_ruta_format, viatgeActual.getOrigen(), viatgeActual.getDesti()));
        txtInfoGeneral.setText(getString(
                R.string.text_info_detall_format,
                UtilitatsData.formatData(viatgeActual.getSortidaMillis()),
                UtilitatsData.formatHora(viatgeActual.getSortidaMillis()),
                UtilitatsData.formatHora(viatgeActual.getArribadaMillis()),
                String.valueOf(viatgeActual.getPlacesDisponibles())
        ));
        txtInfoGeneral.append("\n");
        txtInfoGeneral.append(getString(R.string.text_zona_sortida_format, valorPerMostrar(viatgeActual.getZonaSortida())));
        if (!TextUtils.isEmpty(viatgeActual.getModelCotxeConductor())) {
            txtInfoGeneral.append("\n");
            txtInfoGeneral.append(getString(R.string.text_model_cotxe_format, viatgeActual.getModelCotxeConductor()));
        }

        String observacions = TextUtils.isEmpty(viatgeActual.getObservacions())
                ? getString(R.string.text_sense_observacions)
                : viatgeActual.getObservacions();
        txtObservacions.setText(getString(R.string.text_observacions_format, observacions));

        UtilitatsAvatar.mostraAvatar(
                imatgeConductor,
                txtInicialConductor,
                viatgeActual.getConductorFotoUri(),
                viatgeActual.getConductorNom()
        );
    }

    private void actualitzaAccions() {
        FirebaseUser usuari = auth.getCurrentUser();
        boolean esConductorDelViatge = usuari != null
                && viatgeActual != null
                && usuari.getUid().equals(viatgeActual.getConductorId());

        if (esConductorDelViatge) {
            botoReservar.setVisibility(View.GONE);
            botoObrirXat.setVisibility(View.GONE);
        } else {
            botoReservar.setVisibility(View.VISIBLE);
            botoObrirXat.setVisibility(View.VISIBLE);
        }
    }

    private void mostraMapaSiCal() {
        if (mapa == null || viatgeActual == null) {
            return;
        }

        String origenText = valorPerMostrar(viatgeActual.getOrigen());
        String destiText = valorPerMostrar(viatgeActual.getDesti());

        PuntsMapaViBaCar.resolCoordenada(this, viatgeActual.getOrigen(), origenDirecte ->
                resolOrigenAmbZona(origenDirecte, origenText, destiText));
    }

    private void resolOrigenAmbZona(@Nullable LatLng origen, String origenText, String destiText) {
        if (origen != null) {
            resolDesti(origen, origenText, destiText);
            return;
        }
        PuntsMapaViBaCar.resolCoordenada(this, viatgeActual.getZonaSortida(),
                origenZona -> resolDesti(origenZona, origenText, destiText));
    }

    private void resolDesti(@Nullable LatLng origen, String origenText, String destiText) {
        PuntsMapaViBaCar.resolCoordenada(this, viatgeActual.getDesti(), desti -> {
            LatLng destiFinal = desti != null ? desti : PuntsMapaViBaCar.obteDestiPerDefecte();
            UtilitatsMapa.dibuixaRuta(this, mapa, origen, destiFinal, origenText, destiText, txtMapaAlternatiu);
        });
    }

    private void reservaViatge() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null || viatgeActual == null) {
            Toast.makeText(this, R.string.error_no_usuari, Toast.LENGTH_SHORT).show();
            return;
        }

        if (usuari.getUid().equals(viatgeActual.getConductorId())) {
            Toast.makeText(this, R.string.missatge_no_et_pots_reservar, Toast.LENGTH_SHORT).show();
            return;
        }

        String idReserva = viatgeActual.getId() + "_" + usuari.getUid();
        DocumentReference refViatge = db.collection(UtilitatsFirebase.COL_VIATGES).document(viatgeActual.getId());
        DocumentReference refReserva = db.collection(UtilitatsFirebase.COL_RESERVES).document(idReserva);
        DocumentReference refUsuari = db.collection(UtilitatsFirebase.COL_USUARIS).document(usuari.getUid());

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot reservaDoc = transaction.get(refReserva);
            if (reservaDoc.exists()) {
                throw new IllegalStateException("JA_RESERVAT");
            }

            com.google.firebase.firestore.DocumentSnapshot viatgeDoc = transaction.get(refViatge);
            Viatge viatge = viatgeDoc.toObject(Viatge.class);
            if (viatge == null || viatge.getPlacesDisponibles() <= 0) {
                throw new IllegalStateException("SENSE_PLACES");
            }

            com.google.firebase.firestore.DocumentSnapshot usuariDoc = transaction.get(refUsuari);
            String nomPassatger = usuariDoc.contains("nom")
                    ? String.valueOf(usuariDoc.get("nom"))
                    : (usuari.getEmail() == null ? getString(R.string.text_usuari) : usuari.getEmail());

            Map<String, Object> reserva = new HashMap<>();
            reserva.put("viatgeId", viatgeActual.getId());
            reserva.put("conductorId", viatgeActual.getConductorId());
            reserva.put("passatgerId", usuari.getUid());
            reserva.put("passatgerNom", nomPassatger);
            reserva.put("conductorNom", viatgeActual.getConductorNom());
            reserva.put("origen", viatgeActual.getOrigen());
            reserva.put("desti", viatgeActual.getDesti());
            reserva.put("sortidaMillis", viatgeActual.getSortidaMillis());
            reserva.put("estat", UtilitatsFirebase.ESTAT_RESERVA_PENDENT);
            reserva.put("valorada", false);
            reserva.put("puntuacio", 0f);

            transaction.set(refReserva, reserva);
            return true;
        }).addOnSuccessListener(unused ->
                        Toast.makeText(this, R.string.missatge_solicitud_enviada, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    String marca = e.getMessage() == null ? "" : e.getMessage();
                    if (marca.contains("JA_RESERVAT")) {
                        Toast.makeText(this, R.string.missatge_ja_reservat, Toast.LENGTH_SHORT).show();
                    } else if (marca.contains("SENSE_PLACES")) {
                        Toast.makeText(this, R.string.missatge_places_esgotades, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void creaXatIObre(String idXat, String nomXat, String conductorId, String passatgerId) {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null || viatgeActual == null || TextUtils.isEmpty(idXat)) {
            return;
        }

        Map<String, Object> dadesXat = new HashMap<>();
        dadesXat.put("viatgeId", viatgeActual.getId());
        dadesXat.put("conductorId", conductorId);
        dadesXat.put("passatgerId", passatgerId);
        dadesXat.put("nomConductor", viatgeActual.getConductorNom());
        dadesXat.put("origen", viatgeActual.getOrigen());
        dadesXat.put("desti", viatgeActual.getDesti());
        dadesXat.put("sortidaMillis", viatgeActual.getSortidaMillis());
        dadesXat.put("darreraActualitzacio", System.currentTimeMillis());

        db.collection(UtilitatsFirebase.COL_XATS)
                .document(idXat)
                .set(dadesXat, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Intent intent = new Intent(this, XatActivity.class);
                    intent.putExtra(XatActivity.EXTRA_ID_XAT, idXat);
                    intent.putExtra(XatActivity.EXTRA_NOM_XAT, nomXat);
                    startActivity(intent);
                });
    }

    private void obreXatSiExisteix() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null || viatgeActual == null) {
            return;
        }

        String idReserva = viatgeActual.getId() + "_" + usuari.getUid();
        db.collection(UtilitatsFirebase.COL_RESERVES)
                .document(idReserva)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()
                            || !UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA.equals(documentSnapshot.getString("estat"))) {
                        Toast.makeText(this, R.string.missatge_xat_despres_acceptar, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String idXat = UtilitatsFirebase.creaIdXat(viatgeActual.getId(), usuari.getUid());
                    creaXatIObre(idXat, viatgeActual.getConductorNom(), viatgeActual.getConductorId(), usuari.getUid());
                });
    }

    private String valorPerMostrar(String valor) {
        return valor == null || valor.trim().isEmpty() ? getString(R.string.text_no_definit) : valor.trim();
    }
}
