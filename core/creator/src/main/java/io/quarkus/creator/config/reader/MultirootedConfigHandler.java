package io.quarkus.creator.config.reader;

/**
 *
 * @author Alexey Loubyansky
 */
public class MultirootedConfigHandler extends MappedPropertiesHandler<Object> {

    @Override
    public Object getTarget() {
        return null;
    }
}
