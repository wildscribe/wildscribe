package org.jboss.wildscribe.site;

import org.jboss.dmr.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class Operation implements Comparable<Operation> {

    private final String name;
    private final String description;
    private final String returnType;
    private final List<Parameter> parameters;


    public Operation(String name, String description, String returnType, List<Parameter> parameters) {
        this.name = name;
        this.description = description;
        this.returnType = returnType;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public static Operation fromProperty(final Property property) {
        String name = property.getName();
        String description = property.getValue().get("description").asString();
        String returnType = property.getValue().get("reply-properties").get("type").asString();

        final List<Parameter> parameters = new ArrayList<Parameter>();
        if (property.getValue().hasDefined("request-properties")) {

            for (Property param : property.getValue().get("request-properties").asPropertyList()) {
                String desc = param.getValue().get("description").asString();
                String type = param.getValue().get("type").asString();
                boolean required = false;
                if (param.getValue().hasDefined("required")) {
                    required = param.getValue().get("required").asBoolean();
                }

                boolean nillable = !required;
                if (param.getValue().hasDefined("nillable")) {
                    nillable = param.getValue().get("nillable").asBoolean();
                }
                boolean expressionsAllowed = false;

                if (param.getValue().hasDefined("expressions-allowed")) {
                    expressionsAllowed = param.getValue().get("expressions-allowed").asBoolean();
                }
                parameters.add(new Parameter(param.getName(), type, desc, required, nillable, expressionsAllowed));
            }
        }

        Operation op = new Operation(name, description, returnType, parameters);

        return op;
    }

    @Override
    public int compareTo(Operation o) {
        return name.compareTo(o.name);
    }

    public static final class Parameter {
        private final String name;
        private final String type;
        private final String description;
        private final boolean required;
        private final boolean nillable;
        private final boolean expressionsAllowed;

        public Parameter(String name, String type, String description, boolean required, boolean nillable, boolean expressionsAllowed) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.required = required;
            this.nillable = nillable;
            this.expressionsAllowed = expressionsAllowed;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public boolean isRequired() {
            return required;
        }

        public String getDescription() {
            return description;
        }

        public boolean isNillable() {
            return nillable;
        }

        public boolean isExpressionsAllowed() {
            return expressionsAllowed;
        }
    }
}
