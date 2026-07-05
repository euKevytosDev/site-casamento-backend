package com.casamento.backend.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixPayloadServiceTest {

    @Test
    void geraPayloadPixComValorFixo() {
        PixPayloadService service = new PixPayloadService(
                "teste@example.com",
                "Maria e Joao",
                "Sao Paulo"
        );

        String payload = service.gerarPayload(new BigDecimal("150.50"), "PRESENTE123");

        assertTrue(payload.startsWith("000201"));
        assertTrue(payload.contains("BR.GOV.BCB.PIX"));
        assertTrue(payload.contains("teste@example.com"));
        assertTrue(payload.contains("150.50"));

        String crcInformado = payload.substring(payload.length() - 4);
        String payloadSemCrc = payload.substring(0, payload.length() - 4);
        assertEquals(crcInformado, PixPayloadService.calcularCrc16(payloadSemCrc));
    }

    @Test
    void crc16Consistente() {
        String base = "000201010212";
        String crc = PixPayloadService.calcularCrc16(base);
        assertEquals(4, crc.length());
    }
}
