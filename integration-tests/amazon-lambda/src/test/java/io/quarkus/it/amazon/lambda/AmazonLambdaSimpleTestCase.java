/*
 * Copyright 2019 Red Hat, Inc.
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

package io.quarkus.it.amazon.lambda;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.amazon.lambda.test.LambdaClient;
import io.quarkus.amazon.lambda.test.LambdaException;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonLambdaSimpleTestCase {

    @Test
    public void testSimpleLambdaSucess() throws Exception {
        InputObject in = new InputObject();
        in.setGreeting("Hello");
        in.setName("Stu");
        OutputObject out = LambdaClient.invoke(OutputObject.class, in);
        Assertions.assertEquals("Hello Stu", out.getResult());
        Assertions.assertTrue(out.getRequestId().matches("aws-request-\\d"), "Expected requestId as 'aws-request-<number>'");
    }

    @Test
    public void testSimpleLambdaFailure() throws Exception {
        InputObject in = new InputObject();
        in.setGreeting("Hello");
        in.setName("Stuart");
        try {
            OutputObject out = LambdaClient.invoke(OutputObject.class, in);
            out.getResult();
            Assertions.fail();
        } catch (LambdaException e) {
            Assertions.assertEquals(ProcessingService.CAN_ONLY_GREET_NICKNAMES, e.getMessage());
            Assertions.assertEquals(IllegalArgumentException.class.getName(), e.getType());
        }

    }
}
