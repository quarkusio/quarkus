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

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.jboss.shamrock.test.URLTester;
import org.jboss.shamrock.test.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class HealthTestCase {

    @Test
    public void testHealthCheck() {
        JsonReader parser = URLTester.relative("health").invokeURL().asJsonReader();
        JsonObject obj = parser.readObject();
        System.out.println(obj);
        Assert.assertEquals("UP", obj.getString("outcome"));
        JsonArray list = obj.getJsonArray("checks");
        Assert.assertEquals(1, list.size());
        JsonObject check = list.getJsonObject(0);
        Assert.assertEquals("UP", check.getString("state"));
        Assert.assertEquals("basic", check.getString("name"));
    }
}
