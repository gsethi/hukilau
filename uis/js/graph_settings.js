Ext.ns('org.systemsbiology.hukilau.components.queries');

org.systemsbiology.hukilau.components.queries.GraphDatabaseSelect = Ext.extend(Object, {
    graphsUri: "/addama/graphs",
    container_title: "Graph Database",
    query_tab_panel: undefined,
    data_schema: {},

    constructor: function(config) {
        Ext.apply(this, config);

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
				proxy: new Ext.data.HttpProxy({ url: this.graphsUri, method: "GET" }),
				root: 'items',
				fields: ['label', 'uri', 'id']
            }),
            listeners: {
        		select: {
        			scope: this,
        			fn: function(combo, value) {
                        Ext.Ajax.request({
                            method:"GET",
                            url: value.data.uri + '/metadata',
                            scope:this,
                            success:function (o) {
                                var json,
                                    node_query,
                                    filter_query;

                                json = Ext.util.JSON.decode(o.responseText);
                                this.numberOfNodesLabel.setText(json.numberOfNodes);
                                this.numberOfEdgesLabel.setText(json.numberOfEdges);

                                this.processDataSchema(json);

                                node_query = new org.systemsbiology.hukilau.components.queries.NodeQuery({
                                    workspace_container: this.workspace_container,
                                    graph_uri: value.data.uri,
                                    graph_id: value.data.id,
                                    data_schema: this.data_schema
                                });

                                filter_query = new org.systemsbiology.hukilau.components.queries.FilterQuery({
                                    workspace_container: this.workspace_container,
                                    graph_uri: value.data.uri,
                                    graph_id: value.data.id,
                                    data_schema: this.data_schema
                                });

                                this.query_tab_panel.removeAll();
                                this.query_tab_panel.add(node_query);
                                this.query_tab_panel.add(filter_query.getPanel());

                                this.workspace_container.createWorkspace(value.data);
                            }
                        });
                    }
        		}
        	}
    	});

        return new Ext.Panel({
            title:'Graph Database',
            autoHeight:true,
            width:300,
            padding:5,
            layout:'form',

            items:[
                this.graphDBCombo,
                this.numberOfNodesLabel,
                this.numberOfEdgesLabel
            ]
        });
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
    query_counter: 1,

    constructor: function(config) {
        org.systemsbiology.hukilau.components.queries.NodeQuery.superclass.constructor.apply(this, arguments);
    },

	initComponent: function() {
		var validator_fn = function(value) {
            return value.length > 0;
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

								var node_prop = this.propCombo.getValue();
								var query_term = this.queryTermField.getValue();
								var level = this.traversalCombo.getValue();
                                var selectedWorkspace = this.workspace_container.getWorkspace(this.graph_id);
                                var queryJson = {};
                                queryJson[node_prop] = query_term;

                                Ext.Ajax.request({
                                    method: "GET",
                                    url: this.graph_uri + "/query",
                                    params: {
                                        query: Ext.util.JSON.encode(queryJson),
                                        level: level,
									    nodeLabel: node_prop
                                    },
                                    scope: this,
                                    success: function(o) {
                                        var data = Ext.util.JSON.decode(o.responseText);
                                        selectedWorkspace.insertResultTab(data);
                                    },
                                    failure: function() {
                                        selectedWorkspace.insertMessageTab("Query failed");
                                    }
                                });
							}
						}
					}
				}
			]
		});
		
		org.systemsbiology.hukilau.components.queries.NodeQuery.superclass.initComponent.call(this);
	}
});
