Ext.ns('org.systemsbiology.hukilau.components.filters');

org.systemsbiology.hukilau.components.filters.FilterSetup = Ext.extend(Object, {
    container_title: undefined,
    stack_panel: undefined,
    graph_uri: undefined,

    constructor: function(config) {
        Ext.apply(this, config, {
            title: "Filter"
        });

        this.stack_panel = this.createStackPanel();
    },

    getPanel: function() {
        return this.stack_panel;
    },

    createStackPanel: function() {
        var stack = new Ext.Panel({
            layout: 'auto',
            title: this.title,
            autoScroll: true,
            height: 500,
            tbar: [ ],
            bbar: [ ]
        });

        stack.getTopToolbar().insertButton(0, {
            text: "Clear",
            scope: this,
            handler: function() {
                stack.removeAll();
                stack.doLayout();
            }
        });

        stack.getBottomToolbar().insertButton(0, {
            text: "Add filter",
            scope: this,
            handler: function() {
                stack.add(new org.systemsbiology.hukilau.components.filters.NumericRangeFilter({
                    data: this.data
                }));
                stack.doLayout();
            }
        });

        stack.getBottomToolbar().insert(1, {xtype: 'tbfill'});

        return stack;
    },

    validateFilters: function() {
        var is_valid = true;
        Ext.each(this.stack_panel.items, function(item, index, allItems) {
            if (!allItems.itemAt(index).isValid()) {
                is_valid = false;
            }
        });

        return is_valid;
    },

    getFilterList: function() {
        var filter_list = [];

        Ext.each(this.stack_panel.items, function(item, index, allItems) {
            var filter = allItems.itemAt(index).getFilter();
            filter_list.push(filter);
        });

        return filter_list;
    },

    resetFilters: function() {
        this.stack_panel.removeAll();
        this.stack_panel.doLayout();
    },

    setDataSchema: function(data_schema) {
        this.data = data_schema;
        this.resetFilters();
    }
});

org.systemsbiology.hukilau.components.filters.NumericRangeFilter = Ext.extend(Ext.Panel, {
	layout: 'form',
	height: 140,
	padding: 5,

	constructor: function() {
        Ext.apply(this, arguments, {});
		org.systemsbiology.hukilau.components.filters.NumericRangeFilter.superclass.constructor.apply(this, arguments);
	},
		
	initComponent: function() {
		var validator_fn = function(value) {
			return value.length > 0;
		};

		this.typeCombo = new Ext.form.ComboBox({
			fieldLabel: 'Type',
			emptyText: 'Select type...',
			displayField: 'name',
			forceSelection: true,
			triggerAction: 'all',
			mode: 'local',
			validator: validator_fn,
			store: new Ext.data.JsonStore({
				fields: ['name', 'fields', 'index'],
                data: this.data
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
                this.typeCombo,
                this.propCombo,
                this.minValueField,
                this.maxValueField,
                {
                    xtype: 'button',
                    text: 'Remove',
                    scope: this,
                    handler: function() {
                        this.destroy();
                    }
                }
            ]
        });

        org.systemsbiology.hukilau.components.filters.NumericRangeFilter.superclass.initComponent.call(this);
	},

	isValid: function() {
		if (!this.propCombo.isValid()) {
			return false;
		}

		return this.minValueField.isValid() || this.maxValueField.isValid();
	},

	getFilter: function() {
		var range = {
			filter_type: 'numericrange',
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
