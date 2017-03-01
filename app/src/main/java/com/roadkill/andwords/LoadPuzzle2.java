package com.roadkill.andwords;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class LoadPuzzle2 extends ListActivity
{
	static final int SUNDAY            = 0x00001;
	static final int MONDAY            = 0x00002;
	static final int TUESDAY           = 0x00004;
	static final int WEDNESDAY         = 0x00008;
	static final int THURSDAY          = 0x00010;
	static final int FRIDAY            = 0x00020;
	static final int SATURDAY          = 0x00040;
	
	static final int WEEKLY = SUNDAY|MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY;
	static final int MON_TO_SAT = MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY;
	static final int MON_TO_FRI = MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY;

	static final int FLAG_NO_FLAGS     = 0x000000;
	static final int FLAG_NO_ARCHIVE   = 0x000001;
	
	public static final String LOAD_PUZZLE = "com.roadkill.andwords.LoadPuzzle2.LOAD_PUZZLE";
	
	public static class PuzzleInfo
	{
		public int mDays;
		public String mDisplayName;
		public int mPuzzleID;
		public boolean mCompleted;
		public String mFilePath;
		public int mType;
		public int mFlags;
		
		PuzzleInfo(int d, String name, int id, int nType, int nFlags)
		{
			mDays = d;
			mDisplayName = name;
			mPuzzleID = id;
			mCompleted = false;
			mType = nType;
			mFlags = nFlags;
		}
	};
	
	public static class PuzzleInstance
	{
		public PuzzleInfo mPuzzleInfo;
		public Date mDate;
		public String mFilePath;
		
		PuzzleInstance(PuzzleInfo info, Date d)
		{
			mPuzzleInfo = info;
			mDate = d;
			
			mFilePath = DownloadManager.buildPuzzlePath(info.mDisplayName, d, info.mType);
		}
		
		PuzzleInstance()
		{
			
		}
	};
	
	static PuzzleInfo mPuzzles[] = { 
					new PuzzleInfo(WEEKLY, "NYT Daily", AndWords.NYT_DAILY, AndWords.ACROSS_LITE, FLAG_NO_FLAGS),
					new PuzzleInfo(WEEKLY, "LA Times", AndWords.LA_TIMES, AndWords.UCLICK, FLAG_NO_FLAGS),
					new PuzzleInfo(WEEKLY, "Creators Syndicate", AndWords.BRAINS_ONLY, AndWords.BRAINSONLY_COM, FLAG_NO_FLAGS),
					new PuzzleInfo(WEEKLY, "Universal", AndWords.UNIVERSAL, AndWords.UCLICK, FLAG_NO_FLAGS),
					new PuzzleInfo(WEEKLY, "USA Today", AndWords.USATODAY, AndWords.UCLICK, FLAG_NO_FLAGS),
					new PuzzleInfo(WEEKLY, "Daily American", AndWords.DAILY_AMERICAN, AndWords.UCLICK, FLAG_NO_FLAGS),
					new PuzzleInfo(WEEKLY, "Online Crosswords #1", AndWords.OCP_1, AndWords.ONLINE_CROSSWORDS, FLAG_NO_ARCHIVE),
					new PuzzleInfo(WEEKLY, "Online Crosswords #2", AndWords.OCP_2, AndWords.ONLINE_CROSSWORDS, FLAG_NO_ARCHIVE),
					new PuzzleInfo(WEEKLY, "Online Crosswords #3", AndWords.OCP_3, AndWords.ONLINE_CROSSWORDS, FLAG_NO_ARCHIVE),
					new PuzzleInfo(WEEKLY, "Online Crosswords #4", AndWords.OCP_4, AndWords.ONLINE_CROSSWORDS, FLAG_NO_ARCHIVE),
					new PuzzleInfo(WEEKLY, "Online Crosswords #5", AndWords.OCP_5, AndWords.ONLINE_CROSSWORDS, FLAG_NO_ARCHIVE),
					new PuzzleInfo(WEEKLY, "Online Crosswords #6", AndWords.OCP_6, AndWords.ONLINE_CROSSWORDS, FLAG_NO_ARCHIVE),
					new PuzzleInfo(WEEKLY, "Online Crosswords #7", AndWords.OCP_7, AndWords.ONLINE_CROSSWORDS, FLAG_NO_ARCHIVE),
					new PuzzleInfo(MON_TO_SAT, "Sheffer Daily", AndWords.KFSSHEFFER, AndWords.KFS, FLAG_NO_FLAGS),
					new PuzzleInfo(MON_TO_SAT, "Joseph Daily", AndWords.KFSJOSEPH, AndWords.KFS, FLAG_NO_FLAGS),
					new PuzzleInfo(MON_TO_SAT, "Wall Street Journal", AndWords.GNY, AndWords.GNY_JSON, FLAG_NO_FLAGS),
					new PuzzleInfo(MONDAY, "NY Times Classic 1", AndWords.NYTCLASSIC, AndWords.NYT, FLAG_NO_FLAGS),
					new PuzzleInfo(MONDAY, "NY Times Classic 2", AndWords.NYTCLASSIC_TWO, AndWords.NYT, FLAG_NO_FLAGS),
					new PuzzleInfo(MONDAY, "Puzzles By Fred", AndWords.PUZZLES_BY_FRED, AndWords.ACROSS_LITE, FLAG_NO_FLAGS),
					new PuzzleInfo(TUESDAY, "Brendan Emmett Quigley", AndWords.BRENDAN_EMMETT_QUIGLY_TUESDAY, AndWords.ACROSS_LITE, FLAG_NO_FLAGS),
					new PuzzleInfo(THURSDAY, "Jonesin", AndWords.JONESIN, AndWords.ACROSS_LITE, FLAG_NO_FLAGS),
					new PuzzleInfo(FRIDAY, "Chronicle of Higher Education", AndWords.CHRONICLE_OF_HIGHER_EDUCATION, AndWords.ACROSS_LITE, FLAG_NO_FLAGS),
					new PuzzleInfo(FRIDAY, "Brendan Emmett Quigley", AndWords.BRENDAN_EMMETT_QUIGLY_FRIDAY, AndWords.ACROSS_LITE, FLAG_NO_FLAGS),
					new PuzzleInfo(SUNDAY, "King Features", AndWords.KFSPREMIER, AndWords.KFS, FLAG_NO_FLAGS),
					new PuzzleInfo(SUNDAY, "Cruciverbalist at Law", AndWords.CRUCIVERBALIST, AndWords.ACROSS_LITE, FLAG_NO_FLAGS),
					new PuzzleInfo(SUNDAY, "Boston Globe", AndWords.BOSTON_GLOBE, AndWords.WSJ, FLAG_NO_ARCHIVE),
	};
	
	public class MyArrayAdapter extends ArrayAdapter<PuzzleInstance> 
	{
	    public MyArrayAdapter(Context context, int ID,  ArrayList<PuzzleInstance> users) 
	    {
	       super(context, ID, users);
	    }

	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) 
	    {
	       // Get the data item for this position
	    	PuzzleInstance info = getItem(position);    
	    	
	       // Check if an existing view is being reused, otherwise inflate the view
	       if (convertView == null) 
	       {
	          convertView = LayoutInflater.from(getContext()).inflate(R.layout.simple_list_item, parent, false);
	       }

	       TextView puzzle = (TextView) convertView.findViewById(R.id.list_item_text);
	       ImageView completed = (ImageView) convertView.findViewById(R.id.list_item_completed);

			if (puzzle != null)
			{
				if (info.mPuzzleInfo.mDisplayName.startsWith("-"))
				{
					puzzle.setText(info.mPuzzleInfo.mDisplayName.substring(1));
					puzzle.setEnabled(false);
					puzzle.setClickable(true);
					puzzle.setTextSize(15.0f);
					
					completed.setVisibility(View.INVISIBLE);
				}
				else
				{
					puzzle.setText("   " + info.mPuzzleInfo.mDisplayName);
					puzzle.setEnabled(true);
					puzzle.setClickable(false);
					puzzle.setTextSize(20.0f);
					File f = new File(info.mFilePath);
					
					if (f.exists())
					{
						if (DownloadManager.isPuzzleDone(info.mFilePath))
						{
							completed.setVisibility(View.VISIBLE);
						}
						else
							completed.setVisibility(View.INVISIBLE);
					}
					else
					{
						completed.setVisibility(View.INVISIBLE);
					}
				}
			}
			
	       // Return the completed view to render on screen
	       return convertView;
	   }
	}	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		setTitle("Load Puzzle");
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		boolean useNYTDaily = false;
		
		if (prefs != null)
		{
			useNYTDaily  = prefs.getBoolean(getString(R.string.enable_NYT_Daily), false);
		}
		
		final ListView v = getListView();
		
		v.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) 
			{
				MyArrayAdapter a = (MyArrayAdapter) ((ListView)arg0).getAdapter();
				
				final PuzzleInstance pi = a.getItem(arg2);

				if (pi != null)
				{
					File f = new File(pi.mFilePath);
					if (f.exists())
					{
			    		AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
			    		builder.setTitle("Remove Puzzle");
			    		builder.setMessage("Delete puzzle file and re-download?");
						builder.setNegativeButton("Cancel", null);
			    		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) 
							{
								File puz = new File(pi.mFilePath);
								
								puz.delete();
								
								dialog.dismiss();
								
								Intent i = new Intent();
								i.putExtra("Date", pi.mDate.getTime());
								i.putExtra("Name", pi.mPuzzleInfo.mDisplayName);
								
								setResult(pi.mPuzzleInfo.mPuzzleID, i);
								finish();
							}
						});
			    		
			    		AlertDialog d = builder.create();
			    		d.show();
					}
				}
				
				return true;
			}
		});
		MyArrayAdapter adapter = new MyArrayAdapter(this, R.layout.simple_list_item,  new ArrayList<PuzzleInstance>());
		setListAdapter(adapter);
		
		Calendar c = Calendar.getInstance();
		
		for (int i = 0; i < 13; i++)
		{
			Date d = new Date(System.currentTimeMillis() - (i * 86400000));
			c.setTime(d);
			
			int dow = c.get(Calendar.DAY_OF_WEEK);
			SimpleDateFormat dateFormat = new SimpleDateFormat("E - MM/dd/yyyy", Locale.US);
			
			PuzzleInstance p = new PuzzleInstance();
			String s;
			
			s = "-" + dateFormat.format(d);
			
			PuzzleInfo pi = new PuzzleInfo(SUNDAY, s, 0, AndWords.UCLICK, FLAG_NO_FLAGS);
			p.mDate = d;
			p.mPuzzleInfo = pi;
			
			adapter.add(p);
			
			int nDay = SUNDAY;
			
			if (dow == Calendar.MONDAY)
				nDay = MONDAY;
			else if (dow == Calendar.TUESDAY)
				nDay = TUESDAY;
			else if (dow == Calendar.WEDNESDAY)
				nDay = WEDNESDAY;
			else if (dow == Calendar.THURSDAY)
				nDay = THURSDAY;
			else if (dow == Calendar.FRIDAY)
				nDay = FRIDAY;
			else if (dow == Calendar.SATURDAY)
				nDay = SATURDAY;
			
			for (int j = 0; j < mPuzzles.length; j++)
			{
				// Don't add the NYT daily unless specifically enabled in prefs
				if (mPuzzles[j].mPuzzleID == AndWords.NYT_DAILY && useNYTDaily != true)
					continue;
				
				if ((mPuzzles[j].mDays & nDay) > 0)
				{
					p = new PuzzleInstance(mPuzzles[j], d);
					
					if ((mPuzzles[j].mFlags & FLAG_NO_ARCHIVE) > 0)
					{
						if (i == 0)
							adapter.add(p);
						else
						{
							// if we've downloaded the puzzle previously, add it to the list
							File f = new File(p.mFilePath);
							
							if (f.exists())
								adapter.add(p);
						}
					}
					else
						adapter.add(p);
				}
			}
		}
		
		String screenLayout = prefs.getString(getString(R.string.screen_orientation), getString(R.string.screen_orientation_default_value));
		
		// clean up any old puzzles
		DownloadManager.deleteOldPuzzles();
		
		// force the screen layout if requested to do so
		if (screenLayout.equals("1"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		else if (screenLayout.equals("2"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) 
	{
		super.onListItemClick(l, v, position, id);
		
		MyArrayAdapter a = (MyArrayAdapter) l.getAdapter();
		
		PuzzleInstance pi = a.getItem(position);

		if (pi != null)
		{
			Intent i = new Intent();
			i.putExtra("Date", pi.mDate.getTime());
			i.putExtra("Name", pi.mPuzzleInfo.mDisplayName);
			
			setResult(pi.mPuzzleInfo.mPuzzleID, i);
			finish();
		}
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
    	menu.add(Menu.NONE, AndWords.EDIT_PREFS_ID, Menu.NONE, "Edit Preferences");
    	menu.add(Menu.NONE, AndWords.LOAD_CUSTOM_URL, Menu.NONE, "Load URL");
    	menu.add(Menu.NONE, AndWords.HELP_ID, Menu.NONE, "Help");
    	menu.add(Menu.NONE, AndWords.EXIT_ID, Menu.NONE, "Exit");
    	return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
    	switch (item.getItemId())
    	{
    	case AndWords.EDIT_PREFS_ID:
    		startActivity(new Intent(this, Preferences.class));
    		break;
    	case AndWords.HELP_ID:
    		Uri u = Uri.parse("http://software.roadkill.com/AndWords/");
    		Intent helpIntent = new Intent(Intent.ACTION_VIEW, u);
    		startActivity(helpIntent);
    		break;
    	case AndWords.LOAD_CUSTOM_URL:
    		setResult(AndWords.LOAD_CUSTOM_URL);
    		finish();
    		break;
    	case AndWords.EXIT_ID:
    		setResult(AndWords.EXIT_ID);
    		finish();
    		break;
    	}
    	
		return true;
	}
}
