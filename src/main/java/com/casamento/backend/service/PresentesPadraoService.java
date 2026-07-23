package com.casamento.backend.service;

import com.casamento.backend.model.PresenteCasamento;
import com.casamento.backend.model.Site;
import com.casamento.backend.repository.PresenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Presentes genéricos/engraçados criados automaticamente em sites novos.
 * Só preenche se o site ainda não tiver nenhum presente (a noiva pode remover/trocar).
 */
@Service
public class PresentesPadraoService {

    private record PresentePadrao(String nome, String descricao, String valor, int cotas, String imagem) {}

    private static final List<PresentePadrao> PADROES = List.of(
            new PresentePadrao(
                    "Vale-jantar: date night sem discutir a conta",
                    "Um jantar só de vocês dois — quem paga a conta? Os convidados, claro.",
                    "89.90",
                    8,
                    "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=640&h=640&fit=crop"),
            new PresentePadrao(
                    "Combustível pra lua de mel",
                    "Pra o tanque (e o coração) não ficarem no reservatório no meio da estrada.",
                    "79.90",
                    10,
                    "https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?w=640&h=640&fit=crop"),
            new PresentePadrao(
                    "Pizza às 2h da manhã (tradição sagrada)",
                    "Porque depois da festa a fome bate — e pizza resolve quase tudo.",
                    "49.90",
                    10,
                    "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=640&h=640&fit=crop"),
            new PresentePadrao(
                    "Maratona de série + pipoca XXL",
                    "Assinatura de streaming e pipoca pra brigarem só pelo controle remoto.",
                    "59.90",
                    8,
                    "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?w=640&h=640&fit=crop"),
            new PresentePadrao(
                    "Café da manhã na cama (delivery)",
                    "Pra acordarem como reis e rainhas — pelo menos no domingo.",
                    "69.90",
                    6,
                    "https://images.unsplash.com/photo-1533089860892-a7c6f0a88666?w=640&h=640&fit=crop"),
            new PresentePadrao(
                    "Kit “nunca mais brigue por causa da louça”",
                    "Contribuição simbólica pro enxoval (e pra paz doméstica).",
                    "54.90",
                    10,
                    "https://images.unsplash.com/photo-1556911220-bff31c812dba?w=640&h=640&fit=crop"),
            new PresentePadrao(
                    "Fundo “sair pra jantar sem culpa”",
                    "Reserva de emergência do casal: quando a vontade de pizza gourmet bater.",
                    "99.90",
                    8,
                    "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=640&h=640&fit=crop"),
            new PresentePadrao(
                    "Sofá que aguenta o peso do amor",
                    "Ajuda pro sofá novo — aquele que vai ver mais Netflix do que a sogra.",
                    "129.90",
                    6,
                    "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=640&h=640&fit=crop"),
            new PresentePadrao(
                    "Spa day do casal (ou um banho demorado)",
                    "Relaxar juntos — massagear o ego também conta.",
                    "119.90",
                    5,
                    "https://images.unsplash.com/photo-1544161515-4ab6ce6db874?w=640&h=640&fit=crop"),
            new PresentePadrao(
                    "Abraço… não, melhor: um mimo pro casal",
                    "Presente coringa: usem como quiserem. A gente só quer ver vocês felizes.",
                    "39.90",
                    12,
                    "https://images.unsplash.com/photo-1513201099705-a9746e1e201f?w=640&h=640&fit=crop")
    );

    private final PresenteRepository presenteRepository;

    public PresentesPadraoService(PresenteRepository presenteRepository) {
        this.presenteRepository = presenteRepository;
    }

    /**
     * Cria os 10 presentes padrão se o site ainda não tiver nenhum.
     * Idempotente: se a noiva removeu tudo ou já cadastrou, não reinsere.
     */
    @Transactional
    public void garantirPresentesPadrao(Site site) {
        if (site == null || site.getId() == null) {
            return;
        }
        if (presenteRepository.countBySiteId(site.getId()) > 0) {
            return;
        }

        for (PresentePadrao padrao : PADROES) {
            PresenteCasamento presente = new PresenteCasamento();
            presente.setSite(site);
            presente.setNome(padrao.nome());
            presente.setDescricao(padrao.descricao());
            presente.setValor(new BigDecimal(padrao.valor()));
            presente.setCotasTotal(padrao.cotas());
            presente.setCotasVendidas(0);
            presente.setImagem(padrao.imagem());
            presente.setComprado(false);
            presente.setNomeComprador(null);
            presenteRepository.save(presente);
        }
    }
}
