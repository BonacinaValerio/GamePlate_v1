package com.bonacogo.gameplate.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.interpolator.CustomBounceInterpolator;
import com.google.android.material.transition.MaterialFade;
import com.google.android.material.transition.MaterialFadeThrough;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import static android.content.Context.MODE_PRIVATE;

public class Tutorial01 extends Fragment {
    private int[] poncho = {R.drawable.saluto, R.drawable.saluto, R.drawable.felice, R.drawable.felicissimo};
    private String[] vignetta;
    private String[] next;
    private ImageView img_poncho;
    private TextView text;
    private Button txt_next;
    private LinearLayout containerVignetta;
    private int state = 0;
    private Animation animation1, animation2;

    public Tutorial01() {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View myFragment = inflater.inflate(R.layout.fragment_tutorial01, container, false);

        vignetta = new String[] {getString(R.string.Tutorial01_poncho1), getString(R.string.Tutorial01_poncho2),
                getString(R.string.Tutorial01_poncho3), getString(R.string.Tutorial01_poncho4)};

        next = new String[] {getString(R.string.Tutorial01_user1), getString(R.string.Tutorial01_user2),
                getString(R.string.Tutorial01_user3), getString(R.string.Tutorial01_user4)};

        // views
        containerVignetta = myFragment.findViewById(R.id.vignetta);
        text = myFragment.findViewById(R.id.text);
        img_poncho = myFragment.findViewById(R.id.poncho);
        txt_next = myFragment.findViewById(R.id.next);
        animation1 = createAnimation();
        animation2 = createAnimation();
        // setup animation2
        animation2.setStartOffset(400);
        animation2.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                txt_next.setEnabled(false);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                txt_next.setEnabled(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        // mostra lo stato 0 e incrementa il contatore
        nextState(state++);

        // listener onclick
        txt_next.setOnClickListener(v -> {
            if (state<next.length)
                // nuovo stato se non è l'ultimo
                nextState(state++);
            else {
                // se è l'ultimo stato salva nelle SharedPreferences il completamento del tutorial
                SharedPreferences prefs = getContext().getSharedPreferences("com.bonacogo.gameplate", MODE_PRIVATE);
                prefs.edit().putBoolean("firstRun", false).apply();

                // cambia il fragment e passa a loginscreen
                MaterialFade fade = MaterialFade.create();
                this.setExitTransition(fade);
                Fragment fragment = new LoginScreen();
                MaterialFadeThrough fadeThrough = MaterialFadeThrough.create();
                fragment.setEnterTransition(fadeThrough);

                FragmentManager fManager = getFragmentManager();
                if (fManager == null)
                    return;

                FragmentTransaction fTransaction = fManager.beginTransaction();
                fTransaction.replace(R.id.content_up, fragment).commit();
            }
        });

        return myFragment;
    }

    private void nextState(int i) {
        // setup nuovo stato
        img_poncho.setImageDrawable(getResources().getDrawable(poncho[i]));
        text.setText(vignetta[i]);
        txt_next.setText(next[i]);
        // animazioni
        containerVignetta.startAnimation(animation1);
        txt_next.startAnimation(animation2);
    }

    // crea nuova animazione
    private Animation createAnimation() {
        Animation animFabZoomIn = AnimationUtils.loadAnimation(getContext(), R.anim.bounce_in_tutorial);
        // Use bounce_in interpolator with amplitude 0.2 and frequency 10
        CustomBounceInterpolator interpolator = new CustomBounceInterpolator(0.2, 10);
        animFabZoomIn.setInterpolator(interpolator);
        // mantieni costante dopo l'animazione
        animFabZoomIn.setFillAfter(true);

        return animFabZoomIn;
    }
}