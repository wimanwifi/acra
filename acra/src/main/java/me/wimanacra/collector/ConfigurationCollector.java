/*
 *  Copyright 2010 Emmanuel Astier & Kevin Gaudin
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
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import me.wimanacra.ACRA;
import me.wimanacra.ACRAConstants;
import me.wimanacra.ReportField;
import me.wimanacra.builder.ReportBuilder;
import me.wimanacra.model.ComplexElement;
import me.wimanacra.model.Element;
import org.json.JSONException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Inspects a {@link Configuration} object through reflection API in order to
 * generate a human readable String with values replaced with their constants
 * names. The {@link Configuration#toString()} method was not enough as values
 * like 0, 1, 2 or 3 aren't readable. Using reflection API allows to
 * retrieve hidden fields and can make us hope to be compatible with all Android
 * API levels, even those which are not published yet.
 *
 * @author Kevin Gaudin and F43nd1r
 */
public final class ConfigurationCollector extends Collector {

    private static final String SUFFIX_MASK = "_MASK";
    private static final String FIELD_SCREENLAYOUT = "screenLayout";
    private static final String FIELD_UIMODE = "uiMode";
    private static final String FIELD_MNC = "mnc";
    private static final String FIELD_MCC = "mcc";
    private static final String PREFIX_UI_MODE = "UI_MODE_";
    private static final String PREFIX_TOUCHSCREEN = "TOUCHSCREEN_";
    private static final String PREFIX_SCREENLAYOUT = "SCREENLAYOUT_";
    private static final String PREFIX_ORIENTATION = "ORIENTATION_";
    private static final String PREFIX_NAVIGATIONHIDDEN = "NAVIGATIONHIDDEN_";
    private static final String PREFIX_NAVIGATION = "NAVIGATION_";
    private static final String PREFIX_KEYBOARDHIDDEN = "KEYBOARDHIDDEN_";
    private static final String PREFIX_KEYBOARD = "KEYBOARD_";
    private static final String PREFIX_HARDKEYBOARDHIDDEN = "HARDKEYBOARDHIDDEN_";

    private final Context context;
    private final Element initialConfiguration;

    public ConfigurationCollector(@NonNull Context context, @NonNull Element initialConfiguration) {
        super(ReportField.INITIAL_CONFIGURATION, ReportField.CRASH_CONFIGURATION);
        this.context = context;
        this.initialConfiguration = initialConfiguration;
    }

    @NonNull
    @Override
    Element collect(ReportField reportField, ReportBuilder reportBuilder) {
        switch (reportField) {
            case INITIAL_CONFIGURATION:
                return initialConfiguration;
            case CRASH_CONFIGURATION:
                return collectConfiguration(context);
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Creates an Element listing all values human readable
     * from the provided Configuration instance.
     *
     * @param conf The Configuration to be described.
     * @return An Element describing all the fields of the given Configuration,
     * with values replaced by constant names.
     */
    @NonNull
    private static Element configToElement(@NonNull Configuration conf) {
        final ComplexElement result = new ComplexElement();
        Map<String, SparseArray<String>> valueArrays = getValueArrays();
        for (final Field f : conf.getClass().getFields()) {
            try {
                if (!Modifier.isStatic(f.getModifiers())) {
                    final String fieldName = f.getName();
                    try {
                        if (f.getType().equals(int.class)) {
                            result.put(fieldName, getFieldValueName(valueArrays, conf, f));
                        } else if (f.get(conf) != null) {
                            result.put(fieldName, f.get(conf));
                        }
                    } catch (JSONException e) {
                        ACRA.log.w(ACRA.LOG_TAG, "Could not collect configuration field " + fieldName, e);
                    }
                }
            } catch (@NonNull IllegalArgumentException e) {
                ACRA.log.e(ACRA.LOG_TAG, "Error while inspecting device configuration: ", e);
            } catch (@NonNull IllegalAccessException e) {
                ACRA.log.e(ACRA.LOG_TAG, "Error while inspecting device configuration: ", e);
            }
        }
        return result;
    }

    private static Map<String, SparseArray<String>> getValueArrays() {
        Map<String, SparseArray<String>> valueArrays = new HashMap<String, SparseArray<String>>();
        final SparseArray<String> hardKeyboardHiddenValues = new SparseArray<String>();
        final SparseArray<String> keyboardValues = new SparseArray<String>();
        final SparseArray<String> keyboardHiddenValues = new SparseArray<String>();
        final SparseArray<String> navigationValues = new SparseArray<String>();
        final SparseArray<String> navigationHiddenValues = new SparseArray<String>();
        final SparseArray<String> orientationValues = new SparseArray<String>();
        final SparseArray<String> screenLayoutValues = new SparseArray<String>();
        final SparseArray<String> touchScreenValues = new SparseArray<String>();
        final SparseArray<String> uiModeValues = new SparseArray<String>();

        for (final Field f : Configuration.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())) {
                final String fieldName = f.getName();
                try {
                    if (fieldName.startsWith(PREFIX_HARDKEYBOARDHIDDEN)) {
                        hardKeyboardHiddenValues.put(f.getInt(null), fieldName);
                    } else if (fieldName.startsWith(PREFIX_KEYBOARD)) {
                        keyboardValues.put(f.getInt(null), fieldName);
                    } else if (fieldName.startsWith(PREFIX_KEYBOARDHIDDEN)) {
                        keyboardHiddenValues.put(f.getInt(null), fieldName);
                    } else if (fieldName.startsWith(PREFIX_NAVIGATION)) {
                        navigationValues.put(f.getInt(null), fieldName);
                    } else if (fieldName.startsWith(PREFIX_NAVIGATIONHIDDEN)) {
                        navigationHiddenValues.put(f.getInt(null), fieldName);
                    } else if (fieldName.startsWith(PREFIX_ORIENTATION)) {
                        orientationValues.put(f.getInt(null), fieldName);
                    } else if (fieldName.startsWith(PREFIX_SCREENLAYOUT)) {
                        screenLayoutValues.put(f.getInt(null), fieldName);
                    } else if (fieldName.startsWith(PREFIX_TOUCHSCREEN)) {
                        touchScreenValues.put(f.getInt(null), fieldName);
                    } else if (fieldName.startsWith(PREFIX_UI_MODE)) {
                        uiModeValues.put(f.getInt(null), fieldName);
                    }
                } catch (@NonNull IllegalArgumentException e) {
                    ACRA.log.w(ACRA.LOG_TAG, "Error while inspecting device configuration: ", e);
                } catch (@NonNull IllegalAccessException e) {
                    ACRA.log.w(ACRA.LOG_TAG, "Error while inspecting device configuration: ", e);
                }
            }
        }

        valueArrays.put(PREFIX_HARDKEYBOARDHIDDEN, hardKeyboardHiddenValues);
        valueArrays.put(PREFIX_KEYBOARD, keyboardValues);
        valueArrays.put(PREFIX_KEYBOARDHIDDEN, keyboardHiddenValues);
        valueArrays.put(PREFIX_NAVIGATION, navigationValues);
        valueArrays.put(PREFIX_NAVIGATIONHIDDEN, navigationHiddenValues);
        valueArrays.put(PREFIX_ORIENTATION, orientationValues);
        valueArrays.put(PREFIX_SCREENLAYOUT, screenLayoutValues);
        valueArrays.put(PREFIX_TOUCHSCREEN, touchScreenValues);
        valueArrays.put(PREFIX_UI_MODE, uiModeValues);
        return valueArrays;
    }

    /**
     * Retrieve the name of the constant defined in the {@link Configuration}
     * class which defines the value of a field in a {@link Configuration}
     * instance.
     *
     * @param conf The instance of {@link Configuration} where the value is
     *             stored.
     * @param f    The {@link Field} to be inspected in the {@link Configuration}
     *             instance.
     * @return The value of the field f in instance conf translated to its
     * constant name.
     * @throws IllegalAccessException if the supplied field is inaccessible.
     */
    private static Object getFieldValueName(Map<String, SparseArray<String>> valueArrays, @NonNull Configuration conf, @NonNull Field f) throws IllegalAccessException {
        final String fieldName = f.getName();
        if (fieldName.equals(FIELD_MCC) || fieldName.equals(FIELD_MNC)) {
            return f.getInt(conf);
        } else if (fieldName.equals(FIELD_UIMODE)) {
            return activeFlags(valueArrays.get(PREFIX_UI_MODE), f.getInt(conf));
        } else if (fieldName.equals(FIELD_SCREENLAYOUT)) {
            return activeFlags(valueArrays.get(PREFIX_SCREENLAYOUT), f.getInt(conf));
        } else {
            final SparseArray<String> values = valueArrays.get(fieldName.toUpperCase() + '_');
            if (values == null) {
                // Unknown field, return the raw int as String
                return f.getInt(conf);
            }

            final String value = values.get(f.getInt(conf));
            if (value == null) {
                // Unknown value, return the raw int as String
                return f.getInt(conf);
            }
            return value;
        }
    }

    /**
     * Some fields contain multiple value types which can be isolated by
     * applying a bitmask. That method returns the concatenation of active
     * values.
     *
     * @param valueNames The array containing the different values and names for this
     *                   field. Must contain mask values too.
     * @param bitfield   The bitfield to inspect.
     * @return The names of the different values contained in the bitfield,
     * separated by '+'.
     */
    @NonNull
    private static String activeFlags(@NonNull SparseArray<String> valueNames, int bitfield) {
        final StringBuilder result = new StringBuilder();

        // Look for masks, apply it an retrieve the masked value
        for (int i = 0; i < valueNames.size(); i++) {
            final int maskValue = valueNames.keyAt(i);
            if (valueNames.get(maskValue).endsWith(SUFFIX_MASK)) {
                final int value = bitfield & maskValue;
                if (value > 0) {
                    if (result.length() > 0) {
                        result.append('+');
                    }
                    result.append(valueNames.get(value));
                }
            }
        }
        return result.toString();
    }

    /**
     * Returns the current Configuration for this application.
     *
     * @param context Context for the application being reported.
     * @return A String representation of the current configuration for the application.
     */
    @NonNull
    public static Element collectConfiguration(@NonNull Context context) {
        try {
            return configToElement(context.getResources().getConfiguration());
        } catch (RuntimeException e) {
            ACRA.log.w(ACRA.LOG_TAG, "Couldn't retrieve CrashConfiguration for : " + context.getPackageName(), e);
            return ACRAConstants.NOT_AVAILABLE;
        }
    }
}