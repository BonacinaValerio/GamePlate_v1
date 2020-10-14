package com.bonacogo.gameplate.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.util.GeneralMethod;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RegisterFragment extends BaseFragment {
    private static final String TAG = "RegisterEmailPassword";
    private View myFragment;

    private EditText email, password1, password2;
    private ProgressDialog mProgressDialog;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activityCallback = (ActivityCallback)context;
    }

    public RegisterFragment() {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myFragment = inflater.inflate(R.layout.fragment_login_register, container, false);

        String title_txt = getString(R.string.welcome);
        String interact_txt = getString(R.string.register);

        // views
        email = myFragment.findViewById(R.id.email);
        password1 = myFragment.findViewById(R.id.password1);
        password2 = myFragment.findViewById(R.id.password2);
        Button forgot = myFragment.findViewById(R.id.forgot);
        Button interact = myFragment.findViewById(R.id.interact);
        TextView title = myFragment.findViewById(R.id.title);
        ImageButton back = myFragment.findViewById(R.id.back_btn);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // setup views
        title.setText(title_txt);
        interact.setText(interact_txt);
        forgot.setVisibility(View.GONE);
        password2.setVisibility(View.VISIBLE);

        // listener onclick
        back.setOnClickListener(v -> activityCallback.okBackClick());
        interact.setOnClickListener(v -> createAccount(email.getText().toString(), password1.getText().toString()));

        return myFragment;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        email.clearFocus();
        password1.clearFocus();
        password2.clearFocus();
    }

    // register
    private void createAccount(String email, String password1) {
        // se il form non è validato ritorna
        if (!validateForm()) {
            return;
        }

        // mostra la progressdialog
        mProgressDialog = GeneralMethod.showProgressDialog(mProgressDialog, getContext(), getString(R.string.loading));

        // [START create_user_with_email]
        mAuth.createUserWithEmailAndPassword(email, password1)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        // invia la mail di verifica
                        sendEmailVerification(getContext(), user, mProgressDialog);
                        updateUI(user, myFragment, mProgressDialog);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Exception e = task.getException();
                        // gestione di ogni tipo di errore che può risultare
                        String defaultError = getString(R.string.unknown_error);
                        String message;
                        if (e instanceof FirebaseAuthWeakPasswordException)
                            message = getString(R.string.weak_password); // controllo ulteriore gestito ANCHE da firebase
                        else if (e instanceof FirebaseAuthInvalidCredentialsException)
                            message = getString(R.string.wrong_credentials); // controllo ulteriore gestito ANCHE da firebase
                        else if (e instanceof FirebaseAuthUserCollisionException)
                            message = getString(R.string.email_already_registered);
                        else
                            message = defaultError;
                        Toast.makeText(getContext(), message,
                                Toast.LENGTH_LONG).show();
                        updateUI(null, myFragment, mProgressDialog);
                    }
                });
        // [END create_user_with_email]
    }

    // validazione del form
    private boolean validateForm() {
        // controllo preliminare
        boolean fieldCheck = validateField(email) & validateField(password1) & validateField(password2);
        if (fieldCheck) {
            boolean check = true;
            String email_txt = email.getText().toString();
            // se il controllo preliminare è passato controlla che la mail sia benformata
            if (!Patterns.EMAIL_ADDRESS.matcher(email_txt).matches()) {
                email.setError(getString(R.string.email_not_valid));
                check = false;
            }
            // se il controllo preliminare è passato controlla che le password 1 e 2 coincidano
            if (!password1.getText().toString().equals(password2.getText().toString())) {
                password2.setError(getString(R.string.not_matching_passwords));
                check = false;
            }
            // se il controllo preliminare è passato controlla che le password 1 abbia almeno 6 caratteri
            if (password1.getText().toString().length()<6) {
                password1.setError(getString(R.string.at_least_6_chars));
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
            field.setError(getString(R.string.required_field));
            return false;
        } else {
            field.setError(null);
            return true;
        }
    }
}
