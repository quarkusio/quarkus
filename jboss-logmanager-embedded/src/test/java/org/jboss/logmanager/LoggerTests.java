/*
 * Copyright 2018 Red Hat, Inc.
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

import org.jboss.logmanager.filters.RegexFilter;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import java.util.concurrent.atomic.AtomicBoolean;

public final class LoggerTests {

    @Test
    public void testInstall() {
        assertTrue("Wrong logger class", (java.util.logging.Logger.getLogger("test") instanceof Logger));
    }

    @Test
    public void testCategories() {
        assertNotNull("Logger not created with category: " + LoggerTests.class.getName(), Logger.getLogger(LoggerTests.class.getName()));
        assertNotNull("Logger not created with category: Spaced Logger Name", Logger.getLogger("Spaced Logger Name"));
        assertNotNull("Logger not created with category: /../Weird/Path", Logger.getLogger("/../Weird/Path"));
        assertNotNull("Logger not created with category: random.chars.`~!@#$%^&*()-=_+[]{}\\|;':\",.<>/?", Logger.getLogger("random.chars.`~!@#$%^&*()-=_+[]{}\\|;':\",.<>/?"));
    }

    @Test
    public void testHandlerAdd() {
        final NullHandler h1 = new NullHandler();
        final NullHandler h2 = new NullHandler();
        final NullHandler h3 = new NullHandler();
        final Logger logger = Logger.getLogger("testHandlerAdd");
        logger.addHandler(h1);
        logger.addHandler(h2);
        logger.addHandler(h3);
        boolean f1 = false;
        boolean f2 = false;
        boolean f3 = false;
        for (Handler handler : logger.getHandlers()) {
            if (handler == h1) f1 = true;
            if (handler == h2) f2 = true;
            if (handler == h3) f3 = true;
        }
        assertTrue("Handler 1 missing", f1);
        assertTrue("Handler 2 missing", f2);
        assertTrue("Handler 3 missing", f3);
    }

    @Test
    public void testHandlerAdd2() {
        final NullHandler h1 = new NullHandler();
        final Logger logger = Logger.getLogger("testHandlerAdd2");
        logger.addHandler(h1);
        logger.addHandler(h1);
        logger.addHandler(h1);
        boolean f1 = false;
        final Handler[] handlers = logger.getHandlers();
        for (Handler handler : handlers) {
            if (handler == h1) f1 = true;
        }
        assertTrue("Handler 1 missing", f1);
        assertEquals("Extra handlers missing", 3, handlers.length);
    }

    @Test
    public void testHandlerRemove() {
        final NullHandler h1 = new NullHandler();
        final NullHandler h2 = new NullHandler();
        final NullHandler h3 = new NullHandler();
        final Logger logger = Logger.getLogger("testHandlerRemove");
        logger.addHandler(h1);
        logger.addHandler(h2);
        logger.addHandler(h3);
        logger.removeHandler(h1);
        boolean f1 = false;
        boolean f2 = false;
        boolean f3 = false;
        for (Handler handler : logger.getHandlers()) {
            if (handler == h1) f1 = true;
            if (handler == h2) f2 = true;
            if (handler == h3) f3 = true;
        }
        assertFalse("Handler 1 wasn't removed", f1);
        assertTrue("Handler 2 missing", f2);
        assertTrue("Handler 3 missing", f3);
    }

    @Test
    public void testHandlerRemove2() {
        final NullHandler h1 = new NullHandler();
        final Logger logger = Logger.getLogger("testHandlerRemove2");
        logger.removeHandler(h1);
        final Handler[] handlers = logger.getHandlers();
        assertEquals(0, handlers.length);
    }

    @Test
    public void testHandlerClear() {
        final NullHandler h1 = new NullHandler();
        final NullHandler h2 = new NullHandler();
        final NullHandler h3 = new NullHandler();
        final Logger logger = Logger.getLogger("testHandlerClear");
        logger.addHandler(h1);
        logger.addHandler(h2);
        logger.addHandler(h3);
        logger.clearHandlers();
        boolean f1 = false;
        boolean f2 = false;
        boolean f3 = false;
        for (Handler handler : logger.getHandlers()) {
            if (handler == h1) f1 = true;
            if (handler == h2) f2 = true;
            if (handler == h3) f3 = true;
        }
        assertFalse("Handler 1 wasn't removed", f1);
        assertFalse("Handler 2 wasn't removed", f2);
        assertFalse("Handler 3 wasn't removed", f3);
    }

    @Test
    public void testHandlerRun() {
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("testHandlerRun");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertTrue("Handler wasn't run", ran.get());
    }

    @Test
    public void testResourceBundle() {
        final ListHandler handler = new ListHandler();
        final Logger logger = Logger.getLogger("rbLogger", getClass().getName());
        logger.setLevel(Level.INFO);
        handler.setLevel(Level.INFO);
        logger.addHandler(handler);
        logger.log(Level.INFO, null, new IllegalArgumentException());
        logger.log(Level.INFO, "test", new IllegalArgumentException());
        assertEquals(null, handler.messages.get(0));
        assertEquals("Test message", handler.messages.get(1));
    }

    @Test
    public void testInheritedFilter() {
        final ListHandler handler = new ListHandler();
        final Logger parent = Logger.getLogger("parent", getClass().getName());
        parent.setLevel(Level.INFO);
        handler.setLevel(Level.INFO);
        parent.addHandler(handler);
        parent.setFilter(new RegexFilter(".*(?i)test.*"));

        final Logger child = Logger.getLogger("parent.child", getClass().getName());
        child.setUseParentFilters(true);
        child.setLevel(Level.INFO);

        child.info("This is a test message");
        child.info("This is another test message");
        child.info("One more message");

        assertEquals("Handler should have only contained two messages", 2, handler.messages.size());

        // Clear the handler, reset the inherit filters
        handler.messages.clear();
        child.setUseParentFilters(false);

        child.info("This is a test message");
        child.info("This is another test message");
        child.info("One more message");

        assertEquals("Handler should have only contained three messages", 3, handler.messages.size());

        parent.info("This is a test message");
        parent.info("This is another test message");
        parent.info("One more message");

        assertEquals("Handler should have only contained five messages", 5, handler.messages.size());
    }

    private static final class ListHandler extends ExtHandler {
        final List<String> messages = Collections.synchronizedList(new ArrayList<String>());

        ListHandler() {
            super();
            setFormatter(new PatternFormatter("%s"));
        }

        @Override
        protected void doPublish(final ExtLogRecord record) {
            super.doPublish(record);
            messages.add(record.getFormattedMessage());
        }
    }

    private static final class CheckingHandler extends Handler {
        private final AtomicBoolean ran;

        public CheckingHandler(final AtomicBoolean ran) {
            this.ran = ran;
        }

        public void publish(final LogRecord record) {
            if (isLoggable(record)) {
                ran.set(true);
            }
        }

        public void flush() {
        }

        public void close() throws SecurityException {
        }
    }

    private static final class NullHandler extends Handler {

        public void publish(final LogRecord record) {
        }

        public void flush() {
        }

        public void close() throws SecurityException {
        }
    }
}
