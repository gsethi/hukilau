Ext.ns('org.systemsbiology.hukilau.components.queries');

org.systemsbiology.hukilau.components.queries.BasicQuery = Ext.extend(Ext.Panel, {
	title: 'Basic Query',
	layout: 'form',
	header: false,
	autoHeight: true,
	padding: 5,

    graph_uri: undefined,
    data_schema: undefined,
    query_counter: 1,

    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.hukilau.components.queries.BasicQuery.superclass.constructor.apply(this, arguments);
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
                    {label: '0 nodes', value: 1},
					{label: '1 node', value: 2},
					{label: '2 nodes', value: 3},
					{label: '3 nodes', value: 4}
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
                                var queryJson = {};
                                queryJson[node_prop] = query_term;

                                Ext.Ajax.request({
                                    method: "POST",
                                    url: this.graph_uri + "/query",
                                    params: {
                                        query: Ext.util.JSON.encode(queryJson),
                                        level: level,
									    nodeLabel: node_prop,
                                        filter_config: Ext.encode(this.workspace.filter_config)
                                    },
                                    scope: this,
                                    success: function(o) {
                                        var data = Ext.util.JSON.decode(o.responseText);
                                        this.workspace.insertResultTab(data);
                                    },
                                    failure: function() {
                                        this.workspace.insertMessageTab("Query failed");
                                    }
                                });
							}
						}
					}
				}
			]
		});

        org.systemsbiology.hukilau.components.queries.BasicQuery.superclass.initComponent.call(this);
	}
});
