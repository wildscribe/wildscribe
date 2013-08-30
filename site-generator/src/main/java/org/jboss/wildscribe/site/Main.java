package org.jboss.wildscribe.site;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateExceptionHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String VERSION = "versions.txt";
    public static final String STATICRESOURCES = "staticresources";
    public static final String TEMPLATES = "templates";

    public static void main(final String[] args) {
        try {
            if (args.length != 2) {
                System.out.println("USAGE: java -jar site-generator.jar model-directory output-directory");
                System.exit(1);
            }
            File modelDir = new File(args[0]);

            final File target = new File(args[1]);
            FileUtils.deleteRecursive(target);
            target.mkdirs();
            System.out.print("Generating site in " + target.getAbsolutePath());

            copyResources(target);

            List<Version> versions = loadVersions(modelDir);

            File templateDir = initializeTemplateDir();

            Configuration configuration = createFreemarkerConfig(templateDir);

            SiteGenerator siteGenerator = new SiteGenerator(versions, configuration, target);
            siteGenerator.createMainPage();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static File initializeTemplateDir() throws IOException {
        File tmp = new File(System.getProperty("java.io.tmpdir") + File.separator + "wildscribe_templates");
        FileUtils.deleteRecursive(tmp);
        tmp.mkdirs();
        FileUtils.copyDirectoryFromJar(Main.class.getClassLoader().getResource(TEMPLATES), tmp);
        return tmp;
    }

    private static Configuration createFreemarkerConfig(File templateDir) throws IOException {
        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(templateDir);
        cfg.setObjectWrapper(new DefaultObjectWrapper());
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setIncompatibleImprovements(new freemarker.template.Version(2, 3, 20));  // FreeMarker 2.3.20
        return cfg;
    }

    private static void copyResources(File target) throws IOException {
        FileUtils.copyDirectoryFromJar(Main.class.getClassLoader().getResource(STATICRESOURCES), target);
    }


    private static List<Version> loadVersions(File modelDir) {
        final List<Version> ret = new ArrayList<Version>();
        final String versionsString = FileUtils.readFile(new File(modelDir, VERSION));
        final String[] versionParts = versionsString.split("\n");
        for (final String version : versionParts) {
            final String[] parts = version.split(":");
            ret.add(new Version(parts[0], parts[1], new File(modelDir, parts[2])));
        }
        return ret;
    }


}
