/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

/**
 */
public final class ConsumeFlags {
    private final int val;

    private ConsumeFlags(int val) {
        this.val = val;
    }

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
    public static final ConsumeFlags OPTIONAL = of(ConsumeFlag.OPTIONAL);

    public ConsumeFlags with(ConsumeFlag flag) {
        Assert.checkNotNullParam("flag", flag);
        return values[val | (1 << flag.ordinal())];
    }

    public boolean contains(ConsumeFlag flag) {
        return flag != null && (val & (1 << flag.ordinal())) != 0;
    }

    public boolean containsAll(ConsumeFlags other) {
        return other != null && (val | other.val) == val;
    }
}
