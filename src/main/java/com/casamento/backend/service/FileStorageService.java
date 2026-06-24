package com.casamento.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final Path uploadDir = Paths.get("uploads/presentes").toAbsolutePath().normalize();

    public FileStorageService() throws IOException {
        Files.createDirectories(uploadDir);
    }

    public String salvarImagem(MultipartFile arquivo) throws IOException {
        validarArquivo(arquivo);

        String extensao = obterExtensao(arquivo.getOriginalFilename());
        String nomeArquivo = UUID.randomUUID() + extensao;
        Path destino = uploadDir.resolve(nomeArquivo);

        Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/presentes/" + nomeArquivo;
    }

    public void excluirImagem(String caminho) {
        if (caminho == null || !caminho.startsWith("/uploads/presentes/")) {
            return;
        }

        try {
            String nomeArquivo = caminho.substring("/uploads/presentes/".length());
            Files.deleteIfExists(uploadDir.resolve(nomeArquivo));
        } catch (IOException ignored) {
        }
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Envie uma imagem do presente.");
        }

        String contentType = arquivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new IllegalArgumentException("Formato inválido. Use JPG, PNG, WEBP ou GIF.");
        }
    }

    private String obterExtensao(String nomeOriginal) {
        if (nomeOriginal != null && nomeOriginal.contains(".")) {
            return nomeOriginal.substring(nomeOriginal.lastIndexOf('.')).toLowerCase();
        }
        return ".jpg";
    }
}
