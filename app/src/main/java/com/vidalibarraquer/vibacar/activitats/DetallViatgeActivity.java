package com.vidalibarraquer.vibacar.activitats;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.firebase.firestore.DocumentReference;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Viatge;
import com.vidalibarraquer.vibacar.utilitats.PuntsMapaViBaCar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsMapa;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsNotificacions;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsXats;
import com.vidalibarraquer.vibacar.models.Notificacio;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
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
    private MaterialButton botoEliminarViatge;
    private LinearLayout seccioConductor;
    private LinearLayout blocEstatReserva;
    private TextView txtEstatReservaTitol;
    private TextView txtEstatReservaDescripcio;

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
        botoEliminarViatge = findViewById(R.id.botoEliminarViatge);
        seccioConductor = findViewById(R.id.seccioConductor);
        blocEstatReserva = findViewById(R.id.blocEstatReserva);
        txtEstatReservaTitol = findViewById(R.id.txtEstatReservaTitol);
        txtEstatReservaDescripcio = findViewById(R.id.txtEstatReservaDescripcio);

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
        botoEliminarViatge.setOnClickListener(v -> confirmaEliminacio());
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
        if (!TextUtils.isEmpty(viatgeActual.getModelCotxeConductor())) {
            txtInfoGeneral.append("\n");
            String cotxe = viatgeActual.getModelCotxeConductor();
            if (!TextUtils.isEmpty(viatgeActual.getColorCotxeConductor())) {
                cotxe = cotxe + " · " + viatgeActual.getColorCotxeConductor();
            }
            txtInfoGeneral.append(getString(R.string.text_model_cotxe_format, cotxe));
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
        blocEstatReserva.setVisibility(View.GONE);
        botoReservar.setEnabled(true);
        botoReservar.setAlpha(1f);
        botoReservar.setText(R.string.boto_reservar_placa);
        botoObrirXat.setEnabled(false);
        botoObrirXat.setAlpha(0.4f);

        boolean esConductorDelViatge = usuari != null
                && viatgeActual != null
                && usuari.getUid().equals(viatgeActual.getConductorId());

        if (esConductorDelViatge) {
            botoReservar.setVisibility(View.GONE);
            botoObrirXat.setVisibility(View.GONE);
            botoEliminarViatge.setVisibility(View.VISIBLE);
            seccioConductor.setClickable(false);
        } else {
            botoReservar.setVisibility(View.VISIBLE);
            botoObrirXat.setVisibility(View.VISIBLE);
            botoEliminarViatge.setVisibility(View.GONE);
            seccioConductor.setOnClickListener(v -> {
                if (viatgeActual == null) return;
                Intent intent = new Intent(this, VeurePerfilActivity.class);
                intent.putExtra(VeurePerfilActivity.EXTRA_UID, viatgeActual.getConductorId());
                startActivity(intent);
            });
            if (usuari != null && viatgeActual != null) {
                String idReserva = UtilitatsFirebase.creaIdReserva(viatgeActual.getId(), usuari.getUid());
                db.collection(UtilitatsFirebase.COL_RESERVES).document(idReserva).get()
                        .addOnSuccessListener(doc -> {
                            if (!doc.exists()) {
                                return;
                            }
                            mostraEstatReserva(doc.getString("estat"));
                        });
            }
        }
    }

    private void mostraEstatReserva(String estat) {
        if (TextUtils.isEmpty(estat)) {
            blocEstatReserva.setVisibility(View.GONE);
            return;
        }

        blocEstatReserva.setVisibility(View.VISIBLE);
        botoReservar.setEnabled(false);
        botoReservar.setAlpha(0.5f);

        if (UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA.equals(estat)) {
            txtEstatReservaTitol.setText(R.string.reserva_estat_titol_acceptada);
            txtEstatReservaDescripcio.setText(R.string.reserva_estat_descripcio_acceptada);
            botoReservar.setText(R.string.estat_acceptada);
            botoReservar.setVisibility(View.GONE);
            botoObrirXat.setEnabled(true);
            botoObrirXat.setAlpha(1f);
            return;
        }

        if (UtilitatsFirebase.ESTAT_RESERVA_REBUTJADA.equals(estat)) {
            txtEstatReservaTitol.setText(R.string.reserva_estat_titol_rebutjada);
            txtEstatReservaDescripcio.setText(R.string.reserva_estat_descripcio_rebutjada);
            botoReservar.setText(R.string.estat_rebutjada);
            botoReservar.setVisibility(View.GONE);
            return;
        }

        if (UtilitatsFirebase.ESTAT_RESERVA_CANCELADA.equals(estat)) {
            txtEstatReservaTitol.setText(R.string.reserva_estat_titol_cancelada);
            txtEstatReservaDescripcio.setText(R.string.reserva_estat_descripcio_cancelada);
            botoReservar.setText(R.string.estat_cancelada);
            botoReservar.setVisibility(View.GONE);
            return;
        }

        txtEstatReservaTitol.setText(R.string.reserva_estat_titol_pendent);
        txtEstatReservaDescripcio.setText(R.string.reserva_estat_descripcio_pendent);
        botoReservar.setText(R.string.estat_pendent);
        botoReservar.setVisibility(View.VISIBLE);
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
        if (viatgeActual.getPlacesDisponibles() <= 0) {
            Toast.makeText(this, R.string.missatge_places_esgotades, Toast.LENGTH_SHORT).show();
            return;
        }

        botoReservar.setEnabled(false);
        final String emailFallback = usuari.getEmail() != null ? usuari.getEmail() : getString(R.string.text_usuari);

        db.collection(UtilitatsFirebase.COL_USUARIS).document(usuari.getUid()).get()
                .addOnSuccessListener(usuariDoc -> {
                    String nomPassatger = usuariDoc.contains("nom")
                            ? String.valueOf(usuariDoc.get("nom"))
                            : emailFallback;
                    String fotoPassatger = usuariDoc.getString("fotoUri");
                    creaReserva(usuari.getUid(), nomPassatger, fotoPassatger);
                })
                .addOnFailureListener(e -> creaReserva(usuari.getUid(), emailFallback, null));
    }

    private void creaReserva(String passatgerId, String nomPassatger, @Nullable String fotoPassatger) {
        if (viatgeActual == null) return;

        String idReserva = viatgeActual.getId() + "_" + passatgerId;
        DocumentReference refReserva = db.collection(UtilitatsFirebase.COL_RESERVES).document(idReserva);

        Map<String, Object> reserva = new HashMap<>();
        reserva.put("viatgeId", viatgeActual.getId());
        reserva.put("conductorId", viatgeActual.getConductorId());
        reserva.put("passatgerId", passatgerId);
        reserva.put("passatgerNom", nomPassatger);
        reserva.put("passatgerFotoUri", fotoPassatger);
        reserva.put("conductorNom", viatgeActual.getConductorNom());
        reserva.put("conductorFotoUri", viatgeActual.getConductorFotoUri());
        reserva.put("origen", viatgeActual.getOrigen());
        reserva.put("desti", viatgeActual.getDesti());
        reserva.put("sortidaMillis", viatgeActual.getSortidaMillis());
        reserva.put("estat", UtilitatsFirebase.ESTAT_RESERVA_PENDENT);
        reserva.put("valorada", false);
        reserva.put("puntuacio", 0f);

        refReserva.set(reserva)
                .addOnSuccessListener(unused -> {
                    botoReservar.setEnabled(true);
                    mostraEstatReserva(UtilitatsFirebase.ESTAT_RESERVA_PENDENT);
                    Toast.makeText(this, R.string.missatge_solicitud_enviada, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    botoReservar.setEnabled(true);
                    Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show();
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
        dadesXat.put("nomPassatger", TextUtils.isEmpty(usuari.getDisplayName())
                ? getString(R.string.text_usuari)
                : usuari.getDisplayName());
        dadesXat.put("origen", viatgeActual.getOrigen());
        dadesXat.put("desti", viatgeActual.getDesti());
        dadesXat.put("sortidaMillis", viatgeActual.getSortidaMillis());

        UtilitatsXats.preparaXatEntreUsuaris(
                db,
                conductorId,
                passatgerId,
                idXat,
                dadesXat,
                new UtilitatsXats.Callback() {
                    @Override
                    public void onPreparat(String idPreparat) {
                        Intent intent = new Intent(DetallViatgeActivity.this, XatActivity.class);
                        intent.putExtra(XatActivity.EXTRA_ID_XAT, idPreparat);
                        intent.putExtra(XatActivity.EXTRA_NOM_XAT, nomXat);
                        intent.putExtra(XatActivity.EXTRA_UID_ALTRE, conductorId);
                        startActivity(intent);
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(DetallViatgeActivity.this, R.string.error_generica, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void obreXatSiExisteix() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null || viatgeActual == null) {
            return;
        }

        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("passatgerId", usuari.getUid())
                .whereEqualTo("conductorId", viatgeActual.getConductorId())
                .whereEqualTo("estat", UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA)
                .get()
                .addOnSuccessListener(reserves -> {
                    if (reserves.isEmpty()) {
                        Toast.makeText(this, R.string.missatge_xat_despres_acceptar, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String idLlegat = reserves.getDocuments().get(0).getId();
                    creaXatIObre(idLlegat, viatgeActual.getConductorNom(), viatgeActual.getConductorId(), usuari.getUid());
                })
                .addOnFailureListener(e -> Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show());
    }

    private void confirmaEliminacio() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirma_eliminar_viatge)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> eliminaViatge())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void eliminaViatge() {
        if (viatgeActual == null) return;
        botoEliminarViatge.setEnabled(false);

        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("viatgeId", viatgeActual.getId())
                .get()
                .addOnSuccessListener(reserves -> {
                    WriteBatch batch = db.batch();
                    List<String> passatgersPerNotificar = new ArrayList<>();

                    Map<String, Object> dadesViatge = new HashMap<>();
                    dadesViatge.put("estat", UtilitatsFirebase.ESTAT_VIATGE_CANCELAT);
                    dadesViatge.put("placesDisponibles", 0);
                    dadesViatge.put("cancelatMillis", System.currentTimeMillis());
                    batch.set(
                            db.collection(UtilitatsFirebase.COL_VIATGES).document(viatgeActual.getId()),
                            dadesViatge,
                            SetOptions.merge()
                    );

                    for (QueryDocumentSnapshot reservaDoc : reserves) {
                        String estat = reservaDoc.getString("estat");
                        if (!UtilitatsFirebase.esReservaActiva(estat)) {
                            continue;
                        }
                        String passatgerId = reservaDoc.getString("passatgerId");
                        if (!TextUtils.isEmpty(passatgerId) && !passatgersPerNotificar.contains(passatgerId)) {
                            passatgersPerNotificar.add(passatgerId);
                        }

                        Map<String, Object> dadesReserva = new HashMap<>();
                        dadesReserva.put("estat", UtilitatsFirebase.ESTAT_RESERVA_CANCELADA);
                        dadesReserva.put("canceladaMillis", System.currentTimeMillis());
                        batch.set(reservaDoc.getReference(), dadesReserva, SetOptions.merge());
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                notificaCancelacioAlsPassatgers(passatgersPerNotificar);
                                Toast.makeText(this, R.string.missatge_viatge_eliminat, Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                eliminaViatgeSenseReserves();
                            });
                })
                .addOnFailureListener(e -> {
                    eliminaViatgeSenseReserves();
                });
    }

    private void eliminaViatgeSenseReserves() {
        Map<String, Object> dadesViatge = new HashMap<>();
        dadesViatge.put("estat", UtilitatsFirebase.ESTAT_VIATGE_CANCELAT);
        dadesViatge.put("placesDisponibles", 0);
        dadesViatge.put("cancelatMillis", System.currentTimeMillis());

        db.collection(UtilitatsFirebase.COL_VIATGES)
                .document(viatgeActual.getId())
                .set(dadesViatge, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, R.string.missatge_viatge_eliminat, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    botoEliminarViatge.setEnabled(true);
                    Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show();
                });
    }

    private void notificaCancelacioAlsPassatgers(List<String> passatgers) {
        String text = getString(
                R.string.notif_viatge_cancellat_cos,
                valorPerMostrar(viatgeActual.getOrigen()),
                valorPerMostrar(viatgeActual.getDesti()),
                UtilitatsData.formatData(viatgeActual.getSortidaMillis())
        );

        for (String passatgerId : passatgers) {
            UtilitatsNotificacions.publica(
                    db,
                    passatgerId,
                    Notificacio.TIPUS_VIATGE_CANCELLAT,
                    text,
                    viatgeActual.getId()
            );
        }
    }

    private String valorPerMostrar(String valor) {
        return valor == null || valor.trim().isEmpty() ? getString(R.string.text_no_definit) : valor.trim();
    }
}
