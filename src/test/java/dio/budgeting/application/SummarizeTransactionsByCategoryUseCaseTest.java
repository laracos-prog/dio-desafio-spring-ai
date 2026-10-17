package dio.budgeting.application;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.support.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SummarizeTransactionsByCategoryUseCaseTest {
    private InMemoryTransactionRepository repository;
    private SummarizeTransactionsByCategoryUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        useCase = new SummarizeTransactionsByCategoryUseCase(repository);
    }

    @Test
    void should_sumOnlyTransactionsOfTheRequestedCategory() {
        repository.save(new Transaction("Mercado do bairro", 5000, Category.GROCERIES));
        repository.save(new Transaction("Padaria", 1250, Category.GROCERIES));
        repository.save(new Transaction("Farmácia", 3990, Category.PHARMA));

        var summary = useCase.execute(Category.GROCERIES);

        assertThat(summary.category()).isEqualTo("GROCERIES");
        assertThat(summary.transactionCount()).isEqualTo(2);
        assertThat(summary.total()).isEqualTo(62.50);
    }

    @Test
    void should_returnZero_when_categoryHasNoTransactions() {
        var summary = useCase.execute(Category.AUTO);

        assertThat(summary.transactionCount()).isZero();
        assertThat(summary.total()).isZero();
    }
}
