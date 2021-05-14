package io.quarkus.devtools.generators.file;

import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.generators.kinds.model.Model;
import io.quarkus.devtools.messagewriter.MessageIcons;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import org.apache.maven.project.MavenProject;

public class FileGenerator {

    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RESET = "\u001B[0m";
    private final MavenProject mavenProject;
    public final QuarkusCommandInvocation quarkusCommandInvocation;

    public FileGenerator(QuarkusCommandInvocation quarkusCommandInvocation, MavenProject mavenProject) {
        this.quarkusCommandInvocation = quarkusCommandInvocation;
        this.mavenProject = mavenProject;
    }

    public void execute(FileItem fileItem) {
        try {
            String fileName = fileItem.destination + "/" + fileItem.name + fileItem.extesion;

            if (!verifyIfFileAlReadyExists(fileName, fileItem.showName)) {
                return;
            }
            createDirectoryIfNotExists(fileItem.destination);

            FileOutputStream out = new FileOutputStream(fileName);
            out.write(fileItem.output.getBytes());
            out.close();

            quarkusCommandInvocation.log()
                    .info(MessageIcons.OK_ICON + ANSI_GREEN + " created " + ANSI_RESET + fileItem.showName);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public void generateFile(Model model, String content, String folderDestination) {
        String groupId = mavenProject.getGroupId().replace(".", "/");
        String structureJava = "/src/main/java/";

        String destination = quarkusCommandInvocation.getQuarkusProject().getProjectDirPath().toString() + structureJava
                + groupId + folderDestination;

        FileItem fileItem = new FileItem();
        fileItem.extesion = ".java";
        fileItem.destination = destination;
        fileItem.output = content;
        fileItem.name = model.getClassName();
        fileItem.showName = groupId + folderDestination + "/" + fileItem.name + fileItem.extesion;

        this.execute(fileItem);
    }

    private boolean verifyIfFileAlReadyExists(String fileName, String showname) {
        if (new File(fileName).exists()) {
            Scanner ss = new Scanner(System.in);
            System.out.println("File " + showname + " already exists you want to overwrite? (y/n)");

            String s = ss.nextLine();
            return s.trim().equals("y");
        }
        return true;
    }

    private void createDirectoryIfNotExists(String destination) {
        Path path = Paths.get(destination);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            quarkusCommandInvocation.log().error(MessageIcons.ERROR_ICON + " " + e.getMessage());
        }
    }
}
