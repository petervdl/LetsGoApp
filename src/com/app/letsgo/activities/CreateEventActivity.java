package com.app.letsgo.activities;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;

import com.app.letsgo.R;
import com.app.letsgo.adapters.PlacesAdapter;
import com.app.letsgo.fragments.DatePickerFragment;
import com.app.letsgo.fragments.TimePickerFragment;
import com.app.letsgo.helpers.Utils;
import com.app.letsgo.models.LocalEvent;
import com.app.letsgo.models.LocalEventParcel;
import com.app.letsgo.models.Location;
import com.app.letsgo.models.Place;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class CreateEventActivity extends FragmentActivity {
	EditText etEventName;
	Spinner spEventType;
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
		spEventType = (Spinner) findViewById(R.id.spEventType);
		etDescription = (EditText) findViewById(R.id.etDescription);
		etCost = (EditText) findViewById(R.id.etCost);
		etStartDate = (EditText) findViewById(R.id.etStartDate);
		etStartTime = (EditText) findViewById(R.id.etStartTime);
		etEndDate = (EditText) findViewById(R.id.etEndDate);
		etEndTime = (EditText) findViewById(R.id.etEndTime);

		startDate = new GregorianCalendar();
		endDate = new GregorianCalendar();	   	

		etLocation = (AutoCompleteTextView) findViewById(R.id.etLocation);
		Utils.setupEventType(this, spEventType);

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

	public Activity getContext() {
		return this;
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
		if (spEventType != null) {
			event.setEventType(spEventType.getSelectedItem().toString());
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
		
		event.saveInBackground(new SaveCallback(){
			@Override
			public void done(ParseException e) {
				if(e == null){
					Log.d("OBJECT_SAVE", "Event successfully saved.");
					
					Utils.addToCalendar(getContext(), 
							etEventName.getText().toString(), 
							etLocation.getText().toString(), 
							etDescription.getText().toString(), 
							etStartDate.getText().toString(), 
							etEndDate.getText().toString()); 
					
					Intent data = new Intent();
					LocalEventParcel parcel = new LocalEventParcel(event);
					data.putExtra("event", parcel);
					setResult(RESULT_OK, data);
					finish();
				} else {
					Log.e("OBJECT NOT SAVED", "Event not successfully saved");
				}				
			}			
		});		
	}


	/**
	 *  Returns t/o Map view when click on Cancel button
	 * @param v
	 */
	public void onCancel(View v) {
		finish();  	
	}
	
	public void showStartDatePickerDialog(View v) {
		Utils.showDatePickerDialog(this, etStartDate, startDate);
	}

	public void showStartTimePickerDialog(View v) {
		Utils.showTimePickerDialog(this, etStartTime, startDate);
	}

	// show datepicker for end date
	public void showEndDatePickerDialog(View v) {
		Utils.showDatePickerDialog(this, etEndDate, endDate);
	}

	public void showEndTimePickerDialog(View v) {
		Utils.showTimePickerDialog(this, etEndTime, endDate);
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
