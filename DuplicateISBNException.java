/**
 * Thrown when more than one book with the same ISBN is found during an ISBN search.
 */
public class DuplicateISBNException extends BookCatalogException {
    public DuplicateISBNException(String message) {
        super(message);
    }
}
