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

package io.quarkus.undertow.runtime.graal;

import javax.net.ssl.SSLEngine;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.undertow.protocols.alpn.ALPNProvider;
import io.undertow.protocols.alpn.OpenSSLAlpnProvider;

@TargetClass(className = "io.undertow.protocols.alpn.ALPNManager")
public final class ALPNManagerSubstitution {

    @Substitute
    public ALPNProvider getProvider(SSLEngine engine) {
        return new OpenSSLAlpnProvider();
    }
}
