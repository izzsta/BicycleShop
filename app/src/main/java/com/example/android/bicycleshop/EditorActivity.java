package com.example.android.bicycleshop;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.bicycleshop.data.BicycleContract;
import com.example.android.bicycleshop.data.BicycleContract.BicycleEntry;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    //initialise the variables that will be used here
    private Uri mCurrentBicycleUri;

    //initialize a Cursor loader
    private static final int EDITOR_BICYCLE_LOADER = 0;

    //intialize the spinner
    private Spinner mTypeSpinner;

    //initialize the type
    private int mType = BicycleEntry.TYPE_UNKNOWN;

    //intialise the other EditText fields
    private EditText mModelEditText;
    private EditText mPriceEditText;
    private int mQuantity;
    private EditText mSupplierEditText;

    //set up the onTouchListener variable, default is false
    private boolean mBicycleHasChanged = false;
    //set up the on TouchListener method, to be used later in onCreate
    private View.OnTouchListener mTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mBicycleHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        //examine the intent that led to opening the editor
        //if the intent does not contain a URI, then the user is trying to add a new bicycle
        //if the intent does contain a URI, then the user is trying to edit an existing bicycle
        Intent intent = getIntent();
        mCurrentBicycleUri = intent.getData();

        if (mCurrentBicycleUri == null) {
            setTitle(R.string.add_new_bicycle);

            //call the onPrepareOptions method defined below, and remove the 'delete' option
            invalidateOptionsMenu();
        } else {
            setTitle(R.string.edit_bicycle);

            //if the user wants to edit existing bicycle data, we need to query the database
            //we need to query the database to obtain existing data
            //so here we intialise the Cursor Loader
            getLoaderManager().initLoader(EDITOR_BICYCLE_LOADER, null, this);
        }

        //set up spinner
        mTypeSpinner = (Spinner) findViewById(R.id.spinner_type);
        setupSpinner();

        //set other views
        mModelEditText = (EditText)findViewById(R.id.model);
        mPriceEditText = (EditText)findViewById(R.id.price);
        mSupplierEditText = (EditText)findViewById(R.id.supplier);

        //set up onTouchListeners on each view, so we know when something has been changed
        mModelEditText.setOnTouchListener(mTouchListener);
        mTypeSpinner.setOnTouchListener(mTouchListener);
        //TODO mQuantity.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
    }

    //set up spinner for 'Type'
    private void setupSpinner(){
        //set up array adapter to take spinner options
        ArrayAdapter typeSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_type_options, android.R.layout.simple_spinner_item);
        typeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        //apply spinner to adapter
        mTypeSpinner.setAdapter(typeSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.type_tourer))) {
                        mType = BicycleEntry.TYPE_TOURING;
                    } else if (selection.equals(getString(R.string.type_road))) {
                        mType = BicycleEntry.TYPE_ROAD;
                    } else if (selection.equals(getString(R.string.type_hybrid))) {
                        mType = BicycleEntry.TYPE_HYBRID;
                    } else if (selection.equals(getString(R.string.type_triathlon))) {
                        mType = BicycleEntry.TYPE_TRIATHLON;
                    } else if (selection.equals(getString(R.string.type_mountain))) {
                        mType = BicycleEntry.TYPE_MOUNTAIN;
                    } else {
                        mType = BicycleEntry.TYPE_UNKNOWN;
                    }
                }
            }

            // AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mType = BicycleEntry.TYPE_UNKNOWN;
            }
        });
    }

    //inflate the menu, with options for save and delete in the Edit Bicycle screen
    //and just the option for delete in the Add Bicycle screen
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        return true;
    }
    //onPrepareOptionsMenu is called when invalidateOptionsMenu is called in onCreate
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mCurrentBicycleUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    //define the required actions upon selecting the various options in the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_save:
                //if save selected, save bicycle to database, then exit the activity
                saveBicycle();
                finish();
                return true;
            case R.id.action_delete:
                deletionDialogue();
                return true;
            //TODO set up R.id.home, which uses ifBicycleHasChanged
        }

        //TODO onBackPressed, which uses ifBicycleHasChanged

        return super.onOptionsItemSelected(item);
    }

    //set up method for INSERTING or UPDATING a bicycle
    private void saveBicycle(){
        //retrieve input from edit text fields, and check if they're blank
        String modelString = mModelEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();

        if (mCurrentBicycleUri == null &&
            TextUtils.isEmpty(modelString) && TextUtils.isEmpty(priceString)
                    && TextUtils.isEmpty(supplierString) && mType == BicycleEntry.TYPE_UNKNOWN) {
            //if blank, exit as no changes need be changed
            return;
        }

        //to enable us to pass data into the database, we need to create a ContentValues object
        ContentValues values = new ContentValues();
        values.put(BicycleEntry.COLUMN_BIKE_MODEL, modelString);
        values.put(BicycleEntry.COLUMN_BIKE_TYPE, mType);
        //TODO values.put(BicycleEntry.COLUMN_QUANTITY, mQuantity);
        values.put(BicycleEntry.COLUMN_PRICE, priceString);
        values.put(BicycleEntry.COLUMN_SUPPLIER, supplierString);

        //TODO if items are not input by users, dont try to parse

        //if this is a new bicycle, insert into database, otherwise update the database
        //notify the user of the success/failure of each action
        if (mCurrentBicycleUri == null) {
            Uri newBicycleUri = getContentResolver().insert(BicycleContract.CONTENT_URI, values);

            if(newBicycleUri == null) {
                Toast.makeText(this, getString(R.string.error_inserting),
                        Toast.LENGTH_SHORT).show();
            } else {
                    Toast.makeText(this, getString(R.string.insertion_complete),
                            Toast.LENGTH_SHORT).show();
            }
        } else {
            int updatedRows = getContentResolver().update(mCurrentBicycleUri, values,null,null);
            if(updatedRows == 0) {
                Toast.makeText(this, getString(R.string.error_updating),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.update_complete),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    //set up a method to DELETE a bicycle from the database
    private void deleteBicycle(){
        //can only delete an existing bicycle, so exit the activity otherwise
        if (mCurrentBicycleUri != null) {
            int rowsDeleted = getContentResolver().delete(mCurrentBicycleUri, null, null);
            if(rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.delete_error),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.delete_complete),
                        Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }


    //use CursorLoader to QUERY the database, needed when editing an existing bicycle
    //first get the details of that particular bicycle
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                BicycleEntry.COLUMN_BIKE_MODEL,
                BicycleEntry.COLUMN_BIKE_TYPE,
                BicycleEntry.COLUMN_PRICE,
                BicycleEntry.COLUMN_QUANTITY,
                BicycleEntry.COLUMN_SUPPLIER };

        return new CursorLoader(this,
                mCurrentBicycleUri,
                projection,
                null,
                null,
                null);
        }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (data == null || data.getCount() < 1) {
            return;
        }

        //extract data from first (and only) row of cursor
        if (data.moveToFirst()){
            int modelColumnIndex = data.getColumnIndex(BicycleEntry.COLUMN_BIKE_MODEL);
            int typeColumnIndex = data.getColumnIndex(BicycleEntry.COLUMN_BIKE_TYPE);
            int quantityColumnIndex = data.getColumnIndex(BicycleEntry.COLUMN_QUANTITY);
            int priceColumnIndex = data.getColumnIndex(BicycleEntry.COLUMN_PRICE);
            int supplierColumnIndex = data.getColumnIndex(BicycleEntry.COLUMN_SUPPLIER);

            String bikeModel = data.getString(modelColumnIndex);
            int bikeType = data.getInt(typeColumnIndex);
            //TODO deal with quantity
            int bikeQuantity = data.getInt(quantityColumnIndex);
            String bikePrice = data.getString(priceColumnIndex);
            String bikeSupplier = data.getString(supplierColumnIndex);

            mModelEditText.setText(bikeModel);
            mPriceEditText.setText(bikePrice);
            mSupplierEditText.setText(bikeSupplier);
            //set up a switch statement for type, that will display the right spinner selection
            switch(bikeType){
                case BicycleEntry.TYPE_TOURING:
                    mTypeSpinner.setSelection(0);
                    break;
                case BicycleEntry.TYPE_ROAD:
                    mTypeSpinner.setSelection(1);
                    break;
                case BicycleEntry.TYPE_TRIATHLON:
                    mTypeSpinner.setSelection(2);
                    break;
                case BicycleEntry.TYPE_MOUNTAIN:
                    mTypeSpinner.setSelection(3);
                    break;
                case BicycleEntry.TYPE_HYBRID:
                    mTypeSpinner.setSelection(4);
                    break;
                default:
                    mTypeSpinner.setSelection(5);
            }

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //reset fields if loader is invalidated
        mModelEditText.setText("");
        mTypeSpinner.setSelection(5);
        mPriceEditText.setText("");
        mSupplierEditText.setText("");
    }

    //dialogue that requires user to confirm the deletion of a bicycle
    private void deletionDialogue(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_confirmation);
        //if confirmed, delete the Bicycle
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteBicycle();
            }
        });
        //if cancelled, dismiss the dialogue and return to edit screen
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
        //TODO show a dialogue that warns users about unsaved changes

        //TODO prompt user to confirm that they want to delete the bicycle

        //TODO on back button pressed, warn user of unsaved changes


}
