package be.hyperrail.android;

import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.be.IrailDataProvider;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.contracts.TransportDataSource;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;
import be.hyperrail.opentransportdata.common.requests.RoutePlanningRequest;
import be.hyperrail.opentransportdata.common.requests.VehicleRequest;

/**
 * Instrumented test to measure actual API performance on device.
 *
 * Run with: ./gradlew connectedAndroidTest
 * or from Android Studio: Right-click -> Run 'ApiPerformanceInstrumentedTest'
 *
 * Check results in logcat with tag "ApiPerformanceTest"
 */
@RunWith(AndroidJUnit4.class)
public class ApiPerformanceInstrumentedTest {

    private static final String TAG = "ApiPerformanceTest";
    private static final int RUNS_PER_TEST = 5;
    private static final int TIMEOUT_SECONDS = 30;

    private Context appContext;
    private TransportDataSource api;

    // Station test data
    private StopLocation brusselsCentral;
    private StopLocation oudenaarde;

    @Before
    public void setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Initialize API
        OpenTransportApi.init(appContext, new IrailDataProvider(), null);
        api = OpenTransportApi.getDataProviderInstance();

        // Create test stations
        brusselsCentral = createStation("BE.NMBS.008812005", "Brussels-Central");
        oudenaarde = createStation("BE.NMBS.008892007", "Oudenaarde");

        Log.i(TAG, "========================================");
        Log.i(TAG, "API Performance Test - Starting");
        Log.i(TAG, "========================================");
    }

    @Test
    public void testLiveboardPerformance() throws InterruptedException {
        Log.i(TAG, "\n========================================");
        Log.i(TAG, "TEST 1: Liveboard Performance");
        Log.i(TAG, "========================================");

        List<Long> times = new ArrayList<>();

        for (int i = 1; i <= RUNS_PER_TEST; i++) {
            CountDownLatch latch = new CountDownLatch(1);
            final long[] elapsed = {0};

            Log.i(TAG, "Run " + i + "...");
            long startTime = System.currentTimeMillis();

            LiveboardRequest request = new LiveboardRequest(
                brusselsCentral,
                QueryTimeDefinition.EQUAL_OR_LATER,
                LiveboardType.DEPARTURES,
                DateTime.now()
            );

            request.setCallback((data, tag) -> {
                elapsed[0] = System.currentTimeMillis() - startTime;
                Log.i(TAG, "  Success: " + elapsed[0] + "ms (" + data.getStops().length + " stops)");
                times.add(elapsed[0]);
                latch.countDown();
            }, (error, tag) -> {
                Log.e(TAG, "  Failed: " + error.getMessage());
                latch.countDown();
            }, 0);

            api.getLiveboard(request);

            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Thread.sleep(500); // Delay between requests
        }

        printStatistics("Liveboard", times);
    }

    @Test
    public void testConnectionsPerformance() throws InterruptedException {
        Log.i(TAG, "\n========================================");
        Log.i(TAG, "TEST 2: Connections Performance");
        Log.i(TAG, "========================================");

        List<Long> times = new ArrayList<>();

        for (int i = 1; i <= RUNS_PER_TEST; i++) {
            CountDownLatch latch = new CountDownLatch(1);
            final long[] elapsed = {0};

            Log.i(TAG, "Run " + i + "...");
            long startTime = System.currentTimeMillis();

            RoutePlanningRequest request = new RoutePlanningRequest(
                brusselsCentral,
                oudenaarde,
                QueryTimeDefinition.EQUAL_OR_LATER,
                DateTime.now()
            );

            request.setCallback((data, tag) -> {
                elapsed[0] = System.currentTimeMillis() - startTime;
                Log.i(TAG, "  Success: " + elapsed[0] + "ms (" + data.getRoutes().length + " routes)");
                times.add(elapsed[0]);
                latch.countDown();
            }, (error, tag) -> {
                Log.e(TAG, "  Failed: " + error.getMessage());
                latch.countDown();
            }, 0);

            api.getRoutePlanning(request);

            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Thread.sleep(500);
        }

        printStatistics("Connections", times);
    }

    @Test
    public void testVehiclePerformance() throws InterruptedException {
        Log.i(TAG, "\n========================================");
        Log.i(TAG, "TEST 3: Vehicle Performance");
        Log.i(TAG, "========================================");

        List<Long> times = new ArrayList<>();

        for (int i = 1; i <= RUNS_PER_TEST; i++) {
            CountDownLatch latch = new CountDownLatch(1);
            final long[] elapsed = {0};

            Log.i(TAG, "Run " + i + "...");
            long startTime = System.currentTimeMillis();

            VehicleRequest request = new VehicleRequest("BE.NMBS.IC1832", DateTime.now());

            request.setCallback((data, tag) -> {
                elapsed[0] = System.currentTimeMillis() - startTime;
                Log.i(TAG, "  Success: " + elapsed[0] + "ms (" + data.getStops().length + " stops)");
                times.add(elapsed[0]);
                latch.countDown();
            }, (error, tag) -> {
                Log.e(TAG, "  Failed: " + error.getMessage());
                latch.countDown();
            }, 0);

            api.getVehicleJourney(request);

            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Thread.sleep(500);
        }

        printStatistics("Vehicle", times);
    }

    @Test
    public void testSequentialWorkflow() throws InterruptedException {
        Log.i(TAG, "\n========================================");
        Log.i(TAG, "TEST 4: Sequential Workflow (App Simulation)");
        Log.i(TAG, "========================================");
        Log.i(TAG, "Simulating: Liveboard -> Connections");

        for (int scenario = 1; scenario <= 3; scenario++) {
            Log.i(TAG, "\nScenario " + scenario + ":");

            CountDownLatch latch1 = new CountDownLatch(1);
            CountDownLatch latch2 = new CountDownLatch(1);

            long workflowStart = System.currentTimeMillis();

            // Step 1: Load liveboard
            Log.i(TAG, "  1. Loading liveboard...");
            long step1Start = System.currentTimeMillis();

            LiveboardRequest liveboardRequest = new LiveboardRequest(
                brusselsCentral,
                LiveboardType.DEPARTURES,
                QueryTimeDefinition.EQUAL_OR_LATER,
                DateTime.now()
            );

            liveboardRequest.setCallback((data, tag) -> {
                long step1Time = System.currentTimeMillis() - step1Start;
                Log.i(TAG, "     Liveboard loaded: " + step1Time + "ms");
                latch1.countDown();
            }, (error, tag) -> {
                Log.e(TAG, "     Liveboard failed: " + error.getMessage());
                latch1.countDown();
            }, 0);

            api.getLiveboard(liveboardRequest);
            latch1.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Small delay (simulating user interaction)
            Thread.sleep(100);

            // Step 2: Load connections
            Log.i(TAG, "  2. Loading connections...");
            long step2Start = System.currentTimeMillis();

            RoutePlanningRequest routeRequest = new RoutePlanningRequest(
                brusselsCentral,
                oudenaarde,
                QueryTimeDefinition.EQUAL_OR_LATER,
                DateTime.now()
            );

            routeRequest.setCallback((data, tag) -> {
                long step2Time = System.currentTimeMillis() - step2Start;
                Log.i(TAG, "     Connections loaded: " + step2Time + "ms");
                latch2.countDown();
            }, (error, tag) -> {
                Log.e(TAG, "     Connections failed: " + error.getMessage());
                latch2.countDown();
            }, 0);

            api.getRoutePlanning(routeRequest);
            latch2.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            long totalTime = System.currentTimeMillis() - workflowStart;
            Log.i(TAG, "  Total workflow time: " + totalTime + "ms (" + (totalTime / 1000.0) + "s)");

            Thread.sleep(1000);
        }
    }

    private void printStatistics(String testName, List<Long> times) {
        if (times.isEmpty()) {
            Log.w(TAG, "\nNo successful requests for " + testName);
            return;
        }

        long sum = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        for (long time : times) {
            sum += time;
            if (time < min) min = time;
            if (time > max) max = time;
        }

        long avg = sum / times.size();

        Log.i(TAG, "\n" + testName + " Statistics:");
        Log.i(TAG, "  Average: " + avg + "ms");
        Log.i(TAG, "  Min:     " + min + "ms");
        Log.i(TAG, "  Max:     " + max + "ms");
        Log.i(TAG, "  Success: " + times.size() + "/" + RUNS_PER_TEST);
        Log.i(TAG, "");

        // Analysis
        if (avg < 1000) {
            Log.i(TAG, "  ✓ Performance is good (< 1s)");
        } else if (avg < 3000) {
            Log.i(TAG, "  ⚠ Performance is acceptable (1-3s)");
        } else if (avg < 5000) {
            Log.i(TAG, "  ⚠ Performance is slow (3-5s)");
        } else {
            Log.i(TAG, "  ✗ Performance is very slow (> 5s) - API bottleneck");
        }
    }

    private StopLocation createStation(String id, String name) {
        return new StopLocation() {
            @Override
            public String getHafasId() {
                return id;
            }

            @Override
            public String getSemanticId() {
                return id;
            }

            @Override
            public String getLocalizedName() {
                return name;
            }

            @Override
            public double getLatitude() {
                return 0;
            }

            @Override
            public double getLongitude() {
                return 0;
            }
        };
    }
}
