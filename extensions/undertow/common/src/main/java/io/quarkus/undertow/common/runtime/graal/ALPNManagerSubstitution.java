package io.quarkus.undertow.common.runtime.graal;

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
