package org.jboss.wildscribe.site;

import org.jboss.dmr.Property;

/**
* @author Stuart Douglas
*/
public class Operation implements Comparable<Operation> {

    private final String name;
    private final String description;

    public Operation(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static Operation fromProperty(final Property property) {
        String name = property.getName();
        String description = property.getValue().get("description").asString();
        Operation op = new Operation(name, description);

        return op;
    }

    @Override
    public int compareTo(Operation o) {
        return name.compareTo(o.name);
    }

    public final class Parameters {

    }
}
