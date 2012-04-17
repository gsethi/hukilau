Ext.ns('org.systemsbiology.hukilau.components');

org.systemsbiology.hukilau.components.WorkspaceSetup = Ext.extend(Ext.Panel, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.hukilau.components.WorkspaceSetup.superclass.constructor.apply(this, arguments);
    },

    initComponent: function() {
        this.settings_tab_panel = new Ext.TabPanel({
            region: 'center',
            html: '<div class="query_error">Select a graph database to begin.</div>'
        });
        
        this.graph_db_select = new org.systemsbiology.hukilau.components.GraphDatabaseSelect({
            settings_tab_panel: this.settings_tab_panel,
            workspace_container: this.workspace_container
        });

        Ext.apply(this, {
            layout: 'border',
            items: [
                this.graph_db_select,
                this.settings_tab_panel
            ]
        });

        org.systemsbiology.hukilau.components.WorkspaceSetup.superclass.initComponent.call(this);
    }
});

org.systemsbiology.hukilau.components.GraphDatabaseSelect = Ext.extend(Ext.Panel, {
    constructor: function(config) {
        Ext.apply(this, config, {
            graphsUri: "/addama/graphs",
            container_title: "Graph Database",
            data_schema: {}  
        });

        org.systemsbiology.hukilau.components.GraphDatabaseSelect.superclass.constructor.apply(this, arguments);
    },

    processDataSchema: function(data_schema) {
        this.data_schema.node_types = [];

        Ext.each(data_schema.nodeTypes, function (type, index) {
            this.data_schema.node_types.push({name:type.name, index:index, fields:type.items.slice()});
        }, this);

        this.data_schema.edge_types = [];

        Ext.each(data_schema.edgeTypes, function (type, index) {
            this.data_schema.edge_types.push({name:type.name, index:index, fields:type.items.slice()});
        }, this);
    },

    initComponent: function() {
        this.graph_data = undefined;
        this.node_filter_setup = undefined;
        this.edge_filter_setup = undefined;

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

        this.workspaceNameField = new Ext.form.TextField({
            fieldLabel: 'Workspace Name',
            emptyText: 'Enter title'
        });

        this.graphDBCombo = new Ext.form.ComboBox({
			fieldLabel: 'Graph',
			mode: 'remote',
			emptyText: 'Select database',
			autoWidth: true,
			editable: false,
			displayField: 'label',
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
                        this.graph_data = value.data;

                        Ext.Ajax.request({
                            method:"GET",
                            url: value.data.uri + '/metadata',
                            scope:this,
                            success:function (o) {
                                var json

                                json = Ext.util.JSON.decode(o.responseText);
                                this.numberOfNodesLabel.setText(json.numberOfNodes);
                                this.numberOfEdgesLabel.setText(json.numberOfEdges);

                                if (this.workspaceNameField.getValue().length == 0) {
                                    this.workspaceNameField.setValue(value.data.label);
                                }

                                this.processDataSchema(json);

                                if (this.node_filter_setup === undefined) {
                                    this.node_filter_setup = new org.systemsbiology.hukilau.components.filters.FilterSetup({
                                        workspace_container: this.workspace_container,
                                        title: 'Node Filters',
                                        data: this.data_schema.node_types
                                    });

                                    this.settings_tab_panel.add(this.node_filter_setup.getPanel());
                                    this.settings_tab_panel.setActiveTab(this.node_filter_setup.getPanel());
                                }
                                else {
                                    this.node_filter_setup.setDataSchema(this.data_schema.node_types);
                                    this.settings_tab_panel.setActiveTab(this.node_filter_setup.getPanel());
                                }

                                if (this.edge_filter_setup === undefined) {
                                    this.edge_filter_setup = new org.systemsbiology.hukilau.components.filters.FilterSetup({
                                        workspace_container: this.workspace_container,
                                        title: 'Edge Filters',
                                        data: this.data_schema.edge_types
                                    });

                                    this.settings_tab_panel.add(this.edge_filter_setup.getPanel());
                                }
                                else {
                                    this.edge_filter_setup.setDataSchema(this.data_schema.edge_types);
                                }
                            }
                        });
                    }
                }
            }
        });

        Ext.apply(this, {
            title:'Graph Database',
            autoHeight:true,
            width: 300,
            padding: 5,
            region: 'west',
            layout: 'form',

            items: [
                this.graphDBCombo,
                this.numberOfNodesLabel,
                this.numberOfEdgesLabel,
                this.workspaceNameField
            ],
            bbar: {
                buttons: [
                    {
                        text: 'Create workspace',
                        scope: this,
                        handler: function() {
                            var workspace_name,
                                node_filters,
                                edge_filters;

                            workspace_name = this.workspaceNameField.getValue();

                            if (this.node_filter_setup.validateFilters() == false) {
                                Ext.MessageBox.alert('Error', 'Invalid values in node filter fields.');
                                this.settings_tab_panel.setActiveTab(this.node_filter_setup.getPanel());
                                return;
                            }

                            if (this.edge_filter_setup.validateFilters() == false) {
                                Ext.MessageBox.alert('Error', 'Invalid values in edge filter fields.');
                                this.settings_tab_panel.add(this.edge_filter_setup.getPanel());
                                return;
                            }

                            node_filters = this.node_filter_setup.getFilterList();
                            edge_filters = this.edge_filter_setup.getFilterList();

                            this.workspace_container.createWorkspace({
                                title: workspace_name,
                                data: this.graph_data,
                                data_schema: this.data_schema,
                                filter_config: {
                                    nodes: node_filters,
                                    edges: edge_filters
                                }
                            });
                        }
                    }
                ]
            }
        });

        org.systemsbiology.hukilau.components.GraphDatabaseSelect.superclass.initComponent.call(this);
    }
});
