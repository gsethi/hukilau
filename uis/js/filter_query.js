Ext.ns('org.systemsbiology.hukilau.components.queries');

org.systemsbiology.hukilau.components.queries.FilterQuery = Ext.extend(Object, {
    container_title: undefined,
    stack_panel: undefined,
    data_schema: undefined,
    graph_uri: undefined,
    query_counter: 1,

    constructor: function(config) {
        Ext.apply(this, config, {
            container_title: "Filter"
        });

        this.stack_panel = this.createStackPanel();
    },

    getPanel: function() {
    	return this.stack_panel;
    },

    createStackPanel: function() {
    	var stack = new Ext.Panel({
    		layout: 'auto',
    		title: this.container_title,
    		autoScroll: true,
    		height: 500,
    		items: [
    			new org.systemsbiology.hukilau.components.queries.NumericRangeFilter({
                    data_schema: this.data_schema
                })
			],
			tbar: [ ],
			bbar: [ ]
    	});

    	stack.getTopToolbar().insertButton(0, {
    		text: "Clear",
    		scope: this,
			handler: function() {
				stack.removeAll();
				stack.add(new org.systemsbiology.hukilau.components.queries.NumericRangeFilter({
                    data_schema: this.data_schema
                }));
				stack.doLayout();
			}
    	});

    	stack.getBottomToolbar().insertButton(0, {
     		text: "Add filter",
    		scope: this,
    		handler: function() {
    			stack.add(new org.systemsbiology.hukilau.components.queries.NumericRangeFilter({
                    data_schema: this.data_schema
                }));
    			stack.doLayout();
    		}
    	});

    	stack.getBottomToolbar().insert(1, {xtype: 'tbfill'});

    	var validateFilters = function() {
    		var is_valid = true;
    		Ext.each(stack.items, function(item, index, allItems) {
    			if (!allItems.itemAt(index).isValid()) {
    				is_valid = false;
    			}
    		});

    		return is_valid;
    	};

    	var executeQuery = function() {
    		if (validateFilters() == false) {
				Ext.MessageBox.alert('Error', 'Invalid values in filter fields.');
				return;
    		}

	    	var node_filters = [];

	    	Ext.each(stack.items, function(item, index, allItems) {
	    		filter = allItems.itemAt(index).getFilter();
	    		if (filter.object == "nodes") {
	    			node_filters.push(filter);
	    		}
	    	});

			var query_uri = this.graph_uri + '/filter';

	    	org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('fasd_query_submitted', {
	    		uri: query_uri,
	    		filters: {
	    			nodes: node_filters,
	    			edges: []
	    		}
	    	});

            new org.systemsbiology.hukilau.components.QueryResultDisplay({
                parent_container: this.data_tab_panel,
                container_title: 'Filter Query ' + this.query_counter,
                request: {
                    method: 'post',
                    uri: query_uri,
                    params: {
                        filter_config: Ext.encode({
                            nodes: node_filters,
                            edges: []
                        })
                    }
                }
            });

            this.query_counter++;
    	};

    	stack.getBottomToolbar().insertButton(2, {
     		text: "Query",
     		scope: this,
    		handler: executeQuery
    	});

    	return stack;
    }
});

org.systemsbiology.hukilau.components.queries.NumericRangeFilter = Ext.extend(Ext.Panel, {
	layout: 'form',
	height: 140,
	padding: 5,

	constructor: function() {
        Ext.apply(this, arguments);
		org.systemsbiology.hukilau.components.queries.NumericRangeFilter.superclass.constructor.apply(this, arguments);
	},
		
	initComponent: function() {
		var validator_fn = function(value) {
			return value.length > 0;
		};

		this.objectCombo = new Ext.form.ComboBox({
			fieldLabel: 'Node / Edge',
			emptyText: 'Select entity...',
			displayField: 'name',
			forceSelection: true,
			triggerAction: 'all',
			mode: 'local',
			validator: validator_fn,
			store: new Ext.data.JsonStore({
				fields: ['name'],
				data: [
					{name: 'nodes'}
				]
			}),
			listeners: {
				select: {
					scope: this,
					fn: function(combo, value) {
						if (value.data.name == 'nodes') {
							this.typeCombo.getStore().loadData(this.data_schema.node_types);
						}
						else if (value.data.name == 'edges') {
							this.typeCombo.getStore().loadData(this.data_schema.edge_types);
						}
					}
				}
			}	
		});

		this.typeCombo = new Ext.form.ComboBox({
			fieldLabel: 'Type',
			emptyText: 'Select type...',
			displayField: 'name',
			forceSelection: true,
			triggerAction: 'all',
			mode: 'local',
			validator: validator_fn,
			store: new Ext.data.JsonStore({
				fields: ['name', 'fields', 'index']
			}),
			listeners: {
				select: {
					scope: this,
					fn: function(combo, value) {
						var numeric_fields = [];
						Ext.each(value.data.fields, function(item) {
							if (item.type == 'double') {
								numeric_fields.push(item);
							}
						});
						this.propCombo.getStore().loadData(numeric_fields);
					}
				}
			}
		});
		
		this.propCombo = new Ext.form.ComboBox({
			fieldLabel: 'Property',
			emptyText: 'Select property...',
			displayField: 'name',
			forceSelection: true,
			triggerAction: 'all',
			mode: 'local',
			validator: validator_fn,
			store: new Ext.data.JsonStore({
				fields: ['name', 'type']
			})
		});

		this.minValueField = new Ext.form.NumberField({
			fieldLabel: 'Min',
			allowBlank: false,
			minLength: 1,
			emptyText: 'enter value'
		});

		this.maxValueField = new Ext.form.NumberField({
			fieldLabel: 'Max',
			allowBlank: false,
			minLength: 1,
			emptyText: 'enter value'
		});

        Ext.apply(this, { 
            items: [
            	this.objectCombo,
        		this.typeCombo,
        		this.propCombo,
        		this.minValueField,
        		this.maxValueField
        	] 
        });

        org.systemsbiology.hukilau.components.queries.NumericRangeFilter.superclass.initComponent.call(this);
	},

	isValid: function() {
		if (!this.objectCombo.isValid()) {
			return false;
		}

		if (!this.typeCombo.isValid()) {
			return false;
		}

		if (!this.propCombo.isValid()) {
			return false;
		}

		return this.minValueField.isValid() || this.maxValueField.isValid();
	},

	getFilter: function() {
		var range = {
			filter_type: 'numericrange',
			object: this.objectCombo.getValue(),
			object_type: this.typeCombo.getValue(),
			property: this.propCombo.getValue()
		};

		if (this.minValueField.isValid()) {
			range.min = this.minValueField.getValue();
		}

		if (this.maxValueField.isValid()) {
			range.max = this.maxValueField.getValue();
		}

		return range;
	}
});
