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
import android.widget.ImageButton;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.viewmodel.SettingsViewModel;
import com.facebook.login.LoginManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";

    private SwitchMaterial switchMaterial;
    private LiveData<Boolean> liveData;
    private ProgressDialog mProgressDialog;

    public interface ActivityCallBack {
        void onLogout();
    }
    private ActivityCallBack activityCallBack;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activityCallBack = (ActivityCallBack) context;
    }

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View myFragment = inflater.inflate(R.layout.fragment_settings, container, false);

        SettingsViewModel settingsViewModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication())).get(SettingsViewModel.class);
        mAuth = FirebaseAuth.getInstance();

        // setup views
        ImageButton backBtn = myFragment.findViewById(R.id.back_btn);
        Button logout = myFragment.findViewById(R.id.logout);
        Button contactUs = myFragment.findViewById(R.id.contact_us);
        switchMaterial = myFragment.findViewById(R.id.switch_material);

        liveData = settingsViewModel.getEnableNotification();

        AtomicBoolean firstRun = new AtomicBoolean(true);
        final Observer<Boolean> observer = activated -> {
            switchMaterial.setChecked(activated);
            if (firstRun.get()) {

                // listener
                switchMaterial.setOnCheckedChangeListener((buttonView, isChecked) -> settingsViewModel.setEnableNotification(isChecked));
                firstRun.set(false);
            }
        };

        liveData.observe(getViewLifecycleOwner(), observer);

        logout.setOnClickListener(v -> logout());
        contactUs.setOnClickListener(v -> sendMail());
        backBtn.setOnClickListener(v -> removeFragment());

        return myFragment;
    }

    private void sendMail() {
        String[] recipients = {"Bonacinav@gmail.com", "lucacogo997@gmail.com"};
        String subject = "INFO REQUEST";
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, recipients);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.setType("message/rfc822");
        startActivity(intent);
    }

    private void logout() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // mostra la progressdialog
        showProgressDialog();
        currentUser.getIdToken(false).addOnCompleteListener(task -> {
            // nascondi la progressdialog
            hideProgressDialog();
            if (task.isSuccessful()){
                Log.v(TAG, "getIdToken:success");
                String provider = task.getResult().getSignInProvider();
                // esegui il logout del loginManager se l'utente è loggato con fb
                if (provider.equals("facebook.com"))
                    LoginManager.getInstance().logOut();
                // logout di auth
                mAuth.signOut();
                // aggiorna l'ui
                liveData.removeObservers(requireActivity());
                activityCallBack.onLogout();
                removeFragment();
            }
            else {
                Log.e(TAG, "getIdToken:failure", task.getException());
            }
        });
    }

    private void removeFragment() {
        FragmentManager fManager = getFragmentManager();
        if (fManager == null)
            return;

        FragmentTransaction fTransaction = fManager.beginTransaction();
        fTransaction.remove(this).commit();
    }

    // mostra la progressdialog
    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this.getContext());
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    // nascondi la progressdialog
    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
