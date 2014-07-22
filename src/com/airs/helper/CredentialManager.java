package com.airs.helper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import android.util.Log;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

/**
 * Credential manager to get, save, delete user credentials.
 *
 * @author jbd@google.com (Burcu Dogan)
 */
public class CredentialManager {

	/**
	 * Client secrets object.
	 */
	private GoogleClientSecrets clientSecrets;

	/**
	 * Transport layer for OAuth2 client.
	 */
	private HttpTransport transport;

	/**
	 * JSON factory for OAuth2 client.
	 */
	private JsonFactory jsonFactory;

	/**
	 * Scopes for which to request access from the user.
	 */
	public static final List<String> SCOPES = Arrays.asList(
			// Required to access and manipulate files.
			"https://www.googleapis.com/auth/drive.file",
			// Required to identify the user in our data store.
			"https://www.googleapis.com/auth/userinfo.email",
			"https://www.googleapis.com/auth/userinfo.profile");

	/**
	 * Credential Manager constructor.
	 * 
	 * @param clientSecrets
	 *            App client secrets to be used during OAuth2 exchanges.
	 * @param transport
	 *            Transportation layer for OAuth2 client.
	 * @param factory
	 *            JSON factory for OAuth2 client.
	 */
	public CredentialManager(GoogleClientSecrets clientSecrets,
			HttpTransport transport, JsonFactory factory) {
		this.clientSecrets = clientSecrets;
		this.transport = transport;
		this.jsonFactory = factory;
	}

	/**
	 * Builds an empty credential object.
	 * 
	 * @return An empty credential object.
	 */
	public Credential buildEmpty() {
		return new GoogleCredential.Builder()
				.setClientSecrets(this.clientSecrets).setTransport(transport)
				.setJsonFactory(jsonFactory).build();
	}

	/**
	 * Generates a consent page url.
	 * 
	 * @return A consent page url string for user redirection.
	 */
	public String getAuthorizationUrl() {
		GoogleAuthorizationCodeRequestUrl urlBuilder = new GoogleAuthorizationCodeRequestUrl(
				clientSecrets.getInstalled().getClientId(), clientSecrets.getInstalled()
						.getRedirectUris().get(0), SCOPES).setAccessType(
				"offline").setApprovalPrompt("force");
		return urlBuilder.build();
	}

	/**
	 * Retrieves a new access token by exchanging the given code with OAuth2
	 * end-points.
	 * 
	 * @param code
	 *            Exchange code.
	 * @return A credential object.
	 */
	public Credential retrieve(String code) {
		exchangeCode(code);
		try {
			GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(
					transport, jsonFactory, clientSecrets.getInstalled()
							.getClientId(), clientSecrets.getInstalled()
							.getClientSecret(), code, clientSecrets.getInstalled()
							.getRedirectUris().get(0)).execute();
			return buildEmpty().setAccessToken(response.getAccessToken());
		} catch (IOException e) {
			new RuntimeException(
					"An unknown problem occured while retrieving token");
		}
		return null;
	}

	/**
	 * Build an authorization flow
	 *
	 * @return GoogleAuthorizationCodeFlow instance.
	 * @throws IOException
	 *             Unable to load client_secrets.json.
	 */
	private GoogleAuthorizationCodeFlow getFlow() throws IOException {
		HttpTransport httpTransport = new NetHttpTransport();
		JacksonFactory jsonFactory = new JacksonFactory();
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport,
				jsonFactory, clientSecrets, SCOPES).setAccessType("offline")
				.setApprovalPrompt("force").build();

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
	private Credential exchangeCode(String authorizationCode) {
		try {
			GoogleAuthorizationCodeFlow flow = getFlow();
			GoogleTokenResponse response = flow
					.newTokenRequest(authorizationCode)
					.setRedirectUri(getAuthorizationUrl()).execute();
			return flow.createAndStoreCredential(response, null);
		} catch (IOException e) {
			System.err.println("An error occurred: " + e);
			Log.e("com.airs", "Error exchangeCode()");
			return null;
		}
	}
}
