package io.quarkus.qute.deployment.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class HiddenFragmentsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset("""
                            {#capture faClass}
                            {#when type}
                              {#is "info"}
                                fa fa-lightbulb
                              {#is "warning"}
                                fa fa-exclamation-triangle
                              {#is "error"}
                                fa fa-times-circle
                              {#is "success"}
                                fa fa-check-circle
                              {#is "question"}
                                fa fa-question-circle
                              {#else}
                                fa fa-info-circle
                              {/when}
                            {/}
                            <i class="{capture:faClass(param:type = type.or(anotherType)).strip}"></i>::{capture:faClass.strip}
                                                        """), "templates/hide.html"));

    @Inject
    Template hide;

    @Test
    public void testResolvers() {
        assertEquals("<i class=\"fa fa-times-circle\"></i>::fa fa-times-circle",
                hide.data("type", "error", "anotherType", "foo").render().strip());
        assertEquals("<i class=\"fa fa-question-circle\"></i>::fa fa-info-circle",
                hide.data("type", null, "anotherType", "question").render().strip());
    }

}
