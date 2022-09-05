package io.quarkus.netty.deployment;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import org.jboss.logging.Logger;

import io.netty.util.internal.logging.AbstractInternalLogger;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class JBossNettyLoggerFactory extends InternalLoggerFactory {

    @Override
    protected InternalLogger newInstance(String name) {
        return new JBossNettyInternalLogger(name);
    }

    private static final class JBossNettyInternalLogger extends AbstractInternalLogger {

        final Logger log;

        JBossNettyInternalLogger(String name) {
            super(name);
            log = Logger.getLogger(name);
        }

        @Override
        public boolean isTraceEnabled() {
            return log.isTraceEnabled();
        }

        @Override
        public void trace(String msg) {
            if (isTraceEnabled()) {
                log.trace(msg);
            }
        }

        @Override
        public void trace(String format, Object arg) {
            if (isTraceEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                trace0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void trace(String format, Object argA, Object argB) {
            if (isTraceEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                trace0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void trace(String format, Object... arguments) {
            if (isTraceEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arguments);
                trace0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void trace(String msg, Throwable t) {
            if (isTraceEnabled()) {
                trace0(msg, t);
            }
        }

        private void trace0(String msg, Throwable t) {
            log.trace(msg, t);
        }

        @Override
        public boolean isDebugEnabled() {
            return log.isDebugEnabled();
        }

        @Override
        public void debug(String msg) {
            if (isDebugEnabled()) {
                log.debug(msg);
            }
        }

        @Override
        public void debug(String format, Object arg) {
            if (isDebugEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                debug0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void debug(String format, Object argA, Object argB) {
            if (isDebugEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                debug0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void debug(String format, Object... arguments) {
            if (isDebugEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arguments);
                debug0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void debug(String msg, Throwable t) {
            if (isDebugEnabled()) {
                debug0(msg, t);
            }
        }

        private void debug0(String msg, Throwable t) {
            log.debug(msg, t);
        }

        @Override
        public boolean isInfoEnabled() {
            return log.isInfoEnabled();
        }

        @Override
        public void info(String msg) {
            if (isInfoEnabled()) {
                log.info(msg);
            }
        }

        @Override
        public void info(String format, Object arg) {
            if (isInfoEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                info0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void info(String format, Object argA, Object argB) {
            if (isInfoEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                info0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void info(String format, Object... arguments) {
            if (isInfoEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arguments);
                info0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void info(String msg, Throwable t) {
            if (isInfoEnabled()) {
                info0(msg, t);
            }
        }

        private void info0(String msg, Throwable t) {
            log.info(msg, t);
        }

        @Override
        public boolean isWarnEnabled() {
            return log.isEnabled(Logger.Level.WARN);
        }

        @Override
        public void warn(String msg) {
            if (isWarnEnabled()) {
                log.warn(msg);
            }
        }

        @Override
        public void warn(String format, Object arg) {
            if (isWarnEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                warn0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void warn(String format, Object... arguments) {
            if (isWarnEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arguments);
                warn0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void warn(String format, Object argA, Object argB) {
            if (isWarnEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                warn0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void warn(String msg, Throwable t) {
            if (isWarnEnabled()) {
                warn0(msg, t);
            }
        }

        private void warn0(String msg, Throwable t) {
            log.warn(msg, t);
        }

        @Override
        public boolean isErrorEnabled() {
            return log.isEnabled(Logger.Level.ERROR);
        }

        @Override
        public void error(String msg) {
            if (isErrorEnabled()) {
                log.error(msg);
            }
        }

        @Override
        public void error(String format, Object arg) {
            if (isErrorEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                error0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void error(String format, Object argA, Object argB) {
            if (isErrorEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                error0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void error(String format, Object... arguments) {
            if (isErrorEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arguments);
                error0(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void error(String msg, Throwable t) {
            if (isErrorEnabled()) {
                error0(msg, t);
            }
        }

        private void error0(String msg, Throwable t) {
            log.error(msg, t);
        }

    }

    // MessageFormatter class copied from Netty because its methods are package private

    /*
     * Copyright 2013 The Netty Project
     *
     * The Netty Project licenses this file to you under the Apache License,
     * version 2.0 (the "License"); you may not use this file except in compliance
     * with the License. You may obtain a copy of the License at:
     *
     * https://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
     * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
     * License for the specific language governing permissions and limitations
     * under the License.
     */

    /**
     * Formats messages according to very simple substitution rules. Substitutions
     * can be made 1, 2 or more arguments.
     * <p/>
     * <p/>
     * For example,
     * <p/>
     *
     * <pre>
     * MessageFormatter.format(&quot;Hi {}.&quot;, &quot;there&quot;)
     * </pre>
     * <p/>
     * will return the string "Hi there.".
     * <p/>
     * The {} pair is called the <em>formatting anchor</em>. It serves to designate
     * the location where arguments need to be substituted within the message
     * pattern.
     * <p/>
     * In case your message contains the '{' or the '}' character, you do not have
     * to do anything special unless the '}' character immediately follows '{'. For
     * example,
     * <p/>
     *
     * <pre>
     * MessageFormatter.format(&quot;Set {1,2,3} is not equal to {}.&quot;, &quot;1,2&quot;);
     * </pre>
     * <p/>
     * will return the string "Set {1,2,3} is not equal to 1,2.".
     * <p/>
     * <p/>
     * If for whatever reason you need to place the string "{}" in the message
     * without its <em>formatting anchor</em> meaning, then you need to escape the
     * '{' character with '\', that is the backslash character. Only the '{'
     * character should be escaped. There is no need to escape the '}' character.
     * For example,
     * <p/>
     *
     * <pre>
     * MessageFormatter.format(&quot;Set \\{} is not equal to {}.&quot;, &quot;1,2&quot;);
     * </pre>
     * <p/>
     * will return the string "Set {} is not equal to 1,2.".
     * <p/>
     * <p/>
     * The escaping behavior just described can be overridden by escaping the escape
     * character '\'. Calling
     * <p/>
     *
     * <pre>
     * MessageFormatter.format(&quot;File name is C:\\\\{}.&quot;, &quot;file.zip&quot;);
     * </pre>
     * <p/>
     * will return the string "File name is C:\file.zip".
     * <p/>
     * <p/>
     * The formatting conventions are different than those of {@link MessageFormat}
     * which ships with the Java platform. This is justified by the fact that
     * SLF4J's implementation is 10 times faster than that of {@link MessageFormat}.
     * This local performance difference is both measurable and significant in the
     * larger context of the complete logging processing chain.
     * <p/>
     * <p/>
     * See also {@link #format(String, Object)},
     * {@link #format(String, Object, Object)} and
     * {@link #arrayFormat(String, Object[])} methods for more details.
     */
    public static final class MessageFormatter {
        private static final String DELIM_STR = "{}";
        private static final char ESCAPE_CHAR = '\\';

        /**
         * Performs single argument substitution for the 'messagePattern' passed as
         * parameter.
         * <p/>
         * For example,
         * <p/>
         *
         * <pre>
         * MessageFormatter.format(&quot;Hi {}.&quot;, &quot;there&quot;);
         * </pre>
         * <p/>
         * will return the string "Hi there.".
         * <p/>
         *
         * @param messagePattern The message pattern which will be parsed and formatted
         * @param arg The argument to be substituted in place of the formatting anchor
         * @return The formatted message
         */
        static FormattingTuple format(String messagePattern, Object arg) {
            return arrayFormat(messagePattern, new Object[] { arg });
        }

        /**
         * Performs a two argument substitution for the 'messagePattern' passed as
         * parameter.
         * <p/>
         * For example,
         * <p/>
         *
         * <pre>
         * MessageFormatter.format(&quot;Hi {}. My name is {}.&quot;, &quot;Alice&quot;, &quot;Bob&quot;);
         * </pre>
         * <p/>
         * will return the string "Hi Alice. My name is Bob.".
         *
         * @param messagePattern The message pattern which will be parsed and formatted
         * @param argA The argument to be substituted in place of the first formatting
         *        anchor
         * @param argB The argument to be substituted in place of the second formatting
         *        anchor
         * @return The formatted message
         */
        static FormattingTuple format(final String messagePattern,
                Object argA, Object argB) {
            return arrayFormat(messagePattern, new Object[] { argA, argB });
        }

        /**
         * Same principle as the {@link #format(String, Object)} and
         * {@link #format(String, Object, Object)} methods except that any number of
         * arguments can be passed in an array.
         *
         * @param messagePattern The message pattern which will be parsed and formatted
         * @param argArray An array of arguments to be substituted in place of formatting
         *        anchors
         * @return The formatted message
         */
        static FormattingTuple arrayFormat(final String messagePattern,
                final Object[] argArray) {
            if (argArray == null || argArray.length == 0) {
                return new FormattingTuple(messagePattern, null);
            }

            int lastArrIdx = argArray.length - 1;
            Object lastEntry = argArray[lastArrIdx];
            Throwable throwable = lastEntry instanceof Throwable ? (Throwable) lastEntry : null;

            if (messagePattern == null) {
                return new FormattingTuple(null, throwable);
            }

            int j = messagePattern.indexOf(DELIM_STR);
            if (j == -1) {
                // this is a simple string
                return new FormattingTuple(messagePattern, throwable);
            }

            StringBuilder sbuf = new StringBuilder(messagePattern.length() + 50);
            int i = 0;
            int L = 0;
            do {
                boolean notEscaped = j == 0 || messagePattern.charAt(j - 1) != ESCAPE_CHAR;
                if (notEscaped) {
                    // normal case
                    sbuf.append(messagePattern, i, j);
                } else {
                    sbuf.append(messagePattern, i, j - 1);
                    // check that escape char is not is escaped: "abc x:\\{}"
                    notEscaped = j >= 2 && messagePattern.charAt(j - 2) == ESCAPE_CHAR;
                }

                i = j + 2;
                if (notEscaped) {
                    deeplyAppendParameter(sbuf, argArray[L], null);
                    L++;
                    if (L > lastArrIdx) {
                        break;
                    }
                } else {
                    sbuf.append(DELIM_STR);
                }
                j = messagePattern.indexOf(DELIM_STR, i);
            } while (j != -1);

            // append the characters following the last {} pair.
            sbuf.append(messagePattern, i, messagePattern.length());
            return new FormattingTuple(sbuf.toString(), L <= lastArrIdx ? throwable : null);
        }

        // special treatment of array values was suggested by 'lizongbo'
        private static void deeplyAppendParameter(StringBuilder sbuf, Object o,
                Set<Object[]> seenSet) {
            if (o == null) {
                sbuf.append("null");
                return;
            }
            Class<?> objClass = o.getClass();
            if (!objClass.isArray()) {
                if (Number.class.isAssignableFrom(objClass)) {
                    // Prevent String instantiation for some number types
                    if (objClass == Long.class) {
                        sbuf.append(((Long) o).longValue());
                    } else if (objClass == Integer.class || objClass == Short.class || objClass == Byte.class) {
                        sbuf.append(((Number) o).intValue());
                    } else if (objClass == Double.class) {
                        sbuf.append(((Double) o).doubleValue());
                    } else if (objClass == Float.class) {
                        sbuf.append(((Float) o).floatValue());
                    } else {
                        safeObjectAppend(sbuf, o);
                    }
                } else {
                    safeObjectAppend(sbuf, o);
                }
            } else {
                // check for primitive array types because they
                // unfortunately cannot be cast to Object[]
                sbuf.append('[');
                if (objClass == boolean[].class) {
                    booleanArrayAppend(sbuf, (boolean[]) o);
                } else if (objClass == byte[].class) {
                    byteArrayAppend(sbuf, (byte[]) o);
                } else if (objClass == char[].class) {
                    charArrayAppend(sbuf, (char[]) o);
                } else if (objClass == short[].class) {
                    shortArrayAppend(sbuf, (short[]) o);
                } else if (objClass == int[].class) {
                    intArrayAppend(sbuf, (int[]) o);
                } else if (objClass == long[].class) {
                    longArrayAppend(sbuf, (long[]) o);
                } else if (objClass == float[].class) {
                    floatArrayAppend(sbuf, (float[]) o);
                } else if (objClass == double[].class) {
                    doubleArrayAppend(sbuf, (double[]) o);
                } else {
                    objectArrayAppend(sbuf, (Object[]) o, seenSet);
                }
                sbuf.append(']');
            }
        }

        private static void safeObjectAppend(StringBuilder sbuf, Object o) {
            try {
                String oAsString = o.toString();
                sbuf.append(oAsString);
            } catch (Throwable t) {
                System.err
                        .println("SLF4J: Failed toString() invocation on an object of type ["
                                + o.getClass().getName() + ']');
                t.printStackTrace();
                sbuf.append("[FAILED toString()]");
            }
        }

        private static void objectArrayAppend(StringBuilder sbuf, Object[] a, Set<Object[]> seenSet) {
            if (a.length == 0) {
                return;
            }
            if (seenSet == null) {
                seenSet = new HashSet<Object[]>(a.length);
            }
            if (seenSet.add(a)) {
                deeplyAppendParameter(sbuf, a[0], seenSet);
                for (int i = 1; i < a.length; i++) {
                    sbuf.append(", ");
                    deeplyAppendParameter(sbuf, a[i], seenSet);
                }
                // allow repeats in siblings
                seenSet.remove(a);
            } else {
                sbuf.append("...");
            }
        }

        private static void booleanArrayAppend(StringBuilder sbuf, boolean[] a) {
            if (a.length == 0) {
                return;
            }
            sbuf.append(a[0]);
            for (int i = 1; i < a.length; i++) {
                sbuf.append(", ");
                sbuf.append(a[i]);
            }
        }

        private static void byteArrayAppend(StringBuilder sbuf, byte[] a) {
            if (a.length == 0) {
                return;
            }
            sbuf.append(a[0]);
            for (int i = 1; i < a.length; i++) {
                sbuf.append(", ");
                sbuf.append(a[i]);
            }
        }

        private static void charArrayAppend(StringBuilder sbuf, char[] a) {
            if (a.length == 0) {
                return;
            }
            sbuf.append(a[0]);
            for (int i = 1; i < a.length; i++) {
                sbuf.append(", ");
                sbuf.append(a[i]);
            }
        }

        private static void shortArrayAppend(StringBuilder sbuf, short[] a) {
            if (a.length == 0) {
                return;
            }
            sbuf.append(a[0]);
            for (int i = 1; i < a.length; i++) {
                sbuf.append(", ");
                sbuf.append(a[i]);
            }
        }

        private static void intArrayAppend(StringBuilder sbuf, int[] a) {
            if (a.length == 0) {
                return;
            }
            sbuf.append(a[0]);
            for (int i = 1; i < a.length; i++) {
                sbuf.append(", ");
                sbuf.append(a[i]);
            }
        }

        private static void longArrayAppend(StringBuilder sbuf, long[] a) {
            if (a.length == 0) {
                return;
            }
            sbuf.append(a[0]);
            for (int i = 1; i < a.length; i++) {
                sbuf.append(", ");
                sbuf.append(a[i]);
            }
        }

        private static void floatArrayAppend(StringBuilder sbuf, float[] a) {
            if (a.length == 0) {
                return;
            }
            sbuf.append(a[0]);
            for (int i = 1; i < a.length; i++) {
                sbuf.append(", ");
                sbuf.append(a[i]);
            }
        }

        private static void doubleArrayAppend(StringBuilder sbuf, double[] a) {
            if (a.length == 0) {
                return;
            }
            sbuf.append(a[0]);
            for (int i = 1; i < a.length; i++) {
                sbuf.append(", ");
                sbuf.append(a[i]);
            }
        }

        private MessageFormatter() {
        }
    }

    // FormattingTuple class copied from Netty because its constructor is package private

    /**
     * Copyright (c) 2004-2011 QOS.ch
     * All rights reserved.
     *
     * Permission is hereby granted, free of charge, to any person obtaining
     * a copy of this software and associated documentation files (the
     * "Software"), to deal in the Software without restriction, including
     * without limitation the rights to use, copy, modify, merge, publish,
     * distribute, sublicense, and/or sell copies of the Software, and to
     * permit persons to whom the Software is furnished to do so, subject to
     * the following conditions:
     *
     * The above copyright notice and this permission notice shall be
     * included in all copies or substantial portions of the Software.
     *
     * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
     * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
     * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
     * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
     * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
     * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
     * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
     *
     */

    public static final class FormattingTuple {

        private final String message;
        private final Throwable throwable;

        FormattingTuple(String message, Throwable throwable) {
            this.message = message;
            this.throwable = throwable;
        }

        public String getMessage() {
            return message;
        }

        public Throwable getThrowable() {
            return throwable;
        }
    }

}
