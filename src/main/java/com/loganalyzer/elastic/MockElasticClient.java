package com.loganalyzer.elastic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loganalyzer.parser.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MockElasticClient {

  private static final Logger log = LoggerFactory.getLogger(MockElasticClient.class);
  private static final ObjectMapper mapper = new ObjectMapper()
    .registerModule(new JavaTimeModule());

  private final String host;
  private final String indexName;
  private final List<Map<String, Object>> indexed;

  private MockElasticClient(String host, String indexName) {
    this.host = host;
    this.indexName = indexName;
    this.indexed = new ArrayList<>();
  }

  public static MockElasticClient of(String indexName) {
    return new MockElasticClient("http://localhost:9200", indexName);
  }

  public static MockElasticClient of(String host, String indexName) {
    return new MockElasticClient(host, indexName);
  }

  public String index(LogEntry entry) {
    var id = UUID.randomUUID().toString().substring(0, 8);
    var doc = new LinkedHashMap<String, Object>();
    doc.put("_id", id);
    doc.put("_index", indexName);
    doc.put("timestamp", entry.getTimestamp().toString());
    doc.put("level", entry.getLevel());
    doc.put("source", entry.getSource());
    doc.put("message", entry.getMessage());
    doc.put("metadata", entry.getMetadata());

    indexed.add(doc);
    log.debug("[MockElastic] indexed doc {} into {} on {}", id, indexName, host);
    return id;
  }

  public int bulk(List<LogEntry> entries) {
    entries.forEach(this::index);
    log.info("[MockElastic] bulk indexed {} documents into {}", entries.size(), indexName);
    return entries.size();
  }

  public List<Map<String, Object>> search(String field, String value) {
    return indexed.stream()
      .filter(doc -> {
        var v = doc.get(field);
        return v != null && v.toString().toLowerCase().contains(value.toLowerCase());
      })
      .toList();
  }

  public long count() {
    return indexed.size();
  }

  public List<Map<String, Object>> all() {
    return List.copyOf(indexed);
  }

  public String getHost() {
    return host;
  }

  public String getIndexName() {
    return indexName;
  }
}
