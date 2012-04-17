package org.systemsbiology.cancerregulome.hukilau.rest;

import org.apache.lucene.search.BooleanQuery;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.editors.JSONObjectPropertyEditor;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.jsonconfig.impls.StringPropertyByIdMappingsHandler;
import org.systemsbiology.cancerregulome.hukilau.configs.Neo4jGraphDbMappingsHandler;
import org.systemsbiology.cancerregulome.hukilau.configs.NetworkMetadataMappingsHandler;
import org.systemsbiology.cancerregulome.hukilau.pojo.NodeMaps;
import org.systemsbiology.cancerregulome.hukilau.utils.FilterUtils;
import org.systemsbiology.cancerregulome.hukilau.views.JsonNetworkView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.Long.parseLong;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;
import static org.springframework.web.bind.ServletRequestUtils.getIntParameter;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.getURI;
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
    private final Map<String, JSONObject> networkMetadataById = new HashMap<String, JSONObject>();

    private ServiceConfig serviceConfig;

    public void setServiceConfig(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public void afterPropertiesSet() throws Exception {
        this.serviceConfig.visit(new Neo4jGraphDbMappingsHandler(graphDbsById));
        this.serviceConfig.visit(new StringPropertyByIdMappingsHandler(labelsById, "label"));
        this.serviceConfig.visit(new StringPropertyByIdMappingsHandler(nodeIdxById, "nodeIdx", "genNodeIdx"));
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
        String uri = getURI(request);

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
        String uri = getURI(request);
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

    @RequestMapping(value = "/**/graphs/{graphDbId}/nodes/{nodeId}", method = RequestMethod.GET)
    protected ModelAndView retrieveNode(HttpServletRequest request,
                                        @PathVariable("graphDbId") String graphDbId,
                                        @PathVariable("nodeId") String nodeId,
                                        @RequestParam(value="filter_config", required = false) JSONObject filter_config) throws Exception {
        int traversalLevel = getIntParameter(request, "level", 1);

        AbstractGraphDatabase graphDB = getGraphDb(graphDbId);
        Node targetNode = graphDB.getNodeById(parseLong(nodeId));

        NodeMaps resultNodeMaps = traverseFrom(traversalLevel, targetNode);
        String baseUri = substringBeforeLast(getURI(request), "/nodes");

        if (filter_config != null) {
            JSONArray node_filter_list = null;
            JSONArray edge_filter_list = null;

            try {
                node_filter_list = filter_config.getJSONArray("nodes");
                edge_filter_list = filter_config.getJSONArray("edges");
            } catch (JSONException e) {
                throw new InvalidSyntaxException(e.getMessage());
            }

            NodeMaps filtered = FilterUtils.filterNodeMaps(resultNodeMaps, node_filter_list, edge_filter_list);
            return new ModelAndView(new JsonNetworkView()).addObject(NODE_MAPS, filtered).addObject(BASE_URI, baseUri);
        }
        else {
            return new ModelAndView(new JsonNetworkView()).addObject(NODE_MAPS, resultNodeMaps).addObject(BASE_URI, baseUri);
        }
    }

    @RequestMapping(value = "/**/graphs/{graphDbId}/query", method = RequestMethod.POST)
    protected ModelAndView queryGraph(HttpServletRequest request,
                                      @PathVariable("graphDbId") String graphDbId,
                                      @RequestParam("query") JSONObject queryJson,
                                      @RequestParam(value="filter_config", required = false) JSONObject filter_config) throws Exception {
        int traversalLevel = getIntParameter(request, "level", 1);

        AbstractGraphDatabase graphDB = getGraphDb(graphDbId);
        IndexManager indexMgr = graphDB.index();
        Index<Node> nodeIdx = indexMgr.forNodes(nodeIdxById.get(graphDbId));

        ArrayList<Node> searchNodes = new ArrayList<Node>();
        Iterator itr = queryJson.keys();
        while (itr.hasNext()) {
            String key = (String) itr.next();
            String value = queryJson.getString(key);

            for (Node node : nodeIdx.get(key, value)) {
                searchNodes.add(node);
            }
        }

        NodeMaps queryNodeMaps = traverseFrom(traversalLevel, searchNodes.toArray(new Node[searchNodes.size()]));
        String baseUri = substringBeforeLast(getURI(request), "/query");

        if (filter_config != null) {
            JSONArray node_filter_list = null;
            JSONArray edge_filter_list = null;

            try {
                node_filter_list = filter_config.getJSONArray("nodes");
                edge_filter_list = filter_config.getJSONArray("edges");
            } catch (JSONException e) {
                throw new InvalidSyntaxException(e.getMessage());
            }

            NodeMaps filtered = FilterUtils.filterNodeMaps(queryNodeMaps, node_filter_list, edge_filter_list);
            return new ModelAndView(new JsonNetworkView()).addObject(NODE_MAPS, filtered).addObject(BASE_URI, baseUri);
        }
        else {
            return new ModelAndView(new JsonNetworkView()).addObject(NODE_MAPS, queryNodeMaps).addObject(BASE_URI, baseUri);
        }
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
        BooleanQuery node_query;

        try {
            JSONArray node_filter_list = filter_config.getJSONArray("nodes");
            node_query = FilterUtils.buildBooleanQuery(node_filter_list);
        } catch (JSONException e) {
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

        String baseUri = substringBeforeLast(getURI(request), "/filter");

        return new ModelAndView(new JsonNetworkView()).addObject(NODE_MAPS, nodeMaps).addObject(BASE_URI, baseUri);
    }
}
