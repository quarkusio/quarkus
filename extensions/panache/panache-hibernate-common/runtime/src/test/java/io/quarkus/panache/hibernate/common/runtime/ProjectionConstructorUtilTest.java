package io.quarkus.panache.hibernate.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.panache.common.exception.PanacheQueryException;

public class ProjectionConstructorUtilTest {

    @Test
    public void scalarParametersAreProjected() {
        String select = ProjectionConstructorUtil.buildSelectClause(NameDto.class, (type, path) -> path);

        assertEquals("SELECT new " + NameDto.class.getName() + " (id,name) ", select);
    }

    @Test
    public void collectionParameterIsRejected() {
        PanacheQueryException exception = assertThrows(PanacheQueryException.class,
                () -> ProjectionConstructorUtil.buildSelectClause(WithSetDto.class, (type, path) -> path));

        assertTrue(exception.getMessage().contains("lines"), exception.getMessage());
        assertTrue(exception.getMessage().contains(WithSetDto.class.getName()), exception.getMessage());
    }

    @Test
    public void listParameterIsRejected() {
        assertThrows(PanacheQueryException.class,
                () -> ProjectionConstructorUtil.buildSelectClause(WithListDto.class, (type, path) -> path));
    }

    @Test
    public void mapParameterIsRejected() {
        assertThrows(PanacheQueryException.class,
                () -> ProjectionConstructorUtil.buildSelectClause(WithMapDto.class, (type, path) -> path));
    }

    public record NameDto(Long id, String name) {
    }

    public record WithSetDto(Long id, Set<NameDto> lines) {
    }

    public record WithListDto(Long id, List<String> tags) {
    }

    public record WithMapDto(Long id, Map<String, String> attributes) {
    }
}
