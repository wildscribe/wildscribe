/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.wildscribe.modeldumper;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

/**
 * A utility which dumps the model of a running container to a file.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ModelExporter {
    private static final ModelNode EMPTY_ADDRESS = new ModelNode().setEmptyList();

    /**
     * Exports the model from a running server to a DMR file.
     *
     * @param configuration the configuration to use
     *
     * @return the path to the DMR file
     *
     * @throws IOException if an error occurs writing the model
     */
    public static Path toDmr(final Configuration configuration) throws IOException {
        final Path target = configuration.getTargetFile();
        try (
                ModelControllerClient client = ModelControllerClient.Factory.create(configuration.getProtocol(), configuration.getHostName(), configuration.getPort());
                OutputStream out = Files.newOutputStream(configuration.getTargetFile())
        ) {

            final ModelNode address = configuration.getAddress();

            final Set<String> requiredExtensions = configuration.getRequiredExtensions();
            if (!requiredExtensions.isEmpty()) {
                final Set<String> present = getExtensions(client);
                for (String ext : present) {
                    requiredExtensions.remove(ext);
                }
            }
            if (!requiredExtensions.isEmpty()) {
                throw new RuntimeException(String.format("Running configuration is missing the following required extensions: %s", requiredExtensions));
            }

            final ModelNode operation = Operations.createOperation("read-resource-description", address);
            operation.get("operations").set(true);
            operation.get("inherited").set(false);
            operation.get("recursive").set(true);
            try {
                final ModelNode result = executeForResult(client, operation);
                result.get("possible-capabilities").set(getPossibleCapabilities(client));
                result.get("version-info").set(getVersionInfo(client));
                result.writeExternal(new DataOutputStream(out));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return target;
    }

    /*Path export();

    Path export(Path target);*/

    private static Set<String> getExtensions(final ModelControllerClient client) throws IOException {
        final ModelNode operation = Operations.createOperation("read-children-names");
        operation.get("child-type").set("extension");
        final ModelNode result = executeForResult(client, operation);
        return result.asList().stream().map(ModelNode::asString).collect(Collectors.toSet());
    }


    private static ModelNode executeForResult(final ModelControllerClient client, final ModelNode operation) throws IOException {
        final ModelNode result = client.execute(operation);
        if (Operations.isSuccessfulOutcome(result)) {
            return Operations.readResult(result);
        }
        throw new RuntimeException(String.format("Failed to execute the operation %s:%n%s", operation, Operations.getFailureDescription(result)));
    }

    private static ModelNode getPossibleCapabilities(final ModelControllerClient client) {
        final ModelNode address = Operations.createAddress("core-service", "capability-registry");
        try {
            return executeForResult(client, Operations.createReadAttributeOperation(address, "possible-capabilities"));
        } catch (Exception e) {
            return new ModelNode().setEmptyList();
        }
    }

    private static ModelNode getVersionInfo(final ModelControllerClient client) {
        final ModelNode operation = Operations.createReadResourceOperation(EMPTY_ADDRESS);
        operation.get("attributes-only").set(true);
        try {
            return executeForResult(client, operation);
        } catch (IOException e) {
            return new ModelNode().setEmptyObject();
        }
    }

}
