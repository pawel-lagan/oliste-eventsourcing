package net.oliste.core.cqrs.command;

import io.smallrye.mutiny.Multi;

public interface CommandHandlerInterace<T extends Command, S> {
  public Multi<S> handle(T command);
}
