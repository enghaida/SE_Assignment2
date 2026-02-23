/**
 * Thrown when the first command-line argument does not end with ".txt".
 */
public class InvalidFileNameException extends BookCatalogException {
    public InvalidFileNameException(String message) {
        super(message);
    }
}
