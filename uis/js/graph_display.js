Ext.ns('org.systemsbiology.hukilau.components');

org.systemsbiology.hukilau.components.GraphDisplay = Ext.extend(Object, {
    cy: undefined,
    cytoscape_content_el: undefined,
    container_title: undefined,
    graph_panel: undefined,
    width: undefined,
    height: undefined,
    layout_name: undefined,

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
                    width: 3
                },

                "edge:selected": {
                    lineColor: "#666",
                    targetArrowColor: "#666",
                    sourceArrowColor: "#666"
                },

                "node": {
                    labelText: {
                        defaultValue: "",
                        passthroughMapper: "gene_symbol"
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
        var that = this;

        this.cytoscape_content_el = config.cytoscape_content_el;
        this.container_title = config.container_title === undefined ? "Graph" : config.container_title;

        this.width = config.width === undefined ? 700 : config.width;
        this.height = config.height === undefined ? 700 : config.height;

        this.layout_name = config.layout === undefined ? "grid" : config.layout;

        this.graph_panel = new Ext.Panel({
            title: this.container_title,
            contentEl: this.cytoscape_content_el,
            listeners: {
                activate: function() {
                    if (that.cy !== undefined) {
                        that.cy.layout({
                            name: that.layout_name
                        });
                    }
                }
            },
            tbar: [
                {
                    text: "Grid",
                    handler: function() {
                        that.layout_name = "grid";
                        that.cy.layout({
                            name: that.layout_name
                        });
                    }
                },
                {
                    text: "Random",
                    handler: function() {
                        that.layout_name = "random";
                        that.cy.layout({
                            name: that.layout_name
                        });
                    }
                },
                {
                    text: "Arbor",
                    handler: function() {
                        that.layout_name = "arbor";
                        that.cy.layout({
                            name: that.layout_name
                        });
                    }
                },
                {
                    text: "Springy",
                    handler: function() {
                        that.layout_name = "springy";
                        that.cy.layout({
                            name: that.layout_name
                        });
                    }
                }
            ]
        });

        org.systemsbiology.hukilau.apis.events.MessageBus.on('add_elements_to_graph', this.addElements, this);
    },

    getPanel: function() {
        return this.graph_panel;
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

    addElements: function(params) {
        var elements = [];

        Ext.each(params.node_rows, function(row) {
            if (this.cy.nodes("[id='" + row.data.id + "']").size() == 0) {
                elements.push({
                    group: "nodes",
                    data: row.data
                });
            }
        }, this);

        Ext.each(params.edge_rows, function(row) {
            if (this.cy.edges("[id='" + row.data.id + "']").size() == 0) {
                var edge_data = {};
                Ext.apply(edge_data, row.data);
                Ext.destroyMembers(edge_data, 'source_label', 'target_label');
                elements.push({
                    group: "edges",
                    data: edge_data
                });
            }
        }, this);

        if (this.cy.nodes().size() == 0) {
            this.cy.load(elements);
        }
        else {
            this.cy.add(elements);
        }
    }
});
