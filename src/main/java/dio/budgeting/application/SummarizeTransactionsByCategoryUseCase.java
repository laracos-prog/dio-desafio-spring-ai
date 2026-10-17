package dio.budgeting.application;

import dio.budgeting.application.output.CategorySummaryOutput;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Nova funcionalidade: responde "quanto eu gastei em X?".
 * É usada pelo endpoint REST e também exposta para a IA como ferramenta (Tool Calling).
 */
@Service
public class SummarizeTransactionsByCategoryUseCase {
    private final TransactionRepository transactionRepository;

    public SummarizeTransactionsByCategoryUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Tool(name = "summarize-transactions-by-category",
            description = "Calcula o total gasto e a quantidade de transações de uma categoria. "
                    + "Use quando a pessoa perguntar quanto gastou em uma categoria.")
    public CategorySummaryOutput execute(@ToolParam(description = "Categoria de uma transação") Category category) {
        var transactions = transactionRepository.findAllByCategory(category);
        long totalInCents = transactions.stream().mapToLong(Transaction::getAmount).sum();

        return new CategorySummaryOutput(
                category.name(),
                transactions.size(),
                TransactionOutput.centsToReais(totalInCents));
    }
}
