package ${package};

import jakarta.inject.Named;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;


@Named("stream")
public class StreamLambda implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        int letter;
        while ((letter = inputStream.read()) != -1) {
            int character = Character.toUpperCase(letter);
            outputStream.write(character);
        }
    }
}
