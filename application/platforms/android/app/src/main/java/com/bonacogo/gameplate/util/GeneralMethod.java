package com.bonacogo.gameplate.util;

import android.app.ProgressDialog;
import android.content.Context;

public class GeneralMethod {
    // mostra la progressdialog
    public static ProgressDialog showProgressDialog(ProgressDialog mProgressDialog, Context context, String message) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setMessage(message);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
        return mProgressDialog;
    }

    // nascondi la progressdialog
    public static void hideProgressDialog(ProgressDialog mProgressDialog) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
