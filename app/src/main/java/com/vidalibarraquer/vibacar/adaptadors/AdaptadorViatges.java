package com.vidalibarraquer.vibacar.adaptadors;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Viatge;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsAvatar;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;

import java.util.ArrayList;
import java.util.List;

public class AdaptadorViatges extends RecyclerView.Adapter<AdaptadorViatges.ViatgeViewHolder> {

    public interface AlFerClickViatge {
        void onViatgeClick(Viatge viatge);
    }

    public interface AlFerClickConductor {
        void onConductorClick(Viatge viatge);
    }

    private final Context context;
    private final AlFerClickViatge listener;
    private final AlFerClickConductor conductorListener;
    private final List<Viatge> viatges = new ArrayList<>();

    public AdaptadorViatges(Context context, AlFerClickViatge listener) {
        this(context, listener, null);
    }

    public AdaptadorViatges(Context context, AlFerClickViatge listener, AlFerClickConductor conductorListener) {
        this.context = context;
        this.listener = listener;
        this.conductorListener = conductorListener;
    }

    public void actualitzaDades(List<Viatge> dadesNoves) {
        viatges.clear();
        viatges.addAll(dadesNoves);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViatgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_viatge, parent, false);
        return new ViatgeViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ViatgeViewHolder holder, int position) {
        Viatge viatge = viatges.get(position);

        holder.txtNomConductor.setText(viatge.getConductorNom());
        holder.txtValoracio.setText(context.getString(
                R.string.text_valoracio_format,
                viatge.getConductorValoracio(),
                (int) viatge.getConductorValoracions()
        ));
        holder.txtOrigen.setText(viatge.getOrigen());
        holder.txtDesti.setText(viatge.getDesti());
        holder.txtData.setText(UtilitatsData.formatData(viatge.getSortidaMillis()));
        holder.txtHoraris.setText(context.getString(
                R.string.text_hora_range_format,
                UtilitatsData.formatHora(viatge.getSortidaMillis()),
                UtilitatsData.formatHora(viatge.getArribadaMillis())
        ));
        holder.txtPlaces.setText(context.getString(R.string.text_places_format, viatge.getPlacesDisponibles()));
        holder.txtPreu.setText(UtilitatsData.formatPreu(viatge.getAportacio()));

        UtilitatsAvatar.mostraAvatar(
                holder.imatgeConductor,
                holder.txtInicialConductor,
                viatge.getConductorFotoUri(),
                viatge.getConductorNom()
        );

        holder.itemView.setOnClickListener(v -> listener.onViatgeClick(viatge));
        View.OnClickListener obrePerfil = v -> {
            if (conductorListener != null) conductorListener.onConductorClick(viatge);
        };
        holder.imatgeConductor.setOnClickListener(obrePerfil);
        holder.txtInicialConductor.setOnClickListener(obrePerfil);
        holder.txtNomConductor.setOnClickListener(obrePerfil);
    }

    @Override
    public int getItemCount() {
        return viatges.size();
    }

    static class ViatgeViewHolder extends RecyclerView.ViewHolder {

        final ShapeableImageView imatgeConductor;
        final TextView txtInicialConductor;
        final TextView txtNomConductor;
        final TextView txtValoracio;
        final TextView txtPreu;
        final TextView txtOrigen;
        final TextView txtDesti;
        final TextView txtData;
        final TextView txtHoraris;
        final TextView txtPlaces;

        ViatgeViewHolder(@NonNull View itemView) {
            super(itemView);
            imatgeConductor = itemView.findViewById(R.id.imatgeConductor);
            txtInicialConductor = itemView.findViewById(R.id.txtInicialConductor);
            txtNomConductor = itemView.findViewById(R.id.txtNomConductor);
            txtValoracio = itemView.findViewById(R.id.txtValoracio);
            txtPreu = itemView.findViewById(R.id.txtPreu);
            txtOrigen = itemView.findViewById(R.id.txtOrigen);
            txtDesti = itemView.findViewById(R.id.txtDesti);
            txtData = itemView.findViewById(R.id.txtData);
            txtHoraris = itemView.findViewById(R.id.txtHoraris);
            txtPlaces = itemView.findViewById(R.id.txtPlaces);
        }
    }
}
