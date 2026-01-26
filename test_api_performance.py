#!/usr/bin/env python3
"""
iRail API Performance Test Script

Tests various endpoints and measures response times.
Requires: Python 3.6+ (no external dependencies)

Usage: python3 test_api_performance.py
"""

import time
import urllib.request
import urllib.error
from datetime import datetime
from typing import List, Tuple

# Configuration
USER_AGENT = "HyperRail-Performance-Test"
RUNS_PER_ENDPOINT = 5
TIMEOUT_SECONDS = 30

# Station IDs
BRUSSELS_CENTRAL = "BE.NMBS.008812005"
OUDENAARDE = "BE.NMBS.008892007"


def measure_request(url: str) -> Tuple[bool, int, int]:
    """
    Make a request and measure response time.
    Returns: (success, response_time_ms, response_size)
    """
    try:
        req = urllib.request.Request(url, headers={'User-Agent': USER_AGENT})
        start_time = time.time()

        with urllib.request.urlopen(req, timeout=TIMEOUT_SECONDS) as response:
            data = response.read()
            end_time = time.time()

            elapsed_ms = int((end_time - start_time) * 1000)
            return True, elapsed_ms, len(data)

    except Exception as e:
        return False, 0, 0


def test_endpoint(name: str, url: str) -> None:
    """Test an endpoint multiple times and report statistics."""
    print(f"\nTesting: {name}")
    print(f"URL: {url}")

    response_times: List[int] = []
    response_sizes: List[int] = []
    success_count = 0

    for run in range(1, RUNS_PER_ENDPOINT + 1):
        print(f"  Run {run}: ", end="", flush=True)

        success, elapsed_ms, size = measure_request(url)

        if success:
            response_times.append(elapsed_ms)
            response_sizes.append(size)
            success_count += 1
            print(f"{elapsed_ms}ms ({size} bytes)")
        else:
            print("FAILED")

        # Small delay between requests
        time.sleep(0.5)

    # Calculate statistics
    if response_times:
        avg_time = sum(response_times) // len(response_times)
        min_time = min(response_times)
        max_time = max(response_times)
        avg_size = sum(response_sizes) // len(response_sizes)

        print(f"\n  Results:")
        print(f"    Average: {avg_time}ms")
        print(f"    Min:     {min_time}ms")
        print(f"    Max:     {max_time}ms")
        print(f"    Avg Size: {avg_size:,} bytes")
        print(f"    Success: {success_count}/{RUNS_PER_ENDPOINT}")
    else:
        print(f"\n  Results: All requests failed")


def test_sequential_workflow(date: str, time_str: str) -> None:
    """Test sequential requests simulating real app usage."""
    print("\nSimulating app workflow: Liveboard followed by Connections")

    urls = [
        ("Liveboard", f"https://api.irail.be/liveboard/?format=json&id={BRUSSELS_CENTRAL}&date={date}&time={time_str}&arrdep=dep"),
        ("Connections", f"https://api.irail.be/connections/?format=json&from={BRUSSELS_CENTRAL}&to={OUDENAARDE}&date={date}&time={time_str}&timeSel=depart")
    ]

    for scenario in range(1, 4):
        print(f"\n  Scenario {scenario}:")
        total_time = 0

        for i, (name, url) in enumerate(urls, 1):
            print(f"    {i}. Loading {name.lower()}... ", end="", flush=True)

            success, elapsed_ms, size = measure_request(url)

            if success:
                print(f"{elapsed_ms}ms")
                total_time += elapsed_ms
            else:
                print("FAILED")
                break

            time.sleep(0.1)  # Simulate user interaction delay

        print(f"    Total time: {total_time}ms ({total_time / 1000:.2f} seconds)")
        time.sleep(1)


def main():
    print("=" * 50)
    print("iRail API Performance Test (Python)")
    print("=" * 50)
    print()

    # Get current date/time for tests
    now = datetime.now()
    date = now.strftime("%d%m%y")
    time_str = now.strftime("%H%M")

    print(f"Test configuration:")
    print(f"  - Number of runs per endpoint: {RUNS_PER_ENDPOINT}")
    print(f"  - Timeout: {TIMEOUT_SECONDS}s")
    print(f"  - Date: {date}")
    print(f"  - Time: {time_str}")

    # Test 1: Liveboard
    print("\n" + "=" * 50)
    print("TEST 1: Liveboard API (Station Departures)")
    print("=" * 50)
    test_endpoint(
        "Liveboard - Brussels-Central",
        f"https://api.irail.be/liveboard/?format=json&id={BRUSSELS_CENTRAL}&date={date}&time={time_str}&arrdep=dep"
    )

    # Test 2: Connections
    print("\n" + "=" * 50)
    print("TEST 2: Connections API (Route Planning)")
    print("=" * 50)
    test_endpoint(
        "Connections - Brussels to Oudenaarde",
        f"https://api.irail.be/connections/?format=json&from={BRUSSELS_CENTRAL}&to={OUDENAARDE}&date={date}&time={time_str}&timeSel=depart"
    )

    # Test 3: Vehicle
    print("\n" + "=" * 50)
    print("TEST 3: Vehicle API (Train Journey)")
    print("=" * 50)
    test_endpoint(
        "Vehicle - IC 1832",
        f"https://api.irail.be/vehicle/?format=json&id=BE.NMBS.IC1832&date={date}"
    )

    # Test 4: Disturbances
    print("\n" + "=" * 50)
    print("TEST 4: Disturbances API")
    print("=" * 50)
    test_endpoint(
        "Disturbances",
        "https://api.irail.be/disturbances/?format=json&lineBreakCharacter=<br>&lang=en"
    )

    # Test 5: Composition
    print("\n" + "=" * 50)
    print("TEST 5: Composition API")
    print("=" * 50)
    test_endpoint(
        "Composition - IC 1832",
        "https://api.irail.be/composition/?format=json&id=BE.NMBS.IC1832"
    )

    # Test 6: Sequential workflow
    print("\n" + "=" * 50)
    print("TEST 6: Sequential Requests (App Scenario)")
    print("=" * 50)
    test_sequential_workflow(date, time_str)

    print("\n" + "=" * 50)
    print("Performance Test Complete")
    print("=" * 50)
    print("\nAnalysis:")
    print("  - If API response times are < 2s: Issue is in app logic/parsing")
    print("  - If API response times are 3-5s: API is moderately slow")
    print("  - If API response times are > 5s: API is the primary bottleneck")
    print("  - If sequential workflow is > 10s: Matches reported issue")
    print()


if __name__ == "__main__":
    main()
