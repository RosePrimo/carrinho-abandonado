package com.rose.carrinho.repository;

import com.rose.carrinho.model.Lembrete;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LembreteRepository extends JpaRepository<Lembrete, Long> {
}