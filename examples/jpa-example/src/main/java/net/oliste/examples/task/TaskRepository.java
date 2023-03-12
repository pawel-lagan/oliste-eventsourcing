package net.oliste.reactive.task;

import javax.enterprise.context.ApplicationScoped;
import lombok.AllArgsConstructor;
import net.oliste.core.cqrs.repository.ReadRepository;
import net.oliste.core.cqrs.repository.WriteRepository;
import net.oliste.jpa.hibernate.BaseRepository;

@ApplicationScoped
@AllArgsConstructor
public class TaskRepository extends BaseRepository<Task>
    implements ReadRepository, WriteRepository {}
