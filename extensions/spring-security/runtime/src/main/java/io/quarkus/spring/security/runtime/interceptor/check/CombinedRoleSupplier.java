package io.quarkus.spring.security.runtime.interceptor.check;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class CombinedRoleSupplier implements Supplier<String[]> {

    private final List<Supplier<String[]>> delegates;

    public CombinedRoleSupplier(List<Supplier<String[]>> delegates) {
        this.delegates = delegates;
    }

    @Override
    public String[] get() {
        Set<String> ret = new LinkedHashSet<>();
        for (Supplier<String[]> i : delegates) {
            ret.addAll(Arrays.asList(i.get()));
        }
        return ret.toArray(new String[0]);
    }
}
