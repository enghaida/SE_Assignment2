/**
 * Thrown when an ISBN is not exactly 13 digits or contains non-numeric characters.
 */
public class InvalidISBNException extends BookCatalogException {
    public InvalidISBNException(String message) {
        super(message);
    }
}
