package io.quarkus.maven.utilities;

import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

class SortedProperties extends Properties {

    private static final long serialVersionUID = 1L;

    @Override
    public Set<Object> keySet()
    {
       return new TreeSet<Object>(super.keySet());
    }
}
