package org.systemsbiology.cancerregulome.hukilau.utils;


import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.systemsbiology.cancerregulome.hukilau.pojo.NodeMaps;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class NetworkOps {
    private static final Logger log = Logger.getLogger(NetworkOps.class.getName());
    final static TraversalDescription GEN_TRAVERSAL = Traversal.description()
            .breadthFirst()
            .uniqueness(Uniqueness.RELATIONSHIP_PATH);

    //traversalLevel: # of levels to traverse thru the graph
    //traversalDesc: JSONObject denoting types of relationships and nodes to traverse at each level,
    //if traversalDesc is not null, then must have traversalLevel-1 items.
    //{nodes:[{"type":"gene"}],relationships:[{"type":"ngd",direction:"out"}]
    //startNodes is a node, or list of nodes to start a traversal from.
    public static NodeMaps traverseFrom(Integer traversalLevel, JSONObject traversalDesc, Node... startNodes) throws Exception{
        NodeMaps nodeMaps = new NodeMaps();
        final ArrayList<String> orderedPath = new ArrayList<String>();

       //TODO: add in more than just relationship type traversal
        if(traversalDesc != null){
            JSONArray relArray = traversalDesc.getJSONArray("relationships");
            for(int i=0; i< relArray.length(); i++){
                JSONObject relObject = relArray.getJSONObject(i);
                if(relObject.has("type")){
                    orderedPath.add((String)relArray.getJSONObject(i).get("type"));
                }
                else
                    orderedPath.add("*");
            }
        }

        log.info("orderedPath size: " + orderedPath.size());

        for (Node startNode: startNodes){
            nodeMaps.addNode(startNode);
            TraversalDescription td =  GEN_TRAVERSAL
                    .evaluator(Evaluators.toDepth(traversalLevel))
                    .evaluator(new Evaluator() {
                        @Override
                        public Evaluation evaluate(Path path) {
                            if(path.lastRelationship() != null && path.lastRelationship().isType(DynamicRelationshipType.withName("ngd")))
                                log.info("found an ngd relationship!");
                            else if(path.lastRelationship() != null && !path.lastRelationship().isType(DynamicRelationshipType.withName("domine"))
                                    && !path.lastRelationship().isType(DynamicRelationshipType.withName("ngd_alias")))
                                log.info("different relationship: " + path.lastRelationship().getType().toString());

                            if(path.length() == 0){
                               return Evaluation.EXCLUDE_AND_CONTINUE;
                           }

                            if(orderedPath.size() == 0){
                                return Evaluation.INCLUDE_AND_CONTINUE;
                            }

                           String relType = orderedPath.get(path.length() - 1);
                            if(relType.equals("*")){
                                return Evaluation.INCLUDE_AND_CONTINUE;
                            }
                            else if(path.lastRelationship().isType(DynamicRelationshipType.withName(relType))){
                                return Evaluation.INCLUDE_AND_CONTINUE;
                            }
                            else
                                return Evaluation.EXCLUDE_AND_PRUNE;

                        }
                    });

           org.neo4j.graphdb.traversal.Traverser t = td.traverse(startNode);
           for(Path p : t){
                nodeMaps.addRelationship(p.lastRelationship());
                nodeMaps.addNode(p.endNode());
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
    public static void getRelationships(NodeMaps nodeMap, RelationshipIndex relIdx, Map<RelationshipType,RelationshipType> relTypes, ExecutorService executorService) throws Exception{
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
                if(relTypes.containsKey(connection.getType()))
                    nodeMap.addRelationship(connection);
            }
        }

    }

    public static NodeMaps getRelatedNodes(List<Node> nodeList, List<RelationshipType> relList) throws Exception{
        NodeMaps nodeMaps = new NodeMaps();

        TraversalDescription td =  GEN_TRAVERSAL
                    .evaluator(Evaluators.toDepth(1));

        for(RelationshipType rel : relList){
            log.info("adding relationship: " + rel.name());
             td = td.relationships(rel);
        }

        log.info("traversal desc: " + td.toString());
        for(Node node : nodeList){
            Iterable<Node> nodeIter = td.traverse(node).nodes();
            for(Node n : nodeIter){
                nodeMaps.addNode(n);
            }

        }


        return nodeMaps;
    }

}
