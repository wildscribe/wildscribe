/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.wildscribe.site;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.logging.Logger;

/**
 * @author Stuart Douglas
 */
class FileUtils {

    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getPackage().getName());

    private FileUtils() {

    }

    static Path getUserHome() {
        return Paths.get(System.getProperty("user.home"));
    }

    static Path getPath(final String path) {
        if (path.charAt(0) == '~') {
            if (path.charAt(1) == File.separatorChar) {
                return getUserHome().resolve(path.substring(2)).toAbsolutePath();
            } else {
                return getUserHome().resolve(path.substring(1)).toAbsolutePath();
            }
        }
        return Paths.get(path);
    }

    static Path createTempDir(final String path) {
        return Paths.get(System.getProperty("java.io.tmpdir"), path);
    }

    static void delete(final Path dir, final boolean ignoreHidden) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                return Files.isHidden(dir) && ignoreHidden ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                if (!(Files.isHidden(file) && ignoreHidden)) {
                    Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path d, final IOException exc) throws IOException {
                if (!dir.equals(d)) {
                    Files.delete(d);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static Path copyDirectoryFromJar(final URL resource) throws IOException {
        final Path tmpDir = FileUtils.createTempDir("wildscribe_templates");
        if (Files.exists(tmpDir)) {
            delete(tmpDir, false);
        }
        copyDirectoryFromJar(resource, tmpDir);
        return tmpDir;
    }


    static void copyDirectoryFromJar(final URL resource, final Path dest) throws IOException {
        LOGGER.debugf("Copying %s to %s", resource, dest);
        if ("file".equals(resource.getProtocol())) {
            try {
                final Path src = Paths.get(resource.toURI());
                Files.walkFileTree(src, new CopyDirVisitor(src, dest, StandardCopyOption.REPLACE_EXISTING));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else if ("jar".equals(resource.getProtocol())) {
            int endIndex = resource.getFile().indexOf('!');
            JarFile file = new JarFile(resource.getFile().substring(5, endIndex));
            String path = resource.getPath().substring(endIndex + 2);
            Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    if (entry.getName().startsWith(path)) {
                        LOGGER.debugf("Copying %s", entry.getName().substring(path.length() + 1));
                        Path fileDest = dest.resolve(entry.getName().substring(path.length() + 1));
                        if (Files.notExists(fileDest.getParent())) {
                            Files.createDirectories(fileDest.getParent());
                        }
                        Files.copy(file.getInputStream(entry), fileDest);
                    }
                }
            }
        } else {
            throw new RuntimeException("Unknown scheme " + resource.getProtocol());
        }
    }

    private static class CopyDirVisitor extends SimpleFileVisitor<Path> {
        private final Path fromPath;
        private final Path toPath;
        private final CopyOption copyOption;

        CopyDirVisitor(Path fromPath, Path toPath, CopyOption copyOption) {
            this.fromPath = fromPath;
            this.toPath = toPath;
            this.copyOption = copyOption;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path targetPath = toPath.resolve(fromPath.relativize(dir));
            if (!Files.exists(targetPath)) {
                Files.createDirectory(targetPath);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, toPath.resolve(fromPath.relativize(file)), copyOption);
            return FileVisitResult.CONTINUE;
        }
    }

}
