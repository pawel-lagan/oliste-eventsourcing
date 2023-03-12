package net.oliste.examples.process;

import javax.enterprise.context.ApplicationScoped;
import lombok.AllArgsConstructor;
import net.oliste.core.cqrs.repository.ReadRepository;
import net.oliste.core.cqrs.repository.WriteRepository;

@ApplicationScoped
@AllArgsConstructor
public class ProcessRepository implements ReadRepository, WriteRepository {}
