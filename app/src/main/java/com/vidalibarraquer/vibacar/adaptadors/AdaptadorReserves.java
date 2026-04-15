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
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Reserva;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;

import java.util.ArrayList;
import java.util.List;

public class AdaptadorReserves extends RecyclerView.Adapter<AdaptadorReserves.ReservaViewHolder> {

    public interface AlPuntuarReserva {
        void onPuntua(Reserva reserva);
    }

    private final Context context;
    private final AlPuntuarReserva listener;
    private final List<Reserva> reserves = new ArrayList<>();
    private boolean mostraPassats;

    public AdaptadorReserves(Context context, AlPuntuarReserva listener) {
        this.context = context;
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
        holder.txtTitol.setText(context.getString(R.string.text_ruta_format, reserva.getOrigen(), reserva.getDesti()));
        holder.txtSubtitol.setText(context.getString(
                R.string.text_reserva_subtitol_format,
                UtilitatsData.formatData(reserva.getSortidaMillis()),
                reserva.getConductorNom()
        ));

        holder.txtEstat.setText(mostraPassats ? R.string.text_estat_passat : R.string.text_estat_proper);
        holder.barraValoracio.setRating(reserva.getPuntuacio());
        holder.botoPuntuar.setVisibility(mostraPassats && !reserva.isValorada() ? View.VISIBLE : View.GONE);
        holder.botoPuntuar.setOnClickListener(v -> listener.onPuntua(reserva));
    }

    @Override
    public int getItemCount() {
        return reserves.size();
    }

    static class ReservaViewHolder extends RecyclerView.ViewHolder {
        final TextView txtTitol;
        final TextView txtSubtitol;
        final RatingBar barraValoracio;
        final TextView txtEstat;
        final MaterialButton botoPuntuar;

        ReservaViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitol = itemView.findViewById(R.id.txtTitol);
            txtSubtitol = itemView.findViewById(R.id.txtSubtitol);
            barraValoracio = itemView.findViewById(R.id.barraValoracio);
            txtEstat = itemView.findViewById(R.id.txtEstat);
            botoPuntuar = itemView.findViewById(R.id.botoPuntuar);
        }
    }
}
