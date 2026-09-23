# Checklist curto para amanha

O enunciado de 14/09 exige API, PostgreSQL, deploy automatico, secrets, 100% de
cobertura no service, teste de integracao das rotas e uma rota feita via Pull
Request. Use esta ordem para nao esquecer nenhum item.

## Antes da prova

1. Abra o Docker Desktop.
2. Entre nesta pasta pelo terminal.
3. Confirme que o banco local esta disponivel:

```powershell
docker compose up -d postgres
docker compose ps
```

4. Confirme a base:

```powershell
.\mvnw.cmd clean verify
```

## Quando receber o enunciado

Crie uma branch antes de implementar. Isso permite que uma rota seja entregue
por Pull Request, como no exercicio de referencia:

```powershell
git switch -c feature/primeira-rota
```

Implemente na ordem:

1. entity/model;
2. repository;
3. service;
4. controller;
5. testes unitarios do service;
6. testes de integracao das rotas.

Depois de cada parte importante:

```powershell
.\mvnw.cmd test
```

Antes do commit:

```powershell
.\mvnw.cmd clean verify
```

O `verify` falha automaticamente se alguma classe do pacote `service` ficar
com menos de 100% das linhas cobertas. O relatorio detalhado fica em
`target/site/jacoco/index.html`.

## Pull Request da primeira rota

Quando a primeira rota e seus testes estiverem funcionando:

```powershell
git add .
git commit -m "Implementa primeira rota"
git push -u origin feature/primeira-rota
```

No GitHub, abra um Pull Request para `main`. Espere o workflow **CI** ficar
verde antes de fazer o merge. Depois, continue as demais rotas em outra branch
ou conforme a orientacao do professor.

## Verificacao final

```powershell
.\mvnw.cmd clean verify
docker build -t prova-intermediaria:local .
git status
```

Confira:

- todas as rotas e status HTTP pedidos;
- teste unitario de todos os caminhos do service;
- teste de integracao para cada rota;
- cobertura do service em 100%;
- nenhum segredo ou `.env` versionado;
- CI verde no Pull Request;
- deploy verde depois do merge em `main`.

