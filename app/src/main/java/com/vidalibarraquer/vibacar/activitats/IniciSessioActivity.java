package com.vidalibarraquer.vibacar.activitats;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
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
import java.util.Locale;
import java.util.Map;

public class IniciSessioActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextInputEditText campCorreu;
    private TextInputEditText campContrasenya;
    private TextView txtMissatge;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inici_sessio);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        campCorreu = findViewById(R.id.campCorreu);
        campContrasenya = findViewById(R.id.campContrasenya);
        txtMissatge = findViewById(R.id.txtMissatge);

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        ((MaterialButton) findViewById(R.id.botoIniciarSessio)).setOnClickListener(v -> iniciaSessio());
        ((MaterialButton) findViewById(R.id.botoRecorda)).setOnClickListener(v -> {
            Intent intent = new Intent(this, RecuperaContrasenyaActivity.class);
            intent.putExtra(RecuperaContrasenyaActivity.EXTRA_CORREU_INICIAL, obteText(campCorreu));
            startActivity(intent);
        });
        findViewById(R.id.txtVesCrearCompte).setOnClickListener(v -> startActivity(new Intent(this, CrearCompteActivity.class)));
    }

    private void iniciaSessio() {
        String correu = obteText(campCorreu).toLowerCase(Locale.ROOT);
        String contrasenya = obteText(campContrasenya);

        if (!UtilitatsFirebase.esCorreuCentre(correu)) {
            mostraError(getString(R.string.error_domini_correu));
            return;
        }

        if (contrasenya.length() < 6) {
            mostraError(getString(R.string.error_contrasenya_curta));
            return;
        }

        auth.signInWithEmailAndPassword(correu, contrasenya)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser usuari = auth.getCurrentUser();
                    if (usuari == null) {
                        mostraError(getString(R.string.error_no_usuari));
                        return;
                    }

                    usuari.reload().addOnSuccessListener(unused -> obreSeguentPantalla(usuari));
                })
                .addOnFailureListener(e -> mostraError(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : getString(R.string.error_generica)));
    }

    private void obreSeguentPantalla(FirebaseUser usuari) {
        if (!usuari.isEmailVerified()) {
            startActivity(new Intent(this, VerificaCorreuActivity.class));
            finish();
            return;
        }

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
                            ? PantallaPassatgerActivity.class
                            : ConfiguraPerfilActivity.class;
                    obrePantalla(desti);
                })
                .addOnFailureListener(e -> mostraError(missatgeErrorFirestore(e)));
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
                .addOnSuccessListener(unused -> obrePantalla(ConfiguraPerfilActivity.class))
                .addOnFailureListener(e -> mostraError(missatgeErrorFirestore(e)));
    }

    private void obrePantalla(Class<?> desti) {
        startActivity(new Intent(this, desti));
        finish();
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

    private void mostraError(String missatge) {
        txtMissatge.setText(missatge);
    }

    private String obteText(TextInputEditText camp) {
        return camp.getText() == null ? "" : camp.getText().toString().trim();
    }
}
