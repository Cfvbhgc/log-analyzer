package com.loganalyzer.analyzer;

import com.loganalyzer.parser.LogEntry;
import com.loganalyzer.util.TimeWindow;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AlertRule {

  private final String name;
  private final int threshold;
  private final int windowMinutes;
  private final String levelFilter;

  private AlertRule(String name, int threshold, int windowMinutes, String levelFilter) {
    this.name = name;
    this.threshold = threshold;
    this.windowMinutes = windowMinutes;
    this.levelFilter = levelFilter;
  }

  public static AlertRule of(String name, int threshold, int windowMinutes) {
    return new AlertRule(name, threshold, windowMinutes, "ERROR");
  }

  public static AlertRule of(String name, int threshold, int windowMinutes, String level) {
    return new AlertRule(name, threshold, windowMinutes, level);
  }

  public List<Alert> eval(List<LogEntry> entries) {
    var window = TimeWindow.of(windowMinutes);
    var alerts = new ArrayList<Alert>();

    var filtered = entries.stream()
      .filter(e -> e.getLevel().equalsIgnoreCase(levelFilter))
      .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
      .toList();

    for (var entry : filtered) {
      window.add(entry);
      window.evict(entry.getTimestamp());

      if (window.size() >= threshold) {
        var alert = new Alert(
          name,
          entry.getTimestamp(),
          window.size(),
          threshold,
          windowMinutes,
          entry.getMessage()
        );
        alerts.add(alert);
        window.clear();
      }
    }

    return alerts;
  }

  public String getName() {
    return name;
  }

  public int getThreshold() {
    return threshold;
  }

  public int getWindowMinutes() {
    return windowMinutes;
  }

  public String getLevelFilter() {
    return levelFilter;
  }

  public static class Alert {

    private final String ruleName;
    private final LocalDateTime triggeredAt;
    private final int count;
    private final int threshold;
    private final int windowMinutes;
    private final String sample;

    public Alert(String ruleName, LocalDateTime triggeredAt, int count,
                 int threshold, int windowMinutes, String sample) {
      this.ruleName = ruleName;
      this.triggeredAt = triggeredAt;
      this.count = count;
      this.threshold = threshold;
      this.windowMinutes = windowMinutes;
      this.sample = sample;
    }

    public String getRuleName() {
      return ruleName;
    }

    public LocalDateTime getTriggeredAt() {
      return triggeredAt;
    }

    public int getCount() {
      return count;
    }

    public int getThreshold() {
      return threshold;
    }

    public int getWindowMinutes() {
      return windowMinutes;
    }

    public String getSample() {
      return sample;
    }

    @Override
    public String toString() {
      return String.format("ALERT [%s] at %s: %d events in %d min (threshold: %d) - %s",
        ruleName, triggeredAt, count, windowMinutes, threshold, sample);
    }
  }
}
