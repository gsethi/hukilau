package org.systemsbiology.cancerregulome.hukilau.pojo;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hrovira
 *         TODO: need to load into db different datatypes for properties and then retrieve them here appropriately
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
        if(this.nodesById.containsKey(node.getId()))
            return;     //don't want to get all properties again, if this is already in the map

        this.nodesById.put(node.getId(), node);
        for (String propKey : node.getPropertyKeys()) {
            if (!this.nodeProperties.containsKey(propKey)) {
                this.nodeProperties.put(propKey, getDataType(node.getProperty(propKey)));
            }
        }
    }

    public void addRelationship(Relationship relationship) {
        if(this.relationshipsById.containsKey(relationship.getId()))
            return;  //don't want to get all properties again, if this is already in the map

        this.relationshipsById.put(relationship.getId(), relationship);
        for (String propKey : relationship.getPropertyKeys()) {
            if (!this.relationshipProperties.containsKey(propKey)) {
                this.relationshipProperties.put(propKey, getDataType(relationship.getProperty(propKey)));
            }
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

    public void tieUpLooseEnds() {
        for (Relationship r : getRelationships()) {
            if (!containsNode(r.getStartNode()) || !containsNode(r.getEndNode())) {
                relationshipsById.remove(r.getId());
            }
        }
    }

    /*
     * Private Methods
     */
    private String getDataType(Object obj) {
        if (obj instanceof Double) return "number";
        if (obj instanceof Integer) return "int";
        if (obj instanceof Boolean) return "boolean";
        return "string";
    }

}
