package org.jboss.wildscribe.site;

/**
 * @author Stuart Douglas
 */
public class PathElement {

    private final String key;
    private final String value;


    public PathElement(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public boolean isWildcard() {
        return value.equals("*");
    }
}
