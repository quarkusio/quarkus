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

package io.quarkus.deployment.recording;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.runtime.RuntimeValue;

public class TestTemplate {

    public static final ArrayDeque<Object> RESULT = new ArrayDeque<Object>();

    public void primitiveParams(boolean bool, byte b, char c, short s, int i, long l, float f, double d) {
        RESULT.add(bool);
        RESULT.add(b);
        RESULT.add(c);
        RESULT.add(s);
        RESULT.add(i);
        RESULT.add(l);
        RESULT.add(f);
        RESULT.add(d);
    }

    public void boxedPrimitiveParams(Boolean bool, Byte b, Character c, Short s, Integer i, Long l, Float f, Double d) {
        RESULT.add(bool);
        RESULT.add(b);
        RESULT.add(c);
        RESULT.add(s);
        RESULT.add(i);
        RESULT.add(l);
        RESULT.add(f);
        RESULT.add(d);
    }

    public void intArray(int... args) {
        RESULT.add(args);
    }

    public void doubleArray(double... args) {
        RESULT.add(args);
    }

    public void list(List<?> args) {
        RESULT.add(args);
    }

    public void set(Set<?> args) {
        RESULT.add(args);
    }

    public void map(Map<?, ?> args) {
        RESULT.add(args);
    }

    public void bean(TestJavaBean bean) {
        RESULT.add(bean);
    }

    public void bean(NonSerializable bean) {
        RESULT.add(bean);
    }

    public void add(RuntimeValue<TestJavaBean> bean) {
        bean.getValue().setIval(bean.getValue().getIval() + 1);
    }

    public void result(RuntimeValue<TestJavaBean> bean) {
        RESULT.add(bean.getValue());
    }
}
