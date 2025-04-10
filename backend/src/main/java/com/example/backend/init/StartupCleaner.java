package com.example.backend.init;

import com.example.backend.repository.CodQueryRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
public class StartupCleaner {

    private final CodQueryRepository codQueryRepository;

    public StartupCleaner(CodQueryRepository codQueryRepository) {
        this.codQueryRepository = codQueryRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanIncompleteQueries() {
        long toDelete = codQueryRepository.countByCompletedFalse();
        codQueryRepository.deleteByCompletedFalse();
        System.out.println("[StartupCleaner] Usunięto " + toDelete + " nieukończonych zapytań z tabeli cod_query.");
    }
}
