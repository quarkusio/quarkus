package org.acme;

import java.util.List;

public class ModuleList {

    private final List<String> localModules;

    public ModuleList(List<String> localModules) {
        this.localModules = localModules;
    }

    public List<String> getModules() {
        return localModules;
    }
}