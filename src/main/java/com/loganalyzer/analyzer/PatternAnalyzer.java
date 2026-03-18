package com.loganalyzer.analyzer;

import com.loganalyzer.parser.LogEntry;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PatternAnalyzer {

  public List<LogEntry> scan(List<LogEntry> entries, String pattern) {
    var compiled = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    return entries.stream()
      .filter(e -> compiled.matcher(e.getMessage()).find()
        || e.getLevel().equalsIgnoreCase(pattern))
      .toList();
  }

  public Map<String, Long> freq(List<LogEntry> entries) {
    return entries.stream()
      .collect(Collectors.groupingBy(
        e -> normalize(e.getMessage()),
        Collectors.counting()
      ))
      .entrySet().stream()
      .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
      .collect(Collectors.toMap(
        Map.Entry::getKey, Map.Entry::getValue,
        (a, b) -> a, LinkedHashMap::new
      ));
  }

  public Map<String, List<LogEntry>> cluster(List<LogEntry> entries, int windowMinutes) {
    if (entries.isEmpty()) return Map.of();

    var sorted = entries.stream()
      .sorted(Comparator.comparing(LogEntry::getTimestamp))
      .toList();

    var clusters = new LinkedHashMap<String, List<LogEntry>>();
    var current = new ArrayList<LogEntry>();
    current.add(sorted.get(0));

    for (int i = 1; i < sorted.size(); i++) {
      var prev = sorted.get(i - 1);
      var curr = sorted.get(i);
      long gap = ChronoUnit.MINUTES.between(prev.getTimestamp(), curr.getTimestamp());

      if (gap <= windowMinutes) {
        current.add(curr);
      } else {
        if (current.size() > 1) {
          var label = rangeLabel(current);
          clusters.put(label, List.copyOf(current));
        }
        current = new ArrayList<>();
        current.add(curr);
      }
    }
    if (current.size() > 1) {
      clusters.put(rangeLabel(current), List.copyOf(current));
    }

    return clusters;
  }

  public List<LogEntry> topErrors(List<LogEntry> entries, int limit) {
    var errorFreq = entries.stream()
      .filter(LogEntry::isError)
      .collect(Collectors.groupingBy(
        e -> normalize(e.getMessage()),
        Collectors.toList()
      ));

    return errorFreq.values().stream()
      .sorted((a, b) -> Integer.compare(b.size(), a.size()))
      .limit(limit)
      .map(group -> group.get(0))
      .toList();
  }

  private String normalize(String message) {
    return message
      .replaceAll("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", "<IP>")
      .replaceAll("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}[^\\s]*", "<TS>")
      .replaceAll("\\b[0-9a-f]{8,}\\b", "<ID>")
      .replaceAll("\\d+", "<N>");
  }

  private String rangeLabel(List<LogEntry> group) {
    var first = group.get(0).getTimestamp();
    var last = group.get(group.size() - 1).getTimestamp();
    return first.toString() + " .. " + last.toString() + " (" + group.size() + " entries)";
  }
}
