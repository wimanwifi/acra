/*
 *  Copyright 2016
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
package me.wimanacra.collector;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.hardware.display.DisplayManagerCompat;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Display;
import android.view.Surface;

import me.wimanacra.ACRA;
import me.wimanacra.ReportField;
import me.wimanacra.builder.ReportBuilder;
import me.wimanacra.model.ComplexElement;
import me.wimanacra.model.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Collects information about the connected display(s)
 *
 * @author (original author unknown) & F43nd1r
 */
final class DisplayManagerCollector extends Collector {
    private final Context context;
    private final SparseArray<String> flagNames = new SparseArray<String>();

    DisplayManagerCollector(Context context) {
        super(ReportField.DISPLAY);
        this.context = context;
    }


    @NonNull
    @Override
    Element collect(ReportField reportField, ReportBuilder reportBuilder) {
        final ComplexElement result = new ComplexElement();
        for (Display display : DisplayManagerCompat.getInstance(context).getDisplays()) {
            try {
                result.put(String.valueOf(display.getDisplayId()), collectDisplayData(display));
            } catch (JSONException e) {
                ACRA.log.w(ACRA.LOG_TAG, "Failed to collect data for display " + display.getDisplayId(), e);
            }
        }

        return result;
    }

    @NonNull
    private JSONObject collectDisplayData(@NonNull Display display) throws JSONException {
        final DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        final JSONObject result = new JSONObject();
        collectCurrentSizeRange(display, result);
        collectFlags(display, result);
        collectMetrics(display, result);
        collectRealMetrics(display, result);
        collectName(display, result);
        collectRealSize(display, result);
        collectRectSize(display, result);
        collectSize(display, result);
        collectRotation(display, result);
        collectIsValid(display, result);
        result.put("orientation", display.getRotation())
                .put("refreshRate", display.getRefreshRate());
        //noinspection deprecation
        result.put("height", display.getHeight())
                .put("width", display.getWidth())
                .put("pixelFormat", display.getPixelFormat());
        return result;
    }

    private static void collectIsValid(@NonNull Display display, JSONObject container) throws JSONException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            container.put("isValid", display.isValid());
        }
    }

    private static void collectRotation(@NonNull Display display, JSONObject container) throws JSONException {
        container.put("rotation", rotationToString(display.getRotation()));
    }

    @NonNull
    private static String rotationToString(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_0:
                return "ROTATION_0";
            case Surface.ROTATION_90:
                return "ROTATION_90";
            case Surface.ROTATION_180:
                return "ROTATION_180";
            case Surface.ROTATION_270:
                return "ROTATION_270";
            default:
                return String.valueOf(rotation);
        }
    }

    private static void collectRectSize(@NonNull Display display, JSONObject container) throws JSONException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            final Rect size = new Rect();
            display.getRectSize(size);
            container.put("rectSize", new JSONArray(Arrays.asList(size.top, size.left, size.width(), size.height())));
        }
    }

    private static void collectSize(@NonNull Display display, JSONObject container) throws JSONException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            final Point size = new Point();
            display.getSize(size);
            container.put("size", new JSONArray(Arrays.asList(size.x, size.y)));
        }
    }

    private static void collectRealSize(@NonNull Display display, JSONObject container) throws JSONException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            final Point size = new Point();
            display.getRealSize(size);
            container.put("realSize", new JSONArray(Arrays.asList(size.x, size.y)));
        }
    }

    private static void collectCurrentSizeRange(@NonNull Display display, @NonNull JSONObject container) throws JSONException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            final Point smallest = new Point();
            final Point largest = new Point();
            display.getCurrentSizeRange(smallest, largest);
            JSONObject result = new JSONObject();
            result.put("smallest", new JSONArray(Arrays.asList(smallest.x, smallest.y)));
            result.put("largest", new JSONArray(Arrays.asList(largest.x, largest.y)));
            container.put("currentSizeRange", result);
        }
    }

    private void collectFlags(@NonNull Display display, @NonNull JSONObject container) throws JSONException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            final int flags = display.getFlags();
            for (Field field : display.getClass().getFields()) {
                if (field.getName().startsWith("FLAG_")) {
                    try {
                        flagNames.put(field.getInt(null), field.getName());
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
            container.put("flags", activeFlags(flags));
        }
    }

    private static void collectName(@NonNull Display display, JSONObject container) throws JSONException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            container.put("name", display.getName());
        }
    }

    private static void collectMetrics(@NonNull Display display, JSONObject container) throws JSONException {
        final DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        JSONObject result = new JSONObject();
        collectMetrics(metrics, result);
        container.put("metrics", result);
    }

    private static void collectRealMetrics(@NonNull Display display, JSONObject container) throws JSONException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            final DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);
            JSONObject result = new JSONObject();
            collectMetrics(metrics, result);
            container.put("realMetrics", result);
        }
    }

    private static void collectMetrics(@NonNull DisplayMetrics metrics, JSONObject container) throws JSONException {
        container.put("density", metrics.density)
                .put("densityDpi", metrics.densityDpi)
                .put("scaledDensity", "x" + metrics.scaledDensity)
                .put("widthPixels", metrics.widthPixels)
                .put("heightPixels", metrics.heightPixels)
                .put("xdpi", metrics.xdpi)
                .put("ydpi", metrics.ydpi);
    }

    /**
     * Some fields contain multiple value types which can be isolated by
     * applying a bitmask. That method returns the concatenation of active
     * values.
     *
     * @param bitfield The bitfield to inspect.
     * @return The names of the different values contained in the bitfield,
     * separated by '+'.
     */
    @NonNull
    private String activeFlags(int bitfield) {
        final StringBuilder result = new StringBuilder();

        // Look for masks, apply it an retrieve the masked value
        for (int i = 0; i < flagNames.size(); i++) {
            final int maskValue = flagNames.keyAt(i);
            final int value = bitfield & maskValue;
            if (value > 0) {
                if (result.length() > 0) {
                    result.append('+');
                }
                result.append(flagNames.get(value));
            }
        }
        return result.toString();
    }

}
