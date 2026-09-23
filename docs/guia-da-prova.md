# Guia de implementacao durante a prova

Esta base cuida apenas da infraestrutura que independe do enunciado. Nao crie
entidades, rotas ou regras de negocio antes de identificar exatamente o que foi
pedido.

## 1. Traduzir o enunciado

Antes de programar, marque no texto:

1. substantivos que viram entidades (por exemplo, `Pedido`);
2. campos, tipos e restricoes de cada entidade;
3. operacoes pedidas (`POST`, `GET`, `PUT/PATCH`, `DELETE`);
4. filtros de consulta;
5. regras de negocio e casos de erro;
6. codigos HTTP exigidos;
7. comportamento de exclusao: fisica ou logica.

Monte uma pequena tabela no papel: rota, entrada, regra, saida e status HTTP.
Isso evita implementar algo correto tecnicamente, mas diferente do enunciado.

## 2. Criar as camadas nesta ordem

1. **Entity/model**: campos, `@Entity`, `@Id` e validacoes (`@NotBlank`,
   `@NotNull`, etc.).
2. **Repository**: estenda `JpaRepository<Entidade, TipoDoId>` e declare apenas
   as consultas necessarias.
3. **Service**: concentre regras de negocio e erros aqui. Use injecao pelo
   construtor.
4. **Controller**: traduza HTTP para chamadas do service; use `@Valid` quando a
   entrada possuir Bean Validation.
5. **Tratamento de erro**, se solicitado: excecao propria e
   `@RestControllerAdvice`.
6. **Testes do service**: um teste para cada caminho da regra.
7. **Testes do controller**: um teste para cada endpoint e status relevante.

Fluxo esperado:

```text
HTTP -> Controller -> Service -> Repository -> PostgreSQL
```

O controller nao deve conter regra de negocio. O service nao deve conhecer
`MockMvc`, JSON ou status HTTP. O repository nao deve decidir regras.

## 3. Checklist por endpoint

Para cada endpoint, confirme:

- caminho e verbo HTTP corretos;
- corpo ou parametros exatamente como pedidos;
- status de sucesso correto;
- validacao de entrada;
- caso de recurso inexistente;
- persistencia/consulta correta;
- teste de sucesso;
- teste do principal caminho alternativo ou de erro.

## 4. Comandos de verificacao

Execute frequentemente:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

Antes de entregar:

```powershell
docker build -t prova-intermediaria:local .
docker compose ps
```

Abra `target/site/jacoco/index.html` e procure metodos ou ramos vermelhos. Nao
escreva testes apenas para aumentar uma porcentagem: cubra os comportamentos
que o enunciado permite observar.

