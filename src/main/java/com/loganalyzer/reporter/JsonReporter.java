package com.loganalyzer.reporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loganalyzer.analyzer.AlertRule;
import com.loganalyzer.analyzer.StatisticsCollector;
import com.loganalyzer.parser.LogEntry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonReporter implements ReportGenerator {

  private static final ObjectMapper mapper = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .enable(SerializationFeature.INDENT_OUTPUT)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @Override
  public String generate(StatisticsCollector stats, List<LogEntry> matched,
                         Map<String, Long> frequencies, List<AlertRule.Alert> alerts) {
    var report = new LinkedHashMap<String, Object>();

    var statsMap = new LinkedHashMap<String, Object>();
    statsMap.put("totalEntries", stats.getTotalEntries());
    statsMap.put("errorRate", String.format("%.2f%%", stats.getErrorRate()));
    statsMap.put("levelCounts", stats.getLevelCounts());
    statsMap.put("statusCodes", stats.getStatusCodes());
    statsMap.put("hourlyDistribution", stats.getHourlyDistribution());
    statsMap.put("topSources", stats.getTopSources());
    report.put("statistics", statsMap);

    var matchedList = matched.stream()
      .limit(100)
      .map(this::entryMap)
      .toList();
    report.put("matchedEntries", matchedList);
    report.put("matchedCount", matched.size());

    report.put("patternFrequencies", frequencies);

    var alertList = alerts.stream()
      .map(a -> {
        var m = new LinkedHashMap<String, Object>();
        m.put("rule", a.getRuleName());
        m.put("triggeredAt", a.getTriggeredAt().toString());
        m.put("count", a.getCount());
        m.put("threshold", a.getThreshold());
        m.put("windowMinutes", a.getWindowMinutes());
        m.put("sample", a.getSample());
        return m;
      })
      .toList();
    report.put("alerts", alertList);

    try {
      return mapper.writeValueAsString(report);
    } catch (Exception e) {
      return "{\"error\": \"" + e.getMessage() + "\"}";
    }
  }

  private Map<String, Object> entryMap(LogEntry entry) {
    var m = new LinkedHashMap<String, Object>();
    m.put("timestamp", entry.getTimestamp().toString());
    m.put("level", entry.getLevel());
    m.put("source", entry.getSource());
    m.put("message", entry.getMessage());
    if (!entry.getMetadata().isEmpty()) {
      m.put("metadata", entry.getMetadata());
    }
    return m;
  }
}
