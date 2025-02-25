package io.quarkus.qute.i18n;

import java.io.Reader;
import java.io.StringReader;
import java.util.Optional;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.arc.WithCaching;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.Variant;
import io.quarkus.qute.runtime.MessageBundleRecorder.BundleContext;

@Singleton
public class MessageTemplateLocator implements TemplateLocator {

    @WithCaching // BundleContext is dependent
    @Inject
    Instance<BundleContext> bundleContext;

    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY - 1;
    }

    @Override
    public Optional<TemplateLocation> locate(String id) {
        if (bundleContext.isResolvable()) {
            String template = bundleContext.get().getMessageTemplates().get(id);
            if (template != null) {
                return Optional.of(new MessageTemplateLocation(template));
            }
        }
        return Optional.empty();
    }

    static final class MessageTemplateLocation implements TemplateLocation {

        private final String content;

        private MessageTemplateLocation(String content) {
            this.content = content;
        }

        @Override
        public Reader read() {
            return new StringReader(content);
        }

        @Override
        public Optional<Variant> getVariant() {
            return Optional.empty();
        }

    }

}
