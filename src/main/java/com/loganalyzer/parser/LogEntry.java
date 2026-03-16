package com.loganalyzer.parser;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class LogEntry {

  private final LocalDateTime timestamp;
  private final String level;
  private final String source;
  private final String message;
  private final Map<String, String> metadata;

  private LogEntry(LocalDateTime timestamp, String level, String source,
                   String message, Map<String, String> metadata) {
    this.timestamp = timestamp;
    this.level = level;
    this.source = source;
    this.message = message;
    this.metadata = metadata == null
      ? Collections.emptyMap()
      : Collections.unmodifiableMap(new HashMap<>(metadata));
  }

  public static LogEntry of(LocalDateTime timestamp, String level,
                             String source, String message) {
    return new LogEntry(timestamp, level, source, message, null);
  }

  public static LogEntry of(LocalDateTime timestamp, String level,
                             String source, String message,
                             Map<String, String> metadata) {
    return new LogEntry(timestamp, level, source, message, metadata);
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public String getLevel() {
    return level;
  }

  public String getSource() {
    return source;
  }

  public String getMessage() {
    return message;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public Optional<String> meta(String key) {
    return Optional.ofNullable(metadata.get(key));
  }

  public boolean matches(String pattern) {
    return message.toLowerCase().contains(pattern.toLowerCase())
      || level.equalsIgnoreCase(pattern);
  }

  public boolean isError() {
    return "ERROR".equalsIgnoreCase(level) || "FATAL".equalsIgnoreCase(level);
  }

  public boolean isWarn() {
    return "WARN".equalsIgnoreCase(level) || "WARNING".equalsIgnoreCase(level);
  }

  public int statusCode() {
    return meta("status").map(Integer::parseInt).orElse(0);
  }

  @Override
  public String toString() {
    return String.format("[%s] %s %s - %s", timestamp, level, source, message);
  }
}
