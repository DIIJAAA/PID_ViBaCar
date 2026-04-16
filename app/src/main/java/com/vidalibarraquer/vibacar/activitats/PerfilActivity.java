package com.vidalibarraquer.vibacar.activitats;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
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
import com.vidalibarraquer.vibacar.utilitats.GestorIdioma;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerfilActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private AdaptadorReserves adaptadorReserves;
    private TextView txtNom;
    private TextView txtDades;
    private TextView txtInicialAvatar;
    private TextView txtBuit;
    private ShapeableImageView imatgePerfil;
    private MaterialAutoCompleteTextView campIdioma;
    private boolean mostraPassats;
    private String[] nomsIdiomes;
    private String[] codisIdiomes;

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
        imatgePerfil = findViewById(R.id.imatgePerfil);
        campIdioma = findViewById(R.id.campIdioma);

        RecyclerView llistaReserves = findViewById(R.id.llistaReserves);
        adaptadorReserves = new AdaptadorReserves(this, this::obreDialegPuntuacio);
        llistaReserves.setLayoutManager(new LinearLayoutManager(this));
        llistaReserves.setAdapter(adaptadorReserves);

        nomsIdiomes = getResources().getStringArray(R.array.idiomes_llista);
        codisIdiomes = getResources().getStringArray(R.array.idiomes_codis);
        configuraSelectorIdiomes();

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

    private void configuraSelectorIdiomes() {
        campIdioma.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nomsIdiomes));
        String idiomaActual = GestorIdioma.obteIdiomaGuardat(this);
        int posicio = 0;
        for (int i = 0; i < codisIdiomes.length; i++) {
            if (codisIdiomes[i].equals(idiomaActual)) {
                posicio = i;
                break;
            }
        }
        campIdioma.setText(nomsIdiomes[posicio], false);
        campIdioma.setOnItemClickListener((parent, view, position, id) -> {
            GestorIdioma.guardaIAplica(this, codisIdiomes[position]);
            FirebaseUser usuari = auth.getCurrentUser();
            if (usuari != null) {
                Map<String, Object> dades = new HashMap<>();
                dades.put("idioma", codisIdiomes[position]);
                db.collection(UtilitatsFirebase.COL_USUARIS).document(usuari.getUid()).set(dades, SetOptions.merge());
            }
            recreate();
        });
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
                    if (perfil == null) {
                        return;
                    }
                    txtNom.setText(perfil.getNom());
                    txtDades.setText(getString(
                            R.string.text_resum_perfil_format,
                            perfil.getCorreu(),
                            UtilitatsFirebase.etiquetaRol(this, perfil.getRol()),
                            perfil.getZona(),
                            perfil.getTelefon() == null ? "-" : perfil.getTelefon()
                    ));
                    txtDades.append("\n");
                    txtDades.append(getString(
                            R.string.text_resum_mobilitat_format,
                            valorPerMostrar(perfil.getHoraSortidaHabitual()),
                            valorPerMostrar(perfil.getPuntTrobadaHabitual())
                    ));
                    if (UtilitatsFirebase.esRolConductor(perfil.getRol())) {
                        txtDades.append("\n");
                        txtDades.append(getString(
                                R.string.text_resum_conductor_format,
                                valorPerMostrar(perfil.getModelCotxe()),
                                perfil.getPlacesHabituals() > 0 ? String.valueOf(perfil.getPlacesHabituals()) : "-"
                        ));
                    }
                    UtilitatsAvatar.mostraAvatar(imatgePerfil, txtInicialAvatar, perfil.getFotoUri(), perfil.getNom());
                });
    }

    private void carregaLlista() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            return;
        }

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
                                txtBuit.setVisibility(resultat.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                            });
                });
    }

    private boolean filtreData(long sortidaMillis) {
        boolean esPassat = UtilitatsData.esPassat(sortidaMillis);
        return mostraPassats == esPassat;
    }

    private void actualitzaBotonsFiltre() {
        com.google.android.material.button.MaterialButton botoProxims = findViewById(R.id.botoProxims);
        com.google.android.material.button.MaterialButton botoPassats = findViewById(R.id.botoPassats);

        if (mostraPassats) {
            botoPassats.setBackgroundResource(R.drawable.fons_boto_principal);
            botoPassats.setTextColor(getColor(R.color.color_text_clar));
            botoProxims.setBackgroundResource(android.R.color.transparent);
            botoProxims.setStrokeColorResource(R.color.color_principal);
        } else {
            botoProxims.setBackgroundResource(R.drawable.fons_boto_principal);
            botoProxims.setTextColor(getColor(R.color.color_text_clar));
            botoPassats.setBackgroundResource(android.R.color.transparent);
            botoPassats.setStrokeColorResource(R.color.color_principal);
        }
    }

    private void obreDialegPuntuacio(Reserva reserva) {
        if (reserva.getId().startsWith("viatge_")) {
            return;
        }

        android.view.View vista = LayoutInflater.from(this).inflate(R.layout.dialog_puntuacio, null, false);
        RatingBar barraPuntuacio = vista.findViewById(R.id.barraPuntuacio);

        new AlertDialog.Builder(this)
                .setView(vista)
                .setPositiveButton(R.string.boto_puntuar, (dialog, which) -> desaPuntuacio(reserva, barraPuntuacio.getRating()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void desaPuntuacio(Reserva reserva, float puntuacio) {
        if (puntuacio <= 0f) {
            Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference reservaRef = db.collection(UtilitatsFirebase.COL_RESERVES).document(reserva.getId());
        DocumentReference conductorRef = db.collection(UtilitatsFirebase.COL_USUARIS).document(reserva.getConductorId());

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot conductorDoc = transaction.get(conductorRef);
            long totalValoracions = conductorDoc.contains("totalValoracions")
                    ? conductorDoc.getLong("totalValoracions")
                    : 0L;
            double valoracioMitjana = conductorDoc.contains("valoracioMitjana")
                    ? conductorDoc.getDouble("valoracioMitjana")
                    : 0d;

            long nouTotal = totalValoracions + 1;
            double novaMitjana = ((valoracioMitjana * totalValoracions) + puntuacio) / nouTotal;

            transaction.update(reservaRef, "valorada", true, "puntuacio", puntuacio);

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

    private String valorPerMostrar(String valor) {
        return valor == null || valor.trim().isEmpty() ? "-" : valor.trim();
    }
}
