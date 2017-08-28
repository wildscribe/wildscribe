package org.jboss.wildscribe.site;

import static org.jboss.wildscribe.site.SiteGenerator.INDEX_HTML;

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
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class SingleVersionGenerator {
    public static final String RESOURCE_HTML = "resource.html";
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
        final ModelNode model = new ModelNode();
        model.readExternal(new FileInputStream(version.getDmrFile()));
        capabilities.putAll(getCapabilityMap(model));
        Template template = configuration.getTemplate(layoutHtml);
        createResourcePage(model, template);

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


    private void createResourcePage(ModelNode model, Template template, PathElement... path) throws TemplateException, IOException {
        final String currentUrl = buildCurrentUrl(path);
        final Map<String, Object> data = new HashMap<String, Object>();
        data.put("page", RESOURCE_HTML);
        data.put("versions", versions);
        data.put("version", version);
        data.put("urlbase", getUrlBase());
        data.put("currenturl", currentUrl);
        data.put("globalCapabilities", capabilities);
        if (single) {
            data.put("productHomeUrl", version.getProduct() + '/' + version.getVersion());
        } else {
            data.put("productHomeUrl", version.getProduct() + '/' + version.getVersion());
        }
        ResourceDescription resourceDescription = ResourceDescription.fromModelNode(PathAddress.pathAddress(path), model, capabilities);
        data.put("model", resourceDescription);

        final List<Breadcrumb> crumbs = buildBreadcrumbs(version, path);
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
                        createResourcePage(newModel, template, newPath);

                    }
                } else {
                    for (Child registration : child.getChildren()) {
                        PathElement[] newPath = addToPath(path, child.getName(), registration.getName());

                        ModelNode childModel = model.get("children").get(child.getName());
                        if (childModel.hasDefined("model-description") && childModel.get("model-description").hasDefined(registration.getName())) {
                            ModelNode newModel = childModel.get("model-description").get(registration.getName());

                            createResourcePage(newModel, template, newPath);
                        }
                    }
                }
            }
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

    private List<Breadcrumb> buildBreadcrumbs(Version version, PathElement[] path) {
        final List<Breadcrumb> crumbs = new ArrayList<>();
        crumbs.add(new Breadcrumb(version.getProduct() + " " + version.getVersion(), "index.html"));
        StringBuilder currentUrl = new StringBuilder();
        for (PathElement i : path) {
            currentUrl.append("/").append(i.getKey());
            if (!i.isWildcard()) {
                currentUrl.append("/").append(i.getValue());
            }
            crumbs.add(new Breadcrumb(i.getKey() + (i.isWildcard() ? "" : ("=" + i.getValue())), currentUrl.toString() + "/index.html"));
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
}
