package org.acme.lib;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public class GreetingListFilter<T extends SortAttribute> {
    @QueryParam("sortAttribute")
    public T sortAttribute;
}
