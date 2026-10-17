package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.InvalidTransactionException;
import dio.budgeting.support.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersistTransactionUseCaseTest {
    private InMemoryTransactionRepository repository;
    private PersistTransactionUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        useCase = new PersistTransactionUseCase(repository);
    }

    @Test
    void should_saveAndReturnValueInReais_when_inputIsValid() {
        var output = useCase.execute(new PersistTransactionInput("Remédio", 3990, Category.PHARMA));

        assertThat(repository.size()).isEqualTo(1);
        assertThat(output.description()).isEqualTo("Remédio");
        assertThat(output.category()).isEqualTo("PHARMA");
        assertThat(output.value()).isEqualTo(39.90);
    }

    @Test
    void should_notSave_when_amountIsZero() {
        assertThatThrownBy(() -> useCase.execute(new PersistTransactionInput("Remédio", 0, Category.PHARMA)))
                .isInstanceOf(InvalidTransactionException.class);

        assertThat(repository.size()).isZero();
    }
}
