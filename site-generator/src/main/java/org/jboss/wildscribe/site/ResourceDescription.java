package org.jboss.wildscribe.site;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representation of a management resource, in a nice format for templates
 *
 * @author Stuart Douglas
 */
public class ResourceDescription {

    private final String description;
    private final List<Child> children;
    private final List<Attribute> attributes;
    private final List<Operation> operations;
    private final Deprecated deprecated;

    public ResourceDescription(String description, List<Child> children, List<Attribute> attributes, List<Operation> operations, Deprecated deprecated) {
        this.description = description;
        this.children = children;
        this.attributes = attributes;
        this.operations = operations;
        this.deprecated = deprecated;
    }


    public String getDescription() {
        return description;
    }

    public List<Child> getChildren() {
        return children;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public Deprecated getDeprecated() {
        return deprecated;
    }

    public static ResourceDescription fromModelNode(final ModelNode node) {
        final List<Operation> ops = new ArrayList<Operation>();
        for (Property i : node.get("operations").asPropertyList()) {
            ops.add(Operation.fromProperty(i));
        }
        Collections.sort(ops);


        final List<Child> children = new ArrayList<Child>();
        if (node.hasDefined("children")) {
            for (Property i : node.get("children").asPropertyList()) {
                children.add(Child.fromProperty(i));
            }
            Collections.sort(children);
        }


        final List<Attribute> attributes = new ArrayList<Attribute>();
        if (node.hasDefined("attributes")) {
            for (Property i : node.get("attributes").asPropertyList()) {
                attributes.add(Attribute.fromProperty(i));
            }
            Collections.sort(attributes);
        }

        return new ResourceDescription(node.get("description").asString(), children, attributes, ops, Deprecated.fromModel(node));

    }

}
