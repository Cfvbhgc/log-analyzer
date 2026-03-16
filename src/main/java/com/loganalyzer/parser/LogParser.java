package com.loganalyzer.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public class LogParser {

  private static final DateTimeFormatter APACHE_FMT =
    DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
  private static final DateTimeFormatter ISO_FMT =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS][.SS][.S]");
  private static final DateTimeFormatter SPACE_FMT =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS][.SS][.S]");

  private static final ObjectMapper mapper = new ObjectMapper();

  public List<LogEntry> parse(Path file) throws IOException {
    var format = detect(file);
    var lines = Files.readAllLines(file);
    var entries = new ArrayList<LogEntry>();
    for (var line : lines) {
      if (line.isBlank()) continue;
      extract(line, format).ifPresent(entries::add);
    }
    return entries;
  }

  public Stream<LogEntry> stream(Path file) throws IOException {
    var format = detect(file);
    return Files.lines(file)
      .filter(line -> !line.isBlank())
      .map(line -> extract(line, format))
      .flatMap(Optional::stream);
  }

  public LogFormat detect(Path file) throws IOException {
    var sample = Files.lines(file)
      .filter(l -> !l.isBlank())
      .limit(5)
      .toList();

    if (sample.isEmpty()) return LogFormat.CUSTOM;

    var first = sample.get(0).trim();
    if (first.startsWith("{")) return LogFormat.JSON;

    long apacheHits = sample.stream()
      .filter(l -> LogFormat.APACHE.pattern().matcher(l).find())
      .count();
    if (apacheHits >= sample.size() / 2 + 1) return LogFormat.APACHE;

    long nginxHits = sample.stream()
      .filter(l -> LogFormat.NGINX.pattern().matcher(l).find())
      .count();
    if (nginxHits >= sample.size() / 2 + 1) return LogFormat.NGINX;

    return LogFormat.CUSTOM;
  }

  private Optional<LogEntry> extract(String line, LogFormat format) {
    return switch (format) {
      case APACHE -> parseAccess(line, LogFormat.APACHE);
      case NGINX -> parseAccess(line, LogFormat.NGINX);
      case JSON -> parseJson(line);
      case CUSTOM -> parseCustom(line);
    };
  }

  private Optional<LogEntry> parseAccess(String line, LogFormat format) {
    Matcher m = format.pattern().matcher(line);
    if (!m.find()) return Optional.empty();

    var meta = new HashMap<String, String>();
    meta.put("ip", m.group("ip"));
    meta.put("method", m.group("method"));
    meta.put("path", m.group("path"));
    meta.put("status", m.group("status"));
    meta.put("size", m.group("size"));

    if (format == LogFormat.NGINX) {
      meta.put("referer", m.group("referer"));
      meta.put("agent", m.group("agent"));
    }

    var ts = parseTimestamp(m.group("time"));
    int code = Integer.parseInt(m.group("status"));
    var level = httpLevel(code);
    var msg = String.format("%s %s -> %s", m.group("method"), m.group("path"), m.group("status"));

    return Optional.of(LogEntry.of(ts, level, m.group("ip"), msg, meta));
  }

  private Optional<LogEntry> parseJson(String line) {
    try {
      var node = mapper.readTree(line);
      var ts = parseTimestamp(
        fieldOr(node, "timestamp", fieldOr(node, "@timestamp", "2024-01-01T00:00:00"))
      );
      var level = fieldOr(node, "level", fieldOr(node, "severity", "INFO"));
      var source = fieldOr(node, "logger", fieldOr(node, "source", "unknown"));
      var message = fieldOr(node, "message", fieldOr(node, "msg", ""));

      var meta = new HashMap<String, String>();
      node.fields().forEachRemaining(e -> meta.put(e.getKey(), e.getValue().asText()));

      return Optional.of(LogEntry.of(ts, level.toUpperCase(), source, message, meta));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private Optional<LogEntry> parseCustom(String line) {
    Matcher m = LogFormat.CUSTOM.pattern().matcher(line);
    if (!m.find()) return Optional.empty();

    var ts = parseTimestamp(m.group("time"));
    var level = m.group("level");
    var source = m.group("source");
    var message = m.group("message").trim();

    return Optional.of(LogEntry.of(ts, level, source, message));
  }

  private LocalDateTime parseTimestamp(String raw) {
    if (raw == null || raw.isBlank()) return LocalDateTime.now();
    try {
      return LocalDateTime.parse(raw, APACHE_FMT);
    } catch (DateTimeParseException ignored) {}
    try {
      return LocalDateTime.parse(raw, ISO_FMT);
    } catch (DateTimeParseException ignored) {}
    try {
      return LocalDateTime.parse(raw, SPACE_FMT);
    } catch (DateTimeParseException ignored) {}
    try {
      return LocalDateTime.parse(raw);
    } catch (DateTimeParseException ignored) {}
    return LocalDateTime.now();
  }

  private String httpLevel(int code) {
    if (code >= 500) return "ERROR";
    if (code >= 400) return "WARN";
    return "INFO";
  }

  private String fieldOr(JsonNode node, String field, String fallback) {
    var child = node.get(field);
    return child != null ? child.asText() : fallback;
  }
}
