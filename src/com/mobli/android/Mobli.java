/* Copyright 2012 Mobli Media inc.
 *
 *  The following code is derived from Facebook Android SDK.
 *  Modifications were made to all original methods by Alexander Bezverhni, Mobli, 05/16/2012
 * 
 ********** Original Facebook License *************************************
 *
 * Copyright 2010 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *********** Original Facebook License *************************************
 */

package com.mobli.android;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.CookieSyncManager;

/**
 * Main Mobli object for interacting with the Mobli developer API. Provides
 * methods to log in and log out a user, make requests using the REST APIs, and
 * start user interface interactions with the API (such as pop-ups promoting for
 * credentials, permissions, etc.)
 * 
 * @author Jim Brusstar (jimbru@facebook.com), Yariv Sadan (yariv@facebook.com),
 *         Luke Shepard (lshepard@facebook.com)
 */
public class Mobli {

	// Strings used in the authorization flow
	public static final String REDIRECT_URI_START = "mobli";
	public static final String REDIRECT_URI_END = "://authorize";
	public static final String[] BASIC_PERMISSIONS = new String[] { "basic" };
	public static final String TOKEN = "access_token";
	public static final String EXPIRES = "expires_in";
	public static final String USER_ID = "user_id";

	public static final int FORCE_DIALOG_AUTH = -1;

	// Mobli server endpoints: may be modified in a subclass for testing
	protected static String AUTHORIZE_BASE_URL = "https://oauth.mobli.com";
	protected static String DIALOG_AUTHORIZE_URL = AUTHORIZE_BASE_URL + "/authorize";
	protected static String API_BASE_URL = "https://api.mobli.com/";

	private String mAccessToken = null;
	private long mAccessExpires = 0;
	private String mClientId;
	private String mClientSecret;
	private String mUserId;

	private DialogListener mAuthDialogListener;

	/**
	 * Constructor for Mobli object.
	 * 
	 * @param clientId
	 *            Your Mobli application CLIENT_ID. Found at
	 *            http://developers.mobli.com
	 * @param clientSecret
	 *            Your Mobli application CLIENT_SECRET. Found at
	 *            http://developers.mobli.com
	 */
	public Mobli(String clientId, String clientSecret) {
		if (clientId == null) {
			throw new IllegalArgumentException("You must specify your CLIENT_ID when instantiating "
					+ "a Mobli object. See README for details.");
		}
		if (clientSecret == null) {
			throw new IllegalArgumentException("You must specify your CLIENT_SECRET when instantiating "
					+ "a Mobli object. See README for details.");
		}
		setClientId(clientId);
		setClientSecret(clientSecret);
	}

	/**
	 * Default authorize method. Grants only basic permissions.
	 * 
	 * See authorize() below for @params.
	 */
	public void authorize(Activity activity, final DialogListener listener) {
		authorize(activity, BASIC_PERMISSIONS, listener);
	}

	/**
	 * Full authorize method.
	 * 
	 * Starts a dialog which prompts the user to log in to Mobli and grant the
	 * requested permissions to the given application.
	 * 
	 * In this flow, the user credentials are handled by Mobli in an embedded
	 * WebView. As such, the dialog makes a network request and renders HTML
	 * content rather than a native UI. The access token is retrieved from a
	 * redirect to a special URL that the WebView handles.
	 * 
	 * Note that User credentials could be handled natively using the OAuth 2.0
	 * Username and Password Flow, but this is not supported by this SDK.
	 * 
	 * See http://developers.mobli.com/documentation/authentication and
	 * http://wiki.oauth.net/OAuth-2 for more details.
	 * 
	 * Note that this method is asynchronous and the callback will be invoked in
	 * the original calling thread (not in a background thread).
	 * 
	 * Also note that requests may be made to the API without calling authorize
	 * first, in which case only public information is returned.
	 * 
	 * @param activity
	 *            The Android activity in which we want to display the
	 *            authorization dialog.
	 * @param permissions
	 *            A list of permissions required for this application: e.g.
	 *            "shared", "basic", "advanced", etc. see
	 *            http://developers.mobli
	 *            .com/documentation/authentication#scopes This parameter should
	 *            not be null -- if you do not require any permissions, then
	 *            pass in an empty String array.
	 * @param listener
	 *            Callback interface for notifying the calling application when
	 *            the authentication dialog has completed, failed, or been
	 *            canceled.
	 */
	public void authorize(Activity activity, String[] permissions, final DialogListener listener) {

		mAuthDialogListener = listener;

		startDialogAuth(activity, permissions);
	}

	/**
	 * Internal method to handle dialog-based authentication backend for
	 * authorize().
	 * 
	 * @param activity
	 *            The Android Activity that will parent the auth dialog.
	 * @param permissions
	 *            A list of permissions required for this application. If you do
	 *            not require any permissions, pass an empty String array.
	 */
	private void startDialogAuth(Activity activity, String[] permissions) {
		Bundle params = new Bundle();
		if (permissions.length > 0) {
			params.putString("scope", TextUtils.join(" ", permissions));
		}
		CookieSyncManager.createInstance(activity);
		dialog(activity, params, new DialogListener() {

			public void onComplete(Bundle values) {
				// ensure any cookies set by the dialog are saved
				CookieSyncManager.getInstance().sync();
				setAccessToken(values.getString(TOKEN));
				setAccessExpiresIn(values.getString(EXPIRES));
				setUserId(values.getString(USER_ID));
				if (isSessionValid()) {
					Util.logd("Mobli-authorize", "Login Success! access_token=" + getAccessToken() + " expires=" + getAccessExpires());
					mAuthDialogListener.onComplete(values);
				} else {
					mAuthDialogListener.onMobliError(new MobliError("Failed to receive access token."));
				}
			}

			public void onError(DialogError error) {
				Util.logd("Mobli-authorize", "Login failed: " + error);
				mAuthDialogListener.onError(error);
			}

			public void onMobliError(MobliError error) {
				Util.logd("Mobli-authorize", "Login failed: " + error);
				mAuthDialogListener.onMobliError(error);
			}

			public void onCancel() {
				Util.logd("Mobli-authorize", "Login canceled");
				mAuthDialogListener.onCancel();
			}
		});
	}

	/**
	 * Invalidate the current user session by removing the access token in
	 * memory and clearing the browser cookie.
	 * 
	 * @param context
	 *            The Android context in which the logout should be called: it
	 *            should be the same context in which the login occurred in
	 *            order to clear any stored cookies
	 */
	public void logout(Context context) {
		Util.clearCookies(context);
		setAccessToken(null);
		setAccessExpires(0);
	}

	/**
	 * Make a request to the Mobli API without any parameters.
	 * 
	 * See http://developers.mobli.com/documentation
	 * 
	 * Note that this method blocks waiting for a network response, so do not
	 * call it in a UI thread.
	 * 
	 * @param relativePath
	 *            Relative path to resource in the Mobli API, e.g., to fetch
	 *            data about the currently logged authenticated user, provide
	 *            "me", which will fetch https://api.mobli.com/me
	 * @throws IOException
	 * @throws MalformedURLException
	 * @return JSON string representation of the response
	 */
	public String request(String relativePath) throws MalformedURLException, IOException {
		return request(relativePath, new Bundle(), "GET");
	}

	/**
	 * Make a request to the Mobli API with the given string parameters using an
	 * HTTP GET (default method).
	 * 
	 * See http://developers.mobli.com/documentation
	 * 
	 * Note that this method blocks waiting for a network response, so do not
	 * call it in a UI thread.
	 * 
	 * @param relativePath
	 *            Relative path to resource in the Mobli API, e.g., to fetch
	 *            data about the currently logged authenticated user, provide
	 *            "me", which will fetch https://api.mobli.com/me
	 * @param parameters
	 *            key-value string parameters, e.g. the path "search" with
	 *            parameters {"q" : "leonardo", "entity" : "people",
	 *            "max_per_page" : "2"} would produce a query for the following
	 *            API resource:
	 *            https://api.mobli.com/search?q=leonardo&entity=people
	 *            &max_per_page=2
	 * @throws IOException
	 * @throws MalformedURLException
	 * @return JSON string representation of the response
	 */
	public String request(String relativePath, Bundle parameters) throws MalformedURLException, IOException {
		return request(relativePath, parameters, "GET");
	}

	/**
	 * Synchronously make a request to the Mobli API with the given HTTP method
	 * and string parameters. Note that binary data parameters (e.g. pictures)
	 * are not yet supported by this helper function.
	 * 
	 * See http://developers.mobli.com/documentation
	 * 
	 * Note that this method blocks waiting for a network response, so do not
	 * call it in a UI thread.
	 * 
	 * @param relativePath
	 *            Relative path to resource in the Mobli API, e.g., to fetch
	 *            data about the currently logged authenticated user, provide
	 *            "me", which will fetch https://api.mobli.com/me
	 * @param parameters
	 *            key-value string parameters, e.g. the path "search" with
	 *            parameters {"q" : "leonardo", "entity" : "people",
	 *            "max_per_page" : "2"} would produce a query for the following
	 *            API resource:
	 *            https://api.mobli.com/search?q=leonardo&entity=people
	 *            &max_per_page=2
	 * @param httpMethod
	 *            http verb, e.g. "GET", "POST", "DELETE"
	 * @param baseUrl
	 *            Request base url. Generally, it will be REST API base url
	 * @throws IOException
	 * @throws MalformedURLException
	 * @return JSON string representation of the response
	 */
	public String request(String baseUrl, String relativePath, Bundle params, String httpMethod) throws FileNotFoundException,
			MalformedURLException, IOException {

		if (isSessionValid()) {
			params.putString(TOKEN, getAccessToken());
		}
		String url = baseUrl + relativePath;
		return Util.openUrl(url, httpMethod, params);
	}

	/**
	 * Synchronously make a request to the Mobli API with the given HTTP method
	 * and string parameters. Note that binary data parameters (e.g. pictures)
	 * are not yet supported by this helper function.
	 * 
	 * See http://developers.mobli.com/documentation
	 * 
	 * Note that this method blocks waiting for a network response, so do not
	 * call it in a UI thread.
	 * 
	 * @param relativePath
	 *            Relative path to resource in the Mobli API, e.g., to fetch
	 *            data about the currently logged authenticated user, provide
	 *            "me", which will fetch https://api.mobli.com/me
	 * @param parameters
	 *            key-value string parameters, e.g. the path "search" with
	 *            parameters {"q" : "leonardo", "entity" : "people",
	 *            "max_per_page" : "2"} would produce a query for the following
	 *            API resource:
	 *            https://api.mobli.com/search?q=leonardo&entity=people
	 *            &max_per_page=2
	 * @param httpMethod
	 *            http verb, e.g. "GET", "POST", "DELETE"
	 * @param baseUrl
	 *            Request base url. Generally, it will be REST API base url
	 * @throws IOException
	 * @throws MalformedURLException
	 * @return JSON string representation of the response
	 */
	public String request(String relativePath, Bundle params, String httpMethod) throws FileNotFoundException, MalformedURLException,
			IOException {
		return request(API_BASE_URL, relativePath, params, httpMethod);
	}

	/**
	 * Generate a UI dialog for the authentication action in the given Android
	 * context with the provided parameters.
	 * 
	 * Note that this method is asynchronous and the callback will be invoked in
	 * the original calling thread (not in a background thread).
	 * 
	 * @param context
	 *            The Android context in which we will generate this dialog.
	 * @param parameters
	 *            String key-value pairs to be passed as URL parameters.
	 * @param listener
	 *            Callback interface to notify the application when the dialog
	 *            has completed.
	 */
	public void dialog(Context context, Bundle parameters, final DialogListener listener) {

		String redirectUri = REDIRECT_URI_START + mClientId + REDIRECT_URI_END;
		parameters.putString("redirect_uri", redirectUri);
		parameters.putString("client_id", mClientId);
		parameters.putString("response_type", "token");

		if (isSessionValid()) {
			parameters.putString(TOKEN, getAccessToken());
		}
		String url = DIALOG_AUTHORIZE_URL + "?" + Util.encodeUrl(parameters);
		if (context.checkCallingOrSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
			Util.showAlert(context, "Error", "Application requires permission to access the Internet");
		} else {
			new MobliDialog(context, url, listener, redirectUri).show();
		}
	}

	/**
	 * @return boolean - whether this object has an non-expired session token
	 */
	public boolean isSessionValid() {
		return (getAccessToken() != null) && ((getAccessExpires() == 0) || (System.currentTimeMillis() < getAccessExpires()));
	}

	/**
	 * Retrieve the OAuth 2.0 access token for API access: treat with care.
	 * Returns null if no session exists.
	 * 
	 * @return String - access token
	 */
	public String getAccessToken() {
		return mAccessToken;
	}

	/**
	 * Retrieve the current session's expiration time (in milliseconds since
	 * Unix epoch), or 0 if the session doesn't expire or doesn't exist.
	 * 
	 * @return long - session expiration time
	 */
	public long getAccessExpires() {
		return mAccessExpires;
	}

	/**
	 * Set the OAuth 2.0 access token for API access.
	 * 
	 * @param token
	 *            - access token
	 */
	public void setAccessToken(String token) {
		mAccessToken = token;
	}

	/**
	 * Set the current session's expiration time (in milliseconds since Unix
	 * epoch), or 0 if the session doesn't expire.
	 * 
	 * @param time
	 *            - timestamp in milliseconds
	 */
	public void setAccessExpires(long time) {
		mAccessExpires = time;
	}

	/**
	 * Set the current session's duration (in seconds since Unix epoch), or "0"
	 * if session doesn't expire.
	 * 
	 * @param expiresIn
	 *            - duration in seconds (or 0 if the session doesn't expire)
	 */
	public void setAccessExpiresIn(String expiresIn) {
		if (expiresIn != null) {
			long expires = expiresIn.equals("0") ? 0 : System.currentTimeMillis() + Long.parseLong(expiresIn) * 1000L;
			setAccessExpires(expires);
		}
	}

	public String getClientId() {
		return mClientId;
	}

	private void setClientId(String clientId) {
		mClientId = clientId;
	}

	public String getClientSecret() {
		return mClientSecret;
	}

	private void setClientSecret(String clientSecret) {
		mClientSecret = clientSecret;
	}

	public String getUserId() {
		return mUserId;
	}

	private void setUserId(String userId) {
		this.mUserId = userId;
	}

	/**
	 * Callback interface for dialog requests.
	 * 
	 */
	public static interface DialogListener {

		/**
		 * Called when a dialog completes.
		 * 
		 * Executed by the thread that initiated the dialog.
		 * 
		 * @param values
		 *            Key-value string pairs extracted from the response.
		 */
		public void onComplete(Bundle values);

		/**
		 * Called when a Mobli responds to a dialog with an error.
		 * 
		 * Executed by the thread that initiated the dialog.
		 * 
		 */
		public void onMobliError(MobliError e);

		/**
		 * Called when a dialog has an error.
		 * 
		 * Executed by the thread that initiated the dialog.
		 * 
		 */
		public void onError(DialogError e);

		/**
		 * Called when a dialog is canceled by the user.
		 * 
		 * Executed by the thread that initiated the dialog.
		 * 
		 */
		public void onCancel();

	}

	/**
	 * Callback interface for service requests.
	 */
	public static interface ServiceListener {

		/**
		 * Called when a service request completes.
		 * 
		 * @param values
		 *            Key-value string pairs extracted from the response.
		 */
		public void onComplete(Bundle values);

		/**
		 * Called when a Mobli server responds to the request with an error.
		 */
		public void onMobliError(MobliError e);

		/**
		 * Called when a Mobli Service responds to the request with an error.
		 */
		public void onError(Error e);

	}
}
