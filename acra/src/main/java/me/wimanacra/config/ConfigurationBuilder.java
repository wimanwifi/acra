/*
 *  Copyright 2011 Kevin Gaudin
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
package me.wimanacra.config;

import android.app.Application;
import android.support.annotation.NonNull;

import me.wimanacra.ACRA;
import me.wimanacra.ReportField;
import me.wimanacra.annotation.NoPropagation;
import me.wimanacra.dialog.CrashReportDialog;
import me.wimanacra.sender.HttpSender;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import me.wimanacra.ACRAConstants;

/**
 * Builder responsible for programmatic construction of an immutable {@link ACRAConfiguration}.
 *
 * @since 4.8.1
 */
@SuppressWarnings("unused")
@me.wimanacra.annotation.ConfigurationBuilder
public final class ConfigurationBuilder extends BaseConfigurationBuilder<ConfigurationBuilder> {

    private final Map<String, String> httpHeaders;
    private final Map<ReportField, Boolean> reportContentChanges;

    /**
     * Constructs a ConfigurationBuilder that is prepopulated with any
     * '@ReportCrashes' annotation declared on the Application class.
     *
     * @param app Current Application, from which any annotated config will be gleaned.
     */
    public ConfigurationBuilder(@NonNull Application app) {
        super(app);
        httpHeaders = new HashMap<String, String>();
        reportContentChanges = new EnumMap<ReportField, Boolean>(ReportField.class);
    }

    /**
     * Builds the {@link ACRAConfiguration} which will be used to configure ACRA.
     * <p>
     * You can pass this {@link ConfigurationBuilder} to {@link ACRA#init(Application, ConfigurationBuilder)} and
     * {@link ACRA#init(Application, ConfigurationBuilder)} will handle any Exception.
     * </p>
     *
     * @return new ACRAConfiguration containing all the properties configured on this builder.
     * @throws ACRAConfigurationException if the required values for the configured notification mode have not been provided.
     */
    @NoPropagation
    @NonNull
    public ACRAConfiguration build() throws ACRAConfigurationException {

        switch (reportingInteractionMode()) {
            case TOAST:
                if (resToastText() == ACRAConstants.DEFAULT_RES_VALUE) {
                    throw new ACRAConfigurationException("TOAST mode: you have to define the resToastText parameter in your application @ReportsCrashes() annotation.");
                }
                break;
            case NOTIFICATION:
                if (resNotifTickerText() == ACRAConstants.DEFAULT_RES_VALUE || resNotifTitle() == ACRAConstants.DEFAULT_RES_VALUE || resNotifText() == ACRAConstants.DEFAULT_RES_VALUE) {
                    throw new ACRAConfigurationException("NOTIFICATION mode: you have to define at least the resNotifTickerText, resNotifTitle, resNotifText parameters in your application @ReportsCrashes() annotation.");
                }
                if (CrashReportDialog.class.equals(reportDialogClass()) && resDialogText() == ACRAConstants.DEFAULT_RES_VALUE) {
                    throw new ACRAConfigurationException("NOTIFICATION mode: using the (default) CrashReportDialog requires you have to define the resDialogText parameter in your application @ReportsCrashes() annotation.");
                }
                break;
            case DIALOG:
                if (CrashReportDialog.class.equals(reportDialogClass()) && resDialogText() == ACRAConstants.DEFAULT_RES_VALUE) {
                    throw new ACRAConfigurationException("DIALOG mode: using the (default) CrashReportDialog requires you to define the resDialogText parameter in your application @ReportsCrashes() annotation.");
                }
                break;
            default:
                break;
        }

        if (reportSenderFactoryClasses().length == 0) {
            throw new ACRAConfigurationException("Report sender factories: using no report senders will make ACRA useless. Configure at least one ReportSenderFactory.");
        }
        checkValidity((Class[]) reportSenderFactoryClasses());
        checkValidity(reportDialogClass(), reportPrimerClass(), retryPolicyClass(), keyStoreFactoryClass());

        return new ACRAConfiguration(this);
    }

    private void checkValidity(Class<?>... classes) throws ACRAConfigurationException {
        for (Class<?> clazz : classes) {
            if (clazz.isInterface()) {
                throw new ACRAConfigurationException("Expected class, but found interface " + clazz.getName() + ".");
            } else if (Modifier.isAbstract(clazz.getModifiers())) {
                throw new ACRAConfigurationException("Class " + clazz.getName() + " cannot be abstract.");
            }
            try {
                clazz.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new ACRAConfigurationException("Class " + clazz.getName() + " is missing a no-args Constructor.", e);
            }
        }
    }

    /**
     * Use this if you want to keep the default configuration of reportContent, but set some fields explicitly.
     *
     * @param field  the field to set
     * @param enable if this field should be reported
     * @return this instance
     */
    @NonNull
    public ConfigurationBuilder setReportField(@NonNull ReportField field, boolean enable) {
        this.reportContentChanges.put(field, enable);
        return this;
    }

    /**
     * Set custom HTTP headers to be sent by the provided {@link HttpSender}.
     * This should be used also by third party senders.
     *
     * @param headers A map associating HTTP header names to their values.
     * @return this instance
     */
    @NonNull
    public ConfigurationBuilder setHttpHeaders(@NonNull Map<String, String> headers) {
        this.httpHeaders.clear();
        this.httpHeaders.putAll(headers);
        return this;
    }

    @NonNull
    Map<String, String> httpHeaders() {
        return httpHeaders;
    }

    @NoPropagation
    @NonNull
    @Override
    ReportField[] customReportContent() {
        return super.customReportContent();
    }

    @NonNull
    Set<ReportField> reportContent() {
        final Set<ReportField> reportContent = new LinkedHashSet<ReportField>();
        if (customReportContent().length != 0) {
            if (ACRA.DEV_LOGGING) ACRA.log.d(ACRA.LOG_TAG, "Using custom Report Fields");
            reportContent.addAll(Arrays.asList(customReportContent()));
        } else if (ACRAConstants.DEFAULT_STRING_VALUE.equals(mailTo())) {
            if (ACRA.DEV_LOGGING) ACRA.log.d(ACRA.LOG_TAG, "Using default Report Fields");
            reportContent.addAll(Arrays.asList(ACRAConstants.DEFAULT_REPORT_FIELDS));
        } else {
            if (ACRA.DEV_LOGGING) ACRA.log.d(ACRA.LOG_TAG, "Using default Mail Report Fields");
            reportContent.addAll(Arrays.asList(ACRAConstants.DEFAULT_MAIL_REPORT_FIELDS));
        }

        // Add or remove any extra fields.
        for (Map.Entry<ReportField, Boolean> entry : reportContentChanges.entrySet()) {
            if (entry.getValue()) {
                reportContent.add(entry.getKey());
            } else {
                reportContent.remove(entry.getKey());
            }
        }
        return reportContent;
    }
}
