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

package io.quarkus.example.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * @author Ken Finnigan
 */
@QuarkusTest
public class OpenApiTestCase {

    @TestHTTPResource("openapi")
    URL uri;

    @Test
    public void testOpenAPIJSON() throws Exception {
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
        Assertions.assertNotNull(obj);

        Assertions.assertEquals("3.0.1", obj.getString("openapi"));
        Assertions.assertEquals("Generated API", obj.getJsonObject("info").getString("title"));
        Assertions.assertEquals("1.0", obj.getJsonObject("info").getString("version"));

        JsonObject paths = obj.getJsonObject("paths");

        JsonObject testObj = paths.getJsonObject("/test");
        Assertions.assertNotNull(testObj);
        Set<String> keys = testObj.keySet();
        Assertions.assertEquals(1, keys.size());
        Assertions.assertEquals("get", keys.iterator().next());

        JsonObject injectionObj = paths.getJsonObject("/test/rx");
        Assertions.assertNotNull(injectionObj);
        keys = injectionObj.keySet();
        Assertions.assertEquals(1, keys.size());
        Assertions.assertEquals("get", keys.iterator().next());

        // test RESTEasy extensions

        // make sure String, CompletionStage<String> and Single<String> are detected the same
        Assertions.assertEquals(testObj, injectionObj, "Normal and RX/Single have same schema");
        JsonObject csObj = paths.getJsonObject("/test/cs");
        Assertions.assertEquals(testObj, csObj, "Normal and RX/CS have same schema");

        JsonObject paramsObj = paths.getJsonObject("/test/params/{path}");
        JsonObject params2Obj = paths.getJsonObject("/test/params2/{path}");
        Assertions.assertEquals(paramsObj, params2Obj, "Normal and RESTEasy annotations have same schema");
    }
}
