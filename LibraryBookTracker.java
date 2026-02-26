import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LibraryBookTracker {

    private static int validRecords  = 0;
    private static int searchResults = 0;
    private static int booksAdded    = 0;
    private static int errorCount    = 0;

    private static File errorLogFile = null;

    // -------------------------------------------------------------------------
    // Thread 1: reads the catalog file and populates the shared books list
    // -------------------------------------------------------------------------
    static class FileReader implements Runnable {
        private final File catalogFile;
        private final List<Book> books;

        FileReader(File catalogFile, List<Book> books) {
            this.catalogFile = catalogFile;
            this.books = books;
        }

        @Override
        public void run() {
            try {
                readCatalog(catalogFile, books);
            } catch (IOException e) {
                errorCount++;
                logError("IO ERROR: \"" + e.getMessage() + "\"", e);
                System.err.println("File I/O Error: " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Thread 2: processes the operation (args[1]) against the shared books list
    // -------------------------------------------------------------------------
    static class OperationAnalyzer implements Runnable {
        private final List<Book> books;
        private final String operation;
        private final File catalogFile;

        OperationAnalyzer(List<Book> books, String operation, File catalogFile) {
            this.books = books;
            this.operation = operation;
            this.catalogFile = catalogFile;
        }

        @Override
        public void run() {
            if (operation.matches("\\d{13}")) {
                // ISBN search
                try {
                    performISBNSearch(books, operation);
                } catch (DuplicateISBNException e) {
                    errorCount++;
                    logError("DUPLICATE ISBN: \"" + operation + "\"", e);
                    System.err.println("Error: " + e.getClass().getSimpleName()
                        + ": " + e.getMessage());
                }

            } else if (operation.split(":", -1).length == 4) {
                // Add book
                try {
                    performAddBook(books, operation, catalogFile);
                } catch (BookCatalogException e) {
                    errorCount++;
                    logError("INVALID INPUT: \"" + operation + "\"", e);
                    System.err.println("Error: " + e.getClass().getSimpleName()
                        + ": " + e.getMessage());
                } catch (IOException e) {
                    errorCount++;
                    logError("IO ERROR: \"" + e.getMessage() + "\"", e);
                    System.err.println("File I/O Error: " + e.getMessage());
                }

            } else {
                // Keyword search
                performKeywordSearch(books, operation);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Main
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        try {
            // Validate argument count
            if (args.length < 2) {
                throw new InsufficientArgumentsException(
                    "Insufficient arguments. Usage: java LibraryBookTracker <catalogFile.txt> <operation>");
            }

            // Validate catalog file name
            if (!args[0].endsWith(".txt")) {
                throw new InvalidFileNameException(
                    "Catalog file must end with '.txt': " + args[0]);
            }

            File catalogFile = new File(args[0]);

            // Create parent directories and/or the file if they do not exist
            File parentDir = catalogFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            if (!catalogFile.exists()) {
                catalogFile.createNewFile();
            }

            // Resolve errors.log path (same directory as the catalog)
            errorLogFile = (parentDir != null)
                ? new File(parentDir, "errors.log")
                : new File("errors.log");

            // Shared catalog list — both threads access this same instance
            List<Book> books = new ArrayList<>();

            // --- Thread 1: FileReader ---
            // Reads the catalog file and populates the shared books list
            Thread fileThread = new Thread(new FileReader(catalogFile, books));
            fileThread.start();
            fileThread.join(); // wait until Thread 1 finishes completely

            // --- Thread 2: OperationAnalyzer ---
            // Starts only after Thread 1 has finished; processes args[1]
            String operation = args[1];
            Thread opThread = new Thread(new OperationAnalyzer(books, operation, catalogFile));
            opThread.start();
            opThread.join(); // wait until Thread 2 finishes completely

        } catch (InsufficientArgumentsException e) {
            errorCount++;
            String provided = (args.length > 0) ? String.join(" ", args) : "(none)";
            logError("INSUFFICIENT ARGUMENTS: \"" + provided + "\"", e);
            System.err.println("Error: " + e.getMessage());
        } catch (InvalidFileNameException e) {
            errorCount++;
            logError("INVALID FILE NAME: \"" + args[0] + "\"", e);
            System.err.println("Error: " + e.getMessage());
        } catch (IOException e) {
            errorCount++;
            logError("IO ERROR: \"" + e.getMessage() + "\"", e);
            System.err.println("File I/O Error: " + e.getMessage());
        } catch (InterruptedException e) {
            errorCount++;
            logError("THREAD INTERRUPTED: \"" + e.getMessage() + "\"", e);
            System.err.println("Thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            errorCount++;
            logError("UNEXPECTED ERROR: \"" + e.getMessage() + "\"", e);
            System.err.println("Unexpected error: " + e.getMessage());
        } finally {
            // Always print statistics and closing message
            System.out.println();
            System.out.println("--- Statistics ---");
            System.out.println("Valid records processed : " + validRecords);
            System.out.println("Search results          : " + searchResults);
            System.out.println("Books added             : " + booksAdded);
            System.out.println("Errors encountered      : " + errorCount);
            System.out.println("Thank you for using the Library Book Tracker.");
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods (called by both Runnables via the enclosing class)
    // -------------------------------------------------------------------------

    /**
     * Reads every line from the catalog file, attempts to parse and validate
     * each one, skips invalid lines (logging them), and populates the provided
     * books list.  Called from Thread 1 (FileReader).
     *
     * Note: java.io.FileReader is referenced with its fully-qualified name here
     * to avoid ambiguity with the inner class also named FileReader.
     */
    private static void readCatalog(File catalogFile, List<Book> books) throws IOException {
        try (BufferedReader reader =
                new BufferedReader(new java.io.FileReader(catalogFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    books.add(parseAndValidate(line));
                    validRecords++;
                } catch (BookCatalogException e) {
                    errorCount++;
                    logError("INVALID LINE: \"" + line + "\"", e);
                    System.err.println("Warning – skipping invalid line: "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Parses a raw "Title:Author:ISBN:Copies" string, validates every field,
     * and returns a Book instance.  Throws a BookCatalogException subclass on
     * the first validation failure found.
     */
    private static Book parseAndValidate(String line) throws BookCatalogException {
        String[] parts = line.split(":", -1);

        if (parts.length != 4) {
            throw new MalformedBookEntryException(
                "Entry must have exactly 4 fields separated by ':' (found "
                + parts.length + ")");
        }

        String title     = parts[0].trim();
        String author    = parts[1].trim();
        String isbn      = parts[2].trim();
        String copiesStr = parts[3].trim();

        if (title.isEmpty()) {
            throw new MalformedBookEntryException("Title is empty");
        }
        if (author.isEmpty()) {
            throw new MalformedBookEntryException("Author is empty");
        }

        validateISBN(isbn);

        int copies;
        try {
            copies = Integer.parseInt(copiesStr);
        } catch (NumberFormatException e) {
            throw new MalformedBookEntryException(
                "Copies is not a valid integer: \"" + copiesStr + "\"");
        }
        if (copies <= 0) {
            throw new MalformedBookEntryException(
                "Copies must be a positive integer greater than zero (got " + copies + ")");
        }

        return new Book(title, author, isbn, copies);
    }

    /** Validates that an ISBN string consists of exactly 13 numeric digits. */
    private static void validateISBN(String isbn) throws InvalidISBNException {
        if (!isbn.matches("\\d{13}")) {
            if (!isbn.matches("\\d*")) {
                throw new InvalidISBNException(
                    "ISBN must contain only numeric characters: \"" + isbn + "\"");
            } else {
                throw new InvalidISBNException(
                    "ISBN must be exactly 13 digits (got " + isbn.length()
                    + "): \"" + isbn + "\"");
            }
        }
    }

    /**
     * Searches for books whose ISBN matches exactly;
     * throws DuplicateISBNException if more than one match exists.
     */
    private static void performISBNSearch(List<Book> books, String isbn)
            throws DuplicateISBNException {
        List<Book> results = new ArrayList<>();
        for (Book b : books) {
            if (b.getIsbn().equals(isbn)) results.add(b);
        }

        if (results.size() > 1) {
            throw new DuplicateISBNException(
                "Multiple books (" + results.size() + ") share ISBN: " + isbn);
        }

        printHeader();
        if (results.isEmpty()) {
            System.out.println("No book found with ISBN: " + isbn);
        } else {
            printBook(results.get(0));
            searchResults = 1;
        }
    }

    /**
     * Searches for books whose titles contain the given keyword
     * (case-insensitive) and prints all matches.
     */
    private static void performKeywordSearch(List<Book> books, String keyword) {
        String lower = keyword.toLowerCase();
        List<Book> results = new ArrayList<>();
        for (Book b : books) {
            if (b.getTitle().toLowerCase().contains(lower)) results.add(b);
        }

        printHeader();
        if (results.isEmpty()) {
            System.out.println("No books found matching keyword: \"" + keyword + "\"");
        } else {
            for (Book b : results) printBook(b);
        }
        searchResults = results.size();
    }

    private static void performAddBook(List<Book> books, String entry, File catalogFile)
            throws BookCatalogException, IOException {
        Book newBook = parseAndValidate(entry);   // may throw BookCatalogException

        books.add(newBook);
        books.sort(Comparator.comparing(b -> b.getTitle().toLowerCase()));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(catalogFile))) {
            for (Book b : books) {
                writer.write(b.toFileString());
                writer.newLine();
            }
        }

        booksAdded = 1;
        printHeader();
        printBook(newBook);
    }

    private static void printHeader() {
        System.out.printf("%-30s %-20s %-15s %5s\n", "Title", "Author", "ISBN", "Copies");
        System.out.println("-".repeat(73));
    }

    private static void printBook(Book b) {
        System.out.printf("%-30s %-20s %-15s %5d\n",
            b.getTitle(), b.getAuthor(), b.getIsbn(), b.getCopies());
    }

    private static void logError(String context, Exception e) {
        File target = (errorLogFile != null) ? errorLogFile : new File("errors.log");

        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        String entry = String.format("[%s] %s - %s: %s%n",
            timestamp, context, e.getClass().getSimpleName(), e.getMessage());

        try (FileWriter fw = new FileWriter(target, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(entry);
        } catch (IOException ioEx) {
            System.err.println("Could not write to error log: " + ioEx.getMessage());
        }
    }
}
