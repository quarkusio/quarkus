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

import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import static org.hamcrest.core.Is.is;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.handlers.ConsoleHandler.Target;
import org.junit.After;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class OutputStreamHandlerTest {

    private StringWriter out;
    private OutputStreamHandler handler;
    
    private static final Formatter NO_FORMATTER = new Formatter() {
        public String format(final LogRecord record) {
            return record.getMessage();
        }
    };
    
    public OutputStreamHandlerTest() {
    }
    
    @Before
    public void prepareBuffer() {
        out = new StringWriter();
    }
    
    @After
    public void cleanAll() throws IOException {
       handler.flush();
       handler.close();
       out.close();
    }
    
    @Test
    public void testSetEncoding() throws Exception {
        handler = new OutputStreamHandler();
        handler.setEncoding("UTF-8");
        assertThat(handler.getEncoding(), is("UTF-8"));
    }
    
   @Test
    public void testSetEncodingOnOutputStream() throws Exception {  
       handler = new ConsoleHandler(Target.CONSOLE, NO_FORMATTER);
        handler.setWriter(out);
        handler.setEncoding("UTF-8");
        assertThat(handler.getEncoding(), is("UTF-8"));
       handler.publish(new ExtLogRecord(Level.INFO, "Hello World", getClass().getName()));
        assertThat(out.toString(), is("Hello World"));
    }
    
    @Test
    public void testSetNullEncodingOnOutputStream() throws Exception {
        handler = new OutputStreamHandler(NO_FORMATTER);
        handler.setWriter(out);
        handler.setEncoding(null);
        handler.publish(new ExtLogRecord(Level.INFO, "Hello World", getClass().getName()));
        assertThat(out.toString(), is("Hello World")); 
    }
    
    
}
