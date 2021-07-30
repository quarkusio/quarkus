package io.quarkus.qute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Factory to create a new {@link SectionHelper} based on the {@link SectionInitContextImpl}.
 * 
 * @see EngineBuilder#addSectionHelper(SectionHelperFactory)
 */
public interface SectionHelperFactory<T extends SectionHelper> {

    String MAIN_BLOCK_NAME = "$main";

    /**
     * 
     * @return the list of default aliases used to match the helper
     */
    default List<String> getDefaultAliases() {
        return Collections.emptyList();
    }

    /**
     * 
     * @return the info about the expected parameters
     */
    default ParametersInfo getParameters() {
        return ParametersInfo.EMPTY;
    }

    /**
     * A nested section tag that matches a name of a block will be added as a block to the current section.
     * 
     * @return the list of block labels
     */
    default List<String> getBlockLabels() {
        return Collections.emptyList();
    }

    /**
     * By default, all unknown nested sections are ignored, ie. sections with labels not present in the
     * {@link #getBlockLabels()}. However, sometimes it might be useful to treat such sections as blocks. See
     * {@link IncludeSectionHelper} for an example.
     * 
     * @return true if unknown sections should not be ignored
     */
    default boolean treatUnknownSectionsAsBlocks() {
        return false;
    }

    /**
     * 
     * @param context
     * @return a new helper instance
     */
    T initialize(SectionInitContext context);

    /**
     * Initialize a section block.
     * 
     * @return a new scope if this section introduces a new scope, or the outer scope
     * @see BlockInfo#addExpression(String, String)
     */
    default Scope initializeBlock(Scope outerScope, BlockInfo block) {
        return outerScope;
    }

    interface ParserDelegate {

        TemplateException createParserError(String message);

    }

    interface BlockInfo extends ParserDelegate {

        String getLabel();

        Map<String, String> getParameters();

        default String getParameter(String name) {
            return getParameters().get(name);
        }

        default boolean hasParameter(String name) {
            return getParameters().containsKey(name);
        }

        /**
         * Parse and register an expression for the specified parameter.
         * <p>
         * A registered expression contributes to the {@link Template#getExpressions()}, i.e. can be validated at build time.
         * 
         * @param param
         * @param value
         * @return a new expression
         * @see SectionInitContext#getExpression(String)
         */
        Expression addExpression(String param, String value);

    }

    /**
     * Section initialization context.
     */
    public interface SectionInitContext extends ParserDelegate {

        /**
         * 
         * @return the parameters of the main block
         */
        default public Map<String, String> getParameters() {
            return getBlocks().get(0).parameters;
        }

        /**
         * 
         * @return {@code true} if the main block declares a parameter of the given name
         */
        default public boolean hasParameter(String name) {
            return getParameters().containsKey(name);
        }

        /**
         * 
         * @return the parameter, or null/{@link Parameter.EMPTY} if the main block does not declare a parameter of the given
         *         name
         */
        default public String getParameter(String name) {
            return getParameters().get(name);
        }

        /**
         * 
         * @param name
         * @param defaultValue
         * @return the param or the default value if not specified
         */
        default public String getParameterOrDefault(String name, String defaultValue) {
            String param = getParameter(name);
            return param == null || Parameter.EMPTY.equals(param) ? defaultValue : param;
        }

        /**
         * Note that the expression must be registered in the {@link SectionHelperFactory#initializeBlock(Scope, BlockInfo)}
         * first.
         * 
         * @param parameterName
         * @return an expression registered for the specified param name, or {@code null}
         * @see BlockInfo#addExpression(String, String)
         */
        public Expression getExpression(String parameterName);

        /**
         * Parse the specified value. The expression is not registered in the template.
         * 
         * @param value
         * @return a new expression
         */
        public Expression parseValue(String value);

        public List<SectionBlock> getBlocks();

        /**
         * 
         * @param label
         * @return the first block with the given label, or {code null} if no such exists
         */
        default SectionBlock getBlock(String label) {
            for (SectionBlock block : getBlocks()) {
                if (label.equals(block.label)) {
                    return block;
                }
            }
            return null;
        }

        public Engine getEngine();

    }

    public static final class ParametersInfo implements Iterable<List<Parameter>> {

        public static Builder builder() {
            return new Builder();
        }

        public static final ParametersInfo EMPTY = builder().build();

        private final Map<String, List<Parameter>> parameters;

        private ParametersInfo(Map<String, List<Parameter>> parameters) {
            this.parameters = new HashMap<>(parameters);
        }

        public List<Parameter> get(String sectionPart) {
            return parameters.getOrDefault(sectionPart, Collections.emptyList());
        }

        @Override
        public Iterator<List<Parameter>> iterator() {
            return parameters.values().iterator();
        }

        public static class Builder {

            private final Map<String, List<Parameter>> parameters;

            Builder() {
                this.parameters = new HashMap<>();
            }

            public Builder addParameter(String name) {
                return addParameter(SectionHelperFactory.MAIN_BLOCK_NAME, name, null);
            }

            public Builder addParameter(String name, String defaultValue) {
                return addParameter(SectionHelperFactory.MAIN_BLOCK_NAME, name, defaultValue);
            }

            public Builder addParameter(Parameter param) {
                return addParameter(SectionHelperFactory.MAIN_BLOCK_NAME, param);
            }

            public Builder addParameter(String blockLabel, String name, String defaultValue) {
                return addParameter(blockLabel, new Parameter(name, defaultValue, false));
            }

            public Builder addParameter(String blockLabel, Parameter parameter) {
                parameters.computeIfAbsent(blockLabel, c -> new ArrayList<>()).add(parameter);
                return this;
            }

            public ParametersInfo build() {
                return new ParametersInfo(parameters);
            }
        }

    }

}
