package org.systemsbiology.cancerregulome.hukilau.utils;


import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.graphdb.index.BatchInserterIndex;
import org.neo4j.graphdb.index.BatchInserterIndexProvider;
import org.neo4j.index.impl.lucene.LuceneBatchInserterIndexProvider;
import org.neo4j.kernel.impl.batchinsert.BatchInserter;
import org.neo4j.kernel.impl.batchinsert.BatchInserterImpl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static org.neo4j.graphdb.DynamicRelationshipType.withName;
import static org.neo4j.helpers.collection.MapUtil.map;
import static org.neo4j.helpers.collection.MapUtil.stringMap;
import static org.neo4j.index.lucene.ValueContext.numeric;
import static org.systemsbiology.cancerregulome.hukilau.utils.GraphDBConfiguration.loadConfiguration;


/**
 * @author aeakin
 */
public class DBImport {
    private static final Logger log = Logger.getLogger(DBImport.class.getName());

    private static BatchInserter batchInserter;
    private static BatchInserterIndex nodeIndex;
    private static BatchInserterIndex relationshipIndex;
    private static NetworkConfiguration networkConfiguration;

    public static void main(String[] args) throws Exception {
        networkConfiguration = new NetworkConfiguration(loadConfiguration());

        batchInserter = new BatchInserterImpl(networkConfiguration.getDatabaseRootPath());
        BatchInserterIndexProvider indexProvider = new LuceneBatchInserterIndexProvider(batchInserter);
        relationshipIndex = indexProvider.relationshipIndex("genRelIdx", stringMap("type", "exact"));
        nodeIndex = indexProvider.nodeIndex("genNodeIdx", stringMap("type", "exact"));

        try {
            //insert nodes
            JSONArray nodeFiles = networkConfiguration.getNodeFiles();
            Map<String, Map<String, String>> nodeTypes = networkConfiguration.getNodeTypes();
            for (int i = 0; i < nodeFiles.length(); i++) {
                JSONObject nodeItem = (JSONObject) nodeFiles.get(i);
                String location = nodeItem.getString("location");
                String nodeType = nodeItem.getString("type");
                log.info("processing node file: " + location);
                if (!nodeTypes.containsKey(nodeType)) {
                    log.warning("no matching node type in config file for type: " + nodeType);
                    continue;
                }
                insertNodes(location, nodeType, nodeTypes.get(nodeType));
            }

            //insert edges
            JSONArray edgeFiles = networkConfiguration.getEdgeFiles();
            Map<String, Map<String, String>> edgeTypes = networkConfiguration.getEdgeTypes();
            for (int i = 0; i < edgeFiles.length(); i++) {
                JSONObject edgeItem = (JSONObject) edgeFiles.get(i);
                String location = edgeItem.getString("location");
                String edgeType = edgeItem.getString("relType");
                log.info("processing edge file: " + location);
                if (!edgeTypes.containsKey(edgeType)) {
                    log.warning("no matching edge type in config file for relType: " + edgeType);
                    continue;
                }
                insertEdges(location, edgeType, edgeTypes.get(edgeType));
            }

            networkConfiguration.outputNetworkMetadata();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            indexProvider.shutdown();
            batchInserter.shutdown();
        }
    }

    private static void insertNodes(String nodeFile, String nodeType, Map<String, String> propTypes) {
        try {
            BufferedReader vertexFile = new BufferedReader(new FileReader(nodeFile));
            String vertexLine = vertexFile.readLine();
            if (vertexLine == null) {
                vertexFile.close();
                return;
            }
            String[] columns = vertexLine.split("\t");
            String props = "";
            for (int v = 1; v < columns.length; v++) {
                props += columns[v] + "\t";
            }
            log.info("NodeType: " + nodeType + " with properties: " + props);

            while ((vertexLine = vertexFile.readLine()) != null) {
                String[] vertexInfo = vertexLine.split("\t");
                Map<String, Object> nProperties = map("name", vertexInfo[0], "nodeType", nodeType);
                Map<String, Object> iProperties = map("name", vertexInfo[0], "nodeType", nodeType);
                for (int i = 1; i < vertexInfo.length; i++) {
                    addProperty(columns[i], propTypes, vertexInfo[i], nProperties, iProperties);
                }
                long node = batchInserter.createNode(nProperties);
                nodeIndex.add(node, iProperties);
                networkConfiguration.incrementNodes();
            }

            nodeIndex.flush();
        } catch (Exception e) {
            log.warning("SKIPPING: encountered issue on " + nodeFile + ":" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void insertEdges(String edgeFile, String edgeType, Map<String, String> propTypes) {
        try {
            BufferedReader relFile = new BufferedReader(new FileReader(edgeFile));
            String relLine = relFile.readLine();
            if (relLine == null) {
                relFile.close();
                return;
            }
            String[] columns = relLine.split("\t");
            if (columns.length < 2) {
                relFile.close();
                log.warning(edgeFile + " must have at least 2 columns for sourcenode, and targetnode. File was not processed.");
                return;
            }

            while ((relLine = relFile.readLine()) != null) {
                String[] relInfo = relLine.split("\t");

                Long sourceNode = nodeIndex.get("name", relInfo[0].trim()).getSingle();
                Long targetNode = nodeIndex.get("name", relInfo[1].trim()).getSingle();

                if (sourceNode == null || targetNode == null) {
                    if (sourceNode == null) {
                        log.warning("node: " + relInfo[0].trim() + " not found for relationship");
                    }
                    if (targetNode == null) {
                        log.warning("node: " + relInfo[1].trim() + " not found for relationship");
                    }
                    continue;
                }
                if (!sourceNode.equals(targetNode)) {
                    Map<String, Object> rProperties = map("relType", edgeType);
                    Map<String, Object> iProperties = map("relType", edgeType);
                    for (int i = 2; i < relInfo.length; i++) {
                        addProperty(columns[i], propTypes, relInfo[i].trim(), rProperties, iProperties);
                    }
                    long rel = batchInserter.createRelationship(sourceNode, targetNode, withName(edgeType), rProperties);
                    relationshipIndex.add(rel, iProperties);
                    networkConfiguration.incrementEdges();
                }
            }

            relationshipIndex.flush();
            relFile.close();

        } catch (Exception e) {
            log.warning("SKIPPING: encountered issue on " + edgeFile + ":" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addProperty(String column, Map<String, String> propTypes, String s,
                                    Map<String, Object> nProperties, Map<String, Object> iProperties) {
        if (propTypes.containsKey(column)) {
            String type = propTypes.get(column);
            if (type.equals("int")) {
                nProperties.put(column, parseInt(s));
                //use ValueContext in order to allow for numeric range queries
                iProperties.put(column, numeric(parseInt(s)));
            } else if (type.equals("double")) {
                nProperties.put(column, parseDouble(s));
                //use ValueContext in order to allow for numeric range queries
                iProperties.put(column, numeric(parseDouble(s)));
            } else {
                nProperties.put(column, s);
                iProperties.put(column, s);
            }

        } else {
            //by default, if a property isn't specified - add it as a string
            nProperties.put(column, s);
            iProperties.put(column, s);
        }
    }
}