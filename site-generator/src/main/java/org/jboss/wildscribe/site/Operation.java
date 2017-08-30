package org.jboss.wildscribe.site;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author Stuart Douglas
 */
public class Operation implements Comparable<Operation> {

    private final String name;
    private final String description;
    private final Reply reply;
    private final List<Parameter> parameters;
    private final Deprecated deprecated;
    private final boolean readOnly;
    private final boolean runtimeOnly;


    public Operation(String name, String description, List<Parameter> parameters, Deprecated deprecated, Reply reply, boolean readOnly, boolean runtimeOnly) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
        this.deprecated = deprecated;
        this.reply = reply;
        this.readOnly = readOnly;
        this.runtimeOnly = runtimeOnly;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public Deprecated getDeprecated() {
        return  deprecated;
    }

    public Reply getReply() {
        return reply;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isRuntimeOnly() {
        return runtimeOnly;
    }

    public static Operation fromProperty(final Property property) {
        String name = property.getName();
        ModelNode op = property.getValue();
        String description = op.get("description").asString();
        ModelNode replyProperties = op.get("reply-properties");

        Reply r = null;
        if (replyProperties.isDefined() && replyProperties.hasDefined("type")) {
            String returnType = replyProperties.get("type").asString();
            String returnDescription = replyProperties.get("description").asString("");
            ModelNode returnValueType = replyProperties.get("value-type");
            StringWriter writer = new StringWriter();
            returnValueType.writeString(new PrintWriter(writer), false);
            String valueType = writer.toString();
            r = new Reply(returnType, returnValueType.isDefined() ? valueType : null, returnDescription);
        }


        final List<Parameter> parameters = new ArrayList<Parameter>();
        if (op.hasDefined("request-properties")) {

            for (Property param : op.get("request-properties").asPropertyList()) {
                String desc = param.getValue().get("description").asString();
                String type = param.getValue().get("type").asString();
                String defaultValue = param.getValue().get("default").asStringOrNull();
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

                parameters.add(new Parameter(param.getName(), type, desc, required, nillable, expressionsAllowed, defaultValue));
            }
        }
        //todo this info is missing in model
        boolean readOnly = false; //op.get("read-only").asBoolean(false);
        boolean runtimeOnly = false; //op.get("runtime-only").asBoolean(false);

        return new Operation(name, description, parameters, Deprecated.fromModel(op), r, readOnly, runtimeOnly);
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
        private final String defaultValue;

        public Parameter(String name, String type, String description, boolean required, boolean nillable, boolean expressionsAllowed, String defaultValue) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.required = required;
            this.nillable = nillable;
            this.expressionsAllowed = expressionsAllowed;
            this.defaultValue = defaultValue;

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

        public String getDefaultValue() {
            return defaultValue;
        }


    }
    public static final class Reply{
        private final String type;
        private final String valueType;
        private final String description;

        public Reply(String type, String valueType, String description) {
            this.type = type;
            this.valueType = valueType;
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public String getValueType() {
            return valueType;
        }

        public String getDescription() {
            return description;
        }
    }
}
