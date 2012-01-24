package org.systemsbiology.cancerregulome.hukilau.rest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.jsonconfig.impls.StringPropertyByIdMappingsHandler;
import org.systemsbiology.cancerregulome.hukilau.configs.Neo4jGraphDbMappingsHandler;
import org.systemsbiology.cancerregulome.hukilau.pojo.NodeMaps;
import org.systemsbiology.cancerregulome.hukilau.views.JsonNetworkView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.*;
import static org.springframework.web.bind.ServletRequestUtils.getIntParameter;
import static org.systemsbiology.cancerregulome.hukilau.utils.NetworkOps.traverseFrom;
import static org.systemsbiology.cancerregulome.hukilau.views.JsonNetworkView.BASE_URI;
import static org.systemsbiology.cancerregulome.hukilau.views.JsonNetworkView.NODE_MAPS;

/**
 * @author aeakin
 */
@Controller
public class QueryController implements InitializingBean {
    private static final Logger log = Logger.getLogger(QueryController.class.getName());
    private final Map<String, AbstractGraphDatabase> graphDbsById = new HashMap<String, AbstractGraphDatabase>();
    private final Map<String, String> labelsById = new HashMap<String, String>();
    private final Map<String, String> nodeIdxById = new HashMap<String, String>();

    private ServiceConfig serviceConfig;

    public void setServiceConfig(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public void afterPropertiesSet() throws Exception {
        this.serviceConfig.visit(new Neo4jGraphDbMappingsHandler(graphDbsById));
        this.serviceConfig.visit(new StringPropertyByIdMappingsHandler(labelsById, "label"));
        this.serviceConfig.visit(new StringPropertyByIdMappingsHandler(nodeIdxById, "nodeIdx"));
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

//        AbstractGraphDatabase graphDb = getGraphDb(graphDbId);
//        IndexManager indexMgr = graphDb.index("generalIdx");

        JSONObject json = new JSONObject();
        json.put("uri", uri);
        json.put("id", graphDbId);
        json.put("name", graphDbId);
        json.put("label", labelsById.get(graphDbId));

        // TODO : Need to add node types, edge types, and property data types to the response
//            json.append("nodeTypes", new JSONObject().put("name", niName).put("uri", uri + "/nodeTypes/" + niName));
//        addNumberOf(json, "nodeTypes");

//            json.append("edgeTypes", new JSONObject().put("name", riName).put("uri", uri + "/edgeTypes/" + riName));
//        addNumberOf(json, "edgeTypes");

        JSONObject dataSchema = new JSONObject();
        dataSchema.put("nodes", new JSONArray());
        dataSchema.put("edges", new JSONArray());
        json.put("dataSchema", dataSchema);

        return new ModelAndView(new JsonView()).addObject("json", json);
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

        NodeMaps nodeMaps = traverseFrom(traversalLevel, searchNode);
        String baseUri = substringBetween(request.getRequestURI(), request.getContextPath(), "/nodes");

        return new ModelAndView(new JsonNetworkView()).addObject(NODE_MAPS, nodeMaps).addObject(BASE_URI, baseUri);
    }

    @RequestMapping(value = "/**/graphs/{graphDbId}/query", method = RequestMethod.GET)
    protected ModelAndView queryGraph(HttpServletRequest request,
                                      @PathVariable("graphDbId") String graphDbId,
                                      @RequestParam("query") String query) throws Exception {
        // TODO : Lookup node by name or by ID?
        int traversalLevel = getIntParameter(request, "level", 1);

        if (isEmpty(query)) {
            throw new InvalidSyntaxException("missing 'query' object");
        }

        JSONObject queryJson = new JSONObject(query);

        AbstractGraphDatabase graphDB = getGraphDb(graphDbId);
        IndexManager indexMgr = graphDB.index();
        Index<Node> nodeIdx = indexMgr.forNodes(nodeIdxById.get(graphDbId));

        ArrayList<Node> searchNodes = new ArrayList<Node>();
        Iterator itr = queryJson.keys();
        while (itr.hasNext()) {
            String key = (String) itr.next();
            String value = queryJson.getString(key);
            searchNodes.add(nodeIdx.get(key, value).getSingle());
        }

        NodeMaps nodeMaps = traverseFrom(traversalLevel, searchNodes.toArray(new Node[searchNodes.size()]));
        String baseUri = substringBetween(request.getRequestURI(), request.getContextPath(), "/query");

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
}
