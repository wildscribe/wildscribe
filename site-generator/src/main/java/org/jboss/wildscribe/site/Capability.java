package org.jboss.wildscribe.site;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class Capability {
    private final String name;
    private final boolean dynamic;
    private final List<String> providerPoints;
    private final Map<String,String> providerPointsUrls;

    private Capability(String name, boolean dynamic, List<String> providerPoints) {
        this.name = name;
        this.dynamic = dynamic;
        this.providerPoints = providerPoints;
        this.providerPointsUrls = calculateProviderPoints(providerPoints);
    }

    public String getName() {
        return name;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public List<String> getProviderPoints() {
        return providerPoints;
    }

    public Map<String,String> calculateProviderPoints(List<String> points){
        Map<String,String> resolved = points.stream().collect(Collectors.toMap(s -> s, v -> {
            PathAddress address = PathAddress.parseCLIStyleAddress(v);
            StringBuilder url = new StringBuilder();
            for (PathElement pe : address){
                if (pe.isWildcard()){
                    url.append(pe.getKey()).append('/');
                }else{
                    url.append(pe.getKey()).append('/').append(pe.getValue()).append('/');
                }

           }
            return url.toString();
        }));
        return resolved;
    }

    public Map<String, String> getProviderPointsUrls() {
        return providerPointsUrls;
    }

    /*
        "capabilities" => [{
            "name" => "org.wildfly.io.worker",
            "dynamic" => true
        }],
         */
    static Capability fromModel(ModelNode capability, Map<String, Capability> globalCaps) {
        String name = capability.get("name").asString();
        boolean dynamic = capability.get("dynamic").asBoolean(false);
        List<String> providerPoints;
        if (capability.hasDefined("registration-points")) {
            List<ModelNode> registrationPoints = capability.get("registration-points").asList();
            providerPoints = registrationPoints.stream().map(ModelNode::asString)
                    .collect(Collectors.toList());
        } else {
            if (globalCaps.containsKey(name)) {
                providerPoints = new LinkedList<>(globalCaps.get(name).getProviderPoints());
            } else {
                providerPoints = Collections.emptyList();
            }

        }
        return new Capability(name, dynamic, providerPoints);
    }

    static List<Capability> fromModelList(ModelNode capModel, Map<String, Capability> capabilities) {
        if (!capModel.isDefined()) {
            return Collections.emptyList();
        }
        List<Capability> r = new LinkedList<>();
        capModel.asList().forEach(c -> r.add(fromModel(c, capabilities)));
        return r;
    }

    public String getCapabilityDescriptionUrl(){
        StringBuilder url = new StringBuilder("https://github.com/wildfly/wildfly-capabilities/tree/master/");
        return url.append(name.replaceAll("\\.","/")).append("/capability.adoc").toString();
    }
}
