package me.wimanacra.attachment;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import me.wimanacra.config.ACRAConfiguration;

import java.util.ArrayList;

/**
 * @author F43nd1r
 * @since 09.03.2017
 */

public interface AttachmentUriProvider {

    @NonNull
    ArrayList<Uri> getAttachments(Context context, ACRAConfiguration configuration);
}
