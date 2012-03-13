package org.systemsbiology.cancerregulome.hukilau.utils;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.systemsbiology.cancerregulome.hukilau.pojo.NodeMaps;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author hrovira
 */
public class NetworkOps {

    public static NodeMaps traverseFrom(Integer traversalLevel, Node... startNodes) {
        NodeMaps nodeMaps = new NodeMaps();

        ArrayList<Node> unProcessedNodes = new ArrayList<Node>();
        ArrayList<Node> inProcessNodes = new ArrayList<Node>();

        for (Node startNode : startNodes) {
            inProcessNodes.add(startNode);
            for (int i = 0; i < traversalLevel; i++) {
                for (Node pNode : inProcessNodes) {
                    if (pNode != null) {
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
                }

                //transfer unProcessed to processing
                inProcessNodes = new ArrayList<Node>();
                for (Node n : unProcessedNodes) {
                    inProcessNodes.add(n);
                }

                unProcessedNodes = new ArrayList<Node>();
            }
        }

        nodeMaps.tieUpLooseEnds();
        return nodeMaps;
    }

    private static class EdgeCallable implements Callable {
         private RelationshipIndex relIdx;
         private NodeMaps nodeMap;
         private Node geneNode;
         private List<Relationship> relList;

         public EdgeCallable(RelationshipIndex relIdx, NodeMaps nodes,  Node gene) {
             this.relIdx = relIdx;
             this.nodeMap = nodes;
             this.geneNode = gene;
             this.relList = new ArrayList<Relationship>();

          }

          public List<Relationship>  call() {

              for(Node gene2: nodeMap.getNodes()){
                  if(!gene2.equals(this.geneNode))  {
                      IndexHits<Relationship> relHits = relIdx.get(null,null,geneNode,gene2);
                      for(Relationship connection: relHits){
                          relList.add(connection);
                      }

                  }
              }

              return relList;

          }

      }


    //retrieves all relationships within a given node set, only requirement is relationship start and end nodes must be in the given node set
    public static void getRelationships(NodeMaps nodeMap, RelationshipIndex relIdx, ExecutorService executorService) throws Exception{
        Set<Future<List<Relationship> >> set = new HashSet<Future<List<Relationship> >>();

        for (Node gene : nodeMap.getNodes()) {
            Callable<List<Relationship>> callable = new EdgeCallable(relIdx, nodeMap, gene);
            Future<List<Relationship>> future = executorService.submit(callable);
            set.add(future);
        }

        for (Future<List<Relationship>> future : set) {
            List<Relationship> relList = future.get();
             //put relList items into the relMap
            for(Relationship connection : relList){
                nodeMap.addRelationship(connection);
            }
        }

    }

}
