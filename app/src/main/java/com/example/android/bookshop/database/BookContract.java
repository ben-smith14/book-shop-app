package com.example.android.bookshop.database;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class BookContract {

    /*
     Use the package name for the app as the name for this content provider to guarantee
     uniqueness on the device.
    */
    public static final String CONTENT_AUTHORITY = "com.example.android.bookshop";

    /*
     Use the content authority to create the base URI which apps will use to interact with
     the content provider.
    */
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /*
     Create the possible paths that can be appended to the base content URI to retrieve
     different data from the database tables.
    */
    public static final String PATH_BOOKS = "books";

    // Prevent anyone from instantiating this class.
    private BookContract() {
    }

    // Inner class that defines the table contents using constants.
    public static final class BookEntry implements BaseColumns {
        // The content URI to access the book data in the provider.
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_BOOKS);

        // The MIME type of the {@link #CONTENT_URI} for the list of books.
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_BOOKS;

        // The MIME type of the {@link #CONTENT_URI} for a single book.
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_BOOKS;

        // The table name and column headers.
        public static final String TABLE_NAME = "books";

        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_BOOK_NAME = "name";
        public static final String COLUMN_BOOK_AUTHORS = "authors";
        public static final String COLUMN_BOOK_PAGES = "pages";
        public static final String COLUMN_BOOK_PRICE = "price";
        public static final String COLUMN_BOOK_QUANTITY = "quantity";
        public static final String COLUMN_SUPPLIER_NAME = "supplier_name";
        public static final String COLUMN_SUPPLIER_PHONE_NUMBER = "supplier_phone_number";
    }
}
