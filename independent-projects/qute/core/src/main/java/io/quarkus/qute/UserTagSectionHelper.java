package io.quarkus.qute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class UserTagSectionHelper extends IncludeSectionHelper implements SectionHelper {

    private static final String NESTED_CONTENT = "nested-content";

    protected final boolean isNestedContentNeeded;
    private final HtmlEscaper htmlEscaper;
    private final String itKey;

    UserTagSectionHelper(Supplier<Template> templateSupplier, Map<String, SectionBlock> extendingBlocks,
            Map<String, Expression> parameters, boolean isIsolated, boolean isNestedContentNeeded, HtmlEscaper htmlEscaper,
            String itKey) {
        super(templateSupplier, extendingBlocks, parameters, isIsolated);
        this.isNestedContentNeeded = isNestedContentNeeded;
        this.htmlEscaper = htmlEscaper;
        this.itKey = itKey;
    }

    @Override
    protected boolean optimizeIfNoParams() {
        return false;
    }

    @Override
    protected void addAdditionalEvaluatedParams(SectionResolutionContext context, Map<String, Object> evaluatedParams) {
        evaluatedParams.put(Factory.ARGS, new Arguments(evaluatedParams));
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
        protected boolean ignoreParameterInit(Supplier<String> firstParamSupplier, String key, String value) {
            // {#myTag _isolated=true /}
            return super.ignoreParameterInit(firstParamSupplier, key, value)
                    || (key.equals(ISOLATED)
                            // {#myTag _isolated /}
                            || value.equals(ISOLATED)
                            // {#myTag _unisolated /}
                            || value.equals(UNISOLATED)
                            // IT with default value or not the first agrument
                            // e.g. it=it in {#myTag foo=bar /} or baz in {#myTag foo=bar baz /}
                            || (key.equals(IT) && (!firstParamSupplier.get().equals(value) || value.equals(IT))));
        }

        @Override
        protected String getTemplateId(SectionInitContext context) {
            return templateId;
        }

        @Override
        protected UserTagSectionHelper newHelper(Supplier<Template> template, Map<String, Expression> params,
                Map<String, SectionBlock> extendingBlocks, Boolean isolatedValue, SectionInitContext context) {
            boolean isNestedContentNeeded = !context.getBlock(SectionHelperFactory.MAIN_BLOCK_NAME).isEmpty();
            // Use the filtered map of paramas and not the original map from the SectionInitContext
            Expression itKeyExpr = params.getOrDefault(IT, null);

            return new UserTagSectionHelper(template, extendingBlocks, params,
                    isolatedValue != null ? isolatedValue
                            : Boolean.parseBoolean(context.getParameterOrDefault(ISOLATED, ISOLATED_DEFAULT_VALUE)),
                    isNestedContentNeeded, htmlEscaper,
                    itKeyExpr != null ? stripQuotationMarks(itKeyExpr.toOriginalString()) : null);
        }

        @Override
        protected void handleParam(String key, String value, Supplier<String> firstParamSupplier,
                BiConsumer<String, String> paramConsumer) {
            if (key.equals(IT)) {
                if (value.equals(IT)) {
                    return;
                } else if (isSinglePart(value)) {
                    // Also register the param expression with the defaulted key
                    // {#include "foo" /} => {#include foo="foo" /}
                    String defaultedKey = stripQuotationMarks(value);
                    paramConsumer.accept(defaultedKey, value);
                }
            }
            super.handleParam(key, value, firstParamSupplier, paramConsumer);
        }

    }

    private static String stripQuotationMarks(String value) {
        if (LiteralSupport.isStringLiteral(value)) {
            // {#include "foo" /} => {#include foo="foo" /}
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    public class Arguments implements Iterable<Entry<String, Object>> {

        private final List<Entry<String, Object>> args;

        Arguments(Map<String, Object> map) {
            this.args = new ArrayList<>(Objects.requireNonNull(map).size());
            for (Entry<String, Object> e : map.entrySet()) {
                // Always skip the first unnamed parameter
                if (!e.getKey().equals(UserTagSectionHelper.Factory.IT)) {
                    args.add(e);
                }
            }
            // Sort by key
            this.args.sort(Comparator.comparing(Entry::getKey));
        }

        private Arguments(List<Entry<String, Object>> args, HtmlEscaper htmlEscaper) {
            this.args = args;
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
            return doFilter(e -> !keySet.contains(e.getKey()));
        }

        public Arguments skipIdenticalKeyValue() {
            return doFilter(e -> !e.getKey().equals(e.getValue()));
        }

        /**
         * Skip the first argument if it does not define a name.
         */
        public Arguments skipIt() {
            return doFilter(e -> !e.getKey().equals(itKey));
        }

        public Arguments filter(String... keys) {
            Set<String> keySet = Set.of(keys);
            return doFilter(e -> keySet.contains(e.getKey()));
        }

        public Arguments filterIdenticalKeyValue() {
            return doFilter(e -> e.getKey().equals(e.getValue()));
        }

        // foo="1" bar="true" readonly="readonly"
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

        private Arguments doFilter(Predicate<Entry<String, Object>> predicate) {
            List<Entry<String, Object>> newArgs = new ArrayList<>(args.size());
            for (Entry<String, Object> e : args) {
                if (predicate.test(e)) {
                    newArgs.add(e);
                }
            }
            return new Arguments(newArgs, htmlEscaper);
        }

    }

}
