package com.bonacogo.gameplate.bottomsheetdialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.model.FilterSObject;
import com.bonacogo.gameplate.radiobutton.PresetRadioGroup;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FilterBottomSheetDialog extends BottomSheetDialogFragment {
    private SharedPreferences prefs;
    private PresetRadioGroup radioOrderBy;
    private FilterSObject filterSObject;

    private ClickOrderBy clickOrderBy;

    public FilterBottomSheetDialog() {
    }

    public static FilterBottomSheetDialog newInstance(FilterSObject filterSObject) {
        // restituisco l'istanza di FilterBottomSheetDialog passandogli come argomento filterSObject
        FilterBottomSheetDialog filterBottomSheetDialog = new FilterBottomSheetDialog();
        Bundle args = new Bundle();
        args.putSerializable("filterSObject", filterSObject);
        filterBottomSheetDialog.setArguments(args);
        return filterBottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_dialog_filter, container, false);
        Bundle args = getArguments();

        // estraggo il parametro
        filterSObject = (FilterSObject) args.getSerializable("filterSObject");

        radioOrderBy = view.findViewById(R.id.radio_order_by);

        if (prefs == null)
            prefs = getContext().getSharedPreferences("com.bonacogo.gameplate", Context.MODE_PRIVATE);

        // imposto order_by
        radioOrderBy.setChecked(filterSObject.getOrder_by());

        radioOrderBy.setOnCheckedChangeListener((radioGroup, radioButton, isChecked, checkedId) -> {
            clickOrderBy.onItemClick(radioOrderBy.getChecked());
            final Handler handler = new Handler();
            handler.postDelayed(this::dismiss, 300);
        });

        return view;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        filterSObject.setOrder_by(radioOrderBy.getChecked(), prefs);
    }

    public interface ClickOrderBy {
        void onItemClick(int check);
    }

    public void setClickOrderBy(ClickOrderBy clickOrderBy) {
        this.clickOrderBy = clickOrderBy;
    }
}
