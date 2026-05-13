package com.vidalibarraquer.vibacar.activitats;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.adaptadors.AdaptadorViatges;
import com.vidalibarraquer.vibacar.models.Notificacio;
import com.vidalibarraquer.vibacar.models.Usuari;
import com.vidalibarraquer.vibacar.models.Viatge;
import com.vidalibarraquer.vibacar.utilitats.PuntsMapaViBaCar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsBottomNav;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsGeo;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsNotificacions;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsRecordatoris;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PantallaPassatgerActivity extends AppCompatActivity {

    public static final String EXTRA_CONDUCTOR_ID = "filtre_conductor_id";

    private static final float RADI_KM = 8f;
    private static final long FINESTRA_DIA_MS = 24L * 60L * 60L * 1000L;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private final List<Viatge> totsElsViatges = new ArrayList<>();
    private AdaptadorViatges adaptadorViatges;
    private TextView txtBuit;
    private TextView txtInicialAvatar;
    private ShapeableImageView imatgePerfil;
    private TextInputEditText campOrigen;
    private ImageButton botoFiltres;
    private TextView txtInsignieFiltres;
    private TextView botoNetejar;
    private String filtreConductorId;
    private final Set<String> conductorsSeguits = new HashSet<>();

    private String modeCerca = "propers";
    private String filtreDir = "totes";
    private String textDesti = "";
    private int placesMinimes = 0;
    private boolean recordatorisComprovats;

    private Long dataSeleccionadaMillis;
    private LatLng coordOrigen;
    private LatLng coordDesti;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_passatger);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txtBuit = findViewById(R.id.txtBuit);
        txtInicialAvatar = findViewById(R.id.txtInicialAvatar);
        imatgePerfil = findViewById(R.id.imatgePerfil);
        campOrigen = findViewById(R.id.campOrigen);
        botoFiltres = findViewById(R.id.botoFiltres);
        txtInsignieFiltres = findViewById(R.id.txtInsignieFiltres);
        botoNetejar = findViewById(R.id.botoNetejar);
        filtreConductorId = getIntent().getStringExtra(EXTRA_CONDUCTOR_ID);

        RecyclerView llistaViatges = findViewById(R.id.llistaViatges);
        adaptadorViatges = new AdaptadorViatges(this, viatge -> {
            Intent intent = new Intent(this, DetallViatgeActivity.class);
            intent.putExtra(DetallViatgeActivity.EXTRA_ID_VIATGE, viatge.getId());
            startActivity(intent);
        }, this::obrePerfilConductor);
        llistaViatges.setLayoutManager(new LinearLayoutManager(this));
        llistaViatges.setAdapter(adaptadorViatges);

        findViewById(R.id.tabConductor).setOnClickListener(v -> {
            startActivity(new Intent(this, PantallaConductorActivity.class));
            finish();
        });
        findViewById(R.id.botoPerfil).setOnClickListener(v -> startActivity(new Intent(this, PerfilActivity.class)));
        findViewById(R.id.botoCercar).setOnClickListener(v -> executaCerca());
        botoNetejar.setOnClickListener(v -> netejaCerca());
        botoFiltres.setOnClickListener(v -> obreFullFiltres());

        configuraBotomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregaUsuariCapcalera();
        carregaSeguiments();
        carregaViatges();
        carregaEstatReserves();
        UtilitatsBottomNav.actualitzaBadgeNotificacions(this);
        if (!recordatorisComprovats) {
            recordatorisComprovats = true;
            UtilitatsRecordatoris.comprova(this, db, auth.getCurrentUser());
        }
    }

    private void carregaUsuariCapcalera() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) {
            startActivity(new Intent(this, PantallaBenvingudaActivity.class));
            finish();
            return;
        }
        db.collection(UtilitatsFirebase.COL_USUARIS)
                .document(usuari.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    Usuari perfil = doc.toObject(Usuari.class);
                    String nom = perfil != null && perfil.getNom() != null ? perfil.getNom() : getString(R.string.nom_marca);
                    UtilitatsAvatar.mostraAvatar(imatgePerfil, txtInicialAvatar,
                            perfil != null ? perfil.getFotoUri() : null, nom);
                });
    }

    private void carregaSeguiments() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) return;
        db.collection(UtilitatsFirebase.COL_SEGUIMENTS)
                .whereEqualTo("seguidorId", usuari.getUid())
                .get()
                .addOnSuccessListener(docs -> {
                    conductorsSeguits.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : docs) {
                        String seguitId = doc.getString("seguitId");
                        if (!TextUtils.isEmpty(seguitId)) conductorsSeguits.add(seguitId);
                    }
                    aplicaFiltres();
                });
    }

    private void carregaViatges() {
        FirebaseUser usuariActual = auth.getCurrentUser();
        String uidActual = usuariActual == null ? "" : usuariActual.getUid();
        long ara = System.currentTimeMillis();

        db.collection(UtilitatsFirebase.COL_VIATGES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totsElsViatges.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Viatge viatge = document.toObject(Viatge.class);
                        viatge.setId(document.getId());

                        if (uidActual.equals(viatge.getConductorId())) continue;
                        if (!TextUtils.isEmpty(filtreConductorId)
                                && !filtreConductorId.equals(viatge.getConductorId())) continue;
                        if (!UtilitatsFirebase.ESTAT_VIATGE_DISPONIBLE.equalsIgnoreCase(viatge.getEstat())) continue;
                        if (viatge.getPlacesDisponibles() <= 0) continue;
                        if (viatge.getSortidaMillis() < ara - 2L * 60L * 60L * 1000L) {
                            marcaCompletat(viatge);
                            continue;
                        }
                        totsElsViatges.add(viatge);
                    }
                    totsElsViatges.sort(Comparator.comparingLong(Viatge::getSortidaMillis));
                    aplicaFiltres();
                });
    }

    private void carregaEstatReserves() {
        FirebaseUser usuari = auth.getCurrentUser();
        if (usuari == null) return;
        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("passatgerId", usuari.getUid())
                .get()
                .addOnSuccessListener(docs -> {
                    Map<String, String> mapa = new HashMap<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : docs) {
                        String viatgeId = doc.getString("viatgeId");
                        String estat = doc.getString("estat");
                        if (!TextUtils.isEmpty(viatgeId) && !TextUtils.isEmpty(estat)
                                && (UtilitatsFirebase.ESTAT_RESERVA_PENDENT.equals(estat)
                                || UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA.equals(estat))) {
                            mapa.put(viatgeId, estat);
                        }
                    }
                    adaptadorViatges.actualitzaEstatReserves(mapa);
                });
    }

    private void marcaCompletat(Viatge viatge) {
        if (viatge == null || TextUtils.isEmpty(viatge.getId())) return;
        db.collection(UtilitatsFirebase.COL_VIATGES)
                .document(viatge.getId())
                .update(
                        "estat", UtilitatsFirebase.ESTAT_VIATGE_COMPLETAT,
                        "completatMillis", System.currentTimeMillis()
                );
        String origen = viatge.getOrigen() != null ? viatge.getOrigen() : "";
        String desti = viatge.getDesti() != null ? viatge.getDesti() : "";
        String text = getString(R.string.notif_viatge_completat, origen, desti);
        String idNotificacio = "fi_" + viatge.getId();

        db.collection(UtilitatsFirebase.COL_RESERVES)
                .whereEqualTo("viatgeId", viatge.getId())
                .whereEqualTo("estat", UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA)
                .get()
                .addOnSuccessListener(docs -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : docs) {
                        String passatgerId = doc.getString("passatgerId");
                        String reservaId = doc.getId();
                        UtilitatsNotificacions.publicaAmbId(db, viatge.getConductorId(),
                                idNotificacio + "_cond_" + reservaId,
                                Notificacio.TIPUS_VIATGE_COMPLETAT,
                                text,
                                reservaId);
                        UtilitatsNotificacions.publicaAmbId(db, passatgerId,
                                idNotificacio + "_" + passatgerId,
                                Notificacio.TIPUS_VIATGE_COMPLETAT,
                                text,
                                reservaId);
                    }
                });
    }

    private void executaCerca() {
        String textOrigen = obteText(campOrigen);
        if (TextUtils.isEmpty(textOrigen)) {
            coordOrigen = null;
            aplicaFiltres();
            return;
        }
        PuntsMapaViBaCar.resolCoordenada(this, textOrigen, origen -> {
            coordOrigen = origen;
            if (origen == null) Toast.makeText(this, R.string.cerca_origen_no_resolt, Toast.LENGTH_SHORT).show();
            aplicaFiltres();
        });
    }

    private void netejaCerca() {
        campOrigen.setText("");
        dataSeleccionadaMillis = null;
        coordOrigen = null;
        coordDesti = null;
        textDesti = "";
        placesMinimes = 0;
        modeCerca = "propers";
        filtreDir = "totes";
        actualitzaBadge();
        aplicaFiltres();
    }

    private void obreFullFiltres() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View vista = LayoutInflater.from(this).inflate(R.layout.layout_full_filtres, null, false);
        sheet.setContentView(vista);

        TextView btPropers = vista.findViewById(R.id.botoFiltrePropers);
        TextView btTots = vista.findViewById(R.id.botoFiltreTots);
        TextView btSeguits = vista.findViewById(R.id.botoFiltreSeguits);
        syncMode(btPropers, btTots, btSeguits);
        btPropers.setOnClickListener(v -> { modeCerca = "propers"; syncMode(btPropers, btTots, btSeguits); });
        btTots.setOnClickListener(v -> { modeCerca = "tots"; syncMode(btPropers, btTots, btSeguits); });
        btSeguits.setOnClickListener(v -> { modeCerca = "seguits"; syncMode(btPropers, btTots, btSeguits); });

        TextView btDirTotes = vista.findViewById(R.id.botoDirTotesFull);
        TextView btDirInsti = vista.findViewById(R.id.botoDirInstiFull);
        TextView btDirTornada = vista.findViewById(R.id.botoDirTornadaFull);
        syncDir(btDirTotes, btDirInsti, btDirTornada);
        btDirTotes.setOnClickListener(v -> { filtreDir = "totes"; syncDir(btDirTotes, btDirInsti, btDirTornada); });
        btDirInsti.setOnClickListener(v -> { filtreDir = "insti"; syncDir(btDirTotes, btDirInsti, btDirTornada); });
        btDirTornada.setOnClickListener(v -> { filtreDir = "tornada"; syncDir(btDirTotes, btDirInsti, btDirTornada); });

        TextInputEditText campDestiSheet = vista.findViewById(R.id.campDestiSheet);
        campDestiSheet.setText(textDesti);

        TextInputEditText campDataSheet = vista.findViewById(R.id.campDataSheet);
        if (dataSeleccionadaMillis != null) campDataSheet.setText(UtilitatsData.formatData(dataSeleccionadaMillis));
        campDataSheet.setOnClickListener(v -> {
            Calendar ini = Calendar.getInstance();
            if (dataSeleccionadaMillis != null) ini.setTimeInMillis(dataSeleccionadaMillis);
            new DatePickerDialog(this, (dpv, year, month, day) -> {
                Calendar c = Calendar.getInstance();
                c.set(year, month, day, 0, 0, 0);
                c.set(Calendar.MILLISECOND, 0);
                dataSeleccionadaMillis = c.getTimeInMillis();
                campDataSheet.setText(UtilitatsData.formatData(dataSeleccionadaMillis));
            }, ini.get(Calendar.YEAR), ini.get(Calendar.MONTH), ini.get(Calendar.DAY_OF_MONTH)).show();
        });

        TextInputEditText campPlacesSheet = vista.findViewById(R.id.campPlacesSheet);
        if (placesMinimes > 0) campPlacesSheet.setText(String.valueOf(placesMinimes));

        vista.findViewById(R.id.botoAplicaFiltres).setOnClickListener(v -> {
            String nouTextDesti = campDestiSheet.getText() == null ? "" : campDestiSheet.getText().toString().trim();
            String nouTextPlaces = campPlacesSheet.getText() == null ? "" : campPlacesSheet.getText().toString().trim();
            placesMinimes = parseInt(nouTextPlaces);
            boolean destiCanviat = !nouTextDesti.equals(textDesti);
            textDesti = nouTextDesti;
            if (destiCanviat) coordDesti = null;
            sheet.dismiss();
            actualitzaBadge();
            if (destiCanviat && !TextUtils.isEmpty(textDesti)) {
                PuntsMapaViBaCar.resolCoordenada(this, textDesti, desti -> {
                    coordDesti = desti;
                    aplicaFiltres();
                });
            } else {
                aplicaFiltres();
            }
        });

        sheet.show();
    }

    private void syncMode(TextView btPropers, TextView btTots, TextView btSeguits) {
        pintaActiu(btPropers, "propers".equals(modeCerca));
        pintaActiu(btTots, "tots".equals(modeCerca));
        pintaActiu(btSeguits, "seguits".equals(modeCerca));
    }

    private void syncDir(TextView btTotes, TextView btInsti, TextView btTornada) {
        pintaActiu(btTotes, "totes".equals(filtreDir));
        pintaActiu(btInsti, "insti".equals(filtreDir));
        pintaActiu(btTornada, "tornada".equals(filtreDir));
    }

    private void pintaActiu(TextView boto, boolean actiu) {
        boto.setBackgroundResource(actiu ? R.drawable.fons_boto_principal : android.R.color.transparent);
        boto.setTextColor(getColor(actiu ? R.color.color_text_clar : R.color.color_text_secundari));
    }

    private void aplicaFiltres() {
        List<Viatge> filtrats = new ArrayList<>();
        long inici = dataSeleccionadaMillis != null ? inicideDia(dataSeleccionadaMillis) : 0L;
        long fi = dataSeleccionadaMillis != null ? inici + FINESTRA_DIA_MS : Long.MAX_VALUE;
        float radiMetres = RADI_KM * 1000f;

        for (Viatge viatge : totsElsViatges) {
            if ("seguits".equals(modeCerca) && !conductorsSeguits.contains(viatge.getConductorId())) continue;
            if ("insti".equals(filtreDir)) {
                String d = viatge.getDesti();
                if (d == null || !(d.toLowerCase().contains("vidal") || d.toLowerCase().contains("institut"))) continue;
            } else if ("tornada".equals(filtreDir)) {
                String o = viatge.getOrigen();
                if (o == null || !(o.toLowerCase().contains("vidal") || o.toLowerCase().contains("institut"))) continue;
            }
            if (placesMinimes > 0 && viatge.getPlacesDisponibles() < placesMinimes) continue;
            if (dataSeleccionadaMillis != null) {
                long sortida = viatge.getSortidaMillis();
                if (sortida < inici || sortida >= fi) continue;
            }
            if ("propers".equals(modeCerca) && coordOrigen != null) {
                LatLng origenViatge = new LatLng(viatge.getOrigenLat(), viatge.getOrigenLng());
                if (UtilitatsGeo.distanciaMetres(coordOrigen, origenViatge) > radiMetres) continue;
            }
            if (coordDesti != null) {
                LatLng destiViatge = new LatLng(viatge.getDestiLat(), viatge.getDestiLng());
                if (UtilitatsGeo.distanciaMetres(coordDesti, destiViatge) > radiMetres) continue;
            }
            filtrats.add(viatge);
        }
        if (coordOrigen != null) filtrats.sort(Comparator.comparingDouble(this::distanciaOrigen));
        adaptadorViatges.actualitzaDades(filtrats);
        txtBuit.setVisibility(filtrats.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void actualitzaBadge() {
        int count = 0;
        if (!"propers".equals(modeCerca)) count++;
        if (!"totes".equals(filtreDir)) count++;
        if (!TextUtils.isEmpty(textDesti)) count++;
        if (dataSeleccionadaMillis != null) count++;
        if (placesMinimes > 0) count++;

        if (count > 0) {
            txtInsignieFiltres.setVisibility(View.VISIBLE);
            txtInsignieFiltres.setText(String.valueOf(count));
            botoNetejar.setVisibility(View.VISIBLE);
        } else {
            txtInsignieFiltres.setVisibility(View.GONE);
            botoNetejar.setVisibility(View.GONE);
        }
    }

    private double distanciaOrigen(Viatge viatge) {
        if (coordOrigen == null) return 0d;
        return UtilitatsGeo.distanciaMetres(coordOrigen, new LatLng(viatge.getOrigenLat(), viatge.getOrigenLng()));
    }

    private long inicideDia(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private void configuraBotomNav() {
        UtilitatsBottomNav.marcaSeleccionada(this, UtilitatsBottomNav.SECCIO_BUSCAR);

        View navNotificacions = findViewById(R.id.navNotificacions);
        View navMissatges = findViewById(R.id.navMissatges);

        if (navNotificacions != null) {
            navNotificacions.setOnClickListener(v -> startActivity(new Intent(this, NotificacionsActivity.class)));
        }
        if (navMissatges != null) {
            navMissatges.setOnClickListener(v -> startActivity(new Intent(this, BustiaXatsActivity.class)));
        }
    }

    private String obteText(TextInputEditText camp) {
        return camp.getText() == null ? "" : camp.getText().toString().trim();
    }

    private int parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void obrePerfilConductor(Viatge viatge) {
        if (TextUtils.isEmpty(viatge.getConductorId())) return;
        Intent intent = new Intent(this, VeurePerfilActivity.class);
        intent.putExtra(VeurePerfilActivity.EXTRA_UID, viatge.getConductorId());
        startActivity(intent);
    }
}
