package com.casamento.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PixPayloadService {

    private static final Pattern NAO_ASCII = Pattern.compile("[^a-zA-Z0-9 ]");

    private final String chavePix;
    private final String nomeRecebedor;
    private final String cidadeRecebedor;

    public PixPayloadService(
            @Value("${pix.chave:}") String chavePix,
            @Value("${pix.nome-recebedor:Casamento}") String nomeRecebedor,
            @Value("${pix.cidade:Brasil}") String cidadeRecebedor) {
        this.chavePix = chavePix != null ? chavePix.trim() : "";
        this.nomeRecebedor = nomeRecebedor;
        this.cidadeRecebedor = cidadeRecebedor;
    }

    public boolean isConfigurado() {
        return !chavePix.isBlank();
    }

    public String gerarPayload(BigDecimal valor, String txid) {
        if (!isConfigurado()) {
            throw new IllegalStateException("PIX não configurado no servidor.");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do PIX inválido.");
        }

        String referencia = normalizarTxid(txid);
        String nome = sanitizarEmv(nomeRecebedor, 25);
        String cidade = sanitizarEmv(cidadeRecebedor, 15);
        String valorFormatado = formatarValor(valor);

        String gui = campo("00", "BR.GOV.BCB.PIX");
        String chave = campo("01", chavePix);
        String contaPix = campo("26", gui + chave);

        StringBuilder payload = new StringBuilder();
        payload.append(campo("00", "01"));
        payload.append(contaPix);
        payload.append(campo("52", "0000"));
        payload.append(campo("53", "986"));
        payload.append(campo("54", valorFormatado));
        payload.append(campo("58", "BR"));
        payload.append(campo("59", nome));
        payload.append(campo("60", cidade));
        payload.append(campo("62", campo("05", referencia)));

        String semCrc = payload + "6304";
        return semCrc + calcularCrc16(semCrc);
    }

    public String gerarTxid() {
        return ("CAS" + UUID.randomUUID().toString().replace("-", "")).substring(0, 20).toUpperCase(Locale.ROOT);
    }

    private static String normalizarTxid(String txid) {
        if (txid == null || txid.isBlank()) {
            return "PRESENTE";
        }
        String limpo = txid.replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.ROOT);
        if (limpo.isBlank()) {
            return "PRESENTE";
        }
        return limpo.length() > 25 ? limpo.substring(0, 25) : limpo;
    }

    private static String formatarValor(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return formato.format(valor);
    }

    private static String sanitizarEmv(String valor, int maximo) {
        if (valor == null) {
            return "";
        }
        String normalizado = java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        normalizado = NAO_ASCII.matcher(normalizado).replaceAll(" ").replaceAll("\\s+", " ").trim().toUpperCase(Locale.ROOT);
        if (normalizado.length() > maximo) {
            return normalizado.substring(0, maximo);
        }
        return normalizado;
    }

    private static String campo(String id, String valor) {
        return id + String.format(Locale.ROOT, "%02d", valor.length()) + valor;
    }

    static String calcularCrc16(String payload) {
        int crc = 0xFFFF;
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);

        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF;
            }
        }

        return String.format(Locale.ROOT, "%04X", crc);
    }
}
