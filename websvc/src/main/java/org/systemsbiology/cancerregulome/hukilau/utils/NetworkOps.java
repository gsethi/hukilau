package org.systemsbiology.cancerregulome.hukilau.utils;


import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.index.lucene.ValueContext;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.systemsbiology.cancerregulome.hukilau.pojo.NodeMaps;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
    public static NodeMaps getRelationships(NodeMaps nodeMap, RelationshipIndex relIdx, Map<String,String> relTypes, ExecutorService executorService) throws Exception{
        Set<Future<List<Relationship> >> set = new HashSet<Future<List<Relationship> >>();
        NodeMaps relMap = new NodeMaps();
        for (Node gene : nodeMap.getNodes()) {
            Callable<List<Relationship>> callable = new EdgeCallable(relIdx, nodeMap, gene);
            Future<List<Relationship>> future = executorService.submit(callable);
            set.add(future);
        }

        for (Future<List<Relationship>> future : set) {
            List<Relationship> relList = future.get();
             //put relList items into the relMap
            for(Relationship connection : relList){
                if(relTypes.containsKey(connection.getType().name())) {
                    nodeMap.addRelationship(connection);
                }
            }
        }

        return relMap;

    }

    public static boolean insertGraphNodeData(String nodeName,boolean alias,AbstractGraphDatabase graphDB,Index<Node> nodeIdx, Index<Relationship> relIdx) {

           try {
               BufferedReader vertexFile = new BufferedReader(new FileReader("/local/neo4j-server/"+nodeName+".out"));
               String vertexLine;
               log.info("Now loading values with alias=" + alias);
               boolean first = true;
               Transaction tx = graphDB.beginTx();
               try {
                   while ((vertexLine = vertexFile.readLine()) != null) {
                       //for the first line we need to get the term value, then get relationships
                       String[] vertexInfo = vertexLine.split("\t");
                       Node searchNode = nodeIdx.get("name", vertexInfo[0].toLowerCase()).getSingle();
                       if (first) {
                           first = false;
                           log.info("on first - check if already exists: " + vertexInfo[0].toLowerCase());
                           if (searchNode == null) {
                               //then go ahead and insert and continue
                               log.info("search node was null, inserting: " + vertexInfo[0].toLowerCase());
                               Node n = graphDB.createNode();
                               n.setProperty("name", vertexInfo[0].toLowerCase());
                               n.setProperty("nodeType", "deNovo");
                               nodeIdx.add(n, "name", vertexInfo[0].toLowerCase());
                               nodeIdx.add(n, "nodeType", "deNovo");
                               if (alias) {
                                   log.info("setting alias info for search node");
                                   n.setProperty("aliases", vertexInfo[1]);
                                   n.setProperty("termcount_alias", Integer.parseInt(vertexInfo[4]));
                                   nodeIdx.add(n,"aliases", vertexInfo[1]);
                                   nodeIdx.add(n,"termcount_alias", ValueContext.numeric(Integer.parseInt(vertexInfo[4])));

                               } else {
                                   log.info("setting non alias info for search node");
                                   n.setProperty("termcount", Integer.parseInt(vertexInfo[2]));
                                   nodeIdx.add(n,"termcount", ValueContext.numeric(Integer.parseInt(vertexInfo[2])));
                               }
                               searchNode=n;
                               log.info("correctly created search node");

                           } else {
                               //need to set whatever properties weren't set before
                               if (alias) {
                                   //doing alias - and it isn't set - so we are good
                                   log.info("going to insert the alias into existing term");
                                   searchNode.setProperty("aliases", vertexInfo[1]);
                                   searchNode.setProperty("termcount_alias", Integer.parseInt(vertexInfo[4]));
                                   nodeIdx.add(searchNode,"aliases", vertexInfo[1]);
                                   nodeIdx.add(searchNode,"termcount_alias", ValueContext.numeric(Integer.parseInt(vertexInfo[4])));
                               } else{
                                   log.info("Doing the non-alias version, going to insert termcount");
                                   searchNode.setProperty("termcount", Integer.parseInt(vertexInfo[2]));
                                   nodeIdx.add(searchNode,"termcount", ValueContext.numeric(Integer.parseInt(vertexInfo[2])));
                               }
                           }

                       }


                       if (alias) {

                           String gene1Name = vertexInfo[0].toLowerCase();
                           String gene2Name = vertexInfo[2].toLowerCase();
                           Node gene2 = nodeIdx.get("name", vertexInfo[2].toLowerCase()).getSingle();
                           if(gene2 != null){
                           if (!gene1Name.equals(gene2Name)) {
                               Double ngd = new Double(vertexInfo[7]);
                               if (ngd >= 0) {
                                   Relationship r = searchNode.createRelationshipTo(gene2, DynamicRelationshipType.withName("ngd_alias"));
                                   r.setProperty("ngd", Double.parseDouble(vertexInfo[7]));
                                   r.setProperty("combocount", Integer.parseInt(vertexInfo[6]));
                                   relIdx.add(r,"relType","ngd_alias");
                                   relIdx.add(r,"ngd",ValueContext.numeric(Double.parseDouble(vertexInfo[7])));
                                   relIdx.add(r,"combocount",ValueContext.numeric(Integer.parseInt(vertexInfo[6])));

                                   Relationship r2 = gene2.createRelationshipTo(searchNode, DynamicRelationshipType.withName("ngd_alias"));
                                   r2.setProperty("ngd", Double.parseDouble(vertexInfo[7]));
                                   r2.setProperty("combocount", Integer.parseInt(vertexInfo[6]));
                                   relIdx.add(r2,"relType","ngd_alias");
                                   relIdx.add(r2,"ngd",ValueContext.numeric(Double.parseDouble(vertexInfo[7])));
                                   relIdx.add(r2,"combocount",ValueContext.numeric(Integer.parseInt(vertexInfo[6])));
                               }
                           }
                           }else{
                               log.info("found nothing for: " + vertexInfo[2].toLowerCase());
                           }
                       } else {

                           String gene1Name = vertexInfo[0].toLowerCase();
                           String gene2Name = vertexInfo[1].toLowerCase();
                           Node gene2 = nodeIdx.get("name", vertexInfo[1].toLowerCase()).getSingle();
                           if(gene2 != null){
                           if (!gene1Name.equals(gene2Name)) {

                               Double ngd = new Double(vertexInfo[5]);
                               if (ngd >= 0) {
                                   Relationship r = searchNode.createRelationshipTo(gene2, DynamicRelationshipType.withName("ngd"));
                                   r.setProperty("ngd", Double.parseDouble(vertexInfo[5]));
                                   r.setProperty("combocount", Integer.parseInt(vertexInfo[4]));
                                   relIdx.add(r,"relType","ngd");
                                   relIdx.add(r,"ngd",ValueContext.numeric(Double.parseDouble(vertexInfo[5])));
                                   relIdx.add(r,"combocount",ValueContext.numeric(Integer.parseInt(vertexInfo[4])));

                                   Relationship r2 = gene2.createRelationshipTo(searchNode, DynamicRelationshipType.withName("ngd"));
                                   r2.setProperty("ngd", Double.parseDouble(vertexInfo[5]));
                                   r2.setProperty("combocount", Integer.parseInt(vertexInfo[4]));
                                   relIdx.add(r2,"relType","ngd");
                                   relIdx.add(r2,"ngd",ValueContext.numeric(Double.parseDouble(vertexInfo[5])));
                                   relIdx.add(r2,"combocount",ValueContext.numeric(Integer.parseInt(vertexInfo[4])));


                               }
                           }
                           }else{
                               log.info("found nothing for: " + vertexInfo[1].toLowerCase());
                           }
                       }

                   }
                   tx.success();
                   log.info("insert complete");
               } catch (Exception e) {
                   log.warning(e.getMessage() + " " + e.getStackTrace());
                   return false;
               } finally {
                   tx.finish();
               }

           } catch (IOException ex) {
               log.warning(ex.getMessage());
               return false;
           }

           return true;
       }

    public static NodeMaps getRelatedNodes(List<Node> nodeList, List<RelationshipType> relList) throws Exception{
        NodeMaps nodeMaps = new NodeMaps();

        TraversalDescription td =  GEN_TRAVERSAL
                    .evaluator(Evaluators.toDepth(1));

        for(RelationshipType rel : relList){
             td = td.relationships(rel);
        }

        for(Node node : nodeList){
            Iterator<Path> pathIter = td.traverse(node).iterator();
            while(pathIter.hasNext()){
                Path p = pathIter.next();

                if(p.lastRelationship() != null && p.endNode() != null){
                    log.info("path: " + p.toString());
                nodeMaps.addRelationship(p.lastRelationship());
                nodeMaps.addNode(p.endNode());
                }

            }

            nodeMaps.addNode(node);

        }


        return nodeMaps;
    }

}
