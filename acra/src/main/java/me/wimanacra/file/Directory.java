/*
 * Copyright (c) 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.wimanacra.file;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import java.io.File;

/**
 * @author F43nd1r
 * @since 4.9.1
 */
public enum Directory {
    /**
     * Legacy behaviour:
     * If the string starts with a path separator, this behaves like {@link #ROOT}.
     * Otherwise it behaves like {@link #FILES}.
     */
    FILES_LEGACY {
        @Override
        public File getFile(@NonNull Context context, @NonNull String fileName) {
            return (fileName.startsWith("/") ? Directory.ROOT : Directory.FILES).getFile(context, fileName);
        }
    },
    /**
     * Directory returned by {@link Context#getFilesDir()}
     */
    FILES {
        @Override
        public File getFile(@NonNull Context context, @NonNull String fileName) {
            return new File(context.getFilesDir(), fileName);
        }
    },
    /**
     * Directory returned by {@link Context#getExternalFilesDir(String)}
     */
    EXTERNAL_FILES {
        @Override
        public File getFile(@NonNull Context context, @NonNull String fileName) {
            return new File(context.getExternalFilesDir(null), fileName);
        }
    },
    /**
     * Directory returned by {@link Context#getCacheDir()}
     */
    CACHE {
        @Override
        public File getFile(@NonNull Context context, @NonNull String fileName) {
            return new File(context.getCacheDir(), fileName);
        }
    },
    /**
     * Directory returned by {@link Context#getExternalCacheDir()}
     */
    EXTERNAL_CACHE {
        @Override
        public File getFile(@NonNull Context context, @NonNull String fileName) {
            return new File(context.getExternalCacheDir(), fileName);
        }
    },
    /**
     * Directory returned by {@link Context#getNoBackupFilesDir()}.
     * Will fall back to {@link Context#getFilesDir()} on API &lt; 21
     */
    NO_BACKUP_FILES {
        @Override
        public File getFile(@NonNull Context context, @NonNull String fileName) {
            return new File(ContextCompat.getNoBackupFilesDir(context), fileName);
        }
    },
    /**
     * Directory returned by {@link Environment#getExternalStorageDirectory()}
     */
    EXTERNAL_STORAGE {
        @Override
        public File getFile(@NonNull Context context, @NonNull String fileName) {
            return new File(Environment.getExternalStorageDirectory(), fileName);
        }
    },
    /**
     * Root Directory, paths in this directory are absolute paths
     */
    ROOT {
        @Override
        public File getFile(@NonNull Context context, @NonNull String fileName) {
            return new File("/", fileName);
        }
    };

    public abstract File getFile(@NonNull Context context, @NonNull String fileName);
}
