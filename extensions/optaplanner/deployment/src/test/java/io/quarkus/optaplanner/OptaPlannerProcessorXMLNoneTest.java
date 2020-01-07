package io.quarkus.optaplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;

import io.quarkus.optaplanner.constraints.TestdataPlanningConstraintProvider;
import io.quarkus.optaplanner.domain.TestdataPlanningEntity;
import io.quarkus.optaplanner.domain.TestdataPlanningSolution;
import io.quarkus.test.QuarkusUnitTest;

public class OptaPlannerProcessorXMLNoneTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataPlanningEntity.class,
                            TestdataPlanningSolution.class, TestdataPlanningConstraintProvider.class));

    @Inject
    SolverConfig solverConfig;
    @Inject
    SolverFactory<TestdataPlanningSolution> solverFactory;

    // TODO FIXME
    @Test
    @Disabled("BUG: The solverConfig.xml isn't filtered out, so the terminationConfig isn't null.")
    public void solverConfigXml_default() {
        assertNotNull(solverConfig);
        assertEquals(TestdataPlanningSolution.class, solverConfig.getSolutionClass());
        assertEquals(Collections.singletonList(TestdataPlanningEntity.class), solverConfig.getEntityClassList());
        assertEquals(TestdataPlanningConstraintProvider.class,
                solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass());
        // No termination defined (solverConfig.xml isn't included)
        assertNull(solverConfig.getTerminationConfig());
        assertNotNull(solverFactory);
        assertNotNull(solverFactory.buildSolver());
    }

}
