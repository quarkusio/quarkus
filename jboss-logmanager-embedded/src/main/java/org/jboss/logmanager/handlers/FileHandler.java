/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.logmanager.handlers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.logging.Formatter;

/**
 * A simple file handler.
 */
public class FileHandler extends OutputStreamHandler {

    private File file;
    private boolean append;

    /**
     * Construct a new instance with no formatter and no output file.
     */
    public FileHandler() {
    }

    /**
     * Construct a new instance with the given formatter and no output file.
     *
     * @param formatter the formatter
     */
    public FileHandler(final Formatter formatter) {
        super(formatter);
    }

    /**
     * Construct a new instance with the given formatter and output file.
     *
     * @param formatter the formatter
     * @param file the file
     * @throws FileNotFoundException if the file could not be found on open
     */
    public FileHandler(final Formatter formatter, final File file) throws FileNotFoundException {
        super(formatter);
        setFile(file);
    }

    /**
     * Construct a new instance with the given formatter, output file, and append setting.
     *
     * @param formatter the formatter
     * @param file the file
     * @param append {@code true} to append, {@code false} to overwrite
     * @throws FileNotFoundException if the file could not be found on open
     */
    public FileHandler(final Formatter formatter, final File file, final boolean append) throws FileNotFoundException {
        super(formatter);
        this.append = append;
        setFile(file);
    }

    /**
     * Construct a new instance with the given output file.
     *
     * @param file the file
     * @throws FileNotFoundException if the file could not be found on open
     */
    public FileHandler(final File file) throws FileNotFoundException {
        setFile(file);
    }

    /**
     * Construct a new instance with the given output file and append setting.
     *
     * @param file the file
     * @param append {@code true} to append, {@code false} to overwrite
     * @throws FileNotFoundException if the file could not be found on open
     */
    public FileHandler(final File file, final boolean append) throws FileNotFoundException {
        this.append = append;
        setFile(file);
    }

    /**
     * Construct a new instance with the given output file.
     *
     * @param fileName the file name
     * @throws FileNotFoundException if the file could not be found on open
     */
    public FileHandler(final String fileName) throws FileNotFoundException {
        setFileName(fileName);
    }

    /**
     * Construct a new instance with the given output file and append setting.
     *
     * @param fileName the file name
     * @param append {@code true} to append, {@code false} to overwrite
     * @throws FileNotFoundException if the file could not be found on open
     */
    public FileHandler(final String fileName, final boolean append) throws FileNotFoundException {
        this.append = append;
        setFileName(fileName);
    }

    /**
     * Specify whether to append to the target file.
     *
     * @param append {@code true} to append, {@code false} to overwrite
     */
    public void setAppend(final boolean append) {
        synchronized (outputLock) {
            this.append = append;
        }
    }

    /**
     * Set the output file.
     *
     * @param file the file
     * @throws FileNotFoundException if an error occurs opening the file
     */
    public void setFile(File file) throws FileNotFoundException {
        synchronized (outputLock) {
            if (file == null) {
                this.file = null;
                setOutputStream(null);
                return;
            }
            final File parentFile = file.getParentFile();
            if (parentFile != null) {
                parentFile.mkdirs();
            }
            boolean ok = false;
            final FileOutputStream fos = new FileOutputStream(file, append);
            try {
                final OutputStream bos = new BufferedOutputStream(fos);
                try {
                    setOutputStream(bos);
                    this.file = file;
                    ok = true;
                } finally {
                    if (! ok) {
                        safeClose(bos);
                    }
                }
            } finally {
                if (! ok) {
                    safeClose(fos);
                }
            }
        }
    }

    /**
     * Get the current output file.
     *
     * @return the file
     */
    public File getFile() {
        synchronized (outputLock) {
            return file;
        }
    }

    /**
     * Set the output file by name.
     *
     * @param fileName the file name
     * @throws FileNotFoundException if an error occurs opening the file
     */
    public void setFileName(String fileName) throws FileNotFoundException {
        setFile(fileName == null ? null : new File(fileName));
    }
}
