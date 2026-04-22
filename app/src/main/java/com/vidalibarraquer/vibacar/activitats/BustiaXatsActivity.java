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
        adaptadorXats = new AdaptadorXats(this, usuari.getUid(), this::obreXat);
        llistaXats.setLayoutManager(new LinearLayoutManager(this));
        llistaXats.setAdapter(adaptadorXats);

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                        indexXats.put(xat.getId(), xat);
                    }

                    db.collection(UtilitatsFirebase.COL_XATS)
                            .whereEqualTo("passatgerId", uid)
                            .get()
                            .addOnSuccessListener(passatgerDocs -> {
                                for (QueryDocumentSnapshot doc : passatgerDocs) {
                                    Xat xat = doc.toObject(Xat.class);
                                    xat.setId(doc.getId());
                                    indexXats.put(xat.getId(), xat);
                                }

                                List<Xat> resultat = new ArrayList<>(indexXats.values());
                                resultat.sort(Comparator.comparingLong(Xat::getDarreraActualitzacio).reversed());
                                adaptadorXats.actualitzaDades(resultat);
                                txtBuit.setVisibility(resultat.isEmpty() ? View.VISIBLE : View.GONE);
                            })
                            .addOnFailureListener(e -> mostraLlista(indexXats));
                })
                .addOnFailureListener(e -> mostraLlista(indexXats));
    }

    private void mostraLlista(Map<String, Xat> indexXats) {
        List<Xat> resultat = new ArrayList<>(indexXats.values());
        resultat.sort(Comparator.comparingLong(Xat::getDarreraActualitzacio).reversed());
        adaptadorXats.actualitzaDades(resultat);
        txtBuit.setVisibility(resultat.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void obreXat(Xat xat, String nomMostrat) {
        Intent intent = new Intent(this, XatActivity.class);
        intent.putExtra(XatActivity.EXTRA_ID_XAT, xat.getId());
        intent.putExtra(XatActivity.EXTRA_NOM_XAT, nomMostrat);
        startActivity(intent);
    }
}
