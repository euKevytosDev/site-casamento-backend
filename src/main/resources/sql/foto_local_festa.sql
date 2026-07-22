-- Foto do local da festa (quando cerimônia e festa são separados)
-- Executar no Neon (ddl-auto=none não cria colunas novas)
ALTER TABLE site ADD COLUMN IF NOT EXISTS foto_local_festa_url VARCHAR(500);
