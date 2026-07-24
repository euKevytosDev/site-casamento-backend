# Site de Casamento — Backend

API REST do site de casamento da Rafaella e do Kevin (e da marca Loven). Cuida de confirmação de presença, lista de presentes, login do admin e upload de fotos.

Fiz esse back-end porque o front sozinho não dava conta: precisava persistir dados, proteger o painel e servir vários sites de noivas no mesmo domínio.

## Em produção

- API: https://site-casamento-backend-nrfb.onrender.com
- Front (exemplo): https://rafaekevin.com.br/
- Repositório do front: [site-casamento](https://github.com/euKevytosDev/site-casamento)

## O que a API faz

- Confirmação de presença (pessoa sozinha ou família inteira)
- CRUD de presentes, com imagem no Cloudinary
- Reserva/compra de presente pelo convidado
- Login do admin com JWT
- Configuração por site (multi-tenant leve, via header/slug)

Na subida, a aplicação valida se as variáveis de ambiente obrigatórias existem — evita subir “mudo” e só descobrir o erro na primeira requisição.

## Stack

- Java 17 + Spring Boot 3
- Spring Data JPA
- PostgreSQL no Neon
- JWT no painel admin
- Cloudinary para imagens
- Maven + Docker
- Deploy na Render

## Organização do código

```text
src/main/java/com/casamento/backend/
├── controller/   # endpoints
├── model/        # entidades JPA
├── repository/
├── service/      # regras de negócio
├── config/       # segurança, CORS, Cloudinary
└── dto/
```

Separação clássica de camadas. A lógica fica no service; controller só recebe e responde.

## Endpoints principais

**Público**

| Método | Rota | O que faz |
|--------|------|-----------|
| GET | `/api/presenca` | Lista confirmações |
| POST | `/api/presenca/confirmar-familia` | Confirma família |
| GET | `/api/presentes` | Lista presentes |
| POST | `/api/presentes/{id}/comprar` | Reserva presente |
| GET | `/api/health` | Health check (sem bater no banco) |

**Admin (JWT)**

| Método | Rota | O que faz |
|--------|------|-----------|
| POST | `/api/auth/login` | Login |
| GET/POST/PUT/DELETE | `/api/admin/presentes` | Gerencia presentes |
| GET/DELETE | `/api/admin/presenca` | Gerencia presenças |

Tem um `teste.http` na raiz pra ir batendo nos endpoints no VS Code/Cursor.

## Rodar local

Precisa de Java 17+, Maven (ou `./mvnw`) e um PostgreSQL (Neon ou local).

```bash
git clone https://github.com/euKevytosDev/site-casamento-backend.git
cd site-casamento-backend

cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
# preenche DB, JWT, admin e Cloudinary

./mvnw spring-boot:run
```

API em `http://localhost:8080`.

### Variáveis importantes

```env
DB_URL=jdbc:postgresql://HOST/neondb?sslmode=require
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=chave_com_pelo_menos_32_caracteres
ADMIN_LOGIN=admin
ADMIN_PASSWORD=senha_forte
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
```

Referência completa: `render.env.example`.

## Deploy (Render)

Web Service com Docker. As envs vão no painel da Render; o `PORT` ela injeta sozinha.

### UptimeRobot

No free tier da Render o serviço dorme. O monitor deve bater só no health:

```
GET https://site-casamento-backend-nrfb.onrender.com/api/health
```

Resposta: `{"status":"ok","db":"skipped"}`

Intervalo ~14 min. Não use `/api/presenca` ou `/api/site/config` no monitor — esses mexem no Neon e gastam compute à toa.

## Banco

PostgreSQL serverless no [Neon](https://neon.tech). Tabelas sobem com `ddl-auto=update`. Tem SQL de referência em `src/main/resources/sql/` se precisar montar algo na mão.

## Autor

Raian Kevin — Full Stack

- GitHub: [@euKevytosDev](https://github.com/euKevytosDev)
- Portfólio: [portfolio-raian](https://github.com/euKevytosDev/portfolio-raian)
