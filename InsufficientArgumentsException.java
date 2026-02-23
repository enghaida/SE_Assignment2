/**
 * Thrown when fewer than two command-line arguments are provided.
 */
public class InsufficientArgumentsException extends BookCatalogException {
    public InsufficientArgumentsException(String message) {
        super(message);
    }
}
