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
import org.jboss.jandex.Type.Kind;
import org.junit.jupiter.api.Test;

public class JandexUtilTest {

    private static final DotName SIMPLE = DotName.createSimple(Single.class.getName());
    private static final DotName MULTIPLE = DotName.createSimple(Multiple.class.getName());

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
        checkRepoArg(index, SingleImplNoType.class, Single.class, Object.class);
    }

    @Test
    public void testAbstractSingle() {
        final Index index = index(Single.class, AbstractSingle.class);
        final DotName impl = DotName.createSimple(AbstractSingle.class.getName());
        List<Type> ret = JandexUtil.resolveTypeParameters(impl, SIMPLE, index);
        assertThat(ret).hasSize(1).allMatch(t -> t.kind() == Kind.TYPE_VARIABLE && t.asTypeVariable().identifier().equals("S"));
    }

    @Test
    public void testSimplestImpl() {
        final Index index = index(Single.class, SingleImpl.class);
        checkRepoArg(index, SingleImpl.class, Single.class, String.class);
    }

    @Test
    public void testSimplestImplWithBound() {
        final Index index = index(SingleWithBound.class, SingleWithBoundImpl.class);
        checkRepoArg(index, SingleWithBoundImpl.class, SingleWithBound.class, List.class);
    }

    @Test
    public void testSimpleImplMultipleParams() {
        final Index index = index(Multiple.class, MultipleImpl.class);
        checkRepoArg(index, MultipleImpl.class, Multiple.class, Integer.class, String.class);
    }

    @Test
    public void testInverseParameterNames() {
        final Index index = index(Multiple.class, InverseMultiple.class, InverseMultipleImpl.class);
        checkRepoArg(index, InverseMultipleImpl.class, Multiple.class, Double.class, Integer.class);
    }

    @Test
    public void testImplExtendsSimplestImplementation() {
        final Index index = index(Single.class, SingleImpl.class, SingleImplImpl.class);
        checkRepoArg(index, SingleImplImpl.class, Single.class, String.class);
    }

    @Test
    public void testImplementationOfInterfaceThatExtendsSimpleWithoutParam() {
        final Index index = index(Single.class, ExtendsSimpleNoParam.class, ExtendsSimpleNoParamImpl.class);
        checkRepoArg(index, ExtendsSimpleNoParamImpl.class, Single.class, Double.class);
    }

    @Test
    public void testImplExtendsImplOfInterfaceThatExtendsSimpleWithoutParams() {
        final Index index = index(Single.class, ExtendsSimpleNoParam.class, ExtendsSimpleNoParamImpl.class,
                ExtendsSimpleNoParamImplImpl.class);
        checkRepoArg(index, ExtendsSimpleNoParamImplImpl.class, Single.class, Double.class);
    }

    @Test
    public void testImplOfInterfaceThatExtendsSimpleWithParam() {
        final Index index = index(Single.class, ExtendsSimpleWithParam.class, ExtendsSimpleWithParamImpl.class);
        checkRepoArg(index, ExtendsSimpleWithParamImpl.class, Single.class, Integer.class);
    }

    @Test
    public void testImplOfInterfaceThatExtendsSimpleWithParamInMultipleLevels() {
        final Index index = index(Single.class, ExtendsSimpleWithParam.class, ExtendsExtendsSimpleWithParam.class,
                ExtendsExtendsSimpleWithParamImpl.class);
        checkRepoArg(index, ExtendsExtendsSimpleWithParamImpl.class, Single.class, Double.class);
    }

    @Test
    public void testImplOfInterfaceThatExtendsSimpleWithGenericParamInMultipleLevels() {
        final Index index = index(Single.class, ExtendsSimpleWithParam.class, ExtendsExtendsSimpleWithParam.class,
                ExtendsExtendsSimpleGenericParam.class);
        checkRepoArg(index, ExtendsExtendsSimpleGenericParam.class, Single.class, Map.class);
    }

    @Test
    public void testImplOfMultipleWithParamsInDifferentLevels() {
        final Index index = index(Multiple.class, MultipleT1.class, ExtendsMultipleT1Impl.class);
        checkRepoArg(index, ExtendsMultipleT1Impl.class, Multiple.class, Integer.class, String.class);
    }

    @Test
    public void testImplOfAbstractMultipleWithParamsInDifferentLevels() {
        final Index index = index(Multiple.class, MultipleT1.class, AbstractMultipleT1Impl.class,
                ExtendsAbstractMultipleT1Impl.class);
        checkRepoArg(index, ExtendsAbstractMultipleT1Impl.class, Multiple.class, Integer.class, String.class);
    }

    @Test
    public void testMultiplePathsToSingle() {
        final Index index = index(Single.class, SingleImpl.class, SingleFromInterfaceAndSuperClass.class);
        checkRepoArg(index, SingleFromInterfaceAndSuperClass.class, Single.class, String.class);
    }

    @Test
    public void testExtendsAbstractClass() {
        final Index index = index(Single.class, AbstractSingle.class, AbstractSingleImpl.class,
                ExtendsAbstractSingleImpl.class);
        checkRepoArg(index, AbstractSingleImpl.class, AbstractSingle.class, Integer.class);
        checkRepoArg(index, ExtendsAbstractSingleImpl.class, AbstractSingle.class, Integer.class);
    }

    @Test
    public void testArrayGenerics() {
        final Index index = index(Repo.class, ArrayRepo.class, GenericArrayRepo.class);
        checkRepoArg(index, ArrayRepo.class, Repo.class, Integer[].class);
    }

    @Test
    public void testCompositeGenerics() {
        final Index index = index(Repo.class, Repo2.class, CompositeRepo.class, CompositeRepo2.class,
                GenericCompositeRepo.class, GenericCompositeRepo2.class);
        checkRepoArg(index, CompositeRepo.class, Repo.class, Repo.class.getName() + "<java.lang.Integer>");
        checkRepoArg(index, CompositeRepo2.class, Repo2.class, Repo.class.getName() + "<java.lang.Integer>");
    }

    @Test
    public void testErasedGenerics() {
        final Index index = index(Repo.class, BoundedRepo.class, ErasedRepo1.class, MultiBoundedRepo.class, ErasedRepo2.class,
                A.class);
        checkRepoArg(index, ErasedRepo1.class, Repo.class, A.class);
        checkRepoArg(index, ErasedRepo2.class, Repo.class, A.class);
    }

    @Test
    public void testNonProblematicUnindexed() {
        final Index index = index(Single.class, SingleFromInterfaceAndSuperClass.class);
        checkRepoArg(index, SingleFromInterfaceAndSuperClass.class, Single.class, String.class);
    }

    @Test
    public void testProblematicUnindexed() {
        final Index index = index(Single.class, AbstractSingleImpl.class, ExtendsAbstractSingleImpl.class);
        assertThatThrownBy(() -> {
            JandexUtil.resolveTypeParameters(name(ExtendsAbstractSingleImpl.class), name(Single.class), index);
        }).isInstanceOf(IllegalArgumentException.class);
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

    private void checkRepoArg(Index index, Class<?> baseClass, Class<?> soughtClass, Class<?> expectedArg) {
        List<Type> args = JandexUtil.resolveTypeParameters(name(baseClass), name(soughtClass),
                index);
        assertThat(args).extracting(Type::name).containsOnly(name(expectedArg));
    }

    private void checkRepoArg(Index index, Class<?> baseClass, Class<?> soughtClass, Class<?>... expectedArgs) {
        List<Type> args = JandexUtil.resolveTypeParameters(name(baseClass), name(soughtClass),
                index);
        DotName[] expectedArgNames = new DotName[expectedArgs.length];
        for (int i = 0; i < expectedArgs.length; i++) {
            expectedArgNames[i] = name(expectedArgs[i]);
        }
        assertThat(args).extracting(Type::name).containsOnly(expectedArgNames);
    }

    private void checkRepoArg(Index index, Class<?> baseClass, Class<?> soughtClass, String expectedArg) {
        List<Type> args = JandexUtil.resolveTypeParameters(name(baseClass), name(soughtClass),
                index);
        assertThat(args).singleElement().satisfies(t -> {
            assertThat(t.toString()).isEqualTo(expectedArg);
        });
    }

    private static DotName name(Class<?> klass) {
        return DotName.createSimple(klass.getName());
    }

}
