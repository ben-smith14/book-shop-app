package com.example.android.bookshop.userinterface;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.bookshop.R;
import com.example.android.bookshop.database.BookContract.BookEntry;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int BOOK_LOADER = 0;
    private BookCursorAdapter bookListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
         Find the ListView to populate and set its empty view so that it only shows when the
         list has 0 items.
        */
        ListView bookList = findViewById(R.id.books_list_view);
        View emptyView = findViewById(R.id.empty_view);
        bookList.setEmptyView(emptyView);

        /*
         Instantiate the cursor adapter using the returned cursor from the database query
         and attach it to the ListView.
        */
        bookListAdapter = new BookCursorAdapter(this, null);
        bookList.setAdapter(bookListAdapter);

        /*
         Item click listener to open up the details screen for the selected book so that it
         can be edited.
        */
        bookList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent editBookIntent = new Intent(MainActivity.this, EditorActivity.class);

                /*
                 Create the URI for the selected book to send with the intent by adding the
                 clicked item's id to the end of the content URI.
                */
                Uri currentBookUri = ContentUris.withAppendedId(BookEntry.CONTENT_URI, id);

                // Set the URI on the data field of the intent.
                editBookIntent.setData(currentBookUri);

                startActivity(editBookIntent);
            }
        });

        // Start the loader.
        getLoaderManager().initLoader(BOOK_LOADER, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu appBarMenu) {
        // Inflate the menu options for the app bar.
        getMenuInflater().inflate(R.menu.menu_main, appBarMenu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to a click on the "Add book" menu icon.
            case R.id.insert_new_book:
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                startActivity(intent);
                return true;
            // Respond to a click on the "Delete all books" menu option.
            case R.id.action_delete_all_entries:
                deleteAllPets();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        /*
         Define the projection array to return only the columns we want to display for each
         item in our list.
        */
        String[] projection = {
                BookEntry._ID,
                BookEntry.COLUMN_BOOK_NAME,
                BookEntry.COLUMN_BOOK_AUTHORS,
                BookEntry.COLUMN_BOOK_PRICE,
                BookEntry.COLUMN_BOOK_QUANTITY
        };

        // This loader will execute the ContentProvider's query method on a background thread.
        return new CursorLoader(this,
                BookEntry.CONTENT_URI,
                projection,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        /*
         Update the adapter with the new cursor so that it can display the current state of
         the database.
        */
        bookListAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        bookListAdapter.swapCursor(null);
    }

    // Helper method to delete all pets in the database.
    private void deleteAllPets() {
        int rowsDeleted = getContentResolver().delete(BookEntry.CONTENT_URI, null, null);
        Log.v(LOG_TAG, rowsDeleted + " rows deleted from pet database.");
    }
}