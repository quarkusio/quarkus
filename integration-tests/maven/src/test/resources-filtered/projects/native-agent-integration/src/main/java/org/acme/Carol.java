package org.acme;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Carol
{
    public String sayMyName()
    {
        return "Carol";
    }
}
