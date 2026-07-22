-- Dress code editável pela noiva
ALTER TABLE site ADD COLUMN IF NOT EXISTS dresscode_traje VARCHAR(80);
ALTER TABLE site ADD COLUMN IF NOT EXISTS dresscode_texto VARCHAR(800);
ALTER TABLE site ADD COLUMN IF NOT EXISTS dresscode_cores VARCHAR(800);
ALTER TABLE site ADD COLUMN IF NOT EXISTS dresscode_rodape VARCHAR(200);
