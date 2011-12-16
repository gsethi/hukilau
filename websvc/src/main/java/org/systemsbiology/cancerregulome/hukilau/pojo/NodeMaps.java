package org.systemsbiology.cancerregulome.hukilau.pojo;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hrovira
 * TODO: need to load into db different datatypes for properties and then retrieve them here appropriately
 */
public class NodeMaps {
    private final Map<Long, Node> nodesById = new HashMap<Long, Node>();
    private final Map<String, String> nodeProperties = new HashMap<String, String>();

    private final Map<Long, Relationship> relationshipsById = new HashMap<Long, Relationship>();
    private final Map<String, String> relationshipProperties = new HashMap<String, String>();

    /*
     * Public Getters
     */
    public Map<String, String> getNodeProperties() {
        return nodeProperties;
    }

    public Map<String, String> getRelationshipProperties() {
        return relationshipProperties;
    }

    /*
     * Public Methods
     */
    public void addNode(Node node) {
        this.nodesById.put(node.getId(), node);
        for (String propKey : node.getPropertyKeys()) {
            // TODO: Specify actual property types
            this.nodeProperties.put(propKey, "string");
        }
    }

    public void addRelationship(Relationship relationship) {
        this.relationshipsById.put(relationship.getId(), relationship);
        for (String propKey : relationship.getPropertyKeys()) {
            // TODO: Specify actual property types
            this.relationshipProperties.put(propKey, "string");
        }
    }

    public boolean containsNode(Node node) {
        return this.nodesById.containsKey(node.getId());
    }

    public Integer numberOfNodes() {
        return this.nodesById.size();
    }

    public Integer numberOfRelationships() {
        return this.relationshipsById.size();
    }

    public Node[] getNodes() {
        return this.nodesById.values().toArray(new Node[nodesById.size()]);
    }

    public Relationship[] getRelationships() {
        return this.relationshipsById.values().toArray(new Relationship[relationshipsById.size()]);
    }

}
