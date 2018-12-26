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
public final class ConsumeFlags extends Flags<ConsumeFlag, ConsumeFlags> {

    @Override
    protected ConsumeFlags value(final int bits) {
        return values[bits & enumValues.length - 1];
    }

    @Override
    protected ConsumeFlags this_() {
        return this;
    }

    @Override
    protected ConsumeFlag itemOf(final int index) {
        return enumValues[index];
    }

    @Override
    protected ConsumeFlag castItemOrNull(final Object obj) {
        return obj instanceof ConsumeFlag ? (ConsumeFlag) obj : null;
    }

    @Override
    protected ConsumeFlags castThis(final Object obj) {
        return (ConsumeFlags) obj;
    }

    private ConsumeFlags(int val) {
        super(val);
    }

    private static final ConsumeFlag[] enumValues = ConsumeFlag.values();
    private static final ConsumeFlags[] values;

    static {
        final ConsumeFlags[] flags = new ConsumeFlags[1 << ConsumeFlag.values().length];
        for (int i = 0; i < flags.length; i++) {
            flags[i] = new ConsumeFlags(i);
        }
        values = flags;
    }

    public static ConsumeFlags of(ConsumeFlag flag) {
        Assert.checkNotNullParam("flag", flag);
        return values[1 << flag.ordinal()];
    }

    public static final ConsumeFlags NONE = values[0];
}
