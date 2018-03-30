package org.jboss.wildscribe.site;

import java.io.File;

/**
 * Application server versions
 *
 * @author Stuart Douglas
 */
public final class Version {

    public static final String JBOSS_EAP = "JBoss EAP";
    public static final String WILDFLY = "WildFly";
    public static final String JBOSS_AS7 = "JBoss AS7";

    private final String product;
    private final String version;
    private final File dmrFile;

    public Version(final String product, final String version, final File dmrFile) {
        this.product = product;
        this.version = version;
        this.dmrFile = dmrFile;
    }

    public String getProduct() {
        return product;
    }

    public String getVersion() {
        return version;
    }

    public File getDmrFile() {
        return dmrFile;
    }

    public File getMessagesFile() {
        //hacky, don't run this in a dir with .dmr in the name
        String messagesFile = dmrFile.getAbsolutePath().replace(".dmr", ".messages");
        File m = new File(messagesFile);
        if(m.exists()) {
            return m;
        }
        return null;
    }

    @Override
    public String toString() {
        return "Version{" +
                "product='" + product + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
