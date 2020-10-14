package com.bonacogo.gameplate.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.util.GeneralMethod;
import com.google.firebase.auth.FirebaseAuth;

public class PasswordResetDialog {
    // questa classe non estende la classe dialog ma esegue quanto richiesto per crearne una e fare
    // quanto serve per l'invio della mail di reset della password
    private static final String TAG = "PasswordResetDialog";
    private EditText email;
    private Dialog dialog;
    private Context context;

    private ProgressDialog mProgressDialog;

    public void showDialog(Context context) {
        this.context = context;
        // creo la dialog
        dialog = new Dialog(context);

        // setup
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(R.layout.password_reset_layout);

        // views
        email = dialog.findViewById(R.id.email);
        Button mDialogNo = dialog.findViewById(R.id.no);
        Button mDialogOk = dialog.findViewById(R.id.yes);

        mDialogNo.setOnClickListener(v -> dialog.dismiss());

        mDialogOk.setOnClickListener(v -> resetPassword());

        dialog.show();
    }

    private void resetPassword() {
        // se il form non è validato ritorna
        if (!validateForm()) {
            return;
        }
        // mostra la progressdialog
        mProgressDialog = GeneralMethod.showProgressDialog(mProgressDialog, context, context.getString(R.string.loading));
        // auth firebase
        FirebaseAuth auth = FirebaseAuth.getInstance();
        // email a cui mandare il link di reset
        String emailAddress = email.getText().toString();
        // invia il link di reset della password all'email emailAddress
        auth.sendPasswordResetEmail(emailAddress)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "sendPasswordResetEmail:success");
                        Toast.makeText(context, context.getString(R.string.email_sent_to)+emailAddress,
                                Toast.LENGTH_LONG).show();
                    }
                    else {
                        Log.e(TAG, "sendPasswordResetEmail:failure", task.getException());
                        Toast.makeText(context, R.string.error_mail_not_sent,
                                Toast.LENGTH_LONG).show();
                    }
                    // nascondi la progressdialog
                    GeneralMethod.hideProgressDialog(mProgressDialog);
                    dialog.cancel();
                });
    }

    // validazione del form
    private boolean validateForm() {
        // controllo preliminare
        boolean fieldCheck = validateField(email);
        if (fieldCheck) {
            boolean check = true;
            String email_txt = email.getText().toString();
            // se il controllo preliminare è passato controlla che la mail sia benformata
            if (!Patterns.EMAIL_ADDRESS.matcher(email_txt).matches()) {
                email.setError("Email non valida.");
                check = false;
            }
            return check;
        }
        return false;
    }

    // controllo preliminare che il campo non sia vuoto
    private boolean validateField(EditText field) {
        String txt = field.getText().toString();
        if (TextUtils.isEmpty(txt)) {
            field.setError("Campo obbligatorio.");
            return false;
        } else {
            field.setError(null);
            return true;
        }
    }
}