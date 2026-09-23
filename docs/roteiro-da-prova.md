# Roteiro da prova

Sequência completa, do início ao deploy. Siga na ordem. Os comandos são para
**PowerShell**, dentro da pasta do projeto:

```powershell
cd C:\Users\gabis\OneDrive\Documentos\4semestre\projsoft\conteudo_pi\provaintermediaria
```

O enunciado exige: API + PostgreSQL, deploy automático, secrets, **100% de
cobertura no service**, teste de integração das rotas e **uma rota entregue via
Pull Request**.

Os exemplos abaixo usam uma entidade fictícia `Produto` só para mostrar a forma.
Troque nome, campos, rotas e regras pelo que o enunciado pedir.

---

## Parte 0: antes da prova começar

1. Abra o **Docker Desktop** e espere ficar "running". Os testes de integração
   usam Testcontainers, que precisa do Docker.
2. Atualize o projeto e confirme que a base está verde:

```powershell
git switch main
git pull
.\mvnw.cmd clean verify
```

Tem que terminar com `BUILD SUCCESS`.

3. Confirme que o deploy atual está no ar: abra `http://44.192.87.24:8082` no
   navegador. Uma **Whitelabel Error Page (404)** significa que a API está no ar,
   só ainda não tem rotas.

---

## Parte 1: ler o enunciado (5–10 min, no papel)

Marque no texto:

1. substantivos → entidades (ex: `Produto`);
2. campos, tipos e obrigatoriedade (`@NotBlank`, `@NotNull`, `@Positive`...);
3. rotas: verbo + caminho (`POST /produtos`, `GET /produtos/{id}`...);
4. filtros de consulta (`?nome=...`);
5. regras de negócio e casos de erro;
6. status HTTP de cada caso (201, 200, 204, 400, 404...);
7. exclusão física (`delete`) ou lógica (campo `ativo`/`deletado`).

Monte uma tabela: **rota | entrada | regra | saída | status**. Ela vira a sua
lista de testes.

---

## Parte 2: criar a branch da primeira rota

```powershell
git switch main
git pull
git switch -c feature/primeira-rota
```

---

## Parte 3: implementar as camadas

Estrutura de pacotes (dentro de `src/main/java/insper/edu/br/provaintermediaria/`):

```text
model/        entidades
repository/   interfaces JpaRepository
service/      regras de negócio  ← cobertura de 100% exigida aqui
controller/   rotas HTTP
exception/    exceções com status HTTP
observer/     observers (só se o enunciado pedir; ver Parte 5.5)
```

> A regra de cobertura vale para **todas** as classes do pacote `service`.
> Não coloque DTOs ou exceções dentro dele. Deixe só os services.

### 3.1 Entidade (`model/Produto.java`)

```java
package insper.edu.br.provaintermediaria.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;

    @NotNull
    @Positive
    private Double preco;
}
```

### 3.2 Repository (`repository/ProdutoRepository.java`)

```java
package insper.edu.br.provaintermediaria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import insper.edu.br.provaintermediaria.model.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    // Declare só as consultas que o enunciado pedir
    List<Produto> findByNomeContainingIgnoreCase(String nome);
}
```

### 3.3 Exceção (`exception/RecursoNaoEncontradoException.java`)

```java
package insper.edu.br.provaintermediaria.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
```

Para uma regra de negócio violada, crie outra igual com
`@ResponseStatus(HttpStatus.BAD_REQUEST)` (ou o status que o enunciado pedir).

### 3.4 Service (`service/ProdutoService.java`)

```java
package insper.edu.br.provaintermediaria.service;

import java.util.List;

import org.springframework.stereotype.Service;

import insper.edu.br.provaintermediaria.exception.RecursoNaoEncontradoException;
import insper.edu.br.provaintermediaria.model.Produto;
import insper.edu.br.provaintermediaria.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public Produto criar(Produto produto) {
        return produtoRepository.save(produto);
    }

    public List<Produto> listar(String nome) {
        if (nome == null || nome.isBlank()) {
            return produtoRepository.findAll();
        }
        return produtoRepository.findByNomeContainingIgnoreCase(nome);
    }

    public Produto buscarPorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto " + id + " nao encontrado"));
    }

    public Produto atualizar(Long id, Produto dados) {
        Produto produto = buscarPorId(id);
        produto.setNome(dados.getNome());
        produto.setPreco(dados.getPreco());
        return produtoRepository.save(produto);
    }

    public void excluir(Long id) {
        Produto produto = buscarPorId(id);
        produtoRepository.delete(produto);
    }
}
```

### 3.5 Controller (`controller/ProdutoController.java`)

```java
package insper.edu.br.provaintermediaria.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import insper.edu.br.provaintermediaria.model.Produto;
import insper.edu.br.provaintermediaria.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Produto criar(@RequestBody @Valid Produto produto) {
        return produtoService.criar(produto);
    }

    @GetMapping
    public List<Produto> listar(@RequestParam(required = false) String nome) {
        return produtoService.listar(nome);
    }

    @GetMapping("/{id}")
    public Produto buscar(@PathVariable Long id) {
        return produtoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public Produto atualizar(@PathVariable Long id, @RequestBody @Valid Produto produto) {
        return produtoService.atualizar(id, produto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        produtoService.excluir(id);
    }
}
```

O controller só traduz HTTP → service. Nenhuma regra de negócio aqui.
Com `@Valid`, um campo inválido já devolve **400** automaticamente.

---

## Parte 4: testes unitários do service (cobertura de 100%)

### Como pensar a cobertura

A regra exige **100% das linhas e 100% dos ramos (branches)** de cada classe
em `service`. Para cada método do service, liste os caminhos possíveis:

| Método | Caminhos → um teste para cada |
|---|---|
| `criar` | salva e retorna |
| `listar` | `nome == null` · `nome` em branco (`"  "`) · `nome` preenchido |
| `buscarPorId` | encontrado · não encontrado (exceção) |
| `atualizar` | encontrado (salva com os novos dados) · não encontrado |
| `excluir` | encontrado (chama `delete`) · não encontrado (**não** chama `delete`) |

Todo `if`, `||`, `&&`, `?:` e `orElseThrow` cria ramos. Uma condição com
`||` tem dois lados, então precisa de um teste para cada lado.

### Arquivo `src/test/java/insper/edu/br/provaintermediaria/service/ProdutoServiceTests.java`

```java
package insper.edu.br.provaintermediaria.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import insper.edu.br.provaintermediaria.exception.RecursoNaoEncontradoException;
import insper.edu.br.provaintermediaria.model.Produto;
import insper.edu.br.provaintermediaria.repository.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTests {

    @InjectMocks
    private ProdutoService produtoService;

    @Mock
    private ProdutoRepository produtoRepository;

    private Produto produto(Long id, String nome, Double preco) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setPreco(preco);
        return p;
    }

    @Test
    void deveCriarProduto() {
        Produto entrada = produto(null, "Caneta", 2.5);
        Mockito.when(produtoRepository.save(entrada)).thenReturn(produto(1L, "Caneta", 2.5));

        Produto resposta = produtoService.criar(entrada);

        Assertions.assertEquals(1L, resposta.getId());
        Mockito.verify(produtoRepository).save(entrada);
    }

    @Test
    void deveListarTodosQuandoNomeNulo() {
        Mockito.when(produtoRepository.findAll()).thenReturn(List.of(produto(1L, "Caneta", 2.5)));

        List<Produto> resposta = produtoService.listar(null);

        Assertions.assertEquals(1, resposta.size());
        Mockito.verify(produtoRepository, Mockito.never()).findByNomeContainingIgnoreCase(Mockito.any());
    }

    @Test
    void deveListarTodosQuandoNomeEmBranco() {
        Mockito.when(produtoRepository.findAll()).thenReturn(List.of());

        List<Produto> resposta = produtoService.listar("   ");

        Assertions.assertTrue(resposta.isEmpty());
        Mockito.verify(produtoRepository).findAll();
    }

    @Test
    void deveFiltrarPorNome() {
        Mockito.when(produtoRepository.findByNomeContainingIgnoreCase("can"))
                .thenReturn(List.of(produto(1L, "Caneta", 2.5)));

        List<Produto> resposta = produtoService.listar("can");

        Assertions.assertEquals("Caneta", resposta.get(0).getNome());
        Mockito.verify(produtoRepository, Mockito.never()).findAll();
    }

    @Test
    void deveBuscarPorIdExistente() {
        Mockito.when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(1L, "Caneta", 2.5)));

        Produto resposta = produtoService.buscarPorId(1L);

        Assertions.assertEquals("Caneta", resposta.getNome());
    }

    @Test
    void deveLancarExcecaoQuandoIdNaoExiste() {
        Mockito.when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(RecursoNaoEncontradoException.class,
                () -> produtoService.buscarPorId(99L));
    }

    @Test
    void deveAtualizarProduto() {
        Mockito.when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(1L, "Caneta", 2.5)));
        Mockito.when(produtoRepository.save(Mockito.any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

        produtoService.atualizar(1L, produto(null, "Lapis", 1.0));

        ArgumentCaptor<Produto> captor = ArgumentCaptor.forClass(Produto.class);
        Mockito.verify(produtoRepository).save(captor.capture());
        Assertions.assertEquals("Lapis", captor.getValue().getNome());
        Assertions.assertEquals(1.0, captor.getValue().getPreco());
    }

    @Test
    void naoDeveAtualizarQuandoIdNaoExiste() {
        Mockito.when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(RecursoNaoEncontradoException.class,
                () -> produtoService.atualizar(99L, produto(null, "Lapis", 1.0)));
        Mockito.verify(produtoRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void deveExcluirProduto() {
        Produto existente = produto(1L, "Caneta", 2.5);
        Mockito.when(produtoRepository.findById(1L)).thenReturn(Optional.of(existente));

        produtoService.excluir(1L);

        Mockito.verify(produtoRepository).delete(existente);
    }

    @Test
    void naoDeveExcluirQuandoIdNaoExiste() {
        Mockito.when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(RecursoNaoEncontradoException.class,
                () -> produtoService.excluir(99L));
        Mockito.verify(produtoRepository, Mockito.never()).delete(Mockito.any());
    }
}
```

Rodar **só** esses testes (rápido, não precisa de Docker):

```powershell
.\mvnw.cmd test -Dtest=ProdutoServiceTests
```

---

## Parte 5: testes de integração das rotas

Um teste para cada rota e cada status relevante. Usa um PostgreSQL temporário
(Testcontainers), então o **Docker Desktop precisa estar aberto**.

### Arquivo `src/test/java/insper/edu/br/provaintermediaria/controller/ProdutoControllerTests.java`

```java
package insper.edu.br.provaintermediaria.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import insper.edu.br.provaintermediaria.model.Produto;
import insper.edu.br.provaintermediaria.repository.ProdutoRepository;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProdutoControllerTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
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
    private ProdutoRepository produtoRepository;

    @BeforeEach
    void limparBanco() {
        produtoRepository.deleteAll();
    }

    private Produto salvar(String nome, Double preco) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setPreco(preco);
        return produtoRepository.save(p);
    }

    @Test
    void postDeveCriarERetornar201() throws Exception {
        Produto entrada = new Produto();
        entrada.setNome("Caneta");
        entrada.setPreco(2.5);

        MvcResult result = mockMvc.perform(post("/produtos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(entrada)))
                .andExpect(status().isCreated())
                .andReturn();

        Produto resposta = objectMapper.readValue(result.getResponse().getContentAsString(), Produto.class);
        Assertions.assertNotNull(resposta.getId());
        Assertions.assertTrue(produtoRepository.existsById(resposta.getId()));
    }

    @Test
    void postComCampoInvalidoDeveRetornar400() throws Exception {
        mockMvc.perform(post("/produtos")
                        .contentType("application/json")
                        .content("{\"nome\": \"\", \"preco\": -1}"))
                .andExpect(status().isBadRequest());

        Assertions.assertEquals(0, produtoRepository.count());
    }

    @Test
    void getDeveListarTodos() throws Exception {
        salvar("Caneta", 2.5);
        salvar("Lapis", 1.0);

        MvcResult result = mockMvc.perform(get("/produtos"))
                .andExpect(status().isOk())
                .andReturn();

        Produto[] resposta = objectMapper.readValue(result.getResponse().getContentAsString(), Produto[].class);
        Assertions.assertEquals(2, resposta.length);
    }

    @Test
    void getComFiltroDeveRetornarSoOsCorrespondentes() throws Exception {
        salvar("Caneta", 2.5);
        salvar("Lapis", 1.0);

        MvcResult result = mockMvc.perform(get("/produtos").param("nome", "can"))
                .andExpect(status().isOk())
                .andReturn();

        Produto[] resposta = objectMapper.readValue(result.getResponse().getContentAsString(), Produto[].class);
        Assertions.assertEquals(1, resposta.length);
        Assertions.assertEquals("Caneta", resposta[0].getNome());
    }

    @Test
    void getPorIdDeveRetornar200() throws Exception {
        Produto salvo = salvar("Caneta", 2.5);

        mockMvc.perform(get("/produtos/{id}", salvo.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void getPorIdInexistenteDeveRetornar404() throws Exception {
        mockMvc.perform(get("/produtos/{id}", 999))
                .andExpect(status().isNotFound());
    }

    @Test
    void putDeveAtualizar() throws Exception {
        Produto salvo = salvar("Caneta", 2.5);

        mockMvc.perform(put("/produtos/{id}", salvo.getId())
                        .contentType("application/json")
                        .content("{\"nome\": \"Lapis\", \"preco\": 1.0}"))
                .andExpect(status().isOk());

        Assertions.assertEquals("Lapis", produtoRepository.findById(salvo.getId()).get().getNome());
    }

    @Test
    void deleteDeveRemoverERetornar204() throws Exception {
        Produto salvo = salvar("Caneta", 2.5);

        mockMvc.perform(delete("/produtos/{id}", salvo.getId()))
                .andExpect(status().isNoContent());

        Assertions.assertFalse(produtoRepository.existsById(salvo.getId()));
    }

    @Test
    void deleteInexistenteDeveRetornar404() throws Exception {
        mockMvc.perform(delete("/produtos/{id}", 999))
                .andExpect(status().isNotFound());
    }
}
```

Mais explicações sobre Mockito e MockMvc: [testes.md](testes.md).

---

## Parte 5.5: padrão Observer (se o enunciado pedir)

**Ideia:** quando algo acontece no service (ex: produto criado), ele **avisa**
uma lista de interessados (observers) sem saber o que cada um faz. Cada
observer reage do seu jeito: registra log, atualiza estoque, grava histórico...

```text
ProdutoService ──notifica──► [ LogProdutoObserver, HistoricoProdutoObserver, ... ]
```

O Spring monta a lista sozinho: toda classe `@Component` que implementa a
interface entra automaticamente na `List<ProdutoObserver>` do service.

Pacote novo: `observer/` (fora de `service/`, então não entra na regra de 100%).

### 5.5.1 Interface (`observer/ProdutoObserver.java`)

```java
package insper.edu.br.provaintermediaria.observer;

import insper.edu.br.provaintermediaria.model.Produto;

public interface ProdutoObserver {

    void notificar(Produto produto);
}
```

Se o enunciado tiver vários eventos (criado, atualizado, excluído), crie um
método para cada um na interface, ou passe o tipo do evento como parâmetro:
`void notificar(String evento, Produto produto);`

### 5.5.2 Observer concreto (`observer/LogProdutoObserver.java`)

```java
package insper.edu.br.provaintermediaria.observer;

import org.springframework.stereotype.Component;

import insper.edu.br.provaintermediaria.model.Produto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LogProdutoObserver implements ProdutoObserver {

    @Override
    public void notificar(Produto produto) {
        log.info("Produto criado: id={}, nome={}", produto.getId(), produto.getNome());
    }
}
```

Se o observer precisar mexer no banco (ex: gravar um histórico), é só injetar
o repository nele com `@RequiredArgsConstructor` + `private final ...Repository`.

### 5.5.3 Service notificando (`service/ProdutoService.java`)

Acrescente a lista e chame os observers **depois** de salvar:

```java
@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final List<ProdutoObserver> observers;   // Spring injeta todos os @Component

    public Produto criar(Produto produto) {
        Produto salvo = produtoRepository.save(produto);
        for (ProdutoObserver observer : observers) {
            observer.notificar(salvo);
        }
        return salvo;
    }

    // ... resto igual
}
```

Import novo: `insper.edu.br.provaintermediaria.observer.ProdutoObserver`.

Tenha **pelo menos um** `@Component` implementando a interface, senão o Spring
pode não conseguir montar o service.

### 5.5.4 Testes do service com observer

O `@InjectMocks` não sabe montar uma `List` de mocks. Então, nos testes, crie o
service **na mão** num `@BeforeEach`, trocando o `@InjectMocks`:

```java
@ExtendWith(MockitoExtension.class)
class ProdutoServiceTests {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private ProdutoObserver observer;

    private ProdutoService produtoService;

    @BeforeEach
    void setUp() {
        produtoService = new ProdutoService(produtoRepository, List.of(observer));
    }

    @Test
    void deveNotificarObserversAoCriar() {
        Produto entrada = produto(null, "Caneta", 2.5);
        Produto salvo = produto(1L, "Caneta", 2.5);
        Mockito.when(produtoRepository.save(entrada)).thenReturn(salvo);

        produtoService.criar(entrada);

        Mockito.verify(observer).notificar(salvo);   // o observer recebeu o produto salvo
    }

    @Test
    void naoDeveNotificarQuandoAtualizacaoFalha() {
        Mockito.when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(RecursoNaoEncontradoException.class,
                () -> produtoService.atualizar(99L, produto(null, "Lapis", 1.0)));
        Mockito.verifyNoInteractions(observer);      // em caso de erro, ninguém é avisado
    }

    // ... demais testes iguais aos da Parte 4
}
```

Imports novos: `org.junit.jupiter.api.BeforeEach` e
`insper.edu.br.provaintermediaria.observer.ProdutoObserver`
(remova o `org.mockito.InjectMocks`).

**Cobertura:** o `for` tem 2 ramos (entra no laço / sai do laço). Um teste com
um observer na lista já cobre os dois.

### 5.5.5 Teste do observer concreto (opcional)

Não conta para a regra de 100%, mas é rápido:

```java
class LogProdutoObserverTests {

    @Test
    void deveNotificarSemErro() {
        Produto p = new Produto();
        p.setId(1L);
        p.setNome("Caneta");

        Assertions.assertDoesNotThrow(() -> new LogProdutoObserver().notificar(p));
    }
}
```

Se o observer grava no banco, teste com um repository mockado e
`Mockito.verify(repository).save(...)`.

---

## Parte 6: rodar tudo e conferir a cobertura

```powershell
.\mvnw.cmd clean verify
```

- `BUILD SUCCESS` → testes passando **e** service com 100% de linhas e ramos.
- Se a cobertura falhar, aparece algo como:
  `Rule violated for class ...service.ProdutoService: branches covered ratio is 0.75, but expected minimum is 1.00`

Abra o relatório para ver exatamente o que falta:

```powershell
start target\site\jacoco\index.html
```

Clique no pacote `service` → na classe → as linhas aparecem coloridas:

- **verde**: coberta;
- **amarela**: ramo parcial (passe o mouse no losango para ver quantos ramos faltam);
- **vermelha**: nunca executada.

Crie o teste que falta e rode `clean verify` de novo.

> `.\mvnw.cmd test` só roda os testes. A checagem de 100% só acontece no
> `verify`. Antes de cada commit, rode sempre `clean verify`.

### Teste manual (opcional)

```powershell
docker compose up -d postgres
.\mvnw.cmd spring-boot:run
```

Em outro PowerShell:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/produtos -ContentType "application/json" -Body '{"nome":"Caneta","preco":2.5}'
curl.exe -i http://localhost:8080/produtos
curl.exe -i http://localhost:8080/produtos/1
curl.exe -i -X DELETE http://localhost:8080/produtos/1
```

Pare a aplicação com `Ctrl + C`.

---

## Parte 7: entregar a primeira rota via Pull Request

```powershell
.\mvnw.cmd clean verify
git status
git add .
git commit -m "Implementa rota de produtos"
git push -u origin feature/primeira-rota
```

No GitHub (`github.com/bisaraujo/provaintermediariaprojsoft`):

1. Clique em **Compare & pull request** (ou **Pull requests → New pull request**).
2. Base: `main` ← compare: `feature/primeira-rota`. Escreva um título e crie.
3. Espere o check **CI** ficar verde ✅ dentro do PR.
4. Clique em **Merge pull request → Confirm merge**.

O merge na `main` dispara o **Build and Deploy** automaticamente.

---

## Parte 8: próximas rotas

Para cada rota nova, repita:

```powershell
git switch main
git pull
git switch -c feature/nome-da-rota
# implementa + testa (Partes 3 a 6)
.\mvnw.cmd clean verify
git add .
git commit -m "Implementa <rota>"
git push -u origin feature/nome-da-rota
# abre PR → CI verde → merge
```

Fazer todas as rotas por PR é o caminho mais seguro. Se o tempo apertar, dá
pra commitar direto na `main` depois da primeira rota (`git switch main`,
`git pull`, implementa, `git push`), mas só se o professor permitir.

---

## Parte 9: conferir o deploy

1. **Actions → Build and Deploy** tem que estar verde ✅.
2. Teste a API publicada:

```powershell
curl.exe -i http://44.192.87.24:8082/produtos
```

ou abra `http://44.192.87.24:8082/produtos` no navegador.

---

## Checklist final antes de entregar

- [ ] todas as rotas, verbos e caminhos exatamente como no enunciado
- [ ] status HTTP corretos (201, 200, 204, 400, 404...)
- [ ] teste unitário para **cada caminho** de cada método do service
- [ ] teste de integração para **cada rota** (sucesso + erro principal)
- [ ] `.\mvnw.cmd clean verify` → `BUILD SUCCESS`
- [ ] pelo menos uma rota entregue via Pull Request com CI verde
- [ ] Build and Deploy verde depois do merge na `main`
- [ ] API respondendo em `http://44.192.87.24:8082`
- [ ] nenhum `.env`, senha ou chave no Git (`git status` antes de cada commit)

---

## Se der erro

| Erro | Causa / solução |
|---|---|
| `Could not find a valid Docker environment` | Docker Desktop fechado. Abra e rode de novo. |
| `Rule violated for class ...service...` | Falta teste para alguma linha/ramo. Veja o relatório JaCoCo (Parte 6). |
| `detected dubious ownership` no git | `git config --global --add safe.directory C:/Users/gabis/OneDrive/Documentos/4semestre/projsoft/conteudo_pi/provaintermediaria` |
| `./mvnw: Permission denied` no Actions | `git update-index --chmod=+x mvnw`, commit e push. |
| `Port 8080 was already in use` (local) | Outra instância rodando: feche o outro terminal ou `docker compose ps`. |
| Teste de controller retorna 500 em vez de 404 | A exceção lançada não tem `@ResponseStatus(HttpStatus.NOT_FOUND)`. |
| `push rejected (fetch first)` | `git pull --rebase` e depois `git push` de novo. |
| Deploy vermelho no passo SSH | Abra o log do passo e verifique as secrets `AWS_*`. |
