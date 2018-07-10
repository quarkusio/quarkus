package org.jboss.shamrock.deployment;

/**
 * A build time processor that processes a deployments metadata and uses it to generate wiring bytecode
 *
 * These classes are discovered via {@link java.util.ServiceLoader}
 *
 */
public interface ResourceProcessor {

    /**
     * Processes the current deployment
     *
     * @param archiveContext The archive context
     * @param processorContext The processor context
     * @throws Exception If a problem occurs
     */
    void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception;

    /**
     *
     * TODO: do we want this? We have had many discussions about better ways to do this in EAP F2F's
     *
     * @return The priority that is used to determine the order
     */
    int getPriority();
}
