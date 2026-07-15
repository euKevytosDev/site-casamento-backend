package com.casamento.backend.service;

import com.casamento.backend.config.SiteContext;
import com.casamento.backend.model.Site;
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

    /** PIX global do servidor (fallback se o Site não tiver chave). */
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
        Site site = SiteContext.get();
        if (site != null && site.getPixChave() != null && !site.getPixChave().isBlank()) {
            return true;
        }
        return !chavePix.isBlank();
    }

    /**
     * Monta o texto do PIX Copia e Cola (QR Code).
     *
     * valor = quanto a pessoa vai pagar
     * txid  = um código interno da compra (tipo um "número do pedido")
     *
     * De onde vem a chave PIX?
     * 1) Primeiro tenta do Site do casamento (header X-Site-Id)
     * 2) Se o Site não tiver PIX cadastrado, usa o PIX global do servidor
     */
    public String gerarPayload(BigDecimal valor, String txid) {

        // PARTE A — Descobrir QUAL chave PIX usar
        Site site = SiteContext.get();

        String chave;
        String nome;
        String cidade;

        if (site != null
                && site.getPixChave() != null
                && !site.getPixChave().isBlank()) {

            chave = site.getPixChave().trim();

            if (site.getPixNomeRecebedor() != null
                    && !site.getPixNomeRecebedor().isBlank()) {
                nome = site.getPixNomeRecebedor();
            } else {
                nome = "Casamento";
            }

            if (site.getPixCidade() != null
                    && !site.getPixCidade().isBlank()) {
                cidade = site.getPixCidade();
            } else {
                cidade = "Brasil";
            }

        } else if (!chavePix.isBlank()) {

            chave = chavePix;
            nome = nomeRecebedor;
            cidade = cidadeRecebedor;

        } else {
            throw new IllegalStateException(
                    "PIX não configurado para este casamento."
            );
        }

        // PARTE B — Validar o valor
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do PIX inválido.");
        }

        // PARTE C — Formatar textos no padrão do Banco Central
        String referencia = normalizarTxid(txid);
        nome = sanitizarEmv(nome, 25);
        cidade = sanitizarEmv(cidade, 15);
        String valorFormatado = formatarValor(valor);

        // PARTE D — Montar blocos do código PIX
        String gui = campo("00", "BR.GOV.BCB.PIX");
        String chaveCampo = campo("01", chave);
        String contaPix = campo("26", gui + chaveCampo);

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

        // PARTE E — CRC16 (checksum)
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
