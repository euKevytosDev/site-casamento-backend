package com.casamento.backend.controller;

// IMPORTAÇÕES: Puxamos as ferramentas de web do Spring, o Repository e a Entidade
import com.casamento.backend.model.PresencaCasamento;
import com.casamento.backend.repository.PresencaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // Diz ao Spring que esta classe vai receber conexões da internet
@RequestMapping("/api/presenca") // Define a "URL/Endereço" que o site vai chamar (ex: localhost:8080/api/presenca)
@CrossOrigin(origins = "*") // Novo: Libera o CORS para que seu HTML consiga conversar com o java sem ser bloqueado!
public class PresencaController {

    @Autowired // Diz ao Spring: "Injete o nosso gerente do banco de dados aqui dentro"
    private PresencaRepository presencaRepository;

    // --- 1. COMANDO PARA SALVAR A FAMÍLIA INTEIRA (O NOSSO INSERT EM MASSA) ---
    @PostMapping("/confirmar-familia") // 🚨 ATUALIZADO: Criamos uma rota específica para a lista da família
    public ResponseEntity<String> confirmarPresencaFamilia(@RequestBody List<PresencaCasamento> listaFamilia) {
        // 📜 ENSINAMENTO DETALHADO:
        // Mudamos de 'PresencaCasamento' para 'List<PresencaCasamento>' no @RequestBody.
        // Agora o Spring sabe que vai receber um pacotão com vários convidados de uma vez só!

        // O método '.saveAll()' do Repository faz um insert em massa no PostgreSQL de forma automática!
        presencaRepository.saveAll(listaFamilia);

        // Retorna uma resposta HTTP 200 (OK) avisando ao front-end que deu tudo certo
        return ResponseEntity.ok("Presença da família confirmada com sucesso!");
    }

    // --- 2. COMANDO PARA LISTAR OS CONVIDADOS (O NOSSO SELECT AUTOMÁTICO) ---
    @GetMapping // Diz que se alguém acessar esse endereço, nós vamos MOSTRAR os dados
    public List<PresencaCasamento> listarTodos() {
        // O '.findAll()' faz um "SELECT * FROM presenca_casamento" por debaixo dos panos e traz a lista
        return presencaRepository.findAll();
    }
}