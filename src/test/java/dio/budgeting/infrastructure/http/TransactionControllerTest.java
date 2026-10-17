package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeTransactionsByCategoryUseCase;
import dio.budgeting.support.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa os endpoints REST sem banco de dados e sem OpenAI:
 * os modelos de IA são substituídos por "mocks" (objetos falsos do Mockito).
 */
class TransactionControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        var repository = new InMemoryTransactionRepository();
        var controller = new TransactionController(
                new PersistTransactionUseCase(repository),
                new ListTransactionsByCategoryUseCase(repository),
                new SummarizeTransactionsByCategoryUseCase(repository),
                mock(TranscriptionModel.class),
                new ByteArrayResource("prompt de teste".getBytes()),
                mock(ChatClient.Builder.class, RETURNS_SELF),
                mock(TextToSpeechModel.class));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void should_createTransactionAndShowValueInReais() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Mercado", "category": "GROCERIES", "amount": 5000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(50.0));
    }

    @Test
    void should_return400_when_amountIsZero() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Mercado", "category": "GROCERIES", "amount": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O valor da transação deve ser maior que zero."));
    }

    @Test
    void should_returnCategorySummary() throws Exception {
        for (var amount : new int[]{5000, 1250}) {
            mockMvc.perform(post("/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"description\": \"Mercado\", \"category\": \"GROCERIES\", \"amount\": " + amount + "}"));
        }

        mockMvc.perform(get("/transactions/GROCERIES/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("GROCERIES"))
                .andExpect(jsonPath("$.transactionCount").value(2))
                .andExpect(jsonPath("$.total").value(62.5));
    }
}
