# LogAnalyzer

CLI tool for parsing, analyzing, and reporting on log files. Supports multiple log formats, pattern matching, statistical analysis, threshold-based alerting, and mock integrations with Kafka and Elasticsearch.

## Requirements

- Java 17+
- Gradle 8+

## Build

```bash
./gradlew build
./gradlew jar
```

## Usage

```bash
# Basic analysis - find ERROR entries in an Apache access log
java -jar build/libs/log-analyzer-1.0.0.jar --input data/access.log --pattern ERROR

# Analyze application log with JSON output
java -jar build/libs/log-analyzer-1.0.0.jar -i data/app.log -p "timeout|refused" -f json

# Generate HTML report for Nginx log
java -jar build/libs/log-analyzer-1.0.0.jar -i data/nginx.log -p "5[0-9]{2}" -f html -o report.html

# Custom alert thresholds with Kafka and Elasticsearch mocks
java -jar build/libs/log-analyzer-1.0.0.jar -i data/app.log -p ERROR \
  --alert-threshold 3 --alert-window 5 --kafka --elastic

# Using Gradle run task
./gradlew run --args="--input data/app.log --pattern ERROR --format text"
```

## CLI Options

| Flag | Short | Description | Default |
|------|-------|-------------|---------|
| `--input` | `-i` | Input log file path | (required) |
| `--pattern` | `-p` | Regex pattern or log level to match | `ERROR` |
| `--output` | `-o` | Output file path | stdout |
| `--format` | `-f` | Report format: `text`, `json`, `html` | `text` |
| `--alert-threshold` | | Error count to trigger alert | `5` |
| `--alert-window` | | Time window in minutes for alerts | `10` |
| `--kafka` | | Enable mock Kafka alert producer | disabled |
| `--elastic` | | Enable mock Elasticsearch indexing | disabled |
| `--help` | `-h` | Show help | |
| `--version` | `-V` | Show version | |

## Supported Log Formats

Format detection is automatic based on file content sampling.

### Apache Combined Log
```
192.168.1.10 - - [15/Mar/2024:08:01:12 +0000] "GET /index.html HTTP/1.1" 200 4523
```

### Nginx
```
203.0.113.10 - - [15/Mar/2024:10:00:01 +0000] "GET / HTTP/1.1" 200 5120 "-" "Mozilla/5.0"
```

### JSON
```json
{"timestamp":"2024-03-15T08:00:01","level":"ERROR","logger":"com.app.Service","message":"Connection timeout"}
```

### Custom Application Log
```
2024-03-15T08:04:00 [ERROR] com.app.db.ConnectionPool - Connection lost to primary database
```

## Architecture

```
com.loganalyzer
├── App.java                    # CLI entry point (Picocli)
├── parser/
│   ├── LogParser.java          # Multi-format log parser with auto-detection
│   ├── LogEntry.java           # Immutable log entry (static factory, no Lombok)
│   └── LogFormat.java          # Format enum with regex patterns
├── analyzer/
│   ├── PatternAnalyzer.java    # Pattern matching, frequency analysis, clustering
│   ├── StatisticsCollector.java # Aggregate statistics, error rates, distributions
│   └── AlertRule.java          # Threshold-based alerting with sliding window
├── reporter/
│   ├── ReportGenerator.java    # Reporter interface
│   ├── ConsoleReporter.java    # Plain text output with ASCII charts
│   ├── JsonReporter.java       # Structured JSON output
│   └── HtmlReporter.java       # Styled HTML report
├── kafka/
│   └── MockKafkaProducer.java  # Simulates Kafka alert publishing
├── elastic/
│   └── MockElasticClient.java  # Simulates Elasticsearch indexing and search
└── util/
    └── TimeWindow.java         # Sliding time window for rate calculations
```

## Report Formats

**Text** - Human-readable console output with statistics summary, matched entries, pattern frequencies, alerts, and ASCII bar charts for hourly distribution.

**JSON** - Machine-parseable output suitable for piping into other tools or dashboards. Includes full statistics, matched entries with metadata, and alert details.

**HTML** - Self-contained styled report with dark theme, tables, bar graphs, and color-coded severity levels. Opens directly in any browser.

## Testing

```bash
./gradlew test
```

Test classes cover log parsing (Apache, JSON, custom formats), pattern analysis (matching, frequency, clustering), and report generation (text, JSON, HTML output validation).

## Sample Data

The `data/` directory contains realistic log files for testing:

- `access.log` - Apache access log with mixed HTTP status codes (~100 lines)
- `app.log` - Application log with ERROR, WARN, INFO, DEBUG levels (~80 lines)
- `nginx.log` - Nginx format log with user agents and referers (~50 lines)
