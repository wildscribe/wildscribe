package org.jboss.wildscribe.modeldumper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.dmr.ModelNode;

public class Main {

    // Constants are package protected for unit testing
    static final String REQUIRED_EXTENSIONS_PROPERTY = "org.jboss.wildscribe.moduledumper.required-extensions";
    static final List<String> REQUIRED_EXTENSIONS;

    static {
        String required = System.getProperty(REQUIRED_EXTENSIONS_PROPERTY);
        if (required != null && !required.isEmpty()) {
            List<String> requiredList = new ArrayList<>();
            for (String s : required.split(",")) {
                requiredList.add(s.trim());
            }
            REQUIRED_EXTENSIONS = Collections.unmodifiableList(requiredList);
        } else {
            REQUIRED_EXTENSIONS = Collections.emptyList();
        }
    }

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
