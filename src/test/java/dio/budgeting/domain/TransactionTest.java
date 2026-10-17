package dio.budgeting.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Test
    void should_createTransaction_when_dataIsValid() {
        var transaction = new Transaction("  Compra no mercado  ", 5000, Category.GROCERIES);

        assertThat(transaction.getId()).isNotNull();
        assertThat(transaction.getDescription()).isEqualTo("Compra no mercado");
        assertThat(transaction.getAmount()).isEqualTo(5000);
        assertThat(transaction.getCategory()).isEqualTo(Category.GROCERIES);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void should_reject_when_descriptionIsBlank(String description) {
        assertThatThrownBy(() -> new Transaction(description, 5000, Category.GROCERIES))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("descrição");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -5000})
    void should_reject_when_amountIsNotPositive(long amount) {
        assertThatThrownBy(() -> new Transaction("Farmácia", amount, Category.PHARMA))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("maior que zero");
    }

    @Test
    void should_reject_when_categoryIsMissing() {
        assertThatThrownBy(() -> new Transaction("Gasolina", 15000, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("categoria");
    }
}
