package io.quarkus.platform.tools.config;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoaderContext;
import io.quarkus.platform.tools.DefaultMessageWriter;
import io.quarkus.platform.tools.MessageWriter;

public class QuarkusPlatformConfig {

    public static class Builder {

        private boolean buildDefaultConfig;
        private MessageWriter log;
        private QuarkusPlatformDescriptor platformDescr;

        private Builder(boolean buildSingleton) {
            if (this.buildDefaultConfig = buildSingleton) {
                assertNoDefaultConfig();
            }
        }

        public Builder setMessageWriter(MessageWriter msgWriter) {
            this.log = msgWriter;
            return this;
        }

        private MessageWriter getMessageWriter() {
            return log == null ? log = new DefaultMessageWriter() : log;
        }

        public Builder setPlatformDescriptor(QuarkusPlatformDescriptor platformDescr) {
            this.platformDescr = platformDescr;
            return this;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private QuarkusPlatformDescriptor getPlatformDescriptor() {
            if(platformDescr != null) {
                return platformDescr;
            }

            final Iterator<QuarkusPlatformDescriptorLoader> i = ServiceLoader.load(QuarkusPlatformDescriptorLoader.class).iterator();
            if(!i.hasNext()) {
                throw new IllegalStateException("Failed to locate an implementation of " + QuarkusPlatformDescriptorLoader.class.getName() + " on the classpath");
            }
            final QuarkusPlatformDescriptorLoader<QuarkusPlatformDescriptor, QuarkusPlatformDescriptorLoaderContext> dl = i.next();
            if(i.hasNext()) {
                final StringBuilder buf = new StringBuilder();
                buf.append("Found multiple implementations of ").append(QuarkusPlatformDescriptorLoader.class.getName()).append("on the classpath: ").append(dl.getClass().getName());
                while(i.hasNext()) {
                    buf.append(", ").append(i.next().getClass().getName());
                }
                throw new IllegalStateException(buf.toString());
            }
            return platformDescr = dl.load(new QuarkusPlatformDescriptorLoaderContext() {
                @Override
                public MessageWriter getMessageWriter() {
                    return Builder.this.getMessageWriter();
                }});
        }

        public QuarkusPlatformConfig build() {
            return new QuarkusPlatformConfig(this);
        }
    }

    public static Builder builder() {
        return new Builder(false);
    }

    /**
     * This hopefully will be a temporary way of providing global default config
     * by creating a builder that will create a config instance which will serve
     * as the global default config.
     */
    public static Builder defaultConfigBuilder() {
        return new Builder(true);
    }

    public static QuarkusPlatformConfig newInstance() {
        return builder().build();
    }

    public static QuarkusPlatformConfig getGlobalDefault() {
        final QuarkusPlatformConfig c = defaultConfig.get();
        if(c != null) {
            return c;
        }
        return defaultConfigBuilder().build();
    }

    public static boolean hasGlobalDefault() {
        return defaultConfig.get() != null;
    }

    private static void assertNoDefaultConfig() {
        if (defaultConfig.get() != null) {
            throw new IllegalStateException(
                    "The default instance of " + QuarkusPlatformConfig.class.getName() + " has already been initialized");
        }
    }

    private static final AtomicReference<QuarkusPlatformConfig> defaultConfig = new AtomicReference<>();

    private final MessageWriter log;
    private final QuarkusPlatformDescriptor platformDescr;

    private QuarkusPlatformConfig(Builder builder) {
        this.log = builder.getMessageWriter();
        this.platformDescr = builder.getPlatformDescriptor();
        if(builder.buildDefaultConfig) {
            assertNoDefaultConfig();
            defaultConfig.set(this);
        }
    }

    public MessageWriter getMessageWriter() {
        return log;
    }

    public QuarkusPlatformDescriptor getPlatformDescriptor() {
        return platformDescr;
    }
}
