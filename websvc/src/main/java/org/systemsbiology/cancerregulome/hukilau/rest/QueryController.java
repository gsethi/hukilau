package org.systemsbiology.cancerregulome.hukilau.rest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.EmbeddedServerConfigurator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.cancerregulome.hukilau.pojo.NodeMaps;
import org.systemsbiology.cancerregulome.hukilau.views.JsonNetworkView;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

import static org.systemsbiology.cancerregulome.hukilau.utils.JsonUtils.*;
import static org.systemsbiology.cancerregulome.hukilau.utils.NetworkOps.traverseFrom;

/**
 * @author aeakin
 */
@Controller
public class QueryController {
    private static final Logger log = Logger.getLogger(QueryController.class.getName());
    private EmbeddedGraphDatabase graphDB;

    public void setGraphDB(EmbeddedGraphDatabase graphDB) {
        this.graphDB = graphDB;
    }

    /*
    * Controller Methods
    */
    @RequestMapping(value = "/graphs", method = RequestMethod.GET)
    protected ModelAndView listNetworks() throws Exception {
        JSONObject json = new JSONObject();
        // TODO: Add list of networks
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/graphs/{networkId}", method = RequestMethod.GET)
    protected ModelAndView listNetwork(HttpServletRequest request, @PathVariable("networkId") String networkId) throws Exception {
        log.info("networkId=" + networkId);
        log.info("request=" + request.getParameterMap());

        // TODO : Lookup nodes based on search criteria

        JSONObject data = new JSONObject();
        data.put("nodes", new JSONArray());
        data.put("edges", new JSONArray());

        JSONObject dataSchema = new JSONObject();
        dataSchema.put("nodes", new JSONArray());
        dataSchema.put("edges", new JSONArray());

        return new ModelAndView(new JsonNetworkView()).addObject("data", data).addObject("dataSchema", dataSchema);
    }

    @RequestMapping(value = "/graph/{nodeId}", method = RequestMethod.GET)
    protected ModelAndView handleGraphRetrieval(HttpServletRequest request, @PathVariable("nodeId") String nodeId) throws Exception {
        // TODO : Lookup network first, then node in network
        // TODO : Lookup node by name
        String requestUri = request.getRequestURI();
        log.info(requestUri);
        int traversalLevel = 1; //default to 1
        if (request.getParameter("level") != null) {
            traversalLevel = Integer.parseInt(request.getParameter("level"));
        }

        IndexManager indexMgr = this.graphDB.index();
        Index<Node> nodeIdx = indexMgr.forNodes("generalIdx");
        Node searchNode = nodeIdx.get("name", nodeId).getSingle();

        NodeMaps nodeMaps = traverseFrom(searchNode, traversalLevel);

        log.info("number of nodes: " + nodeMaps.numberOfNodes());
        log.info("number of relationships: " + nodeMaps.numberOfRelationships());

        JSONObject data = new JSONObject();
        data.put("nodes", createNodeJSON(nodeMaps));
        data.put("edges", createEdgeJSON(nodeMaps));

        JSONObject dataSchema = new JSONObject();
        dataSchema.put("nodes", createSchemaJSON(nodeMaps.getNodeProperties()));
        dataSchema.put("edges", createSchemaJSON(nodeMaps.getRelationshipProperties()));

        return new ModelAndView(new JsonNetworkView()).addObject("data", data).addObject("dataSchema", dataSchema);
    }

}
