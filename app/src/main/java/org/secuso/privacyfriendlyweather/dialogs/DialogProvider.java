package org.secuso.privacyfriendlyweather.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.secuso.privacyfriendlyweather.HelpActivity;
import org.secuso.privacyfriendlyweather.R;
import org.secuso.privacyfriendlyweather.orm.City;
import org.secuso.privacyfriendlyweather.orm.CityToWatch;
import org.secuso.privacyfriendlyweather.orm.DatabaseHelper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides predefined dialogs.
 */
public class DialogProvider {

    private final String DEBUG_TAG = "dp_debug_tag";

    /**
     * Visual components
     */
    private LinearLayout addDialogLinearLayout;
    private AutoCompleteTextView addDialogEdtLocation;
    private ArrayAdapter<City> cityAdapter;
    private CheckBox addDialogCbSave;

    private DatabaseHelper dbHelper;
    private boolean isAddLocationDialogInitialized = false;
    private City addDialogSelectedCity;

    /**
     * Constructor.
     *
     * @param dbHelper The database helper is used to provide further features like look-ahead. If
     *                 no DatabaseHelper is provided (null), these features are not available.
     */
    public DialogProvider(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Initializes the linear layout as well as its components that is used for the
     * AddLocationDialog.
     *
     * @param context The context in which the dialog is to be displayed.
     */
    private void initLayoutForAddLocationDialog(final Context context) {
        final int TEXT_VIEW_ADD_CITY_TEXT_SIZE = 16;

        if (!isAddLocationDialogInitialized) {
            final float scale = context.getResources().getDisplayMetrics().density;
            int padding = 10;
            int paddingInPD = (int) (padding * scale + 0.5f);

            addDialogLinearLayout = new LinearLayout(context);
            addDialogLinearLayout.setOrientation(LinearLayout.VERTICAL);
            addDialogLinearLayout.setPadding(paddingInPD, paddingInPD, paddingInPD, paddingInPD);

            TextView addDialogTvMessage = new TextView(context);
            addDialogTvMessage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, TEXT_VIEW_ADD_CITY_TEXT_SIZE);
            addDialogTvMessage.setTextColor(Color.BLACK);
            addDialogTvMessage.setText(R.string.dialog_add_label);

            cityAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, new ArrayList<City>());
            addDialogEdtLocation = new AutoCompleteTextView(context);
            addDialogEdtLocation.setAdapter(cityAdapter);
            addDialogEdtLocation.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    final long LIST_LIMIT = 8L;
                    addDialogSelectedCity = null;
                    if (dbHelper != null) {
                        String content = addDialogEdtLocation.getText().toString();
                        if (content.length() > 3) {
                            // Get the matched cities
                            List<City> cities = dbHelper.getCitiesWhereNameLike(content, LIST_LIMIT);
                            // Set the drop down entries
                            cityAdapter.clear();
                            cityAdapter.addAll(cities);
                            addDialogEdtLocation.showDropDown();
                        } else {
                            addDialogEdtLocation.dismissDropDown();
                        }
                    }
                }
            });
            addDialogEdtLocation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    addDialogSelectedCity = (City) parent.getItemAtPosition(position);
                    Log.d(DEBUG_TAG, "ID: " + addDialogSelectedCity);
                }
            });
            // TODO: Show keyboard by default

            addDialogCbSave = new CheckBox(context);
            addDialogCbSave.setText(R.string.dialog_add_checkbox_save);

            addDialogLinearLayout.addView(addDialogTvMessage);
            addDialogLinearLayout.addView(addDialogEdtLocation);
            addDialogLinearLayout.addView(addDialogCbSave);
        }
    }

    /**
     * Handles the on click event of the 'Add' button of the 'Add city' dialog. It checks the input,
     * required further communication with the user (if no city was selected from a dropdown list)
     * and adds a city to the database.
     *
     * @param context The context in which the dialog is executed.
     * @return If the dialog is to be dismissed, true is returned else false.
     * @throws SQLException This execption will be thrown in case the entered location cannot be
     *                      added to the database.
     */
    private boolean handleOnBtnAddCityClick(Context context) throws SQLException {
        final String SUCCESS_MSG_TEMPLATE = context.getResources().getString(R.string.dialog_add_added_successfully_template);
        boolean dismissDialog;
        City addedCity = null;
        String trimmedInput = addDialogEdtLocation.getText().toString().trim();

        if (trimmedInput.length() > 0) {
            boolean storePermanent = addDialogCbSave.isChecked();
            // User selected a city from the dropdown, this is the nice case
            if (addDialogSelectedCity != null) {
                CityToWatch newCityToWatch = new CityToWatch(addDialogSelectedCity, storePermanent);
                dbHelper.getCityToWatchDao().create(newCityToWatch);
                addedCity = addDialogSelectedCity;
                dismissDialog = true;
            }
            // No city selected => Get matches of entered string and handle the cases
            else {
                List<City> foundCities = dbHelper.getCitiesWhereNameLike(trimmedInput, 2);
                // 1) No city found
                if (foundCities.size() == 0) {
                    String msg = context.getResources().getString(R.string.dialog_add_no_city_found);
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    dismissDialog = false;
                }
                // 2) 1 city found,
                else if (foundCities.size() == 1) {
                    City foundCity = foundCities.get(0);
                    dbHelper.getCityToWatchDao().create(new CityToWatch(foundCity, storePermanent));
                    addedCity = foundCity;
                    dismissDialog = true;
                }
                // 3) > 1 cities found
                else {
                    String msg = context.getResources().getString(R.string.dialog_add_too_many_cities_found);
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    dismissDialog = false;
                }
            }

        }
        // Cannot work with no input
        else {
            addDialogEdtLocation.setError(context.getResources().getString(R.string.dialog_add_textedit_error));
            dismissDialog = false;
        }

        // dismissDialog == true => A city was added, so show a message
        if (dismissDialog) {
            String successMsg = String.format(SUCCESS_MSG_TEMPLATE, addedCity.getCityName(), addedCity.getCountryCode());
            Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show();
        }

        return dismissDialog;
    }

    /**
     * @param context The context in which the dialog is to be displayed.
     * @return Returns an AlertDialog object with an info text for the first app start and two
     * buttons (1. Go to help page, 2. Close dialog).
     */
    public AlertDialog getFirstAppStartDialog(final Context context) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setMessage(R.string.dialog_first_app_start_message);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setNegativeButton(R.string.dialog_first_app_help_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final Activity activity = (Activity) context;
                Fragment fragment = new HelpActivity.HelpFragment();
                FragmentManager fragmentManager = activity.getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.main_content, fragment).addToBackStack(null).commit();
            }
        });
        dialogBuilder.setPositiveButton(R.string.dialog_first_app_close_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        return dialogBuilder.create();
    }

    /**
     * @param context The context in which the dialog is to be displayed.
     * @return Returns an AlertDialog with a text input field for a location and a CheckBox for
     * saving this location.
     */
    public AlertDialog getAddLocationDialog(final Context context) {
        if (!isAddLocationDialogInitialized) {
            initLayoutForAddLocationDialog(context);
        }

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setView(addDialogLinearLayout);

        // Buttons are added but not their onClick implementation; this is done further below
        // as explained here (as of 2016-07-27):
        // http://stackoverflow.com/questions/6275677/alert-dialog-in-android-should-not-dismiss
        dialogBuilder.setNegativeButton(R.string.dialog_add_add_button, null);
        dialogBuilder.setPositiveButton(R.string.dialog_add_close_button, null);

        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button btnClose = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button btnAdd = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                // Close click event
                btnClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                // Add click event
                btnAdd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean dismissDialog = false;
                        try {
                            dismissDialog = handleOnBtnAddCityClick(context);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        if (dismissDialog) {
                            dialog.dismiss();
                        }
                    }
                });
            }
        });

        return dialog;
    }

}