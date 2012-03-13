Ext.onReady(function() {
    var workspaceContainer, graphQueryTabs, graphDBSelect, graphControlPanel, addamaToolbar;

    workspaceContainer = new org.systemsbiology.hukilau.components.WorkspaceContainer({
        region:'center'
    });

    graphQueryTabs = new Ext.TabPanel();

    graphDBSelect = new org.systemsbiology.hukilau.components.queries.GraphDatabaseSelect({
        query_tab_panel: graphQueryTabs,
        workspace_container: workspaceContainer
    });

    graphControlPanel = {
        xtype:'panel',
        region:'west',
        width:300,
        layout:'auto',
        layoutConfig:{
            align:'top'
        },
        items:[
            graphDBSelect.getPanel(),
            graphQueryTabs
        ]
    };

    addamaToolbar = new org.systemsbiology.addama.js.TopBarToolbar({
        region:'north',
        height:30
    });

    new Ext.Viewport({
        layout:'border',
        renderTo:Ext.getBody(),
        items:[
            addamaToolbar,
            graphControlPanel,
            workspaceContainer
        ]
    });
});
