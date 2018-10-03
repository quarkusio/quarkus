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

package org.jboss.logmanager;

import org.junit.Test;
import static org.junit.Assert.*;
import org.jboss.logmanager.handlers.WriterHandler;
import org.jboss.logmanager.handlers.OutputStreamHandler;
import org.jboss.logmanager.handlers.NullHandler;
import org.jboss.logmanager.handlers.FileHandler;
import org.jboss.logmanager.formatters.PatternFormatter;

import java.util.logging.Formatter;

import java.io.StringWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public final class HandlerTests {

    private final Formatter testFormatter = new PatternFormatter("%m");

    @Test
    public void testNullHandler() throws Throwable {
        final NullHandler handler = new NullHandler();
        handler.setLevel(Level.ALL);
        handler.publish(new ExtLogRecord(Level.INFO, "Test message", null));
    }

    private void initHandler(ExtHandler handler) throws UnsupportedEncodingException {
        handler.setFormatter(testFormatter);
        handler.setLevel(Level.ALL);
        handler.setAutoFlush(true);
        handler.setEncoding("utf-8");
    }

    private void testPublish(ExtHandler handler) {
        testPublish(handler, Level.INFO);
    }

    private void testPublish(ExtHandler handler, Level level) {
        handler.publish(new ExtLogRecord(level, "Test message", null));
    }

    @Test
    public void testWriterHandler() throws Throwable {
        final WriterHandler handler = new WriterHandler();
        initHandler(handler);
        final StringWriter writer = new StringWriter();
        handler.setWriter(writer);
        testPublish(handler);
        assertEquals("Test message", writer.toString());
    }

    @Test
    public void testOutputStreamHandler() throws Throwable {
        final OutputStreamHandler handler = new OutputStreamHandler();
        initHandler(handler);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        handler.setOutputStream(stream);
        testPublish(handler);
        assertEquals("Test message", new String(stream.toByteArray(), "utf-8"));
    }

    @Test
    public void testFileHandler() throws Throwable {
        final FileHandler handler = new FileHandler();
        initHandler(handler);
        final File tempFile = File.createTempFile("jblm-", ".log");
        try {
            handler.setAppend(true);
            handler.setFile(tempFile);
            testPublish(handler);
            handler.setFile(tempFile);
            testPublish(handler);
            handler.setAppend(false);
            handler.setFile(tempFile);
            testPublish(handler);
            handler.close();
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                final FileInputStream is = new FileInputStream(tempFile);
                try {
                    int r;
                    while ((r = is.read()) != -1) os.write(r);
                    assertEquals("Test message", new String(os.toByteArray(), "utf-8"));
                    tempFile.deleteOnExit();
                } finally {
                    is.close();
                }
            } finally {
                os.close();
            }
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void testEnableDisableHandler() throws Throwable {
        final StringListHandler handler = new StringListHandler();
        testPublish(handler);
        assertEquals(1, handler.size());
        handler.setEnabled(false);
        testPublish(handler);
        assertEquals(1, handler.size());
        handler.setEnabled(true);
        testPublish(handler);
        assertEquals(2, handler.size());
    }

    @Test
    public void testHandlerDelegation() throws Throwable {
        final StringListHandler debugHandler = new StringListHandler();
        debugHandler.setLevel(Level.DEBUG);

        final StringListHandler infoHandler = new StringListHandler();
        infoHandler.setLevel(Level.INFO);

        final StringListHandler errorHandler = new StringListHandler();
        errorHandler.setLevel(Level.ERROR);

        final MultiHandler handler = MultiHandler.of(debugHandler, infoHandler, errorHandler);
        // Turn off the level for the handler
        handler.setLevel(Level.OFF);

        // Log a debug message
        testPublish(handler, Level.DEBUG);
        assertEquals(1, debugHandler.size());
        assertEquals(0, infoHandler.size());
        assertEquals(0, errorHandler.size());

        // Log an info message
        testPublish(handler, Level.INFO);
        assertEquals(2, debugHandler.size());
        assertEquals(1, infoHandler.size());
        assertEquals(0, errorHandler.size());

        // Log a warn message
        testPublish(handler, Level.WARN);
        assertEquals(3, debugHandler.size());
        assertEquals(2, infoHandler.size());
        assertEquals(0, errorHandler.size());

        // Log an error message
        testPublish(handler, Level.ERROR);
        assertEquals(4, debugHandler.size());
        assertEquals(3, infoHandler.size());
        assertEquals(1, errorHandler.size());
    }

    static class MultiHandler extends ExtHandler {
        protected MultiHandler() {
        }

        public void publish(final LogRecord record) {
            if (isEnabled() && record != null) {
                doPublish(ExtLogRecord.wrap(record));
            }
        }

        public void publish(final ExtLogRecord record) {
            if (isEnabled() && record != null) {
                doPublish(record);
            }
        }

        static MultiHandler of(final ExtHandler... handlers) {
            final MultiHandler result = new MultiHandler();
            result.setHandlers(handlers);
            return result;
        }

        @Override
        protected void doPublish(final ExtLogRecord record) {
            // Process child handlers
            for (Handler handler : getHandlers()) {
                if (handler.isLoggable(record)) {
                    handler.publish(record);
                }
            }
            super.doPublish(record);
        }
    }
}
