package org.systemsbiology.cancerregulome.hukilau.rest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.server.rest.domain.RelationshipDirection;
import org.neo4j.server.rest.web.DatabaseActions;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.editors.*;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.jsonconfig.impls.StringPropertyByIdMappingsHandler;
import org.systemsbiology.cancerregulome.hukilau.configs.Neo4jGraphDbMappingsHandler;
import org.systemsbiology.cancerregulome.hukilau.configs.NetworkMetadataMappingsHandler;
import org.systemsbiology.cancerregulome.hukilau.pojo.NodeMaps;
import org.systemsbiology.cancerregulome.hukilau.utils.NetworkOps;
import org.systemsbiology.cancerregulome.hukilau.views.JsonNetworkView;
import org.systemsbiology.cancerregulome.hukilau.utils.FilterUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import org.apache.lucene.search.*;
import org.apache.lucene.index.*;

import static org.apache.commons.lang.StringUtils.*;
import static org.springframework.web.bind.ServletRequestUtils.getIntParameter;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.pipe_close;
import static org.systemsbiology.cancerregulome.hukilau.utils.NetworkOps.traverseFrom;
import static org.systemsbiology.cancerregulome.hukilau.views.JsonNetworkView.BASE_URI;
import static org.systemsbiology.cancerregulome.hukilau.views.JsonNetworkView.NODE_MAPS;

/**
 * @author aeakin
 */
@Controller
public class QueryController implements InitializingBean {
    private static final Logger log = Logger.getLogger(QueryController.class.getName());
    private ExecutorService executorService;
    private final Map<String, AbstractGraphDatabase> graphDbsById = new HashMap<String, AbstractGraphDatabase>();
    private final Map<String, String> labelsById = new HashMap<String, String>();
    private final Map<String, String> nodeIdxById = new HashMap<String, String>();
    private final Map<String, String> relIdxById = new HashMap<String, String>();
    private final Map<String, JSONObject> networkMetadataById = new HashMap<String, JSONObject>();

    private ServiceConfig serviceConfig;

    public void setServiceConfig(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public void afterPropertiesSet() throws Exception {
        this.serviceConfig.visit(new Neo4jGraphDbMappingsHandler(graphDbsById));
        this.serviceConfig.visit(new StringPropertyByIdMappingsHandler(labelsById, "label"));
        this.serviceConfig.visit(new StringPropertyByIdMappingsHandler(nodeIdxById, "nodeIdx"));
        this.serviceConfig.visit(new StringPropertyByIdMappingsHandler(relIdxById, "relIdx"));
        this.serviceConfig.visit(new NetworkMetadataMappingsHandler(networkMetadataById));
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(JSONObject.class, new JSONObjectPropertyEditor());
    }

    /*
    * Controller Methods
    */
    @RequestMapping(value = "/**/graphs", method = RequestMethod.GET)
    protected ModelAndView listNetworks(HttpServletRequest request) throws Exception {
        String uri = substringAfterLast(request.getRequestURI(), request.getContextPath());

        JSONObject json = new JSONObject();

        for (String id : graphDbsById.keySet()) {
            JSONObject item = new JSONObject();
            String graphDbUri = uri + "/" + id;
            item.put("uri", graphDbUri);
            item.put("id", id);
            item.put("name", id);
            item.put("label", labelsById.get(id));
            json.append("items", item);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/graphs/{graphDbId}", method = RequestMethod.GET)
    protected ModelAndView listNetwork(HttpServletRequest request, @PathVariable("graphDbId") String graphDbId) throws Exception {
        String uri = substringAfterLast(request.getRequestURI(), request.getContextPath());
        log.info("graphDbId=" + graphDbId);

        JSONObject json = new JSONObject();
        json.put("uri", uri);
        json.put("id", graphDbId);
        json.put("name", graphDbId);
        json.put("label", labelsById.get(graphDbId));

        JSONObject dataSchema = new JSONObject();
        dataSchema.put("nodes", new JSONArray());
        dataSchema.put("edges", new JSONArray());
        json.put("dataSchema", dataSchema);

        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/graphs/{graphDbId}/metadata", method = RequestMethod.GET)
    protected ModelAndView networkMetadata(@PathVariable("graphDbId") String graphDbId) throws Exception {
        log.info("graphDbId=" + graphDbId);

        if (!networkMetadataById.containsKey(graphDbId)) {
            throw new ResourceNotFoundException(graphDbId);
        }

        return new ModelAndView(new JsonView()).addObject("json", networkMetadataById.get(graphDbId));
    }

    @RequestMapping(value = "/**/graphs/{graphDbId}/relationships/{nodeName}", method = RequestMethod.POST)
    protected ModelAndView handleNodeInsert(HttpServletRequest request, @PathVariable("graphDbId") String graphDbId,@PathVariable("nodeName") String nodeName) throws Exception {


        AbstractGraphDatabase graphDB = getGraphDb(graphDbId);
        IndexManager indexMgr = graphDB.index();
        Index<Node> nodeIdx = indexMgr.forNodes(nodeIdxById.get(graphDbId));
        Index<Relationship> relIdx = indexMgr.forRelationships(relIdxById.get(graphDbId));

        Node node = nodeIdx.get("name",nodeName).getSingle();

        NetworkOps.insertGraphNodeData(nodeName,Boolean.parseBoolean(request.getParameter("alias")),graphDB, nodeIdx,relIdx);
        NodeMaps nodeMaps = new NodeMaps();

        String baseUri = substringBetween(request.getRequestURI(), request.getContextPath(), "/relationships");

        return new ModelAndView(new JsonNetworkView()).addObject(NODE_MAPS, nodeMaps).addObject(BASE_URI, baseUri);
    }

    @RequestMapping(value = "/**/graphs/{graphDbId}/nodes/export/{nodeName}", method = RequestMethod.GET)
      protected ModelAndView exportNetwork(HttpServletRequest request, HttpServletResponse response, @PathVariable("nodeName") String nodeName,
                                           @PathVariable("graphDbId") String graphDbId) throws Exception {

          String dataFormat = request.getParameter("type");
          if (dataFormat.toLowerCase().equals("csv")) {
              response.setContentType("text/csv");
              response.setHeader("Content-Disposition", "attachment;filename=nodes.csv");
          } else if (dataFormat.toLowerCase().equals("tsv")) {
              response.setContentType("text/tsv");
              response.setHeader("Content-Disposition", "attachment;filename=nodes.tsv");
          }



        AbstractGraphDatabase graphDB = getGraphDb(graphDbId);
        IndexManager indexMgr = graphDB.index();
        Index<Node> nodeIdx = indexMgr.forNodes(nodeIdxById.get(graphDbId));
        RelationshipIndex relIdx= indexMgr.forRelationships(relIdxById.get(graphDbId));

        Node searchNode = nodeIdx.get("name",nodeName).getSingle();

          boolean alias = new Boolean(request.getParameter("alias")).booleanValue();
         String relType="ngd";
        if(alias){
            relType="ngd_alias";
        }

          try {

              BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream());

              String csvLine = "gene,singlecount,ngd,combocount\n";
              out.write(csvLine.getBytes());

              csvLine = nodeName + "," + searchNode.getProperty("termcount", 0) + ",0," + searchNode.getProperty("termcount", 0) + "\n";
              out.write(csvLine.getBytes());


              //go thru ngd relationships and create an array of all node objects that have an ngd distance from the search term
               IndexHits<Relationship> ngdHits = relIdx.get("relType", relType, searchNode, null);
              for (Relationship rel : ngdHits) {
                  JSONObject relJson = new JSONObject();
                  Node gene = rel.getEndNode();
                  csvLine = gene.getProperty("name") + "," + gene.getProperty("termcount", 0) + "," + rel.getProperty("ngd") + "," + rel.getProperty("combocount") + "\n";
                  out.write(csvLine.getBytes());
              }


              out.flush();
              out.close();

          } catch (Exception e) {
              log.info("exception occurred: " + e.getMessage());
              return null;
          }

          return null;
      }

      @RequestMapping(value = "/exportGraph", method = RequestMethod.POST)
      protected ModelAndView exportGraph(HttpServletRequest request, HttpServletResponse response) throws Exception {
          String requestUri = request.getRequestURI();
          log.info(requestUri);
          String dataFormat = request.getParameter("type");
          log.info(request.getRequestURI() + "," + request.getMethod() + dataFormat);

          if (dataFormat.toLowerCase().equals("png")) {
              response.setContentType("image/png");
              response.setHeader("Content-Disposition", "attachment;filename=graph.png");
          } else if (dataFormat.toLowerCase().equals("svg")) {
              response.setContentType("image/svg+xml");
              response.setHeader("Content-Disposition", "attachment;filename=graph.svg");
          } else if (dataFormat.toLowerCase().equals("pdf")) {
              response.setContentType("application/pdf");
              response.setHeader("Content-Disposition", "attachment;filename=graph.pdf");
          }

          log.info("input stream length: " + request.getContentLength());

          pipe_close(request.getInputStream(), response.getOutputStream());
          return null;
      }



    @RequestMapping(value = "/**/graphs/{graphDbId}/nodes/{nodeId}", method = RequestMethod.GET)
    protected ModelAndView retrieveNode(HttpServletRequest request,
                                        @PathVariable("graphDbId") String graphDbId,
                                        @PathVariable("nodeId") String nodeId) throws Exception {
        // TODO : Lookup node by name or by ID?
        int traversalLevel = getIntParameter(request, "level", 1);

        AbstractGraphDatabase graphDB = getGraphDb(graphDbId);
        IndexManager indexMgr = graphDB.index();
        Index<Node> nodeIdx = indexMgr.forNodes(nodeIdxById.get(graphDbId));
        Node searchNode = nodeIdx.get("name", nodeId).getSingle();

        NodeMaps nodeMaps = traverseFrom(traversalLevel,null, searchNode);
        String baseUri = substringBetween(request.getRequestURI(), request.getContextPath(), "/nodes");

        return new ModelAndView(new JsonNetworkView()).addObject(NODE_MAPS, nodeMaps).addObject(BASE_URI, baseUri);
    }

    //query is a JSONObject to define the nodes and relationships to query for each level of the query
    //ex: {nodes:[{"type":"gene"}],relationships:[{"type":"ngd",direction:"out"}]
    //nodeSet is a JSONObject to define the nodes to start the query from
    //ex:{"name":"gene1","text":"thisisit"}
    @RequestMapping(value = "/**/graphs/{graphDbId}/query", method = RequestMethod.GET)
    protected ModelAndView queryGraph(HttpServletRequest request,
                                      @PathVariable("graphDbId") String graphDbId,
                                      @RequestParam("query") String query,
                                      @RequestParam("nodeSet") String nodeSet) throws Exception {
        // TODO : Lookup node by name or by ID?
        int traversalLevel = getIntParameter(request, "level", 1);

        if (isEmpty(query)) {
            throw new InvalidSyntaxException("missing 'query' object");
        }

        JSONObject nodeSetJSON = new JSONObject(nodeSet);

        AbstractGraphDatabase graphDB = getGraphDb(graphDbId);
        IndexManager indexMgr = graphDB.index();
        Index<Node> nodeIdx = indexMgr.forNodes(nodeIdxById.get(graphDbId));

        ArrayList<Node> searchNodes = new ArrayList<Node>();
        Iterator itr = nodeSetJSON.keys();
        while (itr.hasNext()) {
            String key = (String) itr.next();
            String value = nodeSetJSON.getString(key);
            searchNodes.add(nodeIdx.get(key, value).getSingle());
        }

        JSONObject queryJson = new JSONObject(query);

        NodeMaps nodeMaps = traverseFrom(traversalLevel,queryJson, searchNodes.toArray(new Node[searchNodes.size()]));
        String baseUri = substringBetween(request.getRequestURI(), request.getContextPath(), "/query");

        return new ModelAndView(new JsonNetworkView()).addObject(NODE_MAPS, nodeMaps).addObject(BASE_URI, baseUri);
    }

     //retrieves nodes that are connected to the initial nodeSet by the set of relationships in the relationshipSet
    //relationshipSet is the list of types of relationships to traverse out to attached nodes
    //ex: [{"name":"ngd"},{"name":"domine"}]
    //if relationshipSet is empty, then all relationship types are used
    @RequestMapping(value = "/**/graphs/{graphDBId}/nodes/query", method = RequestMethod.GET)
    protected ModelAndView retrieveNodesByRelationship(HttpServletRequest request, @PathVariable("graphDBId") String graphDbId,
                                                       @RequestParam("nodeSet") String nodeSet,
                                                @RequestParam("relationshipSet") String relationshipSet) throws Exception {

        if (isEmpty(nodeSet)) {
            throw new InvalidSyntaxException("missing 'nodeSet' object");
        }

        log.info("nodeSet: " + nodeSet);
        log.info("relationshipSet: " + relationshipSet);
        JSONArray nodeSetJson = new JSONArray(nodeSet);

        List<RelationshipType> relationshipList = new ArrayList<RelationshipType>();
        if(!isEmpty(relationshipSet)){
            JSONArray relationshipSetJson = new JSONArray(relationshipSet);
            for(int i=0; i< relationshipSetJson.length(); i++){
                JSONObject relObj = relationshipSetJson.getJSONObject(i);
                relationshipList.add(DynamicRelationshipType.withName((String) relObj.get("name")));
            }
        }

        AbstractGraphDatabase graphDB = getGraphDb(graphDbId);
        IndexManager indexMgr = graphDB.index();
        Index<Node> nodeIdx = indexMgr.forNodes(nodeIdxById.get(graphDbId));


        //get all node objects from index
        List<Node> nodeList = new ArrayList<Node>();
        for(int i=0; i< nodeSetJson.length(); i++){
            JSONObject nodeItem = (JSONObject) nodeSetJson.get(i);
            String key = (String)nodeItem.keys().next();
             log.info("node item: " + nodeItem.get(key));
            Node node = nodeIdx.get(key,nodeItem.get(key)).getSingle();
            if(node != null){
                nodeList.add(nodeIdx.get(key, nodeItem.get(key)).getSingle());
            }
        }

        //get all relationships between the nodes, nodeMaps will be updated.
        NodeMaps nodeMaps = NetworkOps.getRelatedNodes(nodeList, relationshipList);

        String baseUri = substringBetween(request.getRequestURI(), request.getContextPath(), "/nodes");
        return new ModelAndView(new JsonNetworkView()).addObject(NODE_MAPS, nodeMaps).addObject(BASE_URI, baseUri);



    }

    //retrieves relationships amongst all nodes specified in the nodeSet parameter.
    // nodeset is a JSONArray containing JSONObjects with the node key and node value to lookup specific nodes.
    //ex: [{"name":"fbxw7"},{"name":"skp1"}]
    //relationshipSet is the list of types of relationships to return, if none specified, then all types are returned
    //ex: [{"name":"ngd"},{"name":"domine"}]
    @RequestMapping(value = "/**/graphs/{graphDBId}/relationships/query", method = RequestMethod.GET)
    protected ModelAndView retrieveRelationships(HttpServletRequest request, @PathVariable("graphDBId") String graphDbId,
                                                @RequestParam("nodeSet") String nodeSet,
                                                @RequestParam("relationshipSet") String relationshipSet) throws Exception {

        if (isEmpty(nodeSet)) {
            throw new InvalidSyntaxException("missing 'nodeSet' object");
        }

        JSONArray nodeSetJson = new JSONArray(nodeSet);

        Map<String,String> relationshipMap = new HashMap<String, String>();
        if(!isEmpty(relationshipSet)){
            JSONArray relationshipSetJson = new JSONArray(relationshipSet);
            for(int i=0; i< relationshipSetJson.length(); i++){
                JSONObject relObj = relationshipSetJson.getJSONObject(i);
                relationshipMap.put((String) relObj.get("name"), (String) relObj.get("name"));

            }
        }

        AbstractGraphDatabase graphDB = getGraphDb(graphDbId);
        IndexManager indexMgr = graphDB.index();
        Index<Node> nodeIdx = indexMgr.forNodes(nodeIdxById.get(graphDbId));
        RelationshipIndex relIdx = indexMgr.forRelationships(relIdxById.get(graphDbId));

        //get all node objects from index
        NodeMaps nodeMaps = new NodeMaps();
        for(int i=0; i< nodeSetJson.length(); i++){
            JSONObject nodeItem = (JSONObject) nodeSetJson.get(i);
            String key = (String)nodeItem.keys().next();
            nodeMaps.addNode(nodeIdx.get(key, nodeItem.get(key)).getSingle());
        }

        //get all relationships between the nodes, nodeMaps will be updated.
        NodeMaps relMap = NetworkOps.getRelationships(nodeMaps, relIdx, relationshipMap, executorService);

        log.info("number of relationships " + relMap.numberOfRelationships());
        String baseUri = substringBetween(request.getRequestURI(), request.getContextPath(), "/relationships");
        return new ModelAndView(new JsonNetworkView()).addObject(NODE_MAPS, nodeMaps).addObject(BASE_URI, baseUri);



    }

    private AbstractGraphDatabase getGraphDb(String graphDbId) throws ResourceNotFoundException {
        if (!this.graphDbsById.containsKey(graphDbId)) {
            throw new ResourceNotFoundException(graphDbId);
        }
        AbstractGraphDatabase graphDb = graphDbsById.get(graphDbId);
        if (graphDb == null) {
            throw new ResourceNotFoundException(graphDbId);
        }
        return graphDb;
    }

    @RequestMapping(value = "/**/graphs/{graphDbId}/filter", method = RequestMethod.POST)
    protected ModelAndView filterNodes(HttpServletRequest request,
                                       @PathVariable("graphDbId") String graphDbId,
                                       @RequestParam("filter_config") JSONObject filter_config) throws Exception {
        BooleanQuery node_query = null;

        try {
            JSONArray node_filter_list = filter_config.getJSONArray("nodes");
            node_query = FilterUtils.buildBooleanQuery(node_filter_list);
        }
        catch (JSONException e) {
            throw new InvalidSyntaxException(e.getMessage());
        }
        
        AbstractGraphDatabase graphDB = getGraphDb(graphDbId);
        IndexManager indexMgr = graphDB.index();
        Index<Node> nodeIdx = indexMgr.forNodes(nodeIdxById.get(graphDbId));

        IndexHits<Node> hits = nodeIdx.query(node_query);
        NodeMaps nodeMaps = new NodeMaps();

        for (Node node : hits) {
            nodeMaps.addNode(node);
        }

        String baseUri = substringBetween(request.getRequestURI(), request.getContextPath(), "/filter");

        return new ModelAndView(new JsonNetworkView()).addObject(NODE_MAPS, nodeMaps).addObject(BASE_URI, baseUri);
    }

    public void cleanUp() {
        log.info("executor service shutdown: started");
        try {
            this.executorService.shutdown();
        } catch (Exception e) {
            log.warning("executor service shutdown: " + e.getMessage());
        } finally {
            log.info("executor service shutdown: complete");
        }

    }

    public void setExecutorService(ExecutorService es){
        this.executorService=es;
    }
}
