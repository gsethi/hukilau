package org.systemsbiology.cancerregulome.hukilau.rest;

import org.codehaus.groovy.runtime.ConvertedClosure;
import org.json.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.EmbeddedServerConfigurator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonView;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author aeakin
 */
@Controller
public class QueryController  {
    private static final Logger log = Logger.getLogger(QueryController.class.getName());
    private EmbeddedGraphDatabase graphDB;
    private WrappingNeoServerBootstrapper neoServer;

    @RequestMapping(value = "/graph/{nodeId}", method = RequestMethod.GET)
    protected ModelAndView handleGraphRetrieval(HttpServletRequest request, @PathVariable("nodeId") String nodeId ) throws Exception {
        String requestUri = request.getRequestURI();
        log.info(requestUri);
        int traversalLevel = 1; //default to 1
        if(request.getParameter("level") != null){
           traversalLevel= Integer.parseInt(request.getParameter("level"));
        }


        Map<Long,Node> nodeMap = new HashMap<Long, Node>();
        Map<Long,Relationship> relMap = new HashMap<Long, Relationship>();
        List<Node> unProcessedNodes = new ArrayList<Node>();
        List<Node> inProcessNodes = new ArrayList<Node>();

        IndexManager indexMgr = graphDB.index();
        Index<Node> nodeIdx = indexMgr.forNodes("generalIdx");
        Node searchNode = nodeIdx.get("name", nodeId).getSingle();

        //retrieve all nodes and relationships
        retrieveNodesAndEdges(traversalLevel, nodeMap, relMap, searchNode);

        log.info("number of nodes: " + nodeMap.values().size() + " number of rel: " + relMap.values().size());
         //now create node and edge JSON from the maps
         //need to keep track of node and edge properties for schema
        Map<String,String> nodePropMap = new HashMap<String, String>();
        Map<String,String> relPropMap = new HashMap<String, String>();

        JSONArray nodeJSONArray = new JSONArray();
        createNodeJSON(nodeMap, nodePropMap, nodeJSONArray);

        JSONArray edgeJSONArray = new JSONArray();
        createEdgeJSON(relMap, relPropMap, edgeJSONArray);

        //now create schema - only loading strings at this point as data types
        //TODO: need to load into db different datatypes for properties and then retrieve them here appropriately
        JSONArray npJSONArray = new JSONArray();
        createSchemaJSON(nodePropMap, npJSONArray);

        JSONArray epJSONArray = new JSONArray();
        createSchemaJSON(relPropMap, epJSONArray);

        JSONObject data = new JSONObject();
        data.put("nodes", nodeJSONArray);
        data.put("edges", edgeJSONArray);

        JSONObject dataSchema = new JSONObject();
        dataSchema.put("nodes", npJSONArray);
        dataSchema.put("edges", epJSONArray);

        JSONObject json = new JSONObject();
        json.put("data", data);
        json.put("dataSchema", dataSchema);
        return new ModelAndView(new JsonView()).addObject("json", json);

    }

    private void retrieveNodesAndEdges(int traversalLevel, Map<Long, Node> nodeMap, Map<Long, Relationship> relMap, Node searchNode) {
        List<Node> unProcessedNodes = new ArrayList<Node>();
        List<Node> inProcessNodes = new ArrayList<Node>();

        inProcessNodes.add(searchNode);
        for(int i=0; i<traversalLevel; i++){
            for(Node pNode : inProcessNodes){
                //add search Node to the node map
                nodeMap.put(pNode.getId(),pNode);

                //getting both outbound and inbound relationships to the node
                for(Relationship rel: pNode.getRelationships()){
                    //put relationship in map for edges
                    relMap.put(rel.getId(),rel);
                    //put other node of relationship in list to be processed if it hasn't been processed already
                    if(!nodeMap.containsKey(rel.getOtherNode(pNode).getId())){
                        unProcessedNodes.add(rel.getOtherNode(pNode));
                    }
                }
            }
             //transfer unProcessed to processing
            inProcessNodes.clear();
            for(Node n : unProcessedNodes){
                inProcessNodes.add(n);
            }

            unProcessedNodes.clear();
        }
    }

    private void createEdgeJSON(Map<Long, Relationship> relMap, Map<String, String> relPropMap, JSONArray edgeJSONArray) throws JSONException {
        for(Relationship r : relMap.values()){
            JSONObject edgeJSON = new JSONObject();
            edgeJSON.put("id",r.getId());
            edgeJSON.put("source",r.getStartNode().getId());
            edgeJSON.put("target",r.getEndNode().getId());

            for(String propKey : r.getPropertyKeys()){
                edgeJSON.put(propKey,r.getProperty(propKey));
                relPropMap.put(propKey,propKey);
            }

            edgeJSONArray.put(edgeJSON);
        }
    }

    private void createNodeJSON(Map<Long, Node> nodeMap, Map<String, String> nodePropMap, JSONArray nodeJSONArray) throws JSONException {
        for(Node n : nodeMap.values()){
            JSONObject nodeJSON = new JSONObject();
            nodeJSON.put("id",n.getId());
            for(String propKey : n.getPropertyKeys()){
                nodeJSON.put(propKey,n.getProperty(propKey));
                nodePropMap.put(propKey,propKey);
            }

            nodeJSONArray.put(nodeJSON);
        }
    }

    private void createSchemaJSON(Map<String, String> nodePropMap, JSONArray npJSONArray) throws JSONException {
        for(String nProp : nodePropMap.values()){
            JSONObject nodePropJSON = new JSONObject();
            nodePropJSON.put("name",nProp);
            nodePropJSON.put("type","string");
            npJSONArray.put(nodePropJSON);
        }
    }


    public void cleanUp() {
        this.neoServer.stop();
        this.graphDB.shutdown();
    }

    public void setGraphDB(EmbeddedGraphDatabase graphDB) {
        this.graphDB = graphDB;
        EmbeddedServerConfigurator config = new EmbeddedServerConfigurator(graphDB);
        //TODO: Could put this in a config file....
        config.configuration().setProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, 7474);
        config.configuration().setProperty(Configurator.WEBSERVER_ADDRESS_PROPERTY_KEY, "0.0.0.0");
        this.neoServer = new WrappingNeoServerBootstrapper(graphDB, config);
        neoServer.start();

    }
}
