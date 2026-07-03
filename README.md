# Site de Casamento — Backend (API REST)

API REST desenvolvida em **Java + Spring Boot** para o site de casamento de Rafaella & Kevin. Gerencia confirmações de presença, lista de presentes, autenticação de administradores e upload de imagens.

## Links

- API em produção: https://site-casamento-backend-nrfb.onrender.com
- Front-end: https://eukevytosdev.github.io/site-casamento/
- Repositório front: [site-casamento](https://github.com/euKevytosDev/site-casamento)

## Sobre o projeto

Back-end completo com persistência em **PostgreSQL** (Neon), autenticação **JWT** para o painel admin, upload de imagens via **Cloudinary** e deploy automatizado na **Render** com Docker.

## Stack

| Tecnologia | Uso |
|---|---|
| Java 17 | Linguagem principal |
| Spring Boot 3 | Framework da API |
| Spring Data JPA | Persistência no banco |
| PostgreSQL (Neon) | Banco de dados |
| JWT | Autenticação do admin |
| Cloudinary | Armazenamento de imagens |
| Maven | Gerenciamento de dependências |
| Docker + Render | Deploy em produção |

## Funcionalidades

- Confirmação de presença (individual e em família)
- CRUD de presentes com upload de imagem
- Reserva/compra de presentes pelos convidados
- Painel admin protegido por login JWT
- Validação de variáveis de ambiente na inicialização

## Estrutura do projeto

```text
site-casamento-backend/
├── src/main/java/com/casamento/backend/
│   ├── controller/     # Endpoints da API
│   ├── model/          # Entidades JPA
│   ├── repository/     # Acesso ao banco
│   ├── service/        # Regras de negócio
│   ├── config/         # Segurança, CORS, Cloudinary
│   └── dto/            # Objetos de transferência
├── src/main/resources/
│   ├── application.properties
│   ├── application-local.properties.example
│   └── sql/            # Scripts de referência
├── Dockerfile
├── render.env.example
└── teste.http          # Testes manuais da API
```

## Endpoints principais

### Públicos (convidados)

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/presenca` | Listar confirmações |
| POST | `/api/presenca/confirmar-familia` | Confirmar família |
| GET | `/api/presentes` | Listar presentes disponíveis |
| POST | `/api/presentes/{id}/comprar` | Reservar presente |

### Admin (requer JWT)

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/login` | Login do administrador |
| GET/POST/PUT/DELETE | `/api/admin/presentes` | Gerenciar presentes |
| GET/DELETE | `/api/admin/presenca` | Gerenciar presenças |

## Variáveis de ambiente

Copie `application-local.properties.example` → `application-local.properties` (não versionado) ou configure no Render:

```env
DB_URL=jdbc:postgresql://HOST/neondb?sslmode=require
DB_USERNAME=seu_usuario
DB_PASSWORD=sua_senha

JWT_SECRET=chave_com_pelo_menos_32_caracteres
ADMIN_LOGIN=admin
ADMIN_PASSWORD=senha_forte_8_ou_mais

CLOUDINARY_CLOUD_NAME=seu_cloud
CLOUDINARY_API_KEY=sua_key
CLOUDINARY_API_SECRET=seu_secret
CLOUDINARY_FOLDER=presentes-casamento
```

Veja também: `render.env.example`

## Como rodar localmente

### Pré-requisitos
- Java 17+
- Maven (ou use `./mvnw`)
- Banco PostgreSQL (Neon ou local)

### Passos

1. Clone o repositório:
   ```bash
   git clone https://github.com/euKevytosDev/site-casamento-backend.git
   cd site-casamento-backend
   ```

2. Configure as variáveis:
   ```bash
   cp src/main/resources/application-local.properties.example \
      src/main/resources/application-local.properties
   ```
   Preencha com seus dados reais.

3. Execute:
   ```bash
   ./mvnw spring-boot:run
   ```

4. API disponível em `http://localhost:8080`

5. Teste os endpoints com o arquivo `teste.http` (extensão REST Client no VS Code/Cursor).

## Deploy na Render

1. Conecte o repositório GitHub à Render
2. Tipo: **Web Service** com Docker
3. Configure todas as variáveis de ambiente
4. O `PORT` é injetado automaticamente pela Render

## Banco de dados

- Provider: [Neon](https://neon.tech) (PostgreSQL serverless)
- Tabelas criadas automaticamente via `spring.jpa.hibernate.ddl-auto=update`
- Scripts SQL de referência em `src/main/resources/sql/`

## Autor

**Raian Kevin** — Desenvolvedor Full Stack

- GitHub: [@euKevytosDev](https://github.com/euKevytosDev)
- Portfólio: [portfolio-raian](https://github.com/euKevytosDev/portfolio-raian)
