package io.quarkus.qute.i18n;

import java.util.Optional;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.arc.WithCaching;
import io.quarkus.qute.StringTemplateLocation;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.runtime.MessageBundleRecorder.BundleContext;
import io.quarkus.qute.runtime.MessageBundleRecorder.MessageInfo;

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
            MessageInfo messageInfo = bundleContext.get().getMessageTemplates().get(id);
            if (messageInfo != null) {
                return Optional.of(new StringTemplateLocation(messageInfo.content, Optional.empty(),
                        Optional.ofNullable(messageInfo.parseSource())));
            }
        }
        return Optional.empty();
    }

}
