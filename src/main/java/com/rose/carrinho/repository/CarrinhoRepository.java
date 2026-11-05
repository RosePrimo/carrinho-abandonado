package com.rose.carrinho.repository;

import com.rose.carrinho.model.Carrinho;
import com.rose.carrinho.model.StatusCarrinho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CarrinhoRepository extends JpaRepository<Carrinho, Long> {

    // o Spring cria o SQL só pelo nome do método:
    // SELECT * FROM carrinho WHERE cliente_id = ? AND status = ?
    Optional<Carrinho> findByClienteIdAndStatus(String clienteId, StatusCarrinho status);

    // SELECT ... WHERE status = ? AND atualizado_em < ? ORDER BY atualizado_em LIMIT 100
    List<Carrinho> findTop100ByStatusAndAtualizadoEmBeforeOrderByAtualizadoEmAsc(StatusCarrinho status, Instant limite);

    @Query(value = """
            select * from carrinho
            where status = 'ATIVO' and atualizado_em < :limite
            order by atualizado_em
            limit 100
            for update skip locked
            """, nativeQuery = true)
    List<Carrinho> travarParadosParaAbandono(@Param("limite") Instant limite);
}
