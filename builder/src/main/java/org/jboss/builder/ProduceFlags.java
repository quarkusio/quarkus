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

package org.jboss.builder;

import org.wildfly.common.Assert;
import org.wildfly.common.flags.Flags;

/**
 * Flags which can be set on consume declarations.
 */
public final class ProduceFlags extends Flags<ProduceFlag, ProduceFlags> {

    protected ProduceFlags value(final int bits) {
        return values[bits & enumValues.length - 1];
    }

    protected ProduceFlags this_() {
        return this;
    }

    protected ProduceFlag itemOf(final int index) {
        return enumValues[index];
    }

    protected ProduceFlag castItemOrNull(final Object obj) {
        return obj instanceof ProduceFlag ? (ProduceFlag) obj : null;
    }

    protected ProduceFlags castThis(final Object obj) {
        return (ProduceFlags) obj;
    }

    private ProduceFlags(int val) {
        super(val);
    }

    private static final ProduceFlag[] enumValues = ProduceFlag.values();
    private static final ProduceFlags[] values;

    static {
        final ProduceFlags[] flags = new ProduceFlags[1 << ProduceFlag.values().length];
        for (int i = 0; i < flags.length; i++) {
            flags[i] = new ProduceFlags(i);
        }
        values = flags;
    }

    public static ProduceFlags of(ProduceFlag flag) {
        Assert.checkNotNullParam("flag", flag);
        return values[1 << flag.ordinal()];
    }

    public static final ProduceFlags NONE = values[0];
}
