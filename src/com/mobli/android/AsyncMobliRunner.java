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

import org.json.JSONObject;

import android.os.Bundle;

/**
 * A sample implementation of asynchronous API requests. This class provides the
 * ability to execute API methods and have the call return immediately, without
 * blocking the calling thread. This is necessary when accessing the API in the
 * UI thread, for instance. The request response is returned to the caller via a
 * callback interface, which the developer must implement.
 * 
 * This sample implementation simply spawns a new thread for each request, and
 * makes the API call immediately. This may work in many applications, but more
 * sophisticated users may re-implement this behavior using a thread pool, a
 * network thread, a request queue, or other mechanism. Advanced functionality
 * could be built, such as rate-limiting of requests, as per a specific
 * application's needs.
 * 
 * @see RequestListener The callback interface.
 * 
 * @author Jim Brusstar (jimbru@fb.com), Yariv Sadan (yariv@fb.com), Luke
 *         Shepard (lshepard@fb.com)
 */
public class AsyncMobliRunner {

	Mobli mobli;

	public AsyncMobliRunner(Mobli mobli) {
		this.mobli = mobli;
	}

	/**
	 * Make a request to the Mobli API without any parameters.
	 * 
	 * See http://developers.mobli.com/documentation
	 * 
	 * Note that this method is asynchronous and the callback will be invoked in
	 * a background thread; operations that affect the UI will need to be posted
	 * to the UI thread or an appropriate handler.
	 * 
	 * @param relativePath
	 *            Relative path to resource in the Mobli API, e.g., to fetch
	 *            data about the currently logged authenticated user, provide
	 *            "me", which will fetch https://api.mobli.com/me
	 * @param listener
	 *            Callback interface to notify the application when the request
	 *            has completed.
	 * @param state
	 *            An arbitrary object used to identify the request when it
	 *            returns to the callback. This has no effect on the request
	 *            itself.
	 */
	public void request(String relativePath, RequestListener listener, final Object state) {
		request(relativePath, new Bundle(), "GET", listener, state);
	}

	public void request(String relativePath, RequestListener listener) {
		request(relativePath, new Bundle(), "GET", listener, /* state */null);
	}

	/**
	 * Make a request to the Mobli API with the given string parameters using an
	 * HTTP GET (default method).
	 * 
	 * See http://developers.mobli.com/documentation
	 * 
	 * Note that this method is asynchronous and the callback will be invoked in
	 * a background thread; operations that affect the UI will need to be posted
	 * to the UI thread or an appropriate handler.
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
	 * @param listener
	 *            Callback interface to notify the application when the request
	 *            has completed.
	 * @param state
	 *            An arbitrary object used to identify the request when it
	 *            returns to the callback. This has no effect on the request
	 *            itself.
	 */
	public void request(String relativePath, Bundle parameters, RequestListener listener, final Object state) {
		request(relativePath, parameters, "GET", listener, state);
	}

	public void request(String relativePath, Bundle parameters, RequestListener listener) {
		request(relativePath, parameters, "GET", listener, /* state */null);
	}

	/**
	 * Make a request to the Mobli API with the given HTTP method and string
	 * parameters. Note that binary data parameters (e.g. pictures) are not yet
	 * supported by this helper function.
	 * 
	 * See http://developers.mobli.com/documentation
	 * 
	 * Note that this method is asynchronous and the callback will be invoked in
	 * a background thread; operations that affect the UI will need to be posted
	 * to the UI thread or an appropriate handler.
	 * 
	 * @param baseUrl
	 *            May be "https://api.mobli.com/" or "https://oauth.mobli.com/"
	 * @param relativePath
	 *            Relative path to resource in the Mobli API, e.g., to fetch
	 *            data about the currently logged authenticated user, provide
	 *            "https://api.mobli.com/" baseUrl and "me" relativePath, which
	 *            will fetch https://api.mobli.com/me
	 * @param parameters
	 *            key-value string parameters, e.g. the path "search" with
	 *            parameters {"q" : "leonardo", "entity" : "people",
	 *            "max_per_page" : "2"} would produce a query for the following
	 *            API resource:
	 *            https://api.mobli.com/search?q=leonardo&entity=people
	 *            &max_per_page=2
	 * @param httpMethod
	 *            http verb, e.g. "POST", "DELETE"
	 * @param listener
	 *            Callback interface to notify the application when the request
	 *            has completed.
	 * @param state
	 *            An arbitrary object used to identify the request when it
	 *            returns to the callback. This has no effect on the request
	 *            itself.
	 */
	public void request(final String baseUrl, final String relativePath, final Bundle parameters, final String httpMethod,
			final RequestListener listener, final Object state) {
		new Thread() {
			@Override
			public void run() {
				try {
					String resp = mobli.request(baseUrl, relativePath, parameters, httpMethod);
					listener.onComplete(resp, state);
				} catch (FileNotFoundException e) {
					listener.onFileNotFoundException(e, state);
				} catch (MalformedURLException e) {
					listener.onMalformedURLException(e, state);
				} catch (IOException e) {
					listener.onIOException(e, state);
				}
			}
		}.start();
	}

	/**
	 * Make a request to the Mobli API with the given HTTP method and string
	 * parameters. Note that binary data parameters (e.g. pictures) are not yet
	 * supported by this helper function. By default, this goes to the API base
	 * url (rather than oAuth base url).
	 * 
	 * See http://developers.mobli.com/documentation
	 * 
	 * Note that this method is asynchronous and the callback will be invoked in
	 * a background thread; operations that affect the UI will need to be posted
	 * to the UI thread or an appropriate handler.
	 * 
	 * @param relativePath
	 *            Relative path to resource in the Mobli API, e.g., to fetch
	 *            data about the currently logged authenticated user, provide
	 *            "https://api.mobli.com/" baseUrl and "me" relativePath, which
	 *            will fetch https://api.mobli.com/me
	 * @param parameters
	 *            key-value string parameters, e.g. the path "search" with
	 *            parameters {"q" : "leonardo", "entity" : "people",
	 *            "max_per_page" : "2"} would produce a query for the following
	 *            API resource:
	 *            https://api.mobli.com/search?q=leonardo&entity=people
	 *            &max_per_page=2
	 * @param httpMethod
	 *            http verb, e.g. "POST", "DELETE"
	 * @param listener
	 *            Callback interface to notify the application when the request
	 *            has completed.
	 * @param state
	 *            An arbitrary object used to identify the request when it
	 *            returns to the callback. This has no effect on the request
	 *            itself.
	 */
	public void request(final String relativePath, final Bundle parameters, final String httpMethod, final RequestListener listener,
			final Object state) {
		request(Mobli.API_BASE_URL, relativePath, parameters, httpMethod, listener, state);
	}

	/**
	 * Obtain public (shared) access_token asynchronously.
	 * 
	 * @param listener
	 *            Callback interface to notify the application when the request
	 *            has completed.
	 * @param state
	 *            An arbitrary object used to identify the request when it
	 *            returns to the callback. This has no effect on the request
	 *            itself.
	 */
	public void obtainPublicToken(final RequestListener originalListener, final Object state) {
		RequestListener listener;
		Bundle params = new Bundle();
		params.putString("client_id", mobli.getClientId());
		params.putString("client_secret", mobli.getClientSecret());
		params.putString("grant_type", "client_credentials");
		params.putString("scope", "shared");

		listener = new RequestListener() {

			@Override
			public void onMobliError(MobliError e, Object state) {
				originalListener.onMobliError(e, state);
			}

			@Override
			public void onMalformedURLException(MalformedURLException e, Object state) {
				originalListener.onMalformedURLException(e, state);
			}

			@Override
			public void onIOException(IOException e, Object state) {
				originalListener.onIOException(e, state);
			}

			@Override
			public void onFileNotFoundException(FileNotFoundException e, Object state) {
				originalListener.onFileNotFoundException(e, state);
			}

			@Override
			public void onComplete(String response, Object state) {

				//store public token
				try {
					String publicAccessToken;
					JSONObject json = new JSONObject(response);
					publicAccessToken = json.getString(Mobli.TOKEN);
					mobli.setAccessToken(publicAccessToken);
				} catch (Exception e) {
					// do nothing
				}

				originalListener.onComplete(response, state);
			}
		};

		request(Mobli.AUTHORIZE_BASE_URL, "/shared", params, "POST", listener, state);
	}

	/**
	 * Callback interface for API requests.
	 * 
	 * Each method includes a 'state' parameter that identifies the calling
	 * request. It will be set to the value passed when originally calling the
	 * request method, or null if none was passed.
	 */
	public static interface RequestListener {

		/**
		 * Called when a request completes with the given response.
		 * 
		 * Executed by a background thread: do not update the UI in this method.
		 */
		public void onComplete(String response, Object state);

		/**
		 * Called when a request has a network or request error.
		 * 
		 * Executed by a background thread: do not update the UI in this method.
		 */
		public void onIOException(IOException e, Object state);

		/**
		 * Called when a request fails because the requested resource is invalid
		 * or does not exist.
		 * 
		 * Executed by a background thread: do not update the UI in this method.
		 */
		public void onFileNotFoundException(FileNotFoundException e, Object state);

		/**
		 * Called if an invalid relative path is provided (which may result in a
		 * malformed URL).
		 * 
		 * Executed by a background thread: do not update the UI in this method.
		 */
		public void onMalformedURLException(MalformedURLException e, Object state);

		/**
		 * Called when the server-side Mobli method fails.
		 * 
		 * Executed by a background thread: do not update the UI in this method.
		 */
		public void onMobliError(MobliError e, Object state);

	}

}
