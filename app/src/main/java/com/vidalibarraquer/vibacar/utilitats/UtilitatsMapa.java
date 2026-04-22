package com.vidalibarraquer.vibacar.utilitats;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.vidalibarraquer.vibacar.R;

public final class UtilitatsMapa {

    private UtilitatsMapa() {
    }

    public static void dibuixaRuta(Context context,
                                   @Nullable GoogleMap mapa,
                                   @Nullable LatLng origen,
                                   @Nullable LatLng desti,
                                   @Nullable String textOrigen,
                                   @Nullable String textDesti,
                                   @Nullable TextView txtAlternatiu) {
        if (mapa == null) {
            return;
        }

        mapa.clear();
        if (txtAlternatiu != null) {
            txtAlternatiu.setVisibility(android.view.View.GONE);
        }

        if (origen == null || desti == null) {
            if (txtAlternatiu != null) {
                txtAlternatiu.setText(R.string.text_mapa_sense_dades);
                txtAlternatiu.setVisibility(android.view.View.VISIBLE);
            }
            return;
        }

        mapa.addMarker(new MarkerOptions()
                .position(origen)
                .title(textOrigen)
                .snippet(context.getString(R.string.mapa_inici)));

        mapa.addMarker(new MarkerOptions()
                .position(desti)
                .title(textDesti)
                .snippet(context.getString(R.string.mapa_desti))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        mapa.addPolyline(new PolylineOptions()
                .add(origen, desti)
                .width(10f)
                .color(ContextCompat.getColor(context, R.color.color_principal)));

        LatLngBounds limits = new LatLngBounds.Builder()
                .include(origen)
                .include(desti)
                .build();

        mapa.setOnMapLoadedCallback(() ->
                mapa.animateCamera(CameraUpdateFactory.newLatLngBounds(limits, 120))
        );
    }
}
