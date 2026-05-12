package com.vidalibarraquer.vibacar.activitats;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.utilitats.GestorIdioma;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PantallaBenvingudaActivity extends AppCompatActivity {

    private static final String TAG = "PantallaBenvinguda";
    private static final String PREFS_PERMISOS = "permisos_inicials";
    private static final String CLAU_PERMISOS_DEMANATS = "demanats";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;
    private TextView selectorIdioma;
    private TextView opcioIdiomaCat;
    private TextView opcioIdiomaCast;
    private TextView opcioIdiomaEng;
    private View menuIdiomes;

    private final ActivityResultLauncher<String[]> permisosInicialsLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> getSharedPreferences(PREFS_PERMISOS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(CLAU_PERMISOS_DEMANATS, true)
                    .apply()
    );

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK) {
                    return;
                }
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account != null) {
                        vincularAmbFirebase(account.getIdToken());
                    }
                } catch (ApiException e) {
                    Log.e(TAG, "Google Sign-In failed", e);
                    mostraError("Google: " + e.getStatusCode());
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_benvinguda);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        selectorIdioma = findViewById(R.id.selectorIdioma);
        menuIdiomes = findViewById(R.id.menuIdiomes);
        opcioIdiomaCat = findViewById(R.id.opcioIdiomaCat);
        opcioIdiomaCast = findViewById(R.id.opcioIdiomaCast);
        opcioIdiomaEng = findViewById(R.id.opcioIdiomaEng);
        MaterialButton botoCrearCompte = findViewById(R.id.botoCrearCompte);
        MaterialButton botoIniciarSessio = findViewById(R.id.botoIniciarSessio);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        configuraSelectorIdiomes();
        demanaPermisosInicialsSiCal();

        findViewById(R.id.botoGoogle).setOnClickListener(v -> iniciaSessioGoogle());
        botoCrearCompte.setOnClickListener(v -> startActivity(new Intent(this, CrearCompteActivity.class)));
        botoIniciarSessio.setOnClickListener(v -> startActivity(new Intent(this, IniciSessioActivity.class)));
    }

    private void demanaPermisosInicialsSiCal() {
        boolean jaDemanats = getSharedPreferences(PREFS_PERMISOS, MODE_PRIVATE)
                .getBoolean(CLAU_PERMISOS_DEMANATS, false);
        if (jaDemanats) {
            return;
        }

        List<String> permisos = new ArrayList<>();
        afegeixPermisSiCal(permisos, Manifest.permission.CAMERA);
        afegeixPermisSiCal(permisos, Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            afegeixPermisSiCal(permisos, Manifest.permission.POST_NOTIFICATIONS);
        }

        if (permisos.isEmpty()) {
            getSharedPreferences(PREFS_PERMISOS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(CLAU_PERMISOS_DEMANATS, true)
                    .apply();
            return;
        }
        permisosInicialsLauncher.launch(permisos.toArray(new String[0]));
    }

    private void afegeixPermisSiCal(List<String> permisos, String permis) {
        if (ContextCompat.checkSelfPermission(this, permis) != PackageManager.PERMISSION_GRANTED) {
            permisos.add(permis);
        }
    }

    private void iniciaSessioGoogle() {
        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void vincularAmbFirebase(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser usuari = auth.getCurrentUser();
                    if (usuari == null) {
                        mostraError(getString(R.string.error_no_usuari));
                        return;
                    }
                    if (!UtilitatsFirebase.esCorreuCentre(usuari.getEmail())) {
                        auth.signOut();
                        googleSignInClient.signOut();
                        mostraError(getString(R.string.error_domini_correu));
                        return;
                    }
                    obreSeguentPantalla(usuari);
                })
                .addOnFailureListener(e -> mostraError(e.getLocalizedMessage() != null
                        ? e.getLocalizedMessage()
                        : getString(R.string.error_generica)));
    }

    private void obreSeguentPantalla(FirebaseUser usuari) {
        DocumentReference refPerfil = db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(usuari.getUid());

        refPerfil.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        creaPerfilBase(refPerfil, usuari);
                        return;
                    }

                    Boolean perfilCompletat = documentSnapshot.getBoolean("perfilCompletat");
                    if (!Boolean.TRUE.equals(perfilCompletat)) {
                        obreConfiguraPrimerCop(false);
                        return;
                    }

                    obrePantalla(PantallaPassatgerActivity.class);
                })
                .addOnFailureListener(e -> mostraError(missatgeErrorFirestore(e)));
    }

    private void creaPerfilBase(DocumentReference refPerfil, FirebaseUser usuari) {
        Map<String, Object> dades = new HashMap<>();
        dades.put("uid", usuari.getUid());
        dades.put("nom", usuari.getDisplayName() == null ? "" : usuari.getDisplayName());
        dades.put("correu", usuari.getEmail() == null ? "" : usuari.getEmail());
        dades.put("bio", "");
        dades.put("fotoUri", usuari.getPhotoUrl() != null ? usuari.getPhotoUrl().toString() : "");
        dades.put("perfilCompletat", false);
        dades.put("valoracioMitjana", 0d);
        dades.put("totalValoracions", 0L);
        dades.put("emailVerified", true);
        dades.put("idioma", GestorIdioma.obteIdiomaGuardat(this));

        refPerfil.set(dades, SetOptions.merge())
                .addOnSuccessListener(unused -> obreConfiguraPrimerCop(true))
                .addOnFailureListener(e -> mostraError(missatgeErrorFirestore(e)));
    }

    private void obreConfiguraPrimerCop(boolean primerCop) {
        Intent intent = new Intent(this, ConfiguraPerfilActivity.class);
        intent.putExtra(ConfiguraPerfilActivity.EXTRA_PRIMER_COP, primerCop);
        startActivity(intent);
        finish();
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
        Toast.makeText(this, missatge, Toast.LENGTH_LONG).show();
    }

    private void configuraSelectorIdiomes() {
        selectorIdioma.setOnClickListener(v -> alternaMenuIdiomes());
        opcioIdiomaCat.setOnClickListener(v -> canviaIdioma("ca"));
        opcioIdiomaCast.setOnClickListener(v -> canviaIdioma("es"));
        opcioIdiomaEng.setOnClickListener(v -> canviaIdioma("en"));
        pintaIdiomaActiu();
    }

    private void alternaMenuIdiomes() {
        menuIdiomes.setVisibility(menuIdiomes.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void canviaIdioma(String codi) {
        menuIdiomes.setVisibility(View.GONE);
        if (codi.equals(GestorIdioma.obteIdiomaGuardat(this))) {
            return;
        }
        opcioIdiomaCat.setEnabled(false);
        opcioIdiomaCast.setEnabled(false);
        opcioIdiomaEng.setEnabled(false);
        GestorIdioma.guardaIAplica(this, codi);
        pintaIdiomaActiu();
    }

    private void pintaIdiomaActiu() {
        String idiomaActual = GestorIdioma.obteIdiomaGuardat(this);
        selectorIdioma.setText(etiquetaIdioma(idiomaActual));
        pintaIdioma(opcioIdiomaCat, "ca".equals(idiomaActual));
        pintaIdioma(opcioIdiomaCast, "es".equals(idiomaActual));
        pintaIdioma(opcioIdiomaEng, "en".equals(idiomaActual));
    }

    private String etiquetaIdioma(String codi) {
        if ("es".equals(codi)) return "CAST";
        if ("en".equals(codi)) return "ENG";
        return "CAT";
    }

    private void pintaIdioma(TextView boto, boolean actiu) {
        boto.setBackgroundResource(actiu ? R.drawable.fons_selector_idioma_actiu : android.R.color.transparent);
    }
}
