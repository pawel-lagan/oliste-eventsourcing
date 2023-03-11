package net.oliste.core.es.model;

public interface Aggregate<T extends Event> {
  Long getId();

  void setId(Long id);

  Long getVersion();

  void setVersion(Long id);
}
