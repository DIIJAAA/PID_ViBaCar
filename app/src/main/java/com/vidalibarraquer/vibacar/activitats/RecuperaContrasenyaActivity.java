package com.vidalibarraquer.vibacar.activitats;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
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
    private MaterialButton botoEnviaRecuperacio;
    private CircularProgressIndicator indicadorCarrega;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recupera_contrasenya);

        auth = FirebaseAuth.getInstance();
        campCorreu = findViewById(R.id.campCorreu);
        campCorreuConfirmacio = findViewById(R.id.campCorreuConfirmacio);
        txtMissatge = findViewById(R.id.txtMissatge);
        botoEnviaRecuperacio = findViewById(R.id.botoEnviaRecuperacio);
        indicadorCarrega = findViewById(R.id.indicadorCarrega);

        String correuInicial = getIntent().getStringExtra(EXTRA_CORREU_INICIAL);
        if (!TextUtils.isEmpty(correuInicial)) {
            campCorreu.setText(correuInicial);
        }

        findViewById(R.id.botoEnrere).setOnClickListener(v -> finish());
        botoEnviaRecuperacio.setOnClickListener(v -> enviaRecuperacio());
    }

    private void enviaRecuperacio() {
        txtMissatge.setText("");
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
        mostraCarrega(true);
        auth.sendPasswordResetEmail(correu)
                .addOnSuccessListener(unused -> {
                    mostraCarrega(false);
                    Toast.makeText(this, R.string.missatge_recuperacio_contrasenya, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    mostraCarrega(false);
                    txtMissatge.setText(R.string.error_generica);
                });
    }

    private String obteText(TextInputEditText camp) {
        return camp.getText() == null ? "" : camp.getText().toString().trim();
    }

    private void mostraCarrega(boolean actiu) {
        botoEnviaRecuperacio.setEnabled(!actiu);
        indicadorCarrega.setVisibility(actiu ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}
