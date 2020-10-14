package com.bonacogo.gameplate.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.util.GeneralMethod;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

public class NickChangeDialog extends Dialog {
    private static final String TAG = "NickChangeDialog";
    private ProgressDialog mProgressDialog;
    private FirebaseFunctions mFunctions;
    private String nickAlreadyUsed;
    private EditText nickname;
    private boolean error;

    private DialogListener listener;

    public interface DialogListener {
        void onCompleted();
        void onCanceled(boolean error);
    }

    public NickChangeDialog(@NonNull Context context, String nickAlreadyUsed, boolean error, DialogListener listener) {
        super(context);
        this.nickAlreadyUsed = nickAlreadyUsed;
        this.listener = listener;
        this.error = error;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setup
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.nick_change_layout);
        this.setCancelable(false);

        // views
        nickname = findViewById(R.id.nickname);
        Button mDialogNo = findViewById(R.id.no);
        Button mDialogOk = findViewById(R.id.yes);

        // functions firebase
        this.mFunctions = FirebaseFunctions.getInstance();

        // impostazione del vecchio nick con o senza errore
        if (nickAlreadyUsed != null) {
            nickname.setText(nickAlreadyUsed);
            if (error)
                nickname.setError(getContext().getString(R.string.nickname_already_used));
        }

        mDialogOk.setOnClickListener(v -> {
            if(listener != null)
                changeNick(nickname);
        });

        mDialogNo.setOnClickListener(v -> close(false));
    }

    private void changeNick(EditText nickname) {
        // se il form non è validato ritorna
        if (!validateForm(nickname))
            return;

        // mostra la progressdialog
        mProgressDialog = GeneralMethod.showProgressDialog(mProgressDialog, getContext(), getContext().getString(R.string.loading));

        // creo l'oggetto da dare alla funzione Firebase
        final Map<String, Object> data = new HashMap<>();
        String nick = nickname.getText().toString();
        data.put("nickname", nick);
        // chiamo il metodo per chiamare la funzione
        onChangeNick(data)
                .addOnCompleteListener(task -> {
                    // nascondo la progressdialog
                    GeneralMethod.hideProgressDialog(mProgressDialog);
                    if (task.isSuccessful()) {
                        boolean changed = task.getResult();
                        if (!changed)
                            // se la funzione ritorna false il nick è già utilizzato
                            nickname.setError("Nickname già utilizzato");
                        else {
                            // se la funzione ritorna true il nick è stato cambiato con successo
                            FirebaseAuth mAuth = FirebaseAuth.getInstance();
                            FirebaseUser user = mAuth.getCurrentUser();
                            // fai un reload dell'utente per caricare i metadati aggiornati
                            user.reload().addOnCompleteListener(task2 -> {
                                if (task2.isSuccessful()) {
                                    Toast.makeText(getContext(), "Nickname impostato",
                                            Toast.LENGTH_LONG).show();
                                    // chiama il callback
                                    listener.onCompleted();
                                    // dismiss della dialog
                                    NickChangeDialog.this.dismiss();
                                } else {
                                    Log.e(TAG, "user.reload:onFailure", task2.getException());
                                    close(true);
                                }
                            });
                        }
                    }
                    else {
                        Exception e = task.getException();
                        Log.e(TAG, "onChangeNick:onFailure", e);
                        close(true);
                    }
                });
    }

    // validazione del form
    private boolean validateForm(EditText field) {
        // controllo preliminare
        boolean fieldCheck = validateField(field);
        if (fieldCheck) {
            boolean check = true;
            String nick = field.getText().toString();
            // se il controllo preliminare è passato controlla che ci sia stata una modifica nell'edittext
            if (nick.equals(nickAlreadyUsed)) {
                field.setError(getContext().getString(R.string.insert_different_nickname));
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
            field.setError(getContext().getString(R.string.required_field));
            return false;
        } else {
            field.setError(null);
            return true;
        }
    }

    private Task<Boolean> onChangeNick(Map<String, Object> data) {
        // chiama la funzione Firebase chiamata "onChangeNick"
        return mFunctions
                .getHttpsCallable("onChangeNick")
                .call(data)
                .continueWith(task -> {
                    // Questa blocco viene eseguito in caso di esito positivo o negativo
                    // se l'attività non è riuscita, getResult() genererà un'eccezione che verrà propagata verso il basso.
                    HashMap response = (HashMap) task.getResult().getData();
                    boolean result = (Boolean) response.get("text");
                    return result;
                });
    }

    private void close(boolean error) {
        if(listener != null) {
            // chiama il callback con o senza errore
            listener.onCanceled(error);
            // cancel della dialog
            NickChangeDialog.this.cancel();
        }
    }

}
