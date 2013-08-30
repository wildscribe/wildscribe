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
        data.put("page", "index.html");
        data.put("versions", versions);
        data.put("urlbase", outputDir.getAbsoluteFile());
        template.process(data, new PrintWriter(new FileOutputStream(new File(outputDir, "index.html"))));
    }

}
