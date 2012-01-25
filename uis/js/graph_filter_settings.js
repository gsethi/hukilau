Ext.ns('org.systemsbiology.hukilau.apis.panels')

org.systemsbiology.hukilau.apis.panels.NodeSchemaSettings = {
	id: 'node_dataschema_panel',
	title: 'Node Schema',
	layout: 'form',
	autoHeight: true,
	padding: 5,
	items: [
		{
			xtype: 'combo',
			fieldLabel: 'Node Label',
			editable: false,
			displayField: 'label',
			valueField: 'uri',
			emptyText: 'Coming soon',
			disabled: true,
			forceSelection: true,
			store: new Ext.data.JsonStore({
				autoLoad: false,
				fields: ['name']
			})
		}
	]
};

org.systemsbiology.hukilau.apis.panels.EdgeSchemaSettings = {
	id: 'edge_dataschema_panel',
	title: 'Edge Schema',
	layout: 'form',
	autoHeight: true,
	padding: 5,
	items: [
		{
			xtype: 'combo',
			fieldLabel: 'Edge Label',
			editable: false,
			displayField: 'label',
			valueField: 'uri',
			emptyText: 'Coming soon',
			disabled: true,
			forceSelection: true,
			store: new Ext.data.JsonStore({
				autoLoad: false,
				fields: ['name']
			})
		}
	]
};

org.systemsbiology.hukilau.apis.panels.showDataSchema = function() {

};

org.systemsbiology.hukilau.apis.events.MessageBus.on('graph_metadata_available',
													  org.systemsbiology.hukilau.apis.panels.showDataSchemaPanel,
													  org.systemsbiology.hukilau.apis.panels);
