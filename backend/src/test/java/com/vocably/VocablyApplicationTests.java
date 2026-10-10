package com.vocably;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class VocablyApplicationTests {

    @Test
    void contextLoads() {
        // Passa se a aplicação sobe, conecta no Postgres e o Flyway roda as migrations.
    }
}
