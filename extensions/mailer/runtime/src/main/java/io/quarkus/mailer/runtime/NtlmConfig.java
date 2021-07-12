package io.quarkus.mailer.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class NtlmConfig {

    /**
     * Sets the workstation used on NTLM authentication.
     */
    @ConfigItem
    public Optional<String> workstation;

    /**
     * Sets the domain used on NTLM authentication.
     */
    @ConfigItem
    public Optional<String> domain;

}
