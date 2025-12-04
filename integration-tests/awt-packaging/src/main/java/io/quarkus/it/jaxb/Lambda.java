package io.quarkus.it.jaxb;

import java.io.StringReader;

import jakarta.inject.Named;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.it.jaxb.Book.Cover;

@Named("test")
public class Lambda implements RequestHandler<Book, String> {

    @Override
    public String handleRequest(Book book, Context context) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Book.Cover.class);
            Book.Cover cover = (Cover) jaxbContext.createUnmarshaller()
                    .unmarshal(new StringReader("<cover><image>" + book.getCover() + "</image></cover>"));
            return String.valueOf(cover.getImage().getHeight(null));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
