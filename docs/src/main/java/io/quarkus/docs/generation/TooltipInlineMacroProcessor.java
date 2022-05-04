package io.quarkus.docs.generation;

import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Name;

/**
 *
 * Tooltip inline macro implementation for PDF (HTML) files where tooltip is not supported.
 * Enum constant name is wrapped in `<code></code>` and constant description is ignored.
 */
@Name("tooltip")
public class TooltipInlineMacroProcessor extends InlineMacroProcessor {

    @Override
    public Object process(ContentNode contentNode, String target, Map<String, Object> map) {
        var attributes = new HashMap<String, Object>();
        attributes.put("subs", ":normal");
        return createPhraseNode(contentNode, "quoted", String.format("`%s`", target), attributes);
    }
}
