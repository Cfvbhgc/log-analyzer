package com.loganalyzer.analyzer;

import com.loganalyzer.parser.LogEntry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class StatisticsCollector {

  private int totalEntries;
  private Map<String, Long> levelCounts;
  private Map<Integer, Long> statusCodes;
  private Map<String, Long> hourlyDistribution;
  private Map<String, Long> topSources;
  private double errorRate;

  public StatisticsCollector collect(List<LogEntry> entries) {
    totalEntries = entries.size();

    levelCounts = entries.stream()
      .collect(Collectors.groupingBy(
        e -> e.getLevel().toUpperCase(),
        Collectors.counting()
      ));

    statusCodes = entries.stream()
      .filter(e -> e.statusCode() > 0)
      .collect(Collectors.groupingBy(
        LogEntry::statusCode,
        TreeMap::new,
        Collectors.counting()
      ));

    hourlyDistribution = entries.stream()
      .collect(Collectors.groupingBy(
        e -> e.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00")),
        TreeMap::new,
        Collectors.counting()
      ));

    topSources = entries.stream()
      .collect(Collectors.groupingBy(LogEntry::getSource, Collectors.counting()))
      .entrySet().stream()
      .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
      .limit(10)
      .collect(Collectors.toMap(
        Map.Entry::getKey, Map.Entry::getValue,
        (a, b) -> a, LinkedHashMap::new
      ));

    long errors = entries.stream().filter(LogEntry::isError).count();
    errorRate = totalEntries > 0 ? (double) errors / totalEntries * 100.0 : 0.0;

    return this;
  }

  public int getTotalEntries() {
    return totalEntries;
  }

  public Map<String, Long> getLevelCounts() {
    return levelCounts;
  }

  public Map<Integer, Long> getStatusCodes() {
    return statusCodes;
  }

  public Map<String, Long> getHourlyDistribution() {
    return hourlyDistribution;
  }

  public Map<String, Long> getTopSources() {
    return topSources;
  }

  public double getErrorRate() {
    return errorRate;
  }

  public String summary() {
    var sb = new StringBuilder();
    sb.append(String.format("Total entries: %d%n", totalEntries));
    sb.append(String.format("Error rate: %.2f%%%n", errorRate));
    sb.append("Level breakdown:\n");
    levelCounts.forEach((level, count) ->
      sb.append(String.format("  %-8s %d (%.1f%%)%n", level, count,
        (double) count / totalEntries * 100)));
    if (!statusCodes.isEmpty()) {
      sb.append("HTTP status codes:\n");
      statusCodes.forEach((code, count) ->
        sb.append(String.format("  %d: %d%n", code, count)));
    }
    return sb.toString();
  }
}
