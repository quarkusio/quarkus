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

import org.jboss.logmanager.ExtLogRecord.FormatStyle;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import org.jboss.logmanager.filters.AcceptAllFilter;
import org.jboss.logmanager.filters.DenyAllFilter;
import org.jboss.logmanager.filters.AllFilter;
import org.jboss.logmanager.filters.AnyFilter;
import org.jboss.logmanager.filters.InvertFilter;
import org.jboss.logmanager.filters.LevelFilter;
import org.jboss.logmanager.filters.LevelRangeFilter;
import org.jboss.logmanager.filters.RegexFilter;
import org.jboss.logmanager.filters.SubstituteFilter;

import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public final class FilterTests {

    private static final Filter[] NO_FILTERS = new Filter[0];

    @Test
    public void testAcceptAllFilter() {
        final Filter filter = AcceptAllFilter.getInstance();
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertTrue("Handler wasn't run", ran.get());
    }

    @Test
    public void testDenyAllFilter() {
        final Filter filter = DenyAllFilter.getInstance();
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertFalse("Handler was run", ran.get());
    }

    @Test
    public void testAllFilter0() {
        final Filter filter = new AllFilter(NO_FILTERS);
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertTrue("Handler wasn't run", ran.get());
    }

    @Test
    public void testAllFilter1() {
        final Filter filter = new AllFilter(new Filter[] {
                AcceptAllFilter.getInstance(),
                AcceptAllFilter.getInstance(),
                AcceptAllFilter.getInstance(),
        });
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertTrue("Handler wasn't run", ran.get());
    }

    @Test
    public void testAllFilter2() {
        final Filter filter = new AllFilter(new Filter[] {
                AcceptAllFilter.getInstance(),
                DenyAllFilter.getInstance(),
                AcceptAllFilter.getInstance(),
        });
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertFalse("Handler was run", ran.get());
    }

    @Test
    public void testAllFilter3() {
        final Filter filter = new AllFilter(new Filter[] {
                DenyAllFilter.getInstance(),
                DenyAllFilter.getInstance(),
                DenyAllFilter.getInstance(),
        });
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertFalse("Handler was run", ran.get());
    }

    @Test
    public void testAnyFilter0() {
        final Filter filter = new AnyFilter(NO_FILTERS);
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertFalse("Handler was run", ran.get());
    }

    @Test
    public void testAnyFilter1() {
        final Filter filter = new AnyFilter(new Filter[] {
                AcceptAllFilter.getInstance(),
                AcceptAllFilter.getInstance(),
                AcceptAllFilter.getInstance(),
        });
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertTrue("Handler wasn't run", ran.get());
    }

    @Test
    public void testAnyFilter2() {
        final Filter filter = new AnyFilter(new Filter[] {
                AcceptAllFilter.getInstance(),
                DenyAllFilter.getInstance(),
                AcceptAllFilter.getInstance(),
        });
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertTrue("Handler wasn't run", ran.get());
    }

    @Test
    public void testAnyFilter3() {
        final Filter filter = new AnyFilter(new Filter[] {
                DenyAllFilter.getInstance(),
                DenyAllFilter.getInstance(),
                DenyAllFilter.getInstance(),
        });
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertFalse("Handler was run", ran.get());
    }

    @Test
    public void testInvertFilter0() {
        final Filter filter = new InvertFilter(AcceptAllFilter.getInstance());
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertFalse("Handler was run", ran.get());
    }

    @Test
    public void testInvertFilter1() {
        final Filter filter = new InvertFilter(DenyAllFilter.getInstance());
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertTrue("Handler wasn't run", ran.get());
    }

    @Test
    public void testLevelFilter0() {
        final Filter filter = new LevelFilter(Level.INFO);
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertTrue("Handler wasn't run", ran.get());
    }

    @Test
    public void testLevelFilter1() {
        final Filter filter = new LevelFilter(Level.WARNING);
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertFalse("Handler was run", ran.get());
    }

    @Test
    public void testLevelRangeFilter0() {
        final Filter filter = new LevelRangeFilter(Level.DEBUG, true, Level.WARN, true);
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertTrue("Handler wasn't run", ran.get());
    }

    @Test
    public void testLevelRangeFilter1() {
        final Filter filter = new LevelRangeFilter(Level.DEBUG, true, Level.WARN, true);
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.severe("This is a test.");
        assertFalse("Handler was run", ran.get());
    }

    @Test
    public void name() {
    }

    @Test
    public void testLevelRangeFilter3() {
        final Filter filter = new LevelRangeFilter(Level.DEBUG, false, Level.WARN, true);
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.DEBUG);
        logger.setFilter(filter);
        handler.setLevel(Level.DEBUG);
        logger.log(Level.DEBUG, "This is a test.");
        assertFalse("Handler was run", ran.get());
    }

    @Test
    public void testLevelRangeFilter4() {
        final Filter filter = new LevelRangeFilter(Level.DEBUG, true, Level.WARN, false);
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.warning("This is a test.");
        assertFalse("Handler was run", ran.get());
    }

    @Test
    public void testRegexFilter0() {
        final Filter filter = new RegexFilter("test");
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertTrue("Handler wasn't run", ran.get());
    }

    @Test
    public void testRegexFilter1() {
        final Filter filter = new RegexFilter("pest");
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test.");
        assertFalse("Handler was run", ran.get());
    }

    @Test
    @Ignore("This test is testing essentially invalid/coincidental behavior")
    public void testRegexFilter2() {
        final Filter filter = new RegexFilter("pest");
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        final ExtLogRecord record = new ExtLogRecord(Level.INFO, "This is a test %s", FormatStyle.PRINTF, "filterTest");
        record.setParameters(new String[] {"pest"});
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.log(record);
        assertTrue("Handler wasn't run", ran.get());
    }

    @Test
    public void testRegexFilter3() {
        final Filter filter = new RegexFilter("pest");
        final AtomicBoolean ran = new AtomicBoolean();
        final Handler handler = new CheckingHandler(ran);
        final Logger logger = Logger.getLogger("filterTest");
        final ExtLogRecord record = new ExtLogRecord(Level.INFO, "This is a test %s", FormatStyle.PRINTF, "filterTest");
        record.setParameters(new String[] {"test"});
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.log(record);
        assertFalse("Handler was run", ran.get());
    }

    @Test
    public void regexFilterExceptionNullMessageTest(){
        final ExtLogRecord logRecord = new ExtLogRecord(Level.ALL, null, null);
        final Filter filter = new RegexFilter("test");
        boolean isLoggable = filter.isLoggable(logRecord);
        assertFalse(isLoggable);
        assertNull(logRecord.getFormattedMessage());
    }

    @Test
    public void testSubstitueFilter0() {
        final Filter filter = new SubstituteFilter(Pattern.compile("test"), "lunch", true);
        final AtomicReference<String> result = new AtomicReference<String>();
        final Handler handler = new MessageCheckingHandler(result);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test test.");
        assertEquals("Substitution was not correctly applied", "This is a lunch lunch.", result.get());
    }

    @Test
    public void testSubstituteFilter1() {
        final Filter filter = new SubstituteFilter(Pattern.compile("test"), "lunch", false);
        final AtomicReference<String> result = new AtomicReference<String>();
        final Handler handler = new MessageCheckingHandler(result);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test test.");
        assertEquals("Substitution was not correctly applied", "This is a lunch test.", result.get());
    }

    @Test
    public void testSubstituteFilter2() {
        final Filter filter = new SubstituteFilter(Pattern.compile("t(es)t"), "lunch$1", true);
        final AtomicReference<String> result = new AtomicReference<String>();
        final Handler handler = new MessageCheckingHandler(result);
        final Logger logger = Logger.getLogger("filterTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setFilter(filter);
        handler.setLevel(Level.INFO);
        logger.info("This is a test test.");
        assertEquals("Substitution was not correctly applied", "This is a lunches lunches.", result.get());
    }

    @Test
    public void testSubstituteFilter3() {
        final Filter filter = new SubstituteFilter(Pattern.compile("t(es)t"), "lunch$1", true);
        final ExtLogRecord record = new ExtLogRecord(Level.INFO, "This is a test %s", FormatStyle.PRINTF, FilterTests.class.getName());
        record.setParameters(new String[] {"test"});
        filter.isLoggable(record);
        assertEquals("Substitution was not correctly applied", "This is a lunches lunches", record.getFormattedMessage());
    }

    @Test
    public void substituteFilterExceptionNullMessageTest(){
        final ExtLogRecord logRecord = new ExtLogRecord(Level.ALL, null, null);
        final Filter filter = new SubstituteFilter(Pattern.compile("test"), "lunch", true);
        filter.isLoggable(logRecord);
        assertEquals("null", logRecord.getFormattedMessage());
    }



    private static final class MessageCheckingHandler extends Handler {
        private final AtomicReference<String> msg;

        private MessageCheckingHandler(final AtomicReference<String> msg) {
            this.msg = msg;
        }

        public void publish(final LogRecord record) {
            msg.set(record.getMessage());
        }

        public void flush() {
        }

        public void close() throws SecurityException {
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
}
