package com.vidalibarraquer.vibacar.activitats;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.vidalibarraquer.vibacar.R;
import com.vidalibarraquer.vibacar.utilitats.GestorIdioma;
import com.vidalibarraquer.vibacar.utilitats.UtilitatsFirebase;

import java.util.Locale;

public class RecuperaContrasenyaActivity extends AppCompatActivity {

    public static final String EXTRA_CORREU_INICIAL = "correu_inicial";

    private FirebaseAuth auth;
    private TextInputEditText campCorreu;
    private TextInputEditText campCorreuConfirmacio;
    private TextView txtMissatge;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recupera_contrasenya);

        auth = FirebaseAuth.getInstance();
        campCorreu = findViewById(R.id.campCorreu);
        campCorreuConfirmacio = findViewById(R.id.campCorreuConfirmacio);
        txtMissatge = findViewById(R.id.txtMissatge);

        String correuInicial = getIntent().getStringExtra(EXTRA_CORREU_INICIAL);
        if (!TextUtils.isEmpty(correuInicial)) {
            campCorreu.setText(correuInicial);
        }

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        ((MaterialButton) findViewById(R.id.botoEnviaRecuperacio)).setOnClickListener(v -> enviaRecuperacio());
    }

    private void enviaRecuperacio() {
        String correu = obteText(campCorreu).toLowerCase(Locale.ROOT);
        String correuConfirmacio = obteText(campCorreuConfirmacio).toLowerCase(Locale.ROOT);
        if (!UtilitatsFirebase.esCorreuCentre(correu)) {
            txtMissatge.setText(R.string.error_domini_correu);
            return;
        }
        if (!correu.equals(correuConfirmacio)) {
            txtMissatge.setText(R.string.error_correus_diferents);
            return;
        }

        auth.setLanguageCode(GestorIdioma.obteIdiomaGuardat(this));
        auth.sendPasswordResetEmail(correu)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, R.string.missatge_recuperacio_contrasenya, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> txtMissatge.setText(R.string.error_generica));
    }

    private String obteText(TextInputEditText camp) {
        return camp.getText() == null ? "" : camp.getText().toString().trim();
    }
}
