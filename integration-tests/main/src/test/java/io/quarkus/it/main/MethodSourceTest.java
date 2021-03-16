package io.quarkus.it.main;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.it.arc.UnusedBean;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MethodSourceTest {

    @Inject
    UnusedBean unusedBean;

    @ParameterizedTest
    @MethodSource("provideDummyInput")
    public void testParameterResolver(UnusedBean.DummyInput dummyInput, Matcher<String> matcher) {
        UnusedBean.DummyResult dummyResult = unusedBean.dummy(dummyInput);
        assertThat(dummyResult.getResult(), matcher);
    }

    private static Collection<Arguments> provideDummyInput() {
        return Arrays.asList(
                Arguments.of(
                        new UnusedBean.DummyInput("whatever", new UnusedBean.NestedDummyInput(Arrays.asList(1, 2, 3))),
                        CoreMatchers.is("whatever/6")),
                Arguments.of(
                        new UnusedBean.DummyInput("hi", new UnusedBean.NestedDummyInput(Collections.emptyList())),
                        CoreMatchers.is("hi/0")));
    }

}
