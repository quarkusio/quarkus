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

        Expression addExpression(String param, String value);

    }

    /**
     * Section initialization context.
     */
    public interface SectionInitContext extends ParserDelegate {

        default public Map<String, String> getParameters() {
            return getBlocks().get(0).parameters;
        }

        default public boolean hasParameter(String name) {
            return getParameters().containsKey(name);
        }

        default public String getParameter(String name) {
            return getParameters().get(name);
        }

        public Expression getExpression(String parameterName);

        public Expression parseValue(String value);

        public List<SectionBlock> getBlocks();

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
