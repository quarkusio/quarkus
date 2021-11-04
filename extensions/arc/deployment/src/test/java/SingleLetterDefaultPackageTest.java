import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SingleLetterDefaultPackageTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(B.class, C.class, L.class)
                    .addAsResource(new StringAsset("simpleBean.baz=1"), "application.properties"));

    @Inject
    B b;

    @Inject
    C c;

    @Inject
    L l;

    @Test
    public void testB() {
        assertEquals("1", b.ping());
        assertEquals(c.ping(), b.ping());
        assertEquals(l.ping(), b.ping());
    }

}
