package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class LoopValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Machine.class, MachineStatus.class)
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.typesafe.Machine machine}"
                            + "{#for name in machine.getNames(42)}"
                            + "{name}::{name.length}"
                            + "{/for}"), "templates/machine.html"));

    @Inject
    Template machine;

    @Test
    public void testValidation() {
        assertEquals("ping::4",
                machine.data("machine", new Machine().setStatus(MachineStatus.ON)).render());
    }

}
