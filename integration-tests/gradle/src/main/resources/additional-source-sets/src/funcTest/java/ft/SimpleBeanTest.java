package ft;

import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class SimpleBeanTest {

    @Inject
    SimpleBean bean;

    @Test
    public void verify() {
        assertEquals("hello", bean.hello(), "Did the implementation of the SimpleBean change?");
    }
}
