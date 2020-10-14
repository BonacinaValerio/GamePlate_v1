package com.bonacogo.gameplate.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.util.GeneralMethod;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class LoginScreen extends BaseFragment implements View.OnClickListener {
    private static final String TAG = "FacebookLogin";
    private View myFragment;

    private LoginButton loginButton;
    private CallbackManager mCallbackManager;
    private ProgressDialog mProgressDialog;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activityCallback = (ActivityCallback)context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myFragment = inflater.inflate(R.layout.fragment_login_screen, container, false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // views
        MaterialButton register = myFragment.findViewById(R.id.register);
        TextView exit = myFragment.findViewById(R.id.exit);
        Button login = myFragment.findViewById(R.id.login);
        Button loginFacebook = myFragment.findViewById(R.id.login_fb);
        loginButton = myFragment.findViewById(R.id.login_fb_btn);

        // listener
        register.setOnClickListener(this);
        exit.setOnClickListener(this);
        login.setOnClickListener(this);
        loginFacebook.setOnClickListener(this);

        // [START initialize_fblogin]
        // Initialize Facebook Login button
        mCallbackManager = CallbackManager.Factory.create();

        loginButton.setPermissions("email", "public_profile");
        loginButton.setFragment(this);
        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
                // [START_EXCLUDE]
                updateUI(null, myFragment, mProgressDialog);
                // [END_EXCLUDE]
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
                // [START_EXCLUDE]
                updateUI(null, myFragment, mProgressDialog);
                // [END_EXCLUDE]
            }
        });
        // [END initialize_fblogin]
        return myFragment;
    }

    // [START on_activity_result]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Pass the activity result back to the Facebook SDK
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }
    // [END on_activity_result]

    // [START auth_with_facebook]
    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user, myFragment, mProgressDialog);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(getContext(), R.string.auth_failed,
                                Toast.LENGTH_LONG).show();
                        updateUI(null, myFragment, mProgressDialog);
                    }
                });
    }
    // [END auth_with_facebook]

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.exit) {
            // chiudi il fragment on top
            closeFragment(myFragment);
        }
        else if (i == R.id.register) {
            // cambia il fragment con RegisterFragment
            Fragment fragment = new RegisterFragment();
            changeFragment(fragment);
        }
        else if (i == R.id.login) {
            // cambia il fragment con LoginFragment
            Fragment fragment = new LoginFragment();
            changeFragment(fragment);
        }
        else if (i == R.id.login_fb) {
            // mostra la progressdialog e simula il click del fb loginButton
            mProgressDialog = GeneralMethod.showProgressDialog(mProgressDialog, getContext(), getString(R.string.loading));
            loginButton.performClick();
        }
    }

    // cambia il fragment on top
    private void changeFragment(Fragment fragment) {
        FragmentManager fManager = getFragmentManager();
        FragmentTransaction fTransaction = fManager.beginTransaction();
        fTransaction
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                .replace(R.id.content_up, fragment).commit();
    }
}
