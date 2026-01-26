#!/bin/bash

# API Performance Test Script for iRail API
# Tests various endpoints and measures response times

echo "============================================"
echo "iRail API Performance Test"
echo "============================================"
echo ""

# Test configuration
RUNS=5
USER_AGENT="HyperRail-Performance-Test"

# Brussels-Central station ID
BRUSSELS_CENTRAL="BE.NMBS.008812005"
# Oudenaarde station ID
OUDENAARDE="BE.NMBS.008892007"
# Current date/time for tests
DATE=$(date +%d%m%y)
TIME=$(date +%H%M)

echo "Test configuration:"
echo "  - Number of runs per endpoint: $RUNS"
echo "  - Date: $DATE"
echo "  - Time: $TIME"
echo ""

# Function to measure API response time
measure_endpoint() {
    local url=$1
    local name=$2

    echo "Testing: $name"
    echo "URL: $url"

    local total=0
    local success=0
    local failed=0

    for i in $(seq 1 $RUNS); do
        echo -n "  Run $i: "

        # Measure time using curl with timing
        local start=$(date +%s%N)
        local http_code=$(curl -s -o /dev/null -w "%{http_code}" \
            -H "User-Agent: $USER_AGENT" \
            --max-time 30 \
            "$url" 2>&1)
        local end=$(date +%s%N)

        if [ "$http_code" = "200" ]; then
            local elapsed=$(( ($end - $start) / 1000000 ))  # Convert to milliseconds
            echo "${elapsed}ms (HTTP $http_code)"
            total=$(( $total + $elapsed ))
            success=$(( $success + 1 ))
        else
            echo "FAILED (HTTP $http_code)"
            failed=$(( $failed + 1 ))
        fi

        # Small delay between requests
        sleep 0.5
    done

    if [ $success -gt 0 ]; then
        local avg=$(( $total / $success ))
        echo "  Average: ${avg}ms (${success}/${RUNS} successful)"
    else
        echo "  Average: N/A (all requests failed)"
    fi
    echo ""
}

# Test 1: Liveboard (Station departures)
echo "============================================"
echo "TEST 1: Liveboard API (Station Departures)"
echo "============================================"
measure_endpoint \
    "https://api.irail.be/liveboard/?format=json&id=${BRUSSELS_CENTRAL}&date=${DATE}&time=${TIME}&arrdep=dep" \
    "Brussels-Central Departures"

# Test 2: Connections (Route planning)
echo "============================================"
echo "TEST 2: Connections API (Route Planning)"
echo "============================================"
measure_endpoint \
    "https://api.irail.be/connections/?format=json&from=${BRUSSELS_CENTRAL}&to=${OUDENAARDE}&date=${DATE}&time=${TIME}&timeSel=depart" \
    "Brussels-Central to Oudenaarde"

# Test 3: Vehicle (Train details)
echo "============================================"
echo "TEST 3: Vehicle API (Train Journey)"
echo "============================================"
measure_endpoint \
    "https://api.irail.be/vehicle/?format=json&id=BE.NMBS.IC1832&date=${DATE}" \
    "IC 1832 Journey"

# Test 4: Disturbances
echo "============================================"
echo "TEST 4: Disturbances API"
echo "============================================"
measure_endpoint \
    "https://api.irail.be/disturbances/?format=json&lineBreakCharacter=<br>&lang=en" \
    "Current Disturbances"

# Test 5: Composition (Train composition)
echo "============================================"
echo "TEST 5: Composition API (Train Composition)"
echo "============================================"
measure_endpoint \
    "https://api.irail.be/composition/?format=json&id=BE.NMBS.IC1832" \
    "IC 1832 Composition"

echo "============================================"
echo "Performance Test Complete"
echo "============================================"
echo ""
echo "Summary:"
echo "  - If response times are consistently > 3-5 seconds, the API itself is slow"
echo "  - If response times are < 2 seconds, the issue may be in the app logic"
echo "  - Compare these times with the 10-20 second load times reported"
echo ""
