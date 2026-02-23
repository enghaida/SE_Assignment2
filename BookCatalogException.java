/**
 * Base exception class for all Library Book Tracker errors.
 * All custom exceptions in this application extend this class.
 */
public class BookCatalogException extends Exception {
    public BookCatalogException(String message) {
        super(message);
    }
}
