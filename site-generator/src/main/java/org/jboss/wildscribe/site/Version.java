package org.jboss.wildscribe.site;

import java.io.File;

/**
 * Application server versions
 *
 * @author Stuart Douglas
 */
public final class Version implements Comparable<Version> {

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

    @Override
    public int compareTo(Version o) {
        int s = product.compareTo(o.product);
        if (s != 0) {
            return s;
        }
        return version.compareTo(o.version);
    }
}
