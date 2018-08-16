package com.example.android.bookshop.userinterface;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.bookshop.R;
import com.example.android.bookshop.database.BookContract.BookEntry;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private EditText bookNameEdit;
    private EditText bookAuthorsEdit;
    private EditText bookPagesEdit;
    private EditText bookPriceEdit;
    private EditText bookQuantityEdit;
    private TextView bookQuantityText;
    private EditText changeQuantityEdit;
    private EditText bookSupplierName;
    private EditText bookSupplierNumber;

    // Content URI for the existing book (this will be null if it's a new book).
    private Uri currentBookUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Find all the relevant views that we will need to read user input from.
        bookNameEdit = findViewById(R.id.edit_book_name);
        bookAuthorsEdit = findViewById(R.id.edit_book_authors);
        bookPagesEdit = findViewById(R.id.edit_book_pages);

        bookPriceEdit = findViewById(R.id.edit_book_price);
        bookQuantityEdit = findViewById(R.id.edit_book_quantity);
        bookQuantityText = findViewById(R.id.book_quantity_text);
        ImageButton addStockButton = findViewById(R.id.add_stock);
        ImageButton removeStockButton = findViewById(R.id.remove_stock);
        changeQuantityEdit = findViewById(R.id.change_quantity_value);

        bookSupplierName = findViewById(R.id.edit_supplier_name);
        bookSupplierNumber = findViewById(R.id.edit_supplier_number);
        ImageButton callSupplierButton = findViewById(R.id.call_supplier);

        /*
         Add a TextWatcher to the price EditText field to prevent more than two decimal places
         from being added. Solution adapted from the one given by Apoleo at:
         https://stackoverflow.com/questions/5357455/limit-decimal-places-in-android-edittext.
        */
        bookPriceEdit.addTextChangedListener(new TextWatcher() {
            private String oldPrice;

            @Override
            public void beforeTextChanged(CharSequence currentText, int i, int i1, int i2) {
                oldPrice = currentText.toString();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Check that the price String is not empty before proceeding.
                String newPrice = bookPriceEdit.getText().toString();
                if (newPrice.isEmpty()) {
                    return;
                }

                // Check that the price is in a valid format.
                String checkedText = checkPriceFormat(oldPrice, newPrice, 4, 2);

                // If the checked String is different to the initial String, use the
                // checked String to set the text and move the text cursor to the end.
                if (!checkedText.equals(newPrice)) {
                    bookPriceEdit.setText(checkedText);

                    int lastCharPosition = bookPriceEdit.getText().length();
                    bookPriceEdit.setSelection(lastCharPosition);
                }
            }
        });

        /*
         Add the currency symbol and any additional zeros when the price editor loses focus.
         Remove the currency symbol when it gains focus.
        */
        bookPriceEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View selectedView, boolean hasFocus) {
                EditText priceEditor = (EditText) selectedView;

                /*
                 If the EditText has just gained focus, remove the currency symbol from its
                 beginning.
                */
                if (hasFocus) {
                    String priceText = priceEditor.getText().toString().trim();
                    if (!TextUtils.isEmpty(priceText)) {
                        priceEditor.setText(priceText.substring(1));
                    }
                } else {
                    /*
                     Otherwise, it has just lost focus, so add the symbol back in and check
                     for the correct number of decimal places using the appropriate method.
                    */
                    String priceFormatText = "£" + priceEditor.getText().toString().trim();
                    priceEditor.setText(alterPriceDecimalCount(priceFormatText));
                }
            }
        });

        /*
         Examine the intent that was used to launch this activity, in order to figure out
         if we're creating a new pet or editing an existing one.
        */
        Intent intentWithPossibleUri = getIntent();
        currentBookUri = intentWithPossibleUri.getData();

        /*
         If the intent DOES NOT contain a book content URI, then we know that we are
         creating a new book.
        */
        if (currentBookUri == null) {
            // This is a new book, so change the app bar text to reflect this.
            setTitle(getString(R.string.editor_activity_title_new_book));

            /*
             Hide the quantity TextView and the associated views to change its value, as they
             will not be used here.
            */
            bookQuantityText.setVisibility(View.GONE);
            addStockButton.setVisibility(View.GONE);
            removeStockButton.setVisibility(View.GONE);
            changeQuantityEdit.setVisibility(View.GONE);

            // Also hide the supplier call button when creating a new entry.
            callSupplierButton.setVisibility(View.GONE);

            /*
             Invalidate the options menu, so the "Delete" menu option can be hidden, as it
             doesn't make sense to delete a book that hasn't been created yet.
            */
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing book, so change the app bar text to reflect this.
            setTitle(getString(R.string.editor_activity_title_edit_book));

            /*
             Hide the quantity EditText field when we are editing an entry, as the quantity
             TextView and the associated views to change its value will be used instead.
            */
            bookQuantityEdit.setVisibility(View.GONE);

            /*
             For the end of the overview and inventory sections of the editor, change the
             pop-up keyboard so that the user can close it instead of moving onto to the next
             EditText field.
            */
            bookPagesEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
            changeQuantityEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);

            // Add the amount of stock for the given book using the value specified by the user.
            addStockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String currentStock = bookQuantityText.getText().toString();
                    String amountToAlterBy = changeQuantityEdit.getText().toString().trim();

                    // If the user has not entered an amount to change the stock by, tell them.
                    if (TextUtils.isEmpty(amountToAlterBy)) {
                        createCustomToast(getString(R.string.add_quantity));
                    } else {
                        /*
                         Otherwise, find the new stock amount and display it using the
                         quantity TextView. Also, clear the 'change by amount' field for
                         the next input.
                        */
                        int newStockValue = alterStockValue(Integer.parseInt(currentStock),
                                Integer.parseInt(amountToAlterBy),
                                true);

                        bookQuantityText.setText(String.valueOf(newStockValue));
                        changeQuantityEdit.setText("");
                    }
                }
            });

            // Remove the amount of stock for the given book using the value specified by the user.
            removeStockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String currentStock = bookQuantityText.getText().toString();
                    String amountToAlterBy = changeQuantityEdit.getText().toString().trim();

                    if (TextUtils.isEmpty(amountToAlterBy)) {
                        createCustomToast(getString(R.string.remove_quantity));
                    } else {
                        int newStockValue = alterStockValue(Integer.parseInt(currentStock),
                                Integer.parseInt(amountToAlterBy),
                                false);

                        /*
                         For the remove button, check that the stock amount will not drop below
                         zero. If it will, do not make the change and indicate to the user that
                         they cannot remove that much stock.
                        */
                        if (newStockValue > 0) {
                            bookQuantityText.setText(String.valueOf(newStockValue));
                            changeQuantityEdit.setText("");
                        } else {
                            createCustomToast(getString(R.string.not_enough_stock));
                            changeQuantityEdit.setText("");
                        }
                    }
                }
            });

            // Call the supplier using a phone app if any are available.
            callSupplierButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    /*
                     Retrieve the phone number and alter it to add the UK country code at the
                     beginning and then prepare a phone opening intent.
                    */
                    String numberToParse = bookSupplierNumber.getText().toString().trim().substring(1);
                    numberToParse = getString(R.string.uk_country_code) + numberToParse;
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + numberToParse));

                    /*
                     Check that there is an app that can receive the intent and then start the
                     activity if there is. If there isn't, indicate this with a Toast message.
                    */
                    if (dialIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(dialIntent);
                    } else {
                        createCustomToast(getString(R.string.no_phone_app));
                    }
                }
            });

            /*
             Initialize a loader to read the book data from the database and display the
             current values in the editor.
            */
            getLoaderManager().initLoader(0, null, this);
        }
    }

    /*
     Modified solution from Apoleo at:
     https://stackoverflow.com/questions/5357455/limit-decimal-places-in-android-edittext.
    */
    public String checkPriceFormat(String stringBeforeChange, String stringAfterChange, int maxBeforePoint, int maxDecimal) {
        // If the first character in the String is a decimal point, add a zero to the beginning.
        if (stringAfterChange.charAt(0) == '.') {
            stringAfterChange = "0" + stringAfterChange;
        }

        int maxIndex = stringAfterChange.length();
        boolean cursorAfterPoint = false;
        int aboveDecimal = 0, belowDecimal = 0;
        StringBuilder finalString = new StringBuilder("");

        /*
         Check that the character count before and after the decimal point is within the set limits.
        */
        for (int i = 0; i < maxIndex; i++) {
            char currentChar = stringAfterChange.charAt(i);

            /*
             If the current character is not a point and the point hasn't been reached yet,
             add one to the above decimal count.
            */
            if (currentChar != '.' && !cursorAfterPoint) {
                aboveDecimal++;

                /*
                 If the above decimal count exceeds the limit, ignore the price change and return
                 its previous state.
                */
                if (aboveDecimal > maxBeforePoint) {
                    return stringBeforeChange;
                }
            } else if (currentChar == '.') {
                // When the point is reached, flip the boolean indicating this.
                cursorAfterPoint = true;
            } else {
                // If the current character is after the point, add one to the below decimal count.
                belowDecimal++;
                /*
                 If the below decimal count exceeds the limit, ignore the price change and return
                 its previous state.
                */
                if (belowDecimal > maxDecimal) {
                    return stringBeforeChange;
                }
            }

            // Add any accepted characters to the output string.
            finalString.append(currentChar);
        }

        return finalString.toString();
    }

    /*
     Add any additional zeros to the end of a price String if needed to ensure 2 decimal places.
     Do this by counting the number of characters after the point and then adjusting accordingly.
    */
    private String alterPriceDecimalCount(String priceFormatText) {
        boolean afterPoint = false;
        int belowPoint = 0;

        for (int i = 0; i < priceFormatText.length(); i++) {
            if (afterPoint) {
                belowPoint++;
            }

            if (priceFormatText.charAt(i) == '.') {
                afterPoint = true;
            }
        }

        if (belowPoint == 0) {
            return priceFormatText + "00";
        } else if (belowPoint == 1) {
            return priceFormatText + "0";
        }

        return priceFormatText;
    }

    private int alterStockValue(int currentValue, int amountToAlterBy, boolean toAdd) {
        if (toAdd) {
            return currentValue + amountToAlterBy;
        } else {
            return currentValue - amountToAlterBy;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // If this is a new book, hide the "Delete" menu item.
        if (currentBookUri == null) {
            MenuItem editorDelete = menu.findItem(R.id.action_delete);
            editorDelete.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the XML file.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option.
            case R.id.action_save:
                /*
                 If the user clicks on the save button whilst the price still has focus,
                 forcibly remove focus from it so that it correctly formats before saving.
                */
                if (bookPriceEdit.hasFocus()) {
                    LinearLayout parentLayout = findViewById(R.id.parent_linear_layout);
                    parentLayout.requestFocus();
                }

                saveBook();
                return true;

            // Respond to a click on the "Delete" menu option.
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;

            // Respond to a click on the "Up" arrow button in the app bar.
            case android.R.id.home:
                /*
                 Setup a dialog to check that the user wants to return. Create a click
                 listener to handle the user confirming that changes should be discarded.
                */
                DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, so navigate to parent activity.
                        NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    }
                };

                // Show a dialog that notifies the user they have unsaved changes.
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Save a new book into the database.
    private void saveBook() {
        // Read from the input fields.
        String nameText = bookNameEdit.getText().toString().trim();
        String authorsText = bookAuthorsEdit.getText().toString().trim();
        String pagesText = bookPagesEdit.getText().toString().trim();
        String priceDecimalText = bookPriceEdit.getText().toString();

        /*
         Retrieve the quantity value as a String from either the EditText field or the TextView,
         depending on whether the activity is dealing with a new book or an existing book.
        */
        String quantityText;
        if (bookQuantityEdit.isShown()) {
            quantityText = bookQuantityEdit.getText().toString().trim();
        } else {
            quantityText = bookQuantityText.getText().toString();
        }

        String supplierNameText = bookSupplierName.getText().toString().trim();
        String supplierNumberText = bookSupplierNumber.getText().toString().trim();

        // Don't carry on with the save if any of the non-optional fields are empty.
        if (TextUtils.isEmpty(nameText) || TextUtils.isEmpty(priceDecimalText) || TextUtils.isEmpty(quantityText)
                || TextUtils.isEmpty(supplierNameText) || TextUtils.isEmpty(supplierNumberText)) {
            createCustomToast(getString(R.string.complete_fields));
            return;
        }

        /*
         If the pages parameter is not provided by the user, don't try to parse
         the String into an integer value and use the default value (0) instead.
        */
        int pagesValue = 0;
        if (!TextUtils.isEmpty(pagesText)) {
            pagesValue = Integer.parseInt(pagesText);
        }

        /*
         Convert the price decimal to an integer currency unit value after removing the
         currency symbol from the String.
        */
        float priceDecimal = Float.parseFloat(priceDecimalText.substring(1));
        int priceUnits = (int) (priceDecimal * 100);

        // Convert the quantity String into an integer value.
        int quantityValue = Integer.parseInt(quantityText);

        // Store the input values using the database column name keys.
        ContentValues bookDetails = new ContentValues();
        bookDetails.put(BookEntry.COLUMN_BOOK_NAME, nameText);
        bookDetails.put(BookEntry.COLUMN_BOOK_AUTHORS, authorsText);
        bookDetails.put(BookEntry.COLUMN_BOOK_PAGES, pagesValue);
        bookDetails.put(BookEntry.COLUMN_BOOK_PRICE, priceUnits);
        bookDetails.put(BookEntry.COLUMN_BOOK_QUANTITY, quantityValue);
        bookDetails.put(BookEntry.COLUMN_SUPPLIER_NAME, supplierNameText);
        bookDetails.put(BookEntry.COLUMN_SUPPLIER_PHONE_NUMBER, supplierNumberText);

        // If we are saving a new book, use the insert method from the content provider.
        if (currentBookUri == null) {
            // Return the content URI for the new book after inserting it into the database.
            Uri newUri = getContentResolver().insert(BookEntry.CONTENT_URI, bookDetails);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                createCustomToast(getString(R.string.editor_insert_book_failed));
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                createCustomToast(getString(R.string.editor_insert_book_successful));
            }
        } else {
            /*
             Otherwise this is an EXISTING book, so update the book with the content URI
             'currentBookUri' and pass in the new ContentValues. Pass in null for the selection
             and selection args because currentBookUri will already identify the correct row
             in the database that we want to modify.
            */
            int rowsAffected = getContentResolver().update(currentBookUri, bookDetails, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                createCustomToast(getString(R.string.editor_update_book_failed));
            } else {
                // Otherwise, the update was successful and we can display a toast.
                createCustomToast(getString(R.string.editor_update_book_successful));
            }
        }

        // End the activity if the save method reaches its end.
        finish();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        /*
         Since the editor shows all book attributes, create a projection array that contains
         all the columns from the book table.
        */
        String[] projection = {
                BookEntry._ID,
                BookEntry.COLUMN_BOOK_NAME,
                BookEntry.COLUMN_BOOK_AUTHORS,
                BookEntry.COLUMN_BOOK_PAGES,
                BookEntry.COLUMN_BOOK_PRICE,
                BookEntry.COLUMN_BOOK_QUANTITY,
                BookEntry.COLUMN_SUPPLIER_NAME,
                BookEntry.COLUMN_SUPPLIER_PHONE_NUMBER
        };

        /*
         Execute the ContentProvider's query method on a background thread using the content
         URI for the current book and the projection array to indicate which columns to include
         in the return cursor.
        */
        return new CursorLoader(this,
                currentBookUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or if there is less than 1 row in the cursor.
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        /*
         Proceed with moving to the first row of the cursor and reading data from it
         (This should be the only row in the cursor).
        */
        if (cursor.moveToFirst()) {
            // Find the the column index of the book attributes that we're interested in.
            int nameColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_NAME);
            int authorsColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_AUTHORS);
            int pagesColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_PAGES);
            int priceColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_BOOK_QUANTITY);
            int supplierNameColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_SUPPLIER_NAME);
            int supplierNumberColumnIndex = cursor.getColumnIndex(BookEntry.COLUMN_SUPPLIER_PHONE_NUMBER);

            // Extract out the value from the Cursor for the given column indices.
            String name = cursor.getString(nameColumnIndex);
            String authors = cursor.getString(authorsColumnIndex);
            int pages = cursor.getInt(pagesColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            String supplierName = cursor.getString(supplierNameColumnIndex);
            String supplierNumber = cursor.getString(supplierNumberColumnIndex);

            // Update the views on the screen with the values from the database.
            bookNameEdit.setText(name);

            // Update the author(s) EditText field if the String from the database isn't empty.
            if (!authors.isEmpty()) {
                bookAuthorsEdit.setText(authors);
            }

            /*
             If the stored pages value is greater than the default (0), show it in the
             relevant EditText view. Otherwise, keep it clear.
            */
            if (pages > 0) {
                bookPagesEdit.setText(String.valueOf(pages));
            }

            /*
             Convert the price in integer units to a decimal format with a currency symbol
             and the correct number of decimal places.
            */
            float priceInDecimal = ((float) price) / 100;
            String priceText = "£" + String.valueOf(priceInDecimal);
            bookPriceEdit.setText(alterPriceDecimalCount(priceText));

            /*
             Set the EditText quantity field contents if it is shown. If not, set the TextView
             instead.
            */
            if (bookQuantityEdit.isShown()) {
                bookQuantityEdit.setText(String.valueOf(quantity));
            } else {
                bookQuantityText.setText(String.valueOf(quantity));
            }

            bookSupplierName.setText(supplierName);
            bookSupplierNumber.setText(supplierNumber);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        bookNameEdit.setText("");
        bookAuthorsEdit.setText("");
        bookPagesEdit.setText("");
        bookPriceEdit.setText("");

        if (bookQuantityEdit.isShown()) {
            bookQuantityEdit.setText("");
        } else {
            bookQuantityText.setText("");
        }

        bookSupplierName.setText("");
        bookSupplierNumber.setText("");
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        /*
         Create an alert dialog, setting the message and click listeners for the positive
         and negative buttons on the dialog.
        */
        AlertDialog.Builder discardBookBuilder = new AlertDialog.Builder(this);
        discardBookBuilder.setMessage(R.string.unsaved_changes_dialog_msg);
        discardBookBuilder.setPositiveButton(R.string.discard, discardButtonClickListener);
        discardBookBuilder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface discardDialog, int id) {
                /*
                 User clicked the "Keep editing" button, so dismiss the dialog
                 and continue editing the book.
                */
                if (discardDialog != null) {
                    discardDialog.dismiss();
                }
            }
        });

        // Create and show the alert dialog.
        AlertDialog discardBookDialog = discardBookBuilder.create();
        discardBookDialog.show();
    }

    @Override
    public void onBackPressed() {
        /*
         Setup a dialog to check that the user wants to return. Create a click
         listener to handle the user confirming that the changes should be discarded.
        */
        DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // User clicked "Discard" button, so close the current activity.
                finish();
            }
        };

        // Show the dialog to the user.
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void showDeleteConfirmationDialog() {
        /*
         Create an alert dialog, setting the message and click listeners for the positive
         and negative buttons on the dialog.
        */
        AlertDialog.Builder deleteBookBuilder = new AlertDialog.Builder(this);
        deleteBookBuilder.setMessage(R.string.delete_dialog_msg);
        deleteBookBuilder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface deleteDialog, int id) {
                // User clicked the "Delete" button, so delete the book.
                deleteBook();
            }
        });

        deleteBookBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface deleteDialog, int id) {
                /*
                 User clicked the "Cancel" button, so dismiss the dialog and continue
                 editing the book.
                */
                if (deleteDialog != null) {
                    deleteDialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog.
        AlertDialog deleteBookDialog = deleteBookBuilder.create();
        deleteBookDialog.show();
    }

    // Perform the deletion of the book in the database.
    private void deleteBook() {
        // Only perform the delete if this is an existing book.
        if (currentBookUri != null) {
            /*
             Call the content resolver to delete the book at the given content URI. Pass in
             null for the selection and selection args because the 'currentBookUri'
             content URI already identifies the book that we want.
            */
            int rowsDeleted = getContentResolver().delete(currentBookUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                createCustomToast(getString(R.string.editor_delete_book_failed));
            } else {
                // Otherwise, the delete was successful.
                createCustomToast(getString(R.string.editor_delete_book_successful));
            }

            // Close the activity.
            finish();
        }
    }

    // Method to create a toast message with a custom background and text colour.
    public void createCustomToast(String toastText) {
        // Inflate the custom layout file for the Toast message.
        View toastView = getLayoutInflater().inflate(R.layout.custom_toast, (ViewGroup) findViewById(R.id.custom_toast_container));

        // Get the TextView in the toast layout and set its contents.
        TextView toastTextView = toastView.findViewById(R.id.toast_text);
        toastTextView.setText(toastText);

        // Set the parameters of the toast message.
        Toast customToast = new Toast(getApplicationContext());
        customToast.setGravity(Gravity.BOTTOM, 0, 50);
        customToast.setDuration(Toast.LENGTH_LONG);
        customToast.setView(toastView);

        // Show the toast message.
        customToast.show();
    }
}