package com.muv.backend.service;

import com.muv.backend.dto.CentralNotificacoes;
import com.muv.backend.dto.CentralNotificacoes.Item;
import com.muv.backend.repository.NotificacaoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaNotificacoesService {
    private final NotificacaoRepository notificacoes;

    public ConsultaNotificacoesService(NotificacaoRepository notificacoes) {
        this.notificacoes = notificacoes;
    }

    @Transactional(readOnly = true)
    public CentralNotificacoes listar(long usuarioId, boolean somenteNaoLidas) {
        List<Item> itens = notificacoes.listar(usuarioId, somenteNaoLidas).stream()
                .map(registro -> item(registro))
                .toList();
        return new CentralNotificacoes(itens, notificacoes.contarNaoLidas(usuarioId));
    }

    @Transactional
    public void marcarComoLida(long usuarioId, long notificacaoId) {
        notificacoes.marcarComoLida(usuarioId, notificacaoId);
    }

    @Transactional
    public void marcarTodasComoLidas(long usuarioId) {
        notificacoes.marcarTodasComoLidas(usuarioId);
    }

    private static Item item(NotificacaoRepository.Registro registro) {
        String url = null;
        String rotulo = null;
        if (registro.solicitacaoId() != null) {
            if ("CLIENTE".equals(registro.perfil())) {
                url = "/cliente/solicitacoes/" + registro.solicitacaoId() + "/propostas";
                rotulo = "Ver proposta";
            } else if ("PRESTADOR".equals(registro.perfil())) {
                url = "/prestador/solicitacoes/" + registro.solicitacaoId() + "/avaliacao";
                rotulo = "Ver solicitação";
            }
        }
        if (registro.freteId() != null) {
            if ("CLIENTE".equals(registro.perfil())) {
                url = "/cliente/fretes/" + registro.freteId();
                rotulo = "Ver frete";
            } else if ("PRESTADOR".equals(registro.perfil())) {
                url = "/prestador/historico?frete=" + registro.freteId();
                rotulo = "Ver histórico";
            }
        }
        return new Item(registro.id(), registro.titulo(), registro.mensagem(), registro.criadoEm(),
                registro.lida(), registro.freteId(), registro.solicitacaoId(), rotulo, url);
    }
}
