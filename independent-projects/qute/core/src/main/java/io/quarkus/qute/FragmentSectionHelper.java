package io.quarkus.qute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import io.quarkus.qute.Template.Fragment;
import io.quarkus.qute.TemplateNode.Origin;

/**
 * Demarcates a template fragment that can be rendered separately.
 *
 * @see Template#getFragment(String)
 */
public class FragmentSectionHelper implements SectionHelper {

    private static final String ID = "id";

    // the generated id of the template that declares this fragment section
    private final String generatedTemplateId;
    private final String identifier;
    private final Expression rendered;

    FragmentSectionHelper(String identifier, Expression rendered, String generatedTemplateId) {
        this.identifier = identifier;
        this.rendered = rendered;
        this.generatedTemplateId = generatedTemplateId;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        if (isAlwaysExecuted(context)) {
            return context.execute();
        }
        if (rendered.isLiteral()) {
            return Booleans.isFalsy(rendered.getLiteral()) ? ResultNode.NOOP : context.execute();
        } else {
            return context.resolutionContext().evaluate(rendered).thenCompose(r -> {
                return Booleans.isFalsy(r) ? ResultNode.NOOP : context.execute();
            });
        }
    }

    private boolean isAlwaysExecuted(SectionResolutionContext context) {
        if (rendered == null
                // executed from an include section
                || context.getParameters().containsKey(Fragment.ATTRIBUTE)) {
            return true;
        }
        Object attribute = context.resolutionContext().getAttribute(Fragment.ATTRIBUTE);
        // the attribute is set if executed separately via Template.Fragment
        return attribute != null && attribute.equals(generatedTemplateId + identifier);
    }

    public static class Factory implements SectionHelperFactory<FragmentSectionHelper> {

        static final Pattern FRAGMENT_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

        static final String RENDERED = "rendered";
        static final String HIDDEN = "_hidden";
        static final String CAPTURE = "capture";

        private final Map<String, Map<String, Origin>> templateToFragments = new ConcurrentHashMap<>();

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of("fragment", CAPTURE);
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.builder()
                    .addParameter(ID)
                    .addParameter(Parameter.builder(RENDERED).ignoreUnnamedValues().optional().build())
                    .addParameter(Parameter.builder(HIDDEN).optional().valuePredicate(HIDDEN::equals).build())
                    .build();
        }

        @Override
        public FragmentSectionHelper initialize(SectionInitContext context) {
            String id = context.getParameter(ID);
            if (LiteralSupport.isStringLiteralSeparator(id.charAt(0))) {
                id = id.substring(1, id.length() - 1);
            }
            if (!FRAGMENT_PATTERN.matcher(id).matches()) {
                throw context.error(
                        "found an invalid fragment identifier [{id}] - an identifier can only consist of alphanumeric characters and underscores")
                        .code(Code.INVALID_FRAGMENT_ID)
                        .argument("id", id)
                        .origin(context.getOrigin())
                        .build();
            }
            String generatedId = context.getOrigin().getTemplateGeneratedId();
            Map<String, Origin> fragments = templateToFragments.get(generatedId);
            if (fragments == null) {
                // note that we don't need a concurrent map here because all fragments of a template are initialized sequentially
                // and the map is only used to validate unique fragment ids
                fragments = new HashMap<>();
                fragments.put(id, context.getOrigin());
                templateToFragments.put(generatedId, fragments);
            } else {
                Origin existing = fragments.put(id, context.getOrigin());
                if (existing != null) {
                    throw context.error(
                            "found a non-unique fragment identifier: [{id}]")
                            .code(Code.NON_UNIQUE_FRAGMENT_ID)
                            .argument("id", id)
                            .origin(context.getOrigin())
                            .build();
                }
            }
            Expression rendered = null;
            if (context.getName().equals(CAPTURE)) {
                rendered = ExpressionImpl.literalFrom(-1, "false");
            } else if (context.hasParameter(RENDERED)) {
                rendered = context.getExpression(RENDERED);
            } else if (context.hasParameter(HIDDEN)) {
                rendered = ExpressionImpl.literalFrom(-1, "false");
            }
            return new FragmentSectionHelper(id, rendered, generatedId);
        }

        @Override
        public Scope initializeBlock(Scope previousScope, BlockInfo block) {
            if (block.getLabel().equals(MAIN_BLOCK_NAME)) {
                String visible = block.getParameter(RENDERED);
                if (visible != null) {
                    block.addExpression(RENDERED, visible);
                }
            }
            return previousScope;
        }

    }

    enum Code implements ErrorCode {

        INVALID_FRAGMENT_ID,

        NON_UNIQUE_FRAGMENT_ID;

        @Override
        public String getName() {
            return "FRAGMENT_" + name();
        }

    }
}
