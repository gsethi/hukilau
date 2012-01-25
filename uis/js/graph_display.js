Ext.ns('org.systemsbiology.hukilau.components');

var GraphDisplay = Ext.extend(Object, {
    vis: undefined,
    container_id: undefined,
    data_schema: undefined,

    constructor: function(p_container_id) {
        this.container_id = p_container_id;
    },

    init: function(nodes, edges) {        
        var graph_display = this;

        if (this.data_schema === undefined) {
            console.log("CytoscapeWeb init failed: dataSchema not set");
            return;
        }

        this.vis = new org.cytoscapeweb.Visualization(this.container_id, {
            swfPath: "https://informatics-apps.systemsbiology.net/cytoscapeweb_v1.0/swf/CytoscapeWeb",
            flashInstallerPath: "https://informatics-apps.systemsbiology.net/cytoscapeweb_v1.0/swf/playerProductInstall"
        });

        this.vis.ready(function() {
            var that = this;

            this.addContextMenuItem("Select first neighbors", "nodes", function(evt) {
                var root_node = evt.target;

                var first_neighbors = that.firstNeighbors([root_node]);
                var neighbor_nodes = first_neighbors.neighbors;
                that.select([root_node]).select(neighbor_nodes);
            });

            // Add new elements
            var elements = graph_display.getGraphElements();
            this.addElements(elements);
            
            // Redo layout
            this.layout( {name: 'ForceDirected', options: {}} );
        });
        
        this.vis.draw({
            network: {
                dataSchema: this.data_schema,
                nodes: nodes,
                edges: edges
            },
            nodeTooltipsEnabled: true,
            edgeTooltipsEnabled: true
        });             
    },

    displayError: function() {
        Ext.get(this.container_id).dom.innerHTML = '<p class="graph_error">Please add nodes and edges using the Node and Edge grid tabs.</p>';
    },

    setDataSchema: function(schema) {
        var that = this;
        that.data_schema = schema;

        if (this.vis === undefined) {
            this.init([], []);
        }
    },

    filterEdgeElement: function(element) {
        var el = {};

        var n = this.data_schema.edges.length;
        for (var i = 0; i < n; i++) {
            var field = this.data_schema.edges[i];
            if (element.hasOwnProperty(field.name)) {
                el[field.name] = element[field.name];
            }
        }

        return el;
    },

    getGraphElements: function() {
        var graph = org.systemsbiology.hukilau.components.GraphManager.getGraph();
        var elements = [];

        for (var node_uri in graph.nodes) {
            if (graph.nodes.hasOwnProperty(node_uri)) {
                var node_data = graph.nodes[node_uri];

                elements.push( {
                    group: "nodes",
                    data: node_data
                });
            }
        }

        for (var edge_uri in graph.edges) {
            if (graph.edges.hasOwnProperty(edge_uri)) {
                var edge_data = this.filterEdgeElement(graph.edges[edge_uri]);

                elements.push( {
                    group: "edges",
                    data: edge_data
                });
            }
        }

        return elements;
    },

    addNodes: function(node_uris) {
        var that = this;

        if (that.vis === undefined) {
            console.log("CytoscapeWeb not ready: no nodes added");
        }
        else {
            var n = node_uris.length;
            for (var i = 0; i < n; i++) {
                var uri = node_uris[i];
                var node = org.systemsbiology.hukilau.components.GraphManager.getNode(uri);
                console.log(node);
            }
        }
    }    
});

org.systemsbiology.hukilau.components.GraphDisplay = new GraphDisplay('c_vis');

org.systemsbiology.hukilau.apis.events.MessageBus.on('graph_dataschema_available',
    org.systemsbiology.hukilau.components.GraphDisplay.setDataSchema,
    org.systemsbiology.hukilau.components.GraphDisplay);
