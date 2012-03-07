Ext.ns('org.systemsbiology.hukilau.apis.events');

org.systemsbiology.hukilau.apis.events.MessageBus = new Ext.util.Observable();

// Graph database selection
org.systemsbiology.hukilau.apis.events.MessageBus.addEvents('graph_db_selected');

// Graph manipulation
org.systemsbiology.hukilau.apis.events.MessageBus.addEvents('add_elements_to_graph');
