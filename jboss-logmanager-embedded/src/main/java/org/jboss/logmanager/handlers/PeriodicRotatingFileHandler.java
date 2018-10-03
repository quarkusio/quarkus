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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.ErrorManager;

import org.jboss.logmanager.ExtLogRecord;

/**
 * A file handler which rotates the log at a preset time interval.  The interval is determined by the content of the
 * suffix string which is passed in to {@link #setSuffix(String)}.
 */
public class PeriodicRotatingFileHandler extends FileHandler {

    private SimpleDateFormat format;
    private String nextSuffix;
    private Period period = Period.NEVER;
    private long nextRollover = Long.MAX_VALUE;
    private TimeZone timeZone = TimeZone.getDefault();
    private SuffixRotator suffixRotator = SuffixRotator.EMPTY;

    /**
     * Construct a new instance with no formatter and no output file.
     */
    public PeriodicRotatingFileHandler() {
    }

    /**
     * Construct a new instance with the given output file.
     *
     * @param fileName the file name
     *
     * @throws java.io.FileNotFoundException if the file could not be found on open
     */
    public PeriodicRotatingFileHandler(final String fileName) throws FileNotFoundException {
        super(fileName);
    }

    /**
     * Construct a new instance with the given output file and append setting.
     *
     * @param fileName the file name
     * @param append {@code true} to append, {@code false} to overwrite
     *
     * @throws java.io.FileNotFoundException if the file could not be found on open
     */
    public PeriodicRotatingFileHandler(final String fileName, final boolean append) throws FileNotFoundException {
        super(fileName, append);
    }

    /**
     * Construct a new instance with the given output file.
     *
     * @param file the file
     * @param suffix the format suffix to use
     *
     * @throws java.io.FileNotFoundException if the file could not be found on open
     */
    public PeriodicRotatingFileHandler(final File file, final String suffix) throws FileNotFoundException {
        super(file);
        setSuffix(suffix);
    }

    /**
     * Construct a new instance with the given output file and append setting.
     *
     * @param file the file
     * @param suffix the format suffix to use
     * @param append {@code true} to append, {@code false} to overwrite
     * @throws java.io.FileNotFoundException if the file could not be found on open
     */
    public PeriodicRotatingFileHandler(final File file, final String suffix, final boolean append) throws FileNotFoundException {
        super(file, append);
        setSuffix(suffix);
    }

    @Override
    public void setFile(final File file) throws FileNotFoundException {
        synchronized (outputLock) {
            super.setFile(file);
            if (format != null && file != null && file.lastModified() > 0) {
                calcNextRollover(file.lastModified());
            }
        }
    }

    /** {@inheritDoc}  This implementation checks to see if the scheduled rollover time has yet occurred. */
    protected void preWrite(final ExtLogRecord record) {
        final long recordMillis = record.getMillis();
        if (recordMillis >= nextRollover) {
            rollOver();
            calcNextRollover(recordMillis);
        }
    }

    /**
     * Set the suffix string.  The string is in a format which can be understood by {@link java.text.SimpleDateFormat}.
     * The period of the rotation is automatically calculated based on the suffix.
     * <p>
     * If the suffix ends with {@code .gz} or {@code .zip} the file will be compressed on rotation.
     * </p>
     *
     * @param suffix the suffix
     * @throws IllegalArgumentException if the suffix is not valid
     */
    public void setSuffix(String suffix) throws IllegalArgumentException {
        final SuffixRotator suffixRotator = SuffixRotator.parse(suffix);
        final String dateSuffix = suffixRotator.getDatePattern();
        final SimpleDateFormat format = new SimpleDateFormat(dateSuffix);
        format.setTimeZone(timeZone);
        final int len = dateSuffix.length();
        Period period = Period.NEVER;
        for (int i = 0; i < len; i ++) {
            switch (dateSuffix.charAt(i)) {
                case 'y': period = min(period, Period.YEAR); break;
                case 'M': period = min(period, Period.MONTH); break;
                case 'w':
                case 'W': period = min(period, Period.WEEK); break;
                case 'D':
                case 'd':
                case 'F':
                case 'E': period = min(period, Period.DAY); break;
                case 'a': period = min(period, Period.HALF_DAY); break;
                case 'H':
                case 'k':
                case 'K':
                case 'h': period = min(period, Period.HOUR); break;
                case 'm': period = min(period, Period.MINUTE); break;
                case '\'': while (dateSuffix.charAt(++i) != '\''); break;
                case 's':
                case 'S': throw new IllegalArgumentException("Rotating by second or millisecond is not supported");
            }
        }
        synchronized (outputLock) {
            this.format = format;
            this.period = period;
            this.suffixRotator = suffixRotator;
            final long now;
            final File file = getFile();
            if (file != null && file.lastModified() > 0) {
                now = file.lastModified();
            } else {
                now = System.currentTimeMillis();
            }
            calcNextRollover(now);
        }
    }

    /**
     * Returns the suffix to be used.
     *
     * @return the suffix to be used
     */
    protected final String getNextSuffix() {
        return nextSuffix;
    }

    /**
     * Returns the file rotator for this handler.
     *
     * @return the file rotator
     */
    SuffixRotator getSuffixRotator() {
        return suffixRotator;
    }

    private void rollOver() {
        try {
            final File file = getFile();
            // first, close the original file (some OSes won't let you move/rename a file that is open)
            setFile(null);
            // next, rotate it
            suffixRotator.rotate(getErrorManager(), file.toPath(), nextSuffix);
            // start new file
            setFile(file);
        } catch (IOException e) {
            reportError("Unable to rotate log file", e, ErrorManager.OPEN_FAILURE);
        }
    }

    private void calcNextRollover(final long fromTime) {
        if (period == Period.NEVER) {
            nextRollover = Long.MAX_VALUE;
            return;
        }
        nextSuffix = format.format(new Date(fromTime));
        final Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(fromTime);
        final Period period = this.period;
        // clear out less-significant fields
        switch (period) {
            default:
            case YEAR:
                calendar.set(Calendar.MONTH, 0);
            case MONTH:
                // Needs to be set to the first day of the month
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.clear(Calendar.WEEK_OF_MONTH);
            case WEEK:
                if (period == Period.WEEK) {
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                } else {
                    calendar.clear(Calendar.DAY_OF_WEEK);
                }
                calendar.clear(Calendar.DAY_OF_WEEK_IN_MONTH);
            case DAY:
                calendar.set(Calendar.HOUR_OF_DAY, 0);
            case HALF_DAY:
                if (period == Period.HALF_DAY) {
                    calendar.set(Calendar.HOUR, 0);
                } else {
                    //We want both HOUR_OF_DAY and (HOUR + AM_PM) to be zeroed out
                    //This should ensure the hour is truly zeroed out
                    calendar.set(Calendar.HOUR, 0);
                    calendar.set(Calendar.AM_PM, 0);
                }
            case HOUR:
                calendar.set(Calendar.MINUTE, 0);
            case MINUTE:
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
        }
        // increment the relevant field
        switch (period) {
            case YEAR:
                calendar.add(Calendar.YEAR, 1);
                break;
            case MONTH:
                calendar.add(Calendar.MONTH, 1);
                break;
            case WEEK:
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case DAY:
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case HALF_DAY:
                calendar.add(Calendar.AM_PM, 1);
                break;
            case HOUR:
                calendar.add(Calendar.HOUR_OF_DAY, 1);
                break;
            case MINUTE:
                calendar.add(Calendar.MINUTE, 1);
                break;
        }
        nextRollover = calendar.getTimeInMillis();
    }

    /**
     * Get the configured time zone for this handler.
     *
     * @return the configured time zone
     */
    public TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * Set the configured time zone for this handler.
     *
     * @param timeZone the configured time zone
     */
    public void setTimeZone(final TimeZone timeZone) {
        if (timeZone == null) {
            throw new NullPointerException("timeZone is null");
        }
        this.timeZone = timeZone;
    }

    private static <T extends Comparable<? super T>> T min(T a, T b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    /**
     * Possible period values.  Keep in strictly ascending order of magnitude.
     */
    public enum Period {
        MINUTE,
        HOUR,
        HALF_DAY,
        DAY,
        WEEK,
        MONTH,
        YEAR,
        NEVER,
    }
}
