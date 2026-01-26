# API Performance Testing

This directory contains three test scripts to measure the actual performance of the iRail API endpoints. These tests will help identify whether the 10-20 second load times are caused by:

1. **Slow API responses** (the API itself is the bottleneck)
2. **App-side issues** (parsing, UI updates, unnecessary sequential requests)
3. **Network issues** (your connection to the API)

## Test Scripts

### 1. Bash Script (Linux/macOS)
**File:** `test_api_performance.sh`

**Requirements:** `curl`, `bash`

**Run:**
```bash
chmod +x test_api_performance.sh
./test_api_performance.sh
```

**Pros:** Simple, no dependencies
**Cons:** Less detailed, basic timing

### 2. Python Script (Cross-platform)
**File:** `test_api_performance.py`

**Requirements:** Python 3.6+ (no external libraries needed)

**Run:**
```bash
python3 test_api_performance.py
```

**Pros:** Easy to run, detailed output, cross-platform
**Cons:** Requires Python

### 3. Java Program (Most accurate)
**File:** `ApiPerformanceTest.java`

**Requirements:** Java JDK 8+

**Compile and Run:**
```bash
javac ApiPerformanceTest.java
java ApiPerformanceTest
```

**Pros:** Most accurate (uses same networking stack as Android), detailed statistics
**Cons:** Requires compilation step

## What the Tests Measure

Each script tests the following endpoints:

1. **Liveboard API** - Station departure boards (Brussels-Central)
2. **Connections API** - Route planning (Brussels-Central → Oudenaarde)
3. **Vehicle API** - Train journey details (IC 1832)
4. **Disturbances API** - Current service disruptions
5. **Composition API** - Train composition information
6. **Sequential Workflow** - Simulates real app usage (liveboard then connections)

Each endpoint is tested 5 times to get average, min, and max response times.

## Interpreting the Results

### API Response Times

**< 1 second per request:**
- ✅ API is fast
- ⚠️ The 10-20s issue is likely in the app code (parsing, multiple sequential requests, UI updates)

**1-3 seconds per request:**
- ⚠️ API is moderately slow but acceptable
- ⚠️ Multiple sequential requests could add up
- Consider: Request caching, parallel requests, optimistic UI updates

**3-5 seconds per request:**
- ❌ API is slow
- ❌ Multiple requests will cause noticeable delays
- Consider: More aggressive caching, request bundling

**> 5 seconds per request:**
- ❌ API is very slow and is the primary bottleneck
- ❌ App optimization won't help much
- Consider: Alternative data sources, GTFS static data, local caching

### Sequential Workflow Test

This test simulates what a user experiences: viewing a liveboard, then searching for a route.

**Total time < 5 seconds:**
- ✅ Normal expected behavior
- ⚠️ If app shows 10-20s, the issue is in app logic

**Total time 5-10 seconds:**
- ⚠️ API is contributing to slowness
- ⚠️ App optimizations can help

**Total time > 10 seconds:**
- ❌ Matches reported issue
- ❌ API is the primary bottleneck

## Next Steps Based on Results

### If API is fast (< 2s average):
Focus on app-side optimizations:
- Check for unnecessary sequential requests (should be parallel)
- Profile JSON parsing performance
- Check for main thread blocking
- Look for redundant requests
- Review adapter update efficiency

### If API is slow (> 3s average):
Options to consider:
1. **Increase caching** - Cache responses longer, use stale-while-revalidate
2. **Parallel requests** - Make independent requests in parallel
3. **Optimistic UI** - Show cached/old data immediately, update when fresh data arrives
4. **Request bundling** - If possible, combine multiple requests
5. **Alternative APIs** - Research if belgianrail.be HAFAS is actually faster (test it too)
6. **Static GTFS** - Use GTFS static data for schedules, only use API for real-time updates

## Testing Alternative APIs

To test the direct HAFAS API endpoints mentioned in the research:

### Test belgianrail.be endpoints:

**Liveboard (stboard.exe):**
```bash
curl -X POST "http://www.belgianrail.be/jp/sncb-nmbs-routeplanner/stboard.exe/en" \
  -d "start=yes&input=008812005&boardType=dep&time=$(date +%H:%M)&date=$(date +%d.%m.%Y)"
```

**Connections (query.exe):**
```bash
curl -X POST "http://www.belgianrail.be/jp/sncb-nmbs-routeplanner/query.exe/en" \
  -d "start=1&S=008812005&Z=008892007&time=$(date +%H:%M)&date=$(date +%d.%m.%Y)&timeSel=depart"
```

Compare response times with iRail API to see if it's worth implementing.

## Running Tests from Different Locations

To test if network location affects performance, try running tests from:

1. **Home network** (your typical usage)
2. **Mobile data** (4G/5G)
3. **VPN enabled** (to test VPN impact)
4. **Different geographic location** (if traveling)

This helps identify if the issue is:
- API server location/CDN
- Network congestion
- VPN overhead
- ISP routing issues

## Example Output Analysis

```
Testing: Liveboard - Brussels-Central
  Run 1: 1234ms (HTTP 200, 45231 chars)
  Run 2: 987ms (HTTP 200, 45187 chars)
  Run 3: 1456ms (HTTP 200, 45298 chars)
  Run 4: 1123ms (HTTP 200, 45276 chars)
  Run 5: 1087ms (HTTP 200, 45234 chars)

  Results:
    Average: 1177ms
    Min:     987ms
    Max:     1456ms
    Success: 5/5
```

**Analysis:** ~1.2 seconds average is reasonable. If the app takes 10-20 seconds, the delay is NOT from this API call alone.

## Questions to Answer

After running these tests, you should be able to answer:

1. **How long does each API endpoint actually take?**
2. **Is the 10-20s delay caused by slow API or app logic?**
3. **Are there network/VPN issues affecting performance?**
4. **Would switching to direct HAFAS endpoints help?**
5. **Which endpoint is the slowest?**
6. **How much variance is there in response times?**

## Debugging App-Side Performance

If API is fast but app is slow, check these in Android:

```java
// Add logging to measure actual time in app
long startTime = System.currentTimeMillis();

// Make API request
api.getLiveboard(request);

// In callback:
long endTime = System.currentTimeMillis();
Log.d("Performance", "Total time from request to UI: " + (endTime - startTime) + "ms");
```

Look for time spent in:
- Network request
- JSON parsing
- Database operations
- UI thread updates
- Adapter updates

## Contact

After running these tests, report back with:
- Which test script you used
- Average response times for each endpoint
- Sequential workflow total times
- Your network conditions (WiFi/mobile/VPN)
- Comparison with the 10-20s you're experiencing in the app

This data will help determine the best optimization strategy.
