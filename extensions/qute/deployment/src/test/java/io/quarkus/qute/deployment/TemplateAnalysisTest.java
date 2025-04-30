package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.Expression;
import io.quarkus.qute.ParameterDeclaration;
import io.quarkus.qute.TemplateNode.Origin;
import io.quarkus.qute.Variant;
import io.quarkus.qute.deployment.TemplatesAnalysisBuildItem.TemplateAnalysis;

public class TemplateAnalysisTest {

    @Test
    public void testSortedParamDeclarations() {
        List<ParameterDeclaration> sorted = TemplateAnalysis.getSortedParameterDeclarations(List.of(paramDeclaration("foo", -1),
                paramDeclaration("bar", -1), paramDeclaration("qux", 10), paramDeclaration("baz", 1)));
        assertEquals(4, sorted.size());
        assertEquals("baz", sorted.get(0).getKey());
        assertEquals("qux", sorted.get(1).getKey());
        assertTrue(sorted.get(2).getKey().equals("foo") || sorted.get(2).getKey().equals("bar"));
        assertTrue(sorted.get(3).getKey().equals("foo") || sorted.get(3).getKey().equals("bar"));
    }

    ParameterDeclaration paramDeclaration(String key, int line) {
        return new ParameterDeclaration() {

            @Override
            public String getTypeInfo() {
                return null;
            }

            @Override
            public Origin getOrigin() {
                return new Origin() {

                    @Override
                    public Optional<Variant> getVariant() {
                        return Optional.empty();
                    }

                    @Override
                    public String getTemplateId() {
                        return null;
                    }

                    @Override
                    public String getTemplateGeneratedId() {
                        return null;
                    }

                    @Override
                    public int getLineCharacterStart() {
                        return 0;
                    }

                    @Override
                    public int getLineCharacterEnd() {
                        return 0;
                    }

                    @Override
                    public int getLine() {
                        return line;
                    }
                };
            }

            @Override
            public String getKey() {
                return key;
            }

            @Override
            public Expression getDefaultValue() {
                return null;
            }
        };
    }

}
