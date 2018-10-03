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

import org.jboss.logmanager.formatters.Formatters;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.nio.charset.Charset;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;

/**
 * An output stream handler which supports any {@code OutputStream}, using the specified encoding.  If no encoding is
 * specified, the platform default is used.
 */
public class OutputStreamHandler extends WriterHandler {

    private OutputStream outputStream;
    private Charset charset;

    /**
     * Construct a new instance with no formatter.
     */
    public OutputStreamHandler() {
        setFormatter(Formatters.nullFormatter());
    }

    /**
     * Construct a new instance.
     *
     * @param formatter the formatter to use
     */
    public OutputStreamHandler(final Formatter formatter) {
        setFormatter(formatter);
    }

    /**
     * Construct a new instance.
     *
     * @param outputStream the output stream to use
     * @param formatter the formatter to use
     */
    public OutputStreamHandler(final OutputStream outputStream, final Formatter formatter) {
        setFormatter(formatter);
        setOutputStream(outputStream);
    }

    /**
     * Get the target encoding.
     *
     * @return the target encoding, or {@code null} if the platform default is being used
     */
    public String getEncoding() {
        synchronized (outputLock) {
            return super.getEncoding();
        }
    }

    /**
     * Set the target encoding.
     *
     * @param encoding the new encoding
     * @throws SecurityException if you do not have sufficient permission to invoke this operation
     * @throws java.io.UnsupportedEncodingException if the specified encoding is not supported
     */
    public void setEncoding(final String encoding) throws SecurityException, UnsupportedEncodingException {
        // superclass checks access
        synchronized (outputLock) {
            charset = encoding == null ? null : Charset.forName(encoding);
            super.setEncoding(encoding);
            // we only want to change the writer, not the output stream
            final OutputStream outputStream = this.outputStream;
            if (outputStream != null) {
                super.setWriter(getNewWriter(outputStream));
            }
        }
    }

    /** {@inheritDoc}  Setting a writer will replace any target output stream. */
    public void setWriter(final Writer writer) {
        synchronized (outputLock) {
            super.setWriter(writer);
            final OutputStream oldStream = this.outputStream;
            outputStream = null;
            safeFlush(oldStream);
            safeClose(oldStream);
        }
    }

    /**
     * Set the output stream to write to.  The output stream will then belong to this handler; when the handler is
     * closed or a new writer or output stream is set, this output stream will be closed.
     *
     * @param outputStream the new output stream or {@code null} for none
     */
    public void setOutputStream(final OutputStream outputStream) {
        if (outputStream == null) {
            // call ours, not the superclass one
            this.setWriter(null);
            return;
        }
        // Close the writer, then close the old stream, then establish the new stream with a new writer.
        try {
            synchronized (outputLock) {
                final OutputStream oldStream = this.outputStream;
                // do not close the old stream if creating the writer fails
                final Writer writer = getNewWriter(outputStream);
                try {
                    this.outputStream = outputStream;
                    super.setWriter(writer);
                } finally {
                    safeFlush(oldStream);
                    safeClose(oldStream);
                }
            }
        } catch (Exception e) {
            reportError("Error opening output stream", e, ErrorManager.OPEN_FAILURE);
            return;
        }
    }

    private Writer getNewWriter(OutputStream newOutputStream) {
        if (newOutputStream == null) return null;
        final UninterruptibleOutputStream outputStream = new UninterruptibleOutputStream(new UncloseableOutputStream(newOutputStream));
        final Charset charset = this.charset;
        return charset == null ? new OutputStreamWriter(outputStream) : new OutputStreamWriter(outputStream, charset);
    }
}
