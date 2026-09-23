package insper.edu.br.provaintermediaria.service;


import insper.edu.br.provaintermediaria.model.Avaliacao;
import insper.edu.br.provaintermediaria.repository.AvaliacaoRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AvaliacaoServiceTests {

    @InjectMocks
    private AvaliacaoService avaliacaoService;

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    private Avaliacao avaliacao(Long id, String autor, String conteudo, Integer nota) {
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setId(id);
        avaliacao.setAutor(autor);
        avaliacao.setConteudo(conteudo);
        avaliacao.setNota(nota);
        return avaliacao;
    }

    @Test
    void deveCriarAvaliacao() {
        Avaliacao entrada = avaliacao(null, "Zambao", "Java", 5);
        Mockito.when(avaliacaoRepository.save(entrada)).thenReturn(avaliacao(1L, "Zambao", "Java", 5));

        Avaliacao resposta = avaliacaoService.criar(entrada);

        Assertions.assertEquals(1L, resposta.getId());
        Mockito.verify(avaliacaoRepository).save(entrada);
    }

    @Test
    void deveBuscarPorIdExistente() {
        Mockito.when(avaliacaoRepository.findById(1L)).thenReturn(Optional.of(avaliacao(1L, "Zambao", "Java", 5)));
        Optional<Avaliacao> resposta = avaliacaoService.buscarPorId(1L);
        Assertions.assertEquals(1L, resposta.get().getId());
    }

    @Test
    void deveLancarExcecaoQuandoIdNaoExiste(){
        Mockito.when(avaliacaoRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(RuntimeException.class, () -> avaliacaoService.buscarPorId(99L));
    }

}
