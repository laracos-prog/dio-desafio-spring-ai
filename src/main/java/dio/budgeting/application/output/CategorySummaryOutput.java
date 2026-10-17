package dio.budgeting.application.output;

/**
 * Resumo dos gastos de uma categoria.
 *
 * @param category         categoria consultada
 * @param transactionCount quantidade de transações encontradas
 * @param total            soma dos valores, em reais
 */
public record CategorySummaryOutput(String category, int transactionCount, double total) {
}
