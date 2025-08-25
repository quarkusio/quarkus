package io.quarkus.qute;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.quarkus.qute.Qute.IndexedArgumentsParserHook;
import io.quarkus.qute.TemplateNode.Origin;

public class TemplateException extends RuntimeException {

    /**
     * Note that a qute template can be used to build the exception message.
     *
     * @return a convenient builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private static final long serialVersionUID = 1336799943548973690L;

    // We use a dedicated qute engine instance
    private static Engine engine = Engine.builder()
            .addDefaults()
            .addValueResolver(new ReflectionValueResolver())
            // Make sure that {origin.hasNonGeneratedTemplateId} works in native mode too
            .addValueResolver(ValueResolver.builder().applyToBaseClass(Origin.class).applyToName("hasNonGeneratedTemplateId")
                    .resolveSync(ec -> ((Origin) ec.getBase()).hasNonGeneratedTemplateId()).build())
            .addParserHook(new IndexedArgumentsParserHook(null))
            .build();

    private final Origin origin;
    private final ErrorCode code;
    private final Map<String, Object> arguments;
    private final String messageTemplate;

    public TemplateException(Throwable cause) {
        this(null, null, null, null, cause);
    }

    public TemplateException(String message) {
        this(null, null, message, null, null);
    }

    // kept for backward compatibility
    public TemplateException(Origin origin, String message) {
        this(null, origin, message, null, null);
    }

    public TemplateException(ErrorCode code, Origin origin, String messageTemplate, Map<String, Object> arguments,
            Throwable cause) {
        super(toMessage(messageTemplate, arguments, origin), cause);
        this.origin = origin;
        this.code = code;
        this.messageTemplate = messageTemplate;
        this.arguments = arguments;
    }

    public Origin getOrigin() {
        return origin;
    }

    public ErrorCode getCode() {
        return code;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public Optional<String> getCodeName() {
        return code == null ? Optional.empty() : Optional.ofNullable(code.getName());
    }

    private static String toMessage(String messageTemplate, Map<String, Object> arguments, Origin origin) {
        if (messageTemplate == null || messageTemplate.isBlank()) {
            return null;
        }
        if ((arguments == null || arguments.isEmpty())
                && origin == null) {
            return messageTemplate;
        }
        try {
            TemplateInstance template = engine.parse(messageTemplate).instance();
            if (arguments != null) {
                arguments.forEach(template::data);
            }
            return template.data("origin", origin).render();
        } catch (Throwable t) {
            return "Cannot render error message: " + t;
        }
    }

    public static class Builder {

        private String message;
        private Throwable cause;
        private Origin origin;
        private ErrorCode code;
        private final Map<String, Object> arguments = new HashMap<>();

        /**
         * The message can be a qute template.
         *
         * @param message
         * @return self
         * @see #argument(String, Object)
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        /**
         * If set then the origin key can be used in the message template. For example <code>Some error {origin}</code> will be
         * rendered as <code>Somer error template [foo.html:1]</code>.
         *
         * @param origin
         * @return self
         */
        public Builder origin(Origin origin) {
            this.origin = origin;
            return this;
        }

        public Builder code(ErrorCode code) {
            this.code = code;
            return this;
        }

        /**
         * The argument can be used in a message template set via {@link #message(String)}.
         *
         * @param key
         * @param value
         * @return self
         */
        public Builder argument(String key, Object value) {
            this.arguments.put(key, value);
            return this;
        }

        public Builder arguments(Map<String, Object> arguments) {
            this.arguments.putAll(arguments);
            return this;
        }

        /**
         * Every empty expression <code>{}</code> in the message template is a placeholder that is replaced with an index-based
         * array accessor
         * <code>{data[n]}</code> where {@code n} is the index of the placeholder. The first placeholder is replace with
         * <code>{data[0]}</code>, the second with <code>{data[1]}</code>, and so on. For example, <code>"Hello {}!"</code>
         * becomes <code>Hello {data[0]}!</code>.
         *
         * @param arguments
         * @return self
         */
        public Builder arguments(Object... arguments) {
            return argument("data", arguments);
        }

        public TemplateException build() {
            return new TemplateException(code, origin, message, arguments, cause);
        }

    }

}
