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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.dmr.ModelNode;

/**
 * The configuration used to export the model of a running server to a DMR file.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings({"WeakerAccess", "PublicMethodNotExposedInInterface", "UnusedReturnValue"})
public class Configuration {

    private final Path targetFile;
    private final Set<String> requiredExtensions;
    private String protocol;
    private String hostName;
    private int port;
    private ModelNode address;

    @SuppressWarnings("MagicNumber")
    private Configuration(final Path targetFile) {
        this.targetFile = targetFile;
        this.requiredExtensions = new LinkedHashSet<>();
        this.protocol = null;
        this.hostName = "localhost";
        this.port = 9990;
        this.address = new ModelNode().setEmptyList();
    }

    /**
     * Creates a new configuration for the {@linkplain ModelExporter model exporter}.
     *
     * @param targetFile the file to write the model to
     *
     * @return a new configuration
     */
    public static Configuration of(final Path targetFile) {
        return new Configuration(targetFile);
    }

    /**
     * The address where the model should be dumped from. The default is {@code null}.
     *
     * @return the model where the model should be dumped from or {@code null}
     */
    public ModelNode getAddress() {
        return address;
    }

    /**
     * Sets the address where the model should be dumped from.
     *
     * @param address the address where the model should be dumped from or {@code null} for the root resource
     *
     * @return this configuration
     */
    public Configuration setAddress(final ModelNode address) {
        this.address = address;
        return this;
    }

    /**
     * Returns the host name used for the management connection. The default is {@code localhost}.
     *
     * @return the management host name
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Sets the host name used for the management connection.
     *
     * @param hostName the host name
     *
     * @return this configuration
     */
    public Configuration setHostName(final String hostName) {
        this.hostName = hostName == null ? "localhost" : hostName;
        return this;
    }

    /**
     * Returns the management port fot the client to connect to. The default is {@code 9990}.
     *
     * @return the management port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the management port for the client to connect to.
     *
     * @param port the management port
     *
     * @return this configuration
     */
    public Configuration setPort(final int port) {
        this.port = port;
        return this;
    }

    /**
     * Returns the protocol used for the management connection. The default is {@code null}.
     *
     * @return the management protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Sets the protocol used for the management connection.
     *
     * @param protocol the protocol or {@code null} to use a default value
     *
     * @return this configuration
     */
    public Configuration setProtocol(final String protocol) {
        this.protocol = protocol;
        return this;
    }

    /**
     * Adds a required extension to validate the extension has been added to the running server.
     *
     * @param name the extension name
     *
     * @return this configuration
     */
    public Configuration addRequiredExtension(final String name) {
        requiredExtensions.add(name);
        return this;
    }

    /**
     * Adds the required extensiosn to validate the extensions have been added to the running server.
     *
     * @param names the extension names
     *
     * @return this configuration
     */
    public Configuration addRequiredExtensions(final String... names) {
        Collections.addAll(requiredExtensions, names);
        return this;
    }

    /**
     * Adds the required extensions to validate the extensions have been added to the running server.
     *
     * @param names the extension names
     *
     * @return this configuration
     */
    public Configuration addRequiredExtensions(final Collection<String> names) {
        requiredExtensions.addAll(names);
        return this;
    }

    /**
     * Returns the required extensions to validate they have been added to the running server.
     *
     * @return the extensions
     */
    public Set<String> getRequiredExtensions() {
        return new LinkedHashSet<>(requiredExtensions);
    }

    /**
     * Returns the target file to write the model to.
     *
     * @return the target file
     */
    public Path getTargetFile() {
        return targetFile;
    }
}
