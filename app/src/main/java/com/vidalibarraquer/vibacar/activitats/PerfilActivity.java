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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PerfilActivity extends AppCompatActivity {

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
    private boolean mostraPassats;
    private boolean dialogValoracioMostrat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

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
                String idXat = UtilitatsFirebase.creaIdXatUsuaris(reserva.getConductorId(), reserva.getPassatgerId());
                String nomAltre = reserva.isSocConductor() ? reserva.getPassatgerNom() : reserva.getConductorNom();
                Intent intent = new Intent(PerfilActivity.this, XatActivity.class);
                intent.putExtra(XatActivity.EXTRA_ID_XAT, idXat);
                intent.putExtra(XatActivity.EXTRA_NOM_XAT, nomAltre);
                startActivity(intent);
            }
        });
        llistaReserves.setLayoutManager(new LinearLayoutManager(this));
        llistaReserves.setAdapter(adaptadorReserves);

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        findViewById(R.id.botoEditarPerfil).setOnClickListener(v -> startActivity(new Intent(this, ConfiguraPerfilActivity.class)));
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
            dialogValoracioMostrat = false;
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
                                    reservaPropia.setOrigen(viatge.getOrigen());
                                    reservaPropia.setDesti(viatge.getDesti());
                                    reservaPropia.setSortidaMillis(viatge.getSortidaMillis());
                                    reservaPropia.setValorada(true);
                                    reservaPropia.setPuntuacio(0f);
                                    if (filtreData(viatge.getSortidaMillis())) {
                                        resultat.add(reservaPropia);
                                    }
                                }

                                resultat.sort(Comparator.comparingLong(Reserva::getSortidaMillis));
                                adaptadorReserves.actualitzaDades(resultat, mostraPassats);
                                txtBuit.setVisibility(resultat.isEmpty() ? View.VISIBLE : View.GONE);

                                if (mostraPassats && !dialogValoracioMostrat) {
                                    for (Reserva r : resultat) {
                                        if (!r.getId().startsWith("viatge_")
                                                && !r.isValorada()
                                                && UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA.equals(r.getEstat())
                                                && usuari.getUid().equals(r.getPassatgerId())) {
                                            dialogValoracioMostrat = true;
                                            obreDialegPuntuacio(r);
                                            break;
                                        }
                                    }
                                }
                            });
                });
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
        String conductorId = reserva.getConductorId();
        if (android.text.TextUtils.isEmpty(conductorId)) {
            Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference reservaRef = db.collection(UtilitatsFirebase.COL_RESERVES).document(reserva.getId());
        DocumentReference conductorRef = db.collection(UtilitatsFirebase.COL_USUARIS).document(conductorId);

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot conductorDoc = transaction.get(conductorRef);
            long totalValoracions = conductorDoc.contains("totalValoracions")
                    ? (conductorDoc.getLong("totalValoracions") != null ? conductorDoc.getLong("totalValoracions") : 0L)
                    : 0L;
            double valoracioMitjana = conductorDoc.contains("valoracioMitjana")
                    ? (conductorDoc.getDouble("valoracioMitjana") != null ? conductorDoc.getDouble("valoracioMitjana") : 0d)
                    : 0d;

            long nouTotal = totalValoracions + 1;
            double novaMitjana = ((valoracioMitjana * totalValoracions) + puntuacio) / nouTotal;

            transaction.update(reservaRef, "valorada", true, "puntuacio", puntuacio, "comentari", comentari);

            Map<String, Object> dadesConductor = new HashMap<>();
            dadesConductor.put("totalValoracions", nouTotal);
            dadesConductor.put("valoracioMitjana", novaMitjana);
            transaction.set(conductorRef, dadesConductor, SetOptions.merge());
            return true;
        }).addOnSuccessListener(unused -> {
            Toast.makeText(this, R.string.missatge_puntuacio_guardada, Toast.LENGTH_SHORT).show();
            carregaCapcalera();
            carregaLlista();
        }).addOnFailureListener(e -> Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show());
    }
}
