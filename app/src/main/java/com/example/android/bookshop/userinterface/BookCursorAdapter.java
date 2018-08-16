package com.example.android.bookshop.userinterface;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.android.bookshop.R;
import com.example.android.bookshop.database.BookContract.BookEntry;

import java.util.Locale;

public class BookCursorAdapter extends CursorAdapter {

    private Context globalContext;

    private final int SELL_UNIT_ID = R.string.sell_button_id_tag;
    private final int SELL_UNIT_QUANTITY = R.string.sell_button_quantity_tag;

    BookCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
        this.globalContext = context;
    }

    // Make a new blank list item view. No data is bound to the views yet.
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.book_list_item, parent, false);
    }

    // Bind the book data (in the current row pointed to by cursor) to the given list item layout.
    @Override
    public void bindView(View currentListItem, Context context, Cursor currentCursorRow) {
        // Find the fields to populate in the inflated template.
        TextView bookName = currentListItem.findViewById(R.id.book_name);
        TextView bookAuthors = currentListItem.findViewById(R.id.book_authors);
        TextView bookPrice = currentListItem.findViewById(R.id.unit_price);
        TextView bookQuantity = currentListItem.findViewById(R.id.book_quantity);
        ImageButton sellUnit = currentListItem.findViewById(R.id.sell_unit_button);

        // Extract the values from cursor using the column indices.
        String nameText = currentCursorRow.getString(currentCursorRow.getColumnIndexOrThrow(BookEntry.COLUMN_BOOK_NAME));
        String authorsText = currentCursorRow.getString(currentCursorRow.getColumnIndexOrThrow(BookEntry.COLUMN_BOOK_AUTHORS));
        int price = currentCursorRow.getInt(currentCursorRow.getColumnIndexOrThrow(BookEntry.COLUMN_BOOK_PRICE));
        int quantity = currentCursorRow.getInt(currentCursorRow.getColumnIndexOrThrow(BookEntry.COLUMN_BOOK_QUANTITY));

        // Populate the fields with the extracted properties.
        bookName.setText(nameText);

        /*
         If the author text from the database is empty, set the list item to use the
         unknown author text instead.
        */
        if (!authorsText.isEmpty()) {
            bookAuthors.setText(authorsText);
        } else {
            bookAuthors.setText(globalContext.getString(R.string.unknown_author));
        }

        bookPrice.setText(convertPenceToPounds(price));
        bookQuantity.setText(String.valueOf(quantity));

        /*
         Set tags for each sell unit button to store the current cursor row ID and the current
         quantity. Then add a click listener to it to reduce the quantity value for that item.
        */
        sellUnit.setTag(SELL_UNIT_ID, currentCursorRow.getInt(currentCursorRow.getColumnIndexOrThrow(BookEntry._ID)));
        sellUnit.setTag(SELL_UNIT_QUANTITY, currentCursorRow.getInt(currentCursorRow.getColumnIndexOrThrow(BookEntry.COLUMN_BOOK_QUANTITY)));

        sellUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clickedView) {
                // Pass the clicked view to the method that can reduce the item's quantity.
                decreaseCount(clickedView);
            }
        });
    }

    // Convert pence to pounds and then format it to show 2 decimal places.
    private String convertPenceToPounds(int amountInPence) {
        float amountInPounds = (float) amountInPence / 100;
        return "£" + String.format(Locale.ENGLISH, "%.2f", amountInPounds);
    }

    // Decrease the quantity count by one in the selected list item.
    private void decreaseCount(View clickedView) {
        /*
         Retrieve the tags of the clicked view that indicate which book's quantity should
         be reduced and its current value.
        */
        int bookItemId = (int) clickedView.getTag(SELL_UNIT_ID);
        int currentQuantity = (int) clickedView.getTag(SELL_UNIT_QUANTITY);

        // Check that the current quantity is above 0 so that it can be reduced.
        if (currentQuantity > 0) {
            /*
             If it is, update the quantity of the selected book in the database by storing
             the new quantity value, creating the row URI and then calling the ContentProvider.
            */
            ContentValues newQuantityValue = new ContentValues();
            newQuantityValue.put(BookEntry.COLUMN_BOOK_QUANTITY, currentQuantity - 1);
            Uri updateUri = ContentUris.withAppendedId(BookEntry.CONTENT_URI, bookItemId);

            int rowsAffected = globalContext.getContentResolver().update(updateUri, newQuantityValue, null, null);

            // If no rows were affected, then there was an error with the update.
            if (rowsAffected == 0) {
                Log.e(this.getClass().getSimpleName(), "Error in updating the quantity.");
            } else {
                /*
                 Otherwise, the update was successful and we can notify the data set that
                 values have changed. The tag of the clicked view is also updated.
                */
                notifyDataSetChanged();
                clickedView.setTag(SELL_UNIT_QUANTITY, currentQuantity - 1);
            }
        }
    }
}