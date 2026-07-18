package com.casamento.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> TIPOS_IMAGEM = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif", "image/jpg"
    );

    private static final Set<String> TIPOS_AUDIO = Set.of(
            "audio/mpeg", "audio/mp3", "audio/mpeg3", "audio/x-mpeg-3", "audio/wav", "audio/x-wav", "audio/mp4"
    );

    private static final int LARGURA_MAX = 1600;
    private static final int ALTURA_MAX = 2000;
    private static final long AUDIO_MAX_BYTES = 15L * 1024 * 1024; // 15 MB

    private final Cloudinary cloudinary;
    private final String pastaCloudinary;
    private final Path uploadDirLegado = Paths.get("uploads/presentes").toAbsolutePath().normalize();

    public FileStorageService(
            Cloudinary cloudinary,
            @Value("${cloudinary.folder:presentes-casamento}") String pastaCloudinary) throws IOException {
        this.cloudinary = cloudinary;
        this.pastaCloudinary = pastaCloudinary;
        Files.createDirectories(uploadDirLegado);
    }

    public String salvarImagem(MultipartFile arquivo) throws IOException {
        return salvarImagem(arquivo, pastaCloudinary);
    }

    public String salvarImagem(MultipartFile arquivo, String pasta) throws IOException {
        validarImagem(arquivo);
        String pastaFinal = (pasta == null || pasta.isBlank()) ? pastaCloudinary : pasta.trim();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultado = cloudinary.uploader().upload(
                    arquivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", pastaFinal,
                            "public_id", UUID.randomUUID().toString(),
                            "resource_type", "image",
                            "transformation", ObjectUtils.asMap(
                                    "width", LARGURA_MAX,
                                    "height", ALTURA_MAX,
                                    "crop", "limit",
                                    "quality", "auto:good"
                            )
                    )
            );
            return (String) resultado.get("secure_url");
        } catch (Exception e) {
            throw new IOException(mensagemCloudinary("imagem", e), e);
        }
    }

    /** Envia MP3 (ou áudio) ao Cloudinary e devolve URL pública. */
    public String salvarAudio(MultipartFile arquivo, String pasta) throws IOException {
        validarAudio(arquivo);
        String pastaFinal = (pasta == null || pasta.isBlank()) ? pastaCloudinary + "/musicas" : pasta.trim();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultado = cloudinary.uploader().upload(
                    arquivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", pastaFinal,
                            "public_id", UUID.randomUUID().toString(),
                            "resource_type", "video", // Cloudinary trata mp3 como video/audio
                            "format", "mp3"
                    )
            );
            String url = (String) resultado.get("secure_url");
            if (url == null || url.isBlank()) {
                throw new IOException("Cloudinary não retornou URL do áudio.");
            }
            return url;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(mensagemCloudinary("áudio", e), e);
        }
    }

    public void excluirImagem(String caminho) {
        excluirRecurso(caminho, "image");
    }

    public void excluirAudio(String caminho) {
        excluirRecurso(caminho, "video");
    }

    private void excluirRecurso(String caminho, String resourceType) {
        if (caminho == null || caminho.isBlank()) {
            return;
        }
        if (caminho.startsWith("http")) {
            String publicId = extrairPublicId(caminho);
            if (publicId == null) return;
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
            } catch (Exception ignored) {
            }
            return;
        }
        if (!caminho.startsWith("/uploads/presentes/")) {
            return;
        }
        try {
            String nomeArquivo = caminho.substring("/uploads/presentes/".length());
            Files.deleteIfExists(uploadDirLegado.resolve(nomeArquivo));
        } catch (IOException ignored) {
        }
    }

    private String extrairPublicId(String url) {
        int indiceUpload = url.indexOf("/upload/");
        if (indiceUpload == -1) {
            return null;
        }
        String restante = url.substring(indiceUpload + "/upload/".length());
        // remove transformações e versão (v123/)
        while (restante.contains("/")) {
            if (restante.matches("v\\d+/.*")) {
                restante = restante.substring(restante.indexOf('/') + 1);
                break;
            }
            // se começar com transformação (w_480,...), pula o 1º segmento
            String primeiro = restante.substring(0, restante.indexOf('/'));
            if (primeiro.contains(",") || primeiro.contains("_")) {
                restante = restante.substring(restante.indexOf('/') + 1);
            } else {
                break;
            }
        }
        if (restante.matches("v\\d+/.*")) {
            restante = restante.substring(restante.indexOf('/') + 1);
        }
        int ponto = restante.lastIndexOf('.');
        if (ponto > 0) {
            restante = restante.substring(0, ponto);
        }
        return restante.isBlank() ? null : restante;
    }

    private void validarImagem(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Envie uma imagem.");
        }
        String contentType = normalizarTipo(arquivo.getContentType());
        String nome = nomeArquivo(arquivo);
        boolean tipoOk = contentType != null && TIPOS_IMAGEM.contains(contentType);
        boolean extOk = nome.endsWith(".jpg") || nome.endsWith(".jpeg") || nome.endsWith(".png")
                || nome.endsWith(".webp") || nome.endsWith(".gif");
        if (!tipoOk && !extOk) {
            throw new IllegalArgumentException("Formato inválido. Use JPG, PNG, WEBP ou GIF.");
        }
    }

    private void validarAudio(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo de áudio (MP3).");
        }
        if (arquivo.getSize() > AUDIO_MAX_BYTES) {
            throw new IllegalArgumentException("Áudio muito grande. Máximo 15 MB.");
        }
        String contentType = normalizarTipo(arquivo.getContentType());
        String nome = nomeArquivo(arquivo);
        boolean tipoOk = contentType != null && TIPOS_AUDIO.contains(contentType);
        boolean extOk = nome.endsWith(".mp3") || nome.endsWith(".mpeg") || nome.endsWith(".wav") || nome.endsWith(".m4a");
        if (!tipoOk && !extOk) {
            throw new IllegalArgumentException("Formato inválido. Envie um MP3.");
        }
    }

    private static String normalizarTipo(String contentType) {
        if (contentType == null) return null;
        return contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
    }

    private static String nomeArquivo(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename();
        return nome == null ? "" : nome.toLowerCase(Locale.ROOT);
    }

    private static String mensagemCloudinary(String tipo, Exception e) {
        Throwable causa = e;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        String detalhe = causa.getMessage() != null ? causa.getMessage() : e.getMessage();
        if (detalhe == null || detalhe.isBlank()) {
            detalhe = e.getClass().getSimpleName();
        }
        if (detalhe.toLowerCase(Locale.ROOT).contains("invalid")
                || detalhe.toLowerCase(Locale.ROOT).contains("api_key")
                || detalhe.toLowerCase(Locale.ROOT).contains("401")
                || detalhe.toLowerCase(Locale.ROOT).contains("unauthorized")) {
            return "Falha ao enviar " + tipo + ": credenciais do Cloudinary inválidas. "
                    + "Confira CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY e CLOUDINARY_API_SECRET no Render.";
        }
        return "Falha ao enviar " + tipo + ": " + detalhe;
    }
}
