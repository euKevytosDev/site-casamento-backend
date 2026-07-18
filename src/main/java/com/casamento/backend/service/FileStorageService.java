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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    /**
     * Limite no lado maior: sobra nitidez no celular/desktop estreito
     * e corta peso de fotos 4K da câmera/galeria.
     */
    private static final int LARGURA_MAX = 1600;
    private static final int ALTURA_MAX = 2000;

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
        validarArquivo(arquivo);

        String pastaFinal = (pasta == null || pasta.isBlank()) ? pastaCloudinary : pasta.trim();

        try {
            // Comprime/redimensiona NA ENTRADA: o que fica salvo no Cloudinary já é leve.
            // crop=limit → só reduz se for maior; não estica foto pequena.
            // q_auto:good → boa qualidade visual com arquivo bem menor.
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
            throw new IOException("Não foi possível enviar a imagem para a nuvem.", e);
        }
    }

    public void excluirImagem(String caminho) {
        if (caminho == null || caminho.isBlank()) {
            return;
        }

        if (caminho.startsWith("http")) {
            excluirDaCloudinary(caminho);
            return;
        }

        excluirArquivoLocal(caminho);
    }

    private void excluirDaCloudinary(String url) {
        String publicId = extrairPublicId(url);
        if (publicId == null) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (Exception ignored) {
        }
    }

    private void excluirArquivoLocal(String caminho) {
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
        if (restante.matches("v\\d+/.*")) {
            restante = restante.substring(restante.indexOf('/') + 1);
        }

        int ponto = restante.lastIndexOf('.');
        if (ponto > 0) {
            restante = restante.substring(0, ponto);
        }

        return restante.isBlank() ? null : restante;
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Envie uma imagem.");
        }

        String contentType = arquivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new IllegalArgumentException("Formato inválido. Use JPG, PNG, WEBP ou GIF.");
        }
    }
}
