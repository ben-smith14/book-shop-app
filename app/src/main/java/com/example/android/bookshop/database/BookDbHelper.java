package com.example.android.bookshop.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.bookshop.database.BookContract.BookEntry;

public class BookDbHelper extends SQLiteOpenHelper {

    // Upgrade the database version if the database is altered.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "bookshop.db";

    /*
     Use this raw SQL code for creating a table in the database file. Note that all column
     data types are fairly self explanatory, apart from price, which should be saved in pence
     rather than pounds, and authors, which should be saved as a single string with names
     of different authors separated by commas.
    */
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + BookEntry.TABLE_NAME + " (" +
                    BookEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    BookEntry.COLUMN_BOOK_NAME + " TEXT NOT NULL," +
                    BookEntry.COLUMN_BOOK_AUTHORS + " TEXT," +
                    BookEntry.COLUMN_BOOK_PAGES + " INTEGER NOT NULL DEFAULT 0," +
                    BookEntry.COLUMN_BOOK_PRICE + " INTEGER NOT NULL," +
                    BookEntry.COLUMN_BOOK_QUANTITY + " INTEGER NOT NULL DEFAULT 0," +
                    BookEntry.COLUMN_SUPPLIER_NAME + " TEXT NOT NULL," +
                    BookEntry.COLUMN_SUPPLIER_PHONE_NUMBER + " TEXT NOT NULL);";

    // Use this raw SQL code to delete a table in the database file.
    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + BookEntry.TABLE_NAME;

    BookDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Create a new database if one does not exist.
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
    }

    /*
     Update an existing database if it already exists by removing the old one and then
     recreating it with the new schema/values.
    */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL(SQL_DELETE_ENTRIES);
        onCreate(sqLiteDatabase);
    }
}