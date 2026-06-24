-- Tabela de administradores (criada automaticamente pelo Hibernate)
CREATE TABLE IF NOT EXISTS public.admin_usuario (
    id          SERIAL PRIMARY KEY,
    login       VARCHAR(100) NOT NULL UNIQUE,
    senha_hash  VARCHAR(255) NOT NULL
);

-- Usuário padrão criado na 1ª execução: admin / casamento2027
-- Altere via variáveis ADMIN_LOGIN e ADMIN_PASSWORD no Render
