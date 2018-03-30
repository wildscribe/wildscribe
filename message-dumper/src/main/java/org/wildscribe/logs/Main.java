package org.wildscribe.logs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

public class Main {

    public static void main(String[] params) throws Exception {
        if(params.length != 2) {
            System.out.println("Usage: dumper.jar path-to-modules output-file");
        }
        String path = params[0];
        List<LogMessage> messages = new ArrayList<>();
        Path file = Paths.get(path);
        Files.walkFileTree(file, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
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
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(params[1]))){
            for(LogMessage message : messages ) {
                out.writeUTF(message.code == null ? "" : message.code);
                out.writeUTF(message.level == null ? "" : message.level);
                out.writeUTF(message.returnType);
                out.writeUTF(message.message);
                out.writeInt(message.msgId);
                out.writeInt(message.length);
            }
        }
    }

    private static void processJarFile(Path file, List<LogMessage> messages) throws IOException {
        ZipFile z = new ZipFile(file.toFile());
        Enumeration<? extends ZipEntry> e = z.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = e.nextElement();
            if (entry.getName().endsWith(".class")) {
                try (DataInputStream d = new DataInputStream(z.getInputStream(entry))) {
                    ClassFile classFile = new ClassFile(d);
                    handleClass(classFile, messages);
                }

            }
        }
    }

    private static void handleClass(ClassFile classFile, List<LogMessage> messages) {
        AnnotationsAttribute attr = (AnnotationsAttribute) classFile.getAttribute(AnnotationsAttribute.invisibleTag);
        if (attr == null) {
            return;
        }
        for (Annotation annotation : attr.getAnnotations()) {
            if (annotation.getTypeName().equals("org.jboss.logging.annotations.MessageLogger")) {
                String code = ((StringMemberValue) annotation.getMemberValue("projectCode")).getValue();
                MemberValue lengthValue = annotation.getMemberValue("length");
                int length = lengthValue == null ? 6 : ((IntegerMemberValue) lengthValue).getValue();
                handleMessageLogger(classFile, code, length, messages);
            }
        }

    }

    private static void handleMessageLogger(ClassFile classFile, String code, int length, List<LogMessage> messages) {
        System.out.println(code);
        for (MethodInfo method : (List<MethodInfo>) classFile.getMethods()) {
            String logLevel = null;
            String message = null;
            int msgId = -1;
            AnnotationsAttribute attr = (AnnotationsAttribute) method.getAttribute(AnnotationsAttribute.invisibleTag);
            if (attr == null) {
                continue;
            }
            for (Annotation annotation : attr.getAnnotations()) {
                if (annotation.getTypeName().equals("org.jboss.logging.annotations.LogMessage")) {
                    MemberValue level = annotation.getMemberValue("level");
                    logLevel = level == null ? "INFO" : ((EnumMemberValue) level).getValue();
                } else if (annotation.getTypeName().equals("org.jboss.logging.annotations.Message")) {
                    message = ((StringMemberValue) annotation.getMemberValue("value")).getValue();
                    MemberValue id = annotation.getMemberValue("id");
                    if (id != null) {
                        msgId = ((IntegerMemberValue) id).getValue();
                    }
                }
            }
            if(message != null) {
                LogMessage l = new LogMessage(logLevel, code, message, length, msgId, extractReturnType(method));
                messages.add(l);
            }
            System.out.println(logLevel + " " + msgId + " " + message);
        }
    }

    private static String extractReturnType(MethodInfo method) {
        String descriptor = method.getDescriptor();
        descriptor = descriptor.substring(descriptor.lastIndexOf(")") + 1);
        descriptor = descriptor.replace("/", ".");
        descriptor = descriptor.replace(";", "");
        if(descriptor.startsWith("L")) {
            descriptor = descriptor.substring(1);
        }
        if(descriptor.equals("V")) {
            return "void";
        }
        return descriptor;
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

        public String getLevel() {
            return level;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public int getLength() {
            return length;
        }

        public int getMsgId() {
            return msgId;
        }
    }

}
