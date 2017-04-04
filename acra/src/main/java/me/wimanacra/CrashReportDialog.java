package me.wimanacra;

import android.os.Bundle;


/**
 * Old crash report dialog.
 *
 * @deprecated since 4.8.0 use {@link me.wimanacra.dialog.CrashReportDialog} instead
 **/
public final class CrashReportDialog extends me.wimanacra.dialog.CrashReportDialog {

    @Override
    protected void buildAndShowDialog(Bundle savedInstanceState){
        ACRA.log.w(ACRA.LOG_TAG, "me.wimanacra.CrashReportDialog has been deprecated. Please use me.wimanacra.dialog.CrashReportDialog instead");
        super.buildAndShowDialog(savedInstanceState);
    }
}