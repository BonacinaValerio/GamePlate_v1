package com.bonacogo.gameplate.other;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bonacogo.gameplate.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final TextView titleUi, snippetUi;
    private View view;

    private Context myContext;

    public CustomInfoWindowAdapter(Context aContext) {
        this.myContext = aContext;

        LayoutInflater inflater = (LayoutInflater) myContext.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        view = inflater.inflate(R.layout.custom_info_window_layout, null);

        ImageView restaurantIcon = view.findViewById(R.id.restaurant_icon);
        restaurantIcon.setImageResource(R.drawable.ic_restaurant);
        ImageView playIcon = view.findViewById(R.id.play_icon);
        playIcon.setImageResource(R.drawable.ic_play);
        ImageView pin = view.findViewById(R.id.pin);
        pin.setImageResource(R.drawable.ic_pin_info_window);

        Typeface typeface = ResourcesCompat.getFont(myContext, R.font.poppins_medium);

        titleUi = view.findViewById(R.id.title);
        titleUi.setTextColor(ContextCompat.getColor(myContext, R.color.black80));
        titleUi.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        titleUi.setTypeface(typeface);

        snippetUi = view
                .findViewById(R.id.snippet);
        snippetUi.setTextColor(ContextCompat.getColor(myContext, R.color.black60));
        snippetUi.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        snippetUi.setTypeface(typeface);
        snippetUi.setIncludeFontPadding(false);
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    @Override
    public View getInfoWindow(final Marker marker) {
        view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        final String title = marker.getTitle();
        if (title != null) {
            titleUi.setText(title);
        } else {
            titleUi.setText("");
            titleUi.setVisibility(View.GONE);
        }

        final String snippet = marker.getSnippet();
        if (snippet != null) {
            snippetUi.setText(snippet);
        } else {
            snippetUi.setText("");
            snippetUi.setVisibility(View.GONE);
        }
        return view;
    }
}
