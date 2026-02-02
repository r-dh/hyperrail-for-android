/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.util;

import android.content.Context;

import org.joda.time.DateTime;

import java.util.List;

import be.hyperrail.android.logging.HyperRailLog;
import be.hyperrail.android.persistence.PersistentQueryProvider;
import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSource;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;
import be.hyperrail.opentransportdata.common.requests.RoutePlanningRequest;
import be.hyperrail.opentransportdata.common.requests.VehicleRequest;

/**
 * Warms up API caches by pre-fetching recent and favorite queries in the background.
 *
 * This solves the cold cache problem where first requests take 3-12 seconds:
 * - First request to API endpoint: 3-12s (cold cache, hits backend DB)
 * - Subsequent requests: 90-140ms (warm cache, served from cache)
 *
 * By warming caches on app start, user interactions feel instant because:
 * 1. Client-side cache is populated (Volley's DiskBasedCache)
 * 2. Server-side cache is warmed for everyone
 * 3. By the time user taps, data is already cached
 *
 * Strategy:
 * - Fetch top 3 recent stations (liveboards)
 * - Fetch top 2 recent routes (connections)
 * - Fetch top 2 recent trains (vehicle details)
 * - All requests happen silently in background
 * - Failures are silently ignored (non-critical)
 */
public class ApiCacheWarmer {

    private static final HyperRailLog log = HyperRailLog.getLogger(ApiCacheWarmer.class);

    // Limit warming requests to avoid excessive data usage
    private static final int MAX_STATIONS_TO_WARM = 3;
    private static final int MAX_ROUTES_TO_WARM = 2;
    private static final int MAX_TRAINS_TO_WARM = 2;

    /**
     * Warm up API caches by pre-fetching recent queries in background.
     * Should be called once on app startup.
     *
     * @param context Application context
     */
    public static void warmCaches(Context context) {
        log.info("Starting API cache warming...");

        PersistentQueryProvider queryProvider = PersistentQueryProvider.getInstance(context);
        TransportDataSource api = OpenTransportApi.getDataProviderInstance();
        DateTime now = DateTime.now();

        // Warm station liveboards (most common query)
        warmStationCaches(queryProvider, api, now);

        // Warm route connections
        warmRouteCaches(queryProvider, api, now);

        // Warm train vehicle details
        warmTrainCaches(queryProvider, api, now);

        log.info("API cache warming initiated");
    }

    private static void warmStationCaches(PersistentQueryProvider queryProvider,
                                          TransportDataSource api,
                                          DateTime now) {
        List<be.hyperrail.android.persistence.Suggestion<LiveboardRequest>> recentStations =
                queryProvider.getAllStations();
        int count = 0;

        for (be.hyperrail.android.persistence.Suggestion<LiveboardRequest> suggestion : recentStations) {
            if (count >= MAX_STATIONS_TO_WARM) break;

            try {
                LiveboardRequest station = suggestion.getData();

                // Create warming request for current time
                LiveboardRequest warmingRequest = new LiveboardRequest(
                    station.getStation(),
                    LiveboardType.DEPARTURES,
                    QueryTimeDefinition.EQUAL_OR_LATER,
                    now
                );

                // Silent callbacks - we don't care about results
                warmingRequest.setCallback(
                    (data, tag) -> log.debug("Warmed cache for station: " + station.getStation().getLocalizedName()),
                    (error, tag) -> log.debug("Cache warming failed for station (non-critical): " + error.getMessage()),
                    0
                );

                api.getLiveboard(warmingRequest);
                count++;
            } catch (Exception e) {
                log.warning("Error warming station cache: " + e.getMessage());
            }
        }

        log.info("Warming " + count + " station liveboard caches");
    }

    private static void warmRouteCaches(PersistentQueryProvider queryProvider,
                                        TransportDataSource api,
                                        DateTime now) {
        List<be.hyperrail.android.persistence.Suggestion<RoutePlanningRequest>> recentRoutes =
                queryProvider.getAllRoutes();
        int count = 0;

        for (be.hyperrail.android.persistence.Suggestion<RoutePlanningRequest> suggestion : recentRoutes) {
            if (count >= MAX_ROUTES_TO_WARM) break;

            try {
                RoutePlanningRequest route = suggestion.getData();

                // Create warming request for current time
                RoutePlanningRequest warmingRequest = new RoutePlanningRequest(
                    route.getOrigin(),
                    route.getDestination(),
                    QueryTimeDefinition.EQUAL_OR_LATER,
                    now
                );

                // Silent callbacks
                warmingRequest.setCallback(
                    (data, tag) -> log.debug("Warmed cache for route: " +
                        route.getOrigin().getLocalizedName() + " -> " +
                        route.getDestination().getLocalizedName()),
                    (error, tag) -> log.debug("Cache warming failed for route (non-critical): " + error.getMessage()),
                    0
                );

                api.getRoutePlanning(warmingRequest);
                count++;
            } catch (Exception e) {
                log.warning("Error warming route cache: " + e.getMessage());
            }
        }

        log.info("Warming " + count + " route connection caches");
    }

    private static void warmTrainCaches(PersistentQueryProvider queryProvider,
                                        TransportDataSource api,
                                        DateTime now) {
        List<be.hyperrail.android.persistence.Suggestion<VehicleRequest>> recentTrains =
                queryProvider.getAllTrains();
        int count = 0;

        for (be.hyperrail.android.persistence.Suggestion<VehicleRequest> suggestion : recentTrains) {
            if (count >= MAX_TRAINS_TO_WARM) break;

            try {
                VehicleRequest train = suggestion.getData();

                // Create warming request for current date
                VehicleRequest warmingRequest = new VehicleRequest(
                    train.getVehicleId(),
                    now
                );

                // Silent callbacks
                warmingRequest.setCallback(
                    (data, tag) -> log.debug("Warmed cache for train: " + train.getVehicleId()),
                    (error, tag) -> log.debug("Cache warming failed for train (non-critical): " + error.getMessage()),
                    0
                );

                api.getVehicleJourney(warmingRequest);
                count++;
            } catch (Exception e) {
                log.warning("Error warming train cache: " + e.getMessage());
            }
        }

        log.info("Warming " + count + " train vehicle caches");
    }
}
