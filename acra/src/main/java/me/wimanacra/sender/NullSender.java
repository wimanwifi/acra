package me.wimanacra.sender;

import android.content.Context;
import android.support.annotation.NonNull;

import me.wimanacra.ACRA;
import me.wimanacra.collector.CrashReportData;

/**
 * Sends no report.
 */
final class NullSender implements ReportSender {
    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData errorContent) throws ReportSenderException {
        ACRA.log.w(ACRA.LOG_TAG, context.getPackageName() + " reports will NOT be sent - no valid ReportSender is configured. Try setting 'formUri' or 'mailTo'");
    }
}
