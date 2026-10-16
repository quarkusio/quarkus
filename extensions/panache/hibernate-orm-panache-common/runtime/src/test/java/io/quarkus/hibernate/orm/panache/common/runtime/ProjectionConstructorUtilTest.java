package io.quarkus.hibernate.orm.panache.common.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.panache.common.ProjectedConstructor;
import io.quarkus.panache.hibernate.common.runtime.ProjectionConstructorUtil;

public class ProjectionConstructorUtilTest {

    public static class SimpleDto {
        public final Long id;
        public final String name;

        public SimpleDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class AnnotatedDto {
        public final Long id;
        public final String name;

        public AnnotatedDto(Long id, String name, Object ignored) {
            this.id = id;
            this.name = name;
        }

        @ProjectedConstructor
        public AnnotatedDto(Long id, String name) {
            this(id, name, null);
        }
    }

    @Test
    public void testBuildSelectClauseForSimpleDto() {
        String selectClause = ProjectionConstructorUtil.buildSelectClause(SimpleDto.class, (type, parameterName) -> null);
        Assertions.assertEquals(
                "SELECT new io.quarkus.hibernate.orm.panache.common.runtime.ProjectionConstructorUtilTest$SimpleDto (id,name) ",
                selectClause);
    }

    @Test
    public void testPrefersProjectedConstructor() {
        Constructor<?> constructor = ProjectionConstructorUtil.getProjectionConstructor(AnnotatedDto.class);
        Assertions.assertEquals(2, constructor.getParameterCount());
        Assertions.assertNotNull(constructor.getAnnotation(ProjectedConstructor.class));
    }

    public static class SyntheticLikeDto {
        public final Long id;
        public final String name;

        public SyntheticLikeDto(Long id, String name) {
            this(id, name, 0);
        }

        SyntheticLikeDto(Long id, String name, int bitmask) {
            this.id = id;
            this.name = name;
        }
    }

    public static class DoubleConstructorWithOneEmpty {
        public String name;
        public String ownerName;

        public DoubleConstructorWithOneEmpty() {
        }

        public DoubleConstructorWithOneEmpty(String name) {
            this.name = name;
        }

        public DoubleConstructorWithOneEmpty(String name, String ownerName) {
            this.name = name;
            this.ownerName = ownerName;
        }
    }

    @Test
    public void testPrefersConstructorWithFewestParameters() {
        Constructor<?> constructor = ProjectionConstructorUtil.getProjectionConstructor(SyntheticLikeDto.class);
        Parameter[] parameters = constructor.getParameters();
        Assertions.assertEquals(2, parameters.length);
        Assertions.assertEquals("id", parameters[0].getName());
        Assertions.assertEquals("name", parameters[1].getName());

        Constructor<?> doubleConstructor = ProjectionConstructorUtil
                .getProjectionConstructor(DoubleConstructorWithOneEmpty.class);
        Assertions.assertEquals(1, doubleConstructor.getParameterCount());
        Assertions.assertEquals("name", doubleConstructor.getParameters()[0].getName());
    }
}
