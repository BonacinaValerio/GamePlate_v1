package com.bonacogo.gameplate.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.transition.Transition;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bonacogo.gameplate.CaptureActivity;
import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.adapter.GameRankingAdapter;
import com.bonacogo.gameplate.dialog.NickChangeDialog;
import com.bonacogo.gameplate.other.GlideApp;
import com.bonacogo.gameplate.util.GeneralMethod;
import com.bonacogo.gameplate.viewmodel.GameRankingViewModel;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class ProfileFragment extends Fragment implements View.OnClickListener {
    private String state_txt = "Status: ";
    private static final String TAG = "ProfileFragment";

    private TextView nickname, email, game, match,restaurant;
    private ImageView imgProfile;
    private MaterialCardView verifyEmail, admin;
    private LinearLayout noRankContainer;
    private GameRankingAdapter gameRankingAdapter;

    private GameRankingViewModel gameRankingViewModel;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private View startView;
    private ProgressDialog mProgressDialog;

    public interface ActivityCallback {
        void onLogout();
        void setContentUpVisibility(int visibility);
        void showGameRank(ProfileFragment fragment, View start, String detail);
        void hideGameRank(Fragment fragment, View start);
    }
    private ActivityCallback activityCallback;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activityCallback = (ActivityCallback) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameRankingViewModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication())).get(GameRankingViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View myFragment = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();

        // views
        Button resendEmailVerify = myFragment.findViewById(R.id.resend_verifyemail);
        Button nickChange = myFragment.findViewById(R.id.nick_change);
        Button settingsBtn = myFragment.findViewById(R.id.settings_btn);
        Button scan = myFragment.findViewById(R.id.scan);
        verifyEmail = myFragment.findViewById(R.id.verify_email);
        nickname = myFragment.findViewById(R.id.nickname);
        imgProfile = myFragment.findViewById(R.id.img_profile);
        email = myFragment.findViewById(R.id.email);
        noRankContainer = myFragment.findViewById(R.id.no_rank_container);
        admin = myFragment.findViewById(R.id.admin);
        game = myFragment.findViewById(R.id.game);
        restaurant = myFragment.findViewById(R.id.restaurant);
        match = myFragment.findViewById(R.id.match);

        gameRankingAdapter = new GameRankingAdapter(new LinkedHashMap<>(), this);
        gameRankingAdapter.setAdapterCallBack(this::openGameRank);

        RecyclerView rankings = myFragment.findViewById(R.id.rankings);
        rankings.setLayoutManager(new GridLayoutManager(getContext(),2));
        rankings.setAdapter(gameRankingAdapter);

        // listener
        scan.setOnClickListener(this);
        resendEmailVerify.setOnClickListener(this);
        nickChange.setOnClickListener(this);
        settingsBtn.setOnClickListener(this);

        verifyEmail.setVisibility(View.GONE);

        return myFragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // aggiorna la ui
        updateUI(currentUser);
    }

    private void updateUI(FirebaseUser user) {
        // se l'user è null chiama updateUI della mainactivity
        if (user == null)
            activityCallback.onLogout();
        else {

            LiveData<LinkedHashMap<String, HashMap<String, String>>> liveData = gameRankingViewModel.getGames();

            final Observer<LinkedHashMap<String, HashMap<String, String>>> observer = this::setGamesRank;

            liveData.observe(getViewLifecycleOwner(), observer);


            for(UserInfo profile : user.getProviderData()) {
                // check if the provider id matches "facebook.com"
                if(FacebookAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                    imgProfile.setImageTintList(null);
                    String facebookUserId = profile.getUid();
                    String photoUrl = "https://graph.facebook.com/" + facebookUserId + "/picture?height=500";
                    setImgGame(photoUrl, imgProfile);

                    email.setText(profile.getEmail());
                }
                else {
                    email.setText(user.getEmail());
                }
            }

            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
            mDatabase.child("users").child(user.getUid()).child("admin").addListenerForSingleValueEvent(new ValueEventListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (ProfileFragment.this.isAdded()) {
                        if (dataSnapshot.exists()) {
                            admin.setVisibility(View.VISIBLE);

                            game.setText(getString(R.string.game) + dataSnapshot.child("game").getValue(String.class));
                            restaurant.setText(getString(R.string.restaurant) + dataSnapshot.child("restaurant").getValue(String.class));

                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            DocumentReference docRef = db.collection("coordinates")
                                    .document(Objects.requireNonNull(dataSnapshot.child("restaurantId").getValue(String.class)));
                            docRef.get().addOnCompleteListener(task -> {
                                if (ProfileFragment.this.isAdded()) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document.exists()) {
                                            HashMap data = (HashMap) document.getData().get("d");
                                            match.setText(getString(R.string.games_played) + data.get("relevance"));
                                        } else {
                                            Log.d(TAG, "No such document");
                                        }
                                    } else {
                                        Log.d(TAG, "get failed with ", task.getException());
                                        Toast.makeText(getContext(), "Errore",
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "isAdmin, onCancelled: ", databaseError.toException());
                    Toast.makeText(getContext(), "Errore",
                            Toast.LENGTH_LONG).show();
                }
            });

            if (!user.isEmailVerified())
                // se l'utente non è null ma ha la mail non verificata recupera il suo stato aggiornato
                getStatus(user, new StatusCallback() {
                    @Override
                    public void onSuccess(String status) {
                        if (ProfileFragment.this.isAdded()) {
                            Log.v(TAG, "getStatus:success");
                            // aggiorna l'ui
                            Log.i(TAG, "onSuccess: " + state_txt + status);
                            switch (status) {
                                case "not verified":
                                    verifyEmail.setVisibility(View.VISIBLE);
                                case "verified":
                                    nickname.setText(normalize(user.getDisplayName()));
                                    break;
                                case "loading":
                                    nickname.setText(getResources().getString(R.string.loading));
                            }
                        }
                    }
                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "getStatus:failure", error);
                    }
                });
            else {
                // se l'utente non è null e ha la mail verificata aggiorna l'ui
                Log.i(TAG, "updateUI: "+state_txt+"verified");
                nickname.setText(normalize(user.getDisplayName()));
            }
        }
    }

    // callback dello status dell'utente
    private void getStatus(FirebaseUser user, StatusCallback callback) {
        // ricarica l'utente
        getUserReloaded(user, new UserReloadedCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                // recupera il token di accasso
                user.getIdToken(false).addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        String status = "verified";
                        String provider = task.getResult().getSignInProvider();
                        // se l'utente ha fatto l'accesso con password e la mail non è verificata lo stato è "not verified"
                        if (!provider.equals("facebook.com") && !user.isEmailVerified())
                            status = "not verified";
                            // se l'utente ha fatto l'accesso con facebook e la mail non è verificata lo stato è "loading" perchè
                            // la mail passa nello stato verificato dopo il trigger della funzione firebase OnCreate
                        else if (provider.equals("facebook.com") && !user.isEmailVerified())
                            status = "loading";
                        callback.onSuccess(status);
                    }
                    else {
                        callback.onError(task.getException());
                    }
                });
            }
            @Override
            public void onError(Exception error) {
                callback.onError(error);
            }
        });

    }

    // callback dell'utente dopo il reload
    private void getUserReloaded(FirebaseUser user, UserReloadedCallback callback) {
        user.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess(user);
            }
            else
                callback.onError(task.getException());
        });
    }

    interface UserReloadedCallback{
        void onSuccess(FirebaseUser user);
        void onError(Exception error);
    }

    interface StatusCallback{
        void onSuccess(String status);
        void onError(Exception error);
    }

    private void setGamesRank(LinkedHashMap<String, HashMap<String, String>> gamesRank) {
        if (gamesRank == null || gamesRank.isEmpty()) {
            noRankContainer.setVisibility(View.VISIBLE);
        }
        if (gamesRank != null && !gamesRank.isEmpty()) {
            noRankContainer.setVisibility(View.GONE);
            gameRankingAdapter.setGameRanking(gamesRank);
            gameRankingAdapter.notifyDataSetChanged();
        }
    }

    private void openGameRank(String game, View v) {
        startView = v;
        activityCallback.showGameRank(this, startView, game);
    }

    public void onBackClick(Fragment fragment) {
        activityCallback.hideGameRank(fragment, startView);
        startView = null;
    }

    private void setImgGame(String url, ImageView imageView) {

        RequestOptions options = new RequestOptions().fitCenter();
        GlideApp.with(this)
                .load(url)
                .placeholder(R.drawable.image_loading)
                .error(R.drawable.image_error)
                .transition(withCrossFade())
                .apply(options)
                .into(imageView);
    }

    // gestisci il valore null nel displayname
    private String normalize(String displayName) {
        if(displayName == null)
            return "-";
        else
            return displayName;
    }

    private void changeNick(FirebaseUser user) {
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String provider = task.getResult().getSignInProvider();
                // se il provider è fb
                if (provider.equals("facebook.com")) {
                    // se il displayName è null è quindi verificata anche la mail (operazione eseguita in contemporanea nell'onCreate function) ed
                    // è quindi stato resettato il nickname a fronte del problema "nick già in uso"
                    if (user.getDisplayName() == null)
                        nickFbeExist();
                    // se il displayName non è null non è detto che la mail sia verificata (nel caso in cui si accede prima dell'onCreate function)
                    // quindi se è verificata continua altrimenti segnala errore
                    else if (user.isEmailVerified())
                        launchChangeDialog(user.getDisplayName(), false);
                    else
                        Toast.makeText(getContext(), R.string.error_try_later,
                                Toast.LENGTH_LONG).show();
                }
                else
                    launchChangeDialog(user.getDisplayName(), false);
            }
            else {
                Log.e(TAG, "changeNick: ", task.getException());
                Toast.makeText(getContext(), R.string.error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void nickFbeExist() {
        GraphRequest request = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken(),
                (object, response) -> {
                    try {
                        // recupera il nome da fb e comunica che è già in uso
                        String first_name = object.getString("first_name");
                        String last_name = object.getString("last_name");
                        launchChangeDialog(first_name + " " + last_name, true);
                    } catch (JSONException e) {
                        Log.e(TAG, "nickFbeExist: ", e);
                        Toast.makeText(getContext(), R.string.error,
                                Toast.LENGTH_LONG).show();
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "first_name,last_name");
        request.setParameters(parameters);
        request.executeAsync();
    }


    private void launchChangeDialog(String nickUsed, boolean error) {
        Context context = getContext();
        if (context == null)
            return;

        // avvia la dialog per il cambio del nick
        NickChangeDialog nickChangeDialog = new NickChangeDialog(context, nickUsed, error, new NickChangeDialog.DialogListener() {
            @Override
            public void onCompleted() {
                // se ha avuto successo il nick è stato cambiato e quindi aggiorna la ui
                Log.v(TAG, "nickChangeDialog.onCompleted: success");
                FirebaseUser user = mAuth.getCurrentUser();
                nickname.setText(normalize(user.getDisplayName()));
            }

            @Override
            public void onCanceled(boolean error) {
                Log.e(TAG, "nickChangeDialog.onCanceled: error");
                if (error)
                    Toast.makeText(getContext(), R.string.error,
                            Toast.LENGTH_LONG).show();
            }
        });
        nickChangeDialog.show();
        nickChangeDialog.setCancelable(true);
    }

    // invia la mail di verifica dell'account
    private void sendEmailVerification(Context context, FirebaseUser user, ProgressDialog mProgressDialog) {
        // Send verification email
        // [START send_email_verification]
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    GeneralMethod.hideProgressDialog(mProgressDialog);
                    // [START_EXCLUDE]
                    if (task.isSuccessful()) {
                        Toast.makeText(context,
                                context.getString(R.string.verification_mail_sent) + user.getEmail(),
                                Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "sendEmailVerification", task.getException());
                        Toast.makeText(context,
                                context.getString(R.string.error_mail_not_sent),
                                Toast.LENGTH_LONG).show();
                    }
                    // [END_EXCLUDE]
                });
        // [END send_email_verification]
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.resend_verifyemail) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Context context = getContext();
                if (context == null)
                    return;

                // mostra la progressdialog
                mProgressDialog = GeneralMethod.showProgressDialog(mProgressDialog, context, getString(R.string.loading));
                // invia email di verifica
                sendEmailVerification(context, user, mProgressDialog);
            }
        }
        else if (i == R.id.nick_change) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null)
                changeNick(user);
        }
        else if (i == R.id.settings_btn) {
            // cambia il fragment con SettingsFragment
            FragmentManager fManager = getFragmentManager();
            if (fManager == null)
                return;

            SettingsFragment fragment = new SettingsFragment();

            Fragment fragment0 = fManager.findFragmentById(R.id.content_up);
            if (fragment0 instanceof SettingsFragment)
                fManager.beginTransaction().remove(fragment0).commitNow();

            changeFragment(fragment, fManager);
        }
        else if (i == R.id.scan) {
            setScanner();
        }
    }

    // cambia il fragment on top
    private void changeFragment(Fragment fragment, FragmentManager fManager) {
        fragment.setEnterTransition(MaterialSharedAxis.create(MaterialSharedAxis.Y, true));
        fragment.setExitTransition(MaterialSharedAxis.create(MaterialSharedAxis.Y, false).addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) { }
            @Override
            public void onTransitionEnd(Transition transition) {
                activityCallback.setContentUpVisibility(View.GONE);
            }
            @Override
            public void onTransitionCancel(Transition transition) { }
            @Override
            public void onTransitionPause(Transition transition) { }
            @Override
            public void onTransitionResume(Transition transition) { }
        }));
        FragmentTransaction fTransaction = fManager.beginTransaction();
        activityCallback.setContentUpVisibility(View.VISIBLE);
        fTransaction.replace(R.id.content_up, fragment).commit();
    }

    private void setScanner() {
        IntentIntegrator.forSupportFragment(this)
                .setOrientationLocked(false)
                .setPrompt("Scan")
                .setCaptureActivity(CaptureActivity.class)
                .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                .initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() != null) {
                ransomTicket(result.getContents());
            }
        }
    }

    private void showDialog(String detailTxt, String titleTxt) {
        Context context = getContext();
        if (context == null)
            return;

        // creo la dialog
        Dialog dialog = new Dialog(context, R.style.Theme_Dialog);

        // setup
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(R.layout.terms_layout);

        TextView detail = dialog.findViewById(R.id.terms);
        TextView title = dialog.findViewById(R.id.title);
        View outsideOfDialog = dialog.findViewById(R.id.outside_of_dialog);

        detail.setText(detailTxt);
        title.setText(titleTxt);
        dialog.show();

        outsideOfDialog.setOnClickListener(v -> dialog.dismiss());
    }

    private void showSnack(String text, HashMap<String, String> detail) {
        View view = getView();
        if (view == null)
            return;

        if (detail == null) {
            Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, v -> { }).show();
        }
        else {
            Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.see_details_upper, v -> showDialog(detail.get("terms"), detail.get("title"))).show();
        }
    }

    private void showSnack(String text) {
        showSnack(text, null);
    }

    private static final String TICKET_EXPIRED = "TICKET_EXPIRED";
    private static final String TICKET_USED = "TICKET_USED";
    private static final String TICKET_VALID = "TICKET_VALID";

    private void ransomTicket(String ticketCode) {
        Map<String, Object> data = new HashMap<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            data.put("user", user.getUid());
        if (ticketCode != null)
            data.put("ticketCode", ticketCode);
        data.put("lang", new Locale(Locale.getDefault().getLanguage()).getLanguage());

        mProgressDialog = GeneralMethod.showProgressDialog(mProgressDialog, getContext(), getString(R.string.loading));
        onRansomTicket(data).addOnCompleteListener(task -> {
            // stop loading
            GeneralMethod.hideProgressDialog(mProgressDialog);
            if (task.isSuccessful()) {
                HashMap response = task.getResult();
                if (response != null) {
                    Log.i(TAG, "ransomTicket: "+response.toString());
                    String status = (String) response.get("status");
                    if (status != null) {
                        switch (status) {
                            case TICKET_EXPIRED:
                                showSnack(getString(R.string.ticket_expired_on) + getDate((Long) response.get("details"), "dd/MM/yyyy HH:mm"));
                                break;
                            case TICKET_USED:
                                showSnack(getString(R.string.ticket_used_on) + getDate((Long) response.get("details"), "dd/MM/yyyy HH:mm"));
                                break;
                            case TICKET_VALID:
                                showSnack(getString(R.string.ticket_successful), (HashMap<String, String>) response.get("details"));
                                break;
                            default:
                                error();
                                break;
                        }
                    }
                }
                else
                    error();
            }
            else {
                error();
                Exception e = task.getException();
                Log.e(TAG, "ransomTicket:onFailure", e);
            }
        });
    }

    private static String getDate(long milliSeconds, String dateFormat) {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat, new Locale(Locale.getDefault().getLanguage()));

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    private void error() {
        Toast.makeText(getContext(), R.string.error,
                Toast.LENGTH_LONG).show();
    }

    private Task<HashMap> onRansomTicket(Map<String, Object> data) {
        FirebaseFunctions mFunctions = FirebaseFunctions.getInstance();
        return mFunctions
                .getHttpsCallable("onRansomTicket")
                .call(data)
                .continueWith(task -> {
                    // Questa blocco viene eseguito in caso di esito positivo o negativo
                    // se l'attività non è riuscita, getResult() genererà un'eccezione che verrà propagata verso il basso.

                    HashMap response = (HashMap) task.getResult().getData();

                    Log.i(TAG, "onRansomTicket: "+response.toString());

                    return response;
                });
    }

}
