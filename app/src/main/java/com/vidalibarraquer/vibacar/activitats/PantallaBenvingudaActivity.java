package com.vidalibarraquer.vibacar.activitats;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.utilitats.GestorIdioma;

public class PantallaBenvingudaActivity extends AppCompatActivity {

    private MaterialAutoCompleteTextView campIdioma;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_benvinguda);

        campIdioma = findViewById(R.id.campIdioma);
        MaterialButton botoCrearCompte = findViewById(R.id.botoCrearCompte);
        MaterialButton botoIniciarSessio = findViewById(R.id.botoIniciarSessio);

        configuraSelectorIdiomes();

        botoCrearCompte.setOnClickListener(v -> startActivity(new Intent(this, CrearCompteActivity.class)));
        botoIniciarSessio.setOnClickListener(v -> startActivity(new Intent(this, IniciSessioActivity.class)));
    }

    private void configuraSelectorIdiomes() {
        String[] noms = getResources().getStringArray(R.array.idiomes_llista);
        String[] codis = getResources().getStringArray(R.array.idiomes_codis);

        campIdioma.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, noms));

        String idiomaActual = GestorIdioma.obteIdiomaGuardat(this);
        int posicio = 0;
        for (int i = 0; i < codis.length; i++) {
            if (codis[i].equals(idiomaActual)) {
                posicio = i;
                break;
            }
        }

        campIdioma.setText(noms[posicio], false);
        campIdioma.setOnItemClickListener((parent, view, position, id) -> {
            GestorIdioma.guardaIAplica(this, codis[position]);
            recreate();
        });
    }
}
