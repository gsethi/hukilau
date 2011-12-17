package org.systemsbiology.cancerregulome.hukilau.rest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.impls.StringMapJsonConfigHandler;
import org.systemsbiology.cancerregulome.hukilau.configs.Neo4jGraphJsonConfigHandler;
import org.systemsbiology.cancerregulome.hukilau.pojo.NodeMaps;
import org.systemsbiology.cancerregulome.hukilau.views.JsonNetworkView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;
import static org.apache.commons.lang.StringUtils.substringBetween;
import static org.springframework.web.bind.ServletRequestUtils.getIntParameter;
import static org.systemsbiology.cancerregulome.hukilau.utils.JsonUtils.*;
import static org.systemsbiology.cancerregulome.hukilau.utils.NetworkOps.traverseFrom;

/**
 * @author aeakin
 */
@Controller
public class QueryController implements InitializingBean {
    private static final Logger log = Logger.getLogger(QueryController.class.getName());
    private final Map<String, EmbeddedGraphDatabase> graphDbsById = new HashMap<String, EmbeddedGraphDatabase>();
    private final Map<String, String> labelsByUri = new HashMap<String, String>();

    private JsonConfig jsonConfig;

    public void setJsonConfig(JsonConfig jsonConfig) {
        this.jsonConfig = jsonConfig;
    }

    public void afterPropertiesSet() throws Exception {
        this.jsonConfig.visit(new Neo4jGraphJsonConfigHandler(graphDbsById));
        this.jsonConfig.visit(new StringMapJsonConfigHandler(labelsByUri, "label"));
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
            item.put("name", id);
            item.put("label", labelsByUri.get(graphDbUri));
            json.append("items", item);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/graphs/{graphDbId}", method = RequestMethod.GET)
    protected ModelAndView listNetwork(HttpServletRequest request, @PathVariable("graphDbId") String graphDbId) throws Exception {
//        EmbeddedGraphDatabase graphDB = graphDbsById.get(graphDbId);
        String uri = substringAfterLast(request.getRequestURI(), request.getContextPath());

        log.info("graphDbId=" + graphDbId);

        // TODO : Lookup nodes based on search criteria
        log.info("request=" + request.getParameterMap());

        JSONObject json = new JSONObject();
        json.put("uri", uri);
        json.put("name", graphDbId);

        JSONObject data = new JSONObject();
        data.put("nodes", new JSONArray());
        data.put("edges", new JSONArray());
        json.put("data", data);

        JSONObject dataSchema = new JSONObject();
        dataSchema.put("nodes", new JSONArray());
        dataSchema.put("edges", new JSONArray());
        json.put("dataSchema", dataSchema);

        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/graphs/{graphDbId}/nodes/{nodeId}", method = RequestMethod.GET)
    protected ModelAndView handleGraphRetrieval(HttpServletRequest request,
                                                @PathVariable("graphDbId") String graphDbId,
                                                @PathVariable("nodeId") String nodeId) throws Exception {
        // TODO : Lookup node by name or by ID?
        int traversalLevel = getIntParameter(request, "level", 1);

        EmbeddedGraphDatabase graphDB = graphDbsById.get(graphDbId);
        IndexManager indexMgr = graphDB.index();
        Index<Node> nodeIdx = indexMgr.forNodes("generalIdx");
        Node searchNode = nodeIdx.get("name", nodeId).getSingle();

        NodeMaps nodeMaps = traverseFrom(searchNode, traversalLevel);

        log.info("number of nodes: " + nodeMaps.numberOfNodes());
        log.info("number of relationships: " + nodeMaps.numberOfRelationships());

        String baseUri = substringBetween(request.getRequestURI(), request.getContextPath(), "/nodes");

        JSONObject data = new JSONObject();
        data.put("nodes", createNodeJSON(baseUri, nodeMaps));
        data.put("edges", createEdgeJSON(baseUri, nodeMaps));

        JSONObject dataSchema = new JSONObject();
        dataSchema.put("nodes", createSchemaJSON(nodeMaps.getNodeProperties()));
        dataSchema.put("edges", createSchemaJSON(nodeMaps.getRelationshipProperties()));

        return new ModelAndView(new JsonNetworkView()).addObject("data", data).addObject("dataSchema", dataSchema);
    }

}
