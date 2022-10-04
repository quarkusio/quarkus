package io.quarkus.bootstrap.resolver.maven;

import java.util.Collections;
import java.util.List;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;

public class SettingsDecryptionResultImpl implements SettingsDecryptionResult {

    private final List<Server> servers;
    private final List<Proxy> proxies;
    private final List<SettingsProblem> problems;

    SettingsDecryptionResultImpl(List<Server> servers, List<Proxy> proxies, List<SettingsProblem> problems) {
        this.servers = servers != null ? servers : Collections.emptyList();
        this.proxies = proxies != null ? proxies : Collections.emptyList();
        this.problems = problems != null ? problems : Collections.emptyList();
    }

    @Override
    public Server getServer() {
        return servers.isEmpty() ? null : servers.get(0);
    }

    @Override
    public List<Server> getServers() {
        return servers;
    }

    @Override
    public Proxy getProxy() {
        return proxies.isEmpty() ? null : proxies.get(0);
    }

    @Override
    public List<Proxy> getProxies() {
        return proxies;
    }

    @Override
    public List<SettingsProblem> getProblems() {
        return problems;
    }
}
