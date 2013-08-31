package org.jboss.wildscribe.site;

/**
 * Used for displaying the breadcrumb bar
 *
 * @author Stuart Douglas
 */
public class Breadcrumb {

    private final String label;
    private final String url;

    public Breadcrumb(String label, String url) {
        this.label = label;
        this.url = url;
    }

    public String getLabel() {
        return label;
    }

    public String getUrl() {
        return url;
    }
}
