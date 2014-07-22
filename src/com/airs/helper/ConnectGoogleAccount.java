package com.airs.helper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.airs.R;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ParentReference;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfoplus;

@SuppressLint("NewApi")
public class ConnectGoogleAccount extends Activity {
	static private final int REQUEST_ACCOUNT_PICKER = 1;
	static private final int REQUEST_AUTHORIZATION = 2;
	static private String CLIENTSECRETS_LOCATION;
	private static final List<String> SCOPES = Arrays.asList(
			"https://www.googleapis.com/auth/drive.file",
			"https://www.googleapis.com/auth/userinfo.email",
			"https://www.googleapis.com/auth/userinfo.profile");
	private static GoogleAuthorizationCodeFlow flow = null;

	// the actual credential token
	private Credential credential;
	
	// preferences
	private SharedPreferences settings;
	
	// GDrive folder
	private String GDrive_Folder;
	
	private CredentialManager credManager;

	/**
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState
	 *            a Bundle of the saved state, according to Android lifecycle
	 *            model
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set content of View
		setContentView(R.layout.googleaccounts);

		// get default preferences
		settings = PreferenceManager.getDefaultSharedPreferences(this);

		// FIXME: Add unique Folder key
		GDrive_Folder = settings.getString("GDriveFolder", "AIRS");

		try {
			// set Secretpath
			CLIENTSECRETS_LOCATION = getResources().getAssets().list("")[1];
			HttpTransport httpTransport = new NetHttpTransport();
			JacksonFactory jsonFactory = new JacksonFactory();
			GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
					jsonFactory,
					new InputStreamReader(getAssets().open(CLIENTSECRETS_LOCATION)));
			
			credManager = new CredentialManager(clientSecrets, httpTransport, jsonFactory);
			String s = credManager.getAuthorizationUrl();
			
			//FIXME: HIER GEHTS WEITER MIT URL LESEN
			Uri tmp = Uri.parse(s);
			tmp.getQueryParameterNames();
			
			
			credential = credManager.retrieve(s);
			// create upload directory here for continuing with the
			// authentication
			createDirectory();

			// clear persistent flag
			Editor editor = settings.edit();
			editor.putString("AIRS_local::accountname", "stressapp.fim@gmail.com");
			// finally commit to storing values!!
			editor.commit();
			
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(),
					getString(R.string.ConnectGoogle4), Toast.LENGTH_LONG)
					.show();
			finish();
		}
	}

	/**
	 * Called when the activity is resumed.
	 */
	@Override
	public synchronized void onResume() {
		super.onResume();
	}

	/**
	 * Called when the activity is paused.
	 */
	@Override
	public synchronized void onPause() {
		super.onPause();
	}

	/**
	 * Called when the activity is stopped.
	 */
	@Override
	public void onStop() {
		super.onStop();
	}

	/**
	 * Called when the activity is destroyed.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void createDirectory() {
		new Thread(new Runnable() {
			public void run() {
				boolean running = true;
				com.google.api.services.drive.model.File AIRS_dir = null;
				com.google.api.services.drive.model.File body;

				while (running == true) {
					Drive.Builder tmp = new Drive.Builder(AndroidHttp
							.newCompatibleTransport(), new GsonFactory(),
							credential);
					tmp.setApplicationName("myStress");
					Drive service = tmp.build();
					try {
						SerialPortLogger
								.debugForced("trying to find AIRS recordings directory in root");

						List<com.google.api.services.drive.model.File> files = service
								.files()
								.list()
								.setQ("mimeType = 'application/vnd.google-apps.folder' AND trashed=false AND 'root' in parents")
								.execute().getItems();
						for (com.google.api.services.drive.model.File f : files) {
							if (f.getTitle().compareTo(GDrive_Folder) == 0)
								AIRS_dir = f;
						}

						if (AIRS_dir == null) {
							SerialPortLogger
									.debugForced("...need to create AIRS recordings directory");

							// create recordings directory in root!
							body = new com.google.api.services.drive.model.File();
							body.setTitle(GDrive_Folder);
							body.setParents(Arrays.asList(new ParentReference()
									.setId("root")));
							body.setMimeType("application/vnd.google-apps.folder");
							AIRS_dir = service.files().insert(body).execute();
							if (AIRS_dir != null)
								SerialPortLogger
										.debugForced("Created folder with id = "
												+ AIRS_dir.getId());
						}

						running = false;
					} catch (UserRecoverableAuthIOException e) {
						SerialPortLogger
								.debugForced("Require authorization - starting activity!");
						startActivityForResult(e.getIntent(),
								REQUEST_AUTHORIZATION);
						return;
					} catch (Exception e) {
						SerialPortLogger
								.debugForced("something went wrong with folder creation: "
										+ e.toString());
					}
				}

				finish();
			}
		}).start();
	}

	/**
	 * Build an authorization flow and store it as a static class attribute.
	 *
	 * @return GoogleAuthorizationCodeFlow instance.
	 * @throws IOException
	 *             Unable to load client_secrets.json.
	 */
	static GoogleAuthorizationCodeFlow getFlow() throws IOException {
		if (flow == null) {
			HttpTransport httpTransport = new NetHttpTransport();
			JacksonFactory jsonFactory = new JacksonFactory();
			GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
					jsonFactory,
					new InputStreamReader(GoogleAuthorizationCodeFlow.class
							.getResourceAsStream(CLIENTSECRETS_LOCATION)));
			flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport,
					jsonFactory, clientSecrets, SCOPES)
					.setAccessType("offline").setApprovalPrompt("force")
					.build();
		}
		return flow;
	}

	/**
	 * Exchange an authorization code for OAuth 2.0 credentials.
	 *
	 * @param authorizationCode
	 *            Authorization code to exchange for OAuth 2.0 credentials.
	 * @return OAuth 2.0 credentials.
	 * @throws CodeExchangeException
	 *             An error occurred.
	 */
	static Credential exchangeCode(String authorizationCode) {
		try {
			GoogleAuthorizationCodeFlow flow = getFlow();
			GoogleTokenResponse response = flow
					.newTokenRequest(authorizationCode)
					.setRedirectUri("127.0.0.1").execute();

			return flow.createAndStoreCredential(response, null);
		} catch (IOException e) {
			Log.e("AIRS", e.getMessage());
		}

		return null;
	}
	
		/**
	   * Retrieve credentials using the provided authorization code.
	   *
	   * This function exchanges the authorization code for an access token and
	   * queries the UserInfo API to retrieve the user's e-mail address. If a
	   * refresh token has been retrieved along with an access token, it is stored
	   * in the application database using the user's e-mail address as key. If no
	   * refresh token has been retrieved, the function checks in the application
	   * database for one and returns it if found or throws a NoRefreshTokenException
	   * with the authorization URL to redirect the user to.
	   *
	   * @param authorizationCode Authorization code to use to retrieve an access
	   *        token.
	   * @param state State to set to the authorization URL in case of error.
	   * @return OAuth 2.0 credentials instance containing an access and refresh
	   *         token.
	   * @throws NoRefreshTokenException No refresh token could be retrieved from
	   *         the available sources.
	   * @throws IOException Unable to load client_secrets.json.
	   */
	  public static Credential getCredentials(String authorizationCode, String state) {
	    try {
	      Credential credentials = exchangeCode(authorizationCode);
	      return credentials;
	    } catch (Exception e) {
		      Log.e("AIRS","Error get Credential " + e.getMessage());
		      return null;
	    }
	  }
	  
	  /**
	   * Retrieve the authorization URL.
	   *
	   * @param emailAddress User's e-mail address.
	   * @param state State for the authorization URL.
	   * @return Authorization URL to redirect the user to.
	   * @throws IOException Unable to load client_secrets.json.
	   */
	  public static String getAuthorizationUrl(String emailAddress, String state) throws IOException {
	    GoogleAuthorizationCodeRequestUrl urlBuilder =
	        getFlow().newAuthorizationUrl().setRedirectUri("127.0.0.1").setState(state);
	    urlBuilder.set("user_id", emailAddress);
	    return urlBuilder.build();
	  }
	  
	  /**
	   * Send a request to the UserInfo API to retrieve the user's information.
	   *
	   * @param credentials OAuth 2.0 credentials to authorize the request.
	   * @return User's information.
	   * @throws NoUserIdException An error occurred.
	   */
	  static Userinfoplus getUserInfo(Credential credentials) {
	    Oauth2 userInfoService =
	        new Oauth2.Builder(new NetHttpTransport(), new JacksonFactory(), credentials).build();
	    Userinfoplus userInfo = null;
	    try {
	      userInfo = userInfoService.userinfo().get().execute();
	    } catch (IOException e) {
	      System.err.println("An error occurred: " + e);
	    }
	    if (userInfo != null && userInfo.getId() != null) {
	      return userInfo;
	    } else {
	      Log.e("AIRS","No User Exception");
	      return null;
	    }
	  }
	  
}
