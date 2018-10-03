/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2010 Red Hat, Inc.
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

package org.jboss.logging;

import java.util.ArrayDeque;

abstract class AbstractLoggerProvider {

    private final ThreadLocal<ArrayDeque<Entry>> ndcStack = new ThreadLocal<ArrayDeque<Entry>>();

    public void clearNdc() {
        ArrayDeque<Entry> stack = ndcStack.get();
        if (stack != null)
            stack.clear();
    }

    public String getNdc() {
        ArrayDeque<Entry> stack = ndcStack.get();
        return stack == null || stack.isEmpty() ? null : stack.peek().merged;
    }

    public int getNdcDepth() {
        ArrayDeque<Entry> stack = ndcStack.get();
        return stack == null ? 0 : stack.size();
    }

    public String peekNdc() {
        ArrayDeque<Entry> stack = ndcStack.get();
        return stack == null || stack.isEmpty() ? "" : stack.peek().current;
    }

    public String popNdc() {
        ArrayDeque<Entry> stack = ndcStack.get();
        return stack == null || stack.isEmpty() ? "" : stack.pop().current;
    }

    public void pushNdc(String message) {
        ArrayDeque<Entry> stack = ndcStack.get();
        if (stack == null) {
            stack = new ArrayDeque<Entry>();
            ndcStack.set(stack);
        }
        stack.push(stack.isEmpty() ? new Entry(message) : new Entry(stack.peek(), message));
    }

    public void setNdcMaxDepth(int maxDepth) {
        final ArrayDeque<Entry> stack = ndcStack.get();
        if (stack != null) while (stack.size() > maxDepth) stack.pop();
    }

    private static class Entry {

        private String merged;
        private String current;

        Entry(String current) {
            merged = current;
            this.current = current;
        }

        Entry(Entry parent, String current) {
            merged = parent.merged + ' ' + current;
            this.current = current;
        }
    }
}
