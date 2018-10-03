/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

import java.util.logging.SimpleFormatter;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ExtHandlerTests {

    @Test
    public void testHandlerClose() throws Exception {
        final CloseHandler parent = new CloseHandler();
        final CloseHandler child1 = new CloseHandler();
        final CloseHandler child2 = new CloseHandler();
        parent.setHandlers(new CloseHandler[] {child1, child2, new CloseHandler()});

        // Ensure all handlers are not closed
        Assert.assertFalse(parent.closed);
        Assert.assertFalse(child1.closed);
        Assert.assertFalse(child2.closed);

        // Close the parent handler, the children should be closed
        parent.close();
        Assert.assertTrue(parent.closed);
        Assert.assertTrue(child1.closed);
        Assert.assertTrue(child2.closed);

        // Reset and wrap
        parent.reset();
        child1.reset();
        child2.reset();

        parent.setCloseChildren(false);

        // Ensure all handlers are not closed
        Assert.assertFalse(parent.closed);
        Assert.assertFalse(child1.closed);
        Assert.assertFalse(child2.closed);

        parent.close();

        // The parent should be closed, the others should be open
        Assert.assertTrue(parent.closed);
        Assert.assertFalse(child1.closed);
        Assert.assertFalse(child2.closed);

    }

    @Test
    public void testCallerCalculationCheckFormatterChange() throws Exception {
        final CloseHandler parent = new CloseHandler();

        // Create a formatter for the parent that will require caller calculation
        final PatternFormatter formatter = new PatternFormatter("%d %M %s%e%n");
        parent.setFormatter(formatter);

        Assert.assertTrue(parent.isCallerCalculationRequired());

        // Change the formatter to not require calculation, this should trigger false to be returned
        formatter.setPattern("%d %s%e%n");
        Assert.assertFalse(parent.isCallerCalculationRequired());
    }

    @Test
    public void testCallerCalculationCheckNewFormatter() throws Exception {
        final CloseHandler parent = new CloseHandler();

        // Create a formatter for the parent that will require caller calculation
        final PatternFormatter formatter = new PatternFormatter("%d %M %s%e%n");
        parent.setFormatter(formatter);

        Assert.assertTrue(parent.isCallerCalculationRequired());

        // Add a new formatter which should result in the caller calculation not to be required
        parent.setFormatter(new PatternFormatter("%d %s%e%n"));
        Assert.assertFalse(parent.isCallerCalculationRequired());
    }

    @Test
    public void testCallerCalculationCheckChildFormatter() throws Exception {
        final CloseHandler parent = new CloseHandler();
        final CloseHandler child = new CloseHandler();
        parent.addHandler(child);

        // Create a formatter for the parent that will require caller calculation
        final PatternFormatter formatter = new PatternFormatter("%d %M %s%e%n");
        child.setFormatter(formatter);
        Assert.assertTrue(parent.isCallerCalculationRequired());

        // Remove the child handler which should result in the caller calculation not being required since the formatter
        // is null
        parent.removeHandler(child);
        Assert.assertFalse(parent.isCallerCalculationRequired());

        // Add a formatter to the parent and add he child back, the parent handler will not require calculation, but
        // the child should
        parent.setFormatter(new PatternFormatter("%d %s%e%n"));
        parent.addHandler(child);
        Assert.assertTrue(parent.isCallerCalculationRequired());
    }

    @Test
    public void testCallerCalculationNonExtFormatter() throws Exception {
        final CloseHandler parent = new CloseHandler();
        final CloseHandler child = new CloseHandler();
        parent.addHandler(child);

        // Create a non ExtFormatter for the parent that will require caller calculation
        child.setFormatter(new SimpleFormatter());
        Assert.assertTrue(child.isCallerCalculationRequired());
        Assert.assertTrue(parent.isCallerCalculationRequired());
        parent.setFormatter(new SimpleFormatter());
        Assert.assertTrue(parent.isCallerCalculationRequired());
    }

    static class CloseHandler extends ExtHandler {
        private boolean closed = false;

        @Override
        public void close() {
            closed = true;
            super.close();
        }

        void reset() {
            closed = false;
        }
    }
}
