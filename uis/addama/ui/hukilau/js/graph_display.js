Ext.ns('org.systemsbiology.hukilau.components');

var GraphDisplay = Ext.extend(Object, {
    vis: undefined,
    container_id: undefined,

    constructor: function(p_container_id) {
        this.container_id = p_container_id;
    },

    doQuery: function(uri) {

        var that = this;
        Ext.Ajax.request({
            method: "get",
            url: uri,
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);

                that.redraw(json);
            },
            failure: function(o) {
                console.log('Query failed:');
                console.log(o.responseText);
            }
        });
    },

    redraw: function(json) {
        if (this.vis === undefined) {
            this.vis = new org.cytoscapeweb.Visualization(this.container_id, {
                swfPath: "https://informatics-apps.systemsbiology.net/cytoscapeweb_v0.8/swf/CytoscapeWeb",
                flashInstallerPath: "https://informatics-apps.systemsbiology.net/cytoscapeweb_v0.8/swf/playerProductInstall"
            });            
        }
        
        this.vis.draw({
            network: json,
            nodeTooltipsEnabled: true,
            edgeTooltipsEnabled: true
        });        
    }
});

org.systemsbiology.hukilau.components.GraphDisplay = new GraphDisplay('c_vis');

org.systemsbiology.hukilau.apis.events.MessageBus.on('node_query_submitted',
    org.systemsbiology.hukilau.components.GraphDisplay.doQuery,
    org.systemsbiology.hukilau.components.GraphDisplay);
