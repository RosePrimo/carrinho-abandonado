-- V2: índice na chave estrangeira dos itens.
-- O PostgreSQL NÃO cria índice sozinho nas chaves estrangeiras.
-- Toda vez que o Hibernate busca os itens de um carrinho (where carrinho_id = ?),
-- sem índice ele teria que ler a tabela inteira.

create index idx_item_carrinho_carrinho on item_carrinho (carrinho_id);