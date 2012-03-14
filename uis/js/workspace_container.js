Ext.ns('org.systemsbiology.hukilau.components');

org.systemsbiology.hukilau.components.WorkspaceContainer = Ext.extend(Ext.TabPanel,{
    graphStylesUri: "/addama/stores/graphStyles",
    workspaces: {},
    graph_styles: {},

    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.hukilau.components.WorkspaceContainer.superclass.constructor.apply(this, arguments);

        this.loadStyleObjects();
    },

    initComponent: function() {
        Ext.apply(this, {
            tabPosition: 'bottom',
            activeItem: 0,
            items: [
                {
                    title: "Main",
                    html: '<div class="query_error">Select a graph database to begin.</div>'
                }
            ]
        });

        org.systemsbiology.hukilau.components.WorkspaceContainer.superclass.initComponent.call(this);
    },

    createWorkspace: function(data) {
        if (!this.workspaces.hasOwnProperty(data.id)) {
            var workspace = new org.systemsbiology.hukilau.components.Workspace({
                title: data.label,
                cytoscape_root_id: 'c_vis',
                graph_id: data.id,
                default_visual_style: this.getGraphStyle(data.id)
            });

            workspace.on("close", this.handleWorkspaceClosed, this);

            this.workspaces[data.id] = workspace;

            this.add(workspace);
            this.setActiveTab(workspace);

            return workspace;
        }
    },

    getWorkspace: function(graph_id) {
        if (this.workspaces.hasOwnProperty(graph_id)) {
            return this.workspaces[graph_id];
        }
    },

    handleWorkspaceClosed: function(panel) {
        Ext.destroyMembers(this.workspaces, panel.graph_id);
    },

    loadStyleObjects: function() {
        Ext.Ajax.request({
            method: "GET",
            url: this.graphStylesUri,
            scope: this,
            success: function(o) {
                var data = Ext.util.JSON.decode(o.responseText);
                Ext.each(data.items, function(item) {
                    this.graph_styles[item.id] = item.style;
                }, this);
            }
        });
    },

    getGraphStyle: function(graph_id) {
        if (this.graph_styles.hasOwnProperty(graph_id)) {
            return this.graph_styles[graph_id];
        }
        else {
            return this.graph_styles["default"];
        }
    }
});
