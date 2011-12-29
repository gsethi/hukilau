Ext.ns('org.systemsbiology.hukilau.apis.events');

org.systemsbiology.hukilau.apis.events.MessageBus = new Ext.util.Observable();
org.systemsbiology.hukilau.apis.events.MessageBus.addEvents('graph_db_selected');
org.systemsbiology.hukilau.apis.events.MessageBus.addEvents('graph_metadata_loaded');
org.systemsbiology.hukilau.apis.events.MessageBus.addEvents('node_query_submitted');
