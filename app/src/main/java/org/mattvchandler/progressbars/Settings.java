package org.mattvchandler.progressbars;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import org.mattvchandler.progressbars.databinding.ActivitySettingsBinding;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/*
Copyright (C) 2017 Matthew Chandler

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

// Settings for each timer
public class Settings extends Dynamic_theme_activity implements Precision_dialog_frag.NoticeDialogListener,
                                                                DatePickerDialog.OnDateSetListener,
                                                                TimePickerDialog.OnTimeSetListener
{
    public static final String EXTRA_EDIT_ROW_ID = "org.mattvchandler.progressbars.EDIT_ROW_ID";
    public static final String RESULT_NEW_DATA = "org.mattvchandler.progressbars.RESULT_ROW_ID";
    public static final String RESULT_OLD_DATA = "org.mattvchandler.progressbars.RESULT_OLD_DATA";

    private static final String STATE_DATA = "data";
    private static final String STATE_SAVE_DATA = "save_data";

    private ActivitySettingsBinding binding;
    private Progress_bar_data data;
    private Progress_bar_data save_data;

    private EditText date_time_dialog_target;

    private String date_format;
    private String time_format;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings);
        setSupportActionBar(binding.progressBarToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // we'll reference these a lot, so look them up now
        date_format = PreferenceManager.getDefaultSharedPreferences(this).getString("date_format",
                getResources().getString(R.string.pref_date_format_default));
        time_format = getResources().getString(R.string.time_format);

        // set up timezone spinners
        ArrayAdapter<String> tz_adapter = new ArrayAdapter<>(this, R.layout.right_aligned_spinner, TimeZone.getAvailableIDs());
        tz_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.startTz.setAdapter(tz_adapter);
        binding.endTz.setAdapter(tz_adapter);

        View.OnFocusChangeListener date_listener = new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                // check format when date entry loses focus
                if(!hasFocus)
                {
                    // attempt to parse current text
                    String new_date = ((EditText)v).getText().toString();
                    SimpleDateFormat df = new SimpleDateFormat(date_format , Locale.US);

                    Date date = df.parse(new_date, new ParsePosition(0));
                    if(date == null)
                    {
                        // couldn't parse, show message
                        Toast.makeText(Settings.this, getResources().getString(R.string.invalid_date,
                                  new_date, date_format), Toast.LENGTH_LONG).show();

                        // replace with old value, so field contains valid data
                        if(v.getId() == R.id.start_date_sel)
                        {
                            df.setTimeZone(TimeZone.getTimeZone(data.start_tz));
                            ((EditText) v).setText(df.format(new Date(data.start_time * 1000)));
                        }
                        else if(v.getId() == R.id.end_date_sel)
                        {
                            df.setTimeZone(TimeZone.getTimeZone(data.end_tz));
                            ((EditText) v).setText(df.format(new Date(data.end_time * 1000)));
                        }
                    }
                    else
                    {
                        // new value is valid, set it.
                        new_date = df.format(date);
                        ((EditText) v).setText(new_date);
                    }
                }
            }
        };

        View.OnFocusChangeListener time_listener = new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                // check format when time entry loses focus
                if(!hasFocus)
                {
                    // attempt to parse current text
                    String new_time = ((EditText)v).getText().toString();
                    SimpleDateFormat df = new SimpleDateFormat(time_format, Locale.US);

                    Date time = df.parse(new_time, new ParsePosition(0));
                    if(time == null)
                    {
                        // couldn't parse, show message
                        Toast.makeText(Settings.this, getResources().getString(R.string.invalid_time,
                                                      new_time, time_format),
                                Toast.LENGTH_LONG).show();

                        // replace with old value, so field contains valid data
                        if(v.getId() == R.id.start_time_sel)
                        {
                            df.setTimeZone(TimeZone.getTimeZone(data.start_tz));
                            ((EditText) v).setText(df.format(new Date(data.start_time * 1000)));
                        }
                        else if(v.getId() == R.id.end_time_sel)
                        {
                            df.setTimeZone(TimeZone.getTimeZone(data.end_tz));
                            ((EditText) v).setText(df.format(new Date(data.end_time * 1000)));
                        }
                    }
                    else
                    {
                        // new value is valid, set it.
                        new_time = df.format(time);
                        ((EditText) v).setText(new_time);
                    }
                }
            }
        };

        // set above listeners on time and date fields
        binding.startTimeSel.setOnFocusChangeListener(time_listener);
        binding.endTimeSel.setOnFocusChangeListener(time_listener);

        binding.startDateSel.setOnFocusChangeListener(date_listener);
        binding.endDateSel.setOnFocusChangeListener(date_listener);

        // only run this on 1st creation
        if(savedInstanceState == null)
        {
            long rowid = getIntent().getLongExtra(EXTRA_EDIT_ROW_ID, -1);

            // no rowid passed? make a new one
            if(rowid < 0)
            {
                setTitle(R.string.add_title);
                data = new Progress_bar_data(this);
            }
            else
            {
                // get data from row
                setTitle(R.string.edit_title);
                data = new Progress_bar_data(this, rowid);
            }
            save_data = new Progress_bar_data(data);
        }
        else
        {
            // reload old and current data from save state
            data = (Progress_bar_data)savedInstanceState.getSerializable(STATE_DATA);
            save_data = (Progress_bar_data)savedInstanceState.getSerializable(STATE_SAVE_DATA);
        }

        // populate timezones and set selected values
        binding.setData(data);

        int found = 0;
        for(int i = 0; i < tz_adapter.getCount(); ++i)
        {
            if(tz_adapter.getItem(i).equals(data.start_tz))
            {
                binding.startTz.setSelection(i);
                ++found;
            }
            if(tz_adapter.getItem(i).equals(data.end_tz))
            {
                binding.endTz.setSelection(i);
                ++found;
            }
            if(found == 2)
                break;
        }

        // populate date/time widget values
        SimpleDateFormat df_date = new SimpleDateFormat(date_format, Locale.US);
        SimpleDateFormat df_time = new SimpleDateFormat(time_format, Locale.US);

        Date start_date = new Date(data.start_time * 1000);
        df_date.setTimeZone(TimeZone.getTimeZone(data.start_tz));
        df_time.setTimeZone(TimeZone.getTimeZone(data.start_tz));
        binding.startDateSel.setText(df_date.format(start_date));
        binding.startTimeSel.setText(df_time.format(start_date));

        Date end_date = new Date(data.end_time * 1000);
        df_date.setTimeZone(TimeZone.getTimeZone(data.end_tz));
        df_time.setTimeZone(TimeZone.getTimeZone(data.end_tz));
        binding.endDateSel.setText(df_date.format(end_date));
        binding.endTimeSel.setText(df_time.format(end_date));
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // check for date format change
        String old_date_format = date_format;
        date_format = PreferenceManager.getDefaultSharedPreferences(this).getString("date_format",
                getResources().getString(R.string.pref_date_format_default));

        if(!old_date_format.equals(date_format))
        {
            // date format has changed. get formatter for old and new formats
            SimpleDateFormat new_df = new SimpleDateFormat(date_format, Locale.US);
            SimpleDateFormat old_df = new SimpleDateFormat(old_date_format, Locale.US);

            String date;
            Date date_obj;

            // parse date as old format, replace w/ new
            date = binding.startDateSel.getText().toString();
            date_obj = old_df.parse(date, new ParsePosition(0));
            if(date_obj != null)
            {
                date = new_df.format(date_obj);
                binding.startDateSel.setText(date);
            }

            date = binding.endDateSel.getText().toString();
            date_obj = old_df.parse(date, new ParsePosition(0));
            if(date_obj != null)
            {
                date = new_df.format(date_obj);
                binding.endDateSel.setText(date);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out)
    {
        // save all data to be restored
        store_widgets_to_data();
        super.onSaveInstanceState(out);
        out.putSerializable(STATE_DATA, data);
        out.putSerializable(STATE_SAVE_DATA, save_data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.settings_action_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.save_butt:

                // dump all widget data into data struct
                if(!store_widgets_to_data())
                    return true;

                // insert new or update existing row
                Intent intent = new Intent();
                if(data.rowid < 0)
                {
                    data.insert(this);
                    // don't send old data so caller knows that we added a new row
                }
                else
                {
                    data.update(this);
                    // send back old data
                    intent.putExtra(RESULT_OLD_DATA, save_data);
                }

                // send back new data
                intent.putExtra(RESULT_NEW_DATA, data);

                setResult(RESULT_OK, intent);
                finish();
                return true;

            case R.id.settings:
                startActivity(new Intent(this, Progress_bar_prefs.class));
                return true;
        }
        return false;
    }

    // dump all widget data into data obj
    private boolean store_widgets_to_data()
    {
        boolean errors = false;
        // precision data has been stored through its callback already
        data.start_tz = binding.startTz.getSelectedItem().toString();
        data.end_tz = binding.endTz.getSelectedItem().toString();

        // validate dates and times
        SimpleDateFormat datetime_df = new SimpleDateFormat(date_format + " " + time_format, Locale.US);
        SimpleDateFormat date_df = new SimpleDateFormat(date_format, Locale.US);
        SimpleDateFormat time_df = new SimpleDateFormat(time_format, Locale.US);

        datetime_df.setTimeZone(TimeZone.getTimeZone(data.start_tz));
        date_df.setTimeZone(TimeZone.getTimeZone(data.start_tz));
        time_df.setTimeZone(TimeZone.getTimeZone(data.start_tz));

        Date start_date = date_df.parse(binding.startDateSel.getText().toString(), new ParsePosition((0)));
        Date start_time = time_df.parse(binding.startTimeSel.getText().toString(), new ParsePosition((0)));

        if(start_date == null)
        {
            Toast.makeText(Settings.this, getResources().getString(R.string.invalid_date,
                                          binding.startDateSel.getText(), date_format),
                    Toast.LENGTH_LONG).show();

            errors = true;
        }
        if(start_time == null)
        {
            Toast.makeText(Settings.this, getResources().getString(R.string.invalid_time,
                                          binding.startTimeSel.getText(), time_format),
                    Toast.LENGTH_LONG).show();

            errors = true;
        }

        if(start_date != null && start_time!= null)
        {
            data.start_time = datetime_df.parse(binding.startDateSel.getText().toString() + " " +
                                                binding.startTimeSel.getText().toString(),
                    new ParsePosition((0))).getTime() / 1000;
        }

        datetime_df.setTimeZone(TimeZone.getTimeZone(data.end_tz));
        date_df.setTimeZone(TimeZone.getTimeZone(data.end_tz));
        time_df.setTimeZone(TimeZone.getTimeZone(data.end_tz));

        Date end_date = date_df.parse(binding.endDateSel.getText().toString(), new ParsePosition((0)));
        Date end_time = time_df.parse(binding.endTimeSel.getText().toString(), new ParsePosition((0)));

        if(end_date == null)
        {
            Toast.makeText(Settings.this, getResources().getString(R.string.invalid_date,
                                          binding.endDateSel.getText(), date_format),
                    Toast.LENGTH_LONG).show();

            errors = true;
        }
        if(end_time == null)
        {
            Toast.makeText(Settings.this, getResources().getString(R.string.invalid_time,
                                          binding.endTimeSel.getText(), time_format),
                    Toast.LENGTH_LONG).show();

            errors = true;
        }

        if(end_date != null && end_time != null)
        {
            data.end_time = datetime_df.parse(binding.endDateSel.getText().toString() + " " +
                                              binding.endTimeSel.getText().toString(),
                    new ParsePosition((0))).getTime() / 1000;
        }

        if(data.end_time < data.start_time)
        {
            Toast.makeText(this, getResources().getString(R.string.end_before_start_err), Toast.LENGTH_LONG).show();
            errors = true;
        }

        // get all 'easy' data
        data.title = binding.title.getText().toString();
        data.pre_text = binding.preText.getText().toString();
        data.start_text = binding.startText.getText().toString();
        data.countdown_text = binding.countdownText.getText().toString();
        data.complete_text = binding.completeText.getText().toString();
        data.post_text = binding.postText.getText().toString();

        data.show_progress = binding.showProgress.isChecked();
        data.show_start = binding.showStart.isChecked();
        data.show_end = binding.showEnd.isChecked();
        data.show_years = binding.showYears.isChecked();
        data.show_months = binding.showMonths.isChecked();
        data.show_weeks = binding.showWeeks.isChecked();
        data.show_days = binding.showDays.isChecked();
        data.show_hours = binding.showHours.isChecked();
        data.show_minutes = binding.showMinutes.isChecked();
        data.show_seconds = binding.showSeconds.isChecked();
        data.terminate = binding.terminate.isChecked();
        data.notify_start = binding.notifyStart.isChecked();
        data.notify_end = binding.notifyEnd.isChecked();

        return !errors;
    }

    // date picker dialog
    public static class Datepicker_frag extends DialogFragment
    {
        public static final String STORE_DATE = "STORE_DATE";
        public static final String DATE = "DATE";

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle saved_instance_state)
        {
            // parse from current widget text
            int year, month, day;

            Calendar cal = Calendar.getInstance();

            String date_format = PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getString("date_format", getResources().getString(R.string.pref_date_format_default));

            String date = getArguments().getString(DATE);

            SimpleDateFormat df = new SimpleDateFormat(date_format, Locale.US);
            Date date_obj = df.parse(date, new ParsePosition(0));
            if(date_obj == null)
            {
                // couldn't parse
                Toast.makeText(getActivity(), getResources().getString(R.string.invalid_date, date, date_format),
                        Toast.LENGTH_LONG).show();

                // set to stored date
                cal.setTimeInMillis(getArguments().getLong(STORE_DATE, 0) * 1000);
            }
            else
            {
                cal.setTime(date_obj);
            }

            year = cal.get(Calendar.YEAR);
            month = cal.get(Calendar.MONTH);
            day = cal.get(Calendar.DAY_OF_MONTH);

            return new DatePickerDialog(getActivity(), (Settings) getActivity(), year, month, day);
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day)
    {
        // build new string from returned data
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);

        SimpleDateFormat df = new SimpleDateFormat(date_format, Locale.US);
        date_time_dialog_target.setText(df.format(cal.getTime()));
    }

    public static class Timepicker_frag extends DialogFragment
    {
        public static final String STORE_TIME = "STORE_TIME";
        public static final String TIME = "TIME";

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle saved_instance_state)
        {
            // parse from current widget text
            int hour, minute;

            Calendar cal = Calendar.getInstance();

            String time_format = getResources().getString(R.string.time_format);

            String time = getArguments().getString(TIME);

            SimpleDateFormat df = new SimpleDateFormat(time_format, Locale.US);
            Date date_obj = df.parse(time, new ParsePosition(0));
            if(date_obj == null)
            {
                // couldn't parse
                Toast.makeText(getActivity(), getResources().getString(R.string.invalid_time, time, time_format),
                        Toast.LENGTH_LONG).show();

                // set to stored date
                cal.setTimeInMillis(getArguments().getLong(STORE_TIME, 0) * 1000);
            }
            else
            {
                cal.setTime(date_obj);
            }

            hour = cal.get(Calendar.HOUR_OF_DAY);
            minute = cal.get(Calendar.MINUTE);

            return new TimePickerDialog(getActivity(), (Settings) getActivity(), hour, minute, true);
        }
    }

    @Override
    public void onTimeSet(TimePicker view, int hour, int minute)
    {
        // build new string from returned data
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        SimpleDateFormat df = new SimpleDateFormat(time_format, Locale.US);
        date_time_dialog_target.setText(df.format(cal.getTime()));
    }

    public void on_start_cal_butt(View view)
    {
        // create a calendar dialog, pass current date string
        date_time_dialog_target = binding.startDateSel;
        Datepicker_frag frag = new Datepicker_frag();
        Bundle args = new Bundle();
        args.putString(Datepicker_frag.DATE, binding.startDateSel.getText().toString());
        args.putLong(Datepicker_frag.STORE_DATE, data.start_time);
        frag.setArguments(args);
        frag.show(getSupportFragmentManager(), "start_date_picker");
    }

    public void on_start_clock_butt(View view)
    {
        // create a clock dialog, pass current time string
        date_time_dialog_target = binding.startTimeSel;
        Timepicker_frag frag = new Timepicker_frag();
        Bundle args = new Bundle();
        args.putString(Timepicker_frag.TIME, binding.startTimeSel.getText().toString());
        args.putLong(Timepicker_frag.STORE_TIME, data.start_time);
        frag.setArguments(args);
        frag.show(getSupportFragmentManager(), "start_time_picker");
    }

    public void on_end_cal_butt(View view)
    {
        // create a calendar dialog, pass current date string
        date_time_dialog_target = binding.endDateSel;
        Datepicker_frag frag = new Datepicker_frag();
        Bundle args = new Bundle();
        args.putString(Datepicker_frag.DATE, binding.endDateSel.getText().toString());
        args.putLong(Datepicker_frag.STORE_DATE, data.end_time);
        frag.setArguments(args);
        frag.show(getSupportFragmentManager(), "end_date_picker");
    }

    public void on_end_clock_butt(View view)
    {
        // create a clock dialog, pass current time string
        date_time_dialog_target = binding.endTimeSel;
        Timepicker_frag frag = new Timepicker_frag();
        Bundle args = new Bundle();
        args.putString(Timepicker_frag.TIME, binding.endTimeSel.getText().toString());
        args.putLong(Timepicker_frag.STORE_TIME, data.end_time);
        frag.setArguments(args);
        frag.show(getSupportFragmentManager(), "end_time_picker");
    }

    public void on_precision_butt(View view)
    {
        Precision_dialog_frag d = new Precision_dialog_frag();
        Bundle args = new Bundle();
        args.putInt(Precision_dialog_frag.PRECISION_ARG, data.precision);
        d.setArguments(args);
        d.show(getSupportFragmentManager(), "precision");
    }

    // called when OK pressed on precision dialog
    @Override
    public void on_precision_dialog_positive(Precision_dialog_frag dialog)
    {
        // get and store the data
        data.precision = dialog.getValue();
        binding.precision.setText(String.valueOf(data.precision));
    }
}
