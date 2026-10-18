package io.quarkus.vertx.http.runtime;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;
import static io.quarkus.vertx.http.runtime.TrustedProxyCheck.createNewIpCheck;

import java.net.InetAddress;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.runtime.configuration.CidrAddressConverter;
import io.quarkus.runtime.configuration.InetSocketAddressConverter;

/**
 * Converts proxy address into {@link TrustedProxyCheck.TrustedProxyCheckPart}.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public final class TrustedProxyCheckPartConverter implements Converter<TrustedProxyCheck.TrustedProxyCheckPart> {

    private static final Pattern CIDR_PATTERN = Pattern.compile(".+\\/\\d+");

    @Override
    public TrustedProxyCheck.TrustedProxyCheckPart convert(String proxyAddress) {
        if (CIDR_PATTERN.matcher(proxyAddress).matches()) {
            final var cidrAddress = new CidrAddressConverter().convert(proxyAddress);
            return new TrustedProxyCheck.TrustedProxyCheckPart(new BiPredicate<>() {
                @Override
                public boolean test(InetAddress proxyIP, Integer proxyPort) {
                    return cidrAddress.matches(proxyIP);
                }
            });
        } else {
            if ("localhost".equals(proxyAddress)) {
                // prevents unnecessary localhost lookup, also default DNS does not know localhost
                proxyAddress = "127.0.0.1";
            }
            final var inetSocketAddress = new InetSocketAddressConverter().convert(proxyAddress);
            final boolean useHostName = inetSocketAddress.isUnresolved() || inetSocketAddress.getAddress() == null;
            if (useHostName) {
                return new TrustedProxyCheck.TrustedProxyCheckPart(inetSocketAddress.getHostName(),
                        inetSocketAddress.getPort());
            } else {
                return new TrustedProxyCheck.TrustedProxyCheckPart(
                        createNewIpCheck(inetSocketAddress.getAddress(), inetSocketAddress.getPort()));
            }
        }
    }

}
