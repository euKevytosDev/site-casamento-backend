-- Prazo para confirmação de presença (RSVP)
ALTER TABLE site ADD COLUMN IF NOT EXISTS data_limite_confirmacao DATE;
