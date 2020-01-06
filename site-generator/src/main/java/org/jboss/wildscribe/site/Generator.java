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

package org.jboss.wildscribe.site;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * A utility used to generate a Wildscribe site.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class Generator {
    private static final Logger LOGGER = Logger.getLogger(Generator.class);

    private static final String VERSION = "versions.txt";
    private static final String STATICRESOURCES = "staticresources";
    private static final String TEMPLATES = "templates";

    /**
     * Generates a site based on the {@code modelPath} in the target directory.
     * <p>
     * If the {@code modelPath} is a directory it must contain a file named {@code version.txt}. This file is
     * delimited with {@code :} on each line where the first value is the product name, the second value is the version
     * and the third value is the path to the DMR file.
     * </p>
     * <p>
     * Example file contents:
     * <pre>
     * {@code
     * WildFly:18.0.0.Final:./WildFly-18.0.0.Final.dmr
     * WildFly:17.0.0.Final:WildFly-17.0.0.Final.dmr
     * }
     * </pre>
     * </p>
     *
     * @param modelPath the path to the model file or a directory with a {@code verions.txt} file
     * @param target    the target directory to generate the site in
     *
     * @return the path to the directory the site was generated in
     *
     * @throws IOException if an error occurs generating the site
     */
    public static Path generate(final Path modelPath, final Path target) throws IOException {
        final List<Version> versions;
        if (Files.isDirectory(Objects.requireNonNull(modelPath, "The target cannot be null."))) {
            versions = loadVersions(modelPath);
        } else {
            versions = Collections.singletonList(readVersionFromDmr(modelPath));
        }
        return generate(versions, target);
    }

    /**
     * Generates a site based on the {@code modelPaths} in the target directory.
     * <p>
     * If any of the {@code modelPaths} are directories they must contain a file named {@code version.txt}. This file is
     * delimited with {@code :} on each line where the first value is the product name, the second value is the version
     * and the third value is the path to the DMR file.
     * </p>
     * <p>
     * Example file contents:
     * <pre>
     * {@code
     * WildFly:18.0.0.Final:./WildFly-18.0.0.Final.dmr
     * WildFly:17.0.0.Final:WildFly-17.0.0.Final.dmr
     * }
     * </pre>
     * </p>
     *
     * @param modelPaths the paths to process
     * @param target     the target directory to generate the site in
     *
     * @return the path to the directory the site was generated in
     *
     * @throws IOException if an error occurs generating the site
     */
    public static Path generate(final Collection<Path> modelPaths, final Path target) throws IOException {
        if (modelPaths.isEmpty()) {
            throw new IllegalArgumentException("No module paths were defined.");
        }
        final List<Version> versions = new ArrayList<>(modelPaths.size());
        if (modelPaths.size() > 1) {
            for (Path modelPath : modelPaths) {
                if (Files.isDirectory(modelPath)) {
                    versions.addAll(loadVersions(modelPath));
                } else {
                    versions.add(readVersionFromDmr(modelPath));
                }
            }
        } else {
            versions.add(readVersionFromDmr(modelPaths.iterator().next()));
        }
        return generate(versions, target);
    }

    private static Path generate(final List<Version> versions, final Path target) throws IOException {

        if (Files.notExists(target)) {
            Files.createDirectories(target);
        } else {
            FileUtils.delete(target, true);
        }
        final Path templateDir = FileUtils.copyDirectoryFromJar(getResource(TEMPLATES));
        LOGGER.infof("Generating site in %s", target);

        FileUtils.copyDirectoryFromJar(getResource(STATICRESOURCES), target);
        Configuration configuration = createFreemarkerConfig(templateDir);

        try {
            SiteGenerator siteGenerator = new SiteGenerator(versions, configuration, target);
            if (versions.size() > 1) {
                siteGenerator.createMainPage();
                siteGenerator.createAboutPage();
                siteGenerator.createVersions();
            } else {
                siteGenerator.createSingleVersion();
            }
        } catch (TemplateException e) {
            throw new RuntimeException(e);
        }
        return target;
    }


    private static Version readVersionFromDmr(final Path path) throws IOException {
        final ModelNode fullModel = new ModelNode();
        try (InputStream in = Files.newInputStream(path)) {
            fullModel.readExternal(in);
        }
        if (!fullModel.hasDefined("version-info")) {
            return new Version("unknown", "unknown", path.toFile());
        }
        ModelNode model = fullModel.get("version-info");
        String productName = model.get("product-name").asString();
        String productVersion = model.get("product-version").asString();
        return new Version(productName, productVersion, path.toFile());

    }

    private static Configuration createFreemarkerConfig(final Path templateDir) throws IOException {
        final freemarker.template.Version freemakerVersion = new freemarker.template.Version(2, 3, 20);  // FreeMarker 2.3.20
        Configuration cfg = new Configuration(freemakerVersion);
        cfg.setDirectoryForTemplateLoading(templateDir.toFile());
        cfg.setObjectWrapper(new DefaultObjectWrapper(freemakerVersion));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setURLEscapingCharset("UTF-8");
        return cfg;
    }


    private static List<Version> loadVersions(final Path modelDir) throws IOException {
        final List<Version> ret = new ArrayList<>();
        final List<String> versions = Files.readAllLines(modelDir.resolve(VERSION), StandardCharsets.UTF_8);
        for (final String version : versions) {
            final String[] parts = version.split(":");
            ret.add(new Version(parts[0], parts[1], modelDir.resolve(parts[2]).toFile()));
        }
        return ret;
    }

    private static URL getResource(final String path) {
        URL url = Generator.class.getClassLoader().getResource(path);
        if (url == null) {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                url = cl.getResource(path);
            }
        }
        if (url == null) {
            throw new RuntimeException("Failed to locate resource " + path);
        }
        return url;
    }
}
