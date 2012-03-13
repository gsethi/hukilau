package org.systemsbiology.cancerregulome.hukilau.utils;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;

import java.util.logging.Logger;

public class FilterUtils {
    private static final Logger log = Logger.getLogger(FilterUtils.class.getName());

    enum FilterType {numericrange}

    public static BooleanQuery buildBooleanQuery(JSONArray filter_list) throws JSONException {
        BooleanQuery bq = new BooleanQuery();

        for (int i = 0; i < filter_list.length(); i++) {
            JSONObject filter = filter_list.getJSONObject(i);

            FilterType type = getFilterType(filter);
            Query query = buildQuery(type, filter);
            if (query != null) {
                bq.add(new BooleanClause(query, BooleanClause.Occur.MUST));
            }
            else {
                log.warning("Unknown filter type: " + filter.toString(2));
            }
        }

        return bq;
    }

    public static FilterType getFilterType(JSONObject filter) throws JSONException {

        String filter_type = filter.getString("filter_type");
        return FilterType.valueOf(filter_type);
    }

    public static Query buildQuery(FilterType type, JSONObject filter) throws JSONException {
        switch (type) {
            case numericrange:
                String property = filter.getString("property");
                Double min = null;
                Double max = null;

                if (filter.has("min")) {
                    min = new Double(filter.getDouble("min"));
                }
                if (filter.has("max")) {
                    max = new Double(filter.getDouble("max"));
                }

                return NumericRangeQuery.newDoubleRange(property, min, max, true, true);
        }

        return null;
    }
}
