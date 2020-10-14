package com.bonacogo.gameplate.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Toast;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.util.GeneralMethod;
import com.google.firebase.auth.FirebaseUser;

import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {
    private static final String TAG = "BaseFragment";

    ActivityCallback activityCallback;
    public interface ActivityCallback {
        void okBackClick();
        void updateUI(FirebaseUser user);
        void removeFragment(Fragment fragment);
    }

    // aggiorna l'ui chiudendo il fragment on top
    void updateUI(FirebaseUser user, View myFragment, ProgressDialog mProgressDialog) {
        // nascondi la progressdialog
        GeneralMethod.hideProgressDialog(mProgressDialog);
        if (user != null) {
            activityCallback.updateUI(user);
            closeFragment(myFragment);
        }
    }

    // animazione fade out del fragment con zoom out
    void closeFragment(View myFragment) {
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(new AlphaAnimation(1.0F, 0.0F));
        animationSet.addAnimation(new ScaleAnimation(1, 0.9f, 1, 0.9f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)); // Change args as desired
        animationSet.setDuration(100);
        animationSet.setFillAfter(true);
        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                activityCallback.removeFragment(BaseFragment.this);
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });
        myFragment.startAnimation(animationSet);
    }


    // invia la mail di verifica dell'account
    void sendEmailVerification(Context context, FirebaseUser user, ProgressDialog mProgressDialog) {
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

}
