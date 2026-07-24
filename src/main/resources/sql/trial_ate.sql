-- Trial gratuito da assinatura (fim do período de teste)
ALTER TABLE site ADD COLUMN IF NOT EXISTS trial_ate DATE;
