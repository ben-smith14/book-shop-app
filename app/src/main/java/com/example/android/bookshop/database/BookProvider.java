package com.example.android.bookshop.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.android.bookshop.database.BookContract.BookEntry;

public class BookProvider extends ContentProvider {

    public final String LOG_TAG = BookProvider.class.getSimpleName();

    // URI matcher codes for the book table content URI and single book content URI respectively.
    private static final int BOOKS = 25;
    private static final int BOOK_ID = 50;

    /*
     Initialise the UriMatcher object to match content URIs with their corresponding code.
     Use a static initializer, which is run before anything else in the class, to add the
     paths to the matcher.
    */
    private static final UriMatcher sBookUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sBookUriMatcher.addURI(BookContract.CONTENT_AUTHORITY, BookContract.PATH_BOOKS, BOOKS);
        sBookUriMatcher.addURI(BookContract.CONTENT_AUTHORITY, BookContract.PATH_BOOKS + "/#", BOOK_ID);
    }

    private BookDbHelper databaseHelper;

    // Initialise the database helper object.
    @Override
    public boolean onCreate() {
        databaseHelper = new BookDbHelper(getContext());
        return true;
    }

    /*
     Perform a query for the given URI. Use the given projection, selection, selection
     arguments and sort order in the provider call.
    */
    @Override
    public Cursor query(@NonNull Uri uriInput, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Get a readable version of the database.
        SQLiteDatabase bookDatabase = databaseHelper.getReadableDatabase();

        // Initialise the cursor that will hold the result of the query.
        Cursor responseCursor;

        // Use the URI matcher to match the incoming URI with a specific code.
        final int matchCode = sBookUriMatcher.match(uriInput);
        switch (matchCode) {
            case BOOKS:
                /*
                 For the BOOKS code, query the whole book table directly. The returned
                 cursor will contain all the rows in the book table.
                */
                responseCursor = bookDatabase.query(BookEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case BOOK_ID:
                /*
                 For the BOOK_ID code, extract the ID of the item from the URI into the
                 selection arguments array and then perform the query operation with this
                 as an input.
                */
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uriInput))};

                // This query will return a Cursor object containing a single row of the table.
                responseCursor = bookDatabase.query(BookEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                // Throw an exception if the input URI did not match one of the acceptable cases.
                throw new IllegalArgumentException("Cannot query unknown URI: " + uriInput);
        }

        /*
         Set a notification URI on the Cursor so that we know which content URI the Cursor
         was created for. If the data at this URI changes, this will notify that we need to
         update the Cursor.
        */
        if (getContext() != null) {
            responseCursor.setNotificationUri(getContext().getContentResolver(), uriInput);
        }

        return responseCursor;
    }

    /*
     Insert new data into the provider with the given ContentValues. The only acceptable content
     URI that can be passed to this is the one for the whole table.
    */
    @Override
    public Uri insert(@NonNull Uri uriInput, ContentValues bookValues) {
        final int matchCode = sBookUriMatcher.match(uriInput);
        switch (matchCode) {
            case BOOKS:
                return insertBook(uriInput, bookValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for: " + uriInput);
        }
    }

    /*
     Insert a book into the database with the given ContentValues. Return the new content URI
     for that specific row in the database.
    */
    private Uri insertBook(Uri uriInput, ContentValues bookValues) {
        // Check that the book name is not null.
        String bookName = bookValues.getAsString(BookEntry.COLUMN_BOOK_NAME);
        if (bookName == null) {
            throw new IllegalArgumentException("Book requires a name.");
        }

        // If the number of pages is provided, check that it's greater than or equal to 0.
        Integer bookPages = bookValues.getAsInteger(BookEntry.COLUMN_BOOK_PAGES);
        if (bookPages != null && bookPages < 0) {
            throw new IllegalArgumentException("Book requires a valid number of pages.");
        }

        // Check that the price is provided and that it is greater than or equal to 0.
        Integer bookPrice = bookValues.getAsInteger(BookEntry.COLUMN_BOOK_PRICE);
        if (bookPrice == null) {
            throw new IllegalArgumentException("Book requires a price.");
        } else if (bookPrice < 0) {
            throw new IllegalArgumentException("Book requires a valid price.");
        }

        // Check that the quantity is provided and that it is greater than or equal to 0.
        Integer bookQuantity = bookValues.getAsInteger(BookEntry.COLUMN_BOOK_QUANTITY);
        if (bookQuantity == null) {
            throw new IllegalArgumentException("Book requires a quantity.");
        } else if (bookQuantity < 0) {
            throw new IllegalArgumentException("Book requires a valid quantity.");
        }

        // Check that the supplier name is not null.
        String supplierName = bookValues.getAsString(BookEntry.COLUMN_SUPPLIER_NAME);
        if (supplierName == null) {
            throw new IllegalArgumentException("Book requires a supplier name.");
        }

        // Check that the supplier number is not null.
        String supplierNumber = bookValues.getAsString(BookEntry.COLUMN_SUPPLIER_PHONE_NUMBER);
        if (supplierNumber == null) {
            throw new IllegalArgumentException("Book requires a supplier number.");
        }

        /*
         Retrieve a writable version of the database and insert the given values into a new
         row of the book table.
        */
        SQLiteDatabase bookDatabase = databaseHelper.getWritableDatabase();
        long newBookId = bookDatabase.insert(BookEntry.TABLE_NAME, null, bookValues);

        // Log an error message if the new book could not be inserted.
        if (newBookId == -1) {
            Log.e(LOG_TAG, "Failed to insert new book for: " + uriInput);
            return null;
        }

        // Notify all notification URIs that the data has changed for the given content URI.
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uriInput, null);
        }

        /*
         Once we know the ID of the new row in the table, return the new URI with the ID added
         to its end.
        */
        return ContentUris.withAppendedId(uriInput, newBookId);
    }

    // Updates the data at the given selection and selection arguments with the new ContentValues.
    @Override
    public int update(@NonNull Uri uriInput, ContentValues bookValues, String selection, String[] selectionArgs) {
        final int matchCode = sBookUriMatcher.match(uriInput);
        switch (matchCode) {
            case BOOKS:
                return updateBook(uriInput, bookValues, selection, selectionArgs);
            case BOOK_ID:
                /*
                 For the BOOK_ID code, first extract out the ID from the URI so that we
                 know which row to update and then add this to the selection arguments array.
                */
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uriInput))};

                // Perform the update operation with the new selection and selectionArgs array.
                return updateBook(uriInput, bookValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for: " + uriInput);
        }
    }

    /*
     Update the specified book in the database with the given ContentValues. Return the
     number of rows that were successfully updated.
    */
    private int updateBook(Uri uriInput, ContentValues bookValues, String selection, String[] selectionArgs) {
        // If there are no values to update, then don't try to update the database.
        if (bookValues.size() == 0) {
            return 0;
        }

        // Check that the name is not null if it is present in the ContentValues object.
        if (bookValues.containsKey(BookEntry.COLUMN_BOOK_NAME)) {
            String bookName = bookValues.getAsString(BookEntry.COLUMN_BOOK_NAME);
            if (bookName == null) {
                throw new IllegalArgumentException("Book requires a name.");
            }
        }

        /*
         Check that the number of pages is an acceptable value if it is present in the
         ContentValues object.
        */
        if (bookValues.containsKey(BookEntry.COLUMN_BOOK_PAGES)) {
            Integer bookPages = bookValues.getAsInteger(BookEntry.COLUMN_BOOK_PAGES);
            if (bookPages != null && bookPages < 0) {
                throw new IllegalArgumentException("Book requires a valid number of pages.");
            }
        }

        /*
         Check that the price is not null and that it is an acceptable value if it is present
         in the ContentValues object.
        */
        if (bookValues.containsKey(BookEntry.COLUMN_BOOK_PRICE)) {
            Integer bookPrice = bookValues.getAsInteger(BookEntry.COLUMN_BOOK_PRICE);
            if (bookPrice == null) {
                throw new IllegalArgumentException("Book requires a price.");
            } else if (bookPrice < 0) {
                throw new IllegalArgumentException("Book requires a valid price.");
            }
        }

        /*
         Check that the quantity is not null and that it is an acceptable value if it is
         present in the ContentValues object.
        */
        if (bookValues.containsKey(BookEntry.COLUMN_BOOK_QUANTITY)) {
            Integer bookQuantity = bookValues.getAsInteger(BookEntry.COLUMN_BOOK_QUANTITY);
            if (bookQuantity == null) {
                throw new IllegalArgumentException("Book requires a quantity.");
            } else if (bookQuantity < 0) {
                throw new IllegalArgumentException("Book requires a valid quantity.");
            }
        }

        // Check that the supplier's name is not null if it is present in the ContentValues object.
        if (bookValues.containsKey(BookEntry.COLUMN_SUPPLIER_NAME)) {
            String bookSupplier = bookValues.getAsString(BookEntry.COLUMN_SUPPLIER_NAME);
            if (bookSupplier == null) {
                throw new IllegalArgumentException("Book requires a supplier name.");
            }
        }

        // Check that the supplier's number is not null if it is present in the ContentValues object.
        if (bookValues.containsKey(BookEntry.COLUMN_SUPPLIER_PHONE_NUMBER)) {
            String supplierNumber = bookValues.getAsString(BookEntry.COLUMN_SUPPLIER_PHONE_NUMBER);
            if (supplierNumber == null) {
                throw new IllegalArgumentException("Book requires a supplier number.");
            }
        }

        /*
         Retrieve a writable version of the database and perform the update operation,
         retrieving the number of rows that were altered.
         */
        SQLiteDatabase bookDatabase = databaseHelper.getWritableDatabase();
        int rowsUpdated = bookDatabase.update(BookEntry.TABLE_NAME, bookValues, selection, selectionArgs);

        if (getContext() != null) {
            // Set a notification URI on the Cursor once again.
            if (rowsUpdated != 0) {
                getContext().getContentResolver().notifyChange(uriInput, null);
            }
        }

        // Return the number of rows updated
        return rowsUpdated;
    }

    // Delete the data at the given selection and selection arguments.
    @Override
    public int delete(@NonNull Uri uriInput, String selection, String[] selectionArgs) {
        // Get a writable version of the database.
        SQLiteDatabase bookDatabase = databaseHelper.getWritableDatabase();

        /*
         Track the number of rows that were deleted and use the URI matcher to match the
         incoming URI with a specific code.
        */
        int rowsDeleted;
        final int matchCode = sBookUriMatcher.match(uriInput);
        switch (matchCode) {
            case BOOKS:
                // Delete all the rows that match the selection and selection args.
                rowsDeleted = bookDatabase.delete(BookEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case BOOK_ID:
                // Delete a single row given by the ID in the URI.
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uriInput))};
                rowsDeleted = bookDatabase.delete(BookEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for: " + uriInput);
        }

        if (getContext() != null) {
            // Set a notification URI on the Cursor once again.
            if (rowsDeleted != 0) {
                getContext().getContentResolver().notifyChange(uriInput, null);
            }
        }

        // Return the number of rows deleted.
        return rowsDeleted;
    }

    // Returns the MIME type of data for the content URI.
    @Override
    public String getType(@NonNull Uri uri) {
        final int matchCode = sBookUriMatcher.match(uri);
        switch (matchCode) {
            case BOOKS:
                return BookEntry.CONTENT_LIST_TYPE;
            case BOOK_ID:
                return BookEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI: " + uri + " with match: " + matchCode);
        }
    }
}