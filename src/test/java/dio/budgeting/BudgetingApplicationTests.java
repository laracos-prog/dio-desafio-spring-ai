package dio.budgeting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

// Sobe a aplicação inteira: precisa da chave da OpenAI e do MySQL (docker compose) rodando.
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class BudgetingApplicationTests {

    @Test
    void contextLoads() {
    }

}
