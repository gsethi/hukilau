Ext.ns('org.systemsbiology.hukilau.components');

org.systemsbiology.hukilau.components.GraphDisplay = Ext.extend(Object, {
    cy: undefined,
    cytoscape_content_el: undefined,
    container_title: undefined,
    graph_panel: undefined,
    width: undefined,
    height: undefined,
    layout_name: undefined,
    style_objects: {},

    cytoscape_options: {
        renderer: {
            name: "svg"
        },
        layout: {
            name: "grid"
        },
        style: {
            selectors: {
                "edge": {
                    targetArrowShape: "triangle",
                    width: 1
                },

                "node": {
                    labelText: {
                        defaultValue: ""
                    },
                    shape: "ellipse",
                    height: 10,
                    width: 10
                },

                "node:selected": {
                    fillColor: "#333"
                }
            }
        },
        elements: {
            nodes: [],
            edges: []
        }
    },

    constructor: function(config) {
        Ext.apply(this, config, {
            container_title: "Graph",
            width: 700,
            height: 700,
            layout_name: "grid"
        });

        this.graph_panel = new Ext.Panel({
            title: this.container_title,
            contentEl: this.cytoscape_content_el,
            listeners: {
                activate: {
                    scope: this,
                    fn: function() {
                        if (this.cy !== undefined) {
                            this.cy.layout({
                                name: this.layout_name
                            });
                        }
                    }
                }
            },
            tbar: [
                {
                    text: "Layout",
                    menu: new Ext.menu.Menu({
                        items: [
                            {
                                text: "Grid",
                                scope: this,
                                handler: function() {
                                    this.setLayout("grid");
                                }
                            },
                            {
                                text: "Random",
                                scope: this,
                                handler: function() {
                                    this.setLayout("random");
                                }
                            },
                            {
                                text: "Arbor",
                                scope: this,
                                handler: function() {
                                    this.setLayout("arbor");
                                }
                            },
                            {
                                text: "Springy",
                                scope: this,
                                handler: function() {
                                    this.setLayout("springy");
                                }
                            }
                        ]
                    })
                },
                {
                    text: "Expand neighbours",
                    scope: this,
                    handler: this.expandNeighbours
                }
            ]
        });
        
        this.loadStyleObjects();

        org.systemsbiology.hukilau.apis.events.MessageBus.on('graph_db_selected', this.handleGraphDBChange, this);

        org.systemsbiology.hukilau.apis.events.MessageBus.on('add_elements_to_graph', function(d) {
            this.addElements(d, function(x) { return x.data; });
        }, this);
    },

    getPanel: function() {
        return this.graph_panel;
    },

    loadStyleObjects: function() {
        var styles = {};

        Ext.Ajax.request({
            method: 'get',
            url: '/addama/stores/graphStyles/',
            scope: this,
            success: function(o) {
                var data = Ext.util.JSON.decode(o.responseText);
                Ext.each(data.items, function(item) {
                    styles[item.id] = item.style;
                });
            }
        });

        this.style_objects = styles;
    },

    handleGraphDBChange: function(params) {
        var graph_id;

        graph_id = params.uri.split('graphs/')[1];

        if (this.style_objects.hasOwnProperty(graph_id)) {
            this.cy.style(this.style_objects[graph_id])
        }
        else {
            this.cy.style(this.style_objects["default"]);
        }
    },

    getVisContainer: function() {
        var selector = "#" + this.cytoscape_content_el;
        jQuery(selector).width(this.width).height(this.height);
        return jQuery(selector);
    },

    initCytoscape: function() {
        var that = this;

        this.cytoscape_options.ready = function(cy) {
            that.cy = cy;
        };

        this.getVisContainer().cytoscapeweb(this.cytoscape_options);
    },

    addElements: function(params, data_fn, coordinate_fn) {
        var elements = [];
        var new_nodes = [];

        Ext.each(params.node_rows, function(row, index) {
            var node_data = data_fn(row);

            if (this.cy.nodes("[id='" + node_data.id + "']").size() == 0) {
                new_nodes.push(node_data);
            }
        }, this);

        var num_nodes = new_nodes.length;
        Ext.each(new_nodes, function(node, index) {
            var el = {
                group: "nodes",
                data: node
            };

            if (coordinate_fn !== undefined) {
                el.position = coordinate_fn(num_nodes, index, 100);
            }

            elements.push(el);
        }, this);

        Ext.each(params.edge_rows, function(row) {
            var edge_data = data_fn(row);

            if (this.cy.edges("[id='" + edge_data.id + "']").size() == 0) {
                var edge_element = {};
                Ext.apply(edge_element, edge_data);
                Ext.destroyMembers(edge_element, 'source_label', 'target_label');
                elements.push({
                    group: "edges",
                    data: edge_element
                });
            }
        }, this);

        if (this.cy.nodes().size() == 0) {
            this.cy.load(elements);
        }
        else {
            this.cy.add(elements);
        }
    },

    setLayout: function(name) {
        this.layout_name = name;
        this.cy.layout({
            name: this.layout_name
        });
    },

    expandNeighbours: function(nodes) {
        var selected = this.cy.elements("node:selected");
        var n = selected.length;
        if (n == 0) {
            return;
        }

        var node = selected[0];
        var node_id_split = node.data().id.split('/');
        var node_id = node_id_split[node_id_split.length - 1];

        var graph_uri = Ext.getCmp('graph_database_combo').getValue();
        var query_uri = graph_uri + '/neighbours?query=' + node_id;

        Ext.Ajax.request({
            method: 'get',
            url: query_uri,
            scope: this,
            success: function(d) {
                var json = Ext.util.JSON.decode(d.responseText);

                this.addElements({
                    node_rows: json.data.nodes,
                    edge_rows: json.data.edges
                }, function(x) {return x;}, this.createCircularCoordFn(node.position()));
            }
        })
        
    },

    createCircularCoordFn: function(base) {
        return function(num, index, dist) {
            var rad = index * (360 / num) * Math.PI / 180;

            return {
                x: base.x + dist * (Math.cos(rad) - Math.sin(rad)),
                y: base.y + dist * (Math.sin(rad) + Math.cos(rad))
            }
        }
    }
});
