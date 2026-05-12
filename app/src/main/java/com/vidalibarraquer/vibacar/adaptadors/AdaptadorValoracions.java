package com.vidalibarraquer.vibacar.adaptadors;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Valoracio;

import java.util.ArrayList;
import java.util.List;

public class AdaptadorValoracions extends RecyclerView.Adapter<AdaptadorValoracions.ValoracioViewHolder> {

    private final List<Valoracio> valoracions = new ArrayList<>();

    public void actualitzaDades(List<Valoracio> novesValoracions) {
        valoracions.clear();
        valoracions.addAll(novesValoracions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ValoracioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_valoracio, parent, false);
        return new ValoracioViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ValoracioViewHolder holder, int position) {
        Valoracio valoracio = valoracions.get(position);
        holder.txtAutor.setText(TextUtils.isEmpty(valoracio.getAutorNom())
                ? holder.itemView.getContext().getString(R.string.text_usuari)
                : valoracio.getAutorNom());
        holder.barraPuntuacio.setRating(valoracio.getPuntuacio());

        String origen = TextUtils.isEmpty(valoracio.getOrigen()) ? "" : valoracio.getOrigen();
        String desti = TextUtils.isEmpty(valoracio.getDesti()) ? "" : valoracio.getDesti();
        holder.txtRuta.setText(holder.itemView.getContext().getString(R.string.text_ruta_format, origen, desti));

        if (TextUtils.isEmpty(valoracio.getComentari())) {
            holder.txtComentari.setVisibility(View.GONE);
        } else {
            holder.txtComentari.setVisibility(View.VISIBLE);
            holder.txtComentari.setText(valoracio.getComentari());
        }
    }

    @Override
    public int getItemCount() {
        return valoracions.size();
    }

    static class ValoracioViewHolder extends RecyclerView.ViewHolder {
        final TextView txtAutor;
        final TextView txtRuta;
        final TextView txtComentari;
        final RatingBar barraPuntuacio;

        ValoracioViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAutor = itemView.findViewById(R.id.txtAutor);
            txtRuta = itemView.findViewById(R.id.txtRuta);
            txtComentari = itemView.findViewById(R.id.txtComentari);
            barraPuntuacio = itemView.findViewById(R.id.barraPuntuacio);
        }
    }
}
