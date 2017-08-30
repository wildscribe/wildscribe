package org.jboss.wildscribe.site;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateExceptionHandler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String VERSION = "versions.txt";
    public static final String STATICRESOURCES = "staticresources";
    public static final String TEMPLATES = "templates";

    public static void main(final String[] args) {
        try {
            if (args.length != 2) {
                System.out.println("USAGE: java [-Durl=http://wildscribe.github.io] -jar site-generator.jar model-directory output-directory");
                System.exit(1);
            }
            File templateDir = initializeTemplateDir();

            final Path target = Paths.get(args[1]);
            if (Files.notExists(target)){
                Files.createDirectories(target);
            }

            for(String file : target.toFile().list()) {
                if(file.startsWith(".")) {
                    continue;
                }
                Path sub = target.resolve(file);
                FileUtils.deleteRecursive(sub.toFile());
            }
            System.out.println("Generating site in " + target.toString());

            copyResources(target);
            Configuration configuration = createFreemarkerConfig(templateDir);

            File modelDir = new File(args[0]);

            if(modelDir.isDirectory()) {
                List<Version> versions = loadVersions(modelDir);
                SiteGenerator siteGenerator = new SiteGenerator(versions, configuration, target);
                siteGenerator.createMainPage();
                siteGenerator.createAboutPage();
                siteGenerator.createVersions();
            } else {
                SiteGenerator siteGenerator = new SiteGenerator(modelDir, configuration, target);
                siteGenerator.createSingleVersion();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static File initializeTemplateDir() throws Exception {
        File tmp = new File(System.getProperty("java.io.tmpdir") + File.separator + "wildscribe_templates");
        FileUtils.deleteRecursive(tmp);
        tmp.mkdirs();
        FileUtils.copyDirectoryFromJar(Main.class.getClassLoader().getResource(TEMPLATES), tmp.toPath());
        return tmp;
    }

    private static Configuration createFreemarkerConfig(File templateDir) throws IOException {
        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(templateDir);
        cfg.setObjectWrapper(new DefaultObjectWrapper());
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setIncompatibleImprovements(new freemarker.template.Version(2, 3, 20));  // FreeMarker 2.3.20
        cfg.setURLEscapingCharset("UTF-8");
        return cfg;
    }

    private static void copyResources(Path target) throws Exception {
        FileUtils.copyDirectoryFromJar(Main.class.getClassLoader().getResource(STATICRESOURCES), target);
    }


    private static List<Version> loadVersions(File modelDir) throws IOException {
        final List<Version> ret = new ArrayList<>();
        final List<String> versions = Files.readAllLines(modelDir.toPath().resolve(VERSION), Charset.forName("UTF-8"));
        for (final String version : versions) {
            final String[] parts = version.split(":");
            ret.add(new Version(parts[0], parts[1], new File(modelDir, parts[2])));
        }
        return ret;
    }


}
