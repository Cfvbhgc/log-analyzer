package com.loganalyzer.reporter;

import com.loganalyzer.analyzer.AlertRule;
import com.loganalyzer.analyzer.StatisticsCollector;
import com.loganalyzer.parser.LogEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleReporterTest {

  private final LocalDateTime base = LocalDateTime.of(2024, 1, 10, 14, 0, 0);

  @Test
  void generatesTextReport() {
    var entries = List.of(
      LogEntry.of(base, "ERROR", "svc.A", "Database timeout"),
      LogEntry.of(base.plusSeconds(5), "INFO", "svc.B", "Request OK"),
      LogEntry.of(base.plusSeconds(10), "ERROR", "svc.A", "Database timeout"),
      LogEntry.of(base.plusMinutes(1), "WARN", "svc.C", "Slow query")
    );

    var stats = new StatisticsCollector().collect(entries);
    var frequencies = new LinkedHashMap<String, Long>();
    frequencies.put("Database timeout", 2L);
    frequencies.put("Slow query", 1L);

    var reporter = new ConsoleReporter();
    var report = reporter.generate(stats, entries, frequencies, List.of());

    assertTrue(report.contains("LOG ANALYSIS REPORT"));
    assertTrue(report.contains("Total entries: 4"));
    assertTrue(report.contains("ERROR"));
    assertTrue(report.contains("Database timeout"));
  }

  @Test
  void includesAlerts() {
    var entries = List.of(
      LogEntry.of(base, "ERROR", "svc", "err")
    );
    var stats = new StatisticsCollector().collect(entries);
    var alert = new AlertRule.Alert("test-rule", base, 5, 5, 10, "sample msg");

    var report = new ConsoleReporter().generate(stats, entries, Map.of(), List.of(alert));

    assertTrue(report.contains("Alerts"));
    assertTrue(report.contains("test-rule"));
  }

  @Test
  void jsonReporterProducesValidJson() {
    var entries = List.of(
      LogEntry.of(base, "ERROR", "svc", "fail"),
      LogEntry.of(base.plusSeconds(1), "INFO", "svc", "ok")
    );
    var stats = new StatisticsCollector().collect(entries);

    var report = new JsonReporter().generate(stats, entries, Map.of(), List.of());

    assertTrue(report.startsWith("{"));
    assertTrue(report.contains("\"totalEntries\""));
    assertTrue(report.contains("\"matchedEntries\""));
  }

  @Test
  void htmlReporterProducesHtml() {
    var entries = List.of(
      LogEntry.of(base, "ERROR", "svc", "fail")
    );
    var stats = new StatisticsCollector().collect(entries);

    var report = new HtmlReporter().generate(stats, entries, Map.of(), List.of());

    assertTrue(report.contains("<!DOCTYPE html>"));
    assertTrue(report.contains("Log Analysis Report"));
    assertTrue(report.contains("</html>"));
  }
}
