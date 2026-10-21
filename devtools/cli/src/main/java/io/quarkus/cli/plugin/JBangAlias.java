package io.quarkus.cli.plugin;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class JBangAlias {

    @JsonProperty("script-ref")
    private String scriptRef;
    private Optional<String> description;
    @JsonIgnore
    private Optional<String> remote;

    public JBangAlias() {
    }

    public JBangAlias(String scriptRef, Optional<String> description, Optional<String> remote) {
        this.scriptRef = scriptRef;
        this.description = description;
        this.remote = remote;
    }

    public String getScriptRef() {
        return scriptRef;
    }

    public void setScriptRef(String scriptRef) {
        this.scriptRef = scriptRef;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public void setDescription(Optional<String> description) {
        this.description = description;
    }

    public Optional<String> getRemote() {
        return remote;
    }

    public JBangAlias withRemote(Optional<String> remote) {
        return new JBangAlias(scriptRef, description, remote);
    }
}
