package org.jboss.wildscribe.site;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.jboss.dmr.ModelNode;

/**
 * class Responsible for generating sites
 *
 * @author Stuart Douglas
 */
public class SiteGenerator {

    public static final String INDEX_HTML = "index.html";
    public static final String ABOUT_HTML = "about.html";
    public static final String RESOURCE_HTML = "resource.html";
    public final String layoutHtml;
    private final File singlePageDmr;
    private final List<Version> versions;
    private final Configuration configuration;
    private final Path outputDir;

    public SiteGenerator(List<Version> versions, Configuration configuration, Path outputDir) {
        this.versions = versions;
        this.configuration = configuration;
        this.outputDir = outputDir;
        layoutHtml = "layout.html";
        this.singlePageDmr = null;
    }

    public SiteGenerator(File single, Configuration configuration, Path outputDir) {
        this.versions = null;
        this.configuration = configuration;
        this.outputDir = outputDir;
        layoutHtml = "single-layout.html";
        this.singlePageDmr = single;
    }

    public void createMainPage() throws IOException, TemplateException {
        Template template = configuration.getTemplate(layoutHtml);
        final Map<String, Object> data = new HashMap<String, Object>();
        data.put("page", INDEX_HTML);
        data.put("versions", versions);
        data.put("urlbase", getUrlBase());
        boolean wf = false;
        boolean eap6 = false;
        boolean as7 = false;
        assert versions != null;

        Version eap7 = versions.stream().filter(version -> version.getProduct().equals(Version.JBOSS_EAP))
                .filter(version -> version.getVersion().startsWith("7"))
                .findFirst().get();
        data.put("eap7", eap7);

        for (Version version : versions) {
            if (version.getProduct().equals(Version.JBOSS_AS7) && !as7) {
                as7 = true;
                data.put("as7", version);
            } else if (version.getProduct().equals(Version.JBOSS_EAP) && !eap6) {
                if (version.getVersion().startsWith("6")) {
                    eap6 = true;
                    data.put("eap6", version);
                }
            } else if (version.getProduct().equals(Version.WILDFLY) && !wf) {
                wf = true;
                data.put("wildfly", version);
            }
        }

        template.process(data, new PrintWriter(Files.newBufferedWriter(outputDir.resolve(INDEX_HTML), StandardCharsets.UTF_8)));
    }

    public void createAboutPage() throws IOException, TemplateException {
        Template template = configuration.getTemplate(layoutHtml);
        final Map<String, Object> data = new HashMap<String, Object>();
        data.put("page", ABOUT_HTML);
        data.put("versions", versions);
        data.put("urlbase", getUrlBase());
        template.process(data, new PrintWriter(Files.newBufferedWriter(outputDir.resolve(ABOUT_HTML), StandardCharsets.UTF_8)));
    }


    public void createVersions() throws IOException, TemplateException {
        for (Version version : versions) {
            System.out.println("Processing " + version.getProduct() + " " + version.getVersion());
            new SingleVersionGenerator(versions, version, configuration, outputDir, layoutHtml).generate();

        }
    }


    public void createSingleVersion() throws IOException, TemplateException {
        final ModelNode model = new ModelNode();
        model.readExternal(new FileInputStream(singlePageDmr));
        Version v = readVersionFromDmr(model);

        SingleVersionGenerator gen = new SingleVersionGenerator(versions, v, configuration, outputDir, layoutHtml);
        gen.setSingle(true);
        gen.generate();
    }


    private Version readVersionFromDmr(ModelNode fullModel) {
        if (!fullModel.hasDefined("version-info")) {
            return new Version("unknown", "unknown", singlePageDmr);
        }
        ModelNode model = fullModel.get("version-info");
        String productName = model.get("product-name").asString();
        String productVersion = model.get("product-version").asString();
        return new Version(productName, productVersion, singlePageDmr);

    }


    private String getUrlBase() {
        if (System.getProperty("url") == null) {
            return outputDir.toUri().toString();
        }
        return System.getProperty("url");
    }
}
