package io.quarkus.qute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.qute.TemplateNode.Origin;

/**
 * Factory to create a new {@link SectionHelper} based on the {@link SectionInitContextImpl}.
 *
 * @see EngineBuilder#addSectionHelper(SectionHelperFactory)
 */
public interface SectionHelperFactory<T extends SectionHelper> {

    // The validation of expressions with the metadata hint may be relaxed in some cases
    public static final String HINT_METADATA = "<metadata>";

    String MAIN_BLOCK_NAME = "$main";

    /**
     *
     * @return the list of default aliases used to match the helper
     * @see #cacheFactoryConfig()
     */
    default List<String> getDefaultAliases() {
        return Collections.emptyList();
    }

    /**
     * A factory may define {@code factory parameters} for the start tag of any section block. A factory {@link Parameter} has a
     * name and optional default value. The default value is automatically assigned if no other value is set by a parser. A
     * parameter may be optional. A non-optional parameter that has no value assigned results in a parser error.
     * <p>
     * A section block in a template defines the {@code actual parameters}:
     *
     * <pre>
     * {! The value is "item.isActive". The name is not defined. !}
     * {#if item.isActive}{/}
     *
     * {! The name is "age" and the value is "10". !}
     * {#let age=10}{/}
     * </pre>
     *
     * The actual parameters are parsed taking the factory parameters into account:
     * <ol>
     * <li>Named actual params are processed first and the relevant values are assigned, e.g. the param with name {@code age}
     * has the
     * value {@code 10},</li>
     * <li>Then, if the number of actual params is greater or equals to the number of factory params the values are set
     * according to position of factory params,</li>
     * <li>Otherwise, the values are set according to position but params with no default value take precedence.</li>
     * <li>Finally, all unset parameters that define a default value are initialized with the default value.</li>
     * </ol>
     *
     * @return the factory parameters
     * @see #cacheFactoryConfig()
     * @see BlockInfo#getParameters()
     */
    default ParametersInfo getParameters() {
        return ParametersInfo.EMPTY;
    }

    /**
     * A nested section tag that matches a name of a block will be added as a block to the current section.
     *
     * @return the list of block labels
     * @see #cacheFactoryConfig()
     */
    default List<String> getBlockLabels() {
        return Collections.emptyList();
    }

    /**
     * If the return value is {@code true} then {@link #getDefaultAliases()}, {@link #getParameters()} and
     * {@link #getBlockLabels()} methods are called exactly once and the results are cached when the factory is being
     * registered.
     *
     * @return {@code true} the config should be cached, {@code false} otherwise
     */
    default boolean cacheFactoryConfig() {
        return true;
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
     * Initialize a new helper instance for a specific section node in a template.
     *
     * @param context
     * @return a new helper instance
     */
    T initialize(SectionInitContext context);

    /**
     * Initialize a section block.
     * <p>
     * All section blocks are initialized before {@link #initialize(SectionInitContext)} is called.
     * <p>
     * The factory is responsible to register all expression via {@link BlockInfo#addExpression(String, String)}. The expression
     * can be then used during {@link #initialize(SectionInitContext)} via {@link SectionInitContext#getExpression(String)} and
     * {@link SectionBlock#expressions}.
     *
     * @return a new scope if this section introduces a new scope, or the outer scope
     * @see BlockInfo#addExpression(String, String)
     */
    default Scope initializeBlock(Scope outerScope, BlockInfo block) {
        return outerScope;
    }

    /**
     * A section end tag may be mandatory or optional.
     *
     * @return the strategy
     */
    default MissingEndTagStrategy missingEndTagStrategy() {
        return MissingEndTagStrategy.ERROR;
    }

    /**
     * This strategy is used when an unterminated section is detected during parsing.
     */
    public enum MissingEndTagStrategy {

        /**
         * The end tag is mandatory. A missing end tag results in a parser error.
         */
        ERROR,

        /**
         * The end tag is optional. The section ends where the parent section ends.
         */
        BIND_TO_PARENT;
    }

    interface ParserDelegate extends ErrorInitializer {

        default TemplateException createParserError(String message) {
            return error(message).build();
        }

    }

    interface BlockInfo extends ParserDelegate, WithOrigin {

        String getLabel();

        /**
         * An unmodifiable ordered map of parsed parameters.
         * <p>
         * Note that the order does not necessary reflect the original positions of the parameters but the parsing order.
         *
         * @return the map of parameters
         * @see SectionHelperFactory#getParameters()
         */
        Map<String, String> getParameters();

        default String getParameter(String name) {
            return getParameters().get(name);
        }

        default boolean hasParameter(String name) {
            return getParameters().containsKey(name);
        }

        /**
         *
         * @param position
         * @return the parameter for the specified position, or {@code null} if no such parameter exists
         * @see SectionBlock#getParameter(int)
         */
        String getParameter(int position);

        /**
         * Parse and register an expression for the specified parameter.
         * <p>
         * A registered expression contributes to the {@link Template#getExpressions()}, i.e. can be validated at build time.
         * <p>
         * The origin of the returned expression is the origin of the containing block.
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
         * @see SectionBlock#parameters
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
         * @return the parameter for the specified position
         * @see SectionBlock#getParameter(int)
         */
        default public String getParameter(int position) {
            return getBlocks().get(0).getParameter(position);
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
         * @return the expression registered for the main block under the specified param name, or {@code null}
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

        /**
         *
         * @return the engine
         */
        public Engine getEngine();

        /**
         *
         * @return the origin of the section start tag
         */
        public default Origin getOrigin() {
            return getBlocks().get(0).origin;
        }

        /**
         * Note that the returned supplier may only be used after the template is parsed, e.g. during the invocation of
         * {@link SectionHelper#resolve(io.quarkus.qute.SectionHelper.SectionResolutionContext)}.
         *
         * @return the current template
         */
        Supplier<Template> getCurrentTemplate();

    }

    /**
     *
     * @see Parameter
     */
    public static final class ParametersInfo implements Iterable<List<Parameter>> {

        public static Builder builder() {
            return new Builder();
        }

        public static final ParametersInfo EMPTY = builder().build();

        private final Map<String, List<Parameter>> parameters;
        private final boolean checkNumberOfParams;

        private ParametersInfo(Map<String, List<Parameter>> parameters, boolean checkNumberOfParams) {
            this.parameters = new HashMap<>(parameters);
            this.checkNumberOfParams = checkNumberOfParams;
        }

        public List<Parameter> get(String blockLabel) {
            return parameters.getOrDefault(blockLabel, Collections.emptyList());
        }

        @Override
        public Iterator<List<Parameter>> iterator() {
            return parameters.values().iterator();
        }

        public boolean isCheckNumberOfParams() {
            return checkNumberOfParams;
        }

        public static class Builder {

            private final Map<String, List<Parameter>> parameters;
            private boolean checkNumberOfParams;

            Builder() {
                this.parameters = new HashMap<>();
                this.checkNumberOfParams = true;
            }

            public Builder addParameter(String name) {
                return addParameter(Parameter.builder(name));
            }

            public Builder addParameter(String name, String defaultValue) {
                return addParameter(Parameter.builder(name).defaultValue(defaultValue));
            }

            public Builder addParameter(Parameter.Builder param) {
                return addParameter(param.build());
            }

            public Builder addParameter(Parameter param) {
                return addParameter(SectionHelperFactory.MAIN_BLOCK_NAME, param);
            }

            public Builder addParameter(String blockLabel, String name, String defaultValue) {
                return addParameter(blockLabel, Parameter.builder(name).defaultValue(defaultValue));
            }

            public Builder addParameter(String blockLabel, Parameter.Builder parameter) {
                return addParameter(blockLabel, parameter.build());
            }

            public Builder addParameter(String blockLabel, Parameter parameter) {
                parameters.computeIfAbsent(blockLabel, c -> new ArrayList<>()).add(parameter);
                return this;
            }

            public Builder checkNumberOfParams(boolean value) {
                this.checkNumberOfParams = value;
                return this;
            }

            public ParametersInfo build() {
                return new ParametersInfo(parameters, checkNumberOfParams);
            }
        }

    }

}
