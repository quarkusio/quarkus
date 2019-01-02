/*
 * Copyright 2018 Red Hat, Inc.
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

package org.jboss.shamrock.example.testutils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.json.Json;
import javax.json.JsonReader;

/**
 * Convenience class to invoke an URL and return the response.
 */
public final class URLTester {

    private final URL fullURL;

    private URLTester(final URL fullURL) {
        this.fullURL = fullURL;
    }

    public static URLTester relative(final String relativePath) {
        final URL uri;
        try {
            uri = new URL("http://localhost:8080/" + relativePath);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return new URLTester(uri);
    }

    private URLResponse privateInvokeURL() throws IOException {
        URLConnection connection = fullURL.openConnection();
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        return new URLResponseAdapter(out);
    }

    public URLResponse invokeURL() {
        try {
            return privateInvokeURL();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class URLResponseAdapter implements URLResponse {

        private final ByteArrayOutputStream buffer;

        public URLResponseAdapter(final ByteArrayOutputStream buffer) {
            this.buffer = buffer;
        }

        @Override
        public String toString() {
            return new String(buffer.toByteArray());
        }

        @Override
        public String asString() {
            return toString();
        }

        @Override
        public InputStream asInputStream() {
            return new ByteArrayInputStream(buffer.toByteArray());
        }

        @Override
        public JsonReader asJsonReader() {
            return Json.createReader(asInputStream());
        }
    }

}
