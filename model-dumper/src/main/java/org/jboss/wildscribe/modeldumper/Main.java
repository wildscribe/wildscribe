package org.jboss.wildscribe.modeldumper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.jboss.dmr.ModelNode;

public class Main {
    private static final List<String> REQUIRED_EXTENSIONS = Arrays.asList("org.wildfly.extension.datasources-agroal", "org.wildfly.extension.rts", "org.jboss.as.xts", "org.jboss.as.clustering.jgroups");

    public static void main(final String[] args) throws IOException {

        if (args.length != 1) {
            System.out.println("USAGE: java [-Dpath=dump-path-in-cli-format] -jar model-dumper.jar target-file");
            System.exit(1);
        }

        final Path target = Paths.get(args[0]);
        final Configuration configuration = Configuration.of(target)
                .addRequiredExtensions(REQUIRED_EXTENSIONS);

        final ModelNode addr = new ModelNode().setEmptyList();
        final String address = System.getProperty("path");
        if (address != null) {
            String[] parts = address.split("/");
            for (String p : parts) {
                String[] kv = p.split("=");
                if (kv.length != 2) {
                    throw new RuntimeException("invalid address " + address);
                }
                addr.add(kv[0], kv[1]);
            }
        }
        configuration.setAddress(addr);
        ModelExporter.toDmr(configuration);
    }
}
