package com.vidalibarraquer.vibacar.adaptadors;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Reserva;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

import java.util.ArrayList;
import java.util.List;

public class AdaptadorReserves extends RecyclerView.Adapter<AdaptadorReserves.ReservaViewHolder> {

    public interface AccionsReservaListener {
        void onPuntua(Reserva reserva);

        void onAccepta(Reserva reserva);

        void onRebutja(Reserva reserva);

        void onCancela(Reserva reserva);

        void onObreXat(Reserva reserva);

        void onVeurePerfil(Reserva reserva);
    }

    private final Context context;
    private final AccionsReservaListener listener;
    private final List<Reserva> reserves = new ArrayList<>();
    private final String uidActual;
    private boolean mostraPassats;

    public AdaptadorReserves(Context context, String uidActual, AccionsReservaListener listener) {
        this.context = context;
        this.uidActual = uidActual;
        this.listener = listener;
    }

    public void actualitzaDades(List<Reserva> dadesNoves, boolean mostraPassats) {
        this.mostraPassats = mostraPassats;
        reserves.clear();
        reserves.addAll(dadesNoves);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReservaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reserva, parent, false);
        return new ReservaViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservaViewHolder holder, int position) {
        Reserva reserva = reserves.get(position);
        boolean socConductor = uidActual != null && uidActual.equals(reserva.getConductorId());
        reserva.setSocConductor(socConductor);

        String nomAltre = socConductor
                ? valorDefecte(reserva.getPassatgerNom())
                : valorDefecte(reserva.getConductorNom());
        String rolAltre = socConductor
                ? context.getString(R.string.etiqueta_rol_passatger)
                : context.getString(R.string.etiqueta_rol_conductor);

        String fotoAltre = socConductor ? reserva.getPassatgerFotoUri() : reserva.getConductorFotoUri();
        UtilitatsAvatar.mostraAvatar(holder.imatgeAltre, holder.txtInicialAltre, fotoAltre, nomAltre);
        holder.txtNomAltre.setText(nomAltre);
        holder.txtRol.setText(rolAltre);
        holder.txtEstat.setText(textEstat(reserva.getEstat()));
        holder.txtOrigen.setText(valorDefecte(reserva.getOrigen()));
        holder.txtDesti.setText(valorDefecte(reserva.getDesti()));
        holder.txtData.setText(UtilitatsData.formatData(reserva.getSortidaMillis()));
        holder.txtHorari.setText(UtilitatsData.formatHora(reserva.getSortidaMillis()));

        float puntuacioVisible = socConductor ? reserva.getConductorPuntuacio() : reserva.getPuntuacio();
        holder.barraValoracio.setRating(puntuacioVisible);
        holder.barraValoracio.setVisibility(mostraPassats && puntuacioVisible > 0f ? View.VISIBLE : View.GONE);

        amagaBotons(holder);

        boolean esPendent = UtilitatsFirebase.ESTAT_RESERVA_PENDENT.equals(reserva.getEstat());
        boolean esAcceptada = UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA.equals(reserva.getEstat());

        if (mostraPassats && !socConductor && esAcceptada && !reserva.isValorada()) {
            holder.botoPuntuar.setVisibility(View.VISIBLE);
            holder.botoPuntuar.setOnClickListener(v -> listener.onPuntua(reserva));
        }

        if (mostraPassats && socConductor && esAcceptada && !reserva.isConductorValorada()) {
            holder.botoPuntuar.setVisibility(View.VISIBLE);
            holder.botoPuntuar.setOnClickListener(v -> listener.onPuntua(reserva));
        }

        if (!mostraPassats && socConductor && esPendent) {
            mostraBoto(holder.botoPrincipal, R.string.boto_acceptar_reserva, v -> listener.onAccepta(reserva));
            mostraBoto(holder.botoSecundari, R.string.boto_rebutjar_reserva, v -> listener.onRebutja(reserva));
            mostraBoto(holder.botoPuntuar, R.string.boto_veure_perfil, v -> listener.onVeurePerfil(reserva));
            return;
        }

        if (!mostraPassats && !socConductor && esPendent) {
            mostraBoto(holder.botoPrincipal, R.string.boto_cancelar_reserva, v -> listener.onCancela(reserva));
            return;
        }

        if (!mostraPassats && esAcceptada) {
            mostraBoto(holder.botoPrincipal, R.string.boto_obrir_xat, v -> listener.onObreXat(reserva));
        }
    }

    @Override
    public int getItemCount() {
        return reserves.size();
    }

    private void amagaBotons(ReservaViewHolder holder) {
        holder.botoPrincipal.setVisibility(View.GONE);
        holder.botoSecundari.setVisibility(View.GONE);
        holder.botoPuntuar.setVisibility(View.GONE);
        holder.botoPrincipal.setOnClickListener(null);
        holder.botoSecundari.setOnClickListener(null);
        holder.botoPuntuar.setOnClickListener(null);
    }

    private void mostraBoto(MaterialButton boto, int idText, View.OnClickListener accio) {
        boto.setText(idText);
        boto.setVisibility(View.VISIBLE);
        boto.setOnClickListener(accio);
    }

    private String valorDefecte(String text) {
        if (text == null || text.trim().isEmpty()) {
            return context.getString(R.string.text_usuari);
        }
        return text.trim();
    }

    private String textEstat(String estat) {
        if (UtilitatsFirebase.ESTAT_RESERVA_PENDENT.equals(estat)) {
            return context.getString(R.string.estat_pendent);
        }
        if (UtilitatsFirebase.ESTAT_RESERVA_ACCEPTADA.equals(estat)) {
            return context.getString(R.string.estat_acceptada);
        }
        if (UtilitatsFirebase.ESTAT_RESERVA_REBUTJADA.equals(estat)) {
            return context.getString(R.string.estat_rebutjada);
        }
        if (UtilitatsFirebase.ESTAT_RESERVA_CANCELADA.equals(estat)) {
            return context.getString(R.string.estat_cancelada);
        }
        return context.getString(R.string.text_no_definit);
    }

    static class ReservaViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView imatgeAltre;
        final TextView txtInicialAltre;
        final TextView txtNomAltre;
        final TextView txtRol;
        final TextView txtEstat;
        final TextView txtOrigen;
        final TextView txtDesti;
        final TextView txtData;
        final TextView txtHorari;
        final RatingBar barraValoracio;
        final MaterialButton botoPrincipal;
        final MaterialButton botoSecundari;
        final MaterialButton botoPuntuar;

        ReservaViewHolder(@NonNull View itemView) {
            super(itemView);
            imatgeAltre = itemView.findViewById(R.id.imatgeAltre);
            txtInicialAltre = itemView.findViewById(R.id.txtInicialAltre);
            txtNomAltre = itemView.findViewById(R.id.txtNomAltre);
            txtRol = itemView.findViewById(R.id.txtRol);
            txtEstat = itemView.findViewById(R.id.txtEstat);
            txtOrigen = itemView.findViewById(R.id.txtOrigen);
            txtDesti = itemView.findViewById(R.id.txtDesti);
            txtData = itemView.findViewById(R.id.txtData);
            txtHorari = itemView.findViewById(R.id.txtHorari);
            barraValoracio = itemView.findViewById(R.id.barraValoracio);
            botoPrincipal = itemView.findViewById(R.id.botoPrincipal);
            botoSecundari = itemView.findViewById(R.id.botoSecundari);
            botoPuntuar = itemView.findViewById(R.id.botoPuntuar);
        }
    }
}
