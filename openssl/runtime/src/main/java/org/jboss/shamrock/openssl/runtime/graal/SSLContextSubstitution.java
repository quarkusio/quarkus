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

package org.jboss.shamrock.openssl.runtime.graal;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLException;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.wildfly.openssl.OpenSSLContextSPI;
import org.wildfly.openssl.OpenSSLProvider;

/**
 */
@TargetClass(SSLContext.class)
public final class SSLContextSubstitution {

    @Alias
    SSLContextSubstitution(SSLContextSpi var1, Provider var2, String var3) {
        // empty
    }

    @Substitute
    public static SSLContext getInstance(String protocol) throws NoSuchAlgorithmException {
        return (SSLContext) (Object) new SSLContextSubstitution(Utils.getSpi(protocol), OpenSSLProvider.INSTANCE, protocol);
    }

    @Substitute
    public static SSLContext getInstance(String protocol, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider.equals("OpenSSL")) {
            return getInstance(protocol);
        } else {
            throw new NoSuchProviderException(provider);
        }
    }

    @Substitute
    public static SSLContext getInstance(String protocol, Provider provider) throws NoSuchAlgorithmException {
        if (provider instanceof OpenSSLProvider) {
            return (SSLContext) (Object) new SSLContextSubstitution(Utils.getSpi(protocol), provider, protocol);
        } else {
            throw new NoSuchAlgorithmException(protocol);
        }
    }

    static final class Utils {
        static SSLContextSpi getSpi(String protocol) throws NoSuchAlgorithmException {
            Objects.requireNonNull(protocol, "null protocol name");
            try {
                if ("TLS".equals(protocol) || "Default".equals(protocol)) {
                    return new OpenSSLContextSPI.OpenSSLTLSContextSpi();
                } else if ("TLSv1.0".equals(protocol)) {
                    return new OpenSSLContextSPI.OpenSSLTLS_1_0_ContextSpi();
                } else if ("TLSv1.1".equals(protocol)) {
                    return new OpenSSLContextSPI.OpenSSLTLS_1_1_ContextSpi();
                } else if ("TLSv1.2".equals(protocol)) {
                    return new OpenSSLContextSPI.OpenSSLTLS_1_1_ContextSpi();
                } else {
                    throw new NoSuchAlgorithmException(protocol);
                }
            } catch (SSLException e) {
                throw new NoSuchAlgorithmException(e);
            }
        }
    }
}

