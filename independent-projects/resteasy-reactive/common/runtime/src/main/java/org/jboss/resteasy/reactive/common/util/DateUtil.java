package org.jboss.resteasy.reactive.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A utility class for parsing and formatting HTTP dates as used in cookies and
 * other headers. This class handles dates as defined by RFC 2616 section
 * 3.3.1 as well as some other common non-standard formats.
 *
 * @author Christopher Brown
 * @author Michael Becke
 */
public class DateUtil {

    public static class DateParseException extends RuntimeException {
        public DateParseException() {
        }

        public DateParseException(final String s) {
            super(s);
        }

        public DateParseException(final String s, final Throwable throwable) {
            super(s, throwable);
        }

        public DateParseException(final Throwable throwable) {
            super(throwable);
        }
    }

    /**
     * Date format pattern used to parse HTTP date headers in RFC 1123 format.
     */
    public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * Date format pattern used to parse HTTP date headers in RFC 1036 format.
     */
    public static final String PATTERN_RFC1036 = "EEEE, dd-MMM-yy HH:mm:ss zzz";

    /**
     * Date format pattern used to parse HTTP date headers in ANSI C
     * <code>asctime()</code> format.
     */
    public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";

    private static final Collection DEFAULT_PATTERNS = Arrays.asList(
            new String[] { PATTERN_ASCTIME, PATTERN_RFC1036, PATTERN_RFC1123 });

    private static final Date DEFAULT_TWO_DIGIT_YEAR_START;

    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2000, Calendar.JANUARY, 1, 0, 0);
        DEFAULT_TWO_DIGIT_YEAR_START = calendar.getTime();
    }

    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    /**
     * Parses a date value. The formats used for parsing the date value are retrieved from
     * the default http params.
     *
     * @param dateValue the date value to parse
     * @return the parsed date
     * @throws DateParseException if the value could not be parsed using any of the
     *         supported date formats
     */
    public static Date parseDate(String dateValue) throws DateParseException {
        return parseDate(dateValue, null, null);
    }

    /**
     * Parses the date value using the given date formats.
     *
     * @param dateValue the date value to parse
     * @param dateFormats the date formats to use
     * @return the parsed date
     * @throws DateParseException if none of the dataFormats could parse the dateValue
     */
    public static Date parseDate(String dateValue, Collection dateFormats)
            throws DateParseException {
        return parseDate(dateValue, dateFormats, null);
    }

    /**
     * Parses the date value using the given date formats.
     *
     * @param dateValue the date value to parse
     * @param dateFormats the date formats to use
     * @param startDate During parsing, two digit years will be placed in the range
     *        <code>startDate</code> to <code>startDate + 100 years</code>. This value may
     *        be <code>null</code>. When <code>null</code> is given as a parameter, year
     *        <code>2000</code> will be used.
     * @return the parsed date
     * @throws DateParseException if none of the dataFormats could parse the dateValue
     */
    public static Date parseDate(
            String dateValue,
            Collection dateFormats,
            Date startDate) throws DateParseException {

        if (dateValue == null) {
            throw new IllegalArgumentException("Date was null");
        }
        if (dateFormats == null) {
            dateFormats = DEFAULT_PATTERNS;
        }
        if (startDate == null) {
            startDate = DEFAULT_TWO_DIGIT_YEAR_START;
        }
        // trim single quotes around date if present
        // see issue #5279
        if (dateValue.length() > 1
                && dateValue.startsWith("'")
                && dateValue.endsWith("'")) {
            dateValue = dateValue.substring(1, dateValue.length() - 1);
        }

        SimpleDateFormat dateParser = null;
        Iterator formatIter = dateFormats.iterator();

        while (formatIter.hasNext()) {
            String format = (String) formatIter.next();
            if (dateParser == null) {
                dateParser = new SimpleDateFormat(format, Locale.US);
                dateParser.setTimeZone(TimeZone.getTimeZone("GMT"));
                dateParser.set2DigitYearStart(startDate);
            } else {
                dateParser.applyPattern(format);
            }
            try {
                return dateParser.parse(dateValue);
            } catch (ParseException pe) {
                // ignore this exception, we will try the next format
            }
        }

        // we were unable to parse the date
        throw new DateParseException("Unable to parse " + dateValue);
    }

    /**
     * Formats the given date according to the RFC 1123 pattern.
     *
     * @param date The date to format.
     * @return An RFC 1123 formatted date string.
     * @see #PATTERN_RFC1123
     */
    public static String formatDate(Date date) {
        return formatDate(date, PATTERN_RFC1123);
    }

    /**
     * Formats the given date according to the specified pattern. The pattern
     * must conform to that used by the {@link SimpleDateFormat simple date
     * format} class.
     *
     * @param date The date to format.
     * @param pattern The pattern to use for formatting the date.
     * @return A formatted date string.
     * @throws IllegalArgumentException If the given date pattern is invalid.
     * @see SimpleDateFormat
     */
    public static String formatDate(Date date, String pattern) {
        if (date == null)
            throw new IllegalArgumentException("Date was null");
        if (pattern == null)
            throw new IllegalArgumentException("Pattern was null");

        SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.US);
        formatter.setTimeZone(GMT);
        return formatter.format(date);
    }

    /**
     * This class should not be instantiated.
     */
    private DateUtil() {
    }

}
