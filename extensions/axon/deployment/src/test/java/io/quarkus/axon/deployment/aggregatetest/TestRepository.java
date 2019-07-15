package io.quarkus.axon.deployment.aggregatetest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.enterprise.context.Dependent;

@Dependent
public class TestRepository {
    private Log log = LogFactory.getLog(TestRepository.class);

    public void save() {
        log.info("Test repository save");
    }
}
