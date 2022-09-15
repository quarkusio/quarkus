package io.quarkus.arc.test.resolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.AbstractList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RuntimeResolutionTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyList.class);

    @SuppressWarnings("serial")
    @Test
    public void testResolution() throws IOException {
        ArcContainer arc = Arc.container();
        // MyList bean types: MyList, AbstractList<Integer>, List<Integer>, AbstractCollection<Integer>, Iterable<Integer>, Object
        InstanceHandle<List<? extends Number>> list = arc.instance(new TypeLiteral<List<? extends Number>>() {
        });
        assertTrue(list.isAvailable());
        assertEquals(Integer.valueOf(7), list.get().get(1));
    }

    @Singleton
    static class MyList extends AbstractList<Integer> {

        @Override
        public Integer get(int index) {
            return Integer.valueOf(7);
        }

        @Override
        public int size() {
            return 0;
        }

    }

}
