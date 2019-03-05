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

package io.quarkus.jaeger.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.jaegertracing.Configuration;
import io.jaegertracing.spi.Sender;
import io.jaegertracing.thrift.internal.senders.ThriftSenderFactory;

@TargetClass(className = "io.jaegertracing.internal.senders.SenderResolver")
public final class Target_SenderResolver {

    @Substitute
    public static Sender resolve(Configuration.SenderConfiguration senderConfiguration) {
        return new ThriftSenderFactory().getSender(senderConfiguration);
    }
}
