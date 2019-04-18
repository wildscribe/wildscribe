package org.jboss.wildscribe.site;

import static org.jboss.wildscribe.site.SiteGenerator.INDEX_HTML;

import org.jboss.dmr.ModelNode;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class SingleVersionGenerator {
    public static final String RESOURCE_HTML = "resource.html";
    private static final String LOG_MESSAGE_REFERENCE_HTML = "log-message-reference.html";
    private static final String LOGS_HTML = "logs.html";
    public final String layoutHtml;
    private final Map<String, Capability> capabilities = new LinkedHashMap<>();
    private final List<Version> versions;
    private final Version version;
    private final Configuration configuration;
    private final Path outputDir;
    private boolean single = false;


    public SingleVersionGenerator(List<Version> versions, Version version, Configuration configuration, Path outputDir, String layoutHtml) {
        this.versions = versions;
        this.version = version;
        this.configuration = configuration;
        this.outputDir = outputDir;
        this.layoutHtml = layoutHtml;
    }

    public void setSingle(boolean single) {
        this.single = single;
    }

    public void generate() throws IOException, TemplateException {
        List<LogMessage> messages = loadLogMessages();
        final ModelNode model = new ModelNode();
        model.readExternal(new FileInputStream(version.getDmrFile()));
        capabilities.putAll(getCapabilityMap(model));
        Template template = configuration.getTemplate(layoutHtml);
        createResourcePage(model, template, messages != null);
        if (messages != null) {
            createLogMessagePage(template, messages);
        }
    }

    private List<LogMessage> loadLogMessages() throws IOException {
        File file = version.getMessagesFile();
        if (file == null) {
            return null;
        }
        List<LogMessage> ret = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            for (; ; ) {
                String code = in.readUTF();
                String level = in.readUTF();
                String returnType = in.readUTF();
                String message = in.readUTF();
                int id = in.readInt();
                int length = in.readInt();
                ret.add(new LogMessage(level, code, message, length, id, returnType));
            }
        } catch (EOFException e) {

        }
        return ret;
    }


    private Map<String, Capability> getCapabilityMap(ModelNode fullModel) {
        ModelNode capabilitiesModel = fullModel.get("possible-capabilities");
        Map<String, Capability> capabilityMap = new TreeMap<>();
        if (capabilitiesModel.isDefined()) {
            capabilitiesModel.asList().forEach(cap -> {
                Capability capability = Capability.fromModel(cap, Collections.emptyMap(), null);
                capabilityMap.put(capability.getName(), capability);
            });
        }
        return capabilityMap;
    }


    private void createResourcePage(ModelNode model, Template template, boolean hasLogs, PathElement... path) throws TemplateException, IOException {
        final String currentUrl = buildCurrentUrl(path);
        final String urlBase = getUrlBase();
        final String productHomeUrl = single ? urlBase : version.getProduct() + '/' + version.getVersion();
        final ResourceDescription resourceDescription = ResourceDescription.fromModelNode(PathAddress.pathAddress(path), model, capabilities);
        final List<Breadcrumb> crumbs = buildBreadcrumbs(version, path, productHomeUrl);
        final Map<String, Object> data = new HashMap<>();
        data.put("page", RESOURCE_HTML);
        data.put("versions", versions);
        data.put("version", version);
        data.put("urlbase", urlBase);
        data.put("currenturl", currentUrl);
        data.put("has_messages", hasLogs);
        data.put("globalCapabilities", capabilities);
        data.put("productHomeUrl", productHomeUrl);
        data.put("model", resourceDescription);
        data.put("breadcrumbs", crumbs);

        File parent;
        if (single) {
            parent = new File(outputDir.toFile().getAbsolutePath() + File.separator + currentUrl);
        } else {
            parent = new File(outputDir.toFile().getAbsolutePath() + File.separator + version.getProduct() + File.separator + version.getVersion() + currentUrl);
        }
        parent.mkdirs();
        StringWriter stringWriter = new StringWriter();
        template.process(data, stringWriter);
        HtmlCompressor compressor = new HtmlCompressor();
        String compressedHtml = compressor.compress(stringWriter.getBuffer().toString());
        //String compressedHtml = stringWriter.getBuffer().toString();
        try (FileOutputStream stream = new FileOutputStream(new File(parent, INDEX_HTML))) {
            stream.write(compressedHtml.getBytes("UTF-8"));
        }

        if (resourceDescription.getChildren() != null) {
            for (Child child : resourceDescription.getChildren()) {
                if (child.getChildren().isEmpty()) {
                    PathElement[] newPath = addToPath(path, child.getName(), "*");

                    ModelNode childModel = model.get("children").get(child.getName());
                    if (childModel.hasDefined("model-description")) {
                        ModelNode newModel = childModel.get("model-description").get("*");
                        if (!newModel.hasDefined("operations")) {
                            //System.out.println(String.format("resource '%s' is missing operations node", Arrays.asList(newPath)));
                            newModel.get("operations");
                        }
                        createResourcePage(newModel, template, false, newPath);

                    }
                } else {
                    for (Child registration : child.getChildren()) {
                        PathElement[] newPath = addToPath(path, child.getName(), registration.getName());

                        ModelNode childModel = model.get("children").get(child.getName());
                        if (childModel.hasDefined("model-description") && childModel.get("model-description").hasDefined(registration.getName())) {
                            ModelNode newModel = childModel.get("model-description").get(registration.getName());

                            createResourcePage(newModel, template, false, newPath);
                        }
                    }
                }
            }
        }

    }

    private void createLogMessagePage(Template template, List<LogMessage> messages) throws TemplateException, IOException {
        final String urlBase = getUrlBase();
        final String productHomeUrl = single ? urlBase : version.getProduct() + '/' + version.getVersion();
        final Map<String, Object> data = new HashMap<>();
        data.put("page", LOGS_HTML);
        data.put("versions", versions);
        data.put("version", version);
        data.put("urlbase", urlBase);
        data.put("globalCapabilities", capabilities);
        data.put("productHomeUrl", productHomeUrl);

        Map<String, List<DisplayMessage>> map = new TreeMap<>();
        for (LogMessage msg : messages) {
            if(msg.getCode().isEmpty()) {
                continue;
            }
            String realId = msg.getCode() + String.format("%0" + msg.getLength() + "d", msg.getMsgId());
            DisplayMessage d = new DisplayMessage(realId, msg.getMessage(), msg.getLevel(), msg.getMsgId(), msg.returnType.equals("void") ? "" : msg.returnType);
            map.computeIfAbsent(msg.getCode(), (i) -> new ArrayList<>()).add(d);
        }
        map.forEach((s, messages1) -> Collections.sort(messages1));
        data.put("messages", map);
        data.put("codes", new ArrayList<>(map.keySet()));
        File parent;
        if (single) {
            parent = new File(outputDir.toFile().getAbsolutePath() + File.separator);
        } else {
            parent = new File(outputDir.toFile().getAbsolutePath() + File.separator + version.getProduct() + File.separator + version.getVersion());
        }
        parent.mkdirs();
        StringWriter stringWriter = new StringWriter();
        template.process(data, stringWriter);
        HtmlCompressor compressor = new HtmlCompressor();
        String compressedHtml = compressor.compress(stringWriter.getBuffer().toString());
        //String compressedHtml = stringWriter.getBuffer().toString();
        try (FileOutputStream stream = new FileOutputStream(new File(parent, LOG_MESSAGE_REFERENCE_HTML))) {
            stream.write(compressedHtml.getBytes("UTF-8"));
        }
    }

    private String getUrlBase() {
        if (System.getProperty("url") == null) {
            return outputDir.toUri().toString();
        }
        return System.getProperty("url");
    }

    private PathElement[] addToPath(PathElement[] path, final String key, final String value) {
        PathElement[] newPath = new PathElement[path.length + 1];
        System.arraycopy(path, 0, newPath, 0, path.length);
        newPath[path.length] = new PathElement(key, value);
        return newPath;
    }

    private List<Breadcrumb> buildBreadcrumbs(Version version, PathElement[] path, String productHomeUrl) {
        final List<Breadcrumb> crumbs = new ArrayList<>();
        crumbs.add(new Breadcrumb(version.getProduct() + " " + version.getVersion(), productHomeUrl + "/index.html"));
        StringBuilder currentUrl = new StringBuilder(productHomeUrl);
        for (PathElement i : path) {
            currentUrl.append("/").append(i.getKey());
            if (!i.isWildcard()) {
                currentUrl.append("/").append(i.getValue());
            }
            final String label = i.getKey() + (i.isWildcard() ? "" : ("=" + i.getValue()));
            crumbs.add(new Breadcrumb(label, currentUrl.toString() + "/index.html"));
        }
        return crumbs;
    }

    private String buildCurrentUrl(final PathElement... path) {
        StringBuilder sb = new StringBuilder();
        for (PathElement i : path) {
            sb.append('/');
            sb.append(i.getKey());
            if (!i.isWildcard()) {
                sb.append('/');
                sb.append(i.getValue());
            }
        }
        return sb.toString();
    }


    public static final class LogMessage {
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

        @Override
        public String toString() {
            return "LogMessage{" +
                    "level='" + level + '\'' +
                    ", code='" + code + '\'' +
                    ", message='" + message + '\'' +
                    ", length=" + length +
                    ", msgId=" + msgId +
                    '}';
        }
    }

    public class DisplayMessage implements Comparable<DisplayMessage> {
        private final String id, message, level;
        private final int numericId;
        final String returnType;

        public DisplayMessage(String id, String message, String level, int numericId, String returnType) {
            this.id = id;
            this.message = message;
            this.level = level;
            this.numericId = numericId;
            this.returnType = returnType;
        }

        public String getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }

        public String getLevel() {
            return level;
        }

        public String getReturnType() {
            return returnType;
        }

        @Override
        public int compareTo(DisplayMessage o) {
            return Integer.compare(numericId, o.numericId);
        }
    }
}
