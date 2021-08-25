package org.jboss.resteasy.reactive.server.core.multipart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

/**
 * Factory class that can create a form data parser for a given request.
 * <p>
 * It does this by iterating the available parser definitions, and returning
 * the first parser that is created.
 *
 * @author Stuart Douglas
 */
public class FormParserFactory {

    private final ParserDefinition[] parserDefinitions;

    FormParserFactory(final List<ParserDefinition> parserDefinitions) {
        this.parserDefinitions = parserDefinitions.toArray(new ParserDefinition[parserDefinitions.size()]);
    }

    /**
     * Creates a form data parser for this request.
     *
     * @param exchange The exchange
     * @return A form data parser, or null if there is no parser registered for the request content type
     */
    public FormDataParser createParser(final ResteasyReactiveRequestContext exchange) {
        for (int i = 0; i < parserDefinitions.length; ++i) {
            FormDataParser parser = parserDefinitions[i].create(exchange);
            if (parser != null) {
                return parser;
            }
        }
        return null;
    }

    public interface ParserDefinition<T> {

        FormDataParser create(final ResteasyReactiveRequestContext exchange);

        T setDefaultCharset(String charset);
    }

    public static Builder builder(Supplier<Executor> executorSupplier) {
        return builder(true, executorSupplier);
    }

    public static Builder builder(boolean includeDefault, Supplier<Executor> executorSupplier) {
        Builder builder = new Builder();
        if (includeDefault) {
            builder.addParsers(new FormEncodedDataDefinition(), new MultiPartParserDefinition(executorSupplier));
        }
        return builder;
    }

    public static class Builder {

        private List<ParserDefinition> parsers = new ArrayList<>();

        private String defaultCharset = null;

        public Builder addParser(final ParserDefinition definition) {
            parsers.add(definition);
            return this;
        }

        public Builder addParsers(final ParserDefinition... definition) {
            parsers.addAll(Arrays.asList(definition));
            return this;
        }

        public Builder addParsers(final List<ParserDefinition> definition) {
            parsers.addAll(definition);
            return this;
        }

        public List<ParserDefinition> getParsers() {
            return parsers;
        }

        public void setParsers(List<ParserDefinition> parsers) {
            this.parsers = parsers;
        }

        /**
         * A chainable version of {@link #setParsers}.
         */
        public Builder withParsers(List<ParserDefinition> parsers) {
            setParsers(parsers);
            return this;
        }

        public String getDefaultCharset() {
            return defaultCharset;
        }

        public void setDefaultCharset(String defaultCharset) {
            this.defaultCharset = defaultCharset;
        }

        /**
         * A chainable version of {@link #setDefaultCharset}.
         */
        public Builder withDefaultCharset(String defaultCharset) {
            setDefaultCharset(defaultCharset);
            return this;
        }

        public FormParserFactory build() {
            if (defaultCharset != null) {
                for (ParserDefinition parser : parsers) {
                    parser.setDefaultCharset(defaultCharset);
                }
            }
            return new FormParserFactory(parsers);
        }

    }

}
