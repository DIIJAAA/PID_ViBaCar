package com.vidalibarraquer.vibacar.activitats;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.utilitats.GestorIdioma;

public class PantallaBenvingudaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_benvinguda);

        MaterialButton botoCrearCompte = findViewById(R.id.botoCrearCompte);
        MaterialButton botoIniciarSessio = findViewById(R.id.botoIniciarSessio);
        ImageButton botoIdioma = findViewById(R.id.botoIdioma);

        if (botoIdioma != null) {
            botoIdioma.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(PantallaBenvingudaActivity.this, v);
                popup.getMenu().add(0, 0, 0, "Català");
                popup.getMenu().add(0, 1, 1, "Castellano");
                popup.getMenu().add(0, 2, 2, "English");

                popup.setOnMenuItemClickListener(item -> {
                    String codi;
                    switch (item.getItemId()) {
                        case 1: codi = "es"; break;
                        case 2: codi = "en"; break;
                        default: codi = "ca"; break;
                    }
                    GestorIdioma.guardaIAplica(PantallaBenvingudaActivity.this, codi);
                    recreate();
                    return true;
                });
                popup.show();
            });
        }

        botoCrearCompte.setOnClickListener(v -> startActivity(new Intent(this, CrearCompteActivity.class)));
        botoIniciarSessio.setOnClickListener(v -> startActivity(new Intent(this, IniciSessioActivity.class)));
    }
}
