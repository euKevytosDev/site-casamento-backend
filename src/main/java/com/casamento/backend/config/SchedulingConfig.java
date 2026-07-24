package com.casamento.backend.config;

import com.casamento.backend.service.AssinaturaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    private static final Logger log = LoggerFactory.getLogger(SchedulingConfig.class);

    private final AssinaturaService assinaturaService;

    public SchedulingConfig(AssinaturaService assinaturaService) {
        this.assinaturaService = assinaturaService;
    }

    /** Todo dia às 03:15 — desliga sites em trial cujo prazo já passou sem pagamento. */
    @Scheduled(cron = "${asaas.trial-cron:0 15 3 * * *}")
    public void encerrarTrialsVencidos() {
        int n = assinaturaService.encerrarTrialsVencidos();
        if (n > 0) {
            log.info("Trials vencidos encerrados: {}", n);
        }
    }
}
