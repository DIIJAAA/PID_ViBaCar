package com.vidalibarraquer.vibacar.adaptadors;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Notificacio;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;

import java.util.ArrayList;
import java.util.List;

public class AdaptadorNotificacions extends RecyclerView.Adapter<AdaptadorNotificacions.NotifViewHolder> {

    public interface OnNotifClick {
        void onClick(Notificacio notif);
    }

    private final List<Notificacio> items = new ArrayList<>();
    private final OnNotifClick listener;

    public AdaptadorNotificacions(OnNotifClick listener) {
        this.listener = listener;
    }

    public void actualitzaDades(List<Notificacio> noves) {
        items.clear();
        items.addAll(noves);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notificacio, parent, false);
        return new NotifViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
        Notificacio notif = items.get(position);
        holder.txtText.setText(notif.getText());
        holder.txtData.setText(UtilitatsData.formatData(notif.getDataMillis()));
        holder.puntNoLlegit.setVisibility(notif.isLlegida() ? View.INVISIBLE : View.VISIBLE);
        holder.itemView.setOnClickListener(v -> listener.onClick(notif));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NotifViewHolder extends RecyclerView.ViewHolder {
        final TextView txtText;
        final TextView txtData;
        final View puntNoLlegit;

        NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            txtText = itemView.findViewById(R.id.txtTextNotif);
            txtData = itemView.findViewById(R.id.txtDataNotif);
            puntNoLlegit = itemView.findViewById(R.id.puntNoLlegit);
        }
    }
}
