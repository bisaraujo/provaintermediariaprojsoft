package insper.edu.br.provaintermediaria.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "avaliações")
public class Avaliacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String autor;

    @Column(nullable = false)
    private String conteudo;

    @Column(nullable = false)
    @Range(min = 1, max = 5)
    private Integer nota;


    private LocalDate dataCriacao;

}
