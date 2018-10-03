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
import java.io.OutputStream;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
final class CountingOutputStream extends OutputStream {
    private final OutputStream delegate;
    long currentSize;

    CountingOutputStream(final OutputStream delegate) {
        this.delegate = delegate;
        currentSize = 0;
    }

    public void write(final int b) throws IOException {
        delegate.write(b);
        currentSize++;
    }

    public void write(final byte[] b) throws IOException {
        delegate.write(b);
        currentSize += b.length;
    }

    public void write(final byte[] b, final int off, final int len) throws IOException {
        delegate.write(b, off, len);
        currentSize += len;
    }

    public void flush() throws IOException {
        delegate.flush();
    }

    public void close() throws IOException {
        delegate.close();
    }
}
