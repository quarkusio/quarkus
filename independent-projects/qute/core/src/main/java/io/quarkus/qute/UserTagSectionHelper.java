package io.quarkus.qute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class UserTagSectionHelper extends IncludeSectionHelper implements SectionHelper {

    private static final String NESTED_CONTENT = "nested-content";

    protected final boolean isNestedContentNeeded;

    private final HtmlEscaper htmlEscaper;

    UserTagSectionHelper(Supplier<Template> templateSupplier, Map<String, SectionBlock> extendingBlocks,
            Map<String, Expression> parameters, boolean isIsolated, boolean isNestedContentNeeded, HtmlEscaper htmlEscaper) {
        super(templateSupplier, extendingBlocks, parameters, isIsolated);
        this.isNestedContentNeeded = isNestedContentNeeded;
        this.htmlEscaper = htmlEscaper;
    }

    @Override
    protected boolean optimizeIfNoParams() {
        return false;
    }

    @Override
    protected void addAdditionalEvaluatedParams(SectionResolutionContext context, Map<String, Object> evaluatedParams) {
        evaluatedParams.put(Factory.ARGS, new Arguments(evaluatedParams, htmlEscaper));
        if (isNestedContentNeeded) {
            // If needed then add the {nested-content} to the evaluated params
            Expression nestedContent = ((TemplateImpl) template.get()).findExpression(this::isNestedContent);
            if (nestedContent != null) {
                // Execute the nested content first and make it accessible via the "nested-content" key
                evaluatedParams.put(NESTED_CONTENT,
                        context.execute(
                                context.resolutionContext().createChild(Mapper.wrap(evaluatedParams), null)));
            }
        }
    }

    private boolean isNestedContent(Expression expr) {
        return expr.getParts().size() == 1 && expr.getParts().get(0).getName().equals(NESTED_CONTENT);
    }

    public static class Factory extends IncludeSectionHelper.AbstractIncludeFactory<UserTagSectionHelper> {

        public static final String ARGS = "_args";

        private static final String IT = "it";
        // Unlike regular includes user tags are isolated by default
        private static final String ISOLATED_DEFAULT_VALUE = "true";

        private final String name;
        private final String templateId;
        private final HtmlEscaper htmlEscaper;

        /**
         *
         * @param name Identifies the tag
         * @param templateId Used to locate the template
         */
        public Factory(String name, String templateId) {
            this.name = name;
            this.templateId = templateId;
            this.htmlEscaper = new HtmlEscaper(List.of());
        }

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of(name);
        }

        @Override
        String isolatedDefaultValue() {
            return ISOLATED_DEFAULT_VALUE;
        }

        @Override
        public ParametersInfo getParameters() {
            ParametersInfo.Builder builder = ParametersInfo.builder().addParameter(Parameter.builder(IT).defaultValue(IT));
            addDefaultParams(builder);
            return builder.build();
        }

        @Override
        protected boolean ignoreParameterInit(String key, String value) {
            // {#myTag _isolated=true /}
            return super.ignoreParameterInit(key, value) || (key.equals(ISOLATED)
                    // {#myTag _isolated /}
                    || value.equals(ISOLATED)
                    // {#myTag _unisolated /}
                    || value.equals(UNISOLATED)
                    // IT with default value, e.g. {#myTag foo=bar /}
                    || (key.equals(IT) && value.equals(IT)));
        }

        @Override
        protected String getTemplateId(SectionInitContext context) {
            return templateId;
        }

        @Override
        protected UserTagSectionHelper newHelper(Supplier<Template> template, Map<String, Expression> params,
                Map<String, SectionBlock> extendingBlocks, Boolean isolatedValue, SectionInitContext context) {
            boolean isNestedContentNeeded = !context.getBlock(SectionHelperFactory.MAIN_BLOCK_NAME).isEmpty();
            return new UserTagSectionHelper(template, extendingBlocks, params,
                    isolatedValue != null ? isolatedValue
                            : Boolean.parseBoolean(context.getParameterOrDefault(ISOLATED, ISOLATED_DEFAULT_VALUE)),
                    isNestedContentNeeded, htmlEscaper);
        }

        @Override
        protected void handleParamInit(String key, String value, SectionInitContext context, Map<String, Expression> params) {
            if (key.equals(IT)) {
                if (value.equals(IT)) {
                    return;
                } else if (isSinglePart(value)) {
                    // Also register the param with a defaulted key
                    params.put(value, context.getExpression(key));
                }
            }
            super.handleParamInit(key, value, context, params);
        }

    }

    public static class Arguments implements Iterable<Entry<String, Object>> {

        private final List<Entry<String, Object>> args;
        private final HtmlEscaper htmlEscaper;

        Arguments(Map<String, Object> map, HtmlEscaper htmlEscaper) {
            this.args = new ArrayList<>(Objects.requireNonNull(map).size());
            map.entrySet().forEach(args::add);
            // sort by key
            this.args.sort(Comparator.comparing(Entry::getKey));
            this.htmlEscaper = htmlEscaper;
        }

        private Arguments(List<Entry<String, Object>> args, HtmlEscaper htmlEscaper) {
            this.args = args;
            this.htmlEscaper = htmlEscaper;
        }

        public boolean isEmpty() {
            return args.isEmpty();
        }

        public int size() {
            return args.size();
        }

        public Object get(String key) {
            for (Entry<String, Object> e : args) {
                if (e.getKey().equals(key)) {
                    return e.getValue();
                }
            }
            return null;
        }

        @Override
        public Iterator<Entry<String, Object>> iterator() {
            return args.iterator();
        }

        public Arguments skip(String... keys) {
            Set<String> keySet = Set.of(keys);
            List<Entry<String, Object>> newArgs = new ArrayList<>(args.size());
            for (Entry<String, Object> e : args) {
                if (!keySet.contains(e.getKey())) {
                    newArgs.add(e);
                }
            }
            return new Arguments(newArgs, htmlEscaper);
        }

        public Arguments filter(String... keys) {
            Set<String> keySet = Set.of(keys);
            List<Entry<String, Object>> newArgs = new ArrayList<>(args.size());
            for (Entry<String, Object> e : args) {
                if (keySet.contains(e.getKey())) {
                    newArgs.add(e);
                }
            }
            return new Arguments(newArgs, htmlEscaper);
        }

        // foo="1" bar="true"
        public RawString asHtmlAttributes() {
            StringBuilder builder = new StringBuilder();
            for (Iterator<Entry<String, Object>> it = args.iterator(); it.hasNext();) {
                Entry<String, Object> e = it.next();
                builder.append(e.getKey());
                builder.append("=\"");
                builder.append(htmlEscaper.escape(String.valueOf(e.getValue())));
                builder.append("\"");
                if (it.hasNext()) {
                    builder.append(" ");
                }
            }
            return new RawString(builder.toString());
        }

    }

}
