package io.quarkus.arc.test.stereotype;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Stereotype;
import javax.inject.Inject;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

public class BeanDefiningAnnotationStereotypeTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class, MakeItBean.class, DependentStereotype.class))
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
                    }
                }).produces(BeanDefiningAnnotationBuildItem.class).build();
            }
        };
    }

    @Inject
    Instance<MyBean> instance;

    @Test
    public void test() {
        assertNotEquals(instance.get().getId(), instance.get().getId());
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

    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    public @interface MakeItBean {
    }

    @Dependent
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    public @interface DependentStereotype {
    }

}
