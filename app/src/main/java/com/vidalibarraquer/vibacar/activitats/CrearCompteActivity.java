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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.utilitats.GestorIdioma;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CrearCompteActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextInputEditText campNom;
    private TextInputEditText campCorreu;
    private TextInputEditText campContrasenya;
    private TextInputEditText campRepeteixContrasenya;
    private TextView txtMissatge;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_compte);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        campNom = findViewById(R.id.campNom);
        campCorreu = findViewById(R.id.campCorreu);
        campContrasenya = findViewById(R.id.campContrasenya);
        campRepeteixContrasenya = findViewById(R.id.campRepeteixContrasenya);
        txtMissatge = findViewById(R.id.txtMissatge);

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        ((MaterialButton) findViewById(R.id.botoContinuaPerfil)).setOnClickListener(v -> creaCompte());
    }

    private void creaCompte() {
        String nom = obteText(campNom);
        String correu = obteText(campCorreu).toLowerCase(Locale.ROOT);
        String contrasenya = obteText(campContrasenya);
        String repetir = obteText(campRepeteixContrasenya);

        if (TextUtils.isEmpty(nom)) {
            mostraError(getString(R.string.error_nom_buit));
            return;
        }
        if (!UtilitatsFirebase.esCorreuCentre(correu)) {
            mostraError(getString(R.string.error_domini_correu));
            return;
        }
        if (contrasenya.length() < 6) {
            mostraError(getString(R.string.error_contrasenya_curta));
            return;
        }
        if (!contrasenya.equals(repetir)) {
            mostraError(getString(R.string.error_contrasenyes_diferents));
            return;
        }

        auth.createUserWithEmailAndPassword(correu, contrasenya)
                .addOnSuccessListener(authResult -> desaUsuariBase(nom, correu))
                .addOnFailureListener(e -> mostraError(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : getString(R.string.error_generica)));
    }

    private void desaUsuariBase(String nom, String correu) {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            mostraError(getString(R.string.error_no_usuari));
            return;
        }

        Map<String, Object> dadesUsuari = new HashMap<>();
        dadesUsuari.put("uid", usuari.getUid());
        dadesUsuari.put("nom", nom);
        dadesUsuari.put("correu", correu);
        dadesUsuari.put("telefon", "");
        dadesUsuari.put("rol", "");
        dadesUsuari.put("zona", "");
        dadesUsuari.put("horaSortidaHabitual", "");
        dadesUsuari.put("puntTrobadaHabitual", "");
        dadesUsuari.put("modelCotxe", "");
        dadesUsuari.put("placesHabituals", 0);
        dadesUsuari.put("bio", "");
        dadesUsuari.put("fotoUri", "");
        dadesUsuari.put("idioma", GestorIdioma.obteIdiomaGuardat(this));
        dadesUsuari.put("perfilCompletat", false);
        dadesUsuari.put("valoracioMitjana", 0d);
        dadesUsuari.put("totalValoracions", 0L);
        dadesUsuari.put("emailVerified", false);

        db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(usuari.getUid())
                .set(dadesUsuari, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Intent intent = new Intent(this, ConfiguraPerfilActivity.class);
                    intent.putExtra(ConfiguraPerfilActivity.EXTRA_PRIMER_COP, true);
                    intent.putExtra(ConfiguraPerfilActivity.EXTRA_NOM_INICIAL, nom);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> mostraError(getString(R.string.error_generica)));
    }

    private void mostraError(String missatge) {
        txtMissatge.setText(missatge);
    }

    private String obteText(TextInputEditText camp) {
        return camp.getText() == null ? "" : camp.getText().toString().trim();
    }
}
