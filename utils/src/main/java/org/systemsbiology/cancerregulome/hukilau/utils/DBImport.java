package org.systemsbiology.cancerregulome.hukilau.utils;


import org.json.*;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.index.BatchInserterIndex;
import org.neo4j.graphdb.index.BatchInserterIndexProvider;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.impl.lucene.LuceneBatchInserterIndexProvider;
import org.neo4j.index.lucene.ValueContext;
import org.neo4j.kernel.impl.batchinsert.BatchInserter;
import org.neo4j.kernel.impl.batchinsert.BatchInserterImpl;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.JsonConfigHandler;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;



/**
 * @author aeakin
 */
public class DBImport {
    private static final Logger log = Logger.getLogger(DBImport.class.getName());
    private static JsonConfig jsonConfig;
    private static String dbPath;
    private static Map<String, Map<String,String>> nodeTypes = new HashMap<String, Map<String,String>>();
    private static Map<String, Map<String,String>> edgeTypes = new HashMap<String, Map<String,String>>();
    private static JSONArray nodeFiles;
    private static JSONArray edgeFiles;


    private static class MyJsonConfigHandler implements JsonConfigHandler {
        public void handle(JSONObject configuration) throws Exception {
            if (configuration.has("db")) {
                JSONObject dbObject = configuration.getJSONObject("db");
                dbPath = dbObject.getString("rootPath");
                JSONArray edgeArray = dbObject.getJSONArray("edgeTypes");
                for (int i = 0; i < edgeArray.length(); i++) {
                    JSONObject edgeTypeObject = (JSONObject)edgeArray.get(i);
                    String typeName = edgeTypeObject.getString("name");
                    Map<String,String> edgeProps = new HashMap<String,String>();
                    if(edgeTypeObject.has("items")){
                        JSONArray propItems = edgeTypeObject.getJSONArray("items");
                        for(int j=0; j< propItems.length(); j++){
                            JSONObject propItem = (JSONObject) propItems.get(j);
                            edgeProps.put(propItem.getString("name"),propItem.getString("type"));
                        }
                    }

                    edgeTypes.put(typeName,edgeProps);
                }
                JSONArray nodeArray = dbObject.getJSONArray("nodeTypes");
                for (int i = 0; i < nodeArray.length(); i++) {
                    JSONObject nodeTypeObject = (JSONObject) nodeArray.get(i);
                    String typeName = nodeTypeObject.getString("name");
                    Map<String,String> nodeProps = new HashMap<String, String>();
                    if(nodeTypeObject.has("items")){
                        JSONArray propItems = nodeTypeObject.getJSONArray("items");
                        for(int j=0; j< propItems.length(); j++){
                            JSONObject propItem = (JSONObject) propItems.get(j);
                            nodeProps.put(propItem.getString("name"),propItem.getString("type"));
                        }
                    }

                    nodeTypes.put(typeName, nodeProps);
                }

            }
            if (configuration.has("nodeFiles")) {
                nodeFiles = configuration.getJSONArray("nodeFiles");
            }
            if (configuration.has("edgeFiles")) {
                edgeFiles = configuration.getJSONArray("edgeFiles");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        //load config containing node and edge type definitions and file locations
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
        jsonConfig = (JsonConfig) ctx.getBean("jsonConfig");
        jsonConfig.visit(new MyJsonConfigHandler());
        log.info("loaded config");


        BatchInserter inserter = new BatchInserterImpl(dbPath);
        BatchInserterIndexProvider indexProvider = new LuceneBatchInserterIndexProvider(inserter);
        BatchInserterIndex genRelIdx = indexProvider.relationshipIndex("genRelIdx",MapUtil.stringMap("type","exact"));
        BatchInserterIndex genNodeIdx = indexProvider.nodeIndex("genNodeIdx", MapUtil.stringMap("type", "exact"));

        try{

            //insert nodes
            for( int i=0; i< nodeFiles.length(); i++){
                JSONObject nodeItem = (JSONObject) nodeFiles.get(i);
                log.info("processing file: " + nodeItem.getString("location"));
                 if(nodeTypes.get(nodeItem.getString("type"))== null){
                    log.warning("no matching node type in config file for type: " + nodeItem.getString("type"));
                    continue;
                }
                insertNodes(nodeItem.getString("location"),nodeItem.getString("type"),inserter,genNodeIdx);
            }

            //insert edges
            for(int i=0; i< edgeFiles.length(); i++){
                JSONObject edgeItem= (JSONObject) edgeFiles.get(i);
                log.info("processing file: " + edgeItem.getString("location"));
                if(edgeTypes.get(edgeItem.getString("relType"))== null){
                    log.warning("no matching edge type in config file for relType: " + edgeItem.getString("relType"));
                    continue;
                }
                insertEdges(edgeItem.getString("location"),edgeItem.getString("relType"),inserter,genNodeIdx,genRelIdx);
            }



        }catch(Exception ex){
            ex.printStackTrace();
        }finally{
            if(indexProvider != null){
                indexProvider.shutdown();
            }
            if(inserter != null){
                inserter.shutdown();
            }
        }

        return;
    }


    private static void insertNodes(String nodeFile, String nodeType, BatchInserter inserter, BatchInserterIndex nodeIdx) {



        try {
            BufferedReader vertexFile = new BufferedReader(new FileReader(nodeFile));
            String vertexLine = vertexFile.readLine();
            if(vertexLine == null){
                vertexFile.close();
                return;
            }
            String[] columns = vertexLine.split("\t");
            String props="";
            for (int v=1; v< columns.length; v++){
               props=props + columns[v] + "\t";
            }

            log.info("NodeType: " + nodeType + " with properties: " + props);

            Map<String,String> propTypes = nodeTypes.get(nodeType);
            while ((vertexLine = vertexFile.readLine()) != null) {
                String[] vertexInfo = vertexLine.split("\t");
                Map<String,Object> nProperties = MapUtil.map("name", vertexInfo[0],"nodeType",nodeType);
                Map<String,Object> iProperties = MapUtil.map("name", vertexInfo[0],"nodeType",nodeType);
                for(int i=1; i< vertexInfo.length; i++){
                    addProperty(columns[i], propTypes, vertexInfo[i], nProperties, iProperties);
                }
                long node = inserter.createNode(nProperties);
                nodeIdx.add(node, iProperties);

            }

            nodeIdx.flush();
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

        return;
    }

    private static void addProperty(String column, Map<String, String> propTypes, String s, Map<String, Object> nProperties, Map<String, Object> iProperties) {
        if(propTypes.containsKey(column)){
            log.info("found key: " + column);
            String type = propTypes.get(column);
            if(type.equals("int")){
                nProperties.put(column, Integer.parseInt(s));
                //use ValueContext in order to allow for numeric range queries
                iProperties.put(column, ValueContext.numeric(Integer.parseInt(s)));
            }
            else if(type.equals("double")){
                nProperties.put(column, Double.parseDouble(s));
                //use ValueContext in order to allow for numeric range queries
                iProperties.put(column, ValueContext.numeric(Double.parseDouble(s)));
            }
            else {
                nProperties.put(column, s);
                iProperties.put(column, s);
            }

        }
        else{
           //by default, if a property isn't specified - add it as a string
           nProperties.put(column, s);
            iProperties.put(column, s);
        }
    }

    private static void insertEdges(String edgeFile, String edgeType, BatchInserter inserter, BatchInserterIndex nodeIdx, BatchInserterIndex relIdx){


            try{
                BufferedReader relFile = new BufferedReader(new FileReader(edgeFile));
                String relLine=relFile.readLine();
                if(relLine == null){
                    relFile.close();
                    return;
                }
                String[] columns=relLine.split("\t");
                if(columns.length < 2){
                    relFile.close();
                    log.warning(edgeFile + " must have at least 2 columns for sourcenode, and targetnode. File was not processed.");
                    return;
                }

                Map<String,String> propTypes = edgeTypes.get(edgeType);
                while ((relLine = relFile.readLine()) != null) {
                    String[] relInfo = relLine.split("\t");

                    Long sourceNode = nodeIdx.get("name", relInfo[0]).getSingle();
                    Long targetNode = nodeIdx.get("name", relInfo[1]).getSingle();

                    if(sourceNode == null || targetNode == null){
                        if(sourceNode == null){
                            log.warning("node: " + relInfo[0] + " not found for relationship");
                        }
                        if(targetNode == null){
                            log.warning("node: " + relInfo[1] + " not found for relationship");
                        }
                        continue;
                    }
                    if (!sourceNode.equals(targetNode)) {
                        Map<String,Object> rProperties = MapUtil.map("relType",edgeType);
                        Map<String,Object> iProperties = MapUtil.map("relType",edgeType);
                        for(int i=2; i< relInfo.length; i++){
                            addProperty(columns[i], propTypes, relInfo[i], rProperties, iProperties);
                        }
                        long rel = inserter.createRelationship(sourceNode, targetNode, DynamicRelationshipType.withName(edgeType),rProperties);
                        relIdx.add(rel,iProperties);
                    }
                }

            relIdx.flush();
            relFile.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }




}