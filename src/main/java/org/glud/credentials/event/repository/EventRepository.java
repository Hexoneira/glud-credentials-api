package org.glud.credentials.event.repository;

import org.glud.credentials.event.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByTenantTenantIdOrderByStartsAtDesc(Long tenantId);

    List<Event> findAllByOrderByStartsAtDesc();
}
