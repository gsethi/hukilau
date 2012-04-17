Ext.onReady(function() {
    new Ext.Viewport({
        layout:'border',
        renderTo:Ext.getBody(),
        items:[
            new org.systemsbiology.addama.js.TopBarToolbar({
                region:'north',
                height:30
            }),
            new org.systemsbiology.hukilau.components.WorkspaceContainer()
        ]
    });
});
