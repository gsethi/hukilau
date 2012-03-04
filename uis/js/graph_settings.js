Ext.ns('org.systemsbiology.hukilau.components.queries');

org.systemsbiology.hukilau.components.queries.GraphDatabaseSelect = Ext.extend(Object, {
    container_title: undefined,
    query_tab_panel: undefined,
    current_filter_panel: undefined,
    data_schema: undefined,

    constructor: function(config) {
        if (config.query_tab_panel === undefined) {
            console.log("Error: Parent container for query tabs is not defined.")
            return;
        }

        this.query_tab_panel = config.query_tab_panel;
		this.container_title = config.container_title === undefined ? "Graph Database" : config.container_title;
		this.data_schema = {};
		this.graphdb_panel = this.createPanel();
    },

    getPanel: function() {
    	return this.graphdb_panel;
    },

    processDataSchema: function(data_schema) {
    	this.data_schema.node_types = [];

    	Ext.each(data_schema.nodeTypes, function(type, index) {
    		this.data_schema.node_types.push({name: type.name, index: index, fields: type.items.slice()});
    	}, this);

    	this.data_schema.edge_types = [];

    	Ext.each(data_schema.edgeTypes, function(type, index) {
    		this.data_schema.edge_types.push({name: type.name, index: index, fields: type.items.slice()});
    	}, this);
    },

    createPanel: function() {
		this.numberOfNodesLabel = new Ext.form.Label({
			name: 'numberOfNodes',
			fieldLabel: 'Nodes',
			text: '-'			
		});

		this.numberOfEdgesLabel = new Ext.form.Label({
			name: 'numberOfEdges',
			fieldLabel: 'Edges',
			text: '-'
		});

    	this.graphDBCombo = new Ext.form.ComboBox({
			fieldLabel: 'Graph',
			id: 'graph_database_combo',
			mode: 'remote',
			emptyText: 'Select database',
			autoWidth: true,
			editable: false,
			displayField: 'label',
			valueField: 'uri',
			forceSelection: true,
			triggerAction: 'all',
			store: new Ext.data.JsonStore({
				autoLoad: true,
				proxy: new Ext.data.HttpProxy({
					url: '/addama/graphs',
					method: 'GET'
				}),

				root: 'items',
				fields: ['label', 'uri']
            }),
            listeners: {
        		select: {
        			scope: this,
        			fn: function(combo, value) {
        				org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('graph_db_selected', value.data);

						var graph_name = value.data.uri.split('graphs/')[1];

        				var meta_uri = '/addama/graphs/' + graph_name + '/metadata';

        				Ext.Ajax.request({
		                    method: "get",
		                    url: meta_uri,
		                    scope: this,
		                    success: function(o) {
		                    	var json = Ext.util.JSON.decode(o.responseText);
								this.numberOfNodesLabel.setText(json.numberOfNodes);
								this.numberOfEdgesLabel.setText(json.numberOfEdges);

								this.processDataSchema(json);

								var node_query = new org.systemsbiology.hukilau.components.queries.NodeQuery({
									graph_uri: value.data.uri,
									data_schema: this.data_schema
								});

								var filter_query = new org.systemsbiology.hukilau.components.queries.FilterQuery({
									graph_uri: value.data.uri,
									data_schema: this.data_schema
								});

								this.query_tab_panel.removeAll();
								this.query_tab_panel.add(node_query);
								this.query_tab_panel.add(filter_query.getPanel());

								org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('graph_metadata_loaded', json);
		                    }
        				});
        			}
        		}
        	}
    	});
		
    	var graphdb_panel = new Ext.Panel({
		    title: 'Graph Database',
		    autoHeight: true,
		    width: 300,
		    padding: 5,
		    layout: 'form',

		    items: [
				this.graphDBCombo,
				this.numberOfNodesLabel,
				this.numberOfEdgesLabel
		    ]
    	});

    	return graphdb_panel;
    }
});

org.systemsbiology.hukilau.components.queries.NodeQuery = Ext.extend(Ext.Panel, {
	title: 'Node Query',
	layout: 'form',
	header: false,
	autoHeight: true,
	padding: 5,

    graph_uri: undefined,
    data_schema: undefined,

    constructor: function(config) {
        this.graph_uri = config.graph_uri;
        this.data_schema = config.data_schema;
        org.systemsbiology.hukilau.components.queries.NodeQuery.superclass.constructor.apply(this, arguments);
    },

	initComponent: function() {
		var validator_fn = function(value) {
			if (value.length > 0) {
				return true;
			}
			else {
				return false;
			}
		};

		this.typeCombo = new Ext.form.ComboBox({
			fieldLabel: 'Node Type',
			emptyText: 'Select node type...',
			displayField: 'name',
			triggerAction: 'all',
			mode: 'local',
			validator: validator_fn,
			forceSelection: true,
			store: new Ext.data.JsonStore({
				fields: ['name', 'fields', 'index'],
				data: this.data_schema.node_types
			}),
			listeners: {
				select: {
					scope: this,
					fn: function(combo, value) {
						var text_fields = [];
						Ext.each(value.data.fields, function(item) {
							if (item.type == 'string') {
								text_fields.push(item);
							}
						});
						this.propCombo.getStore().loadData(text_fields);
					}
				}
			}			
		});

		this.propCombo = new Ext.form.ComboBox({
			fieldLabel: 'Node Property',
			emptyText: 'Select property...',
			displayField: 'name',
			valueField: 'name',
			triggerAction: 'all',
			mode: 'local',
			validator: validator_fn,
			forceSelection: true,
			width: 150,
			store: new Ext.data.JsonStore({
				fields: ['name', 'type']
			})
		});

		this.operatorCombo = new Ext.form.ComboBox({
			fieldLabel: 'Operator',
			mode: 'local',
			width: 100,
			triggerAction: 'all',
			displayField: 'name',
			valueField: 'value',
			value: 'eq',
			validator: validator_fn,
			store: new Ext.data.JsonStore({
				fields: ['name', 'value'],
				data: [
					{name: 'equals', value: 'eq'}
				]
			})
		});

		this.queryTermField = new Ext.form.TextField({
			fieldLabel: 'Query Term',
			minLength: 1,
			emptyText: 'Enter query term...',
			validator: validator_fn
		});

		this.traversalCombo = new Ext.form.ComboBox({
			fieldLabel: 'Traversal',
			mode: 'local',
			width: 80,
			triggerAction: 'all',
			displayField: 'label',
			valueField: 'value',
			value: '1',
			store: new Ext.data.JsonStore({
				fields: ['label', 'value'],
				data: [
					{label: '1 node', value: 1},
					{label: '2 nodes', value: 2},
					{label: '3 nodes', value: 3},
					{label: '4 nodes', value: 4}
				]
			})
		});

		Ext.apply(this, {
			items: [
				this.typeCombo,
				this.propCombo,
				this.queryTermField,
				this.traversalCombo
			],
			buttons: [
				{
					text: 'Query',
					listeners: {
						click: {
							scope: this,
							fn: function() {
								if (!this.typeCombo.isValid()) {
									Ext.MessageBox.alert('Error', 'Please select a node type.', function() {
										this.typeCombo.focus();
									}, this);

									return;
								}

								if (!this.propCombo.isValid()) {
									Ext.MessageBox.alert('Error', 'Please select a node property.', function() {
										this.propCombo.focus();
									}, this);

									return;
								}

								if (!this.queryTermField.isValid()) {
									Ext.MessageBox.alert('Error', 'Please enter a query term.', function() {
										this.queryTermField.focus();
									}, this);

									return;
								}

								var node_type = this.typeCombo.getValue();
								var node_prop = this.propCombo.getValue();
								var query_term = this.queryTermField.getValue();
								var level = this.traversalCombo.getValue();

								var query_uri = this.graph_uri + '/query?query={' + node_prop + '=\"' + query_term + '\"}' +
												'&level=' + level +
												'&nodeLabel=' + node_prop;
								
								org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('node_query_submitted', query_uri);
							}
						}
					}
				}
			]
		});
		
		org.systemsbiology.hukilau.components.queries.NodeQuery.superclass.initComponent.call(this);
	}
});

org.systemsbiology.hukilau.components.queries.QueryResultInfo = new Ext.Panel({
	title: 'Query Result',
	layout: 'form',
	autoHeight: true,
	padding: 5,
	items: [
		{
			xtype: 'label',
			id: 'num_of_nodes_in_query',
			name: 'numberOfNodesInQuery',
			fieldLabel: 'Nodes',
			text: '-'
		},
		{
			xtype: 'label',
			id: 'num_of_edges_in_query',
			name: 'numberOfEdgesInQuery',
			fieldLabel: 'Edges',
			text: '-'
		}		
	],
	listeners: {
		render: function() {
			var query_result_handler = function(d) {
				Ext.getCmp('num_of_nodes_in_query').setText(d.data.numberOfNodes);
				Ext.getCmp('num_of_edges_in_query').setText(d.data.numberOfEdges);
			}
			
			org.systemsbiology.hukilau.apis.events.MessageBus.on('query_result_available', query_result_handler, this);
		}
		
	}
});

org.systemsbiology.hukilau.components.queries.FilterResultInfo = new Ext.Panel({
	id: 'filter_result_panel',
	title: 'Filter Result',
	layout: 'form',
	autoHeight: true,
	padding: 5,
	items: [
		{
			xtype: 'label',
			id: 'num_of_filtered_nodes',
			name: 'numberOfFilteredNodes',
			fieldLabel: 'Nodes',
			text: '-'
		},
		{
			xtype: 'label',
			id: 'num_of_filtered_edges',
			name: 'numberOfFilteredEdges',
			fieldLabel: 'Edges',
			text: '-'
		}		
	],
	listeners: {
		render: function() {
			var filtered_graph_handler = function(d) {
				Ext.getCmp('num_of_filtered_nodes').setText(d.data.numberOfNodes);
				Ext.getCmp('num_of_filtered_edges').setText(d.data.numberOfEdges);
			}
			
			org.systemsbiology.hukilau.apis.events.MessageBus.on('filtered_graph_available', filtered_graph_handler, this);
		}
		
	}
});
