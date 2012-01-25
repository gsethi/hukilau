Ext.ns('org.systemsbiology.hukilau.components');

var GraphManager = Ext.extend(Object, {
    graph: {
        nodes: {},
        edges: {}
    },

    addNodes: function(node_uris) {
        var qh = org.systemsbiology.hukilau.components.QueryHandler;
        var added = [];

        var n = node_uris.length;
        for (var i = 0; i < n; i++) {
            var uri = node_uris[i];
            if ( !this.graph.nodes.hasOwnProperty(uri) ) {
                this.graph.nodes[uri] = true;
                added.push(uri);
            }
        }

        // return {
        //     nodes: added,
        //     edges: []
        // };
    },

    // addNodesHandler: function(node_rows) {
    //     var added = this.addNodes(node_rows);
    //     if (added.length > 0) {
    //         org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('graph_topology_update', added);
    //     }
    // },

    addEdges: function(edge_uris) {
        var current_data = org.systemsbiology.hukilau.components.QueryHandler.getCurrentData();
        var added = [];

        var n = edge_uris.length;
        for (var i = 0; i < n; i++) {
            var uri = edge_uris[i];

            if ( !this.graph.edges.hasOwnProperty(uri) ) {
                var edge = current_data.edges[uri];
                this.addNodes([edge.source, edge.target]);
                this.graph.edges[uri] = true;
                added.push(uri);
            }
        }

        // if (added.length > 0) {
        //     org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('graph_edges_update', added);
        // }
        
    //     return {
    //         nodes: [],
    //         edges: added
    //     };
    },
    
    // addEdges: function(edge_rows) {
    //     var added = [];

    //     var n = edge_rows.length;
    //     for (var i = 0; i < n; i++) {
    //         var edge = edge_rows[i].data;

    //         if ( !this.graph.edges.hasOwnProperty(edge.uri) ) {
    //             this.graph.edges[edge.uri] = edge;
    //             added.push(edge.uri);
    //         }
    //     }

    //     if (added.length > 0) {
    //         org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('graph_edges_update', added);
    //     }
    // },

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
