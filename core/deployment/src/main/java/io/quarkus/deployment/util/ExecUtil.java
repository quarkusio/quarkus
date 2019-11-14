
package io.quarkus.deployment.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ExecUtil {

    /**
     * Execute the specified command from within the current directory.
     * 
     * @param command The command
     * @param args The command arguments
     * @return true if commands where executed successfully
     */
    public static boolean exec(String command, String... args) {
        return exec(new File("."), command, args);
    }

    /**
     * Execute the specified command from within the specified directory.
     * 
     * @param directory The directory
     * @param command The command
     * @param args The command arguments
     * @return true if commands where executed successfully
     */
    public static boolean exec(File directory, String command, String... args) {
        Process process = null;
        try {
            String[] cmd = new String[args.length + 1];
            cmd[0] = command;
            if (args.length > 0) {
                System.arraycopy(args, 0, cmd, 1, args.length);
            }
            process = new ProcessBuilder()
                    .directory(directory)
                    .command(cmd)
                    .redirectErrorStream(true)
                    .start();

            try (InputStreamReader isr = new InputStreamReader(process.getInputStream());
                    BufferedReader reader = new BufferedReader(isr)) {

                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    System.out.println(line);
                }
                process.waitFor();
            }
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            return false;
        }
        if (process != null) {
            return process.exitValue() == 0;
        }
        return false;
    }
}
