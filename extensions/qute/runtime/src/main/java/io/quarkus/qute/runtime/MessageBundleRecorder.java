package io.quarkus.qute.runtime;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MessageBundleRecorder {

    public static class MessageInfo {

        public final String source;
        public final String content;

        public MessageInfo(String source, String content) {
            this.source = source;
            this.content = content;
        }

        public URI parseSource() {
            if (source != null) {
                try {
                    return new URI(source);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return "MessageInfo [source=" + source + ", content=" + content + "]";
        }
    }

    public Supplier<Object> createContext(Map<String, MessageInfo> messageTemplates,
            Map<String, Map<String, Class<?>>> bundleInterfaces) {
        return new Supplier<Object>() {

            @Override
            public Object get() {
                return new BundleContext() {

                    @Override
                    public Map<String, MessageInfo> getMessageTemplates() {
                        return messageTemplates;
                    }

                    @Override
                    public Map<String, Map<String, Class<?>>> getBundleInterfaces() {
                        return bundleInterfaces;
                    }
                };
            }
        };
    }

    public interface BundleContext {

        // message id -> message template
        Map<String, MessageInfo> getMessageTemplates();

        // bundle name -> (locale -> interface class)
        Map<String, Map<String, Class<?>>> getBundleInterfaces();

    }
}
