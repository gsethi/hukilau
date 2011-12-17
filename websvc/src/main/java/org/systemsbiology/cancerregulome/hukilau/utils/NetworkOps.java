package org.systemsbiology.cancerregulome.hukilau.utils;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.systemsbiology.cancerregulome.hukilau.pojo.NodeMaps;

import java.util.ArrayList;

/**
 * @author hrovira
 */
public class NetworkOps {

    public static NodeMaps traverseFrom(Node startNode, Integer traversalLevel) {
        NodeMaps nodeMaps = new NodeMaps();

        ArrayList<Node> unProcessedNodes = new ArrayList<Node>();
        ArrayList<Node> inProcessNodes = new ArrayList<Node>();

        inProcessNodes.add(startNode);
        for (int i = 0; i < traversalLevel; i++) {
            for (Node pNode : inProcessNodes) {
                nodeMaps.addNode(pNode);

                //getting both outbound and inbound relationships to the node
                for (Relationship rel : pNode.getRelationships()) {
                    nodeMaps.addRelationship(rel);

                    //put other node of relationship in list to be processed if it hasn't been processed already
                    Node otherNode = rel.getOtherNode(pNode);
                    if (!nodeMaps.containsNode(otherNode)) {
                        unProcessedNodes.add(otherNode);
                    }
                }
            }

            //transfer unProcessed to processing
            inProcessNodes = new ArrayList<Node>();
            for (Node n : unProcessedNodes) {
                inProcessNodes.add(n);
            }

            unProcessedNodes = new ArrayList<Node>();
        }

        nodeMaps.tieUpLooseEnds();
        return nodeMaps;
    }


}
