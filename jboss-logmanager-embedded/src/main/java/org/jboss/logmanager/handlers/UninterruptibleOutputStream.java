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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.interrupted;

/**
 * An output stream which is not interruptible.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class UninterruptibleOutputStream extends OutputStream {
    private final OutputStream out;

    /**
     * Construct a new instance.
     *
     * @param out the delegate stream
     */
    public UninterruptibleOutputStream(final OutputStream out) {
        this.out = out;
    }

    /**
     * Write the given byte uninterruptibly.
     *
     * @param b the byte to write
     * @throws IOException if an error occurs
     */
    public void write(final int b) throws IOException {
        boolean intr = false;
        try {
            for (;;) try {
                out.write(b);
                return;
            } catch (InterruptedIOException e) {
                final int transferred = e.bytesTransferred;
                if (transferred == 1) {
                    return;
                }
                intr |= interrupted();
            }
        } finally {
            if (intr) {
                currentThread().interrupt();
            }
        }
    }

    /**
     * Write the given bytes uninterruptibly.
     *
     * @param b the bytes to write
     * @param off the offset into the array
     * @param len the length of the array to write
     * @throws IOException if an error occurs
     */
    public void write(final byte[] b, int off, int len) throws IOException {
        boolean intr = false;
        try {
            while (len > 0) try {
                out.write(b, off, len);
                return;
            } catch (InterruptedIOException e) {
                final int transferred = e.bytesTransferred;
                if (transferred > 0) {
                    off += transferred;
                    len -= transferred;
                }
                intr |= interrupted();
            }
        } finally {
            if (intr) {
                currentThread().interrupt();
            }
        }
    }

    /**
     * Flush the stream uninterruptibly.
     *
     * @throws IOException if an error occurs
     */
    public void flush() throws IOException {
        boolean intr = false;
        try {
            for (;;) try {
                out.flush();
                return;
            } catch (InterruptedIOException e) {
                intr |= interrupted();
            }
        } finally {
            if (intr) {
                currentThread().interrupt();
            }
        }
    }

    /**
     * Close the stream uninterruptibly.
     *
     * @throws IOException if an error occurs
     */
    public void close() throws IOException {
        boolean intr = false;
        try {
            for (;;) try {
                out.close();
                return;
            } catch (InterruptedIOException e) {
                intr |= interrupted();
            }
        } finally {
            if (intr) {
                currentThread().interrupt();
            }
        }
    }

    /**
     * Get the string representation of this stream.
     *
     * @return the string
     */
    public String toString() {
        return "uninterruptible " + out.toString();
    }
}
