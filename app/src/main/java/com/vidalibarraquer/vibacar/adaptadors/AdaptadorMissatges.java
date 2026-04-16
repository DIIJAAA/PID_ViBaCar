package com.vidalibarraquer.vibacar.adaptadors;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Missatge;

import java.util.ArrayList;
import java.util.List;

public class AdaptadorMissatges extends RecyclerView.Adapter<AdaptadorMissatges.MissatgeViewHolder> {

    private static final int TIPUS_MEU = 1;
    private static final int TIPUS_ALTRE = 2;

    private final String usuariActualId;
    private final List<Missatge> missatges = new ArrayList<>();

    public AdaptadorMissatges(String usuariActualId) {
        this.usuariActualId = usuariActualId;
    }

    public void actualitzaDades(List<Missatge> dadesNoves) {
        missatges.clear();
        missatges.addAll(dadesNoves);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return missatges.get(position).getEmissorId().equals(usuariActualId) ? TIPUS_MEU : TIPUS_ALTRE;
    }

    @NonNull
    @Override
    public MissatgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TIPUS_MEU ? R.layout.item_missatge_meu : R.layout.item_missatge_altre;
        View vista = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MissatgeViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull MissatgeViewHolder holder, int position) {
        holder.txtMissatge.setText(missatges.get(position).getText());
    }

    @Override
    public int getItemCount() {
        return missatges.size();
    }

    static class MissatgeViewHolder extends RecyclerView.ViewHolder {
        final TextView txtMissatge;

        MissatgeViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMissatge = itemView.findViewById(R.id.txtMissatge);
        }
    }
}
