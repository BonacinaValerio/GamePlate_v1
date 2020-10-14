package com.bonacogo.gameplate.fragment;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.model.TicketRewardObject;
import com.bonacogo.gameplate.other.GlideApp;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class TicketDetailsFragment extends Fragment {

    private static final String TAG = "TicketDetailsFragment";
    private static final String TICKET = "TICKET";

    public TicketDetailsFragment() {
        super();
    }

    public static TicketDetailsFragment newInstance(TicketRewardObject ticket) {
        // creo una nuova istanza con il parametro
        TicketDetailsFragment fragment = new TicketDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(TICKET, ticket);
        fragment.setArguments(args);
        return fragment;
    }

    // activity callback
    public interface ActivityCallBack {
        void onBackClick(Fragment fragment);
    }
    private ActivityCallBack activityCallBack;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activityCallBack = (ActivityCallBack) context;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View myFragment = inflater.inflate(R.layout.fragment_ticket_details, container, false);
        final TicketRewardObject ticket = (TicketRewardObject) getArguments().getSerializable(TICKET);

        ImageView header = myFragment.findViewById(R.id.header);
        TextView restaurant_name = myFragment.findViewById(R.id.restaurant_name);
        TextView reward_type = myFragment.findViewById(R.id.reward_type);
        ImageView qr_code = myFragment.findViewById(R.id.qr_code);
        Button details = myFragment.findViewById(R.id.details);
        ImageButton backBtn = myFragment.findViewById(R.id.back_btn);

        TextView mDays = myFragment.findViewById(R.id.days);
        TextView mHours = myFragment.findViewById(R.id.hour);
        TextView mMins = myFragment.findViewById(R.id.minutes);

        // Set reward type
        String type = getString(R.string.you_won) + " " + ticket.getType() + " " + getString(R.string.at);
        reward_type.setText(type);

        // Set restaurant name
        restaurant_name.setText(ticket.getRestaurant());

        // Set image header
        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("restaurants")
                .child(ticket.getRestaurantId())
                .child("logo.jpg");

        RequestOptions options = new RequestOptions().fitCenter();
        GlideApp.with(this)
                .load(storageReference)
                .placeholder(R.drawable.image_loading)
                .error(R.drawable.image_error)
                .transition(withCrossFade())
                .apply(options)
                .into(header);


        // Set Qr code image
        qr_code.setImageBitmap(createQrImage(ticket.getTicketCode(), 400));


        // Handling Countdown
        Date date = new Date();
        long countDown = ticket.getDeadline() - date.getTime();
        new CountDownTimer(countDown, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // We're forced to use long instead of int : int values->risk of negative results
                setCountdown(millisUntilFinished/1000, mDays, mHours, mMins);
            }
            @Override
            public void onFinish() {
                mDays.setText("0");
                mHours.setText("0");
                mMins.setText("0");
            }


        }.start();

        qr_code.setOnClickListener(v -> {
            Context context = getContext();
            if (context == null)
                return;

            Dialog dialogQR = new Dialog(context);
            dialogQR.setCanceledOnTouchOutside(true);
            dialogQR.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialogQR.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialogQR.setContentView(R.layout.qr_zoom);

            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            ImageView qr = dialogQR.findViewById(R.id.qr_code);
            qr.setImageBitmap(createQrImage(ticket.getTicketCode(), size.x));

            dialogQR.show();
        });

        details.setOnClickListener(v -> {
            Context context = getContext();
            if (context == null)
                return;

            Dialog dialogTerms = new Dialog(context, R.style.Theme_Dialog);
            dialogTerms.setCanceledOnTouchOutside(true);
            dialogTerms.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialogTerms.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialogTerms.setContentView(R.layout.terms_layout);
            TextView title = dialogTerms.findViewById(R.id.title);
            TextView subtitle = dialogTerms.findViewById(R.id.subtitle);
            TextView terms = dialogTerms.findViewById(R.id.terms);
            View outsideOfDialog = dialogTerms.findViewById(R.id.outside_of_dialog);

            title.setText(ticket.getType());
            subtitle.setText(ticket.getRestaurant());

            terms.setText(ticket.getRewardString().getTerms());
/*
            terms.setText("id porta nibh venenatis cras sed felis eget velit aliquet sagittis id consectetur purus ut faucibus pulvinar elementum integer enim neque volutpat ac tincidunt vitae semper quis lectus nulla at volutpat diam ut venenatis tellus in metus vulputate eu scelerisque felis imperdiet proin fermentum leo vel orci porta non pulvinar neque laoreet suspendisse interdum consectetur libero id faucibus nisl tincidunt eget nullam non nisi est sit amet facilisis magna etiam tempor orci eu lobortis elementum nibh tellus molestie nunc non blandit massa enim nec dui nunc mattis enim ut tellus elementum sagittis vitae et leo duis ut diam quam nulla porttitor massa id neque aliquam vestibulum morbi blandit cursus risus at ultrices mi tempus imperdiet nulla malesuada pellentesque elit eget gravida cum sociis natoque penatibus et magnis dis parturient montes nascetur ridiculus mus mauris vitae ultricies leo integer malesuada nunc vel risus commodo viverra maecenas accumsan lacus vel facilisis volutpat est velit egestas dui id ornare arcu odio ut sem nulla pharetra diam sit amet nisl suscipit adipiscing bibendum est ultricies integer quis auctor elit sed vulputate mi sit amet mauris commodo quis imperdiet massa tincidunt nunc pulvinar sapien et ligula ullamcorper malesuada proin libero nunc consequat interdum varius sit amet mattis vulputate enim nulla aliquet porttitor lacus luctus accumsan tortor posuere ac ut consequat semper viverra nam libero justo laoreet sit amet cursus sit amet dictum sit amet justo donec enim diam vulputate ut pharetra sit amet aliquam id diam maecenas ultricies mi eget mauris pharetra et ultrices neque ornare aenean euismod elementum nisi quis eleifend quam adipiscing vitae proin sagittis nisl rhoncus mattis rhoncus urna neque viverra justo nec ultrices dui sapien eget mi proin sed libero enim sed faucibus turpis in eu mi bibendum neque egestas congue quisque egestas diam in arcu cursus euismod quis viverra nibh cras pulvinar mattis nunc sed blandit libero volutpat sed cras ornare arcu dui vivamus arcu felis bibendum ut tristique et egestas quis ipsum suspendisse ultrices gravida dictum fusce ut placerat orci nulla pellentesque dignissim enim sit amet venenatis urna cursus eget nunc scelerisque viverra mauris in aliquam sem fringilla ut morbi tincidunt augue interdum velit euismod in pellentesque massa placerat duis ultricies lacus sed turpis tincidunt id aliquet risus feugiat in ante metus dictum at tempor commodo ullamcorper a lacus vestibulum sed arcu non odio euismod lacinia at quis risus sed vulputate odio ut enim blandit volutpat maecenas volutpat blandit aliquam etiam erat velit scelerisque in dictum non consectetur a erat nam at lectus urna duis convallis convallis tellus id interdum velit laoreet id donec ultrices tincidunt arcu non sodales neque sodales ut etiam sit amet nisl purus in mollis nunc sed id semper risus in hendrerit gravida rutrum quisque non tellus orci ac auctor augue mauris augue neque gravida in fermentum et sollicitudin ac orci phasellus egestas tellus rutrum tellus pellentesque eu tincidunt tortor aliquam nulla facilisi cras fermentum odio eu feugiat pretium nibh ipsum consequat nisl vel pretium lectus quam id leo in vitae turpis massa sed elementum");
*/

            outsideOfDialog.setOnClickListener(v1 -> dialogTerms.dismiss());


            dialogTerms.show();
        });

        backBtn.setOnClickListener(v -> activityCallBack.onBackClick(this));

        return myFragment;

    }

    private Bitmap createQrImage(String code, int size) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        Bitmap bitmap = null;
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(code, BarcodeFormat.QR_CODE,size,size);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            bitmap = barcodeEncoder.createBitmap(bitMatrix);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    private void setCountdown(long sec, TextView mDays, TextView mHours, TextView mMins) {
        // We're forced to use long instead of int : int values->risk of negative results
        long numberOfDays = sec / 86400;
        long numberOfHours = (sec % 86400 ) / 3600;
        long numberOfMinutes = ((sec % 86400 ) % 3600 ) / 60;
        mDays.setText(String.valueOf(numberOfDays));
        mHours.setText(String.valueOf(numberOfHours));
        mMins.setText(String.valueOf(numberOfMinutes));
    }
}
