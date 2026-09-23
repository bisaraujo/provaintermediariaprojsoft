package insper.edu.br.provaintermediaria.service;

import insper.edu.br.provaintermediaria.model.Avaliacao;
import insper.edu.br.provaintermediaria.repository.AvaliacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AvaliacaoService {
    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    public Avaliacao criar(Avaliacao avaliacao) {
        return avaliacaoRepository.save(avaliacao);
    }

    public List<Avaliacao> listar(){
        return avaliacaoRepository.findAll();
    }

    public Optional<Avaliacao> buscarPorId(Long id){
        return Optional.of(avaliacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada por esse id")));
    }

    public boolean deletar(Long id){
        if(avaliacaoRepository.existsById(id)){
            avaliacaoRepository.deleteById(id);
            return true;
        }

        return false;
    }
}
