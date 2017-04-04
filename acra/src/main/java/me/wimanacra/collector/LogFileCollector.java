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

package me.wimanacra.collector;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import me.wimanacra.ACRA;
import me.wimanacra.ACRAConstants;
import me.wimanacra.ReportField;
import me.wimanacra.builder.ReportBuilder;
import me.wimanacra.config.ACRAConfiguration;
import me.wimanacra.file.Directory;
import me.wimanacra.model.Element;
import me.wimanacra.model.StringElement;
import me.wimanacra.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Collects the N last lines of a text stream. Use this collector if your
 * application handles its own logging system.
 *
 * @author Kevin Gaudin & F43nd1r
 */
final class LogFileCollector extends Collector {
    private final Context context;
    private final ACRAConfiguration config;

    LogFileCollector(Context context, ACRAConfiguration config) {
        super(ReportField.APPLICATION_LOG);
        this.context = context;
        this.config = config;
    }

    /**
     * Reads the last lines of a custom log file. The file name is assumed as
     * located in the {@link Application#getFilesDir()} directory if it does not
     * contain any path separator.
     *
     * @return An Element containing all of the requested lines.
     */
    @NonNull
    @Override
    Element collect(ReportField reportField, ReportBuilder reportBuilder) {
        try {
            return new StringElement(IOUtils.streamToString(
                    getStream(config.applicationLogFileDir(), config.applicationLogFile()),
                    config.applicationLogFileLines()));
        } catch (IOException e) {
            return ACRAConstants.NOT_AVAILABLE;
        }
    }

    /**
     * get the application log file location and open it
     *
     * @param directory the base directory for the file path
     * @param fileName the name of the file
     * @return a stream to the file or an empty stream if the file was not found
     */
    @NonNull
    private InputStream getStream(@NonNull Directory directory, @NonNull String fileName) {
        final File file = directory.getFile(context, fileName);
        if (!file.exists()) {
            if (ACRA.DEV_LOGGING)
                ACRA.log.d(ACRA.LOG_TAG, "Log file '" + file.getPath() + "' does not exist");
        } else if (file.isDirectory()) {
            ACRA.log.e(ACRA.LOG_TAG, "Log file '" + file.getPath() + "' is a directory");
        } else if (!file.canRead()) {
            ACRA.log.e(ACRA.LOG_TAG, "Log file '" + file.getPath() + "' can't be read");
        } else {
            try {
                return new FileInputStream(file);
            } catch (IOException e) {
                ACRA.log.e(ACRA.LOG_TAG, "Could not open stream for log file '" + file.getPath() + "'");
            }
        }
        return new ByteArrayInputStream(new byte[0]);
    }
}
