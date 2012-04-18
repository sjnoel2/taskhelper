package uk.co.vurt.taskhelper.activities;

import uk.co.vurt.taskhelper.Constants;
import uk.co.vurt.taskhelper.authenticator.AuthenticatorActivity;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class DispatcherActivity extends Activity {

	public final static String RETURN_TO_START_KEY = "uk.co.vurt.taskhelper.activities.ReturnToStart";
	protected AccountManager accountManager;
	
	/**
	 * Check to see if there is an existing TaskHelper account registered.
	 * If so, use that account.
	 * If not, invoke the account setup activity
	 */
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		//check for sync server setting
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		accountManager = AccountManager.get(this);
		Account[] accounts = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE); //retrieve all TaskHelper accounts
		Intent intent;
		if(preferences.getString("sync_server", null) == null){
			intent = new Intent(this, PreferencesActivity.class);
			intent.putExtra(RETURN_TO_START_KEY, true);
		} else if(accounts.length <= 0){
			//no accounts registered, invoke registration
			intent = new Intent(this, AuthenticatorActivity.class);
			intent.putExtra(RETURN_TO_START_KEY, true);
		} else {
			//Account found, so carry on as normal.
			intent = new Intent(this, JobList.class);
		}
		startActivity(intent);
		finish();
	}
}