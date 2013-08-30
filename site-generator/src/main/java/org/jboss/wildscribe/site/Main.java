package org.jboss.wildscribe.site;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String VERSION = "versions.txt";

    public static void main(final String[] args) {
        try {
            if (args.length != 2) {
                System.out.println("USAGE: java -jar site-generator.jar model-directory output-directory");
                System.exit(1);
            }
            File modelDir = new File(args[0]);

            final File target = new File(args[1]);
            FileUtils.deleteRecursive(target);
            target.mkdirs();
            System.out.print("Generating site in " + target.getAbsolutePath());

            copyResources(target);

            List<Version> versions = loadVersions(modelDir);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void copyResources(File target) throws IOException {
        FileUtils.copyDirectoryFromJar(Main.class.getClassLoader().getResource("staticresources"), target);
        FileUtils.copyDirectoryFromJar(Main.class.getClassLoader().getResource("templates"), target);
    }


    private static List<Version> loadVersions(File modelDir) {
        final List<Version> ret = new ArrayList<Version>();
        final String versionsString = FileUtils.readFile(new File(modelDir, VERSION));
        final String[] versionParts = versionsString.split("\n");
        for (final String version : versionParts) {
            final String[] parts = version.split(":");
            ret.add(new Version(parts[0], parts[1], new File(modelDir, parts[2])));
        }
        return ret;
    }


}
