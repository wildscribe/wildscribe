package org.jboss.wildscribe.modeldumper;

import static org.jboss.as.controller.client.helpers.ClientConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.RECURSIVE;
import static org.jboss.as.controller.client.helpers.ClientConstants.RESULT;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.xnio.IoUtils;

public class Main {

    public static void main(final String[] args) throws IOException {

        if(args.length != 1) {
            System.out.println("USAGE: java [-Dpath=dump-path-in-cli-format] -jar model-dumper.jar target-file");
            System.exit(1);
        }

        ModelControllerClient client = connectDefault();

        ModelNode addr = new ModelNode();
        String address = System.getProperty("path");
        if(address != null) {
            String[] parts = address.split("/");
            for(String p : parts) {
                String[] kv = p.split("=");
                if(kv.length != 2) {
                    throw new RuntimeException("invalid address " + address);
                }
                addr.add(kv[0], kv[1]);
            }
        }

        try {
            validateRunningConfiguration(client);
            final OutputStream out = new FileOutputStream(args[0]);

            final ModelNode operation = new ModelNode();
            operation.get(OP).set("read-resource-description");
            operation.get(RECURSIVE).set(true);
            operation.get("operations").set(true);
            operation.get("inherited").set(false);
            operation.get(OP_ADDR).set(addr);
            try {
                ModelNode result = executeForResult(client, operation);
                result.get("possible-capabilities").set(getPossibleCapabilites(client));
                result.get("version-info").set(getVersionInfo(client));
                
                result.writeExternal(new DataOutputStream(out));
                //result.writeString(new PrintWriter(Files.newBufferedWriter(Paths.get("c:\\temp\\test.dmr"))), false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            IoUtils.safeClose(client);
        }
    }

    //this are usual missing subsystems, this might change over time
    private static List<String> REQUIRED_EXTENSIONS = Arrays.asList("org.wildfly.extension.picketlink", "org.jboss.as.xts", "org.jboss.as.clustering.jgroups");

    private static void validateRunningConfiguration(ModelControllerClient client) throws IOException {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-children-names");
        operation.get("child-type").set("extension");
        operation.get(OP_ADDR).set(new ModelNode());
        ModelNode result = executeForResult(client, operation);
        List<String> present = result.asList().stream().map(ModelNode::asString).collect(Collectors.toList());
        for (String ext : REQUIRED_EXTENSIONS){
            if (!present.contains(ext)){
                throw new RuntimeException(String.format("Running configuration is missing '%s' extension",ext));
            }
        }
    }
    private static ModelNode getPossibleCapabilites(ModelControllerClient client) throws IOException {
        ModelNode addr = new ModelNode();
        addr.add("core-service","capability-registry");

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-attribute");
        operation.get("name").set("possible-capabilities");
        operation.get(OP_ADDR).set(addr);
        try {
            return executeForResult(client, operation);
        } catch (Exception e) {
            return new ModelNode().setEmptyList();
        }
    }

    private static ModelNode getVersionInfo(ModelControllerClient client) throws IOException {
        ModelNode addr = new ModelNode();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-resource");
        operation.get("attributes-only").set(true);
        operation.get(OP_ADDR).set(addr);
        try {
            return executeForResult(client, operation);
        } catch (IOException e) {
            return new ModelNode().setEmptyObject();
        }
    }


    private static ModelControllerClient connectDefault() {
        ModelControllerClient client = null;
        try {
            client = ModelControllerClient.Factory.create("http-remoting", "localhost", 9990);

            final ModelNode operation = new ModelNode();
            operation.get(OP).set("read-resource-description");
            operation.get(RECURSIVE).set(false);
            operation.get("operations").set(true);
            operation.get("inherited").set(false);
            operation.get(OP_ADDR).set(new ModelNode());
            client.execute(operation);
            return client;
        } catch (Exception e) {
            IoUtils.safeClose(client);
            try {
                return ModelControllerClient.Factory.create("remote", "localhost", 9999);
            } catch (Exception e1) {
                throw new RuntimeException(e);
            }
        }

    }

    private static ModelControllerClient connect(String uri) {
        try {
            URI connect = new URI(uri);
            return ModelControllerClient.Factory.create(connect.getScheme(), connect.getHost(), connect.getPort());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ModelNode executeForResult(final ModelControllerClient client, final ModelNode operation) throws IOException {
        final ModelNode result = client.execute(operation);
        checkSuccessful(result);
        return result.get(RESULT);
    }

    private static void checkSuccessful(final ModelNode result) {
        if (!SUCCESS.equals(result.get(OUTCOME).asString())) {
            throw new RuntimeException(result.get(
                    FAILURE_DESCRIPTION).toString());
        }
    }
}
