package com.loganalyzer.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogParserTest {

  private final LogParser parser = new LogParser();

  @TempDir
  Path tempDir;

  @Test
  void parsesApacheAccessLog() throws IOException {
    var file = tempDir.resolve("access.log");
    Files.writeString(file, String.join("\n",
      "192.168.1.1 - - [10/Jan/2024:14:30:00 +0000] \"GET /index.html HTTP/1.1\" 200 1024",
      "10.0.0.5 - - [10/Jan/2024:14:30:05 +0000] \"POST /api/data HTTP/1.1\" 500 512",
      "172.16.0.1 - - [10/Jan/2024:14:30:10 +0000] \"GET /missing HTTP/1.1\" 404 256"
    ));

    var entries = parser.parse(file);

    assertEquals(3, entries.size());
    assertEquals("INFO", entries.get(0).getLevel());
    assertEquals("ERROR", entries.get(1).getLevel());
    assertEquals("WARN", entries.get(2).getLevel());
    assertEquals("192.168.1.1", entries.get(0).getSource());
    assertTrue(entries.get(0).meta("path").isPresent());
    assertEquals("/index.html", entries.get(0).meta("path").get());
  }

  @Test
  void parsesCustomAppLog() throws IOException {
    var file = tempDir.resolve("app.log");
    Files.writeString(file, String.join("\n",
      "2024-01-10T14:30:00 [ERROR] com.app.Service - Database connection timeout",
      "2024-01-10T14:30:01 [INFO] com.app.Controller - Request processed in 45ms",
      "2024-01-10T14:30:02 [WARN] com.app.Cache - Cache miss ratio above 80%"
    ));

    var entries = parser.parse(file);

    assertEquals(3, entries.size());
    assertEquals("ERROR", entries.get(0).getLevel());
    assertEquals("com.app.Service", entries.get(0).getSource());
    assertEquals("Database connection timeout", entries.get(0).getMessage());
    assertTrue(entries.get(2).isWarn());
  }

  @Test
  void parsesJsonLog() throws IOException {
    var file = tempDir.resolve("json.log");
    Files.writeString(file, String.join("\n",
      "{\"timestamp\":\"2024-01-10T14:30:00\",\"level\":\"ERROR\",\"logger\":\"db.pool\",\"message\":\"Connection refused\"}",
      "{\"timestamp\":\"2024-01-10T14:30:01\",\"level\":\"INFO\",\"logger\":\"http.server\",\"message\":\"Started on port 8080\"}"
    ));

    var entries = parser.parse(file);

    assertEquals(2, entries.size());
    assertTrue(entries.get(0).isError());
    assertEquals("db.pool", entries.get(0).getSource());
    assertEquals("Connection refused", entries.get(0).getMessage());
  }

  @Test
  void detectsApacheFormat() throws IOException {
    var file = tempDir.resolve("detect.log");
    Files.writeString(file, String.join("\n",
      "10.0.0.1 - - [10/Jan/2024:14:00:00 +0000] \"GET / HTTP/1.1\" 200 500",
      "10.0.0.2 - - [10/Jan/2024:14:00:01 +0000] \"GET /test HTTP/1.1\" 200 300",
      "10.0.0.3 - - [10/Jan/2024:14:00:02 +0000] \"POST /api HTTP/1.1\" 201 100"
    ));

    assertEquals(LogFormat.APACHE, parser.detect(file));
  }

  @Test
  void handlesEmptyLines() throws IOException {
    var file = tempDir.resolve("sparse.log");
    Files.writeString(file, String.join("\n",
      "2024-01-10T14:30:00 [INFO] app.Main - Starting",
      "",
      "",
      "2024-01-10T14:30:01 [ERROR] app.Main - Failed",
      ""
    ));

    var entries = parser.parse(file);
    assertEquals(2, entries.size());
  }

  @Test
  void streamReturnsMatchingEntries() throws IOException {
    var file = tempDir.resolve("stream.log");
    Files.writeString(file, String.join("\n",
      "2024-01-10T14:30:00 [ERROR] svc.A - err one",
      "2024-01-10T14:30:01 [INFO] svc.B - ok",
      "2024-01-10T14:30:02 [ERROR] svc.C - err two"
    ));

    var errorCount = parser.stream(file)
      .filter(LogEntry::isError)
      .count();

    assertEquals(2, errorCount);
  }
}
