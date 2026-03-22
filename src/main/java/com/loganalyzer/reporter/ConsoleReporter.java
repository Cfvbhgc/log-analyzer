package com.loganalyzer.reporter;

import com.loganalyzer.analyzer.AlertRule;
import com.loganalyzer.analyzer.StatisticsCollector;
import com.loganalyzer.parser.LogEntry;

import java.util.List;
import java.util.Map;

public class ConsoleReporter implements ReportGenerator {

  @Override
  public String generate(StatisticsCollector stats, List<LogEntry> matched,
                         Map<String, Long> frequencies, List<AlertRule.Alert> alerts) {
    var sb = new StringBuilder();

    sb.append("=".repeat(70)).append("\n");
    sb.append("  LOG ANALYSIS REPORT\n");
    sb.append("=".repeat(70)).append("\n\n");

    sb.append("--- Statistics ---\n");
    sb.append(stats.summary()).append("\n");

    if (!matched.isEmpty()) {
      sb.append("--- Matched Entries (").append(matched.size()).append(") ---\n");
      matched.stream()
        .limit(50)
        .forEach(e -> sb.append("  ").append(e).append("\n"));
      if (matched.size() > 50) {
        sb.append(String.format("  ... and %d more%n", matched.size() - 50));
      }
      sb.append("\n");
    }

    if (!frequencies.isEmpty()) {
      sb.append("--- Pattern Frequencies ---\n");
      frequencies.entrySet().stream()
        .limit(20)
        .forEach(e -> sb.append(String.format("  [%4d] %s%n", e.getValue(), e.getKey())));
      sb.append("\n");
    }

    if (!alerts.isEmpty()) {
      sb.append("--- Alerts (").append(alerts.size()).append(") ---\n");
      alerts.forEach(a -> sb.append("  ").append(a).append("\n"));
      sb.append("\n");
    }

    if (!stats.getHourlyDistribution().isEmpty()) {
      sb.append("--- Hourly Distribution ---\n");
      var max = stats.getHourlyDistribution().values().stream()
        .mapToLong(Long::longValue).max().orElse(1);
      stats.getHourlyDistribution().forEach((hour, count) -> {
        int barLen = (int) (count * 40 / max);
        sb.append(String.format("  %s | %-40s %d%n", hour, "#".repeat(barLen), count));
      });
      sb.append("\n");
    }

    sb.append("=".repeat(70)).append("\n");
    return sb.toString();
  }
}
