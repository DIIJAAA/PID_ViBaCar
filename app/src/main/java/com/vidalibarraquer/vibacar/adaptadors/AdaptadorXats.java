package com.vidalibarraquer.vibacar.adaptadors;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.models.Xat;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdaptadorXats extends RecyclerView.Adapter<AdaptadorXats.XatViewHolder> {

    public interface OnXatClickListener {
        void onXatClick(Xat xat, String nomMostrat);
    }

    public interface OnAvatarClickListener {
        void onAvatarClick(String uid);
    }

    private final Context context;
    private final String uidActual;
    private final OnXatClickListener listener;
    private final OnAvatarClickListener avatarListener;
    private final List<Xat> xats = new ArrayList<>();

    public AdaptadorXats(Context context, String uidActual, OnXatClickListener listener) {
        this(context, uidActual, listener, null);
    }

    public AdaptadorXats(Context context, String uidActual, OnXatClickListener listener, OnAvatarClickListener avatarListener) {
        this.context = context;
        this.uidActual = uidActual;
        this.listener = listener;
        this.avatarListener = avatarListener;
    }

    public void actualitzaDades(List<Xat> dadesNoves) {
        xats.clear();
        xats.addAll(dadesNoves);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public XatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_xat, parent, false);
        return new XatViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull XatViewHolder holder, int position) {
        Xat xat = xats.get(position);
        boolean socConductor = uidActual != null && uidActual.equals(xat.getConductorId());
        String nomAltre = socConductor ? valorDefecte(xat.getNomPassatger()) : valorDefecte(xat.getNomConductor());

        holder.txtNom.setText(nomAltre);
        holder.txtInicial.setText(inicial(nomAltre));

        if (TextUtils.isEmpty(xat.getDarrerMissatge())) {
            holder.txtDarrerMissatge.setText(R.string.xat_sense_missatges);
        } else {
            boolean meu = uidActual != null && uidActual.equals(xat.getDarrerEmissorId());
            holder.txtDarrerMissatge.setText(meu
                    ? context.getString(R.string.xat_darrer_missatge_meu_format, xat.getDarrerMissatge())
                    : xat.getDarrerMissatge());
        }

        if (xat.getDarreraActualitzacio() > 0) {
            holder.txtHora.setText(UtilitatsData.formatHora(xat.getDarreraActualitzacio()));
            holder.txtHora.setVisibility(View.VISIBLE);
        } else {
            holder.txtHora.setVisibility(View.GONE);
        }

        String altreUid = socConductor ? xat.getPassatgerId() : xat.getConductorId();
        holder.txtInicial.setOnClickListener(v -> {
            if (avatarListener != null && altreUid != null) avatarListener.onAvatarClick(altreUid);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onXatClick(xat, nomAltre);
            }
        });
    }

    @Override
    public int getItemCount() {
        return xats.size();
    }

    private String valorDefecte(String text) {
        if (text == null || text.trim().isEmpty()) {
            return context.getString(R.string.text_usuari);
        }
        return text.trim();
    }

    private String inicial(String text) {
        if (TextUtils.isEmpty(text)) {
            return "?";
        }
        return text.trim().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    static class XatViewHolder extends RecyclerView.ViewHolder {
        final TextView txtInicial;
        final TextView txtNom;
        final TextView txtDarrerMissatge;
        final TextView txtHora;

        XatViewHolder(@NonNull View itemView) {
            super(itemView);
            txtInicial = itemView.findViewById(R.id.txtInicial);
            txtNom = itemView.findViewById(R.id.txtNom);
            txtDarrerMissatge = itemView.findViewById(R.id.txtDarrerMissatge);
            txtHora = itemView.findViewById(R.id.txtHora);
        }
    }
}
