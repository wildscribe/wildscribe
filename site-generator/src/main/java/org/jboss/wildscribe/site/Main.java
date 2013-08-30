package org.jboss.wildscribe.site;

import org.jboss.dmr.ModelNode;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

public class Main {

    public static void main(final String[] args) {
        try {
            if (args.length != 2) {
                System.out.println("USAGE: java -jar site-generator.jar model-file.txt output-directory");
                System.exit(1);
            }
            File modelFile = new File(args[0]);

            final File target = new File(args[1]);
            target.mkdirs();
            System.out.print("Generating site in " + target.getAbsolutePath());

            ModelNode node = new ModelNode();
            node.readExternal(new BufferedInputStream(new FileInputStream(modelFile)));

            System.out.println(node);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
