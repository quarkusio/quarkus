package io.quarkus.optaplanner;

import java.util.Collections;
import javax.inject.Inject;

import io.quarkus.optaplanner.constraints.TestdataPlanningConstraintProvider;
import io.quarkus.optaplanner.domain.TestdataPlanningEntity;
import io.quarkus.optaplanner.domain.TestdataPlanningSolution;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OptaPlannerProcessorXMLPropertyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.optaplanner.solver-config-xml", "io/quarkus/optaplanner/customSolverConfig.xml")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataPlanningEntity.class,
                            TestdataPlanningSolution.class, TestdataPlanningConstraintProvider.class)
                    .addAsResource("io/quarkus/optaplanner/customSolverConfig.xml"));

    @Inject
    SolverConfig solverConfig;
    @Inject
    SolverFactory<TestdataPlanningSolution> solverFactory;

    // TODO
    @Test @Disabled("Missing feature: the solverConfigXml property isn't picked up yet")
    public void solverConfigXml_property() {
        assertNotNull(solverConfig);
        assertEquals(TestdataPlanningSolution.class, solverConfig.getSolutionClass());
        assertEquals(Collections.singletonList(TestdataPlanningEntity.class), solverConfig.getEntityClassList());
        assertEquals(TestdataPlanningConstraintProvider.class, solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass());
        // Properties defined in solverConfig.xml
        assertEquals(3L, solverConfig.getTerminationConfig().getSecondsSpentLimit().longValue());
        assertNotNull(solverFactory);
        assertNotNull(solverFactory.buildSolver());
    }

}
