package io.quarkus.qute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class TemplateNodesTest {

    @Test
    public void testGetNodes() {
        Engine engine = Engine.builder()
                .addDefaults()
                .build();
        Template t1 = engine.parse("Hello\nworld!{#if true}next{level}{/}");
        List<TemplateNode> rootNodes = t1.getNodes();
        assertEquals(2, rootNodes.size());
        assertTrue(rootNodes.get(0).isText());
        assertTrue(rootNodes.get(1).isSection());
        SectionNode iftrue = rootNodes.get(1).asSection();
        assertEquals(1, iftrue.getBlocks().size());
        assertEquals(SectionHelperFactory.MAIN_BLOCK_NAME, iftrue.getBlocks().get(0).label);
        assertTrue(iftrue.getBlocks().get(0).nodes.get(0).isText());
    }

    @Test
    public void testFindNodes() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addSectionHelper(new UserTagSectionHelper.Factory("bundle", "bundle.html"))
                .build();
        Template t1 = engine.parse("Hello\nworld!{#if true}next{level}{/}");
        Collection<TemplateNode> sections = t1.findNodes(TemplateNode::isSection);
        assertEquals(1, sections.size());
        assertEquals("if", sections.iterator().next().asSection().name);
        assertThat(sections.iterator().next().asSection().getHelper()).isInstanceOf(IfSectionHelper.class);
        Collection<String> texts = t1.findNodes(TemplateNode::isText).stream().map(TemplateNode::asText)
                .map(TextNode::getValue).collect(Collectors.toList());
        assertEquals(2, texts.size());
        assertThat(texts).containsAll(List.of("Hello\nworld!", "next"));
        assertEquals("level",
                t1.findNodes(TemplateNode::isExpression).iterator().next().asExpression().expression
                        .toOriginalString());
        Template t2 = engine.parse("{#bundle tag=script}foo{/bundle}");
        SectionNode bundle = t2.findNodes(TemplateNode::isSection).stream().map(TemplateNode::asSection)
                .filter(s -> s.getName().equals("bundle")).findFirst().orElse(null);
        assertNotNull(bundle);
        assertTrue(bundle.getHelper() instanceof UserTagSectionHelper);
        UserTagSectionHelper helper = (UserTagSectionHelper) bundle.getHelper();
        Expression e1 = helper.getParameters().get("tag");
        assertNotNull(e1);
        assertEquals("script", e1.toOriginalString());
    }

}
