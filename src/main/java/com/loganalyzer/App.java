package com.loganalyzer;

import com.loganalyzer.analyzer.AlertRule;
import com.loganalyzer.analyzer.PatternAnalyzer;
import com.loganalyzer.analyzer.StatisticsCollector;
import com.loganalyzer.elastic.MockElasticClient;
import com.loganalyzer.kafka.MockKafkaProducer;
import com.loganalyzer.parser.LogParser;
import com.loganalyzer.reporter.ConsoleReporter;
import com.loganalyzer.reporter.HtmlReporter;
import com.loganalyzer.reporter.JsonReporter;
import com.loganalyzer.reporter.ReportGenerator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
  name = "log-analyzer",
  mixinStandardHelpOptions = true,
  version = "log-analyzer 1.0.0",
  description = "Analyzes log files for patterns, statistics, and anomalies."
)
public class App implements Callable<Integer> {

  @Option(names = {"-i", "--input"}, required = true,
    description = "Input log file path")
  private Path input;

  @Option(names = {"-p", "--pattern"}, defaultValue = "ERROR",
    description = "Pattern to search for (regex or level name)")
  private String pattern;

  @Option(names = {"-o", "--output"},
    description = "Output file path (stdout if omitted)")
  private Path output;

  @Option(names = {"-f", "--format"}, defaultValue = "text",
    description = "Report format: text, json, html")
  private String format;

  @Option(names = {"--alert-threshold"}, defaultValue = "5",
    description = "Alert if error count exceeds this in the time window")
  private int alertThreshold;

  @Option(names = {"--alert-window"}, defaultValue = "10",
    description = "Alert time window in minutes")
  private int alertWindow;

  @Option(names = {"--kafka"},
    description = "Enable mock Kafka producer for alerts")
  private boolean kafkaEnabled;

  @Option(names = {"--elastic"},
    description = "Enable mock Elasticsearch indexing")
  private boolean elasticEnabled;

  @Override
  public Integer call() {
    try {
      return run();
    } catch (IOException e) {
      System.err.println("Failed to read input: " + e.getMessage());
      return 1;
    }
  }

  private int run() throws IOException {
    if (!Files.exists(input)) {
      System.err.println("File not found: " + input);
      return 1;
    }

    var parser = new LogParser();
    var entries = parser.parse(input);

    if (entries.isEmpty()) {
      System.err.println("No log entries parsed from " + input);
      return 1;
    }

    var analyzer = new PatternAnalyzer();
    var matched = analyzer.scan(entries, pattern);
    var frequencies = analyzer.freq(matched);

    var stats = new StatisticsCollector().collect(entries);

    var rule = AlertRule.of("error-burst", alertThreshold, alertWindow);
    var alerts = rule.eval(entries);

    if (kafkaEnabled && !alerts.isEmpty()) {
      var kafka = MockKafkaProducer.of("log-alerts");
      kafka.emit(alerts);
      System.err.printf("[Kafka] Sent %d alerts to topic 'log-alerts'%n", alerts.size());
    }

    if (elasticEnabled) {
      var elastic = MockElasticClient.of("logs");
      elastic.bulk(entries);
      System.err.printf("[Elastic] Indexed %d documents into 'logs'%n", entries.size());
    }

    ReportGenerator reporter = switch (format.toLowerCase()) {
      case "json" -> new JsonReporter();
      case "html" -> new HtmlReporter();
      default -> new ConsoleReporter();
    };

    var report = reporter.generate(stats, matched, frequencies, alerts);

    if (output != null) {
      Files.writeString(output, report);
      System.err.println("Report written to " + output);
    } else {
      System.out.println(report);
    }

    return 0;
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new App()).execute(args);
    System.exit(exitCode);
  }
}
