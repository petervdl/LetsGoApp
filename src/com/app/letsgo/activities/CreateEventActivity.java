package com.app.letsgo.activities;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.app.letsgo.R;
import com.app.letsgo.adapters.PlacesAdapter;
import com.app.letsgo.helpers.Utils;
import com.app.letsgo.models.LocalEvent;
import com.app.letsgo.models.Location;
import com.app.letsgo.models.Place;
import com.parse.ParseUser;

public class CreateEventActivity extends FragmentActivity {
	EditText etEventName;
	EditText etEventType;
	EditText etDescription;
	EditText etCost;
	EditText etStartDate;
	EditText etStartTime;
	EditText etEndDate;
	EditText etEndTime;
	GregorianCalendar startDate;
	GregorianCalendar endDate;

	AutoCompleteTextView etLocation;
	GeoCodeAsyncTask geoAsyncTask;
	LocalEvent event;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		setContentView(R.layout.activity_create_event);

		// make sure to call super after setting the content view
		super.onCreate(savedInstanceState);

		etEventName = (EditText) findViewById(R.id.etEventName);
		etEventType = (EditText) findViewById(R.id.etEventType);
		etDescription = (EditText) findViewById(R.id.etDescription);
		etCost = (EditText) findViewById(R.id.etCost);
		etStartDate = (EditText) findViewById(R.id.etStartDate);
		etStartTime = (EditText) findViewById(R.id.etStartTime);
		etEndDate = (EditText) findViewById(R.id.etEndDate);
		etEndTime = (EditText) findViewById(R.id.etEndTime);

		startDate = new GregorianCalendar();
		endDate = new GregorianCalendar();	   	

		etLocation = (AutoCompleteTextView) findViewById(R.id.etLocation);

		setLocation();
	}

	/** 
	 * call back to get location info from places api
	 */
	private void setLocation() {
		final PlacesAdapter adapter = new PlacesAdapter(this, R.layout.place_list);
		etLocation.setAdapter(adapter);

		etLocation.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				Place place = adapter.getPlace(position);
				geoAsyncTask = new GeoCodeAsyncTask();
				geoAsyncTask.execute(place);
				Log.d(Utils.LOG_TAG,  "CreateEvent.setLocation(): " + place.getAddress());
			}
		});
	}

	public static class GeoCodeAsyncTask extends AsyncTask<Place, Integer, Location> {
		@Override
		protected Location doInBackground(Place... params) {
			Place place = params[0];
			return Utils.getGeocode(place);
		}

		@Override
		protected void onPostExecute(Location location) {
			Log.d(Utils.LOG_TAG, "Location: " + location.toString());
		}
	}

	public void onCreateEvent(View v) {

		event = new LocalEvent();
		// TODO: add validation later
		// for now all fields are set default values if the field is not entered
		event.setEventName("party");
		if (!Utils.isNull(etEventName)) {
			event.setEventName(etEventName.getText().toString());
		}
		event.setEventType("music");
		if (!Utils.isNull(etEventType)) {
			event.setEventType(etEventType.getText().toString());
		}

		saveDates();

		if (!Utils.isNull(etCost)) {
			try {
				Double cost = Double.parseDouble(etCost.getText().toString());
				event.setCost(cost);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				event.setCost(0);
			}
		} else event.setCost(0); 

		// set description if it's entered
		event.setDescription("More details...");
		if (!Utils.isNull(etDescription)) {
			event.setDescription(etDescription.getText().toString());
		}

		ParseUser currentUser = ParseUser.getCurrentUser();
		event.setCreatedBy(currentUser);

		if (geoAsyncTask != null) {
			try {
				event.setLocation(geoAsyncTask.get());
				Log.d(Utils.LOG_TAG,  "Location:" + event.getLocation().toString());
			} catch (Exception e) {
				Log.d(Utils.LOG_TAG,  "Failed to get location");
			}
		}

		// TODO: default to 0 for now
		event.setCost(0);  // default to free
		event.setUpCount(0);
		event.setDownCount(0);
		event.put("public", true); // default to public
		event.saveInBackground();

		addToCalendar();
		finish();
	}

	/**
	 *  Add the newly created event into calendar
	 */
	public void addToCalendar() {
		Intent intent = new Intent(Intent.ACTION_INSERT);
		intent.setData(CalendarContract.Events.CONTENT_URI);
		intent.setType("vnd.android.cursor.item/event");
		intent.putExtra(Events.TITLE, etEventName.getText().toString());
		intent.putExtra(Events.EVENT_LOCATION, etLocation.getText().toString());
		if (!Utils.isNull(etDescription)) {
			intent.putExtra(Events.DESCRIPTION, etDescription.getText().toString());
		}

		// Setting dates    	
		intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startDate);
		intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endDate);
		startActivity(intent);     	
	}

	// TODO: return to Map view
	public void onCancel(View v) {
		finish();  	
	}

	public class DatePickerFragment extends DialogFragment
	implements DatePickerDialog.OnDateSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the current date as the default date in the picker
			final Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);

			// Create a new instance of DatePickerDialog and return it
			return new DatePickerDialog(getActivity(), this, year, month, day);
		}

		@Override
		public void onDateSet(DatePicker view, int year, int month, int day) {
			showDate(etStartDate, year, month, day);
			startDate.set(GregorianCalendar.YEAR, year);
			startDate.set(GregorianCalendar.MONTH, month);
			startDate.set(GregorianCalendar.DAY_OF_MONTH, day);
		}		
	}

	public class TimePickerFragment extends DialogFragment
	implements TimePickerDialog.OnTimeSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the current time as the default values for the picker
			final Calendar c = Calendar.getInstance();
			int hour = c.get(Calendar.HOUR_OF_DAY);
			int minute = c.get(Calendar.MINUTE);

			// Create a new instance of TimePickerDialog and return it
			return new TimePickerDialog(getActivity(), this, hour, minute,
					DateFormat.is24HourFormat(getActivity()));
		}

		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			showTime(etStartTime, hourOfDay, minute);
			startDate.set(GregorianCalendar.HOUR, hourOfDay);
			startDate.set(GregorianCalendar.MINUTE, minute);
		}
	}

	// TODO: need to refactor this!
	public class EndDatePickerFragment extends DialogFragment
	implements DatePickerDialog.OnDateSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the current date as the default date in the picker
			final Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);

			// Create a new instance of DatePickerDialog and return it
			return new DatePickerDialog(getActivity(), this, year, month, day);
		}

		@Override
		public void onDateSet(DatePicker view, int year, int month, int day) {
			showDate(etEndDate, year, month, day);
			endDate.set(GregorianCalendar.YEAR, year);
			endDate.set(GregorianCalendar.MONTH, month);
			endDate.set(GregorianCalendar.DAY_OF_MONTH, day);
		}

	}

	// TODO: need to refactor this!
	public class EndTimePickerFragment extends DialogFragment
	implements TimePickerDialog.OnTimeSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the current time as the default values for the picker
			final Calendar c = Calendar.getInstance();
			int hour = c.get(Calendar.HOUR_OF_DAY);
			int minute = c.get(Calendar.MINUTE);

			// Create a new instance of TimePickerDialog and return it
			return new TimePickerDialog(getActivity(), this, hour, minute,
					DateFormat.is24HourFormat(getActivity()));
		}

		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			showTime(etEndTime, hourOfDay, minute);
			endDate.set(GregorianCalendar.HOUR, hourOfDay);
			endDate.set(GregorianCalendar.MINUTE, minute);
		}
	}

	public void showDate(EditText date, int year, int month, int day) {
		date.setText(new StringBuilder().append(month + 1)
				.append("/").append(day).append("/").append(year));
	}

	public void showTime(EditText time, int hour, int minute) {
		time.setText(new StringBuilder().append(hour).append(":").append(minute));
	}

	public void showStartDatePickerDialog(View v) {
		DialogFragment newFragment = new DatePickerFragment();
		newFragment.show(getSupportFragmentManager(), "datePicker");	
	}

	public void showStartTimePickerDialog(View v) {
		DialogFragment newFragment = new TimePickerFragment();
		newFragment.show(getSupportFragmentManager(), "timePicker");
	}

	// TODO: refactor this
	public void showEndDatePickerDialog(View v) {
		DialogFragment newFragment = new EndDatePickerFragment();
		newFragment.show(getSupportFragmentManager(), "datePicker");	
	}

	public void showEndTimePickerDialog(View v) {
		DialogFragment newFragment = new EndTimePickerFragment();
		newFragment.show(getSupportFragmentManager(), "timePicker");
	}

	public void saveDates() {
		event.setStartDate("7/18/14");
		if (!Utils.isNull(etStartDate)) {
			event.setStartDate(etStartDate.getText().toString());
		}
		event.setStartTime("16:00");
		if (!Utils.isNull(etStartTime)) {
			event.setStartTime(etStartTime.getText().toString());
		}
		event.setEndDate("7/18/14");
		if (!Utils.isNull(etEndDate)) {
			event.setEndDate(etEndDate.getText().toString());
		}
		event.setEndTime("22:00");
		if (!Utils.isNull(etEndTime)) {
			event.setEndTime(etEndTime.getText().toString());
		}
	}


}
