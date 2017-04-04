package me.wimanacra.sender;

import android.content.Context;
import android.support.annotation.NonNull;

import me.wimanacra.config.ACRAConfiguration;

/**
 * Constructs an {@link EmailIntentSender}.
 */
public final class EmailIntentSenderFactory implements ReportSenderFactory {

    @NonNull
    @Override
    public ReportSender create(@NonNull Context context, @NonNull ACRAConfiguration config) {
        return new EmailIntentSender(config);
    }
}
