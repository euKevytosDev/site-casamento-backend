-- Tabela de administradores (criada automaticamente pelo Hibernate)
CREATE TABLE IF NOT EXISTS public.admin_usuario (
    id          SERIAL PRIMARY KEY,
    login       VARCHAR(100) NOT NULL UNIQUE,
    senha_hash  VARCHAR(255) NOT NULL
);

-- Usuário admin criado na 1ª execução se a tabela estiver vazia.
-- Configure ADMIN_LOGIN e ADMIN_PASSWORD no Render (mín. 8 caracteres na senha).
