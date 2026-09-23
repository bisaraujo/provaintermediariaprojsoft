# Prova intermediaria

Base generica para uma API REST com Spring Boot 4, Java 17, PostgreSQL, Docker,
testes automatizados e GitHub Actions. O dominio da prova deve ser implementado
somente depois da leitura do enunciado.

## Antes de comecar

- Java 17
- Docker Desktop aberto
- Git (quando o repositorio remoto for conectado)

## Banco local

Suba apenas o PostgreSQL:

```powershell
docker compose up -d postgres
docker compose ps
```

As configuracoes padrao sao:

- banco: `prova_intermediaria`
- usuario: `postgres`
- senha: `postgres`
- porta: `5432`

Esses valores existem apenas para desenvolvimento local. Para personaliza-los,
copie `.env.example` para `.env`; o arquivo `.env` nao entra no Git.

## Executar a aplicacao

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

A aplicacao usa `http://localhost:8080`. Para parar o banco:

```powershell
docker compose down
```

Use `docker compose down -v` somente se quiser apagar tambem os dados locais.

## Testes e cobertura

```powershell
.\mvnw.cmd clean verify
```

O relatorio do JaCoCo fica em `target/site/jacoco/index.html`. Os testes de
integracao usam um PostgreSQL temporario com Testcontainers, portanto precisam
do Docker Desktop. Leia [docs/testes.md](docs/testes.md) antes de implementar o dominio.

## Documentacao

Os guias ficam na pasta [`docs/`](docs/):

| Guia | Para que serve |
|---|---|
| [checklist-da-prova.md](docs/checklist-da-prova.md) | sequencia curta de comandos para o dia da prova |
| [guia-da-prova.md](docs/guia-da-prova.md) | ordem pratica para implementar a API a partir do enunciado |
| [testes.md](docs/testes.md) | explicacao e modelos de Mockito e MockMvc |
| [github-e-deploy.md](docs/github-e-deploy.md) | secrets, Docker Hub, VM e deploy automatico |

## Estrutura do projeto

```text
.github/workflows/   CI (testes + cobertura) e deploy automatico
docs/                guias de apoio
src/main/            codigo da API
src/test/            testes unitarios e de integracao
compose.yaml         PostgreSQL local
Dockerfile           imagem da API
```
