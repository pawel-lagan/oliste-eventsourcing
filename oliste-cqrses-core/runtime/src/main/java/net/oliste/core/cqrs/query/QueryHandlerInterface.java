package net.oliste.core.cqrs.query;

public interface QueryHandlerInterface<T extends Query, S> {
  S performQuery(T query);
}
