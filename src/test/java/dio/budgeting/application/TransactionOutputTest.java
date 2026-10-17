package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionOutputTest {

    @Test
    void should_convertCentsToReais() {
        var transaction = new Transaction("Mercado", 5000, Category.GROCERIES);

        assertThat(TransactionOutput.from(transaction).value()).isEqualTo(50.00);
    }

    @Test
    void should_keepCentsPrecision() {
        assertThat(TransactionOutput.centsToReais(1)).isEqualTo(0.01);
        assertThat(TransactionOutput.centsToReais(12345)).isEqualTo(123.45);
    }
}
