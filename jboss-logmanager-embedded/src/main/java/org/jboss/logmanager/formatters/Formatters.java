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

package org.jboss.logmanager.formatters;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.security.AccessController.doPrivileged;

import java.io.PrintWriter;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

/**
 * Formatter utility methods.
 */
public final class Formatters {

    public static final String THREAD_ID = "id";

    private static final boolean DEFAULT_TRUNCATE_BEGINNING = false;
    private static final String NEW_LINE = String.format("%n");
    private static final Pattern PRECISION_INT_PATTERN = Pattern.compile("\\d+");


    private Formatters() {
    }

    private static final Formatter NULL_FORMATTER = new Formatter() {
        public String format(final LogRecord record) {
            return "";
        }
    };

    /**
     * Get the null formatter, which outputs nothing.
     *
     * @return the null formatter
     */
    public static Formatter nullFormatter() {
        return NULL_FORMATTER;
    }

    /**
     * Create a format step which simply emits the given string.
     *
     * @param string the string to emit
     * @return a format step
     */
    public static FormatStep textFormatStep(final String string) {
        return new FormatStep() {
            public void render(final StringBuilder builder, final ExtLogRecord record) {
                builder.append(string);
            }

            public int estimateLength() {
                return string.length();
            }
        };
    }

    /**
     * Apply up to {@code count} trailing segments of the given string to the given {@code builder}.
     *
     * @param count the maximum number of segments to include
     * @param subject the subject string
     * @return the substring
     */
    private static String applySegments(final int count, final String subject) {
        if (count == 0) {
            return subject;
        }
        int idx = subject.length() + 1;
        for (int i = 0; i < count; i ++) {
            idx = subject.lastIndexOf('.', idx - 1);
            if (idx == -1) {
                return subject;
            }
        }
        return subject.substring(idx + 1);
    }

    /**
     * Apply up to {@code precision} trailing segments of the given string to the given {@code builder}. If the
     * precision contains non-integer values
     *
     * @param precision the precision used to
     * @param subject   the subject string
     *
     * @return the substring
     */
    private static String applySegments(final String precision, final String subject) {
        if (precision == null || subject == null) {
            return subject;
        }

        // Check for dots
        if (PRECISION_INT_PATTERN.matcher(precision).matches()) {
            return applySegments(Integer.parseInt(precision), subject);
        }
        // %c{1.} would be o.j.l.f.FormatStringParser
        // %c{1.~} would be o.~.~.~.FormatStringParser
        // %c{.} ....FormatStringParser
        final Map<Integer, Segment> segments = parsePatternSegments(precision);
        final Deque<String> categorySegments = parseCategorySegments(subject);
        final StringBuilder result = new StringBuilder();
        Segment segment = null;
        int index = 0;
        while (true) {
            index++;
            if (segments.containsKey(index)) {
                segment = segments.get(index);
            }
            final String s = categorySegments.poll();
            // Always print the last part of the category segments
            if (categorySegments.peek() == null) {
                result.append(s);
                break;
            }
            if (segment == null) {
                result.append(s).append('.');
            } else {
                if (segment.len > 0) {
                    if (segment.len > s.length()) {
                        result.append(s);
                    } else {
                        result.append(s.substring(0, segment.len));
                    }
                }
                if (segment.text != null) {
                    result.append(segment.text);
                }
                result.append('.');
            }
        }
        return result.toString();
    }

    private abstract static class JustifyingFormatStep implements FormatStep {
        private final boolean leftJustify;
        private final boolean truncateBeginning;
        private final int minimumWidth;
        private final int maximumWidth;

        protected JustifyingFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
            if (maximumWidth != 0 && minimumWidth > maximumWidth) {
                throw new IllegalArgumentException("Specified minimum width may not be greater than the specified maximum width");
            }
            if (maximumWidth < 0 || minimumWidth < 0) {
                throw new IllegalArgumentException("Minimum and maximum widths must not be less than zero");
            }
            this.leftJustify = leftJustify;
            this.truncateBeginning = truncateBeginning;
            this.minimumWidth = minimumWidth;
            this.maximumWidth = maximumWidth == 0 ? Integer.MAX_VALUE : maximumWidth;
        }

        public void render(final StringBuilder builder, final ExtLogRecord record) {
            render(null, builder, record);
        }

        public void render(Formatter formatter, StringBuilder builder, ExtLogRecord record) {
            final int minimumWidth = this.minimumWidth;
            final int maximumWidth = this.maximumWidth;
            final boolean leftJustify = this.leftJustify;
            if (leftJustify) {
                // no copy necessary for left justification
                final int oldLen = builder.length();
                renderRaw(formatter, builder, record);
                final int newLen = builder.length();
                // if we exceeded the max width, chop it off
                final int writtenLen = newLen - oldLen;
                final int overflow = writtenLen - maximumWidth;
                if (overflow > 0) {
                    if (truncateBeginning) {
                        builder.delete(oldLen, oldLen + overflow);
                    }
                    builder.setLength(newLen - overflow);
                } else {
                    final int spaces = minimumWidth - writtenLen;
                    for (int i = 0; i < spaces; i ++) {
                        builder.append(' ');
                    }
                }
            } else {
                // only copy the data if we're right justified
                final StringBuilder subBuilder = new StringBuilder();
                renderRaw(formatter, subBuilder, record);
                final int len = subBuilder.length();
                if (len > maximumWidth) {
                    if (truncateBeginning) {
                        final int overflow = len - maximumWidth;
                        subBuilder.delete(0, overflow);
                    }
                    subBuilder.setLength(maximumWidth);
                } else if (len < minimumWidth) {
                    // right justify
                    int spaces = minimumWidth - len;
                    for (int i = 0; i < spaces; i ++) {
                        builder.append(' ');
                    }
                }
                builder.append(subBuilder);
            }
        }

        public int estimateLength() {
            final int maximumWidth = this.maximumWidth;
            final int minimumWidth = this.minimumWidth;
            if (maximumWidth != 0) {
                return min(maximumWidth, minimumWidth * 3);
            } else {
                return max(32, minimumWidth);
            }
        }

        public abstract void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record);
    }

    private abstract static class SegmentedFormatStep extends JustifyingFormatStep {
        private final int count;
        private final String precision;

        protected SegmentedFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth, final int count) {
            super(leftJustify, minimumWidth, truncateBeginning, maximumWidth);
            this.count = count;
            precision = null;
        }

        protected SegmentedFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth, final String precision) {
            super(leftJustify, minimumWidth, truncateBeginning, maximumWidth);
            this.count = 0;
            this.precision = precision;
        }

        public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
            if (precision == null) {
                builder.append(applySegments(count, getSegmentedSubject(record)));
            } else {
                builder.append(applySegments(precision, getSegmentedSubject(record)));
            }
        }

        public abstract String getSegmentedSubject(final ExtLogRecord record);
    }

    /**
     * Create a format step which emits the logger name with the given justification rules.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @param precision    the argument used for the logger name, may be {@code null} or contain dots to format the logger name
     * @return the format
     */
    public static FormatStep loggerNameFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth, final String precision) {
        return loggerNameFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth, precision);
    }

    /**
     * Create a format step which emits the logger name with the given justification rules.
     *
     * @param leftJustify       {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth      the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth      the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @param precision         the argument used for the logger name, may be {@code null} or contain dots to format the
     *                          logger name
     *
     * @return the format
     */
    public static FormatStep loggerNameFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth, final String precision) {
        return new SegmentedFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth, precision) {
            public String getSegmentedSubject(final ExtLogRecord record) {
                return record.getLoggerName();
            }
        };
    }

    /**
     * Create a format step which emits the source class name with the given justification rules (NOTE: call stack
     * introspection introduces a significant performance penalty).
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @param precision    the argument used for the class name, may be {@code null} or contain dots to format the class name
     * @return the format step
     */
    public static FormatStep classNameFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth, final String precision) {
        return classNameFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth, precision);
    }

    /**
     * Create a format step which emits the source class name with the given justification rules (NOTE: call stack
     * introspection introduces a significant performance penalty).
     *
     * @param leftJustify       {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth      the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth      the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @param precision         the argument used for the class name, may be {@code null} or contain dots to format the
     *                          class name
     *
     * @return the format step
     */
    public static FormatStep classNameFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth, final String precision) {
        return new SegmentedFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth, precision) {
            public String getSegmentedSubject(final ExtLogRecord record) {
                return record.getSourceClassName();
            }

            @Override
            public boolean isCallerInformationRequired() {
                return true;
            }
        };
    }

    /**
     * Create a format step which emits the source module name with the given justification rules (NOTE: call stack
     * introspection introduces a significant performance penalty).
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @param precision    the argument used for the class name, may be {@code null} or contain dots to format the class name
     * @return the format step
     */
    public static FormatStep moduleNameFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth, final String precision) {
        return moduleNameFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth, precision);
    }

    /**
     * Create a format step which emits the source module name with the given justification rules (NOTE: call stack
     * introspection introduces a significant performance penalty).
     *
     * @param leftJustify       {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth      the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth      the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @param precision         the argument used for the class name, may be {@code null} or contain dots to format the
     *                          class name
     *
     * @return the format step
     */
    public static FormatStep moduleNameFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth, final String precision) {
        return new SegmentedFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth, precision) {
            public String getSegmentedSubject(final ExtLogRecord record) {
                return record.getSourceModuleName();
            }

            @Override
            public boolean isCallerInformationRequired() {
                return true;
            }
        };
    }

    /**
     * Create a format step which emits the source module version with the given justification rules (NOTE: call stack
     * introspection introduces a significant performance penalty).
     *
     * @param leftJustify       {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth      the minimum field width, or 0 for none
     * @param maximumWidth      the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @param precision         the argument used for the class name, may be {@code null} or contain dots to format the
     *                          class name
     *
     * @return the format step
     */
    public static FormatStep moduleVersionFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth, final String precision) {
        return new SegmentedFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth, precision) {
            public String getSegmentedSubject(final ExtLogRecord record) {
                return record.getSourceModuleVersion();
            }

            @Override
            public boolean isCallerInformationRequired() {
                return true;
            }
        };
    }

    /**
     * Create a format step which emits the date of the log record with the given justification rules.
     *
     * @param timeZone the time zone to format to
     * @param formatString the date format string
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep dateFormatStep(final TimeZone timeZone, final String formatString, final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return dateFormatStep(timeZone, formatString, leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth);
    }

    /**
     * Create a format step which emits the date of the log record with the given justification rules.
     *
     * @param timeZone          the time zone to format to
     * @param formatString      the date format string
     * @param leftJustify       {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth      the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth      the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     *
     * @return the format step
     */
    public static FormatStep dateFormatStep(final TimeZone timeZone, final String formatString, final boolean leftJustify, final int minimumWidth,
                                            final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            private final ThreadLocal<SimpleDateFormat> holder = new ThreadLocal<SimpleDateFormat>() {
                protected SimpleDateFormat initialValue() {
                    final SimpleDateFormat dateFormat = new SimpleDateFormat(formatString == null ? "yyyy-MM-dd HH:mm:ss,SSS" : formatString);
                    dateFormat.setTimeZone(timeZone);
                    return dateFormat;
                }
            };

            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                builder.append(holder.get().format(new Date(record.getMillis())));
            }
        };
    }

    /**
     * Create a format step which emits the date of the log record with the given justification rules.
     *
     * @param formatString the date format string
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep dateFormatStep(final String formatString, final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return dateFormatStep(TimeZone.getDefault(), formatString, leftJustify, minimumWidth, maximumWidth);
    }

    /**
     * Create a format step which emits the source file name with the given justification rules (NOTE: call stack
     * introspection introduces a significant performance penalty).
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep fileNameFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return fileNameFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth);
    }

    /**
     * Create a format step which emits the source file name with the given justification rules (NOTE: call stack
     * introspection introduces a significant performance penalty).
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep fileNameFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                builder.append(record.getSourceFileName());
            }

            @Override
            public boolean isCallerInformationRequired() {
                return true;
            }
        };
    }

    /**
     * Create a format step which emits the source process name with the given justification rules.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep processNameFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                builder.append(record.getProcessName());
            }
        };
    }

    /**
     * Create a format step which emits the source file line number with the given justification rules (NOTE: call stack
     * introspection introduces a significant performance penalty).
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep processIdFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                builder.append(record.getProcessId());
            }
        };
    }

    /**
     * Create a format step which emits the hostname.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @param qualified {@code true} to use the fully qualified host name, {@code false} to only use the
     * @return the format step
     */
    public static FormatStep hostnameFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth, final boolean qualified) {
        return new SegmentedFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth, null) {
            public String getSegmentedSubject(final ExtLogRecord record) {
                final String hostName = record.getHostName();
                final int idx = hostName.indexOf('.');
                return idx == -1 ? hostName :hostName.substring(0, idx);
            }
        };
    }

    /**
     * Create a format step which emits the hostname.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @param precision    the argument used for the class name, may be {@code null} or contain dots to format the class name
     * @return the format step
     */
    public static FormatStep hostnameFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth, final String precision) {
        return new SegmentedFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth, precision) {
            public String getSegmentedSubject(final ExtLogRecord record) {
                return record.getHostName();
            }
        };
    }

    /**
     * Create a format step which emits the complete source location information with the given justification rules
     * (NOTE: call stack introspection introduces a significant performance penalty).
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep locationInformationFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return locationInformationFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth);
    }

    /**
     * Create a format step which emits the complete source location information with the given justification rules
     * (NOTE: call stack introspection introduces a significant performance penalty).
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep locationInformationFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                final String fileName = record.getSourceFileName();
                final int lineNumber = record.getSourceLineNumber();
                final String className = record.getSourceClassName();
                final String methodName = record.getSourceMethodName();
                builder.append(className).append('.').append(methodName);
                builder.append('(').append(fileName);
                if (lineNumber != -1) {
                    builder.append(':').append(lineNumber);
                }
                builder.append(')');
            }

            @Override
            public boolean isCallerInformationRequired() {
                return true;
            }
        };
    }

    /**
     * Create a format step which emits the source file line number with the given justification rules (NOTE: call stack
     * introspection introduces a significant performance penalty).
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep lineNumberFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return lineNumberFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth);
    }

    /**
     * Create a format step which emits the source file line number with the given justification rules (NOTE: call stack
     * introspection introduces a significant performance penalty).
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep lineNumberFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                builder.append(record.getSourceLineNumber());
            }

            @Override
            public boolean isCallerInformationRequired() {
                return true;
            }
        };
    }

    /**
     * Create a format step which emits the formatted log message text with the given justification rules.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep messageFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return messageFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth);
    }

    /**
     * Create a format step which emits the formatted log message text with the given justification rules.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep messageFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                String formatted;
                if (formatter == null || record.getFormatStyle() == ExtLogRecord.FormatStyle.PRINTF && ! (formatter instanceof ExtFormatter)) {
                    formatted = record.getFormattedMessage();
                } else {
                    formatted = formatter.formatMessage(record);
                }
                builder.append(formatted);
                final Throwable t = record.getThrown();
                if (t != null) {
                    builder.append(": ");
                    t.printStackTrace(new PrintWriter(new StringBuilderWriter(builder)));
                }
            }
        };
    }

    /**
     * Create a format step which emits the formatted log message text (simple version, no exception traces) with the given justification rules.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep simpleMessageFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return simpleMessageFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth);
    }

    /**
     * Create a format step which emits the formatted log message text (simple version, no exception traces) with the given justification rules.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep simpleMessageFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                String formatted;
                if (formatter == null || record.getFormatStyle() == ExtLogRecord.FormatStyle.PRINTF && ! (formatter instanceof ExtFormatter)) {
                    formatted = record.getFormattedMessage();
                } else {
                    formatted = formatter.formatMessage(record);
                }
                builder.append(formatted);
            }
        };
    }

    /**
     * Create a format step which emits the formatted log message text (simple version, no exception traces) with the given justification rules.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep simpleMessageFormatStep(final ExtFormatter formatter, final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return simpleMessageFormatStep(formatter, leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth);
    }

    /**
     * Create a format step which emits the formatted log message text (simple version, no exception traces) with the given justification rules.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep simpleMessageFormatStep(final ExtFormatter formatter, final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                builder.append(formatter.format(record));
            }
        };
    }

    /**
     * Create a format step which emits the stack trace of an exception with the given justification rules.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @param extended {@code true} if the stack trace should attempt to include extended JAR version information
     * @return the format step
     */
    public static FormatStep exceptionFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth, final boolean extended) {
        return exceptionFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth, null);
    }

    /**
     * Create a format step which emits the stack trace of an exception with the given justification rules.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep exceptionFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth, final String argument) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        final Throwable t = record.getThrown();
                        if (t != null) {
                            int depth = -1;
                            if (argument != null) {
                                try {
                                    depth = Integer.parseInt(argument);
                                } catch (NumberFormatException ignore) {
                                }
                            }
                            StackTraceFormatter.renderStackTrace(builder, t, depth);
                        }
                        return null;
                    }
                });
            }
        };
    }

    /**
     * Create a format step which emits the log message resource key (if any) with the given justification rules.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep resourceKeyFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return resourceKeyFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth);
    }

    /**
     * Create a format step which emits the log message resource key (if any) with the given justification rules.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep resourceKeyFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                final String key = record.getResourceKey();
                if (key != null) builder.append(key);
            }
        };
    }

    /**
     * Create a format step which emits the source method name with the given justification rules (NOTE: call stack
     * introspection introduces a significant performance penalty).
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep methodNameFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return methodNameFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth);
    }

    /**
     * Create a format step which emits the source method name with the given justification rules (NOTE: call stack
     * introspection introduces a significant performance penalty).
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep methodNameFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                builder.append(record.getSourceMethodName());
            }

            @Override
            public boolean isCallerInformationRequired() {
                return true;
            }
        };
    }

    private static final String separatorString;

    static {
        separatorString = doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty("line.separator");
            }
        });
    }

    /**
     * Create a format step which emits the platform line separator.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep lineSeparatorFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return lineSeparatorFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth);
    }

    /**
     * Create a format step which emits the platform line separator.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep lineSeparatorFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                builder.append(separatorString);
            }
        };
    }

    /**
     * Create a format step which emits the log level name.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep levelFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return levelFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth);
    }

    /**
     * Create a format step which emits the log level name.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep levelFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                final Level level = record.getLevel();
                builder.append(level.getName());
            }
        };
    }

    /**
     * Create a format step which emits the localized log level name.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep localizedLevelFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return localizedLevelFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth);
    }

    /**
     * Create a format step which emits the localized log level name.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep localizedLevelFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                final Level level = record.getLevel();
                builder.append(level.getResourceBundleName() != null ? level.getLocalizedName() : level.getName());
            }
        };
    }

    /**
     * Create a format step which emits the number of milliseconds since the given base time.
     *
     * @param baseTime the base time as milliseconds as per {@link System#currentTimeMillis()}
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep relativeTimeFormatStep(final long baseTime, final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return relativeTimeFormatStep(baseTime, leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth);
    }

    /**
     * Create a format step which emits the number of milliseconds since the given base time.
     *
     * @param baseTime the base time as milliseconds as per {@link System#currentTimeMillis()}
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep relativeTimeFormatStep(final long baseTime, final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                builder.append(record.getMillis() - baseTime);
            }
        };
    }

    /**
     * Create a format step which emits the id if {@code id} is passed as the argument, otherwise the the thread name
     * is used.
     *
     * @param argument          the argument which may be {@code id} to indicate the thread id or {@code null} to
     *                          indicate the thread name
     * @param leftJustify       {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth      the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth      the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     *
     * @return the format step
     */
    public static FormatStep threadFormatStep(final String argument, final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        if (argument != null && THREAD_ID.equals(argument.toLowerCase(Locale.ROOT))) {
            return threadIdFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth);
        }
        return threadNameFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth);
    }

    /**
     * Create a format step which emits the id of the thread which originated the log record.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep threadIdFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                builder.append(record.getThreadID());
            }
        };
    }

    /**
     * Create a format step which emits the name of the thread which originated the log record.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep threadNameFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return threadNameFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth);
    }

    /**
     * Create a format step which emits the name of the thread which originated the log record.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep threadNameFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                builder.append(record.getThreadName());
            }
        };
    }

    /**
     * Create a format step which emits the NDC value of the log record.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep ndcFormatStep(final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return ndcFormatStep(leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth, 0);
    }

    /**
     * Create a format step which emits the NDC value of the log record.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @param count the limit to the number of segments to format
     * @return the format step
     */
    public static FormatStep ndcFormatStep(final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth, final int count) {
        return new SegmentedFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth, count) {
            public String getSegmentedSubject(final ExtLogRecord record) {
                return record.getNdc();
            }
        };
    }

    /**
     * Create a format step which emits the MDC value associated with the given key of the log record.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep mdcFormatStep(final String key, final boolean leftJustify, final int minimumWidth, final int maximumWidth) {
        return mdcFormatStep(key, leftJustify, minimumWidth, DEFAULT_TRUNCATE_BEGINNING, maximumWidth);
    }

    /**
     * Create a format step which emits the MDC value associated with the given key of the log record.
     *
     * @param leftJustify {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     * @return the format step
     */
    public static FormatStep mdcFormatStep(final String key, final boolean leftJustify, final int minimumWidth, final boolean truncateBeginning, final int maximumWidth) {
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                if (key == null) {
                    builder.append(new TreeMap<>(record.getMdcCopy()));
                } else {
                    final String value = record.getMdc(key);
                    if (value != null) {
                        builder.append(value);
                    }
                }
            }
        };
    }

    public static FormatStep formatColor(final ColorMap colors, final String color) {
            return new FormatStep() {
            public void render(final StringBuilder builder, final ExtLogRecord record) {
                String code = colors.getCode(color, record.getLevel());
                if (code != null) {
                    builder.append(code);
                }
            }

            public int estimateLength() {
                return 7;
            }

        };
    }

    /**
     * Create a format step which emits a system property value associated with the given key.
     *
     * @param argument          the argument that may be a key or key with a default value separated by a colon, cannot
     *                          be {@code null}
     * @param leftJustify       {@code true} to left justify, {@code false} to right justify
     * @param minimumWidth      the minimum field width, or 0 for none
     * @param truncateBeginning {@code true} to truncate the beginning, otherwise {@code false} to truncate the end
     * @param maximumWidth      the maximum field width (must be greater than {@code minimumFieldWidth}), or 0 for none
     *
     * @return the format step
     *
     * @throws IllegalArgumentException if the {@code argument} is {@code null}
     * @throws SecurityException        if a security manager exists and its {@code checkPropertyAccess} method doesn't
     *                                  allow access to the specified system property
     */
    public static FormatStep systemPropertyFormatStep(final String argument, final boolean leftJustify, final int minimumWidth,
                                                      final boolean truncateBeginning, final int maximumWidth) {
        if (argument == null) {
            throw new IllegalArgumentException("System property requires a key for the lookup");
        }
        return new JustifyingFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth) {
            public void renderRaw(Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
                // Check for a default value
                final String[] parts = argument.split("(?<!\\\\):");
                final String key = parts[0];
                String value = System.getProperty(key);
                if (value == null && parts.length > 1) {
                    value = parts[1];
                }
                builder.append(value);
            }
        };
    }

    static Map<Integer, Segment> parsePatternSegments(final String pattern) {
        final Map<Integer, Segment> segments = new HashMap<Integer, Segment>();
        StringBuilder len = new StringBuilder();
        StringBuilder text = new StringBuilder();
        int pos = 0;
        // Process each character
        for (char c : pattern.toCharArray()) {
            if (c >= '0' && c <= '9') {
                len.append(c);
            } else if (c == '.') {
                pos++;
                final int i = (len.length() > 0 ? Integer.parseInt(len.toString()) : 0);
                segments.put(pos, new Segment(i, text.length() > 0 ? text.toString() : null));
                text = new StringBuilder();
                len = new StringBuilder();
            } else {
                text.append(c);
            }
        }
        if (len.length() > 0 || text.length() > 0) {
            pos++;
            final int i = (len.length() > 0 ? Integer.parseInt(len.toString()) : 0);
            segments.put(pos, new Segment(i, text.length() > 0 ? text.toString() : null));
        }
        return Collections.unmodifiableMap(segments);
    }

    static Deque<String> parseCategorySegments(final String category) {
        // The category needs to be split into segments
        final Deque<String> categorySegments = new ArrayDeque<String>();
        StringBuilder cat = new StringBuilder();
        for (char c : category.toCharArray()) {
            if (c == '.') {
                if (cat.length() > 0) {
                    categorySegments.add(cat.toString());
                    cat = new StringBuilder();
                } else {
                    categorySegments.add("");
                }
            } else {
                cat.append(c);
            }
        }
        if (cat.length() > 0) {
            categorySegments.add(cat.toString());
        }
        return categorySegments;
    }

    static class Segment {
        final int len;
        final String text;

        Segment(final int len, final String text) {
            this.len = len;
            this.text = text;
        }
    }
}
