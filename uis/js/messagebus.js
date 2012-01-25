Ext.ns('org.systemsbiology.hukilau.apis.events');

org.systemsbiology.hukilau.apis.events.MessageBus = new Ext.util.Observable();

// Graph database selection
org.systemsbiology.hukilau.apis.events.MessageBus.addEvents('graph_db_selected');
org.systemsbiology.hukilau.apis.events.MessageBus.addEvents('graph_metadata_loaded');

// Queries
org.systemsbiology.hukilau.apis.events.MessageBus.addEvents('node_query_submitted');
org.systemsbiology.hukilau.apis.events.MessageBus.addEvents('query_result_available');
org.systemsbiology.hukilau.apis.events.MessageBus.addEvents('graph_dataschema_available');

// Graph manipulation
org.systemsbiology.hukilau.apis.events.MessageBus.addEvents('add_edges_to_graph');
org.systemsbiology.hukilau.apis.events.MessageBus.addEvents('graph_dataschema_update');
org.systemsbiology.hukilau.apis.events.MessageBus.addEvents('graph_topology_update');
