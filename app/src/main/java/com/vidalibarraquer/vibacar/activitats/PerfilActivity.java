package com.vidalibarraquer.vibacar.activitats;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.adaptadors.AdaptadorReserves;
import com.vidalibarraquer.vibacar.models.Reserva;
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.utilitats.GestorIdioma;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PerfilActivity extends AppCompatActivity implements AdaptadorReserves.AccionsReservaListener {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private AdaptadorReserves adaptadorReserves;
    private TextView txtNom;
    private TextView txtDades;
    private TextView txtInicialAvatar;
    private TextView txtBuit;
    private TextView txtTotalValoracions;
    private ShapeableImageView imatgePerfil;
    private RatingBar barraReputacio;
    private MaterialButton botoVeureCotxe;
    private MaterialButton botoVeureInfo;
    private MaterialAutoCompleteTextView campIdioma;
    private boolean mostraPassats;
    private String[] nomsIdiomes;
    private String[] codisIdiomes;

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
        barraReputacio = findViewById(R.id.barraReputacio);
        botoVeureCotxe = findViewById(R.id.botoVeureCotxe);
        botoVeureInfo = findViewById(R.id.botoVeureInfo);
        campIdioma = findViewById(R.id.campIdioma);

        FirebaseUser usuariActual = auth.getCurrentUser();
        String uidActual = usuariActual == null ? "" : usuariActual.getUid();

        RecyclerView llistaReserves = findViewById(R.id.llistaReserves);
        adaptadorReserves = new AdaptadorReserves(this, uidActual, this);
        llistaReserves.setLayoutManager(new LinearLayoutManager(this));
        llistaReserves.setAdapter(adaptadorReserves);

        nomsIdiomes = getResources().getStringArray(R.array.idiomes_llista);
        codisIdiomes = getResources().getStringArray(R.array.idiomes_codis);
        configuraSelectorIdiomes();

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        findViewById(R.id.botoEditarPerfil).setOnClickListener(v -> startActivity(new Intent(this, EditaPerfilActivity.class)));
        findViewById(R.id.botoBustiaXats).setOnClickListener(v -> startActivity(new Intent(this, BustiaXatsActivity.class)));
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

    private void configuraSelectorIdiomes() {
        campIdioma.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nomsIdiomes));
        String idiomaActual = GestorIdioma.obteIdiomaGuardat(this);
        int posicio = 0;
        for (int i = 0; i < codisIdiomes.length; i++) {
            if (codisIdiomes[i].equals(idiomaActual)) {
                posicio = i;
                break;
            }
        }
        campIdioma.setText(nomsIdiomes[posicio], false);
        campIdioma.setOnItemClickListener((parent, view, position, id) -> {
            GestorIdioma.guardaIAplica(this, codisIdiomes[position]);
            FirebaseUser usuari = auth.getCurrentUser();
            if (usuari != null) {
                Map<String, Object> dades = new HashMap<>();
                dades.put("idioma", codisIdiomes[position]);
                db.collection(UtilitatsFirebase.COL_USUARIS).document(usuari.getUid()).set(dades, SetOptions.merge());
            }
            recreate();
        });
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
                    if (perfil == null) {
                        return;
                    }
                    txtNom.setText(perfil.getNom());
                    barraReputacio.setRating((float) perfil.getValoracioMitjana());
                    txtTotalValoracions.setText(getString(R.string.text_total_valoracions_format, perfil.getTotalValoracions()));
                    txtDades.setText(getString(
                            R.string.text_resum_perfil_format,
                            valorPerMostrar(perfil.getCorreu()),
                            valorPerMostrar(perfil.getZona()),
                            valorPerMostrar(perfil.getTelefon())
                    ));
                    txtDades.append("\n");
                    txtDades.append(getString(
                            R.string.text_resum_mobilitat_format,
                            valorPerMostrar(perfil.getHoraSortidaHabitual()),
                            valorPerMostrar(perfil.getPuntTrobadaHabitual())
                    ));
                    if ((perfil.getModelCotxe() != null && !perfil.getModelCotxe().trim().isEmpty())
                            || perfil.getPlacesHabituals() > 0) {
                        txtDades.append("\n");
                        txtDades.append(getString(
                            R.string.text_resum_conductor_format,
                                valorPerMostrar(perfil.getModelCotxe()),
                                perfil.getPlacesHabituals() > 0 ? String.valueOf(perfil.getPlacesHabituals()) : "-"
                            ));
                    }
                    botoVeureInfo.setOnClickListener(v -> obreDialegInfo(perfil));
                    if (UtilitatsFirebase.esRolConductor(perfil.getRol())) {
                        botoVeureCotxe.setVisibility(View.VISIBLE);
                        botoVeureCotxe.setOnClickListener(v -> obreDialegVehicle(perfil));
                    } else {
                        botoVeureCotxe.setVisibility(View.GONE);
                    }
                    UtilitatsAvatar.mostraAvatar(imatgePerfil, txtInicialAvatar, perfil.getFotoUri(), perfil.getNom());
                });
    }

    private void obreDialegInfo(Usuari perfil) {
        View vista = LayoutInflater.from(this).inflate(R.layout.dialog_perfil_detall, null, false);
        TextView txtNomDetall = vista.findViewById(R.id.txtNomDetall);
        TextView txtDataNaixementDetall = vista.findViewById(R.id.txtDataNaixementDetall);
        TextView txtSexeDetall = vista.findViewById(R.id.txtSexeDetall);
        TextView txtBioDetall = vista.findViewById(R.id.txtBioDetall);
        TextView txtCorreuDetall = vista.findViewById(R.id.txtCorreuDetall);
        TextView txtTelDetall = vista.findViewById(R.id.txtTelDetall);

        txtNomDetall.setText(valorPerMostrar(perfil.getNom()));
        txtDataNaixementDetall.setText(getString(R.string.perfil_data_naixement_format, valorPerMostrar(perfil.getDataNaixement())));
        txtSexeDetall.setText(getString(R.string.perfil_sexe_format, valorPerMostrar(perfil.getSexe())));
        txtBioDetall.setText(getString(R.string.perfil_bio_format, valorPerMostrar(perfil.getBio())));
        txtCorreuDetall.setText(valorPerMostrar(perfil.getCorreu()));
        txtTelDetall.setText(valorPerMostrar(perfil.getTelefon()));

        new AlertDialog.Builder(this)
                .setTitle(R.string.perfil_sobre_mi)
                .setView(vista)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void obreDialegVehicle(Usuari perfil) {
        View vista = LayoutInflater.from(this).inflate(R.layout.dialog_detall_vehicle, null, false);
        TextView txtModel = vista.findViewById(R.id.txtModelCotxe);
        TextView txtPlaces = vista.findViewById(R.id.txtPlacesCotxe);

        txtModel.setText(getString(R.string.perfil_model_format, valorPerMostrar(perfil.getModelCotxe())));
        txtPlaces.setText(getString(
                R.string.perfil_places_format,
                perfil.getPlacesHabituals() > 0 ? String.valueOf(perfil.getPlacesHabituals()) : "-"
        ));

        new AlertDialog.Builder(this)
                .setTitle(R.string.perfil_vehicle)
                .setView(vista)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void carregaLlista() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            return;
        }

        Map<String, Reserva> indexReserves = new LinkedHashMap<>();

        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("passatgerId", usuari.getUid())
                .get()
                .addOnSuccessListener(passatgerDocs -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : passatgerDocs) {
                        Reserva reserva = doc.toObject(Reserva.class);
                        reserva.setId(doc.getId());
                        reserva.setSocConductor(false);
                        if (filtreReserva(reserva)) {
                            indexReserves.put(reserva.getId(), reserva);
                        }
                    }

                    db.collection(UtilitatsFirebase.COL_RESERVES)
                            .whereEqualTo("conductorId", usuari.getUid())
                            .get()
                            .addOnSuccessListener(conductorDocs -> {
                                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : conductorDocs) {
                                    Reserva reserva = doc.toObject(Reserva.class);
                                    reserva.setId(doc.getId());
                                    reserva.setSocConductor(true);
                                    if (filtreReserva(reserva)) {
                                        indexReserves.put(reserva.getId(), reserva);
                                    }
                                }

                                List<Reserva> resultat = new ArrayList<>(indexReserves.values());
                                resultat.sort(Comparator.comparingLong(Reserva::getSortidaMillis));
                                adaptadorReserves.actualitzaDades(resultat, mostraPassats);
                                txtBuit.setVisibility(resultat.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                            });
                });
    }

    private boolean filtreReserva(Reserva reserva) {
        boolean esPassat = UtilitatsData.esPassat(reserva.getSortidaMillis());
        if (mostraPassats) {
            return esPassat && UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA.equals(reserva.getEstat());
        }
        return !esPassat || UtilitatsFirebase.ESTAT_RESERVA_PENDENT.equals(reserva.getEstat());
    }

    private void actualitzaBotonsFiltre() {
        com.google.android.material.button.MaterialButton botoProxims = findViewById(R.id.botoProxims);
        com.google.android.material.button.MaterialButton botoPassats = findViewById(R.id.botoPassats);

        if (mostraPassats) {
            botoPassats.setBackgroundResource(R.drawable.fons_boto_principal);
            botoPassats.setTextColor(getColor(R.color.color_text_clar));
            botoProxims.setBackgroundResource(android.R.color.transparent);
            botoProxims.setStrokeColorResource(R.color.color_principal);
        } else {
            botoProxims.setBackgroundResource(R.drawable.fons_boto_principal);
            botoProxims.setTextColor(getColor(R.color.color_text_clar));
            botoPassats.setBackgroundResource(android.R.color.transparent);
            botoPassats.setStrokeColorResource(R.color.color_principal);
        }
    }

    @Override
    public void onPuntua(Reserva reserva) {
        android.view.View vista = LayoutInflater.from(this).inflate(R.layout.dialog_puntuacio, null, false);
        RatingBar barraPuntuacio = vista.findViewById(R.id.barraPuntuacio);

        new AlertDialog.Builder(this)
                .setView(vista)
                .setPositiveButton(R.string.boto_puntuar, (dialog, which) -> desaPuntuacio(reserva, barraPuntuacio.getRating()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onAccepta(Reserva reserva) {
        DocumentReference reservaRef = db.collection(UtilitatsFirebase.COL_RESERVES).document(reserva.getId());
        DocumentReference viatgeRef = db.collection(UtilitatsFirebase.COL_VIATGES).document(reserva.getViatgeId());

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot reservaDoc = transaction.get(reservaRef);
            if (!reservaDoc.exists()) {
                throw new IllegalStateException("NO_RESERVA");
            }
            String estatActual = reservaDoc.getString("estat");
            if (!UtilitatsFirebase.ESTAT_RESERVA_PENDENT.equals(estatActual)) {
                throw new IllegalStateException("NO_PENDENT");
            }

            com.google.firebase.firestore.DocumentSnapshot viatgeDoc = transaction.get(viatgeRef);
            Long placesDisponibles = viatgeDoc.getLong("placesDisponibles");
            if (placesDisponibles == null || placesDisponibles <= 0) {
                throw new IllegalStateException("SENSE_PLACES");
            }

            transaction.update(reservaRef, "estat", UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA);
            transaction.update(viatgeRef, "placesDisponibles", placesDisponibles - 1);
            return true;
        }).addOnSuccessListener(unused -> {
            creaXatSiNoExisteix(reserva);
            Toast.makeText(this, R.string.missatge_reserva_acceptada, Toast.LENGTH_SHORT).show();
            carregaLlista();
        }).addOnFailureListener(e -> Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRebutja(Reserva reserva) {
        db.collection(UtilitatsFirebase.COL_RESERVES)
                .document(reserva.getId())
                .update("estat", UtilitatsFirebase.ESTAT_RESERVA_REBUTJADA)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, R.string.missatge_reserva_rebutjada, Toast.LENGTH_SHORT).show();
                    carregaLlista();
                })
                .addOnFailureListener(e -> Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onCancela(Reserva reserva) {
        DocumentReference reservaRef = db.collection(UtilitatsFirebase.COL_RESERVES).document(reserva.getId());
        DocumentReference viatgeRef = db.collection(UtilitatsFirebase.COL_VIATGES).document(reserva.getViatgeId());

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot reservaDoc = transaction.get(reservaRef);
            if (!reservaDoc.exists()) {
                throw new IllegalStateException("NO_RESERVA");
            }
            String estatActual = reservaDoc.getString("estat");
            if (UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA.equals(estatActual)) {
                com.google.firebase.firestore.DocumentSnapshot viatgeDoc = transaction.get(viatgeRef);
                Long placesDisponibles = viatgeDoc.getLong("placesDisponibles");
                long novesPlaces = placesDisponibles == null ? 1 : placesDisponibles + 1;
                transaction.update(viatgeRef, "placesDisponibles", novesPlaces);
            }
            transaction.update(reservaRef, "estat", UtilitatsFirebase.ESTAT_RESERVA_CANCELADA);
            return true;
        }).addOnSuccessListener(unused -> {
            Toast.makeText(this, R.string.missatge_reserva_cancelada, Toast.LENGTH_SHORT).show();
            carregaLlista();
        }).addOnFailureListener(e -> Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onObreXat(Reserva reserva) {
        if (!UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA.equals(reserva.getEstat())) {
            Toast.makeText(this, R.string.missatge_xat_despres_acceptar, Toast.LENGTH_SHORT).show();
            return;
        }

        String idXat = UtilitatsFirebase.creaIdXat(reserva.getViatgeId(), reserva.getPassatgerId());
        Map<String, Object> dadesXat = construeixDadesXat(reserva);

        db.collection(UtilitatsFirebase.COL_XATS)
                .document(idXat)
                .set(dadesXat, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Intent intent = new Intent(this, XatActivity.class);
                    intent.putExtra(XatActivity.EXTRA_ID_XAT, idXat);
                    String nomXat = reserva.isSocConductor() ? valorPerMostrar(reserva.getPassatgerNom()) : valorPerMostrar(reserva.getConductorNom());
                    intent.putExtra(XatActivity.EXTRA_NOM_XAT, nomXat);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show());
    }

    private void creaXatSiNoExisteix(Reserva reserva) {
        String idXat = UtilitatsFirebase.creaIdXat(reserva.getViatgeId(), reserva.getPassatgerId());
        db.collection(UtilitatsFirebase.COL_XATS).document(idXat).set(construeixDadesXat(reserva), SetOptions.merge());
    }

    private Map<String, Object> construeixDadesXat(Reserva reserva) {
        Map<String, Object> dadesXat = new HashMap<>();
        dadesXat.put("viatgeId", reserva.getViatgeId());
        dadesXat.put("conductorId", reserva.getConductorId());
        dadesXat.put("passatgerId", reserva.getPassatgerId());
        dadesXat.put("nomConductor", reserva.getConductorNom());
        dadesXat.put("nomPassatger", reserva.getPassatgerNom());
        dadesXat.put("origen", reserva.getOrigen());
        dadesXat.put("desti", reserva.getDesti());
        dadesXat.put("sortidaMillis", reserva.getSortidaMillis());
        dadesXat.put("darreraActualitzacio", System.currentTimeMillis());
        return dadesXat;
    }

    private void desaPuntuacio(Reserva reserva, float puntuacio) {
        if (puntuacio <= 0f) {
            Toast.makeText(this, R.string.error_generica, Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference reservaRef = db.collection(UtilitatsFirebase.COL_RESERVES).document(reserva.getId());
        DocumentReference conductorRef = db.collection(UtilitatsFirebase.COL_USUARIS).document(reserva.getConductorId());

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot conductorDoc = transaction.get(conductorRef);
            long totalValoracions = conductorDoc.contains("totalValoracions")
                    ? conductorDoc.getLong("totalValoracions")
                    : 0L;
            double valoracioMitjana = conductorDoc.contains("valoracioMitjana")
                    ? conductorDoc.getDouble("valoracioMitjana")
                    : 0d;

            long nouTotal = totalValoracions + 1;
            double novaMitjana = ((valoracioMitjana * totalValoracions) + puntuacio) / nouTotal;

            transaction.update(reservaRef, "valorada", true, "puntuacio", puntuacio);

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

    private String valorPerMostrar(String valor) {
        return valor == null || valor.trim().isEmpty() ? getString(R.string.text_no_definit) : valor.trim();
    }
}
