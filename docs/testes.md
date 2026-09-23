# Como construir os testes da prova

> Modelos completos, com imports, prontos para adaptar: veja a Parte 4 e a
> Parte 5 do [roteiro-da-prova.md](roteiro-da-prova.md). Este arquivo explica
> o porque de cada parte.

O projeto `curso` mostra dois niveis de teste que provavelmente serao pedidos:

1. **teste unitario do service com Mockito**: rapido, sem Spring e sem banco;
2. **teste de integracao do controller com MockMvc e Testcontainers**: sobe o
   contexto Spring e usa um PostgreSQL real, descartavel, dentro do Docker.

Use os dois. Eles respondem perguntas diferentes.

## Regra mental: Arrange, Act, Assert

Todo teste pode ser escrito em tres blocos:

```java
// Arrange: monte dados e ensine o mock a responder
// Act: execute exatamente o metodo/comando que esta sendo testado
// Assert: confira o resultado e/ou a colaboracao esperada
```

Um teste deve ter um motivo claro para falhar. Nomes como
`deveCriarEntidadeQuandoDadosSaoValidos` ajudam a enxergar o comportamento.

## 1. Teste unitario do service com Mockito

O objetivo e testar somente a regra do service. O repository e substituido por
um mock: ele nao acessa banco e so faz aquilo que o teste configurar.

Crie o arquivo em um pacote equivalente a:
`src/test/java/.../service/EntidadeServiceTests.java`.

```java
@ExtendWith(MockitoExtension.class)
class EntidadeServiceTests {

    @InjectMocks
    private EntidadeService entidadeService;

    @Mock
    private EntidadeRepository entidadeRepository;

    @Test
    void deveCriarEntidade() {
        // Arrange
        Entidade entrada = new Entidade();
        entrada.setNome("Exemplo");

        Entidade salva = new Entidade();
        salva.setId(1L);
        salva.setNome("Exemplo");

        Mockito.when(entidadeRepository.save(Mockito.any(Entidade.class)))
                .thenReturn(salva);

        // Act
        Entidade resposta = entidadeService.criar(entrada);

        // Assert: valor devolvido
        Assertions.assertEquals(1L, resposta.getId());
        Assertions.assertEquals("Exemplo", resposta.getNome());

        // Assert: colaboracao com o repository
        Mockito.verify(entidadeRepository).save(entrada);
    }
}
```

O que cada anotacao faz:

- `@ExtendWith(MockitoExtension.class)`: liga Mockito ao JUnit 5;
- `@Mock`: cria um repository falso;
- `@InjectMocks`: cria o service e injeta os mocks no construtor;
- `when(...).thenReturn(...)`: programa uma resposta do mock;
- `verify(...)`: garante que o service chamou a operacao esperada;
- `any(Entidade.class)`: aceita qualquer objeto daquele tipo.

### Consultar por ID: caso encontrado e nao encontrado

Se o service usa `repository.findById(id)`, teste os dois caminhos:

```java
@Test
void deveBuscarEntidadeExistente() {
    Entidade entidade = new Entidade();
    entidade.setId(1L);

    Mockito.when(entidadeRepository.findById(1L))
            .thenReturn(Optional.of(entidade));

    Entidade resposta = entidadeService.buscarPorId(1L);

    Assertions.assertEquals(1L, resposta.getId());
}

@Test
void deveLancarExcecaoQuandoEntidadeNaoExiste() {
    Mockito.when(entidadeRepository.findById(99L))
            .thenReturn(Optional.empty());

    Assertions.assertThrows(
            RuntimeException.class, // troque pela excecao real do projeto
            () -> entidadeService.buscarPorId(99L)
    );

    Mockito.verify(entidadeRepository, Mockito.never()).save(Mockito.any());
}
```

### Lista com e sem filtro

Cada ramo do `if` precisa de um teste. Se o codigo trata `null`, string vazia e
filtro preenchido, normalmente sao tres cenarios:

```java
Mockito.when(entidadeRepository.findAll()).thenReturn(List.of(entidade));
List<Entidade> resposta = entidadeService.listar(null);
Assertions.assertEquals(1, resposta.size());

Mockito.when(entidadeRepository.findByNomeStartingWith("Ex"))
        .thenReturn(List.of(entidade));
List<Entidade> filtrada = entidadeService.listar("Ex");
Assertions.assertEquals("Exemplo", filtrada.get(0).getNome());
```

Nao configure uma chamada que o service nao deveria executar. Depois, use
`verify` para comprovar qual ramo foi escolhido.

### Atualizar e excluir

Para atualizar, em geral o mock precisa responder primeiro ao `findById` e
depois ao `save`. Confira os campos alterados com `ArgumentCaptor` quando o
objeto salvo nao estiver facilmente acessivel:

```java
ArgumentCaptor<Entidade> captor = ArgumentCaptor.forClass(Entidade.class);
Mockito.verify(entidadeRepository).save(captor.capture());
Assertions.assertEquals("Novo nome", captor.getValue().getNome());
```

Para exclusao fisica, verifique `delete` ou `deleteById`. Para exclusao logica,
verifique que o campo (por exemplo, `deletado`) mudou e que o objeto foi salvo.

## 2. Teste do controller com MockMvc e PostgreSQL

Aqui o objetivo e atravessar controller, service, repository, serializacao JSON
e banco. O repository nao e mockado. O Testcontainers cria um PostgreSQL limpo
para a classe de teste.

Estrutura inicial:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EntidadeControllerTests {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("prova_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntidadeRepository entidadeRepository;

    @BeforeEach
    void limparBanco() {
        entidadeRepository.deleteAll();
    }
}
```

`MockMvc` simula uma chamada HTTP sem abrir uma porta real. `ObjectMapper`
converte Java para JSON e JSON para Java. `@BeforeEach` impede que um teste
dependa dos dados deixados por outro. `@DirtiesContext` encerra o contexto
Spring antes que o PostgreSQL temporario seja removido, evitando conexoes
penduradas no fim da classe.

> No Spring Boot 4 deste projeto, os imports usados pelo modelo de `curso` sao
> `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` e
> `tools.jackson.databind.ObjectMapper`.

### POST

```java
@Test
void deveCriarEntidade() throws Exception {
    Entidade entrada = new Entidade();
    entrada.setNome("Exemplo");

    MvcResult result = mockMvc.perform(
                    post("/entidades")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(entrada)))
            .andExpect(status().isCreated())
            .andReturn();

    Entidade resposta = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            Entidade.class);

    Assertions.assertNotNull(resposta.getId());
    Assertions.assertEquals("Exemplo", resposta.getNome());
    Assertions.assertTrue(entidadeRepository.existsById(resposta.getId()));
}
```

### GET de lista

Prepare o banco diretamente pelo repository, chame a rota e desserialize como
array (porque a resposta JSON e uma lista):

```java
Entidade salva = new Entidade();
salva.setNome("Exemplo");
entidadeRepository.save(salva);

MvcResult result = mockMvc.perform(get("/entidades"))
        .andExpect(status().isOk())
        .andReturn();

Entidade[] resposta = objectMapper.readValue(
        result.getResponse().getContentAsString(), Entidade[].class);

Assertions.assertEquals(1, resposta.length);
```

Para filtros, acrescente `.param("nome", "Ex")` e prepare dados que permitam
confirmar que apenas os corretos voltaram.

### GET por ID, DELETE e erros

Use parametros de rota assim:

```java
mockMvc.perform(get("/entidades/{id}", entidade.getId()))
        .andExpect(status().isOk());

mockMvc.perform(delete("/entidades/{id}", entidade.getId()))
        .andExpect(status().isNoContent());
```

Tambem teste IDs inexistentes e confira o status exigido, normalmente `404`.
Para entrada invalida em um `POST`, envie JSON sem o campo obrigatorio e
confira o status definido pelo enunciado, normalmente `400`.

## 3. Quantos testes escrever

Nao existe um numero magico. Transforme cada comportamento em um cenario:

- caminho feliz de cada metodo do service;
- cada ramo de `if` ou regra de negocio;
- recurso encontrado e nao encontrado;
- entrada valida e invalida;
- lista sem filtro e cada filtro relevante;
- sucesso de cada endpoint e seus principais status de erro.

Um endpoint simples costuma ter pelo menos um teste de integracao. Um service
deve ter um teste para cada caminho logico, nao apenas um por metodo.

## 4. Erros comuns no modelo de referencia

Ao adaptar o projeto `curso`, cuide destes pontos:

- nao copie nomes `Curso`, `/cursos` ou pacotes antigos;
- nao deixe imports duplicados;
- nao use `orElse(null)` e depois acesse o objeto: lance uma excecao clara;
- limpe o repository entre testes de controller;
- nao use o banco local nos testes: deixe Testcontainers fornecer a URL;
- nao teste o mock; teste o comportamento do service;
- nao dependa da ordem dos testes;
- sempre execute a suite completa antes de entregar.

## 5. Cobertura JaCoCo

Rode:

```powershell
.\mvnw.cmd clean verify
```

Abra `target/site/jacoco/index.html`. Cores principais:

- verde: executado pelos testes;
- amarelo: alguns ramos foram executados;
- vermelho: nao executado.

Se um `if` estiver amarelo, provavelmente falta testar uma das condicoes. A
cobertura ajuda a encontrar buracos, mas as assercoes e os cenarios corretos e
que provam que o sistema funciona.

O comando `verify` tambem executa uma regra automatica: cada classe dentro do
pacote `service` precisa ter 100% das linhas **e 100% dos ramos (branches)**
cobertos. Se faltar uma linha, o
build e o GitHub Actions falham e mostram a classe abaixo de 100%.


