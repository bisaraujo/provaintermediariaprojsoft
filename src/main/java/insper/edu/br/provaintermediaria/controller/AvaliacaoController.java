package insper.edu.br.provaintermediaria.controller;

import insper.edu.br.provaintermediaria.model.Avaliacao;
import insper.edu.br.provaintermediaria.service.AvaliacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    @Autowired
    private AvaliacaoService avaliacaoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Avaliacao criar(@RequestBody Avaliacao avaliacao) {return avaliacaoService.criar(avaliacao);}

    @GetMapping
    public List<Avaliacao> listar(){ return avaliacaoService.listar();}

    @GetMapping("/{id}")
    public Avaliacao buscar(@PathVariable Long id){
        Optional<Avaliacao> avaliacao = avaliacaoService.buscarPorId(id);
        if(avaliacao.isPresent()){
            return avaliacao.get();
        }
        return null;
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id){
        avaliacaoService.deletar(id);
    }
}
