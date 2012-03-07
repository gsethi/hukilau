Ext.ns('org.systemsbiology.hukilau.components');

org.systemsbiology.hukilau.components.QueryResultDisplay = Ext.extend(Object,{
	node_prop_store: undefined,
	node_grid: undefined,
	edge_prop_store: undefined,
	edge_grid: undefined,
	container: undefined,
	data_schema: undefined,
	container_title: undefined,

	constructor: function(config) {
        Ext.apply(this, config, {
            container_title: "Query Result"
        });

        if (this.json.data.numberOfEdges > 0 || this.json.data.numberOfNodes > 0) {
            this.create_grids(this.json);
        }
        else {
            this.create_msg_panel("Query result is empty");
        }
	},

	create_msg_panel: function(message) {
        this.container = new Ext.Panel({
			title: this.container_title,
			html: '<div class="query_error">' + message + '</div>',
			closable: true
        });
	},

	create_grids: function(data) {
		this.node_prop_store = new Ext.data.JsonStore({
			data: {
				rows: data.data.nodes,
				metaData: {
					fields: data.dataSchema.nodes,
					root: 'rows'
				}
			}
		});

		this.hide_fields(data.dataSchema.nodes, ['name', 'label', 'id', 'uri'], this.find_field_indices(data.dataSchema.nodes));
		var node_columns = this.create_node_columns(data.dataSchema.nodes);
        this.node_grid = new Ext.grid.GridPanel({
		    title: 'Nodes',
		    region: 'center',
		    autoScroll: true,
		    autoWidth: true,
		    loadMask: true,
		    disabled: false,
		    ds: this.node_prop_store,
		    cm: new Ext.grid.ColumnModel(node_columns),
		    tbar: [
		    	{
		    		text: "Add Nodes",
                    scope: this,
		    		handler: function() {
                        var node_rows = this.node_grid.getSelectionModel().getSelections();
		    			var nodes = {};

		    			Ext.each(node_rows, function(row) {
	    					nodes[row.data.id] = true;
		    			});

                        var filter_fn = function (record) {
                            var source = record.data.source;
		    				var target = record.data.target;

		    				return nodes.hasOwnProperty(source) && nodes.hasOwnProperty(target);
		    			};

		    			var edge_rows = this.edge_grid.getStore().queryBy(filter_fn);

		    			org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('add_elements_to_graph', {
		    				graph_uri: Ext.getCmp('graph_database_combo').getValue(),
		    				node_rows: node_rows,
		    				edge_rows: edge_rows.items
		    			});	
		    		}
		    	}
		    ]
		});

		this.add_node_label_fields(data.dataSchema.edges);
		this.add_node_labels(data.data.edges, data.data.nodes);
		this.edge_prop_store = new Ext.data.JsonStore({
			data: {
				rows: data.data.edges,
				metaData: {
					fields: data.dataSchema.edges,
					root: 'rows'
				}
			}
		});

		this.hide_fields(data.dataSchema.edges, ['name', 'label', 'id', 'uri', 'source', 'target'], this.find_field_indices(data.dataSchema.edges));
		var edge_columns = this.create_edge_columns(data.dataSchema.edges);
		this.edge_grid = new Ext.grid.GridPanel({
		    title: 'Edges',
		    region: 'south',
		    split: true,
		    height: 300,
            autoScroll: true,
		    autoWidth: true,
		    loadMask: true,
		    disabled: false,
		    ds: this.edge_prop_store,
		    cm: new Ext.grid.ColumnModel(edge_columns),
		    tbar: [
		    	{
		    		text: "Add Edges",
                    scope: this,
		    		handler: function() {
		    			var nodes = {};

		    			var edge_rows = this.edge_grid.getSelectionModel().getSelections();
		    			Ext.each(edge_rows, function(row) {
		    				nodes[row.data.source] = true;
		    				nodes[row.data.target] = true;
		    			});

		    			// Build a filter function for finding the source and target node rows for each edge
                        var filter_fn = function (record) {
                            return nodes.hasOwnProperty(record.data.id);
		    			};

		    			var node_rows = this.node_grid.getStore().queryBy(filter_fn);

		    			org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('add_elements_to_graph', {
		    				graph_uri: Ext.getCmp('graph_database_combo').getValue(),
		    				node_rows: node_rows.items,
		    				edge_rows: edge_rows
		    			});
		    		}
		    	}
		    ]
		});

		this.container = new Ext.Panel({
			title: this.container_title,
			layout: 'accordion',
			closable: true,
            layoutConfig: {
                hideCollapseTool: true,
                titleCollapse: true
            },
			items: [
				this.node_grid,
				this.edge_grid
			]
		});
    },

    getPanel: function() {
        return this.container;
    },

    build_column_model: function(meta_data) {
		var columns = [];

        for (var i = 0; i < meta_data.length; i++ ) {
            var field = meta_data[i];

            var col = {
            	xtype: 'gridcolumn',
                header: field.name,
                type: field.type,
                dataIndex: field.name
            };

            if (field.header) {
            	col.header = field.header;
            }
            else {
            	col.header = field.name;
            }

            if (field.hidden) {
            	col.hidden = true;
            	col.sortable = false;
            }
            else {
            	col.sortable = true;
            }

            columns.push(col);
        }
		
		return columns;
	},

	map_field: function(p_array, key) {
		var map = {};
		var n = p_array.length;

		for (var i = 0; i < n; i++) {
			map[p_array[i][key]] = p_array[i];
		}

		return map;
	},

	find_field_indices: function(p_array) {
		var field_map = {};
		var n = p_array.length;

		for (var i = 0; i < n; i++) {
			field_map[p_array[i].name] = i;
		}

		return field_map;
	},

	hide_fields: function(meta, fields, name_to_index_map) {
		var n = fields.length;
		for (var i = 0; i < n; i++) {
			if (name_to_index_map.hasOwnProperty(fields[i])) {
				var index = name_to_index_map[fields[i]];
				meta[index].hidden = true;
			}
		}
	},

	create_node_columns: function(schema) {
		return this.build_column_model(schema);
	},

	create_edge_columns: function(schema) {
		var columns = this.build_column_model(schema);
		        
		columns.push({
			xtype: 'gridcolumn',
			name: "source_label",
			header: "source",
			dataIndex: "source_label"
		});

		columns.push({
			xtype: 'gridcolumn',
			name: "target_label",
			header: "target",
			dataIndex: "target_label"
		});

		return columns;
	},

	add_node_label_fields: function(edge_schema) {
		edge_schema.push({
			name: "source_label",
			header: "source",
			dataIndex: "source_label"
		});

		edge_schema.push({
			name: "target_label",
			header: "target",
			dataIndex: "target_label"
		});			
	},

	add_node_labels: function(edges, nodes) {
		var nodes_by_uri = this.map_field(nodes, 'uri');
		var n = edges.length;
		for (var i = 0; i < n; i++) {
			var edge = edges[i];
			edge.source_label = nodes_by_uri[edge.source].label;
			edge.target_label = nodes_by_uri[edge.target].label;
		}
	}
});
