package com.vidalibarraquer.vibacar.activitats;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.adaptadors.AdaptadorReserves;
import com.vidalibarraquer.vibacar.models.Reserva;
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.models.Viatge;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PerfilActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private AdaptadorReserves adaptadorReserves;
    private TextView txtNom;
    private TextView txtDades;
    private TextView txtInicialAvatar;
    private TextView txtBuit;
    private TextView txtTotalValoracions;
    private ShapeableImageView imatgePerfil;
    private RatingBar barraReputacio;
    private MaterialButton botoVeureCotxe;
    private MaterialButton botoVeureInfo;
    private boolean mostraPassats;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txtNom = findViewById(R.id.txtNom);
        txtDades = findViewById(R.id.txtDades);
        txtInicialAvatar = findViewById(R.id.txtInicialAvatar);
        txtBuit = findViewById(R.id.txtBuit);
        txtTotalValoracions = findViewById(R.id.txtTotalValoracions);
        imatgePerfil = findViewById(R.id.imatgePerfil);
        barraReputacio = findViewById(R.id.barraReputacio);
        botoVeureCotxe = findViewById(R.id.botoVeureCotxe);
        botoVeureInfo = findViewById(R.id.botoVeureInfo);

        String uidActual = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        RecyclerView llistaReserves = findViewById(R.id.llistaReserves);
        adaptadorReserves = new AdaptadorReserves(this, uidActual, new AdaptadorReserves.AccionsReservaListener() {
            @Override
            public void onPuntua(Reserva reserva) {
                obreDialegPuntuacio(reserva);
            }

            @Override
            public void onAccepta(Reserva reserva) {}

            @Override
            public void onRebutja(Reserva reserva) {}

            @Override
            public void onCancela(Reserva reserva) {}

            @Override
            public void onObreXat(Reserva reserva) {
                Toast.makeText(PerfilActivity.this, "Obrint xat amb el conductor...", Toast.LENGTH_SHORT).show();
            }
        });
        llistaReserves.setLayoutManager(new LinearLayoutManager(this));
        llistaReserves.setAdapter(adaptadorReserves);

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        findViewById(R.id.botoEditarPerfil).setOnClickListener(v -> startActivity(new Intent(this, ConfiguraPerfilActivity.class)));
        findViewById(R.id.botoTancarSessio).setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(this, PantallaBenvingudaActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.botoProxims).setOnClickListener(v -> {
            mostraPassats = false;
            actualitzaBotonsFiltre();
            carregaLlista();
        });
        findViewById(R.id.botoPassats).setOnClickListener(v -> {
            mostraPassats = true;
            actualitzaBotonsFiltre();
            carregaLlista();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualitzaBotonsFiltre();
        carregaCapcalera();
        carregaLlista();
    }

    private void carregaCapcalera() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            finish();
            return;
        }

        db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(usuari.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Usuari perfil = documentSnapshot.toObject(Usuari.class);
                    if (perfil == null) return;

                    txtNom.setText(perfil.getNom());
                    barraReputacio.setRating((float) perfil.getValoracioMitjana());
                    txtTotalValoracions.setText(String.format(Locale.getDefault(), "(%d)", perfil.getTotalValoracions()));

                    String dadesResum = String.format("%s • %s",
                            perfil.getZona(),
                            UtilitatsFirebase.etiquetaRol(this, perfil.getRol()));
                    txtDades.setText(dadesResum);

                    botoVeureInfo.setOnClickListener(v -> obreDialegInfo(perfil));

                    if (UtilitatsFirebase.esRolConductor(perfil.getRol())) {
                        botoVeureCotxe.setVisibility(View.VISIBLE);
                        botoVeureCotxe.setOnClickListener(v -> obreDialegVehicle(perfil));
                    } else {
                        botoVeureCotxe.setVisibility(View.GONE);
                    }

                    UtilitatsAvatar.mostraAvatar(imatgePerfil, txtInicialAvatar, perfil.getFotoUri(), perfil.getNom());
                });
    }

    private void obreDialegInfo(Usuari perfil) {
        View vista = LayoutInflater.from(this).inflate(R.layout.dialog_perfil_detall, null);
        TextView txtNomDetall = vista.findViewById(R.id.txtNomDetall);
        TextView txtDataNaixementDetall = vista.findViewById(R.id.txtDataNaixementDetall);
        TextView txtSexeDetall = vista.findViewById(R.id.txtSexeDetall);
        TextView txtBioDetall = vista.findViewById(R.id.txtBioDetall);
        TextView txtCorreuDetall = vista.findViewById(R.id.txtCorreuDetall);
        TextView txtTelDetall = vista.findViewById(R.id.txtTelDetall);

        txtNomDetall.setText(perfil.getNom());
        txtDataNaixementDetall.setText(perfil.getDataNaixement() != null && !perfil.getDataNaixement().isEmpty() ? perfil.getDataNaixement() : "-");
        txtSexeDetall.setText(perfil.getSexe() != null && !perfil.getSexe().isEmpty() ? perfil.getSexe() : "-");
        txtBioDetall.setText(perfil.getBio() != null && !perfil.getBio().isEmpty() ? perfil.getBio() : "Sense biografia.");
        txtCorreuDetall.setText(perfil.getCorreu());
        txtTelDetall.setText(perfil.getTelefon() != null && !perfil.getTelefon().isEmpty() ? perfil.getTelefon() : "-");

        new AlertDialog.Builder(this)
                .setView(vista)
                .setPositiveButton("D'acord", null)
                .show();
    }

    private void obreDialegVehicle(Usuari perfil) {
        View vista = LayoutInflater.from(this).inflate(R.layout.dialog_detall_vehicle, null);
        TextView txtModel = vista.findViewById(R.id.txtModelCotxe);
        TextView txtPlaces = vista.findViewById(R.id.txtPlacesCotxe);

        txtModel.setText(perfil.getModelCotxe() != null ? perfil.getModelCotxe() : "-");
        txtPlaces.setText(String.valueOf(perfil.getPlacesHabituals()));

        new AlertDialog.Builder(this)
                .setTitle("El meu vehicle")
                .setView(vista)
                .setPositiveButton("D'acord", null)
                .show();
    }

    private void carregaLlista() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) return;

        List<Reserva> resultat = new ArrayList<>();

        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("passatgerId", usuari.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Reserva reserva = doc.toObject(Reserva.class);
                        reserva.setId(doc.getId());
                        if (filtreData(reserva.getSortidaMillis())) {
                            resultat.add(reserva);
                        }
                    }

                    db.collection(UtilitatsFirebase.COL_VIATGES)
                            .whereEqualTo("conductorId", usuari.getUid())
                            .get()
                            .addOnSuccessListener(viatgesDocs -> {
                                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : viatgesDocs) {
                                    Viatge viatge = doc.toObject(Viatge.class);
                                    Reserva reservaPropia = new Reserva();
                                    reservaPropia.setId("viatge_" + doc.getId());
                                    reservaPropia.setViatgeId(doc.getId());
                                    reservaPropia.setConductorId(usuari.getUid());
                                    reservaPropia.setConductorNom(getString(R.string.nom_marca));
                                    reservaPropia.setOrigen(viatge.getOrigen());
                                    reservaPropia.setDesti(viatge.getDesti());
                                    reservaPropia.setSortidaMillis(viatge.getSortidaMillis());
                                    reservaPropia.setValorada(true);
                                    reservaPropia.setPuntuacio(0f);
                                    if (filtreData(viatge.getSortidaMillis())) {
                                        resultat.add(reservaPropia);
                                    }
                                }

                                resultat.sort(Comparator.comparingLong(Reserva::getSortidaMillis));
                                adaptadorReserves.actualitzaDades(resultat, mostraPassats);
                                txtBuit.setVisibility(resultat.isEmpty() ? View.VISIBLE : View.GONE);
                            });
                });
    }

    private boolean filtreData(long sortidaMillis) {
        boolean esPassat = UtilitatsData.esPassat(sortidaMillis);
        return mostraPassats == esPassat;
    }

    private void actualitzaBotonsFiltre() {
        MaterialButton botoProxims = findViewById(R.id.botoProxims);
        MaterialButton botoPassats = findViewById(R.id.botoPassats);

        if (mostraPassats) {
            botoPassats.setBackgroundResource(R.drawable.fons_boto_principal);
            botoPassats.setTextColor(getColor(R.color.color_text_clar));
            botoProxims.setBackgroundResource(android.R.color.transparent);
            botoProxims.setStrokeColorResource(R.color.color_principal);
            botoProxims.setTextColor(getColor(R.color.color_principal));
        } else {
            botoProxims.setBackgroundResource(R.drawable.fons_boto_principal);
            botoProxims.setTextColor(getColor(R.color.color_text_clar));
            botoPassats.setBackgroundResource(android.R.color.transparent);
            botoPassats.setStrokeColorResource(R.color.color_principal);
            botoPassats.setTextColor(getColor(R.color.color_principal));
        }
    }

    private void obreDialegPuntuacio(Reserva reserva) {
        if (reserva.getId().startsWith("viatge_")) return;

        View vista = LayoutInflater.from(this).inflate(R.layout.dialog_puntuacio, null, false);
        RatingBar barraPuntuacio = vista.findViewById(R.id.barraPuntuacio);
        com.google.android.material.textfield.TextInputEditText campComentari = vista.findViewById(R.id.campComentari);

        new AlertDialog.Builder(this)
                .setTitle(R.string.boto_puntuar)
                .setView(vista)
                .setPositiveButton(R.string.boto_puntuar, (dialog, which) -> {
                    String comentari = campComentari.getText() != null ? campComentari.getText().toString() : "";
                    desaPuntuacio(reserva, barraPuntuacio.getRating(), comentari);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void desaPuntuacio(Reserva reserva, float puntuacio, String comentari) {
        if (puntuacio <= 0f) {
            Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference reservaRef = db.collection(UtilitatsFirebase.COL_RESERVES).document(reserva.getId());
        DocumentReference conductorRef = db.collection(UtilitatsFirebase.COL_USUARIS).document(reserva.getConductorId());

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot conductorDoc = transaction.get(conductorRef);
            long totalValoracions = conductorDoc.contains("totalValoracions")
                    ? (conductorDoc.getLong("totalValoracions") != null ? conductorDoc.getLong("totalValoracions") : 0L)
                    : 0L;
            double valoracioMitjana = conductorDoc.contains("valoracioMitjana")
                    ? (conductorDoc.getDouble("valoracioMitjana") != null ? conductorDoc.getDouble("valoracioMitjana") : 0d)
                    : 0d;

            long nouTotal = totalValoracions + 1;
            double novaMitjana = ((valoracioMitjana * totalValoracions) + puntuacio) / nouTotal;

            transaction.update(reservaRef, "valorada", true, "puntuacio", puntuacio, "comentari", comentari);

            Map<String, Object> dadesConductor = new HashMap<>();
            dadesConductor.put("totalValoracions", nouTotal);
            dadesConductor.put("valoracioMitjana", novaMitjana);
            transaction.set(conductorRef, dadesConductor, SetOptions.merge());
            return true;
        }).addOnSuccessListener(unused -> {
            Toast.makeText(this, R.string.missatge_puntuacio_guardada, Toast.LENGTH_SHORT).show();
            carregaCapcalera();
            carregaLlista();
        }).addOnFailureListener(e -> Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show());
    }
}
