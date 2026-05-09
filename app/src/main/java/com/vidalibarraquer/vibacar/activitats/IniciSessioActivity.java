package com.vidalibarraquer.vibacar.activitats;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
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

@SuppressWarnings("SpellCheckingInspection")
public class IniciSessioActivity extends AppCompatActivity {

    private static final String TAG = "IniciSessioActivity";
    public static final String PROVIDER_PASSWORD = "password";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextInputEditText campCorreu;
    private TextInputEditText campContrasenya;
    private TextView txtMissatge;
    private View indicadorCarrega;
    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            vincularAmbFirebase(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        Log.e(TAG, "Google Sign-In failed", e);
                        mostraError("Error Google: " + e.getStatusCode());
                    }
                } else {
                    Log.w(TAG, "Google Sign-In result not OK: " + result.getResultCode());
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inici_sessio);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        campCorreu = findViewById(R.id.campCorreu);
        campContrasenya = findViewById(R.id.campContrasenya);
        txtMissatge = findViewById(R.id.txtMissatge);
        indicadorCarrega = findViewById(R.id.indicadorCarrega);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        findViewById(R.id.botoIniciarSessio).setOnClickListener(v -> iniciaSessio());
        findViewById(R.id.botoRecorda).setOnClickListener(v -> {
            Intent intent = new Intent(this, RecuperaContrasenyaActivity.class);
            intent.putExtra(RecuperaContrasenyaActivity.EXTRA_CORREU_INICIAL, obteText(campCorreu));
            startActivity(intent);
        });
        findViewById(R.id.txtVesCrearCompte).setOnClickListener(v -> startActivity(new Intent(this, CrearCompteActivity.class)));

        View botoGoogle = findViewById(R.id.botoGoogle);
        if (botoGoogle != null) {
            botoGoogle.setOnClickListener(v -> {
                txtMissatge.setText("");
                Intent signInIntent = googleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        }
    }

    private void vincularAmbFirebase(String idToken) {
        mostrarCarrega(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser usuari = auth.getCurrentUser();
                    if (usuari != null) {
                        if (!UtilitatsFirebase.esCorreuCentre(usuari.getEmail())) {
                            auth.signOut();
                            googleSignInClient.signOut();
                            mostrarCarrega(false);
                            mostraError(getString(R.string.error_domini_correu));
                            return;
                        }
                        mostrarCarrega(false);
                        obreSeguentPantalla(usuari);
                    }
                })
                .addOnFailureListener(e -> {
                    mostrarCarrega(false);
                    Log.e(TAG, "Firebase Auth with Google failed", e);
                    mostraError(e.getMessage());
                });
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

        mostrarCarrega(true);
        auth.signInWithEmailAndPassword(correu, contrasenya)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser usuari = auth.getCurrentUser();
                    if (usuari == null) {
                        mostrarCarrega(false);
                        mostraError(getString(R.string.error_no_usuari));
                        return;
                    }

                    usuari.reload()
                            .addOnSuccessListener(unused -> {
                                mostrarCarrega(false);
                                obreSeguentPantalla(usuari);
                            })
                            .addOnFailureListener(e -> {
                                mostrarCarrega(false);
                                mostraError(getString(R.string.error_generica));
                            });
                })
                .addOnFailureListener(e -> {
                    mostrarCarrega(false);
                    mostraError(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : getString(R.string.error_generica));
                });
    }

    private void obreSeguentPantalla(FirebaseUser usuari) {
        boolean esPasswordProvider = false;
        for (UserInfo profile : usuari.getProviderData()) {
            if (PROVIDER_PASSWORD.equals(profile.getProviderId())) {
                esPasswordProvider = true;
                break;
            }
        }

        if (esPasswordProvider && !usuari.isEmailVerified()) {
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
        txtMissatge.setText(missatge);
    }

    private void mostrarCarrega(boolean actiu) {
        if (indicadorCarrega != null) {
            indicadorCarrega.setVisibility(actiu ? View.VISIBLE : View.GONE);
        }
        if (actiu) txtMissatge.setText("");
    }

    private String obteText(TextInputEditText camp) {
        return camp.getText() == null ? "" : camp.getText().toString().trim();
    }
}
