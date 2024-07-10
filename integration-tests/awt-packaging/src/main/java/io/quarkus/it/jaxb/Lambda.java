package io.quarkus.it.jaxb;

import java.io.StringReader;

import jakarta.inject.Named;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

@Named("test")
public class Lambda implements RequestHandler<String, String> {

    private static final Logger LOGGER = Logger.getLogger(Lambda.class);

    @Override
    public String handleRequest(String input, Context context) {
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(Book.class);
            final Book book = (Book) jaxbContext.createUnmarshaller().unmarshal(new StringReader(input));
            return String.valueOf(book.getCover().getHeight(null));
        } catch (JAXBException e) {
            context.getLogger().log("Error: " + e.getMessage());
            LOGGER.error(e);
        }
        return null;
    }
}
