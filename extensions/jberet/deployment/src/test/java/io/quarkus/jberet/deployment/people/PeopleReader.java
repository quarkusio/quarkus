package io.quarkus.jberet.deployment.people;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;

import javax.batch.api.chunk.ItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

@Named
@Dependent
public class PeopleReader implements ItemReader {

    private BufferedReader reader;

    @Override
    public void open(Serializable checkpoint) {
        // no checkpointing support in this basic example
        InputStream inputStream = this.getClass().getResourceAsStream("/people.txt");
        reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object readItem() throws IOException {
        String name = reader.readLine();
        if (name != null) {
            return new Person(name);
        } else {
            // reached the end of file
            return null;
        }
    }

    @Override
    public Serializable checkpointInfo() {
        // no checkpointing support in this basic example
        return null;
    }
}
