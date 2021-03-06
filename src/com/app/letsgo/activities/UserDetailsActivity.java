package com.app.letsgo.activities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.widget.ProfilePictureView;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

import com.app.letsgo.R;
import com.app.letsgo.LetsGoApplication;
import com.app.letsgo.LetsGoApplication.*;
import com.app.letsgo.models.User;


public class UserDetailsActivity extends Activity {
	private ProfilePictureView userProfilePictureView;
	private TextView userNameView;
	private TextView userLocationView;
	private TextView userGenderView;
	private TextView userDateOfBirthView;
	private TextView userRelationshipView;
	private Button logoutButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_userdetails);

		userProfilePictureView = (ProfilePictureView) findViewById(R.id.userProfilePicture);
		userNameView = (TextView) findViewById(R.id.userName);
		logoutButton = (Button) findViewById(R.id.logoutButton);
		logoutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onLogoutButtonClicked();
			}
		});

		// Fetch Facebook user info if the session is active
		Session session = ParseFacebookUtils.getSession();
		if (session != null && session.isOpened()) {
			makeMeRequest();
			makeFriendsRequest();
			startMapViewActivity();
		}
	}
	
	private void startMapViewActivity(){
		Intent intent = new Intent(this, MapActivity.class);
		startActivity(intent);
	}


	@Override
	public void onResume() {
		super.onResume();

		ParseUser currentUser = ParseUser.getCurrentUser();
		if (currentUser != null) {
			// Check if the user is currently logged
			// and show any cached content
			updateViewsWithProfileInfo();
		} else {
			// If the user is not logged in, go to the
			// activity showing the login view.
			startLoginActivity();
		}
	}

	private void makeMeRequest() {
		Request request = Request.newMeRequest(ParseFacebookUtils.getSession(),
				new Request.GraphUserCallback() {
			@Override
			public void onCompleted(GraphUser user, Response response) {
				if (user != null) {
					// Create a JSON object to hold the profile info
					JSONObject userProfile = new JSONObject();
					try {
						// Populate the JSON object
						userProfile.put("facebookId", user.getId());
						userProfile.put("name", user.getName());
						if (user.getLocation().getProperty("name") != null) {
							userProfile.put("location", (String) user
									.getLocation().getProperty("name"));
						}
						if (user.getProperty("gender") != null) {
							userProfile.put("gender",
									(String) user.getProperty("gender"));
						}
						if (user.getBirthday() != null) {
							userProfile.put("birthday",
									user.getBirthday());
						}
						if (user.getProperty("relationship_status") != null) {
							userProfile
							.put("relationship_status",
									(String) user
									.getProperty("relationship_status"));
						}

						// Save the user profile info in a user property
						ParseUser currentUser = ParseUser
								.getCurrentUser();
						currentUser.put("profile", userProfile);
						currentUser.saveInBackground();

						// Show the user info
						updateViewsWithProfileInfo();
					} catch (JSONException e) {
						Log.d(LetsGoApplication.TAG,
								"Error parsing returned user data.");
					}

				} else if (response.getError() != null) {
					if ((response.getError().getCategory() == FacebookRequestError.Category.AUTHENTICATION_RETRY)
							|| (response.getError().getCategory() == FacebookRequestError.Category.AUTHENTICATION_REOPEN_SESSION)) {
						Log.d(LetsGoApplication.TAG,
								"The facebook session was invalidated.");
						onLogoutButtonClicked();
					} else {
						Log.d(LetsGoApplication.TAG,
								"Some other error: "
										+ response.getError()
										.getErrorMessage());
					}
				}
			}
		});
		request.executeAsync();
	}

    
	private void makeFriendsRequest() {
		// retrieve your friends ids from facebook.
		// alas, fb only gives you friends who have already logged into this app.
		// not "all your friends" as you would want.
        String fqlQuery = "SELECT uid, name, pic_square FROM user WHERE uid IN " +
        		"(SELECT uid2 FROM friend WHERE uid1 = me() LIMIT 25)";
        Bundle params = new Bundle();
        params.putString("q", fqlQuery);
        Session session = Session.getActiveSession();
        Request request = new Request(session,
        		"/fql",                         
        		params,                         
        		HttpMethod.GET,                 
        		new Request.Callback(){         
        	public void onCompleted(Response response) {
        		JSONObject json;
        		try {
        			GraphObject go = response.getGraphObject();
        			JSONObject jo = go.getInnerJSONObject();
        			JSONArray jarray = jo.getJSONArray("data");
        			
        			for(int i = 0; i < jarray.length(); i++){
        				JSONObject obj = jarray.getJSONObject(i);
        				//get your values
        				String name = obj.getString("name");
        				String uid = obj.getString("uid"); 
        				User.addToFriendsList(uid, name, "-1");
        			}
        		} catch (JSONException e) {
        			e.printStackTrace();
        			Log.e("letsgo", "makeFriendRequest in Login failed: "+e.getMessage());
        		}
        	}
        	
        }); 
          Request.executeBatchAsync(request);         
	}
	private void updateViewsWithProfileInfo() {
		ParseUser currentUser = ParseUser.getCurrentUser();
		if (currentUser.get("profile") != null) {
			JSONObject userProfile = currentUser.getJSONObject("profile");
			try {
				if (userProfile.getString("facebookId") != null) {
					String facebookId = userProfile.get("facebookId")
							.toString();
					userProfilePictureView.setProfileId(facebookId);
				} else {
					// Show the default, blank user profile picture
					userProfilePictureView.setProfileId(null);
				}
				if (userProfile.getString("name") != null) {
					userNameView.setText(userProfile.getString("name"));
				} else {
					userNameView.setText("");
				}

			} catch (JSONException e) {
				Log.d(LetsGoApplication.TAG,
						"Error parsing in uVwithP in UserDetailsActivity. Excpn e="
				        +e.getMessage());
			}
		}
	}

	private void onLogoutButtonClicked() {
		// Log the user out
		ParseUser.logOut();

		// Go to the login view
		startLoginActivity();
	}
	
	private void startLoginActivity() {
		Intent intent = new Intent(this, LoginActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
	
}

