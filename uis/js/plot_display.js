Ext.ns('org.systemsbiology.hukilau.components');

org.systemsbiology.hukilau.components.PlotDisplay = Ext.extend(Ext.Panel,{
    constructor: function(config) {
        Ext.apply(this, config, { });

        org.systemsbiology.hukilau.components.PlotDisplay.superclass.constructor.apply(this, arguments);
    },

    initComponent: function() {
        this.plot_content_el = this.createVisContainer();
        this.scatterplot_obj = undefined;

        Ext.apply(this, {
            title: 'Data Plot',
            contentEl: this.plot_content_el
        });

        org.systemsbiology.hukilau.components.PlotDisplay.superclass.initComponent.call(this);
    },

    createVisContainer: function() {
        // Insert a div element for SVG rendering
        var el = Ext.DomHelper.append(this.plot_root_id, {
            tag: 'div'
        });

        // Generate a unique id for the visualization div and apply that id to the element
        return Ext.id(el);
    },

    getVisContainer: function() {
        return document.getElementById(this.plot_content_el);
    },

    updateData: function(event_obj) {
        var probe_id = event_obj.node.data().probe_id;

        var query_json = {
            tq: 'SELECT probe_id, time, value WHERE probe_id=\"' + probe_id + '\" ORDER BY time ',
            tqx: 'out:json_array'
        };

        var data_table = 'expr_' + this.workspace.data.id;
        var data_query = '/addama/datasources/datasets/' + data_table + '/query?' + Ext.urlEncode(query_json);

        Ext.Ajax.request({
            url: data_query,
            scope: this,
            success: function(response) {
                var data = Ext.decode(response.responseText);
                this.showScatterPlot(data);
            }
        });
    },

    showScatterPlot: function(data_array) {
        if (this.scatterplot_obj === undefined) {
            this.scatterplot_obj = new vq.ScatterPlot();

            var plot_data = {
                DATATYPE : "vq.models.ScatterPlotData",
                CONTENTS : {
                    PLOT : {
                        container: this.getVisContainer(),
                        width : 640,
                        height: 640,
                        vertical_padding : 80,
                        horizontal_padding: 100,
                        x_label_displacement: 50,
                        y_label_displacement: -70,
                        x_tick_displacement: 20,
                        y_tick_displacement: -10,
                        enable_transitions: false
                    },
                    axis_font :"14px helvetica",
                    tick_font :"14px helvetica",
                    stroke_width: 2,
                    radius: 4,
                    data_array: data_array,
                    regression: 'none',
                    xcolumnid: 'time',
                    xcolumnlabel: 'time',
                    ycolumnid: 'value',
                    ycolumnlabel: 'expression',
                    valuecolumnid: 'probe_id'
                }
            };

            this.scatterplot_obj.draw(plot_data);
        }
        else {
            this.scatterplot_obj.resetData(data_array);
        }
    }
});
