/**
 * Represents a single book record in the library catalog.
 * A book has a title, author, 13-digit ISBN, and a positive copies count.
 */
public class Book {
    private final String title;
    private final String author;
    private final String isbn;
    private final int copies;

    public Book(String title, String author, String isbn, int copies) {
        this.title  = title;
        this.author = author;
        this.isbn   = isbn;
        this.copies = copies;
    }

    public String getTitle()  { return title;  }
    public String getAuthor() { return author; }
    public String getIsbn()   { return isbn;   }
    public int    getCopies() { return copies; }

    /** Returns the catalog file representation: Title:Author:ISBN:Copies */
    public String toFileString() {
        return title + ":" + author + ":" + isbn + ":" + copies;
    }

    @Override
    public String toString() {
        return toFileString();
    }
}
