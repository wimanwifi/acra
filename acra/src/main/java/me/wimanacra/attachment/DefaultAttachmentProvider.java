package me.wimanacra.attachment;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import me.wimanacra.ACRA;
import me.wimanacra.config.ACRAConfiguration;

import java.util.ArrayList;

/**
 * @author F43nd1r
 * @since 10.03.2017
 */

public class DefaultAttachmentProvider implements AttachmentUriProvider {
    @NonNull
    @Override
    public ArrayList<Uri> getAttachments(Context context, ACRAConfiguration configuration) {
        final ArrayList<Uri> result = new ArrayList<Uri>();
        for (String s : configuration.attachmentUris()){
            try {
                result.add(Uri.parse(s));
            }catch (Exception e){
                ACRA.log.e(ACRA.LOG_TAG, "Failed to parse Uri " + s, e);
            }
        }
        return result;
    }
}
