package com.vidalibarraquer.vibacar.utilitats;

import android.app.Activity;
import android.graphics.Typeface;
import android.widget.ImageView;
import android.widget.TextView;

import com.vidalibarraquer.vibacar.R;

public final class UtilitatsBottomNav {

    public static final int SECCIO_BUSCAR = 1;
    public static final int SECCIO_NOTIFICACIONS = 2;
    public static final int SECCIO_MISSATGES = 3;

    private UtilitatsBottomNav() {}

    public static void marcaSeleccionada(Activity activity, int seccio) {
        pintaOpcio(activity, R.id.iconaBuscar, R.id.textNavBuscar, seccio == SECCIO_BUSCAR);
        pintaOpcio(activity, R.id.iconaNotificacions, R.id.textNavNotificacions, seccio == SECCIO_NOTIFICACIONS);
        pintaOpcio(activity, R.id.iconaMissatges, R.id.textNavMissatges, seccio == SECCIO_MISSATGES);
    }

    private static void pintaOpcio(Activity activity, int idIcona, int idText, boolean seleccionada) {
        int color = activity.getColor(seleccionada ? R.color.color_principal : R.color.color_text_secundari);
        ImageView icona = activity.findViewById(idIcona);
        TextView text = activity.findViewById(idText);

        if (icona != null) {
            icona.setColorFilter(color);
        }
        if (text != null) {
            text.setTextColor(color);
            text.setTypeface(Typeface.DEFAULT, seleccionada ? Typeface.BOLD : Typeface.NORMAL);
        }
    }
}
