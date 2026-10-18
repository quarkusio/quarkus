package io.quarkus.docs.generation;

import java.util.Map;

import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.StructuralNode;
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
    public PhraseNode process(StructuralNode parent, String target, Map<String, Object> attributes) {
        return createPhraseNode(parent, "quoted", String.format("`%s`", target), Map.of("subs", ":normal"));
    }
}
