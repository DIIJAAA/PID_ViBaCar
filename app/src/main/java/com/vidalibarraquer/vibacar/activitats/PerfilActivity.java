package com.vidalibarraquer.vibacar.activitats;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.adaptadors.AdaptadorReserves;
import com.vidalibarraquer.vibacar.models.Reserva;
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.models.Viatge;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsNotificacions;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsXats;
import com.vidalibarraquer.vibacar.models.Notificacio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PerfilActivity extends AppCompatActivity {

    public static final String EXTRA_MOSTRA_PASSATS = "mostra_passats";
    public static final String EXTRA_RESERVA_DESTACADA = "reserva_destacada";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private AdaptadorReserves adaptadorReserves;
    private TextView txtNom;
    private TextView txtDades;
    private TextView txtInicialAvatar;
    private TextView txtBuit;
    private TextView txtTotalValoracions;
    private ShapeableImageView imatgePerfil;
    private ImageView iconaVerificat;
    private RatingBar barraReputacio;
    private String fotoPerfilActual;
    private boolean mostraPassats;
    private String reservaDestacadaId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        mostraPassats = getIntent().getBooleanExtra(EXTRA_MOSTRA_PASSATS, false);
        reservaDestacadaId = getIntent().getStringExtra(EXTRA_RESERVA_DESTACADA);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txtNom = findViewById(R.id.txtNom);
        txtDades = findViewById(R.id.txtDades);
        txtInicialAvatar = findViewById(R.id.txtInicialAvatar);
        txtBuit = findViewById(R.id.txtBuit);
        txtTotalValoracions = findViewById(R.id.txtTotalValoracions);
        imatgePerfil = findViewById(R.id.imatgePerfil);
        iconaVerificat = findViewById(R.id.iconaVerificat);
        barraReputacio = findViewById(R.id.barraReputacio);

        String uidActual = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        RecyclerView llistaReserves = findViewById(R.id.llistaReserves);
        adaptadorReserves = new AdaptadorReserves(this, uidActual, new AdaptadorReserves.AccionsReservaListener() {
            @Override
            public void onPuntua(Reserva reserva) {
                obreDialegPuntuacio(reserva);
            }

            @Override
            public void onAccepta(Reserva reserva) {}

            @Override
            public void onRebutja(Reserva reserva) {}

            @Override
            public void onCancela(Reserva reserva) {}

            @Override
            public void onObreXat(Reserva reserva) {
                String nomAltre = reserva.isSocConductor() ? reserva.getPassatgerNom() : reserva.getConductorNom();
                String uidAltre = reserva.isSocConductor() ? reserva.getPassatgerId() : reserva.getConductorId();
                Map<String, Object> dadesXat = new HashMap<>();
                dadesXat.put("viatgeId", reserva.getViatgeId());
                dadesXat.put("conductorId", reserva.getConductorId());
                dadesXat.put("passatgerId", reserva.getPassatgerId());
                dadesXat.put("nomConductor", reserva.getConductorNom());
                dadesXat.put("nomPassatger", reserva.getPassatgerNom());
                dadesXat.put("origen", reserva.getOrigen());
                dadesXat.put("desti", reserva.getDesti());
                dadesXat.put("sortidaMillis", reserva.getSortidaMillis());
                String idLlegat = reserva.getViatgeId() + "_" + reserva.getPassatgerId();
                UtilitatsXats.preparaXatEntreUsuaris(
                        db,
                        reserva.getConductorId(),
                        reserva.getPassatgerId(),
                        idLlegat,
                        dadesXat,
                        new UtilitatsXats.Callback() {
                            @Override
                            public void onPreparat(String idXat) {
                                Intent intent = new Intent(PerfilActivity.this, XatActivity.class);
                                intent.putExtra(XatActivity.EXTRA_ID_XAT, idXat);
                                intent.putExtra(XatActivity.EXTRA_NOM_XAT, nomAltre);
                                intent.putExtra(XatActivity.EXTRA_UID_ALTRE, uidAltre);
                                startActivity(intent);
                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(PerfilActivity.this, R.string.error_generica, Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onVeurePerfil(Reserva reserva) {
                String uidAltre = reserva.isSocConductor() ? reserva.getPassatgerId() : reserva.getConductorId();
                if (android.text.TextUtils.isEmpty(uidAltre)) return;
                Intent intent = new Intent(PerfilActivity.this, VeurePerfilActivity.class);
                intent.putExtra(VeurePerfilActivity.EXTRA_UID, uidAltre);
                startActivity(intent);
            }
        });
        llistaReserves.setLayoutManager(new LinearLayoutManager(this));
        llistaReserves.setAdapter(adaptadorReserves);

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        findViewById(R.id.botoEditarPerfil).setOnClickListener(v -> startActivity(new Intent(this, ConfiguraPerfilActivity.class)));
        findViewById(R.id.botoVeurePerfilPublic).setOnClickListener(v -> obrePerfilPublicPropi());
        findViewById(R.id.botoCanviarContrasenya).setOnClickListener(v -> enviaCanviContrasenya());
        findViewById(R.id.botoTancarSessio).setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(this, PantallaBenvingudaActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.botoProxims).setOnClickListener(v -> {
            mostraPassats = false;
            actualitzaBotonsFiltre();
            carregaLlista();
        });
        findViewById(R.id.botoPassats).setOnClickListener(v -> {
            mostraPassats = true;
            actualitzaBotonsFiltre();
            carregaLlista();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualitzaBotonsFiltre();
        carregaCapcalera();
        carregaLlista();
    }

    private void obrePerfilPublicPropi() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) return;
        Intent intent = new Intent(this, VeurePerfilActivity.class);
        intent.putExtra(VeurePerfilActivity.EXTRA_UID, usuari.getUid());
        startActivity(intent);
    }

    private void carregaCapcalera() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            finish();
            return;
        }

        db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(usuari.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Usuari perfil = documentSnapshot.toObject(Usuari.class);
                    if (perfil == null) return;

                    txtNom.setText(perfil.getNom());
                    barraReputacio.setRating((float) perfil.getValoracioMitjana());
                    txtTotalValoracions.setText(String.format(Locale.getDefault(), "(%d)", perfil.getTotalValoracions()));

                    String bio = perfil.getBio() == null || perfil.getBio().trim().isEmpty()
                            ? getString(R.string.perfil_bio_buida)
                            : perfil.getBio().trim();
                    txtDades.setText(bio);
                    iconaVerificat.setVisibility(usuari.isEmailVerified() ? View.VISIBLE : View.GONE);
                    fotoPerfilActual = perfil.getFotoUri();

                    UtilitatsAvatar.mostraAvatar(imatgePerfil, txtInicialAvatar, perfil.getFotoUri(), perfil.getNom());
                });
    }

    private void enviaCanviContrasenya() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null || usuari.getEmail() == null) return;

        if (!teProviderPassword(usuari)) {
            enviaEnllacReset(usuari);
            return;
        }

        TextInputEditText campActual = creaCampContrasenya();
        TextInputEditText campRepetida = creaCampContrasenya();
        campRepetida.setHint(R.string.etiqueta_repeteix_contrasenya_actual);

        LinearLayout contenidor = new LinearLayout(this);
        contenidor.setOrientation(LinearLayout.VERTICAL);
        int marge = (int) (20 * getResources().getDisplayMetrics().density);
        contenidor.setPadding(marge, 8, marge, 0);
        contenidor.addView(campActual);
        contenidor.addView(campRepetida);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.canvi_contrasenya_titol)
                .setMessage(R.string.canvi_contrasenya_missatge)
                .setView(contenidor)
                .setPositiveButton(R.string.boto_envia_recuperacio, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> validaIEnviaReset(usuari, campActual, campRepetida, dialog)));
        dialog.show();
    }

    private TextInputEditText creaCampContrasenya() {
        TextInputEditText camp = new TextInputEditText(this);
        camp.setHint(R.string.etiqueta_contrasenya_actual);
        camp.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        camp.setSingleLine(true);
        camp.setTextSize(13);
        camp.setHintTextColor(getColor(R.color.color_text_secundari));
        return camp;
    }

    private boolean teProviderPassword(FirebaseUser usuari) {
        for (UserInfo info : usuari.getProviderData()) {
            if (IniciSessioActivity.PROVIDER_PASSWORD.equals(info.getProviderId())) {
                return true;
            }
        }
        return false;
    }

    private void validaIEnviaReset(FirebaseUser usuari, TextInputEditText campActual,
                                   TextInputEditText campRepetida, AlertDialog dialog) {
        String actual = obteText(campActual);
        String repetida = obteText(campRepetida);
        if (actual.length() < 6 || !actual.equals(repetida)) {
            Toast.makeText(this, R.string.error_contrasenya_actual_diferent, Toast.LENGTH_SHORT).show();
            return;
        }
        AuthCredential credential = EmailAuthProvider.getCredential(usuari.getEmail(), actual);
        usuari.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    dialog.dismiss();
                    enviaEnllacReset(usuari);
                })
                .addOnFailureListener(e -> Toast.makeText(this, R.string.error_contrasenya_actual, Toast.LENGTH_SHORT).show());
    }

    private void enviaEnllacReset(FirebaseUser usuari) {
        auth.sendPasswordResetEmail(usuari.getEmail())
                .addOnSuccessListener(unused -> Toast.makeText(this, R.string.missatge_recuperacio_contrasenya, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show());
    }

    private String obteText(TextInputEditText camp) {
        return camp.getText() == null ? "" : camp.getText().toString().trim();
    }

    private String valor(String text) {
        return text == null || text.trim().isEmpty() ? getString(R.string.text_no_definit) : text.trim();
    }

    private void carregaLlista() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) return;

        List<Reserva> resultat = new ArrayList<>();

        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("passatgerId", usuari.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Reserva reserva = doc.toObject(Reserva.class);
                        reserva.setId(doc.getId());
                        if (!UtilitatsFirebase.esReservaActiva(reserva.getEstat())) {
                            continue;
                        }
                        if (filtreData(reserva.getSortidaMillis())) {
                            resultat.add(reserva);
                        }
                    }

                    carregaReservesComConductor(resultat, usuari);
                });
    }

    private void carregaReservesComConductor(List<Reserva> resultat, FirebaseUser usuari) {
        if (!mostraPassats) {
            carregaViatgesPropis(resultat, usuari);
            return;
        }

        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("conductorId", usuari.getUid())
                .whereEqualTo("estat", UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA)
                .get()
                .addOnSuccessListener(reservesDocs -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : reservesDocs) {
                        Reserva reserva = doc.toObject(Reserva.class);
                        reserva.setId(doc.getId());
                        if (filtreData(reserva.getSortidaMillis())) {
                            resultat.add(reserva);
                        }
                    }
                    carregaViatgesPropis(resultat, usuari);
                })
                .addOnFailureListener(e -> carregaViatgesPropis(resultat, usuari));
    }

    private void carregaViatgesPropis(List<Reserva> resultat, FirebaseUser usuari) {
        db.collection(UtilitatsFirebase.COL_VIATGES)
                .whereEqualTo("conductorId", usuari.getUid())
                .get()
                .addOnSuccessListener(viatgesDocs -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : viatgesDocs) {
                        Viatge viatge = doc.toObject(Viatge.class);
                        if (UtilitatsFirebase.ESTAT_VIATGE_CANCELAT.equalsIgnoreCase(viatge.getEstat())
                                || UtilitatsFirebase.ESTAT_VIATGE_COMPLETAT.equalsIgnoreCase(viatge.getEstat())) {
                            continue;
                        }
                        Reserva reservaPropia = new Reserva();
                        reservaPropia.setId("viatge_" + doc.getId());
                        reservaPropia.setViatgeId(doc.getId());
                        reservaPropia.setConductorId(usuari.getUid());
                        reservaPropia.setConductorNom(getString(R.string.nom_marca));
                        reservaPropia.setConductorFotoUri(fotoPerfilActual);
                        reservaPropia.setOrigen(viatge.getOrigen());
                        reservaPropia.setDesti(viatge.getDesti());
                        reservaPropia.setSortidaMillis(viatge.getSortidaMillis());
                        reservaPropia.setValorada(true);
                        reservaPropia.setConductorValorada(true);
                        reservaPropia.setPuntuacio(0f);
                        if (filtreData(viatge.getSortidaMillis())) {
                            resultat.add(reservaPropia);
                        }
                    }
                    mostraReserves(resultat, usuari);
                })
                .addOnFailureListener(e -> mostraReserves(resultat, usuari));
    }

    private void mostraReserves(List<Reserva> resultat, FirebaseUser usuari) {
        resultat.sort((a, b) -> {
            if (reservaDestacadaId != null) {
                boolean aDestacada = reservaDestacadaId.equals(a.getId());
                boolean bDestacada = reservaDestacadaId.equals(b.getId());
                if (aDestacada != bDestacada) return aDestacada ? -1 : 1;
            }
            return Long.compare(a.getSortidaMillis(), b.getSortidaMillis());
        });
        adaptadorReserves.actualitzaDades(resultat, mostraPassats);
        txtBuit.setVisibility(resultat.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean filtreData(long sortidaMillis) {
        boolean esPassat = UtilitatsData.esPassat(sortidaMillis);
        return mostraPassats == esPassat;
    }

    private void actualitzaBotonsFiltre() {
        MaterialButton botoProxims = findViewById(R.id.botoProxims);
        MaterialButton botoPassats = findViewById(R.id.botoPassats);

        pintaBotoFiltre(botoProxims, !mostraPassats);
        pintaBotoFiltre(botoPassats, mostraPassats);
    }

    private void pintaBotoFiltre(MaterialButton boto, boolean seleccionat) {
        int colorFons = getColor(seleccionat ? R.color.color_principal : R.color.color_targeta);
        int colorText = getColor(seleccionat ? R.color.color_text_clar : R.color.color_principal);
        int colorVora = getColor(seleccionat ? R.color.color_principal : R.color.color_linia);

        boto.setBackgroundTintList(ColorStateList.valueOf(colorFons));
        boto.setTextColor(colorText);
        boto.setStrokeColor(ColorStateList.valueOf(colorVora));
        boto.setStrokeWidth(seleccionat ? 0 : (int) (getResources().getDisplayMetrics().density + 0.5f));
    }

    private void obreDialegPuntuacio(Reserva reserva) {
        if (reserva.getId().startsWith("viatge_")) return;

        View vista = LayoutInflater.from(this).inflate(R.layout.dialog_puntuacio, null, false);
        RatingBar barraPuntuacio = vista.findViewById(R.id.barraPuntuacio);
        barraPuntuacio.setRating(3.0f);
        com.google.android.material.textfield.TextInputEditText campComentari =
                vista.findViewById(R.id.campComentari);

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_valorar_titol)
                .setView(vista)
                .setPositiveButton(R.string.boto_puntuar, (dialog, which) -> {
                    String comentari = campComentari != null && campComentari.getText() != null
                            ? campComentari.getText().toString().trim() : "";
                    desaPuntuacio(reserva, barraPuntuacio.getRating(), comentari);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void desaPuntuacio(Reserva reserva, float puntuacio, String comentari) {
        if (puntuacio <= 0f) {
            Toast.makeText(this, R.string.error_valoracio_zero, Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) return;

        boolean socConductor = usuari.getUid().equals(reserva.getConductorId());
        String uidValorat = socConductor ? reserva.getPassatgerId() : reserva.getConductorId();
        if (android.text.TextUtils.isEmpty(uidValorat)) {
            Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference reservaRef = db.collection(UtilitatsFirebase.COL_RESERVES).document(reserva.getId());
        DocumentReference usuariValoratRef = db.collection(UtilitatsFirebase.COL_USUARIS).document(uidValorat);
        DocumentReference valoracioRef = usuariValoratRef
                .collection("valoracions")
                .document(reserva.getId() + "_" + usuari.getUid());
        String nomValorador = txtNom.getText() == null || txtNom.getText().toString().trim().isEmpty()
                ? getString(R.string.text_usuari)
                : txtNom.getText().toString().trim();

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot reservaDoc = transaction.get(reservaRef);
            com.google.firebase.firestore.DocumentSnapshot valoracioDoc = transaction.get(valoracioRef);
            com.google.firebase.firestore.DocumentSnapshot usuariValoratDoc = transaction.get(usuariValoratRef);
            if (!reservaDoc.exists()
                    || !UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA.equals(reservaDoc.getString("estat"))) {
                throw new IllegalStateException("La reserva no es pot valorar");
            }
            String campValorada = socConductor ? "conductorValorada" : "valorada";
            Boolean jaValorada = reservaDoc.getBoolean(campValorada);
            if (Boolean.TRUE.equals(jaValorada) || valoracioDoc.exists()) {
                throw new IllegalStateException("Aquesta reserva ja esta valorada");
            }

            long totalValoracions = usuariValoratDoc.contains("totalValoracions")
                    ? (usuariValoratDoc.getLong("totalValoracions") != null ? usuariValoratDoc.getLong("totalValoracions") : 0L)
                    : 0L;
            double valoracioMitjana = usuariValoratDoc.contains("valoracioMitjana")
                    ? (usuariValoratDoc.getDouble("valoracioMitjana") != null ? usuariValoratDoc.getDouble("valoracioMitjana") : 0d)
                    : 0d;

            long nouTotal = totalValoracions + 1;
            double novaMitjana = ((valoracioMitjana * totalValoracions) + puntuacio) / nouTotal;

            if (socConductor) {
                transaction.update(reservaRef,
                        "conductorValorada", true,
                        "conductorPuntuacio", puntuacio,
                        "conductorComentari", comentari);
            } else {
                transaction.update(reservaRef,
                        "valorada", true,
                        "puntuacio", puntuacio,
                        "comentari", comentari);
            }

            Map<String, Object> dadesUsuariValorat = new HashMap<>();
            dadesUsuariValorat.put("totalValoracions", nouTotal);
            dadesUsuariValorat.put("valoracioMitjana", novaMitjana);
            transaction.set(usuariValoratRef, dadesUsuariValorat, SetOptions.merge());

            Map<String, Object> dadesValoracio = new HashMap<>();
            dadesValoracio.put("autorId", usuari.getUid());
            dadesValoracio.put("autorNom", nomValorador);
            dadesValoracio.put("valoratId", uidValorat);
            dadesValoracio.put("reservaId", reserva.getId());
            dadesValoracio.put("viatgeId", reserva.getViatgeId());
            dadesValoracio.put("origen", reserva.getOrigen());
            dadesValoracio.put("desti", reserva.getDesti());
            dadesValoracio.put("puntuacio", puntuacio);
            dadesValoracio.put("comentari", comentari);
            dadesValoracio.put("dataMillis", System.currentTimeMillis());
            transaction.set(valoracioRef, dadesValoracio, SetOptions.merge());
            return true;
        }).addOnSuccessListener(unused -> {
            UtilitatsNotificacions.publica(
                    db,
                    uidValorat,
                    Notificacio.TIPUS_NOVA_VALORACIO,
                    getString(R.string.notif_nova_valoracio, nomValorador),
                    valoracioRef.getId()
            );
            Toast.makeText(this, R.string.missatge_puntuacio_guardada, Toast.LENGTH_SHORT).show();
            carregaCapcalera();
            carregaLlista();
        }).addOnFailureListener(e -> Toast.makeText(this, missatgeErrorValoracio(e), Toast.LENGTH_LONG).show());
    }

    private String missatgeErrorValoracio(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException ex = (FirebaseFirestoreException) e;
            if (ex.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return getString(R.string.error_valoracio_permisos);
            }
        }
        String missatge = e.getLocalizedMessage();
        if (!android.text.TextUtils.isEmpty(missatge)
                && missatge.toLowerCase(Locale.ROOT).contains("valorada")) {
            return getString(R.string.error_valoracio_ja_feta);
        }
        return getString(R.string.error_generica);
    }
}
