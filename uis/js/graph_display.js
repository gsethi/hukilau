Ext.ns('org.systemsbiology.hukilau.components');

org.systemsbiology.hukilau.components.GraphDisplay = Ext.extend(Ext.util.Observable, {
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
            width: 1024,
            height: 1024,
            layout_name: "grid"
        });

        this.cytoscape_options.style = this.default_visual_style;
        this.initCytoscape();

        this.graph_panel = new Ext.Panel({
            title: this.container_title,
            contentEl: this.cytoscape_content_el,
            listeners: {
                activate: {
                    scope: this,
                    fn: function() {
                        if (this.cy !== undefined && this.update_layout_flag) {
                            this.cy.layout({
                                name: this.layout_name
                            });

                            this.update_layout_flag = Boolean(false);
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

        this.addEvents({
            nodeClicked: true
        });
    },

    getPanel: function() {
        return this.graph_panel;
    },

    getVisContainer: function() {
        // Insert a div element for Cytoscape SVG rendering
        var el = Ext.DomHelper.append(this.cytoscape_root_id, {
            tag: 'div'
        });

        // Generate a unique id for the Cytoscape div and apply that id to the element
        this.cytoscape_content_el = Ext.id(el);

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

    destroy: function(){
        // Remove the Cytoscape div
        var el = Ext.get(this.cytoscape_content_el);
        Ext.DomHelper.overwrite(el, "");
    },

    addElements: function(params) {
        var that = this;
        var elements = [];
        var new_nodes = [];

        Ext.each(params.node_rows, function(row, index) {
            var node_data = params.data_fn(row);

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

            if (params.coordinate_fn !== undefined) {
                el.position = params.coordinate_fn(num_nodes, index, 100);
            }

            elements.push(el);
        }, this);

        Ext.each(params.edge_rows, function(row) {
            var edge_data = params.data_fn(row);

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

        if (elements.length > 0) {
            this.update_layout_flag = Boolean(true);
        }

        if (this.cy.nodes().size() == 0) {
            this.cy.load(elements);
        }
        else {
            this.cy.add(elements);
        }

        this.cy.nodes().click(function() {
            var node = this;
            that.nodeClickHandler(node);
        });
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

        Ext.Ajax.request({
            method: "GET",
            url: node.data().uri,
            params: {
                level: 2,
                filter_config: Ext.encode(this.workspace.filter_config)
            },
            scope: this,
            success: function(d) {
                var json = Ext.util.JSON.decode(d.responseText);

                this.addElements({
                    node_rows: json.data.nodes,
                    edge_rows: json.data.edges,
                    data_fn: function(x) {return x;},
                    coordinate_fn: this.createCircularCoordFn(node.position())
                });
            }
        });
    },

    createCircularCoordFn: function(base) {
        return function(num, index, dist) {
            var rad = index * (360 / num) * Math.PI / 180;

            return {
                x: base.x + dist * (Math.cos(rad) - Math.sin(rad)),
                y: base.y + dist * (Math.sin(rad) + Math.cos(rad))
            };
        };
    },

    nodeClickHandler: function(node) {
        this.fireEvent("nodeClicked", {
            node: node
        });
    }
});
