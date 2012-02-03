Ext.ns('org.systemsbiology.hukilau.components');

var GraphManager = Ext.extend(Object, {
    graph: {
        nodes: {},
        edges: {}
    },

    addNodes: function(node_rows) {
        var n = node_rows.length;
        for (var i = 0; i < n; i++) {
            var data = node_rows[i].data;
            if ( !this.graph.nodes.hasOwnProperty(data.uri) ) {
                this.graph.nodes[data.uri] = data;
            }
        }
    },
    
    addEdges: function(edge_rows) {
        var n = edge_rows.length;
        for (var i = 0; i < n; i++) {
            var edge = edge_rows[i].data;

            if ( !this.graph.edges.hasOwnProperty(edge.uri) ) {
                this.graph.edges[edge.uri] = edge;
            }
        }
    },

    removeNode: function(nodes) {
        
    },

    getNode: function(uri) {
        return this.graph.nodes[uri];
    },

    getGraph: function() {
        return this.graph;
    }
});

org.systemsbiology.hukilau.components.GraphManager = new GraphManager();

org.systemsbiology.hukilau.apis.events.MessageBus.on('add_edges_to_graph',
    org.systemsbiology.hukilau.components.GraphManager.addEdges,
    org.systemsbiology.hukilau.components.GraphManager);
