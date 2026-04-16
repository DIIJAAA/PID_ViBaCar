package com.vidalibarraquer.vibacar.activitats;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Viatge;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

import java.util.HashMap;
import java.util.Map;

public class DetallViatgeActivity extends AppCompatActivity {

    public static final String EXTRA_ID_VIATGE = "id_viatge";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String idViatge;
    private Viatge viatgeActual;
    private TextView txtNomConductor;
    private TextView txtValoracioConductor;
    private TextView txtRuta;
    private TextView txtInfoGeneral;
    private TextView txtObservacions;
    private TextView txtInicialConductor;
    private ShapeableImageView imatgeConductor;

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
        imatgeConductor = findViewById(R.id.imatgeConductor);

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        findViewById(R.id.botoReservar).setOnClickListener(v -> reservaViatge());
        findViewById(R.id.botoObrirXat).setOnClickListener(v -> obreXatSiExisteix());
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregaViatge();
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

            Map<String, Object> reserva = new HashMap<>();
            reserva.put("viatgeId", viatgeActual.getId());
            reserva.put("conductorId", viatgeActual.getConductorId());
            reserva.put("passatgerId", usuari.getUid());
            reserva.put("conductorNom", viatgeActual.getConductorNom());
            reserva.put("origen", viatgeActual.getOrigen());
            reserva.put("desti", viatgeActual.getDesti());
            reserva.put("sortidaMillis", viatgeActual.getSortidaMillis());
            reserva.put("estat", "confirmada");
            reserva.put("valorada", false);
            reserva.put("puntuacio", 0f);

            transaction.set(refReserva, reserva);
            transaction.update(refViatge, "placesDisponibles", viatge.getPlacesDisponibles() - 1);
            return true;
        }).addOnSuccessListener(unused -> creaXatIObre())
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

    private void creaXatIObre() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null || viatgeActual == null) {
            return;
        }

        String idXat = UtilitatsFirebase.creaIdXat(viatgeActual.getId(), usuari.getUid());
        Map<String, Object> dadesXat = new HashMap<>();
        dadesXat.put("viatgeId", viatgeActual.getId());
        dadesXat.put("conductorId", viatgeActual.getConductorId());
        dadesXat.put("passatgerId", usuari.getUid());
        dadesXat.put("nomConductor", viatgeActual.getConductorNom());
        dadesXat.put("darreraActualitzacio", System.currentTimeMillis());

        db.collection(UtilitatsFirebase.COL_XATS)
                .document(idXat)
                .set(dadesXat, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Intent intent = new Intent(this, XatActivity.class);
                    intent.putExtra(XatActivity.EXTRA_ID_XAT, idXat);
                    intent.putExtra(XatActivity.EXTRA_NOM_XAT, viatgeActual.getConductorNom());
                    startActivity(intent);
                    Toast.makeText(this, R.string.missatge_reserva_feta, Toast.LENGTH_SHORT).show();
                });
    }

    private void obreXatSiExisteix() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null || viatgeActual == null) {
            return;
        }

        String idXat = UtilitatsFirebase.creaIdXat(viatgeActual.getId(), usuari.getUid());
        db.collection(UtilitatsFirebase.COL_XATS)
                .document(idXat)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, R.string.missatge_primer_reserva, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Intent intent = new Intent(this, XatActivity.class);
                    intent.putExtra(XatActivity.EXTRA_ID_XAT, idXat);
                    intent.putExtra(XatActivity.EXTRA_NOM_XAT, viatgeActual.getConductorNom());
                    startActivity(intent);
                });
    }
}
