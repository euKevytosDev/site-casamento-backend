-- Banco: casamento | Schema: public
-- Execute no DBeaver se preferir criar a tabela manualmente.
-- Com ddl-auto=update, o Spring Boot também cria/atualiza automaticamente.

CREATE TABLE IF NOT EXISTS presente_casamento (
    id              SERIAL PRIMARY KEY,
    nome            VARCHAR(255)    NOT NULL,
    descricao       TEXT,
    valor           NUMERIC(10, 2)  NOT NULL,
    imagem          VARCHAR(500)    NOT NULL,  -- URL Cloudinary (https://res.cloudinary.com/...)
    comprado        BOOLEAN         NOT NULL DEFAULT FALSE,
    nome_comprador  VARCHAR(255),
    data_cadastro   TIMESTAMP       NOT NULL DEFAULT NOW()
);
