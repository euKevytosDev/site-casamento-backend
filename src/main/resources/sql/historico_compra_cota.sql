-- Histórico de compras de cotas (quem comprou, quantas, valor, data)
-- Com ddl-auto=update, o Spring Boot também cria automaticamente.

CREATE TABLE IF NOT EXISTS historico_compra_cota (
    id              SERIAL PRIMARY KEY,
    presente_id     BIGINT,
    nome_presente   VARCHAR(255)    NOT NULL,
    nome_comprador  VARCHAR(255)    NOT NULL,
    quantidade      INTEGER         NOT NULL,
    valor_cota      NUMERIC(10, 2)  NOT NULL,
    valor_total     NUMERIC(10, 2)  NOT NULL,
    data_compra     TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Opcional: importar compras antigas que já existiam antes do histórico
-- INSERT INTO historico_compra_cota (presente_id, nome_presente, nome_comprador, quantidade, valor_cota, valor_total, data_compra)
-- SELECT id, nome, nome_comprador, cotas_vendidas, valor, valor * cotas_vendidas, COALESCE(data_cadastro, NOW())
-- FROM presente_casamento
-- WHERE cotas_vendidas > 0 AND nome_comprador IS NOT NULL;
