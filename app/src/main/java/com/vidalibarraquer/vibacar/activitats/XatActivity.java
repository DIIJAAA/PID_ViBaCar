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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.adaptadors.AdaptadorMissatges;
import com.vidalibarraquer.vibacar.models.Missatge;
import com.vidalibarraquer.vibacar.models.Notificacio;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsNotificacions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XatActivity extends AppCompatActivity {

    public static final String EXTRA_ID_XAT = "id_xat";
    public static final String EXTRA_NOM_XAT = "nom_xat";
    public static final String EXTRA_UID_ALTRE = "uid_altre";
    private static final int MIDA_PAGINA = 30;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String idXat;
    private AdaptadorMissatges adaptadorMissatges;
    private RecyclerView llistaMissatges;
    private com.google.android.material.textfield.TextInputEditText campMissatge;
    private String conductorIdXat;
    private String passatgerIdXat;
    private String nomUsuariActual;
    private String uidAltreIntent;
    private TextView txtNomXat;
    private TextView txtAvatarXat;
    private final Map<String, Missatge> missatgesPerId = new LinkedHashMap<>();
    private DocumentSnapshot documentMesAntic;
    private ListenerRegistration registreMissatges;
    private boolean carregantAnteriors;
    private boolean hiHaMesAnteriors = true;
    private boolean primeraCarrega = true;
    private LinearLayoutManager gestorMissatges;

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

        String nomXat = getIntent().getStringExtra(EXTRA_NOM_XAT);
        uidAltreIntent = getIntent().getStringExtra(EXTRA_UID_ALTRE);

        txtNomXat = findViewById(R.id.txtNomXat);
        txtAvatarXat = findViewById(R.id.txtAvatarXat);
        mostraCapcaleraXat(nomXat);
        android.view.View.OnClickListener obrePerfil = v -> {
            if (!TextUtils.isEmpty(uidAltreIntent)) {
                android.content.Intent intent = new android.content.Intent(this, VeurePerfilActivity.class);
                intent.putExtra(VeurePerfilActivity.EXTRA_UID, uidAltreIntent);
                startActivity(intent);
            }
        };
        txtNomXat.setOnClickListener(obrePerfil);
        txtAvatarXat.setOnClickListener(obrePerfil);

        llistaMissatges = findViewById(R.id.llistaMissatges);
        campMissatge = findViewById(R.id.campMissatge);

        adaptadorMissatges = new AdaptadorMissatges(auth.getCurrentUser().getUid());
        gestorMissatges = new LinearLayoutManager(this);
        gestorMissatges.setStackFromEnd(true);
        llistaMissatges.setLayoutManager(gestorMissatges);
        llistaMissatges.setAdapter(adaptadorMissatges);
        llistaMissatges.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (gestorMissatges.findFirstVisibleItemPosition() <= 2) {
                    carregaMissatgesAnteriors();
                }
            }
        });

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
                        completaCapcaleraAmbXat(xatDoc);
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

    private void completaCapcaleraAmbXat(DocumentSnapshot xatDoc) {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) return;

        boolean socConductor = usuari.getUid().equals(conductorIdXat);
        uidAltreIntent = socConductor ? passatgerIdXat : conductorIdXat;
        String nomAltre = socConductor ? xatDoc.getString("nomPassatger") : xatDoc.getString("nomConductor");
        if (!TextUtils.isEmpty(nomAltre) && !getString(R.string.text_usuari).equals(nomAltre)) {
            mostraCapcaleraXat(nomAltre);
            return;
        }

        if (TextUtils.isEmpty(uidAltreIntent)) return;
        db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(uidAltreIntent)
                .get()
                .addOnSuccessListener(usuariDoc -> {
                    String nom = usuariDoc.getString("nom");
                    if (!TextUtils.isEmpty(nom)) {
                        mostraCapcaleraXat(nom);
                    }
                });
    }

    private void mostraCapcaleraXat(String nomXat) {
        String nom = TextUtils.isEmpty(nomXat) ? getString(R.string.text_usuari) : nomXat;
        txtNomXat.setText(nom);
        txtAvatarXat.setText(nom.substring(0, 1).toUpperCase(java.util.Locale.ROOT));
    }

    @Override
    protected void onStart() {
        super.onStart();
        escoltaMissatges();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (registreMissatges != null) {
            registreMissatges.remove();
            registreMissatges = null;
        }
    }

    private void escoltaMissatges() {
        if (registreMissatges != null) {
            registreMissatges.remove();
        }
        registreMissatges = db.collection(UtilitatsFirebase.COL_XATS)
                .document(idXat)
                .collection(UtilitatsFirebase.COL_MISSATGES)
                .orderBy("dataMillis")
                .limitToLast(MIDA_PAGINA)
                .addSnapshotListener(this, (value, error) -> {
                    if (error != null || value == null) {
                        return;
                    }

                    if (!value.getDocuments().isEmpty()
                            && (documentMesAntic == null || missatgesPerId.isEmpty())) {
                        documentMesAntic = value.getDocuments().get(0);
                    }

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                        Missatge missatge = doc.toObject(Missatge.class);
                        missatge.setId(doc.getId());
                        missatgesPerId.put(doc.getId(), missatge);
                    }
                    List<Missatge> missatges = ordenaMissatges();
                    adaptadorMissatges.actualitzaDades(missatges);
                    if (!missatges.isEmpty() && primeraCarrega) {
                        primeraCarrega = false;
                        llistaMissatges.scrollToPosition(missatges.size() - 1);
                    }
                });
    }

    private void carregaMissatgesAnteriors() {
        if (carregantAnteriors || !hiHaMesAnteriors || documentMesAntic == null) {
            return;
        }

        carregantAnteriors = true;
        int midaAnterior = missatgesPerId.size();

        db.collection(UtilitatsFirebase.COL_XATS)
                .document(idXat)
                .collection(UtilitatsFirebase.COL_MISSATGES)
                .orderBy("dataMillis", Query.Direction.ASCENDING)
                .endBefore(documentMesAntic)
                .limitToLast(MIDA_PAGINA)
                .get()
                .addOnSuccessListener(value -> {
                    List<DocumentSnapshot> docs = value.getDocuments();
                    if (docs.isEmpty()) {
                        hiHaMesAnteriors = false;
                        return;
                    }

                    documentMesAntic = docs.get(0);
                    for (DocumentSnapshot doc : docs) {
                        Missatge missatge = doc.toObject(Missatge.class);
                        if (missatge == null) {
                            continue;
                        }
                        missatge.setId(doc.getId());
                        missatgesPerId.put(doc.getId(), missatge);
                    }

                    List<Missatge> missatges = ordenaMissatges();
                    adaptadorMissatges.actualitzaDades(missatges);
                    int afegits = missatgesPerId.size() - midaAnterior;
                    if (afegits > 0) {
                        gestorMissatges.scrollToPositionWithOffset(afegits, 0);
                    }
                })
                .addOnCompleteListener(task -> carregantAnteriors = false);
    }

    private List<Missatge> ordenaMissatges() {
        List<Missatge> missatges = new ArrayList<>(missatgesPerId.values());
        missatges.sort(Comparator.comparingLong(Missatge::getDataMillis));
        return missatges;
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
