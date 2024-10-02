package io.quarkus.qute.generator.hierarchy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.generator.SimpleGeneratorTest;
import io.quarkus.qute.generator.TestClassOutput;
import io.quarkus.qute.generator.ValueResolverGenerator;

public class HierarchyTest {

    static Set<String> generatedTypes = new HashSet<>();

    @BeforeAll
    public static void init() throws IOException {
        TestClassOutput classOutput = new TestClassOutput();
        Index index = SimpleGeneratorTest.index(Level1.class, Level2.class, Level3.class, Level4.class, FirstLevel.class,
                SecondLevel.class);
        ValueResolverGenerator generator = ValueResolverGenerator.builder().setIndex(index).setClassOutput(classOutput)
                .addClass(index.getClassByName(DotName.createSimple(Level4.class.getName())))
                .build();

        generator.generate();
        generatedTypes.addAll(generator.getGeneratedTypes());
    }

    @Test
    public void testHierarchy() throws Exception {
        EngineBuilder builder = Engine.builder().addDefaults();
        for (String generatedType : generatedTypes) {
            builder.addValueResolver((ValueResolver) SimpleGeneratorTest.newResolver(generatedType));
        }
        Engine engine = builder.build();

        Level4 level4 = new Level4();
        assertEquals(1, level4.getLevel1());
        assertEquals(1, level4.firstLevel());
        assertEquals(2, level4.secondLevel());
        assertEquals(4, level4.getLevel4());
        assertEquals(4, level4.overridenLevel);

        assertEquals("1::1::2::4::4",
                engine.parse(
                        "{level.level1}::{level.firstLevel}::{level.secondLevel}::{level.getLevel4}::{level.overridenLevel}")
                        .render(Map.of("level", level4)));
    }

}
