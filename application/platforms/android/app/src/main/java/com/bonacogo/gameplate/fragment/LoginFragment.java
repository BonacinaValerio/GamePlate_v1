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
import com.bonacogo.gameplate.dialog.PasswordResetDialog;
import com.bonacogo.gameplate.util.GeneralMethod;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LoginFragment extends BaseFragment {
    private String title_txt = "Login";
    private String interact_txt = "Login";
    private static final String TAG = "LoginEmailPassword";
    private View myFragment;

    private EditText email, password1;
    private ProgressDialog mProgressDialog;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activityCallback = (ActivityCallback)context;
    }

    public LoginFragment() {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myFragment = inflater.inflate(R.layout.fragment_login_register, container, false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // views
        email = myFragment.findViewById(R.id.email);
        password1 = myFragment.findViewById(R.id.password1);
        EditText password2 = myFragment.findViewById(R.id.password2);
        Button forgot = myFragment.findViewById(R.id.forgot);
        Button interact = myFragment.findViewById(R.id.interact);
        TextView title = myFragment.findViewById(R.id.title);
        ImageButton back = myFragment.findViewById(R.id.back_btn);

        // setup views
        title.setText(title_txt);
        interact.setText(interact_txt);
        password2.setVisibility(View.GONE);
        forgot.setVisibility(View.VISIBLE);

        // listener on click
        back.setOnClickListener(v -> activityCallback.okBackClick());
        interact.setOnClickListener(v -> signIn(email.getText().toString(), password1.getText().toString()));
        forgot.setOnClickListener(v -> {
            PasswordResetDialog alert = new PasswordResetDialog();
            alert.showDialog(getContext());
        });
        return myFragment;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        email.clearFocus();
        password1.clearFocus();
    }

    // login
    private void signIn(String email, String password) {
        // se il form non è validato ritorna
        if (!validateForm()) {
            return;
        }

        // mostra la progressdialog
        mProgressDialog = GeneralMethod.showProgressDialog(mProgressDialog, getContext(), getString(R.string.loading));

        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user, myFragment, mProgressDialog);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Exception e = task.getException();
                        // gestione di ogni tipo di errore che può risultare
                        String defaultError = getString(R.string.unknown_error);
                        String message;
                        if (e instanceof FirebaseAuthInvalidCredentialsException)
                            message = getString(R.string.wrong_credentials);
                        else if (e instanceof FirebaseAuthInvalidUserException) {
                            String code = ((FirebaseAuthInvalidUserException) e).getErrorCode();
                            switch (code) {
                                case "ERROR_USER_NOT_FOUND":
                                    message = getString(R.string.user_not_exist);
                                    break;
                                case "ERROR_USER_DISABLED":
                                    message = getString(R.string.account_disabled);
                                    break;
                                default:
                                    message = defaultError;
                            }
                        }
                        else
                            message = defaultError;
                        Toast.makeText(getContext(), message,
                                Toast.LENGTH_LONG).show();
                        updateUI(null, myFragment, mProgressDialog);
                    }
                });
        // [END sign_in_with_email]
    }

    // validazione del form
    private boolean validateForm() {
        // controllo preliminare
        boolean fieldCheck = validateField(email) & validateField(password1);
        if (fieldCheck) {
            boolean check = true;
            String email_txt = email.getText().toString();
            // se il controllo preliminare è passato controlla che la mail sia benformata
            if (!Patterns.EMAIL_ADDRESS.matcher(email_txt).matches()) {
                email.setError(getString(R.string.email_not_valid));
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
