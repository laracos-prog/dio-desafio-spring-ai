package dio.budgeting.domain;

/**
 * Lançada quando alguém tenta criar uma transação com dados inválidos
 * (descrição vazia, valor menor ou igual a zero ou categoria ausente).
 */
public class InvalidTransactionException extends IllegalArgumentException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}
