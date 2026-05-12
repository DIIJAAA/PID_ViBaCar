package com.vidalibarraquer.vibacar.adaptadors;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Notificacio;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdaptadorNotificacions extends RecyclerView.Adapter<AdaptadorNotificacions.NotifViewHolder> {

    public interface OnNotifClick {
        void onClick(Notificacio notif);
    }

    public interface OnSeleccioCanvi {
        void onCanvi(int seleccionades);
    }

    private final List<Notificacio> items = new ArrayList<>();
    private final OnNotifClick listener;
    private final OnSeleccioCanvi seleccioListener;
    private final Set<String> seleccionades = new HashSet<>();
    private boolean modeSeleccio;

    public AdaptadorNotificacions(OnNotifClick listener, OnSeleccioCanvi seleccioListener) {
        this.listener = listener;
        this.seleccioListener = seleccioListener;
    }

    public void actualitzaDades(List<Notificacio> noves) {
        items.clear();
        items.addAll(noves);
        seleccionades.removeIf(id -> buscaPerId(id) == null);
        modeSeleccio = !seleccionades.isEmpty();
        notifyDataSetChanged();
        notificaSeleccio();
    }

    public boolean estaEnModeSeleccio() {
        return modeSeleccio;
    }

    public List<Notificacio> obteSeleccionades() {
        List<Notificacio> resultat = new ArrayList<>();
        for (Notificacio item : items) {
            if (seleccionades.contains(item.getId())) {
                resultat.add(item);
            }
        }
        return resultat;
    }

    public void seleccionaTotes() {
        seleccionades.clear();
        for (Notificacio item : items) {
            seleccionades.add(item.getId());
        }
        modeSeleccio = !seleccionades.isEmpty();
        notifyDataSetChanged();
        notificaSeleccio();
    }

    public void netejaSeleccio() {
        seleccionades.clear();
        modeSeleccio = false;
        notifyDataSetChanged();
        notificaSeleccio();
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
        boolean seleccionada = seleccionades.contains(notif.getId());
        holder.checkSeleccio.setVisibility(modeSeleccio ? View.VISIBLE : View.GONE);
        holder.checkSeleccio.setChecked(seleccionada);
        holder.itemView.setSelected(seleccionada);
        holder.itemView.setOnClickListener(v -> {
            if (modeSeleccio) {
                alternaSeleccio(notif);
            } else {
                listener.onClick(notif);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            alternaSeleccio(notif);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void alternaSeleccio(Notificacio notif) {
        if (notif == null || notif.getId() == null) return;
        if (seleccionades.contains(notif.getId())) {
            seleccionades.remove(notif.getId());
        } else {
            seleccionades.add(notif.getId());
        }
        modeSeleccio = !seleccionades.isEmpty();
        notifyDataSetChanged();
        notificaSeleccio();
    }

    private Notificacio buscaPerId(String id) {
        for (Notificacio item : items) {
            if (id != null && id.equals(item.getId())) return item;
        }
        return null;
    }

    private void notificaSeleccio() {
        if (seleccioListener != null) {
            seleccioListener.onCanvi(seleccionades.size());
        }
    }

    static class NotifViewHolder extends RecyclerView.ViewHolder {
        final TextView txtText;
        final TextView txtData;
        final View puntNoLlegit;
        final CheckBox checkSeleccio;

        NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            txtText = itemView.findViewById(R.id.txtTextNotif);
            txtData = itemView.findViewById(R.id.txtDataNotif);
            puntNoLlegit = itemView.findViewById(R.id.puntNoLlegit);
            checkSeleccio = itemView.findViewById(R.id.checkSeleccio);
        }
    }
}
