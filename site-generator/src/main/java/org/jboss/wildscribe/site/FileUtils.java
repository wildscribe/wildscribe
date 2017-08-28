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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

/**
 * @author Stuart Douglas
 */
public class FileUtils {

    private FileUtils() {

    }

    public static String readFile(Class<?> testClass, String fileName) {
        final URL res = testClass.getResource(fileName);
        return readFile(res);
    }

    public static String readFile(URL url) {
        try {
            return readFile(url.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readFile(final File file) {
        try {
            return readFile(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readFile(InputStream file) {
        BufferedInputStream stream = null;
        try {
            stream = new BufferedInputStream(file);
            byte[] buff = new byte[1024];
            StringBuilder builder = new StringBuilder();
            int read = -1;
            while ((read = stream.read(buff)) != -1) {
                builder.append(new String(buff, 0, read));
            }
            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    public static void copyDirectory(final Path src, final Path dest) throws IOException {
        Files.walkFileTree(src, new CopyDirVisitor(src, dest, StandardCopyOption.REPLACE_EXISTING));
    }


    public static void copyDirectoryFromJar(final URL resource, final Path dest) throws Exception {
        System.out.println(resource + " ---- " + dest);
        if (resource.getProtocol().equals("file")) {

            copyDirectory(Paths.get(resource.toURI()), dest);
        } else if (resource.getProtocol().equals("jar")) {
            int endIndex = resource.getFile().indexOf('!');
            JarFile file = new JarFile(resource.getFile().substring(5, endIndex));
            String path = resource.getPath().substring(endIndex + 2);
            Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    if (entry.getName().startsWith(path)) {
                        System.out.println(entry.getName().substring(path.length() + 1));
                        Path fileDest = dest.resolve(entry.getName().substring(path.length() + 1));
                        if (Files.notExists(fileDest.getParent())) {
                            Files.createDirectory(fileDest.getParent());
                        }
                        Files.copy(file.getInputStream(entry), fileDest);
                    }
                }
            }
        } else {
            throw new RuntimeException("Unknown scheme " + resource.getProtocol());
        }
    }

    public static void copyFile(final File src, final File dest) throws IOException {
        final InputStream in = new BufferedInputStream(new FileInputStream(src));
        try {
            copyFile(in, dest);
        } finally {
            close(in);
        }
    }

    public static void copyFile(final InputStream in, final File dest) throws IOException {
        dest.getParentFile().mkdirs();
        final OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
        try {
            int i = in.read();
            while (i != -1) {
                out.write(i);
                i = in.read();
            }
        } finally {
            close(out);
        }
    }

    public static class CopyDirVisitor extends SimpleFileVisitor<Path> {
        private final Path fromPath;
        private final Path toPath;
        private final CopyOption copyOption;

        public CopyDirVisitor(Path fromPath, Path toPath, CopyOption copyOption) {
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

    public static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignore) {
        }
    }

    public static void deleteRecursive(final File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                deleteRecursive(f);
            }
        }
        file.delete();
    }

}
