-- Executar uma vez no Neon (ddl-auto=none não cria colunas novas)
ALTER TABLE site ADD COLUMN IF NOT EXISTS fonte_nomes VARCHAR(40);
