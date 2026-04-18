#!/usr/bin/env bash
# Smoke test script — run after deployment to verify the app is healthy.
# Usage: ./smoke-test.sh https://agents.example.com your-jwt-token

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
TOKEN="${2:-}"

pass() { echo "  [PASS] $1"; }
fail() { echo "  [FAIL] $1"; exit 1; }

echo "=== Smoke Test: $BASE_URL ==="

# 1. Liveness probe
status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health/liveness")
[ "$status" == "200" ] && pass "Liveness probe" || fail "Liveness probe returned $status"

# 2. Readiness probe
status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health/readiness")
[ "$status" == "200" ] && pass "Readiness probe" || fail "Readiness probe returned $status"

# 3. Prometheus metrics endpoint
status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/prometheus")
[ "$status" == "200" ] && pass "Prometheus metrics" || fail "Prometheus metrics returned $status"

# 4. OpenAPI docs
status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/swagger-ui.html")
[ "$status" == "200" ] && pass "Swagger UI" || fail "Swagger UI returned $status"

# 5. Deployment info endpoint (no auth required for this demo endpoint)
response=$(curl -s "$BASE_URL/api/v1/deployment/info")
echo "$response" | grep -q '"status":"UP"' && pass "App info endpoint" || fail "App info endpoint: $response"

# 6. Auth guard — unauthenticated request should return 401
status=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/hello/chat" \
  -H "Content-Type: application/json" -d '{"message":"hi"}')
[ "$status" == "401" ] && pass "Unauthenticated → 401" || fail "Expected 401, got $status"

echo ""
echo "=== All smoke tests passed ==="
