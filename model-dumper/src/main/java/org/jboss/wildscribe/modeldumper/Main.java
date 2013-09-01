package org.jboss.wildscribe.modeldumper;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.xnio.IoUtils;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import static org.jboss.as.controller.client.helpers.ClientConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.RECURSIVE;
import static org.jboss.as.controller.client.helpers.ClientConstants.RESULT;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;

public class Main {

    public static void main(final String[] args) {

        if(args.length != 1) {
            System.out.println("USAGE: java -jar model-dumper.jar target-file");
            System.exit(1);
        }

        ModelControllerClient client = connectDefault();

        try {

            final OutputStream out = new FileOutputStream(args[0]);

            final ModelNode operation = new ModelNode();
            operation.get(OP).set("read-resource-description");
            operation.get(RECURSIVE).set(true);
            operation.get("operations").set(true);
            operation.get(OP_ADDR).set(new ModelNode());
            try {
                ModelNode result = executeForResult(client, operation);
                result.writeExternal(new DataOutputStream(out));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            IoUtils.safeClose(client);
        }
    }

    private static ModelControllerClient connectDefault() {
        try {
            ModelControllerClient client = ModelControllerClient.Factory.create("http-remoting", "localhost", 9990);

            final ModelNode operation = new ModelNode();
            operation.get(OP).set("read-resource-description");
            operation.get(RECURSIVE).set(false);
            operation.get("operations").set(true);
            operation.get(OP_ADDR).set(new ModelNode());
            client.execute(operation);
            return client;
        } catch (Exception e) {
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
