package org.wildscribe.logs;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] params) throws Exception {
        if (params.length != 2) {
            System.out.println("Usage: dumper.jar path-to-modules output-file");
            System.exit(1);
        }
        Path moduleDir = Paths.get(params[0]);
        final Path target = Paths.get(params[1]);
        MessageExporter.export(moduleDir, target);
    }

}
