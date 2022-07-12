//usr/bin/env jbang "$0" "$@" ; exit $?

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class script {

    public static void main(String[] args) {

        Path currDir = Path.of(System.getProperty("user.dir"));

        File apiChangesFile = currDir.resolve("api-changes.xml").toFile();
        File apiSuggestionsFile = currDir.resolve("target/api-changes-suggestions.xml").toFile();


        if(!apiSuggestionsFile.exists()){
            throw new IllegalStateException("api-changes-suggestions.xml file does not exist. Run revapi first.");
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document targetDoc;

            if(!apiChangesFile.exists())
                targetDoc = createApiChangesXmlDoc(db,apiChangesFile);

            else
                targetDoc = loadApiChangesXmlDoc(db,apiChangesFile);

            Document sourceDoc = loadApiSuggestionsXmlDoc(db, apiSuggestionsFile);

            copyNodes(sourceDoc, targetDoc, apiChangesFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Document createApiChangesXmlDoc(DocumentBuilder db, File file) throws Exception{

        Document document = db.newDocument();

        Element rootElement = document.createElement("versions");
        document.appendChild(rootElement);

        Element versionNumber = document.createElement("v999.0.0");
        rootElement.appendChild(versionNumber);

        Element revapiDifferences = document.createElement("revapi.differences");
        versionNumber.appendChild(revapiDifferences);

        Element criticality = document.createElement("criticality");
        criticality.appendChild(document.createTextNode("documented"));

        Element justification = document.createElement("justification");
        justification.appendChild(document.createTextNode("ADD YOUR EXPLANATION FOR THE NECESSITY OF THIS CHANGE"));

        revapiDifferences.appendChild(criticality);
        revapiDifferences.appendChild(justification);

        Element differences = document.createElement("differences");
        revapiDifferences.appendChild(differences);

        writeToXmlFile(document, file);


        return document;
    }

    private static Document loadApiChangesXmlDoc(DocumentBuilder db, File file) throws Exception{

        return db.parse(file);
    }

    private static Document loadApiSuggestionsXmlDoc(DocumentBuilder db, File file) throws Exception{

        Enumeration<InputStream> inputStreams = Collections.enumeration(
                Arrays.asList(new InputStream[] {
                        new ByteArrayInputStream("<root>".getBytes()),
                        new FileInputStream(file),
                        new ByteArrayInputStream("</root>".getBytes()),
                }));

        SequenceInputStream sequenceStream = new SequenceInputStream(inputStreams);

        return db.parse(sequenceStream);

    }

    private static void writeToXmlFile(Document document, File file) throws Exception{

        TransformerFactory tff = TransformerFactory.newInstance();
        Transformer tf = tff.newTransformer();

        tf.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(document);

        StreamResult target = new StreamResult(file);

        tf.transform(source, target);
    }

    private static void copyNodes(Document sourceDoc, Document targetDoc, File file) throws Exception{

        Node differencesNode = targetDoc.getDocumentElement().getElementsByTagName("differences").item(0);

        NodeList items = sourceDoc.getDocumentElement().getElementsByTagName("item");

        for(int i=0; i<items.getLength(); i++){

            Element node = (Element) items.item(i);

            // remove ignore and justification nodes
            node.removeChild(node.getElementsByTagName("ignore").item(0));
            node.removeChild(node.getElementsByTagName("justification").item(0));

            // node copying
            Node importNode = targetDoc.importNode(node, true);
            differencesNode.appendChild(importNode);
        }

        writeToXmlFile(targetDoc,file);
    }

}