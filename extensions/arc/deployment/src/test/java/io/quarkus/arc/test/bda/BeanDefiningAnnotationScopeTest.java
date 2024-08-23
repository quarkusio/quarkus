package io.quarkus.arc.test.bda;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

public class BeanDefiningAnnotationScopeTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(MyBean.class, AnotherBean.class, YetAnotherBean.class, MakeItBean.class,
                            QualifierMakeItBean.class, DependentStereotype.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {

                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new BeanDefiningAnnotationBuildItem(DotName.createSimple(MakeItBean.class.getName()),
                                DotName.createSimple(ApplicationScoped.class.getName())));
                        context.produce(
                                new BeanDefiningAnnotationBuildItem(DotName.createSimple(QualifierMakeItBean.class.getName()),
                                        DotName.createSimple(ApplicationScoped.class.getName())));
                    }
                }).produces(BeanDefiningAnnotationBuildItem.class).build();
            }
        };
    }

    @Inject
    Instance<MyBean> my;

    @Inject
    Instance<AnotherBean> another;

    @Inject
    @QualifierMakeItBean
    Instance<YetAnotherBean> yetAnother;

    @Test
    public void testExplicitStereotypeScopeWins() {
        assertNotEquals(my.get().getId(), my.get().getId());
    }

    @Test
    public void testDefaultScopeIsUsed() {
        assertEquals(another.get().getId(), another.get().getId());
    }

    @Test
    public void testQualifierAsBeanDefiningAnnotation() {
        assertEquals(yetAnother.get().getId(), yetAnother.get().getId());
    }

    @DependentStereotype
    @MakeItBean
    static class MyBean {

        private String id;

        public String getId() {
            return id;
        }

        @PostConstruct
        void init() {
            this.id = UUID.randomUUID().toString();
        }

    }

    @MakeItBean
    static class AnotherBean {

        private String id;

        public String getId() {
            return id;
        }

        @PostConstruct
        void init() {
            this.id = UUID.randomUUID().toString();
        }

    }

    @QualifierMakeItBean
    static class YetAnotherBean {

        private String id;

        public String getId() {
            return id;
        }

        @PostConstruct
        void init() {
            this.id = UUID.randomUUID().toString();
        }

    }

    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    public @interface MakeItBean {
    }

    @Qualifier
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    public @interface QualifierMakeItBean {
    }

    @Dependent
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    public @interface DependentStereotype {
    }

}
