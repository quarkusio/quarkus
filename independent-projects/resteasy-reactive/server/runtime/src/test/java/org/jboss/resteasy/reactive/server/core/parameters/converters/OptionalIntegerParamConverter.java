package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.util.Optional;

import jakarta.ws.rs.ext.ParamConverter;

public class OptionalIntegerParamConverter implements ParamConverter<Optional<Integer>> {

	@Override
    public Optional<Integer> fromString(String value) {
		if (value == null) {
			return null;
		}
		
        if (value.trim().isEmpty()) {
        	return Optional.empty();
        }
        
        try {
        	int parsedInt = Integer.parseInt(value);
        	return Optional.of(parsedInt);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value");
        }
    }
    @Override
    public String toString(Optional<Integer> value) {
    	if (!value.isPresent()) {
    		return null;
    	}
    	
    	Integer intValue = value.get();
        if (intValue == null) {
            return null;
        } else {
            return intValue.toString();
        }
    }

}
