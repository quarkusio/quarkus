package io.quarkus.qute.deployment.i18n;

import static io.quarkus.qute.i18n.Message.UNDERSCORED_ELEMENT_NAME;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle
public interface AppMessages {

    @Message("Hello world!")
    String hello();

    @Message("Hello {name}!")
    String hello_name(String name);

    @Message("Hello {name} {surname}!")
    String hello_fullname(String name, String surname);

    // key=hello_with_if_section
    @Message(key = UNDERSCORED_ELEMENT_NAME, value = "{#if count eq 1}"
            + "{msg:hello_name('you guy')}"
            + "{#else}"
            + "{msg:hello_name('you guys')}"
            + "{/if}")
    String helloWithIfSection(int count);

    @Message("Item name: {item.name}, age: {item.age}")
    String itemDetail(Item item);

    @Message(key = "dot.test", value = "Dot test!")
    String dotTest();

    @Message("There {msg:fileStrings(numberOfFiles)} on {disk}.")
    String files(int numberOfFiles, String disk);

    @Message("{#when numberOfFiles}"
            + "{#is 0}are no files"
            + "{#is 1}is one file"
            + "{#else}are {numberOfFiles} files"
            + "{/when}")
    String fileStrings(int numberOfFiles);

}
