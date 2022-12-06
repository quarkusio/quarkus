package io.quarkus.grpc.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Set;
import java.util.stream.Stream;

import jakarta.transaction.Transactional;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableSet;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;

public class GrpcServerProcessorTest {

    static Stream<Arguments> blockingAnnotations() {
        return Stream.of(
                arguments(BlockingRoot.class, BlockingRoot.EXPECTED),
                arguments(BlockingExtendsBlockingRoot.class, BlockingExtendsBlockingRoot.EXPECTED),
                arguments(NonBlockingExtendsBlockingRoot.class, NonBlockingExtendsBlockingRoot.EXPECTED),
                arguments(ExtendsBlockingRoot.class, ExtendsBlockingRoot.EXPECTED),
                arguments(NonBlockingRoot.class, NonBlockingRoot.EXPECTED),
                arguments(BlockingExtendsNonBlockingRoot.class, BlockingExtendsNonBlockingRoot.EXPECTED),
                arguments(NonBlockingExtendsNonBlockingRoot.class, NonBlockingExtendsNonBlockingRoot.EXPECTED),
                arguments(ExtendsNonBlockingRoot.class, ExtendsNonBlockingRoot.EXPECTED),
                arguments(NoClassAnnotationsRoot.class, NoClassAnnotationsRoot.EXPECTED),
                arguments(NoClassAnnotationsReverseMeaning.class, NoClassAnnotationsReverseMeaning.EXPECTED),
                arguments(NoClassAnnotationsEmpty.class, NoClassAnnotationsEmpty.EXPECTED),
                arguments(ClassAnnotationsBlocking.class, ClassAnnotationsBlocking.EXPECTED),
                arguments(ClassAnnotationsNonBlocking.class, ClassAnnotationsNonBlocking.EXPECTED),
                arguments(TransactionalEmpty.class, TransactionalEmpty.EXPECTED),
                arguments(TransactionalOverriding.class, TransactionalOverriding.EXPECTED),
                arguments(OverridingTransactionalRoot.class, OverridingTransactionalRoot.EXPECTED),
                arguments(NonBlockingOverridingTransactional.class, NonBlockingOverridingTransactional.EXPECTED),
                arguments(BlockingOverridingTransactional.class, BlockingOverridingTransactional.EXPECTED));
    }

    @ParameterizedTest
    @MethodSource("blockingAnnotations")
    public void blockingAnnotations(Class<?> clazz, Set<String> expectedBlocking) throws Exception {

        DotName className = DotName.createSimple(clazz.getName());

        Indexer indexer = new Indexer();
        while (true) {
            indexer.indexClass(clazz);
            if (clazz.getSuperclass() == Object.class) {
                break;
            }
            clazz = clazz.getSuperclass();
        }

        Index index = indexer.complete();

        ClassInfo classInfo = index.getClassByName(className);

        assertThat(GrpcServerProcessor.gatherBlockingMethodNames(classInfo, index))
                .containsExactlyInAnyOrderElementsOf(expectedBlocking);
    }

    @Blocking
    static class BlockingRoot {
        static final Set<String> EXPECTED = ImmutableSet.of("method");

        void method() {
        }
    }

    @Blocking
    static class BlockingExtendsBlockingRoot extends BlockingRoot {
        static final Set<String> EXPECTED = ImmutableSet.of("method");

        void method() {
        }
    }

    @NonBlocking
    static class NonBlockingExtendsBlockingRoot extends BlockingRoot {
        static final Set<String> EXPECTED = ImmutableSet.of();

        void method() {
        }
    }

    static class ExtendsBlockingRoot extends BlockingRoot {
        static final Set<String> EXPECTED = ImmutableSet.of();

        void method() {
        }
    }

    @NonBlocking
    static class NonBlockingRoot {
        static final Set<String> EXPECTED = ImmutableSet.of();

        void method() {
        }
    }

    @Blocking
    static class BlockingExtendsNonBlockingRoot extends NonBlockingRoot {
        static final Set<String> EXPECTED = ImmutableSet.of("method");

        void method() {
        }
    }

    @NonBlocking
    static class NonBlockingExtendsNonBlockingRoot extends NonBlockingRoot {
        static final Set<String> EXPECTED = ImmutableSet.of();

        void method() {
        }
    }

    static class ExtendsNonBlockingRoot extends NonBlockingRoot {
        static final Set<String> EXPECTED = ImmutableSet.of();

        void method() {
        }
    }

    static class NoClassAnnotationsRoot {
        static final Set<String> EXPECTED = ImmutableSet.of("blocking", "transactional");

        @NonBlocking
        void nonBlocking() {
        }

        @Blocking
        void blocking() {
        }

        @Transactional
        void transactional() {
        }

        void noAnnotation() {
        }
    }

    static class NoClassAnnotationsReverseMeaning extends NoClassAnnotationsRoot {
        static final Set<String> EXPECTED = ImmutableSet.of("nonBlocking", "noAnnotation");

        @Blocking
        void nonBlocking() {
        }

        @NonBlocking
        void blocking() {
        }

        @NonBlocking
        void transactional() {
        }

        @Transactional
        void noAnnotation() {
        }
    }

    static class NoClassAnnotationsEmpty extends NoClassAnnotationsRoot {
        static final Set<String> EXPECTED = ImmutableSet.of("blocking", "transactional");
    }

    @Transactional
    static class TransactionalEmpty extends NoClassAnnotationsRoot {
        static final Set<String> EXPECTED = ImmutableSet.of("noAnnotation", "blocking", "transactional");
    }

    @Transactional
    static class TransactionalOverriding extends NoClassAnnotationsRoot {
        static final Set<String> EXPECTED = ImmutableSet.of("noAnnotation", "blocking", "transactional");

        void blocking() {
        }

        void transactional() {
        }
    }

    @Blocking
    static class ClassAnnotationsBlocking extends NoClassAnnotationsRoot {
        static final Set<String> EXPECTED = ImmutableSet.of("nonBlocking", "blocking", "transactional", "noAnnotation");

        void nonBlocking() {
        }

        void blocking() {
        }

        void transactional() {
        }

        void noAnnotation() {
        }
    }

    @NonBlocking
    static class ClassAnnotationsNonBlocking extends NoClassAnnotationsRoot {
        static final Set<String> EXPECTED = ImmutableSet.of();

        void nonBlocking() {
        }

        void blocking() {
        }

        void transactional() {
        }

        void noAnnotation() {
        }
    }

    @Transactional
    static class OverridingTransactionalRoot {
        static final Set<String> EXPECTED = ImmutableSet.of("method", "transactional", "another");

        void method() {
        }

        void transactional() {
        }

        void another() {
        }
    }

    static class NonBlockingOverridingTransactional extends OverridingTransactionalRoot {
        static final Set<String> EXPECTED = ImmutableSet.of("transactional", "another");

        @NonBlocking
        void method() {
        }

        void another() {
        }
    }

    static class BlockingOverridingTransactional extends OverridingTransactionalRoot {
        static final Set<String> EXPECTED = ImmutableSet.of("method", "transactional", "another");

        @Blocking
        void method() {
        }
    }
}
