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

package org.jboss.shamrock.example.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.jboss.shamrock.test.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Ken Finnigan
 */
@RunWith(ShamrockTest.class)
public class OpenApiTestCase {

    @Test
    public void testOpenAPIJSON() throws Exception {
        URL uri = new URL("http://localhost:8080/openapi");
        URLConnection connection = uri.openConnection();
        connection.setRequestProperty("Accept", "application/json");
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        JsonReader parser = Json.createReader(new ByteArrayInputStream(out.toByteArray()));
        JsonObject obj = parser.readObject();
        Assert.assertNotNull(obj);

        Assert.assertEquals("3.0.1", obj.getString("openapi"));
        Assert.assertEquals("Generated API", obj.getJsonObject("info").getString("title"));
        Assert.assertEquals("1.0", obj.getJsonObject("info").getString("version"));

        JsonObject paths = obj.getJsonObject("paths");

        JsonObject testObj = paths.getJsonObject("/rest/test");
        Assert.assertNotNull(testObj);
        Set<String> keys = testObj.keySet();
        Assert.assertEquals(1, keys.size());
        Assert.assertEquals("get", keys.iterator().next());


        JsonObject injectionObj = paths.getJsonObject("/rest/test/rx");
        Assert.assertNotNull(injectionObj);
        keys = injectionObj.keySet();
        Assert.assertEquals(1, keys.size());
        Assert.assertEquals("get", keys.iterator().next());
    }
}
