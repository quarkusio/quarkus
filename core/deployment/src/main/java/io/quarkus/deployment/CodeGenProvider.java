package io.quarkus.deployment;

import org.wildfly.common.annotation.NotNull;

import io.quarkus.bootstrap.prebuild.CodeGenException;

/**
 * Service providers for this interfaces are triggered during generate-sources phase of build of Quarkus applications
 */
public interface CodeGenProvider {
    /**
     *
     * @return unique name of the code gen provider, will correspond to the directory in <code>generated-sources</code>
     */
    @NotNull
    String providerId();

    /**
     * File extension that CodeGenProvider will generate code from
     *
     * @return file extension
     */
    @NotNull
    String inputExtension();

    /**
     * Name of the directory containing the input files for the CodeGenProvider
     * for <code>foo</code>, <code>src/main/foo</code> for application and <code>src/test/foo</code> for test resources
     *
     * @return the input directory
     */
    @NotNull
    String inputDirectory();

    /**
     * Trigger code generation
     * 
     * @param context code generation context
     * @return true if files were generated/modified
     */
    boolean trigger(CodeGenContext context) throws CodeGenException;

}
