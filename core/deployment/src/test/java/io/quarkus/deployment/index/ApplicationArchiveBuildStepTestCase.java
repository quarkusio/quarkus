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

package io.quarkus.deployment.index;

import static io.quarkus.deployment.index.ApplicationArchiveBuildStep.urlToPath;
import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.Test;

public class ApplicationArchiveBuildStepTestCase {
    @Test
    public void testUrlToPath() throws MalformedURLException {
        assertEquals(Paths.get("/a/path"), urlToPath(new URL("jar:file:/a/path!/META-INF/services/my.Service")));
        assertEquals(Paths.get("/a/path"), urlToPath(new URL("file:/a/path/META-INF/services/my.Service")));
        assertEquals(Paths.get("/a/path"), urlToPath(new URL("file:/a/path")));
    }

    @Test(expected = RuntimeException.class)
    public void testUrlToPathWithWrongProtocol() throws MalformedURLException {
        urlToPath(new URL("http://a/path"));
    }
}
