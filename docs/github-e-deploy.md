# GitHub Actions, Docker Hub e servidor

Ha dois workflows:

- `ci.yml`: roda testes e publica o relatorio JaCoCo como artefato em todo push
  e pull request;
- `deploy.yml`: em push na branch `main` (ou manualmente), testa, publica a
  imagem no Docker Hub e atualiza o container no servidor por SSH.

## Secrets do repositorio

No GitHub: **Settings > Secrets and variables > Actions > New repository
secret**. Cadastre:

| Secret | Conteudo |
|---|---|
| `DOCKER_USERNAME` | usuario do Docker Hub |
| `DOCKER_TOKEN` | access token do Docker Hub, nao a senha da conta |
| `AWS_HOST` | IP ou DNS publico da maquina virtual |
| `AWS_USER` | usuario SSH, por exemplo `ubuntu` |
| `AWS_SSH_KEY` | conteudo completo da chave privada SSH |
| `DB_URL` | URL interna, por exemplo `jdbc:postgresql://postgres:5432/prova_intermediaria` |
| `DB_USERNAME` | usuario do PostgreSQL de producao |
| `DB_PASSWORD` | senha do PostgreSQL de producao |

Em **Variables**, a variavel opcional `DOCKER_IMAGE_NAME` muda o nome da imagem.
Se ela nao existir, o workflow usa `provaintermediariaprojsoft`, correspondente
ao repositorio `saraujobi/provaintermediariaprojsoft` criado no Docker Hub.

Crie tambem a variavel opcional `APP_PORT` se a API precisar ser publicada em
outra porta. O valor padrao e `8082` (a 8080 ja esta ocupada na VM pelo container `pagamento`).

Nunca coloque valores reais em `application.properties`, `.env.example`, YAML,
commits ou prints. O `.env` local ja esta ignorado pelo Git.

## Preparacao do servidor

O servidor precisa ter Docker instalado e aceitar SSH pela chave cadastrada. A
porta TCP definida em `APP_PORT` deve ser liberada apenas conforme a arquitetura
pedida pelo professor.

Assim como no projeto `curso`, o workflow pressupoe que estes recursos ja foram
preparados uma vez na VM:

- a rede Docker `prova-network`;
- o volume persistente `prova-postgres-data`;
- o container de PostgreSQL conectado a essa rede com o alias `postgres`.

A cada deploy, apenas o container `prova-intermediaria` e substituido. A API usa
`postgres` como hostname interno. O PostgreSQL nao precisa publicar sua porta na
VM; somente a API recebe uma porta publica. Os dados sobrevivem aos deploys
porque ficam no volume Docker.

Importante: o usuario, senha e nome do banco usados ao criar o PostgreSQL devem
ser exatamente os mesmos das secrets. Trocar uma secret depois que o banco ja
possui dados nao muda automaticamente a senha existente.

### Configuracao unica do PostgreSQL na VM

Conecte por SSH e crie um arquivo protegido para as variaveis do banco:

```bash
sudo mkdir -p /opt/prova-intermediaria
sudo nano /opt/prova-intermediaria/postgres.env
```

Conteudo do arquivo (substitua pelos valores reais):

```dotenv
POSTGRES_DB=prova_intermediaria
POSTGRES_USER=usuario_do_banco
POSTGRES_PASSWORD=senha_forte
```

Proteja o arquivo e crie os recursos Docker:

```bash
sudo chown $USER:$USER /opt/prova-intermediaria/postgres.env
chmod 600 /opt/prova-intermediaria/postgres.env

docker network inspect prova-network >/dev/null 2>&1 || docker network create prova-network
docker volume inspect prova-postgres-data >/dev/null 2>&1 || docker volume create prova-postgres-data

docker run -d \
  --name prova-postgres \
  --restart unless-stopped \
  --network prova-network \
  --network-alias postgres \
  --env-file /opt/prova-intermediaria/postgres.env \
  -v prova-postgres-data:/var/lib/postgresql/data \
  postgres:16-alpine
```

Nao use `-p 5432:5432`: a API acessa o banco pela rede Docker interna, portanto
nao e necessario expor o PostgreSQL para a internet.

Confirme o funcionamento:

```bash
docker ps
docker exec prova-postgres pg_isready -U usuario_do_banco -d prova_intermediaria
```

Com os valores do exemplo, as secrets correspondentes seriam:

```text
DB_URL=jdbc:postgresql://postgres:5432/prova_intermediaria
DB_USERNAME=usuario_do_banco
DB_PASSWORD=senha_forte
```

Os valores reais de `POSTGRES_USER` e `POSTGRES_PASSWORD` nao devem ser enviados
para o Git nem escritos diretamente no workflow.

## Observacoes da configuracao real

- A VM compartilhada ja usa as portas 8080 (`pagamento`) e 8081 (`reservas`);
  por isso a API da prova e publicada na **8082**.
- O `chown` no `postgres.env` e necessario porque o `docker run` roda sem sudo
  e precisa ler o arquivo.
- No Windows, o `mvnw` precisa ser marcado como executavel no Git
  (`git update-index --chmod=+x mvnw`), senao o GitHub Actions falha com
  `Permission denied`.

## Antes de ativar o deploy

1. conectar esta pasta ao repositorio remoto;
2. confirmar se a branch principal e `main`;
3. criar repositorio/token no Docker Hub;
4. cadastrar secrets e, se desejado, `DOCKER_IMAGE_NAME`;
5. preparar a VM e testar SSH;
6. conferir a porta publica em `APP_PORT` (padrao 8082);
7. disparar manualmente `Build and Deploy` uma vez e verificar os logs.

