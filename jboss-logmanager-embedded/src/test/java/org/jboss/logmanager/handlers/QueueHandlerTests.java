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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class QueueHandlerTests extends AbstractHandlerTest {

    @Test
    public void testQueueSize() throws Exception {
        final QueueHandler handler = new QueueHandler(5);

        // Log 6 records and ensure only 5 are available
        for (int i = 0; i < 6; i++) {
            handler.publish(createLogRecord("Test message %d", i));
        }
        final ExtLogRecord[] records = handler.getQueue();

        Assert.assertEquals("QueueHandler held onto more records than allowed", 5, records.length);

        // Check each message, the last should be the sixth
        Assert.assertEquals("Test message 1", records[0].getMessage());
        Assert.assertEquals("Test message 2", records[1].getMessage());
        Assert.assertEquals("Test message 3", records[2].getMessage());
        Assert.assertEquals("Test message 4", records[3].getMessage());
        Assert.assertEquals("Test message 5", records[4].getMessage());
    }

    @Test
    public void testNestedHandlers() throws Exception {
        // Create a nested a handler
        final NestedHandler nestedHandler = new NestedHandler();
        nestedHandler.setLevel(Level.INFO);

        final QueueHandler handler = new QueueHandler(10);
        handler.setLevel(Level.ERROR);
        handler.addHandler(nestedHandler);

        // Log an info messages
        handler.publish(createLogRecord(Level.INFO, "Test message"));
        Assert.assertEquals(1, nestedHandler.getRecords().size());
        Assert.assertEquals(0, handler.getQueue().length);

        // Log a warn messages
        handler.publish(createLogRecord(Level.WARN, "Test message"));
        Assert.assertEquals(2, nestedHandler.getRecords().size());
        Assert.assertEquals(0, handler.getQueue().length);

        // Log an error messages
        handler.publish(createLogRecord(Level.ERROR, "Test message"));
        Assert.assertEquals(3, nestedHandler.getRecords().size());
        Assert.assertEquals(1, handler.getQueue().length);
    }

    static class NestedHandler extends ExtHandler {
        private final List<ExtLogRecord> records = new ArrayList<ExtLogRecord>();

        @Override
        protected void doPublish(final ExtLogRecord record) {
            records.add(record);
            super.doPublish(record);
        }

        Collection<ExtLogRecord> getRecords() {
            return Collections.unmodifiableCollection(records);
        }
    }
}
