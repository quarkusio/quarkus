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

import org.jboss.shamrock.test.URLTester;
import org.jboss.shamrock.test.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class DatasourceTestCase {


    @Test
    public void testDataSource() {
        Assert.assertEquals("10", URLTester.relative("rest/datasource").invokeURL().asString());
    }

    @Test
    public void testDataSourceTransactions() {
        Assert.assertEquals("PASSED", URLTester.relative("rest/datasource/txn").invokeURL().asString());
    }

}
