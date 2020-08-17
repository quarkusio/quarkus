package io.quarkus.arc.test.unproxyable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Vetoed;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.deployment.BeanRegistrarBuildItem;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.test.QuarkusUnitTest;

public class SynthProxiableBeanWithoutNoArgConstructorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SynthBean.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {

                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new BeanRegistrarBuildItem(new BeanRegistrar() {
                            @Override
                            public void register(RegistrationContext context) {
                                context.configure(SynthBean.class)
                                        .scope(ApplicationScoped.class)
                                        .types(SynthBean.class)
                                        .unremovable()
                                        .creator(mc -> {
                                            ResultHandle ret = mc.newInstance(
                                                    MethodDescriptor.ofConstructor(SynthBean.class, String.class),
                                                    mc.load("foo"));
                                            mc.returnValue(ret);
                                        })
                                        .done();
                            }
                        }));
                    }
                }).produces(BeanRegistrarBuildItem.class).build();
            }
        };
    }

    @Test
    public void testSyntheticBean() {
        InstanceHandle<SynthBean> instance = Arc.container().instance(SynthBean.class);
        assertTrue(instance.isAvailable());
        assertEquals("foo", instance.get().getString());
    }

    @Vetoed
    static class SynthBean {
        private String s;

        public SynthBean(String s) {
            this.s = s;
        }

        public String getString() {
            return s;
        }
    }
}
