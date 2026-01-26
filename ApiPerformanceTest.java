import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Standalone Java test to measure iRail API performance
 *
 * Compile: javac ApiPerformanceTest.java
 * Run: java ApiPerformanceTest
 */
public class ApiPerformanceTest {

    private static final String USER_AGENT = "HyperRail-Performance-Test";
    private static final int RUNS_PER_ENDPOINT = 5;
    private static final int TIMEOUT_MS = 30000;

    // Station IDs
    private static final String BRUSSELS_CENTRAL = "BE.NMBS.008812005";
    private static final String OUDENAARDE = "BE.NMBS.008892007";
    private static final String GHENT = "BE.NMBS.008892007";

    public static void main(String[] args) {
        System.out.println("============================================");
        System.out.println("iRail API Performance Test (Java)");
        System.out.println("============================================");
        System.out.println();

        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyy", Locale.US);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.US);
        Date now = new Date();
        String date = dateFormat.format(now);
        String time = timeFormat.format(now);

        System.out.println("Test configuration:");
        System.out.println("  - Number of runs per endpoint: " + RUNS_PER_ENDPOINT);
        System.out.println("  - Timeout: " + TIMEOUT_MS + "ms");
        System.out.println("  - Date: " + date);
        System.out.println("  - Time: " + time);
        System.out.println();

        // Test 1: Liveboard
        System.out.println("============================================");
        System.out.println("TEST 1: Liveboard API (Station Departures)");
        System.out.println("============================================");
        testEndpoint("Liveboard - Brussels-Central",
                "https://api.irail.be/liveboard/?format=json&id=" + BRUSSELS_CENTRAL +
                "&date=" + date + "&time=" + time + "&arrdep=dep");

        // Test 2: Connections
        System.out.println("============================================");
        System.out.println("TEST 2: Connections API (Route Planning)");
        System.out.println("============================================");
        testEndpoint("Connections - Brussels to Oudenaarde",
                "https://api.irail.be/connections/?format=json&from=" + BRUSSELS_CENTRAL +
                "&to=" + OUDENAARDE + "&date=" + date + "&time=" + time + "&timeSel=depart");

        // Test 3: Vehicle
        System.out.println("============================================");
        System.out.println("TEST 3: Vehicle API (Train Journey)");
        System.out.println("============================================");
        testEndpoint("Vehicle - IC 1832",
                "https://api.irail.be/vehicle/?format=json&id=BE.NMBS.IC1832&date=" + date);

        // Test 4: Disturbances
        System.out.println("============================================");
        System.out.println("TEST 4: Disturbances API");
        System.out.println("============================================");
        testEndpoint("Disturbances",
                "https://api.irail.be/disturbances/?format=json&lineBreakCharacter=<br>&lang=en");

        // Test 5: Composition
        System.out.println("============================================");
        System.out.println("TEST 5: Composition API");
        System.out.println("============================================");
        testEndpoint("Composition - IC 1832",
                "https://api.irail.be/composition/?format=json&id=BE.NMBS.IC1832");

        // Test 6: Multiple sequential requests (like the app does)
        System.out.println("============================================");
        System.out.println("TEST 6: Sequential Requests (App Scenario)");
        System.out.println("============================================");
        testSequentialRequests(date, time);

        System.out.println();
        System.out.println("============================================");
        System.out.println("Performance Test Complete");
        System.out.println("============================================");
    }

    private static void testEndpoint(String name, String urlString) {
        System.out.println("Testing: " + name);
        System.out.println("URL: " + urlString);

        List<Long> responseTimes = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (int i = 1; i <= RUNS_PER_ENDPOINT; i++) {
            System.out.print("  Run " + i + ": ");

            try {
                long startTime = System.currentTimeMillis();

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);

                int responseCode = conn.getResponseCode();

                // Read the response to ensure full transfer
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                long endTime = System.currentTimeMillis();
                long elapsed = endTime - startTime;

                if (responseCode == 200) {
                    responseTimes.add(elapsed);
                    successCount++;
                    int responseLength = response.length();
                    System.out.println(elapsed + "ms (HTTP " + responseCode +
                                     ", " + responseLength + " chars)");
                } else {
                    failureCount++;
                    System.out.println("FAILED (HTTP " + responseCode + ")");
                }

                conn.disconnect();

                // Small delay between requests
                Thread.sleep(500);

            } catch (Exception e) {
                failureCount++;
                System.out.println("FAILED (" + e.getMessage() + ")");
            }
        }

        // Calculate statistics
        if (successCount > 0) {
            long total = responseTimes.stream().mapToLong(Long::longValue).sum();
            long avg = total / successCount;
            long min = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
            long max = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

            System.out.println();
            System.out.println("  Results:");
            System.out.println("    Average: " + avg + "ms");
            System.out.println("    Min:     " + min + "ms");
            System.out.println("    Max:     " + max + "ms");
            System.out.println("    Success: " + successCount + "/" + RUNS_PER_ENDPOINT);
        } else {
            System.out.println("  Results: All requests failed");
        }
        System.out.println();
    }

    private static void testSequentialRequests(String date, String time) {
        System.out.println("Simulating app workflow: Liveboard followed by Connection");

        for (int i = 1; i <= 3; i++) {
            System.out.println("\n  Scenario " + i + ":");
            long totalTime = 0;

            try {
                // First request: Liveboard
                System.out.print("    1. Loading liveboard... ");
                long start1 = System.currentTimeMillis();
                makeRequest("https://api.irail.be/liveboard/?format=json&id=" +
                           BRUSSELS_CENTRAL + "&date=" + date + "&time=" + time + "&arrdep=dep");
                long time1 = System.currentTimeMillis() - start1;
                System.out.println(time1 + "ms");
                totalTime += time1;

                Thread.sleep(100); // Small delay like user interaction

                // Second request: Connections
                System.out.print("    2. Loading connections... ");
                long start2 = System.currentTimeMillis();
                makeRequest("https://api.irail.be/connections/?format=json&from=" +
                           BRUSSELS_CENTRAL + "&to=" + OUDENAARDE +
                           "&date=" + date + "&time=" + time + "&timeSel=depart");
                long time2 = System.currentTimeMillis() - start2;
                System.out.println(time2 + "ms");
                totalTime += time2;

                System.out.println("    Total time: " + totalTime + "ms (" +
                                 (totalTime / 1000.0) + " seconds)");

                Thread.sleep(1000);

            } catch (Exception e) {
                System.out.println("FAILED: " + e.getMessage());
            }
        }
        System.out.println();
    }

    private static void makeRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);

        BufferedReader in = new BufferedReader(
            new InputStreamReader(conn.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            // Read all data
        }
        in.close();
        conn.disconnect();
    }
}
