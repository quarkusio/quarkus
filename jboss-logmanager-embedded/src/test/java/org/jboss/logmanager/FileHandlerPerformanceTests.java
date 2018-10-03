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

import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.FileHandler;
import org.junit.Test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.logging.Formatter;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class FileHandlerPerformanceTests {
    private static final Formatter testFormatter = new PatternFormatter("%m\n");

    private static void initHandler(ExtHandler handler) throws UnsupportedEncodingException {
        handler.setFormatter(testFormatter);
        handler.setLevel(Level.ALL);
        handler.setAutoFlush(true);
        handler.setEncoding("utf-8");
    }

    private static void publish(final ExtHandler handler, final String msg) {
        handler.publish(new ExtLogRecord(Level.INFO, msg, null));
    }

    @Test
    public void testPerformance() throws Exception {
        final FileHandler handler = new FileHandler();
        initHandler(handler);
        final File tempFile = File.createTempFile("jblm-", ".log");
        tempFile.deleteOnExit();
        handler.setFile(tempFile);
        final long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            publish(handler, "Test message " + i);
        }
        // the result is system dependant and can therefore only be checked manually
        // a 'sluggish' build indicates a problem
        System.out.println((System.currentTimeMillis() - start));
    }
}
