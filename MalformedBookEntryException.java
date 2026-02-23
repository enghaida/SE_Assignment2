/**
 *  when a book entry has missing fields, empty fields,
 * or an invalid  value 
 */
public class MalformedBookEntryException extends BookCatalogException {
    public MalformedBookEntryException(String message) {
        super(message);
    }
}
