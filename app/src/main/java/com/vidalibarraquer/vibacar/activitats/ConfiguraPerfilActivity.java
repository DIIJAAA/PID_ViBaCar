package com.vidalibarraquer.vibacar.activitats;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.utilitats.GestorIdioma;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

import java.util.HashMap;
import java.util.Map;

public class ConfiguraPerfilActivity extends AppCompatActivity {

    public static final String EXTRA_PRIMER_COP = "primer_cop";
    public static final String EXTRA_NOM_INICIAL = "nom_inicial";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextInputEditText campNom;
    private TextInputEditText campBio;
    private ShapeableImageView imatgePerfil;
    private TextView txtInicialAvatar;
    private TextView txtMissatge;
    private LinearProgressIndicator indicadorCarrega;
    private boolean primerCop;
    private String fotoUri;
    private Uri fotoSeleccionadaUri;
    private Runnable accioDespresPermisNotificacions;

    private final ActivityResultLauncher<String> selectorFoto = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    fotoSeleccionadaUri = uri;
                    fotoUri = uri.toString();
                    actualitzaAvatar();
                }
            }
    );

    private final ActivityResultLauncher<String[]> permisosImatge = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> selectorFoto.launch("image/*")
    );

    private final ActivityResultLauncher<String> permisNotificacions = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            concedit -> {
                if (accioDespresPermisNotificacions != null) {
                    accioDespresPermisNotificacions.run();
                    accioDespresPermisNotificacions = null;
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configura_perfil);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        primerCop = getIntent().getBooleanExtra(EXTRA_PRIMER_COP, false);

        campNom = findViewById(R.id.campNom);
        campBio = findViewById(R.id.campBio);
        imatgePerfil = findViewById(R.id.imatgePerfil);
        txtInicialAvatar = findViewById(R.id.txtInicialAvatar);
        txtMissatge = findViewById(R.id.txtMissatge);
        indicadorCarrega = findViewById(R.id.indicadorCarrega);

        carregaPerfilSiExisteix();

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        
        FloatingActionButton botoTriaFoto = findViewById(R.id.botoTriaFoto);
        if (botoTriaFoto != null) {
            botoTriaFoto.setOnClickListener(v -> obreSelectorFotoAmbPermisos());
        }

        ((MaterialButton) findViewById(R.id.botoDesarPerfil)).setOnClickListener(v -> desaPerfil());
    }

    private void carregaPerfilSiExisteix() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            finish();
            return;
        }

        String nomInicial = getIntent().getStringExtra(EXTRA_NOM_INICIAL);
        if (!TextUtils.isEmpty(nomInicial)) {
            campNom.setText(nomInicial);
            actualitzaAvatar();
        }

        mostrarCarrega(true);
        db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(usuari.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    mostrarCarrega(false);
                    Usuari perfil = documentSnapshot.toObject(Usuari.class);
                    if (perfil == null) {
                        return;
                    }

                    if (!TextUtils.isEmpty(perfil.getNom())) {
                        campNom.setText(perfil.getNom());
                    }
                    campBio.setText(perfil.getBio());
                    fotoUri = perfil.getFotoUri();

                    actualitzaAvatar();
                })
                .addOnFailureListener(e -> mostrarCarrega(false));
    }

    private void desaPerfil() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            txtMissatge.setText(R.string.error_no_usuari);
            return;
        }

        String nom = obteText(campNom);
        String bio = obteText(campBio);

        if (TextUtils.isEmpty(nom)) {
            txtMissatge.setText(R.string.error_nom_buit);
            return;
        }

        mostrarCarrega(true);
        if (fotoSeleccionadaUri != null) {
            pujaFotoIDesaPerfil(usuari, nom, bio);
            return;
        }
        desaPerfilAmbFoto(usuari, nom, bio, fotoUri);
    }

    private void pujaFotoIDesaPerfil(FirebaseUser usuari, String nom, String bio) {
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference()
                .child("usuaris")
                .child(usuari.getUid())
                .child("perfil.jpg");

        ref.putFile(fotoSeleccionadaUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    fotoUri = uri.toString();
                    fotoSeleccionadaUri = null;
                    desaPerfilAmbFoto(usuari, nom, bio, fotoUri);
                })
                .addOnFailureListener(e -> {
                    mostrarCarrega(false);
                    txtMissatge.setText(R.string.error_generica);
                });
    }

    private void desaPerfilAmbFoto(FirebaseUser usuari, String nom, String bio, String fotoDefinitiva) {
        String fotoPerfil = fotoDefinitiva == null ? "" : fotoDefinitiva;
        Map<String, Object> dades = new HashMap<>();
        dades.put("uid", usuari.getUid());
        dades.put("nom", nom);
        dades.put("correu", usuari.getEmail());
        dades.put("telefon", "");
        dades.put("rol", "usuari");
        dades.put("bio", bio);
        dades.put("fotoUri", fotoPerfil);
        dades.put("idioma", GestorIdioma.obteIdiomaGuardat(this));
        dades.put("perfilCompletat", true);
        dades.put("emailVerified", usuari.isEmailVerified());

        db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(usuari.getUid())
                .set(dades, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    actualitzaFotoViatgesActius(usuari.getUid(), fotoPerfil, () -> {
                        mostrarCarrega(false);
                        demanaPermisNotificacionsSiCal(() -> navegaDespresDeGuardar(usuari));
                    });
                })
                .addOnFailureListener(e -> {
                    mostrarCarrega(false);
                    txtMissatge.setText(R.string.error_generica);
                });
    }

    private void actualitzaFotoViatgesActius(String uid, String fotoDefinitiva, Runnable continuacio) {
        db.collection(UtilitatsFirebase.COL_VIATGES)
                .whereEqualTo("conductorId", uid)
                .whereEqualTo("estat", UtilitatsFirebase.ESTAT_VIATGE_DISPONIBLE)
                .get()
                .addOnSuccessListener(docs -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : docs) {
                        doc.getReference().set(
                                java.util.Collections.singletonMap("conductorFotoUri", fotoDefinitiva),
                                SetOptions.merge()
                        );
                    }
                    actualitzaFotoReservesComConductor(uid, fotoDefinitiva, continuacio);
                })
                .addOnFailureListener(e -> actualitzaFotoReservesComConductor(uid, fotoDefinitiva, continuacio));
    }

    private void actualitzaFotoReservesComConductor(String uid, String fotoDefinitiva, Runnable continuacio) {
        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("conductorId", uid)
                .get()
                .addOnSuccessListener(docs -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : docs) {
                        doc.getReference().set(
                                java.util.Collections.singletonMap("conductorFotoUri", fotoDefinitiva),
                                SetOptions.merge()
                        );
                    }
                    actualitzaFotoReservesComPassatger(uid, fotoDefinitiva, continuacio);
                })
                .addOnFailureListener(e -> actualitzaFotoReservesComPassatger(uid, fotoDefinitiva, continuacio));
    }

    private void actualitzaFotoReservesComPassatger(String uid, String fotoDefinitiva, Runnable continuacio) {
        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("passatgerId", uid)
                .get()
                .addOnSuccessListener(docs -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : docs) {
                        doc.getReference().set(
                                java.util.Collections.singletonMap("passatgerFotoUri", fotoDefinitiva),
                                SetOptions.merge()
                        );
                    }
                    continuacio.run();
                })
                .addOnFailureListener(e -> continuacio.run());
    }

    private void actualitzaAvatar() {
        UtilitatsAvatar.mostraAvatar(imatgePerfil, txtInicialAvatar, fotoUri, obteText(campNom));
    }

    private void obreSelectorFotoAmbPermisos() {
        String[] permisos = Build.VERSION.SDK_INT >= 33
                ? new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES}
                : new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};

        boolean calDemanar = false;
        for (String permis : permisos) {
            if (ContextCompat.checkSelfPermission(this, permis) != PackageManager.PERMISSION_GRANTED) {
                calDemanar = true;
                break;
            }
        }
        if (calDemanar) {
            permisosImatge.launch(permisos);
        } else {
            selectorFoto.launch("image/*");
        }
    }

    private void demanaPermisNotificacionsSiCal(Runnable continuacio) {
        if (primerCop && Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            accioDespresPermisNotificacions = continuacio;
            permisNotificacions.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        continuacio.run();
    }

    private void navegaDespresDeGuardar(FirebaseUser usuari) {
        if (primerCop) {
            UtilitatsFirebase.enviaVerificacio(this, usuari, task -> {
                startActivity(new Intent(this, VerificaCorreuActivity.class));
                finish();
            });
        } else if (usuari.isEmailVerified()) {
            startActivity(new Intent(this, PantallaPassatgerActivity.class));
            finish();
        } else {
            startActivity(new Intent(this, VerificaCorreuActivity.class));
            finish();
        }
    }

    private String obteText(TextInputEditText camp) {
        if (camp == null) return "";
        return camp.getText() == null ? "" : camp.getText().toString().trim();
    }

    private void mostrarCarrega(boolean actiu) {
        if (indicadorCarrega != null) {
            indicadorCarrega.setVisibility(actiu ? View.VISIBLE : View.GONE);
        }
        if (actiu) txtMissatge.setText("");
    }
}
