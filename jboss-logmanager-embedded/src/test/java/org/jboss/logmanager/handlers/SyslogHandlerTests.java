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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.logging.Level;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.SyslogHandler.SyslogType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class SyslogHandlerTests {
    private static final String ENCODING = "UTF-8";
    private static final String BOM = Character.toString((char) 0xFEFF);
    private static final String MSG = "This is a test message";
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 10999;

    private SyslogHandler handler;

    @Before
    public void setupHandler() throws Exception {
        handler = new SyslogHandler(HOSTNAME, PORT);
        handler.setFormatter(new PatternFormatter("%s"));
    }

    @After
    public void closeHandler() throws Exception {
        // Close the handler
        handler.flush();
        handler.close();
    }

    @Test
    public void testRFC5424Tcp() throws Exception {
        // Setup the handler
        handler.setSyslogType(SyslogType.RFC5424);
        handler.setMessageDelimiter("\n");
        handler.setUseMessageDelimiter(true);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        handler.setOutputStream(out);

        final Calendar cal = getCalendar();
        // Create the record
        handler.setHostname("test");
        ExtLogRecord record = createRecord(cal, MSG);
        String expectedMessage = "<14>1 2012-01-09T04:39:22.000" + calculateTimeZone(cal) + " test java " + handler.getPid() + " - - " + BOM + MSG + '\n';
        handler.publish(record);
        Assert.assertEquals(expectedMessage, createString(out));

        // Create the record
        out.reset();
        record = createRecord(cal, MSG);
        expectedMessage = "<14>1 2012-01-09T04:39:22.000" + calculateTimeZone(cal) + " test java " + handler.getPid() + " - - " + BOM + MSG + '\n';
        handler.publish(record);
        Assert.assertEquals(expectedMessage, createString(out));

        out.reset();
        cal.set(Calendar.DAY_OF_MONTH, 31);
        record = createRecord(cal, MSG);
        handler.setHostname("test");
        handler.setAppName("java");
        expectedMessage = "<14>1 2012-01-31T04:39:22.000" + calculateTimeZone(cal) + " test java " + handler.getPid() + " - - " + BOM + MSG + '\n';
        handler.publish(record);
        Assert.assertEquals(expectedMessage, createString(out));
    }

    @Test
    public void testRFC31644Format() throws Exception {
        // Setup the handler
        handler.setSyslogType(SyslogType.RFC3164);
        handler.setHostname("test");
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        handler.setOutputStream(out);

        final Calendar cal = getCalendar();

        // Create the record
        ExtLogRecord record = createRecord(cal, MSG);
        handler.publish(record);
        String expectedMessage = "<14>Jan  9 04:39:22 test java[" + handler.getPid() + "]: " + MSG;
        Assert.assertEquals(expectedMessage, createString(out));

        out.reset();
        cal.set(Calendar.DAY_OF_MONTH, 31);
        record = createRecord(cal, MSG);
        handler.publish(record);
        expectedMessage = "<14>Jan 31 04:39:22 test java[" + handler.getPid() + "]: " + MSG;
        Assert.assertEquals(expectedMessage, createString(out));
    }

    @Test
    public void testOctetCounting() throws Exception {
        // Setup the handler
        handler.setSyslogType(SyslogType.RFC5424);
        handler.setUseMessageDelimiter(false);
        handler.setUseCountingFraming(true);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        handler.setOutputStream(out);

        final Calendar cal = getCalendar();
        // Create the record
        handler.setHostname("test");
        ExtLogRecord record = createRecord(cal, MSG);
        String expectedMessage = "<14>1 2012-01-09T04:39:22.000" + calculateTimeZone(cal) + " test java " + handler.getPid() + " - - " + BOM + MSG;
        expectedMessage = byteLen(expectedMessage)+ " " + expectedMessage;
        handler.publish(record);
        Assert.assertEquals(expectedMessage, createString(out));
    }

    @Test
    public void testTruncation() throws Exception {
        // Setup the handler
        handler.setSyslogType(SyslogType.RFC5424);
        handler.setUseMessageDelimiter(false);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        handler.setOutputStream(out);

        final Calendar cal = getCalendar();
        // Create the record
        handler.setHostname("test");
        final String part1 = "This is a longer message and should be truncated after this.";
        final String part2 = "Truncated portion of the message that will not be shown in.";
        final String message = part1 + " " + part2;

        final String header = "<14>1 2012-01-09T04:39:22.000" + calculateTimeZone(cal) + " test java " + handler.getPid() + " - - " + BOM;

        handler.setMaxLength(byteLen(header, part1));
        handler.setTruncate(true);

        ExtLogRecord record = createRecord(cal, message);
        String expectedMessage = header + part1;
        handler.publish(record);
        Assert.assertEquals(expectedMessage, createString(out));

        out.reset();
        // Wrap a message
        handler.setTruncate(false);
        handler.publish(record);
        // Extra space from message
        expectedMessage = header + part1 + header + " " + part2;
        Assert.assertEquals(expectedMessage, createString(out));
    }

    @Test
    public void testMultibyteTruncation() throws Exception {
        String part1 = "This is a longer message and should be truncated after this  À あ";
        String part2 = "あ À Truncated portion of the message that will not be shown in.";
        testMultibyteTruncation(part1, part2, 1);

        // Test some characters with surrogates
        part1 = "Truncate after double byte 𥹖";
        part2 = "second message 𥹖";
        testMultibyteTruncation(part1, part2, 2);

    }

    private void testMultibyteTruncation(final String part1, final String part2, final int charsToTruncate) throws Exception {
        // Setup the handler
        handler.setSyslogType(SyslogType.RFC5424);
        handler.setUseMessageDelimiter(false);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        handler.setOutputStream(out);

        final Calendar cal = getCalendar();
        // Create the record
        handler.setHostname("test");
        String message = part1 + " " + part2;

        final String header = "<14>1 2012-01-09T04:39:22.000" + calculateTimeZone(cal) + " test java " + handler.getPid() + " - - " + BOM;

        handler.setMaxLength(byteLen(header, part1));
        handler.setTruncate(true);

        ExtLogRecord record = createRecord(cal, message);
        String expectedMessage = header + part1;
        handler.publish(record);
        Assert.assertTrue(String.format("Expected: %s:%n Received: %s", expectedMessage, createString(out)), Arrays.equals(expectedMessage.getBytes(ENCODING), out.toByteArray()));

        out.reset();
        // Wrap a message
        handler.setTruncate(false);
        handler.publish(record);
        // Extra space from message
        expectedMessage = header + part1 + header + " " + part2;
        Assert.assertTrue(String.format("Expected: %s:%n Received: %s", expectedMessage, createString(out)), Arrays.equals(expectedMessage.getBytes(ENCODING), out.toByteArray()));

        // Reset out, write the message with a maximum length of the current length minus 1 to ensure the multi-byte character was not written at the end
        out.reset();
        handler.setTruncate(true);
        handler.setMaxLength(byteLen(header, part1) - 1);
        expectedMessage = header + part1.substring(0, part1.length() - charsToTruncate);
        handler.publish(record);
        Assert.assertTrue(String.format("Expected: %s:%n Received: %s", expectedMessage, createString(out)), Arrays.equals(expectedMessage.getBytes(ENCODING), out.toByteArray()));
    }

    private static ExtLogRecord createRecord(final Calendar cal, final String message) {
        final String loggerName = SyslogHandlerTests.class.getName();
        final ExtLogRecord record = new ExtLogRecord(Level.INFO, message, loggerName);
        record.setMillis(cal.getTimeInMillis());
        return record;
    }

    private static Calendar getCalendar() {
        final Calendar cal = Calendar.getInstance();
        cal.set(2012, Calendar.JANUARY, 9, 4, 39, 22);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private static String calculateTimeZone(final Calendar cal) {
        final int tz = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
        final StringBuilder buffer = new StringBuilder();
        if (tz == 0) {
            buffer.append("+00:00");
        } else {
            int tzMinutes = tz / 60000; // milliseconds to minutes
            if (tzMinutes < 0) {
                tzMinutes = -tzMinutes;
                buffer.append('-');
            } else {
                buffer.append('+');
            }
            final int tzHour = tzMinutes / 60; // minutes to hours
            tzMinutes -= tzHour * 60; // subtract hours from minutes in minutes
            if (tzHour < 10) {
                buffer.append(0);
            }
            buffer.append(tzHour).append(':');
            if (tzMinutes < 10) {
                buffer.append(0);
            }
            buffer.append(tzMinutes);
        }
        return buffer.toString();
    }

    private static String createString(final ByteArrayOutputStream out) throws UnsupportedEncodingException {
        return out.toString(ENCODING);
    }

    private static int byteLen(final String s) throws UnsupportedEncodingException {
        return s.getBytes(ENCODING).length;
    }

    private static int byteLen(final String s1, final String s2) throws UnsupportedEncodingException {
        return s1.getBytes(ENCODING).length + s2.getBytes(ENCODING).length;
    }
}
