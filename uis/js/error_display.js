Ext.ns('org.systemsbiology.hukilau.components.queries');

org.systemsbiology.hukilau.components.queries.ErrorDisplay = Ext.extend(Ext.Panel, {
    constructor: function() {
        Ext.apply(this, arguments, {
            closable: true
        });

        org.systemsbiology.hukilau.components.queries.ErrorDisplay.superclass.constructor.apply(this, arguments);
    },

    initComponent: function() {
        Ext.apply(this, {
            html: '<div class="query_error">' + this.message + '</div>'
        });

        org.systemsbiology.hukilau.components.queries.ErrorDisplay.superclass.initComponent.call(this);
    }
});
