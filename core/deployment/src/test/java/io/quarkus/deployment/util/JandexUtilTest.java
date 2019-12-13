package io.quarkus.deployment.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Test;

public class JandexUtilTest {

    private static final DotName SIMPLE = DotName.createSimple(Single.class.getName());
    private static final DotName MULTIPLE = DotName.createSimple(Multiple.class.getName());
    private static final DotName STRING = DotName.createSimple(String.class.getName());
    private static final DotName INTEGER = DotName.createSimple(Integer.class.getName());
    private static final DotName DOUBLE = DotName.createSimple(Double.class.getName());

    @Test
    public void testInterfaceNotInHierarchy() {
        final Index index = index(Single.class, SingleImpl.class, Multiple.class);
        final DotName impl = DotName.createSimple(SingleImpl.class.getName());
        final List<Type> result = JandexUtil.resolveTypeParameters(impl, MULTIPLE, index);
        assertThat(result).isEmpty();
    }

    @Test
    public void testNoTypePassed() {
        final Index index = index(Single.class, SingleImplNoType.class);
        final DotName impl = DotName.createSimple(SingleImplNoType.class.getName());
        final List<Type> result = JandexUtil.resolveTypeParameters(impl, SIMPLE, index);
        assertThat(result).isEmpty();
    }

    @Test
    public void testAbstractSingle() {
        final Index index = index(Single.class, AbstractSingle.class);
        final DotName impl = DotName.createSimple(AbstractSingle.class.getName());
        assertThatThrownBy(() -> JandexUtil.resolveTypeParameters(impl, SIMPLE, index))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testSimplestImpl() {
        final Index index = index(Single.class, SingleImpl.class);
        final DotName impl = DotName.createSimple(SingleImpl.class.getName());
        final List<Type> result = JandexUtil.resolveTypeParameters(impl, SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(STRING);
    }

    @Test
    public void testSimplestImplWithBound() {
        final Index index = index(SingleWithBound.class, SingleWithBoundImpl.class);
        final DotName impl = DotName.createSimple(SingleWithBoundImpl.class.getName());
        final List<Type> result = JandexUtil.resolveTypeParameters(impl,
                DotName.createSimple(SingleWithBound.class.getName()), index);
        assertThat(result).extracting("name").containsOnly(DotName.createSimple(List.class.getName()));
    }

    @Test
    public void testSimpleImplMultipleParams() {
        final Index index = index(Multiple.class, MultipleImpl.class);
        final DotName impl = DotName.createSimple(MultipleImpl.class.getName());
        final List<Type> result = JandexUtil.resolveTypeParameters(impl, MULTIPLE, index);
        assertThat(result).extracting("name").containsExactly(INTEGER, STRING);
    }

    @Test
    public void testInverseParameterNames() {
        final Index index = index(Multiple.class, InverseMultiple.class, InverseMultipleImpl.class);
        final DotName impl = DotName.createSimple(InverseMultipleImpl.class.getName());
        final List<Type> result = JandexUtil.resolveTypeParameters(impl, MULTIPLE, index);
        assertThat(result).extracting("name").containsExactly(DOUBLE, INTEGER);
    }

    @Test
    public void testImplExtendsSimplestImplementation() {
        final Index index = index(Single.class, SingleImpl.class, SingleImplImpl.class);
        final DotName impl = DotName.createSimple(SingleImplImpl.class.getName());
        final List<Type> result = JandexUtil.resolveTypeParameters(impl, SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(STRING);
    }

    @Test
    public void testImplementationOfInterfaceThatExtendsSimpleWithoutParam() {
        final Index index = index(Single.class, ExtendsSimpleNoParam.class, ExtendsSimpleNoParamImpl.class);
        final DotName impl = DotName.createSimple(ExtendsSimpleNoParamImpl.class.getName());
        final List<Type> result = JandexUtil.resolveTypeParameters(impl, SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(DOUBLE);
    }

    @Test
    public void testImplExtendsImplOfInterfaceThatExtendsSimpleWithoutParams() {
        final Index index = index(Single.class, ExtendsSimpleNoParam.class, ExtendsSimpleNoParamImpl.class,
                ExtendsSimpleNoParamImplImpl.class);
        final DotName impl = DotName.createSimple(ExtendsSimpleNoParamImplImpl.class.getName());
        final List<Type> result = JandexUtil.resolveTypeParameters(impl, SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(DOUBLE);
    }

    @Test
    public void testImplOfInterfaceThatExtendsSimpleWithParam() {
        final Index index = index(Single.class, ExtendsSimpleWithParam.class, ExtendsSimpleWithParamImpl.class);
        final DotName impl = DotName.createSimple(ExtendsSimpleWithParamImpl.class.getName());
        final List<Type> result = JandexUtil.resolveTypeParameters(impl, SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(INTEGER);
    }

    @Test
    public void testImplOfInterfaceThatExtendsSimpleWithParamInMultipleLevels() {
        final Index index = index(Single.class, ExtendsSimpleWithParam.class, ExtendsExtendsSimpleWithParam.class,
                ExtendsExtendsSimpleWithParamImpl.class);
        final DotName impl = DotName.createSimple(ExtendsExtendsSimpleWithParamImpl.class.getName());
        final List<Type> result = JandexUtil.resolveTypeParameters(impl, SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(DOUBLE);
    }

    @Test
    public void testImplOfInterfaceThatExtendsSimpleWithGenericParamInMultipleLevels() {
        final Index index = index(Single.class, ExtendsSimpleWithParam.class, ExtendsExtendsSimpleWithParam.class,
                ExtendsExtendsSimpleGenericParam.class);
        final DotName impl = DotName.createSimple(ExtendsExtendsSimpleGenericParam.class.getName());
        final List<Type> result = JandexUtil.resolveTypeParameters(impl, SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(DotName.createSimple(Map.class.getName()));
    }

    @Test
    public void testImplOfMultipleWithParamsInDifferentLevels() {
        final Index index = index(Multiple.class, MultipleT1.class, ExtendsMultipleT1Impl.class);
        final DotName impl = DotName.createSimple(ExtendsMultipleT1Impl.class.getName());
        final List<Type> result = JandexUtil.resolveTypeParameters(impl, MULTIPLE, index);
        assertThat(result).extracting("name").containsOnly(INTEGER, STRING);
    }

    @Test
    public void testImplOfAbstractMultipleWithParamsInDifferentLevels() {
        final Index index = index(Multiple.class, MultipleT1.class, AbstractMultipleT1Impl.class,
                ExtendsAbstractMultipleT1Impl.class);
        final DotName impl = DotName.createSimple(ExtendsAbstractMultipleT1Impl.class.getName());
        final List<Type> result = JandexUtil.resolveTypeParameters(impl, MULTIPLE, index);
        assertThat(result).extracting("name").containsOnly(INTEGER, STRING);
    }

    @Test
    public void testMultiplePathsToSingle() {
        final Index index = index(Single.class, SingleImpl.class, SingleFromInterfaceAndSuperClass.class);
        final DotName impl = DotName.createSimple(SingleFromInterfaceAndSuperClass.class.getName());
        final List<Type> result = JandexUtil.resolveTypeParameters(impl, SIMPLE, index);
        assertThat(result).extracting("name").containsOnly(STRING);
    }

    @Test
    public void testExtendsAbstractClass() {
        final DotName abstractSingle = DotName.createSimple(AbstractSingle.class.getName());
        final Index index = index(Single.class, AbstractSingle.class, AbstractSingleImpl.class,
                ExtendsAbstractSingleImpl.class);
        assertThat(JandexUtil.resolveTypeParameters(DotName.createSimple(AbstractSingleImpl.class.getName()), abstractSingle,
                index)).extracting("name").containsOnly(INTEGER);
        assertThat(JandexUtil.resolveTypeParameters(DotName.createSimple(ExtendsAbstractSingleImpl.class.getName()),
                abstractSingle, index)).extracting("name").containsOnly(INTEGER);
    }

    @Test
    public void testArrayGenerics() {
        final Index index = index(Repo.class, ArrayRepo.class, GenericArrayRepo.class);
        assertThat(JandexUtil.resolveTypeParameters(DotName.createSimple(ArrayRepo.class.getName()), DotName.createSimple(Repo.class.getName()),
                index)).extracting("name").containsOnly(DotName.createSimple(Integer[].class.getName()));
    }

    @Test
    public void testCompositeGenerics() {
        final Index index = index(Repo.class, Repo2.class, CompositeRepo.class, CompositeRepo2.class, GenericCompositeRepo.class, GenericCompositeRepo2.class);
        assertThat(JandexUtil.resolveTypeParameters(DotName.createSimple(CompositeRepo.class.getName()), DotName.createSimple(Repo.class.getName()),
                index)).hasOnlyOneElementSatisfying(t -> {
            assertThat(t.toString()).isEqualTo(Repo.class.getName() + "<java.lang.Integer>");
        });
        assertThat(JandexUtil.resolveTypeParameters(DotName.createSimple(CompositeRepo2.class.getName()), DotName.createSimple(Repo2.class.getName()),
                index)).hasOnlyOneElementSatisfying(t -> {
            assertThat(t.toString()).isEqualTo(Repo.class.getName() + "<java.lang.Integer>");
        });
    }

    @Test
    public void testErasedGenerics() {
        final Index index = index(Repo.class, BoundedRepo.class, ErasedRepo1.class, MultiBoundedRepo.class, ErasedRepo2.class, A.class);
        assertThat(JandexUtil.resolveTypeParameters(DotName.createSimple(ErasedRepo1.class.getName()), DotName.createSimple(Repo.class.getName()),
                index)).extracting("name").containsOnly(DotName.createSimple(A.class.getName()));
        assertThat(JandexUtil.resolveTypeParameters(DotName.createSimple(ErasedRepo2.class.getName()), DotName.createSimple(Repo.class.getName()),
                index)).extracting("name").containsOnly(DotName.createSimple(A.class.getName()));
    }

    public interface Single<T> {
    }

    public interface SingleWithBound<T extends Collection<?>> {
    }

    public interface ExtendsSimpleNoParam extends Single<Double> {
    }

    public interface ExtendsSimpleWithParam<T> extends Single<T> {
    }

    public interface ExtendsExtendsSimpleWithParam<T> extends ExtendsSimpleWithParam<T> {
    }

    public interface Multiple<T1, T2> {
    }

    public interface InverseMultiple<T1, T2> extends Multiple<T2, T1> {
    }

    public interface MultipleT1<T> extends Multiple<Integer, T> {
    }

    public static class SingleImpl implements Single<String> {
    }

    public static class SingleImplNoType implements Single {
    }

    public static abstract class AbstractSingle<S> implements Single<S> {

    }

    public static class SingleWithBoundImpl implements SingleWithBound<List<String>> {
    }

    public static class SingleImplImpl extends SingleImpl {
    }

    public static class AbstractSingleImpl extends AbstractSingle<Integer> {
    }

    public static class ExtendsAbstractSingleImpl extends AbstractSingleImpl {
    }

    public static class MultipleImpl implements Multiple<Integer, String> {
    }

    public static class InverseMultipleImpl implements InverseMultiple<Integer, Double> {
    }

    public static class ExtendsSimpleNoParamImpl implements ExtendsSimpleNoParam {
    }

    public static class ExtendsSimpleNoParamImplImpl extends ExtendsSimpleNoParamImpl {
    }

    public static class ExtendsSimpleWithParamImpl implements ExtendsSimpleWithParam<Integer> {
    }

    public static class ExtendsExtendsSimpleWithParamImpl implements ExtendsExtendsSimpleWithParam<Double> {
    }

    public static class ExtendsExtendsSimpleGenericParam implements ExtendsExtendsSimpleWithParam<Map<String, List<String>>> {
    }

    public abstract static class AbstractMultipleT1Impl<S> implements MultipleT1<String> {
    }

    public static class ExtendsAbstractMultipleT1Impl extends AbstractMultipleT1Impl<Integer> {
    }

    public static class ExtendsMultipleT1Impl implements MultipleT1<String> {
    }

    public static class SingleFromInterfaceAndSuperClass<W> extends SingleImpl implements Single<String> {
    }

    public interface Repo<T> {
    }

    public interface Repo2<T> {
    }

    public static class DirectRepo implements Repo<Integer> {
    }

    public static class IndirectRepo extends DirectRepo {
    }

    public static class GenericRepo<X> implements Repo<X> {
    }

    public static class IndirectGenericRepo extends GenericRepo<Integer> {
    }

    public static class GenericArrayRepo<X> implements Repo<X[]> {
    }

    public static class ArrayRepo extends GenericArrayRepo<Integer> {
    }

    public static class GenericCompositeRepo<X> implements Repo<Repo<X>> {
    }

    public static class GenericCompositeRepo2<X> implements Repo2<Repo<X>> {
    }

    public static class CompositeRepo extends GenericCompositeRepo<Integer> {
    }

    public static class CompositeRepo2 extends GenericCompositeRepo2<Integer> {
    }

    public static class BoundedRepo<X extends A> implements Repo<X> {
    }

    public static class ErasedRepo1 extends BoundedRepo {
    }

    public interface A {
    }

    public interface B {
    }

    public static class MultiBoundedRepo<X extends A & B> implements Repo<X> {
    }

    public static class ErasedRepo2 extends MultiBoundedRepo {
    }

    private static Index index(Class<?>... classes) {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            try {
                try (InputStream stream = JandexUtilTest.class.getClassLoader()
                        .getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                    indexer.index(stream);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return indexer.complete();
    }

}
