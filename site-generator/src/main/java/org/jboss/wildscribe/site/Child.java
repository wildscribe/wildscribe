package org.jboss.wildscribe.site;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class Child implements Comparable<Child> {
    private final String name;
    private final String description;

    /**
     * If this is a non-wildcard registration
     */
    private final List<Child> children;

    public Child(String name, String description, List<Child> children) {
        this.name = name;
        this.description = description;
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Child> getChildren() {
        return children;
    }

    @Override
    public int compareTo(Child o) {
        return name.compareTo(o.getName());
    }


    public static Child fromProperty(final Property property) {
        String name = property.getName();
        String description = property.getValue().get("description").asString();

        final List<Child> registrations = new ArrayList<Child>();
        ModelNode modelDesc = property.getValue().get("model-description");
        if (modelDesc.isDefined()) {
            for (Property child : modelDesc.asPropertyList()) {
                if (!child.getName().equals("*")) {
                    registrations.add(new Child(child.getName(), child.getValue().get("description").asString(), null));
                }
            }
        }
        Collections.sort(registrations);

        Child op = new Child(name, description, registrations);

        return op;
    }

}
