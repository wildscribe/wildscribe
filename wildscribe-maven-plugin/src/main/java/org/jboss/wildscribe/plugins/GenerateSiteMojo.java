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

package org.jboss.wildscribe.plugins;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.wildscribe.modeldumper.Configuration;
import org.jboss.wildscribe.modeldumper.ModelExporter;
import org.jboss.wildscribe.site.Generator;
import org.wildfly.core.launcher.CommandBuilder;
import org.wildfly.core.launcher.Launcher;
import org.wildfly.core.launcher.StandaloneCommandBuilder;
import org.wildfly.plugin.core.ContainerDescription;
import org.wildfly.plugin.core.ServerHelper;
import org.wildscribe.logs.MessageExporter;

/**
 * A simple plugin used to generate a Wildscribe site.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Mojo(name = "generate-site")
@SuppressWarnings("InstanceVariableMayNotBeInitialized")
public class GenerateSiteMojo extends AbstractMojo {
    static {
        // Add a hint for JBoss Logging to use slf4j which is what Maven uses
        System.setProperty("org.jboss.logging.provider", "slf4j");
    }

    /**
     * The application server's home directory. This is a required parameter and must point to a valid server directory.
     * The server must include the module JAR's in order to get the log messages for the generated site.
     */
    @Parameter(alias = "jboss-home", property = "wildscribe.jboss.home", required = true)
    private String jbossHome;

    /**
     * The protocol used to connect the management client.
     */
    @Parameter(property = "wildscribe.management.protocol")
    private String protocol;

    /**
     * Specifies the host name for the management client.
     */
    @Parameter(defaultValue = "localhost", property = "wildscribe.management.hostname")
    private String hostname;

    /**
     * Specifies the port number for the management client.
     */
    @Parameter(defaultValue = "9990", property = "wildscribe.management.port")
    private int port;

    /**
     * The JVM options used for starting the server.
     */
    @Parameter(alias = "java-opts", property = "wildscribe.java.opts")
    private String[] javaOpts;

    /**
     * The path to the server configuration to use.
     */
    @Parameter(alias = "server-config", defaultValue = "standalone-full-ha.xml", property = "wildscribe.server.config")
    private String serverConfig;

    /**
     * The target directory the model and message files will be written to.
     */
    @Parameter(alias = "model-dir", defaultValue = "${project.build.directory}/wildscribe", property = "wildscribe.model.dir")
    private String modelDir;

    /**
     * The target directory the site to be generated in.
     */
    @Parameter(alias = "site-dir", defaultValue = "${project.build.directory}/wildscribe", property = "wildscribe.site.dir")
    private String siteDir;

    /**
     * This is only used for a single version and will be used for the displayed name. If left {@code null} the name
     * will be resolved from the running server.
     */
    @Parameter(alias = "display-name", property = "wildscribe.display.name")
    private String displayName;

    /**
     * This is only used for a single version and will be used for the displayed version. If left {@code null} the
     * version will be resolved from the running server.
     */
    @Parameter(alias = "display-version", property = "wildscribe.display.version")
    private String displayVersion;

    /**
     * The extensions that are required to be installed before the model is dumped. If any of these extensions do not
     * already exist on the server then they will be added.
     */
    @Parameter(alias = "required-extensions", property = "wildscribte.required.extensions")
    private List<String> requiredExtensions;

    /**
     * Set to {@code true} if you want this goal to be skipped, otherwise {@code false}.
     */
    @Parameter(defaultValue = "false", property = "wildscribe.skip")
    private boolean skip;

    /**
     * The timeout, in seconds, to wait for a management connection.
     */
    @Parameter(defaultValue = "60", property = "wildscribe.timeout")
    private int timeout;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            return;
        }

        final Path wildflyHome = Paths.get(jbossHome);
        if (!ServerHelper.isValidHomeDirectory(wildflyHome)) {
            throw new MojoExecutionException(String.format("Invalid directory %s is not a valid JBoss Home directory.", jbossHome));
        }
        final StandaloneCommandBuilder commandBuilder = createCommandBuilder(wildflyHome);
        Process process = null;
        try {
            final Path consoleRedirect = wildflyHome.resolve("standalone").resolve("logs").resolve("console-output.log");
            if (Files.notExists(consoleRedirect.getParent())) {
                Files.createDirectories(consoleRedirect.getParent());
            }
            process = startContainer(commandBuilder, consoleRedirect);
            try (ModelControllerClient client = createClient()) {
                // Get the server configuration to generate the model dump
                final ContainerDescription containerDescription = ServerHelper.getContainerDescription(client);
                final String baseFileName = containerDescription.getProductName().replace(' ', '-')
                        + '-' + containerDescription.getProductVersion();
                final Path dmrFile = createModel(client, baseFileName, containerDescription);

                // Generate the message file
                createMessageFile(wildflyHome, dmrFile.getParent(), baseFileName);

                // Generate the site
                final Path siteTarget = Paths.get(siteDir);
                if (Files.notExists(siteTarget)) {
                    Files.createDirectories(siteTarget);
                }
                Generator.generate(dmrFile, siteTarget, displayName, displayVersion);
            }
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new MojoExecutionException("Failed to generate the Wildscribe sight.", e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Allows the {@link #javaOpts} to be set as a string. The string is assumed to be space delimited.
     *
     * @param value a spaced delimited value of JVM options
     */
    @SuppressWarnings("unused")
    public void setJavaOpts(final String value) {
        if (value != null) {
            javaOpts = value.split("\\s+");
        }
    }

    private StandaloneCommandBuilder createCommandBuilder(final Path wildflyPath) {
        final StandaloneCommandBuilder commandBuilder = StandaloneCommandBuilder.of(wildflyPath)
                .setServerConfiguration(serverConfig);

        if (hostname != null) {
            commandBuilder.setBindAddressHint("management", hostname);
        }
        if (port > 0) {
            commandBuilder.addJavaOptions("-Djboss.management.http.port=" + port);
        }

        // Set the JVM options
        if (javaOpts != null && javaOpts.length > 0) {
            commandBuilder.setJavaOptions(javaOpts);
        }
        return commandBuilder;
    }

    private Process startContainer(final CommandBuilder commandBuilder, final Path consoleRedirct) throws IOException, InterruptedException, TimeoutException {
        final Launcher launcher = Launcher.of(commandBuilder)
                .setRedirectErrorStream(true)
                .redirectOutput(consoleRedirct);
        final Process process = launcher.launch();
        try (ModelControllerClient client = createClient()) {
            ServerHelper.waitForStandalone(process, client, timeout);
        }
        return process;
    }

    private Path createModel(final ModelControllerClient client, final String baseFileName, final ContainerDescription containerDescription) throws MojoExecutionException {
        final Path targetDir = Paths.get(this.modelDir);
        if (Files.notExists(targetDir)) {
            try {
                Files.createDirectories(targetDir);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to create directories for " + targetDir, e);
            }
        }
        final Path dmrFile;
        try {
            final Configuration configuration = Configuration.of(targetDir.resolve(baseFileName + ".dmr"))
                    .setProtocol(protocol)
                    .setHostName(hostname)
                    .setPort(port);


            if (requiredExtensions != null && !requiredExtensions.isEmpty()) {
                configuration.addRequiredExtensions(requiredExtensions);
                // Get a list of the current extensions
                final ModelNode op = Operations.createOperation("read-children-names");
                op.get("child-type").set("extension");
                final Collection<String> extensionsToAdd = new ArrayList<>(requiredExtensions);
                extensionsToAdd.removeAll(executeForResult(client, op, "Failed to read the current extensions")
                        .asList()
                        .stream()
                        .map(ModelNode::asString)
                        .collect(Collectors.toList()));
                final CompositeOperationBuilder builder = CompositeOperationBuilder.create();
                for (String name : extensionsToAdd) {
                    builder.addStep(Operations.createAddOperation(Operations.createAddress("extension", name)));
                }
                executeForResult(client, builder.build(), "Failed to add the following extensions %s", extensionsToAdd);
            }

            // Export the model to DMR
            dmrFile = ModelExporter.toDmr(configuration);

            // We shouldn't need WildFly any more so we can shut it down
            final ModelNode op = Operations.createOperation("shutdown");
            // Gives the server 10 seconds of graceful shutdown. This should be adequate for what is being done.
            op.get("timeout").set(10);
            executeForResult(client, op, "Failed to shutdown the server %s", configuration);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to export the server model " + containerDescription, e);
        }
        return dmrFile;
    }

    private void createMessageFile(final Path wildflyHome, final Path dir, final String baseFileName) throws MojoExecutionException {
        // Generate the message file
        try {
            MessageExporter.export(wildflyHome, dir.resolve(baseFileName + ".messages"));
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create the message file for directory " + wildflyHome, e);
        }
    }

    private ModelControllerClient createClient() throws UnknownHostException {
        return ModelControllerClient.Factory.create(protocol, hostname, port);
    }

    private static ModelNode executeForResult(final ModelControllerClient client, final ModelNode op,
                                              final String failureMessage, final Object... args) throws MojoExecutionException {
        return executeForResult(client, Operation.Factory.create(op), failureMessage, args);
    }

    private static ModelNode executeForResult(final ModelControllerClient client, final Operation op,
                                              final String failureMessage, final Object... args) throws MojoExecutionException {
        try {
            final ModelNode result = client.execute(op);
            if (Operations.isSuccessfulOutcome(result)) {
                return Operations.readResult(result);
            }
            throw new MojoExecutionException(String.format(failureMessage, args) + ": " + Operations.getFailureDescription(result));
        } catch (IOException e) {
            throw new MojoExecutionException(String.format(failureMessage, args), e);
        }
    }
}
