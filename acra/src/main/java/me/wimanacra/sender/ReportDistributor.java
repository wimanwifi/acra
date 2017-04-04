/*
 *  Copyright 2012 Kevin Gaudin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package me.wimanacra.sender;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import me.wimanacra.ACRA;
import me.wimanacra.collector.CrashReportData;
import me.wimanacra.config.ACRAConfiguration;
import me.wimanacra.config.DefaultRetryPolicy;
import me.wimanacra.config.RetryPolicy;
import me.wimanacra.file.CrashReportPersister;
import me.wimanacra.util.IOUtils;
import me.wimanacra.util.InstanceCreator;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Distributes reports to all Senders.
 *
 * @author William Ferguson
 * @since 4.8.0
 */
final class ReportDistributor {

    private final Context context;
    private final ACRAConfiguration config;
    private final List<ReportSender> reportSenders;

    /**
     * Creates a new {@link ReportDistributor} to try sending pending reports.
     *
     * @param context               ApplicationContext in which the reports are being sent.
     * @param config                Configuration to use while sending.
     * @param reportSenders         List of ReportSender to use to send the crash reports.
     */
    ReportDistributor(@NonNull Context context, @NonNull ACRAConfiguration config, @NonNull List<ReportSender> reportSenders) {
        this.context = context;
        this.config = config;
        this.reportSenders = reportSenders;
    }

    /**
     * Send report via all senders.
     *
     * @param reportFile    Report to send.
     */
    public void distribute(@NonNull File reportFile) {

        ACRA.log.i(ACRA.LOG_TAG, "Sending report " + reportFile );
        try {
            final CrashReportPersister persister = new CrashReportPersister();
            final CrashReportData previousCrashReport = persister.load(reportFile);
            sendCrashReport(previousCrashReport);
            IOUtils.deleteReport(reportFile);
        } catch (RuntimeException e) {
            ACRA.log.e(ACRA.LOG_TAG, "Failed to send crash reports for " + reportFile, e);
            IOUtils.deleteReport(reportFile);
        } catch (IOException e) {
            ACRA.log.e(ACRA.LOG_TAG, "Failed to load crash report for " + reportFile, e);
            IOUtils.deleteReport(reportFile);
        } catch (JSONException e) {
            ACRA.log.e(ACRA.LOG_TAG, "Failed to load crash report for " + reportFile, e);
            IOUtils.deleteReport(reportFile);
        }catch (ReportSenderException e) {
            ACRA.log.e(ACRA.LOG_TAG, "Failed to send crash report for " + reportFile, e);
            // An issue occurred while sending this report but we can still try to
            // send other reports. Report sending is limited by ACRAConstants.MAX_SEND_REPORTS
            // so there's not much to fear about overloading a failing server.
        }
    }

    /**
     * Sends the report with all configured ReportSenders. If at least one
     * sender completed its job, the report is considered as sent and will not
     * be sent again for failing senders.
     *
     * @param errorContent  Crash data.
     * @throws ReportSenderException if unable to send the crash report.
     */
    private void sendCrashReport(@NonNull CrashReportData errorContent) throws ReportSenderException {
        if (!isDebuggable() || config.sendReportsInDevMode()) {
            final List<RetryPolicy.FailedSender> failedSenders = new LinkedList<RetryPolicy.FailedSender>();
            for (ReportSender sender : reportSenders) {
                try {
                    if (ACRA.DEV_LOGGING) ACRA.log.d(ACRA.LOG_TAG, "Sending report using " + sender.getClass().getName());
                    sender.send(context, errorContent);
                    if (ACRA.DEV_LOGGING) ACRA.log.d(ACRA.LOG_TAG, "Sent report using " + sender.getClass().getName());
                } catch (ReportSenderException e) {
                    failedSenders.add(new RetryPolicy.FailedSender(sender, e));
                }
            }

            final InstanceCreator instanceCreator = new InstanceCreator();
            if (failedSenders.isEmpty()) {
                if (ACRA.DEV_LOGGING) ACRA.log.d(ACRA.LOG_TAG, "Report was sent by all senders");
            } else if (instanceCreator.create(config.retryPolicyClass(), new DefaultRetryPolicy()).shouldRetrySend(reportSenders, failedSenders)) {
                final Throwable firstFailure = failedSenders.get(0).getException();
                throw new ReportSenderException("Policy marked this task as incomplete. ACRA will try to send this report again.", firstFailure);
            } else {
                final StringBuilder builder = new StringBuilder("ReportSenders of classes [");
                for (final RetryPolicy.FailedSender failedSender : failedSenders) {
                    builder.append(failedSender.getSender().getClass().getName());
                    builder.append(", ");
                }
                builder.append("] failed, but Policy marked this task as complete. ACRA will not send this report again.");
                ACRA.log.w(ACRA.LOG_TAG, builder.toString());
            }
        }
    }

    /**
     * Returns true if the application is debuggable.
     *
     * @return true if the application is debuggable.
     */
    private boolean isDebuggable() {
        final PackageManager pm = context.getPackageManager();
        try {
            return (pm.getApplicationInfo(context.getPackageName(), 0).flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
