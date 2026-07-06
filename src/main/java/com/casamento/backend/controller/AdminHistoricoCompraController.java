package com.casamento.backend.controller;

import com.casamento.backend.model.HistoricoCompraCota;
import com.casamento.backend.repository.HistoricoCompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/historico-compras")
@CrossOrigin(origins = "*")
public class AdminHistoricoCompraController {

    @Autowired
    private HistoricoCompraRepository historicoCompraRepository;

    @GetMapping
    public List<HistoricoCompraCota> listarTodos() {
        return historicoCompraRepository.findAllByOrderByDataCompraDesc();
    }
}
