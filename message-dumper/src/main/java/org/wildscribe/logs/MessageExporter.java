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

package org.wildscribe.logs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import org.jboss.logging.Logger;

/**
 * A utility to export message id's and messages from modules to a binary data file.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class MessageExporter {
    private static final Logger LOGGER = Logger.getLogger(MessageExporter.class);

    /**
     * Reads the JAR's from the modules directory looking for messages to write to the target file.
     *
     * @param modulesDir the module directory where the JAR's to scan are located
     * @param target     the file to be written
     *
     * @return the path to the written file
     *
     * @throws IOException if an error occurs reading the JAR or writing the binary data file
     */
    public static Path export(final Path modulesDir, final Path target) throws IOException {

        final Collection<LogMessage> messages = new ArrayList<>();
        Files.walkFileTree(modulesDir, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".jar")) {
                    processJarFile(file, messages);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(target))) {
            for (LogMessage message : messages) {
                out.writeUTF(message.code == null ? "" : message.code);
                out.writeUTF(message.level == null ? "" : message.level);
                out.writeUTF(message.returnType);
                out.writeUTF(message.message);
                out.writeInt(message.msgId);
                out.writeInt(message.length);
            }
        }
        return target;
    }

    private static void processJarFile(final Path file, final Collection<LogMessage> messages) throws IOException {
        try (FileSystem zipFs = zipFs(file)) {
            for (Path dir : zipFs.getRootDirectories()) {
                Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                                if (file.getFileName().toString().endsWith(".class")) {
                                    try (DataInputStream d = new DataInputStream(Files.newInputStream(file))) {
                                        ClassFile classFile = new ClassFile(d);
                                        handleClass(classFile, messages);
                                    }
                                }
                                return super.visitFile(file, attrs);
                            }
                        }
                );
            }
        }
    }

    private static void handleClass(final ClassFile classFile, final Collection<LogMessage> messages) {
        AnnotationsAttribute attr = (AnnotationsAttribute) classFile.getAttribute(AnnotationsAttribute.invisibleTag);
        if (attr == null) {
            return;
        }
        for (Annotation annotation : attr.getAnnotations()) {
            if ("org.jboss.logging.annotations.MessageLogger".equals(annotation.getTypeName())) {
                String code = ((StringMemberValue) annotation.getMemberValue("projectCode")).getValue();
                MemberValue lengthValue = annotation.getMemberValue("length");
                int length = lengthValue == null ? 6 : ((IntegerMemberValue) lengthValue).getValue();
                handleMessageLogger(classFile, code, length, messages);
            }
        }

    }

    private static void handleMessageLogger(final ClassFile classFile, final String code, final int length, final Collection<LogMessage> messages) {
        LOGGER.debug(code);
        for (MethodInfo method : Collections.unmodifiableList(classFile.getMethods())) {
            String logLevel = null;
            String message = null;
            int msgId = -1;
            AnnotationsAttribute attr = (AnnotationsAttribute) method.getAttribute(AnnotationsAttribute.invisibleTag);
            if (attr == null) {
                continue;
            }
            for (Annotation annotation : attr.getAnnotations()) {
                if ("org.jboss.logging.annotations.LogMessage".equals(annotation.getTypeName())) {
                    MemberValue level = annotation.getMemberValue("level");
                    logLevel = level == null ? "INFO" : ((EnumMemberValue) level).getValue();
                } else if ("org.jboss.logging.annotations.Message".equals(annotation.getTypeName())) {
                    message = ((StringMemberValue) annotation.getMemberValue("value")).getValue();
                    MemberValue id = annotation.getMemberValue("id");
                    if (id != null) {
                        msgId = ((IntegerMemberValue) id).getValue();
                    }
                }
            }
            if (message != null) {
                LogMessage l = new LogMessage(logLevel, code, message, length, msgId, extractReturnType(method));
                messages.add(l);
            }
            LOGGER.debugf("%s %s: %s", logLevel, msgId, message);
        }
    }

    private static String extractReturnType(final MethodInfo method) {
        String descriptor = method.getDescriptor();
        descriptor = descriptor.substring(descriptor.lastIndexOf(")") + 1);
        descriptor = descriptor.replace("/", ".");
        descriptor = descriptor.replace(";", "");
        if (descriptor.startsWith("L")) {
            descriptor = descriptor.substring(1);
        }
        if ("V".equals(descriptor)) {
            return "void";
        }
        return descriptor;
    }

    private static FileSystem zipFs(final Path path) throws IOException {
        final Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        return zipFs(path, env);
    }

    private static FileSystem zipFs(final Path path, final Map<String, String> env) throws IOException {
        // locate file system by using the syntax
        // defined in java.net.JarURLConnection
        URI uri = URI.create("jar:" + path.toUri());
        try {
            return FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException ignore) {
        }
        return FileSystems.newFileSystem(uri, env);
    }

    private static final class LogMessage {
        final String level;
        final String code;
        final String message;
        final int length;
        final int msgId;
        final String returnType;

        private LogMessage(String level, String code, String message, int length, int msgId, String returnType) {
            this.level = level;
            this.code = code;
            this.message = message;
            this.length = length;
            this.msgId = msgId;
            this.returnType = returnType;
        }
    }
}
