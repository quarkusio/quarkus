package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class WhenValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Machine.class, MachineStatus.class)
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.typesafe.Machine machine}"
                            + "{#when machine.status}"
                            + "{#is ON}1"
                            + "{#is OFF}0"
                            + "{/when} "
                            + "{#when machine.status}"
                            + "{#is in ON OFF}"
                            + "OK"
                            + "{/when} "
                            + "{#when machine.ping}"
                            + "{#is machine.status}"
                            + "NOK"
                            + "{#is in 1 2}"
                            + "OK"
                            + "{/when}"), "templates/machine.html"));

    @Inject
    Template machine;

    @Test
    public void testValidation() {
        assertEquals("1 OK OK",
                machine.data("machine", new Machine().setStatus(MachineStatus.ON)).render());
        assertEquals("0 OK OK",
                machine.data("machine", new Machine().setStatus(MachineStatus.OFF)).render());
    }

}
