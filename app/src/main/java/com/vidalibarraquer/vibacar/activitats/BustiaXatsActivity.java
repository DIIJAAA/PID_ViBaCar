package com.vidalibarraquer.vibacar.activitats;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.adaptadors.AdaptadorXats;
import com.vidalibarraquer.vibacar.models.Xat;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsBottomNav;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BustiaXatsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private AdaptadorXats adaptadorXats;
    private TextView txtBuit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bustia_xats);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            finish();
            return;
        }

        txtBuit = findViewById(R.id.txtBuit);

        RecyclerView llistaXats = findViewById(R.id.llistaXats);
        String uidActual = usuari.getUid();
        adaptadorXats = new AdaptadorXats(this, uidActual, this::obreXat, uid -> {
            Intent intent = new Intent(this, VeurePerfilActivity.class);
            intent.putExtra(VeurePerfilActivity.EXTRA_UID, uid);
            startActivity(intent);
        });
        llistaXats.setLayoutManager(new LinearLayoutManager(this));
        llistaXats.setAdapter(adaptadorXats);

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        configuraBotomNav();
    }

    private void configuraBotomNav() {
        UtilitatsBottomNav.marcaSeleccionada(this, UtilitatsBottomNav.SECCIO_MISSATGES);

        View navBuscar = findViewById(R.id.navBuscar);
        View navNotificacions = findViewById(R.id.navNotificacions);

        if (navBuscar != null) {
            navBuscar.setOnClickListener(v -> {
                startActivity(new Intent(this, PantallaPassatgerActivity.class));
                finish();
            });
        }
        if (navNotificacions != null) {
            navNotificacions.setOnClickListener(v -> {
                startActivity(new Intent(this, NotificacionsActivity.class));
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        UtilitatsBottomNav.actualitzaBadgeNotificacions(this);
        carregaXats();
    }

    private void carregaXats() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            return;
        }
        String uid = usuari.getUid();
        Map<String, Xat> indexXats = new LinkedHashMap<>();

        db.collection(UtilitatsFirebase.COL_XATS)
                .whereEqualTo("conductorId", uid)
                .get()
                .addOnSuccessListener(conductorDocs -> {
                    for (QueryDocumentSnapshot doc : conductorDocs) {
                        Xat xat = doc.toObject(Xat.class);
                        xat.setId(doc.getId());
                        afegeixXatDeduplicat(indexXats, xat);
                    }

                    db.collection(UtilitatsFirebase.COL_XATS)
                            .whereEqualTo("passatgerId", uid)
                            .get()
                            .addOnSuccessListener(passatgerDocs -> {
                                for (QueryDocumentSnapshot doc : passatgerDocs) {
                                    Xat xat = doc.toObject(Xat.class);
                                    xat.setId(doc.getId());
                                    afegeixXatDeduplicat(indexXats, xat);
                                }

                                List<Xat> resultat = new ArrayList<>(indexXats.values());
                                resultat.sort(Comparator.comparingLong(Xat::getDarreraActualitzacio).reversed());
                                mostraResultat(resultat, uid);
                            })
                            .addOnFailureListener(e -> mostraLlista(indexXats));
                })
                .addOnFailureListener(e -> mostraLlista(indexXats));
    }

    private void mostraLlista(Map<String, Xat> indexXats) {
        List<Xat> resultat = new ArrayList<>(indexXats.values());
        resultat.sort(Comparator.comparingLong(Xat::getDarreraActualitzacio).reversed());
        FirebaseUser usuari = auth.getCurrentUser();
        mostraResultat(resultat, usuari == null ? "" : usuari.getUid());
    }

    private void mostraResultat(List<Xat> resultat, String uidActual) {
        adaptadorXats.actualitzaDades(resultat);
        txtBuit.setVisibility(resultat.isEmpty() ? View.VISIBLE : View.GONE);
        completaNomsXats(resultat, uidActual);
    }

    private void completaNomsXats(List<Xat> xats, String uidActual) {
        for (Xat xat : xats) {
            boolean socConductor = uidActual.equals(xat.getConductorId());
            String altreUid = socConductor ? xat.getPassatgerId() : xat.getConductorId();
            String nomActual = socConductor ? xat.getNomPassatger() : xat.getNomConductor();
            if (altreUid == null || nomActual != null && !nomActual.trim().isEmpty()) {
                continue;
            }
            db.collection(UtilitatsFirebase.COL_USUARIS)
                    .document(altreUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String nom = doc.getString("nom");
                        if (nom == null || nom.trim().isEmpty()) return;
                        if (socConductor) {
                            xat.setNomPassatger(nom);
                        } else {
                            xat.setNomConductor(nom);
                        }
                        adaptadorXats.notifyDataSetChanged();
                    });
        }
    }

    private void afegeixXatDeduplicat(Map<String, Xat> indexXats, Xat xat) {
        if (xat.getConductorId() == null || xat.getPassatgerId() == null) return;
        String clau = UtilitatsFirebase.creaIdXatUsuaris(xat.getConductorId(), xat.getPassatgerId());
        Xat existent = indexXats.get(clau);
        if (existent == null) {
            indexXats.put(clau, xat);
            return;
        }

        Xat conserva = existent.getDarreraActualitzacio() >= xat.getDarreraActualitzacio() ? existent : xat;
        indexXats.put(clau, conserva);
    }


    private void obreXat(Xat xat, String nomMostrat) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        boolean socConductor = uid.equals(xat.getConductorId());
        String altreUid = socConductor ? xat.getPassatgerId() : xat.getConductorId();
        Intent intent = new Intent(this, XatActivity.class);
        intent.putExtra(XatActivity.EXTRA_ID_XAT, xat.getId());
        intent.putExtra(XatActivity.EXTRA_NOM_XAT, nomMostrat);
        intent.putExtra(XatActivity.EXTRA_UID_ALTRE, altreUid);
        startActivity(intent);
    }
}
