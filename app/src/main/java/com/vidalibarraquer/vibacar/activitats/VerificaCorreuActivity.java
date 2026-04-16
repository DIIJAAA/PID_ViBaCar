package com.vidalibarraquer.vibacar.activitats;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.utilitats.GestorIdioma;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

import java.util.HashMap;
import java.util.Map;

public class VerificaCorreuActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextView txtInfoCorreu;
    private TextView txtMissatge;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verifica_correu);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        txtInfoCorreu = findViewById(R.id.txtInfoCorreu);
        txtMissatge = findViewById(R.id.txtMissatge);

        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            obreBenvinguda();
            return;
        }

        txtInfoCorreu.setText(getString(R.string.text_info_verificacio_format, usuari.getEmail()));

        ((MaterialButton) findViewById(R.id.botoReenviar)).setOnClickListener(v ->
                UtilitatsFirebase.enviaVerificacio(this, usuari, task -> txtMissatge.setText(R.string.missatge_correu_verificacio)));

        ((MaterialButton) findViewById(R.id.botoJaVerificat)).setOnClickListener(v -> comprovaVerificacio());
        ((MaterialButton) findViewById(R.id.botoTancarSessio)).setOnClickListener(v -> {
            auth.signOut();
            obreBenvinguda();
        });
    }

    private void comprovaVerificacio() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            obreBenvinguda();
            return;
        }

        usuari.reload().addOnSuccessListener(unused -> {
            FirebaseUser usuariActualitzat = auth.getCurrentUser();
            if (usuariActualitzat == null) {
                obreBenvinguda();
                return;
            }

            if (!usuariActualitzat.isEmailVerified()) {
                txtMissatge.setText(R.string.missatge_verificacio_necessaria);
                return;
            }

            Map<String, Object> dades = new HashMap<>();
            dades.put("emailVerified", true);
            db.collection(UtilitatsFirebase.COL_USUARIS)
                    .document(usuariActualitzat.getUid())
                    .set(dades, SetOptions.merge())
                    .addOnSuccessListener(v -> obreSeguentPantalla(usuariActualitzat))
                    .addOnFailureListener(e -> txtMissatge.setText(missatgeErrorFirestore(e)));
        }).addOnFailureListener(e -> txtMissatge.setText(getString(R.string.error_generica)));
    }

    private void obreSeguentPantalla(FirebaseUser usuari) {
        DocumentReference refPerfil = db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(usuari.getUid());

        refPerfil
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        creaPerfilBase(refPerfil, usuari);
                        return;
                    }

                    Boolean perfilCompletat = documentSnapshot.getBoolean("perfilCompletat");
                    Class<?> desti = Boolean.TRUE.equals(perfilCompletat)
                            ? PantallaPrincipalActivity.class
                            : ConfiguraPerfilActivity.class;
                    startActivity(new Intent(this, desti));
                    finish();
                })
                .addOnFailureListener(e -> txtMissatge.setText(missatgeErrorFirestore(e)));
    }

    private void creaPerfilBase(DocumentReference refPerfil, FirebaseUser usuari) {
        Map<String, Object> dades = new HashMap<>();
        dades.put("uid", usuari.getUid());
        dades.put("nom", usuari.getDisplayName() == null ? "" : usuari.getDisplayName());
        dades.put("correu", usuari.getEmail() == null ? "" : usuari.getEmail());
        dades.put("telefon", "");
        dades.put("rol", "");
        dades.put("zona", "");
        dades.put("horaSortidaHabitual", "");
        dades.put("puntTrobadaHabitual", "");
        dades.put("modelCotxe", "");
        dades.put("placesHabituals", 0);
        dades.put("bio", "");
        dades.put("fotoUri", "");
        dades.put("perfilCompletat", false);
        dades.put("valoracioMitjana", 0d);
        dades.put("totalValoracions", 0L);
        dades.put("emailVerified", usuari.isEmailVerified());
        dades.put("idioma", GestorIdioma.obteIdiomaGuardat(this));

        refPerfil.set(dades, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    startActivity(new Intent(this, ConfiguraPerfilActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> txtMissatge.setText(missatgeErrorFirestore(e)));
    }

    private String missatgeErrorFirestore(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException ex = (FirebaseFirestoreException) e;
            if (ex.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return getString(R.string.error_base_dades_permis);
            }
            if (ex.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return getString(R.string.error_base_dades_connexio);
            }
        }
        return e.getLocalizedMessage() != null ? e.getLocalizedMessage() : getString(R.string.error_generica);
    }

    private void obreBenvinguda() {
        startActivity(new Intent(this, PantallaBenvingudaActivity.class));
        finish();
    }
}
