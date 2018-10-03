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

import java.util.Arrays;

/**
 * Nested diagnostic context.  This is basically a thread-local stack that holds a string which can be included
 * in a log message.
 */
public final class NDC {

    private NDC() {}

    private static final Holder ndc = new Holder();

    /**
     * Push a value on to the NDC stack, returning the new stack depth which should later be used to restore the stack.
     *
     * @param context the new value
     * @return the new stack depth
     */
    public static int push(String context) {
        final Stack<String> stack = ndc.get();
        try {
            return stack.depth();
        } finally {
            stack.push(context);
        }
    }

    /**
     * Pop the topmost value from the NDC stack and return it.
     *
     * @return the old topmost value
     */
    public static String pop() {
        final Stack<String> stack = ndc.get();
        if (stack.isEmpty()) {
            return "";
        } else {
            return stack.pop();
        }
    }

    /**
     * Clear the thread's NDC stack.
     */
    public static void clear() {
        ndc.get().trimTo(0);
    }

    /**
     * Trim the thread NDC stack down to no larger than the given size.  Used to restore the stack to the depth returned
     * by a {@code push()}.
     *
     * @param size the new size
     */
    public static void trimTo(int size) {
        ndc.get().trimTo(size);
    }

    /**
     * Get the current NDC stack depth.
     *
     * @return the stack depth
     */
    public static int getDepth() {
        return ndc.get().depth();
    }

    /**
     * Get the current NDC value.
     *
     * @return the current NDC value, or {@code ""} if there is none
     */
    public static String get() {
        final Stack<String> stack = ndc.get();
        if (stack.isEmpty()) {
            return "";
        } else {
            return stack.toString();
        }
    }

    /**
     * Provided for compatibility with log4j.  Get the NDC value that is {@code n} entries from the bottom.
     *
     * @param n the index
     * @return the value or {@code null} if there is none
     */
    public static String get(int n) {
        return ndc.get().get(n);
    }

    private static final class Holder extends ThreadLocal<Stack<String>> {
        protected Stack<String> initialValue() {
            return new Stack<String>();
        }
    }

    private static final class Stack<T> {
        private Object[] data = new Object[32];
        private int sp;

        public void push(T value) {
            final int oldlen = data.length;
            if (sp == oldlen) {
                Object[] newdata = new Object[oldlen * 3 / 2];
                System.arraycopy(data, 0, newdata, 0, oldlen);
                data = newdata;
            }
            data[sp++] = value;
        }

        @SuppressWarnings("unchecked")
        public T pop() {
            try {
                return (T) data[--sp];
            } finally {
                data[sp] = null;
            }
        }

        @SuppressWarnings("unchecked")
        public T top() {
            return (T) data[sp - 1];
        }

        public boolean isEmpty() {
            return sp == 0;
        }

        public int depth() {
            return sp;
        }

        public void trimTo(int max) {
            final int sp = this.sp;
            if (sp > max) {
                Arrays.fill(data, max, sp - 1, null);
                this.sp = max;
            }
        }

        @SuppressWarnings("unchecked")
        public T get(int n) {
            return n < sp ? (T) data[n] : null;
        }

        public String toString() {
            final StringBuilder b = new StringBuilder();
            final int sp = this.sp;
            for (int i = 0; i < sp; i++) {
                b.append(data[i]);
                if ((i + 1) < sp) {
                    b.append('.');
                }
            }
            return b.toString();
        }
    }
}
