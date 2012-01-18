package uk.co.vurt.taskhelper.syncadapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.auth.AuthenticationException;
import org.json.JSONException;

import uk.co.vurt.taskhelper.Constants;
import uk.co.vurt.taskhelper.client.NetworkUtilities;
import uk.co.vurt.taskhelper.domain.definition.TaskDefinition;
import uk.co.vurt.taskhelper.domain.job.DataItem;
import uk.co.vurt.taskhelper.domain.job.JobDefinition;
import uk.co.vurt.taskhelper.domain.job.Submission;
import uk.co.vurt.taskhelper.providers.Dataitem;
import uk.co.vurt.taskhelper.providers.Job;
import uk.co.vurt.taskhelper.providers.Task;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private static final String TAG = "SyncAdapter";
	private static final String LAST_UPDATED_KEY = "uk.co.vurt.taskhelper.syncadapter.lastUpdated";
	private static final boolean NOTIFY_AUTH_FAILURE = true;
	
	private final AccountManager accountManager;
	private final Context context;
	
//	private Date lastUpdated;
	
	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		this.context = context;
		accountManager = AccountManager.get(context);
	}

	private void storeTaskDefinition(ContentProviderClient provider, TaskDefinition definition){
		Log.d(TAG, "Adding a definition to database: " + definition);
		if(definition != null){
			ContentValues values = new ContentValues();
			values.put(Task.Definitions._ID, definition.getId());
			values.put(Task.Definitions.NAME, definition.getName());
			values.put(Task.Definitions.DESCRIPTION, definition.getDescription());
			
			//Serialise the object as json, rather than individually storing pages etc.
			values.put(Task.Definitions.JSON, definition.toJson());
			
			//We need to see if the definition already exists, if it does do an update otherwise do an insert
			Uri definitionUri = ContentUris.withAppendedId(Task.Definitions.CONTENT_URI, definition.getId());
			
			try {
				Cursor cur = provider.query(definitionUri, null, null, null, null);
				if(cur.moveToFirst()){
					Log.d(TAG, "Updating task definition " + definition.getId());
					provider.update(definitionUri, values, null, null);
				}else{
					Log.d(TAG, "Inserting new definition " + definition.getId());
					provider.insert(Task.Definitions.CONTENT_URI, values);
				}
				cur.close();
				cur = null;
			} catch (RemoteException re) {
				Log.e(TAG, "RemoteException", re);
			}
		}else{
			Log.d(TAG, "Null definition");
		}
	}
	
	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {

		List<JobDefinition> jobs = new ArrayList<JobDefinition>();
		List<TaskDefinition> definitions = new ArrayList<TaskDefinition>();
		String authToken = null;
		try{
			authToken = accountManager.blockingGetAuthToken(account, Constants.AUTHTOKEN_TYPE, NOTIFY_AUTH_FAILURE);
			long lastUpdated = getLastUpdatedDate(account);
			
			
			//Submit completed jobs
			//Find which jobs have been completed.
			Cursor jobCursor = provider.query(Job.Definitions.CONTENT_URI, Job.Definitions.ALL, Job.Definitions.STATUS + " = ?", new String[]{"COMPLETED"}, null);
			if(jobCursor != null){
				jobCursor.moveToFirst();
				//for each completed job:
				while(!jobCursor.isAfterLast()){
					
					
					int jobId = jobCursor.getInt(0);
					
					Submission submission = new Submission();
					submission.setJobId(jobId);
					submission.setUsername(account.name);

					// retrieve dataitems for them and combine into a submission
					Cursor diCursor = provider.query(Dataitem.Definitions.CONTENT_URI, 
													 new String[]{Dataitem.Definitions.PAGENAME, Dataitem.Definitions.NAME, Dataitem.Definitions.TYPE, Dataitem.Definitions.VALUE}, 
													 Dataitem.Definitions.JOB_ID + " = ?", 
													 new String[]{""+jobId}, 
													 null);
					if(diCursor != null){
						diCursor.moveToFirst();
						while(!diCursor.isAfterLast()){
							submission.addDataItem(new DataItem(diCursor.getString(0),diCursor.getString(1),diCursor.getString(2),diCursor.getString(3)));
							diCursor.moveToNext();
						}
						diCursor.close();
						diCursor = null;
					}

					//if successful, delete completed job and dataitems.
					boolean submitted = NetworkUtilities.submitData(context, account, authToken, submission);
					if(submitted){
						provider.delete(Dataitem.Definitions.CONTENT_URI, Dataitem.Definitions.JOB_ID + " = ?", new String[]{""+submission.getJobId()});
						provider.delete(Uri.withAppendedPath(Job.Definitions.CONTENT_URI, ""+submission.getJobId()), null, null);
					}
					jobCursor.moveToNext();
				}
				
			}
			jobCursor.close();
			
			
			jobs = NetworkUtilities.fetchJobs(context, account, authToken, new Date(lastUpdated));
			/**
			 * Commented out while developing/testing job synch
			 */
//			definitions = NetworkUtilities.fetchTaskDefinitions(account, authToken, new Date(lastUpdated));
			
			
			lastUpdated = System.currentTimeMillis(); //set to the current date/time
			//TODO: This should be handled by a TaskManager or similar really
			//for each TaskDefinition, we need to insert it into the SQLLite database
			for(TaskDefinition definition: definitions){
				storeTaskDefinition(provider, definition);
			}
			
			for(JobDefinition job: jobs){
				Log.d(TAG, "Adding a job to database: " + job);
				if(job != null){
					
					storeTaskDefinition(provider, job.getDefinition());
					
					ContentValues values = new ContentValues();
					values.put(Job.Definitions._ID, job.getId());
					values.put(Job.Definitions.NAME, job.getName());
					values.put(Job.Definitions.TASK_DEFINITION_ID, job.getDefinition().getId());
					values.put(Job.Definitions.CREATED, job.getCreated().getTime());
					values.put(Job.Definitions.DUE, job.getDue().getTime());
					values.put(Job.Definitions.STATUS, job.getStatus());
					if(job.getGroup() != null){
						values.put(Job.Definitions.GROUP, job.getGroup());
					}
					
					Uri jobUri = ContentUris.withAppendedId(Job.Definitions.CONTENT_URI, job.getId());

					//TODO: If we have a job already on the device, but it doesn't come through in the list of newly synched jobs, then we want to remove it from the device.
					//This is because the job could have been completed on another device or by other means.
					
					//TODO: Stop fetched jobs blatting any saved jobs - particuarly the status - if the job has previously been completed.
					try{
						Cursor cursor = provider.query(jobUri, null, null, null, null);
						if(cursor.moveToFirst()){
							Log.d(TAG, "Updating job " + job.getId());
							values.remove(Job.Definitions.STATUS);
							provider.update(jobUri, values, null, null);
						} else {
							Log.d(TAG, "Inserting new job " + job.getId());
							provider.insert(Job.Definitions.CONTENT_URI, values);
						}
						cursor.close();
						cursor = null;
					} catch(RemoteException re){
						Log.e(TAG, "RemoteException", re);
					}
				}else{
					Log.d(TAG, "Null job");
				}
			}
			setLastUpdatedDate(account, lastUpdated);
		}catch(final AuthenticatorException ae){
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "AuthenticatorException", ae);
		}catch(final AuthenticationException ae){
			accountManager.invalidateAuthToken(Constants.ACCOUNT_TYPE, authToken);
	        syncResult.stats.numAuthExceptions++;
			Log.e(TAG, "AuthenticationException", ae);
		}catch(final JSONException je){
			Log.e(TAG, "JSONException", je);
			syncResult.stats.numParseExceptions++;
		}catch(final OperationCanceledException oce){
			Log.e(TAG, "OperationCanceledException", oce);
		}catch(final IOException ioe){
			Log.e(TAG, "IOException", ioe);
			syncResult.stats.numIoExceptions++;
		} catch (RemoteException e) {
			Log.e(TAG, "RemoteException ", e);
		}
	}

	private long getLastUpdatedDate(Account account){
		String lastUpdatedString = accountManager.getUserData(account, LAST_UPDATED_KEY);
		if(lastUpdatedString != null && lastUpdatedString.length() > 0){
			return Long.parseLong(lastUpdatedString);
		} else {
			return 0;
		}
		
	}
	
	private void setLastUpdatedDate(Account account, long lastUpdated){
		accountManager.setUserData(account, LAST_UPDATED_KEY, Long.toString(lastUpdated));
	}
}
