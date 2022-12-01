package io.quarkus.hibernate.orm.runtime.devconsole;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

public class PersistenceUnitNameComparatorTestCase {

    @Test
    public void puNameComparatorTest() {
        List<String> names = new ArrayList<>();
        names.add("gamma");
        names.add("alpha");
        names.add("<default>");
        names.add("beta");
        names.sort(new PersistenceUnitUtil.PersistenceUnitNameComparator());

        assertThat(names.get(0)).isEqualTo("<default>");
        assertThat(names.get(1)).isEqualTo("alpha");
        assertThat(names.get(2)).isEqualTo("beta");
        assertThat(names.get(3)).isEqualTo("gamma");
    }
}
