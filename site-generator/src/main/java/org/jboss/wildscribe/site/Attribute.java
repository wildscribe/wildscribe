package org.jboss.wildscribe.site;

import org.jboss.dmr.Property;

/**
* @author Stuart Douglas
*/
public class Attribute implements Comparable<Attribute> {
    private final String name;
    private final String description;

    public Attribute(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int compareTo(Attribute o) {
        return name.compareTo(o.getName());
    }


    public static Attribute fromProperty(final Property property) {
        String name = property.getName();
        String description = property.getValue().get("description").asString();
        Attribute op = new Attribute(name, description);
        return op;
    }
}
