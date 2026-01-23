/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.util.health;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import be.hyperrail.android.BuildConfig;
import be.hyperrail.android.logging.HyperRailLog;


/**
 * Created in be.hyperrail.android.util on 13/03/2018.
 */

public class HealthStateChecker {

    private final HealthStateCheckerListener connectionReceiverListener;
    private final RequestQueue requestQueue;
    private static final String USER_AGENT = "HyperRail for Android - " + BuildConfig.VERSION_NAME;

    public HealthStateChecker(Context context, HealthStateCheckerListener listener) {
        connectionReceiverListener = listener;
        this.requestQueue = Volley.newRequestQueue(context);
        checkHealth();
    }

    private void checkHealth() {
        Response.Listener<JSONObject> successListener = response -> {
            connectionReceiverListener.onSystemHealthChanged(new HealthState(response));
        };

        Response.ErrorListener errorListener = error -> {
            HyperRailLog.getLogger(HealthStateChecker.class)
                    .warning("Failed to fetch system health status", error);
            // Silently fail - health check is non-critical
            // The app will continue to work without system health info
        };

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, "https://hyperrail.be/status.json", null, successListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-agent", USER_AGENT);
                return headers;
            }
        };

        // Set timeout and retry policy consistent with main API
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,  // 10 second timeout
                2,      // 2 retries (3 total attempts)
                2.0f    // Exponential backoff multiplier
        ));

        requestQueue.add(jsObjRequest);
    }


}

