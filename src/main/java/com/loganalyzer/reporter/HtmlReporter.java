package com.loganalyzer.reporter;

import com.loganalyzer.analyzer.AlertRule;
import com.loganalyzer.analyzer.StatisticsCollector;
import com.loganalyzer.parser.LogEntry;

import java.util.List;
import java.util.Map;

public class HtmlReporter implements ReportGenerator {

  @Override
  public String generate(StatisticsCollector stats, List<LogEntry> matched,
                         Map<String, Long> frequencies, List<AlertRule.Alert> alerts) {
    var sb = new StringBuilder();
    sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
    sb.append("<meta charset=\"UTF-8\">\n");
    sb.append("<title>Log Analysis Report</title>\n");
    sb.append("<style>\n");
    sb.append("body{font-family:monospace;margin:2em;background:#1a1a2e;color:#e0e0e0}\n");
    sb.append("h1{color:#0abde3}h2{color:#48dbfb;border-bottom:1px solid #333;padding-bottom:4px}\n");
    sb.append("table{border-collapse:collapse;width:100%;margin:1em 0}\n");
    sb.append("th,td{border:1px solid #333;padding:6px 10px;text-align:left}\n");
    sb.append("th{background:#16213e}tr:nth-child(even){background:#0f3460}\n");
    sb.append(".error{color:#ee5a24}.warn{color:#f6b93b}.info{color:#78e08f}\n");
    sb.append(".bar{background:#0abde3;height:16px;display:inline-block}\n");
    sb.append(".alert{background:#6c1313;padding:8px;margin:4px 0;border-left:3px solid #ee5a24}\n");
    sb.append("</style>\n</head>\n<body>\n");

    sb.append("<h1>Log Analysis Report</h1>\n");

    sb.append("<h2>Statistics</h2>\n");
    sb.append(String.format("<p>Total entries: <strong>%d</strong></p>%n", stats.getTotalEntries()));
    sb.append(String.format("<p>Error rate: <strong>%.2f%%</strong></p>%n", stats.getErrorRate()));

    sb.append("<h3>Level Breakdown</h3>\n<table><tr><th>Level</th><th>Count</th><th>%</th></tr>\n");
    stats.getLevelCounts().forEach((level, count) -> {
      var cls = level.equalsIgnoreCase("ERROR") ? "error"
        : level.equalsIgnoreCase("WARN") ? "warn" : "info";
      double pct = (double) count / stats.getTotalEntries() * 100;
      sb.append(String.format(
        "<tr><td class=\"%s\">%s</td><td>%d</td><td>%.1f%%</td></tr>%n",
        cls, level, count, pct));
    });
    sb.append("</table>\n");

    if (!stats.getStatusCodes().isEmpty()) {
      sb.append("<h3>HTTP Status Codes</h3>\n<table><tr><th>Code</th><th>Count</th></tr>\n");
      stats.getStatusCodes().forEach((code, count) ->
        sb.append(String.format("<tr><td>%d</td><td>%d</td></tr>%n", code, count)));
      sb.append("</table>\n");
    }

    if (!stats.getHourlyDistribution().isEmpty()) {
      var max = stats.getHourlyDistribution().values().stream()
        .mapToLong(Long::longValue).max().orElse(1);
      sb.append("<h3>Hourly Distribution</h3>\n<table><tr><th>Hour</th><th>Count</th><th>Graph</th></tr>\n");
      stats.getHourlyDistribution().forEach((hour, count) -> {
        int width = (int) (count * 200 / max);
        sb.append(String.format(
          "<tr><td>%s</td><td>%d</td><td><span class=\"bar\" style=\"width:%dpx\"></span></td></tr>%n",
          hour, count, width));
      });
      sb.append("</table>\n");
    }

    if (!frequencies.isEmpty()) {
      sb.append("<h2>Pattern Frequencies</h2>\n<table><tr><th>Count</th><th>Pattern</th></tr>\n");
      frequencies.entrySet().stream().limit(20).forEach(e ->
        sb.append(String.format("<tr><td>%d</td><td>%s</td></tr>%n",
          e.getValue(), esc(e.getKey()))));
      sb.append("</table>\n");
    }

    if (!alerts.isEmpty()) {
      sb.append("<h2>Alerts</h2>\n");
      alerts.forEach(a -> sb.append(String.format(
        "<div class=\"alert\"><strong>%s</strong> at %s: %d events in %d min (threshold %d)<br>%s</div>%n",
        esc(a.getRuleName()), a.getTriggeredAt(), a.getCount(),
        a.getWindowMinutes(), a.getThreshold(), esc(a.getSample()))));
    }

    if (!matched.isEmpty()) {
      sb.append("<h2>Matched Entries</h2>\n<table><tr><th>Time</th><th>Level</th><th>Source</th><th>Message</th></tr>\n");
      matched.stream().limit(100).forEach(e -> {
        var cls = e.isError() ? "error" : e.isWarn() ? "warn" : "info";
        sb.append(String.format(
          "<tr><td>%s</td><td class=\"%s\">%s</td><td>%s</td><td>%s</td></tr>%n",
          e.getTimestamp(), cls, e.getLevel(), esc(e.getSource()), esc(e.getMessage())));
      });
      sb.append("</table>\n");
      if (matched.size() > 100) {
        sb.append(String.format("<p>... and %d more entries</p>%n", matched.size() - 100));
      }
    }

    sb.append("</body>\n</html>\n");
    return sb.toString();
  }

  private String esc(String s) {
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
