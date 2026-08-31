package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.OptionalInt;

import io.netty.util.AsciiString;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.XForwardedForSelector;
import io.quarkus.vertx.http.runtime.ProxyConfig.ForwardedPrecedence;

public class ForwardingProxyOptions {
    public final boolean proxyAddressForwarding;
    final boolean validateForwardedProto;
    final boolean allowForwarded;
    final boolean allowXForwarded;
    final boolean enableForwardedHost;
    final boolean enableForwardedPrefix;
    final AsciiString forwardedHostHeader;
    final AsciiString forwardedPrefixHeader;
    final boolean strictForwardedControl;
    final ForwardedPrecedence forwardedPrecedence;
    final boolean enableTrustedProxyHeader;
    final XForwardedForSelector xForwardedForSelector;

    private ForwardingProxyOptions(Builder builder) {
        this.proxyAddressForwarding = builder.proxyAddressForwarding;
        this.validateForwardedProto = builder.validateForwardedProto;
        this.allowForwarded = builder.allowForwarded;
        this.allowXForwarded = builder.allowXForwarded;
        this.enableForwardedHost = builder.enableForwardedHost;
        this.enableForwardedPrefix = builder.enableForwardedPrefix;
        this.forwardedHostHeader = builder.forwardedHostHeader;
        this.forwardedPrefixHeader = builder.forwardedPrefixHeader;
        this.strictForwardedControl = builder.strictForwardedControl;
        this.forwardedPrecedence = builder.forwardedPrecedence;
        this.enableTrustedProxyHeader = builder.enableTrustedProxyHeader;
        this.xForwardedForSelector = builder.xForwardedForSelector;
    }

    static Builder builder() {
        return new Builder();
    }

    static ForwardingProxyOptions from(ProxyConfig proxyConfig, String configPrefix) {
        final boolean allowForwarded = proxyConfig.allowForwarded();
        final boolean allowXForwarded = proxyConfig.allowXForwarded().orElse(!allowForwarded);

        return builder()
                .proxyAddressForwarding(proxyConfig.proxyAddressForwarding())
                .validateForwardedProto(
                        proxyConfig.forwardedProtoValidation() == ProxyConfig.ForwardedProtoValidation.REJECT)
                .allowForwarded(allowForwarded)
                .allowXForwarded(allowXForwarded)
                .enableForwardedHost(proxyConfig.enableForwardedHost())
                .enableForwardedPrefix(proxyConfig.enableForwardedPrefix())
                .enableTrustedProxyHeader(proxyConfig.enableTrustedProxyHeader())
                .strictForwardedControl(proxyConfig.strictForwardedControl())
                .forwardedPrecedence(proxyConfig.forwardedPrecedence())
                .forwardedHostHeader(AsciiString.cached(proxyConfig.forwardedHostHeader()))
                .forwardedPrefixHeader(AsciiString.cached(proxyConfig.forwardedPrefixHeader()))
                .xForwardedForSelector(resolveXForwardedForSelector(proxyConfig, configPrefix, allowForwarded))
                .build();
    }

    private static XForwardedForSelector resolveXForwardedForSelector(ProxyConfig proxyConfig, String configPrefix,
            boolean allowForwarded) {
        var selectorBeans = Arc.requireContainer().select(XForwardedForSelector.class);
        if (selectorBeans.isAmbiguous()) {
            throw new ConfigurationException(
                    "Only one '" + XForwardedForSelector.class.getName() + "' CDI bean is allowed");
        }
        boolean hasSelectorBean = !selectorBeans.isUnsatisfied();
        String selectorBeanClass = hasSelectorBean ? selectorBeans.getHandle().getBean().getBeanClass().getName() : null;

        OptionalInt index = proxyConfig.xForwardedForIndex();
        if (index.isPresent()) {
            int selectedIndex = index.getAsInt();
            if (selectedIndex < 1) {
                throw new ConfigurationException(
                        "'" + configPrefix + ".proxy.x-forwarded-for-index' must be greater than 0");
            }
            if (allowForwarded) {
                throw new ConfigurationException("'" + configPrefix + ".proxy.x-forwarded-for-index' and '"
                        + configPrefix + ".proxy.allow-forwarded' are mutually exclusive");
            }
            if (hasSelectorBean) {
                throw new ConfigurationException("'" + configPrefix + ".proxy.x-forwarded-for-index' and the '"
                        + selectorBeanClass + "' CDI bean are mutually exclusive");
            }
            return context -> {
                List<String> values = context.xForwardedForValues();
                int position = values.size() - selectedIndex;
                return position >= 0 ? values.get(position) : null;
            };
        }

        if (hasSelectorBean) {
            if (allowForwarded) {
                throw new ConfigurationException("The '" + selectorBeanClass + "' CDI bean and '"
                        + configPrefix + ".proxy.allow-forwarded' are mutually exclusive");
            }
            return selectorBeans.get();
        }

        return null;
    }

    static final class Builder {

        private boolean proxyAddressForwarding;
        private boolean validateForwardedProto;
        private boolean allowForwarded;
        private boolean allowXForwarded;
        private boolean enableForwardedHost;
        private boolean enableForwardedPrefix;
        private AsciiString forwardedHostHeader;
        private AsciiString forwardedPrefixHeader;
        private boolean strictForwardedControl;
        private ForwardedPrecedence forwardedPrecedence;
        private boolean enableTrustedProxyHeader;
        private XForwardedForSelector xForwardedForSelector;

        private Builder() {
        }

        Builder proxyAddressForwarding(boolean proxyAddressForwarding) {
            this.proxyAddressForwarding = proxyAddressForwarding;
            return this;
        }

        Builder validateForwardedProto(boolean validateForwardedProto) {
            this.validateForwardedProto = validateForwardedProto;
            return this;
        }

        Builder allowForwarded(boolean allowForwarded) {
            this.allowForwarded = allowForwarded;
            return this;
        }

        Builder allowXForwarded(boolean allowXForwarded) {
            this.allowXForwarded = allowXForwarded;
            return this;
        }

        Builder enableForwardedHost(boolean enableForwardedHost) {
            this.enableForwardedHost = enableForwardedHost;
            return this;
        }

        Builder enableForwardedPrefix(boolean enableForwardedPrefix) {
            this.enableForwardedPrefix = enableForwardedPrefix;
            return this;
        }

        Builder forwardedHostHeader(AsciiString forwardedHostHeader) {
            this.forwardedHostHeader = forwardedHostHeader;
            return this;
        }

        Builder forwardedPrefixHeader(AsciiString forwardedPrefixHeader) {
            this.forwardedPrefixHeader = forwardedPrefixHeader;
            return this;
        }

        Builder strictForwardedControl(boolean strictForwardedControl) {
            this.strictForwardedControl = strictForwardedControl;
            return this;
        }

        Builder forwardedPrecedence(ForwardedPrecedence forwardedPrecedence) {
            this.forwardedPrecedence = forwardedPrecedence;
            return this;
        }

        Builder enableTrustedProxyHeader(boolean enableTrustedProxyHeader) {
            this.enableTrustedProxyHeader = enableTrustedProxyHeader;
            return this;
        }

        Builder xForwardedForSelector(XForwardedForSelector xForwardedForSelector) {
            this.xForwardedForSelector = xForwardedForSelector;
            return this;
        }

        ForwardingProxyOptions build() {
            return new ForwardingProxyOptions(this);
        }
    }
}
