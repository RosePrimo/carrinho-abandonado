package com.rose.carrinho;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class CarrinhoApplicationTests {

	@Test
	void contextLoads() {
	}
}