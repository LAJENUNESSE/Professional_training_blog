# Performance Notes

This folder contains a minimal JMeter test plan for before/after comparisons.

## JMeter CLI

```bash
jmeter -n \
  -t docs/perf/jmeter-articles.jmx \
  -JHOST=localhost \
  -JPORT=8080 \
  -JTHREADS=20 \
  -JRAMP_UP=10 \
  -JLOOPS=100 \
  -l build/jmeter/articles.jtl \
  -e -o build/jmeter/report
```

Notes:
- Run JMeter from a separate machine if possible; a 2c2g server should keep threads low.
- Compare `build/jmeter/report/index.html` before/after caching and indexes.
- Use `/actuator/metrics/http.server.requests` and `/actuator/prometheus` to correlate QPS and latency.
