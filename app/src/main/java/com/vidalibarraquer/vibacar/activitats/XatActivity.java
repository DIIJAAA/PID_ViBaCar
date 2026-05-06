package com.vidalibarraquer.vibacar.activitats;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.adaptadors.AdaptadorMissatges;
import com.vidalibarraquer.vibacar.models.Missatge;
import com.vidalibarraquer.vibacar.models.Notificacio;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsNotificacions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XatActivity extends AppCompatActivity {

    public static final String EXTRA_ID_XAT = "id_xat";
    public static final String EXTRA_NOM_XAT = "nom_xat";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String idXat;
    private AdaptadorMissatges adaptadorMissatges;
    private RecyclerView llistaMissatges;
    private com.google.android.material.textfield.TextInputEditText campMissatge;
    private String conductorIdXat;
    private String passatgerIdXat;
    private String nomUsuariActual;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xat);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        idXat = getIntent().getStringExtra(EXTRA_ID_XAT);

        if (TextUtils.isEmpty(idXat) || auth.getCurrentUser() == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.txtNomXat)).setText(getIntent().getStringExtra(EXTRA_NOM_XAT));
        llistaMissatges = findViewById(R.id.llistaMissatges);
        campMissatge = findViewById(R.id.campMissatge);

        adaptadorMissatges = new AdaptadorMissatges(auth.getCurrentUser().getUid());
        LinearLayoutManager gestor = new LinearLayoutManager(this);
        gestor.setStackFromEnd(true);
        llistaMissatges.setLayoutManager(gestor);
        llistaMissatges.setAdapter(adaptadorMissatges);

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        findViewById(R.id.botoEnviar).setOnClickListener(v -> enviaMissatge());

        carregaInfoXat();
    }

    private void carregaInfoXat() {
        db.collection(UtilitatsFirebase.COL_XATS)
                .document(idXat)
                .get()
                .addOnSuccessListener(xatDoc -> {
                    if (xatDoc.exists()) {
                        conductorIdXat = xatDoc.getString("conductorId");
                        passatgerIdXat = xatDoc.getString("passatgerId");
                    }
                });

        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) return;
        db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(usuari.getUid())
                .get()
                .addOnSuccessListener(usuariDoc -> {
                    String nom = usuariDoc.getString("nom");
                    nomUsuariActual = (nom != null && !nom.isEmpty()) ? nom : getString(R.string.text_usuari);
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        escoltaMissatges();
    }

    private void escoltaMissatges() {
        db.collection(UtilitatsFirebase.COL_XATS)
                .document(idXat)
                .collection(UtilitatsFirebase.COL_MISSATGES)
                .orderBy("dataMillis")
                .addSnapshotListener(this, (value, error) -> {
                    if (error != null || value == null) {
                        return;
                    }

                    List<Missatge> missatges = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                        Missatge missatge = doc.toObject(Missatge.class);
                        missatge.setId(doc.getId());
                        missatges.add(missatge);
                    }
                    adaptadorMissatges.actualitzaDades(missatges);
                    if (!missatges.isEmpty()) {
                        llistaMissatges.scrollToPosition(missatges.size() - 1);
                    }
                });
    }

    private void notificaAltreParticipant(String uidEmissor, String text) {
        String altreUid = null;
        if (conductorIdXat != null && passatgerIdXat != null) {
            altreUid = uidEmissor.equals(conductorIdXat) ? passatgerIdXat : conductorIdXat;
        }
        if (altreUid == null || altreUid.isEmpty()) return;

        String nom = nomUsuariActual != null ? nomUsuariActual : getString(R.string.text_usuari);
        UtilitatsNotificacions.publica(
                db,
                altreUid,
                Notificacio.TIPUS_NOU_MISSATGE,
                getString(R.string.notif_nou_missatge, nom),
                idXat
        );
    }

    private void enviaMissatge() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            return;
        }

        String text = campMissatge.getText() == null ? "" : campMissatge.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, R.string.error_missatge_buit, Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> missatge = new HashMap<>();
        missatge.put("emissorId", usuari.getUid());
        missatge.put("text", text);
        missatge.put("dataMillis", System.currentTimeMillis());

        db.collection(UtilitatsFirebase.COL_XATS)
                .document(idXat)
                .collection(UtilitatsFirebase.COL_MISSATGES)
                .add(missatge)
                .addOnSuccessListener(documentReference -> {
                    Map<String, Object> actualitzacio = new HashMap<>();
                    actualitzacio.put("darreraActualitzacio", System.currentTimeMillis());
                    actualitzacio.put("darrerMissatge", text);
                    actualitzacio.put("darrerEmissorId", usuari.getUid());
                    db.collection(UtilitatsFirebase.COL_XATS).document(idXat).set(actualitzacio, SetOptions.merge());
                    campMissatge.setText("");
                    notificaAltreParticipant(usuari.getUid(), text);
                })
                .addOnFailureListener(e -> Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show());
    }
}
