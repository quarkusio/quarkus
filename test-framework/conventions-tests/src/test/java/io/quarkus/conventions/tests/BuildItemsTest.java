package io.quarkus.conventions.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.EmptyBuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.test.QuarkusConventionsTest;

/**
 * Test that build items are declared properly
 */
public class BuildItemsTest {

    @RegisterExtension
    static QuarkusConventionsTest test = new QuarkusConventionsTest();

    @Test
    public void testEmptyBuildItemsFinal() {
        doTest(EmptyBuildItem.class);
    }

    @Test
    public void testSimpleBuildItemsFinal() {
        doTest(SimpleBuildItem.class);
    }

    @Test
    public void testMultiBuildItemsFinal() {
        doTest(MultiBuildItem.class);
    }

    private <T extends BuildItem> void doTest(Class<T> buildItemClass) {
        IndexView index = test.getExtensionIndex();
        Collection<ClassInfo> buildItems = index
                .getAllKnownSubclasses(DotName.createSimple(buildItemClass.getName()));
        List<String> nonFinal = new ArrayList<>();
        for (ClassInfo buildItem : buildItems) {
            if (!Modifier.isAbstract(buildItem.flags()) && !Modifier.isFinal(buildItem.flags())) {
                nonFinal.add(buildItem.name().toString());
            }
        }
        if (!nonFinal.isEmpty()) {
            fail("Expected all classes extending " + buildItemClass.getSimpleName()
                    + " to be final but the following classes were not: " + nonFinal);
        }
    }
}
