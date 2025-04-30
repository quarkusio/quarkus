package io.quarkus.mailer.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface NtlmConfig {

    /**
     * Sets the workstation used on NTLM authentication.
     */
    public Optional<String> workstation();

    /**
     * Sets the domain used on NTLM authentication.
     */
    public Optional<String> domain();

}
