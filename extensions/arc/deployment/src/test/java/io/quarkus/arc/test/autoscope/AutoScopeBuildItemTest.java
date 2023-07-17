package io.quarkus.arc.test.autoscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

public class AutoScopeBuildItemTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(SimpleBean.class))
            .addBuildChainCustomizer(b -> {
                b.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(AutoAddScopeBuildItem.builder().match((clazz, annotations, index) -> {
                            return clazz.name().toString().equals(SimpleBean.class.getName());
                        }).defaultScope(BuiltinScope.DEPENDENT)
                                .scopeAlreadyAdded((scope, reason) -> {
                                    // We can't pass the state directly to AutoScopeBuildItemTest because it's loaded by a different classloader
                                    Logger.getLogger("AutoScopeBuildItemTest").info(scope + ":" + reason);
                                }).build());
                    }
                }).produces(AutoAddScopeBuildItem.class).build();
                b.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(AutoAddScopeBuildItem.builder().match((clazz, annotations, index) -> {
                            return clazz.name().toString().equals(SimpleBean.class.getName());
                        }).defaultScope(BuiltinScope.SINGLETON).priority(10).reason("Foo!").build());
                    }
                }).produces(AutoAddScopeBuildItem.class).build();
                b.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(AutoAddScopeBuildItem.builder().match((clazz, annotations, index) -> {
                            return clazz.name().toString().equals(NotABean.class.getName());
                        }).defaultScope(BuiltinScope.SINGLETON).unremovable().build());
                    }
                }).produces(AutoAddScopeBuildItem.class).build();
            }).setLogRecordPredicate(log -> "AutoScopeBuildItemTest".equals(log.getLoggerName()))
            .assertLogRecords(records -> {
                assertEquals(1, records.size());
                assertEquals("jakarta.inject.Singleton:Foo!", records.get(0).getMessage());
            });

    @Inject
    Instance<SimpleBean> instance;

    @Test
    public void testBeans() {
        assertTrue(instance.isResolvable());
        // The scope should be @Singleton
        assertEquals(instance.get().ping(), instance.get().ping());

        NotABean notABean1 = Arc.container().instance(NotABean.class).get();
        assertNotNull(notABean1);
        assertEquals(notABean1.ping(), Arc.container().instance(NotABean.class).get().ping());
    }

    static class SimpleBean {

        private String id;

        public String ping() {
            return id;
        }

        @PostConstruct
        void init() {
            id = UUID.randomUUID().toString();
        }

    }

    // add @Singleton and make it unremovable
    static class NotABean {

        private String id;

        public String ping() {
            return id;
        }

        @PostConstruct
        void init() {
            id = UUID.randomUUID().toString();
        }

    }

}
