package com.bonacogo.gameplate.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.bonacogo.gameplate.R;
import com.bonacogo.gameplate.adapter.HistoryAdapter;
import com.bonacogo.gameplate.bottomsheetdialog.FilterBottomSheetDialog;
import com.bonacogo.gameplate.model.FilterSObject;
import com.bonacogo.gameplate.model.HistoryObject;
import com.bonacogo.gameplate.util.CommonStrings;
import com.bonacogo.gameplate.util.SharedObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class HistorySearchFragment extends Fragment {
    private static final String TAG = "HistorySearchFragment";

    private String[] LABEL;
    private SharedPreferences prefs;

    // callback per homeFragment
    public interface ActivityCallBack {
        void onItemClick(String name);
    }
    private ActivityCallBack activityCallBack;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activityCallBack = (ActivityCallBack) context;
        LABEL = new String[]{context.getString(R.string.relevance), context.getString(R.string.award)};
        prefs = context.getSharedPreferences(CommonStrings.SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }

    public HistorySearchFragment() {
        super();
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View myFragment = inflater.inflate(R.layout.fragment_history_search, container, false);

        // views
        RecyclerView recyclerView = myFragment.findViewById(R.id.recycler_history);
        Button orderBy = myFragment.findViewById(R.id.order_by);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        // tolgo lo scroll nidificato
        recyclerView.setNestedScrollingEnabled(false);

        HistoryObject historyObject = (HistoryObject) SharedObject.getObject(prefs, HistoryObject.DETAILS_STRING);
        FilterSObject filterSObject = (FilterSObject) SharedObject.getObject(prefs, FilterSObject.DETAILS_STRING);

        FilterBottomSheetDialog filterBottomSheetDialog = FilterBottomSheetDialog.newInstance(filterSObject);
        orderBy.setText(LABEL[filterSObject.getOrder_by()]);
        // specify an adapter
        HistoryAdapter mAdapter = new HistoryAdapter(historyObject);
        // callback onclick
        orderBy.setOnClickListener(v -> {
            FragmentManager fManager = getFragmentManager();
            if (fManager == null)
                return;

            filterBottomSheetDialog.show(fManager, "filter_relevance");
        });
        filterBottomSheetDialog.setClickOrderBy(check -> orderBy.setText(LABEL[check]));
        mAdapter.setAdapterCallBack(name -> activityCallBack.onItemClick(name));
        // imposto l'adapter
        recyclerView.setAdapter(mAdapter);

        return myFragment;
    }
}
