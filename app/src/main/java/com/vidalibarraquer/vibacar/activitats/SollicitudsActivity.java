package com.vidalibarraquer.vibacar.activitats;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.adaptadors.AdaptadorReserves;
import com.vidalibarraquer.vibacar.models.Notificacio;
import com.vidalibarraquer.vibacar.models.Reserva;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsNotificacions;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsXats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SollicitudsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private AdaptadorReserves adaptador;
    private TextView txtBuit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sollicituds);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        txtBuit = findViewById(R.id.txtBuit);

        FirebaseUser usuari = auth.getCurrentUser();
        String uidActual = usuari != null ? usuari.getUid() : "";

        RecyclerView llista = findViewById(R.id.llistaSollicituds);
        adaptador = new AdaptadorReserves(this, uidActual, new AdaptadorReserves.AccionsReservaListener() {
            @Override
            public void onPuntua(Reserva reserva) {}

            @Override
            public void onAccepta(Reserva reserva) {
                acceptaReserva(reserva);
            }

            @Override
            public void onRebutja(Reserva reserva) {
                rebutjaReserva(reserva);
            }

            @Override
            public void onCancela(Reserva reserva) {}

            @Override
            public void onObreXat(Reserva reserva) {}

            @Override
            public void onVeurePerfil(Reserva reserva) {
                if (TextUtils.isEmpty(reserva.getPassatgerId())) return;
                Intent intent = new Intent(SollicitudsActivity.this, VeurePerfilActivity.class);
                intent.putExtra(VeurePerfilActivity.EXTRA_UID, reserva.getPassatgerId());
                startActivity(intent);
            }
        });
        llista.setLayoutManager(new LinearLayoutManager(this));
        llista.setAdapter(adaptador);

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregaSollicituds();
    }

    private void carregaSollicituds() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            finish();
            return;
        }

        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("conductorId", usuari.getUid())
                .whereEqualTo("estat", UtilitatsFirebase.ESTAT_RESERVA_PENDENT)
                .get()
                .addOnSuccessListener(docs -> {
                    List<Reserva> pendents = new ArrayList<>();
                    if (docs.isEmpty()) {
                        mostraResultat(pendents);
                        return;
                    }

                    final int[] pendentsValidacio = {docs.size()};
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : docs) {
                        Reserva reserva = doc.toObject(Reserva.class);
                        reserva.setId(doc.getId());
                        validaReservaAmbViatge(reserva, pendents, () -> {
                            pendentsValidacio[0]--;
                            if (pendentsValidacio[0] == 0) {
                                mostraResultat(pendents);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> mostraResultat(new ArrayList<>()));
    }

    private void validaReservaAmbViatge(Reserva reserva, List<Reserva> pendents, Runnable finalitzat) {
        if (TextUtils.isEmpty(reserva.getViatgeId())) {
            finalitzat.run();
            return;
        }

        db.collection(UtilitatsFirebase.COL_VIATGES)
                .document(reserva.getViatgeId())
                .get()
                .addOnSuccessListener(viatge -> {
                    String estat = viatge.getString("estat");
                    boolean viatgeActiu = viatge.exists()
                            && UtilitatsFirebase.ESTAT_VIATGE_DISPONIBLE.equals(estat);
                    if (viatgeActiu) {
                        pendents.add(reserva);
                    } else {
                        marcaReservaCancelada(reserva);
                    }
                    finalitzat.run();
                })
                .addOnFailureListener(e -> finalitzat.run());
    }

    private void mostraResultat(List<Reserva> pendents) {
        pendents.sort(Comparator.comparingLong(Reserva::getSortidaMillis));
        adaptador.actualitzaDades(pendents, false);
        txtBuit.setVisibility(pendents.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void acceptaReserva(Reserva reserva) {
        if (reserva == null || TextUtils.isEmpty(reserva.getId()) || TextUtils.isEmpty(reserva.getViatgeId())) {
            return;
        }

        DocumentReference reservaRef = db.collection(UtilitatsFirebase.COL_RESERVES).document(reserva.getId());
        DocumentReference viatgeRef = db.collection(UtilitatsFirebase.COL_VIATGES).document(reserva.getViatgeId());

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot reservaDoc = transaction.get(reservaRef);
            com.google.firebase.firestore.DocumentSnapshot viatgeDoc = transaction.get(viatgeRef);

            if (!reservaDoc.exists() || !viatgeDoc.exists()) {
                throw new IllegalStateException("Reserva o viatge inexistent");
            }
            String estatReserva = reservaDoc.getString("estat");
            String estatViatge = viatgeDoc.getString("estat");
            Long places = viatgeDoc.getLong("placesDisponibles");
            if (!UtilitatsFirebase.ESTAT_RESERVA_PENDENT.equals(estatReserva)
                    || !UtilitatsFirebase.ESTAT_VIATGE_DISPONIBLE.equals(estatViatge)
                    || places == null
                    || places <= 0) {
                throw new IllegalStateException("Reserva no disponible");
            }

            transaction.update(reservaRef,
                    "estat", UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA,
                    "acceptadaMillis", System.currentTimeMillis());
            transaction.update(viatgeRef, "placesDisponibles", FieldValue.increment(-1));
            return true;
        }).addOnSuccessListener(unused -> {
            preparaXatINotifica(reserva);
            Toast.makeText(this, R.string.missatge_reserva_acceptada, Toast.LENGTH_SHORT).show();
            carregaSollicituds();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show();
            carregaSollicituds();
        });
    }

    private void rebutjaReserva(Reserva reserva) {
        if (reserva == null || TextUtils.isEmpty(reserva.getId())) return;

        Map<String, Object> dades = new HashMap<>();
        dades.put("estat", UtilitatsFirebase.ESTAT_RESERVA_REBUTJADA);
        dades.put("rebutjadaMillis", System.currentTimeMillis());

        db.collection(UtilitatsFirebase.COL_RESERVES)
                .document(reserva.getId())
                .set(dades, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    UtilitatsNotificacions.publica(
                            db,
                            reserva.getPassatgerId(),
                            Notificacio.TIPUS_RESERVA_REBUTJADA,
                            getString(R.string.notif_reserva_rebutjada, reserva.getOrigen(), reserva.getDesti()),
                            reserva.getViatgeId()
                    );
                    Toast.makeText(this, R.string.missatge_reserva_rebutjada, Toast.LENGTH_SHORT).show();
                    carregaSollicituds();
                })
                .addOnFailureListener(e -> Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show());
    }

    private void marcaReservaCancelada(Reserva reserva) {
        if (TextUtils.isEmpty(reserva.getId())) return;
        Map<String, Object> dades = new HashMap<>();
        dades.put("estat", UtilitatsFirebase.ESTAT_RESERVA_CANCELADA);
        dades.put("canceladaMillis", System.currentTimeMillis());
        db.collection(UtilitatsFirebase.COL_RESERVES).document(reserva.getId()).set(dades, SetOptions.merge());
    }

    private void preparaXatINotifica(Reserva reserva) {
        Map<String, Object> dadesXat = new HashMap<>();
        dadesXat.put("viatgeId", reserva.getViatgeId());
        dadesXat.put("conductorId", reserva.getConductorId());
        dadesXat.put("passatgerId", reserva.getPassatgerId());
        dadesXat.put("nomConductor", reserva.getConductorNom());
        dadesXat.put("nomPassatger", reserva.getPassatgerNom());
        dadesXat.put("origen", reserva.getOrigen());
        dadesXat.put("desti", reserva.getDesti());
        dadesXat.put("sortidaMillis", reserva.getSortidaMillis());

        UtilitatsXats.preparaXatEntreUsuaris(
                db,
                reserva.getConductorId(),
                reserva.getPassatgerId(),
                reserva.getId(),
                dadesXat,
                new UtilitatsXats.Callback() {
                    @Override
                    public void onPreparat(String idXat) {
                        UtilitatsNotificacions.publica(
                                db,
                                reserva.getPassatgerId(),
                                Notificacio.TIPUS_RESERVA_ACCEPTADA,
                                getString(R.string.notif_reserva_acceptada,
                                        reserva.getOrigen(), reserva.getDesti()),
                                idXat
                        );
                    }

                    @Override
                    public void onError(Exception e) {
                        UtilitatsNotificacions.publica(
                                db,
                                reserva.getPassatgerId(),
                                Notificacio.TIPUS_RESERVA_ACCEPTADA,
                                getString(R.string.notif_reserva_acceptada,
                                        reserva.getOrigen(), reserva.getDesti()),
                                UtilitatsFirebase.creaIdXatUsuaris(
                                        reserva.getConductorId(), reserva.getPassatgerId())
                        );
                    }
                });
    }
}
