package org.jboss.wildscribe.site;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * class Responsible for generating sites
 *
 * @author Stuart Douglas
 */
public class SiteGenerator {

    public static final String INDEX_HTML = "index.html";
    public static final String ABOUT_HTML = "about.html";
    private final List<Version> versions;
    private final Configuration configuration;
    private final File outputDir;

    public SiteGenerator(List<Version> versions, Configuration configuration, File outputDir) {
        this.versions = versions;
        this.configuration = configuration;
        this.outputDir = outputDir;
    }

    public void createMainPage() throws IOException, TemplateException {
        Template template = configuration.getTemplate("layout.html");
        final Map<String, Object> data = new HashMap<String, Object>();
        data.put("page", INDEX_HTML);
        data.put("versions", versions);
        data.put("urlbase", outputDir.getAbsoluteFile());
        boolean wf = false;
        boolean eap = false;
        boolean as7 = false;
        for (Version version : versions) {
            if (version.getProduct().equals(Version.JBOSS_AS7) && !as7) {
                as7 = true;
                data.put("as7", version);
            } else if (version.getProduct().equals(Version.JBOSS_EAP) && !eap) {
                eap = true;
                data.put("eap", version);
            } else if (version.getProduct().equals(Version.WILDFLY) && !wf) {
                wf = true;
                data.put("wildfly", version);
            }
        }

        template.process(data, new PrintWriter(new FileOutputStream(new File(outputDir, INDEX_HTML))));
    }

    public void createAboutPage() throws IOException, TemplateException {
        Template template = configuration.getTemplate("layout.html");
        final Map<String, Object> data = new HashMap<String, Object>();
        data.put("page", ABOUT_HTML);
        data.put("versions", versions);
        data.put("urlbase", outputDir.getAbsoluteFile());
        template.process(data, new PrintWriter(new FileOutputStream(new File(outputDir, ABOUT_HTML))));
    }
}
