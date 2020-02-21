package org.jboss.wildscribe.site;

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
import org.jboss.logging.Logger;

/**
 * class Responsible for generating sites
 *
 * @author Stuart Douglas
 */
class SiteGenerator {
    private static final Logger LOGGER = Logger.getLogger(SiteGenerator.class.getPackage().getName());
    private static final String DEFAULT_LAYOUT_HTML = "layout.html";
    private static final String SINGLE_LAYOUT_HTML = "single-layout.html";

    public static final String INDEX_HTML = "index.html";
    public static final String ABOUT_HTML = "about.html";
    public static final String RESOURCE_HTML = "resource.html";
    private final List<Version> versions;
    private final Configuration configuration;
    private final Path outputDir;

    public SiteGenerator(List<Version> versions, Configuration configuration, Path outputDir) {
        this.versions = versions;
        this.configuration = configuration;
        this.outputDir = outputDir;
    }

    public void createMainPage() throws IOException, TemplateException {
        Template template = configuration.getTemplate(DEFAULT_LAYOUT_HTML);
        final Map<String, Object> data = new HashMap<String, Object>();
        data.put("page", INDEX_HTML);
        data.put("versions", versions);
        data.put("urlbase", getUrlBase());
        boolean wf = false;
        boolean eap6 = false;
        boolean as7 = false;
        assert versions != null;

        versions.stream().filter(version -> version.getProduct().equals(Version.JBOSS_EAP))
                .filter(version -> version.getVersion().startsWith("7"))
                .findFirst().ifPresent(v -> data.put("eap7", v));

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
        final String alertMessage = System.getProperty("wildscribe.index.alert.message");
        if (alertMessage != null) {
            final AlertMessage am = new AlertMessage();
            final String dismissible = System.getProperty("wildscribe.index.alert.dismissible");
            am.setDismissible(dismissible != null && dismissible.isEmpty() || Boolean.parseBoolean(dismissible));
            am.setHeader(System.getProperty("wildscribe.index.alert.header"));
            am.setMessage(alertMessage);
            am.setType(System.getProperty("wildscribe.index.alert.type"));
            data.put("alertMessage", am);
        }

        template.process(data, new PrintWriter(Files.newBufferedWriter(outputDir.resolve(INDEX_HTML), StandardCharsets.UTF_8)));
    }

    public void createAboutPage() throws IOException, TemplateException {
        Template template = configuration.getTemplate(DEFAULT_LAYOUT_HTML);
        final Map<String, Object> data = new HashMap<String, Object>();
        data.put("page", ABOUT_HTML);
        data.put("versions", versions);
        data.put("urlbase", getUrlBase());
        template.process(data, new PrintWriter(Files.newBufferedWriter(outputDir.resolve(ABOUT_HTML), StandardCharsets.UTF_8)));
    }


    public void createVersions() throws IOException, TemplateException {
        for (Version version : versions) {
            LOGGER.infof("Processing %s %s", version.getProduct(), version.getVersion());
            new SingleVersionGenerator(versions, version, configuration, outputDir, DEFAULT_LAYOUT_HTML).generate();

        }
    }


    public void createSingleVersion() throws IOException, TemplateException {
        SingleVersionGenerator gen = new SingleVersionGenerator(null, versions.get(0), configuration, outputDir, SINGLE_LAYOUT_HTML);
        gen.setSingle(true);
        gen.generate();
    }

    private String getUrlBase() {
        if (System.getProperty("url") == null) {
            return outputDir.toUri().toString();
        }
        return System.getProperty("url");
    }
}
