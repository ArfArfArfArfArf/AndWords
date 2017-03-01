package com.roadkill.andwords;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;


@SuppressLint("NewApi")
public class AndWords extends Activity implements AndGrid.PuzzleCompletedListener, OnSharedPreferenceChangeListener {
	public static final boolean DEBUG = false;

	private static final String TAG = "andwords.AndWords";

	private AndGrid mGrid;

	// menu IDs
	public static final int EDIT_PREFS_ID = 101;
	public static final int HELP_ID = 102;
	public static final int EXIT_ID = 104;

	// Source Types
	public static final int LOAD_FILE = 1;
	public static final int LOAD_URL = 2;

	// Puzzle Types
	public static final int ACROSS_LITE = 1;
	public static final int BRAINSONLY_COM = 2;
	public static final int UCLICK = 3;
	public static final int JSZ = 4;
	public static final int KFS = 5;
	public static final int NYT = 6;
	public static final int WSJ = 7;
	public static final int ONLINE_CROSSWORDS = 8;
	public static final int GNY_JSON = 9;

	// activity return codes
	public static final int PUZZLE_LOADED = 1;

	// Load Puzzle Result Codes
	public static final int NYT_DAILY = 39;
	public static final int OCP_1 = 40;
	public static final int OCP_2 = 41;
	public static final int OCP_3 = 42;
	public static final int OCP_4 = 43;
	public static final int OCP_5 = 44;
	public static final int OCP_6 = 45;
	public static final int OCP_7 = 46;
	public static final int LA_TIMES = 1;
	public static final int WALL_STREET_JOURNAL = 2;
	public static final int BRAINS_ONLY = 6;
	public static final int USATODAY = 8;
	public static final int UNIVERSAL = 10;
	public static final int DAILY_AMERICAN = 36;
	public static final int BOATLOAD = 37;
	public static final int CRUCIVERBALIST = 38;
	public static final int KFSPREMIER = 23;
	public static final int KFSJOSEPH = 24;
	public static final int KFSSHEFFER = 25;
	public static final int NYTCLASSIC_TWO = 34;
	public static final int GNY = 35;
	public static final int NYTCLASSIC = 7;
	public static final int PUZZLES_BY_FRED = 30;
	public static final int BRENDAN_EMMETT_QUIGLY_TUESDAY = 29;
	public static final int BRENDAN_EMMETT_QUIGLY_FRIDAY = 32;
	public static final int BOSTON_GLOBE = 12;
	public static final int JONESIN = 16;
	public static final int LOAD_CUSTOM_URL = 19;
	public static final int CHRONICLE_OF_HIGHER_EDUCATION = 5;

	// prefs for saving state
	private static final String PREF_TIMER = "Timer";
	private static final String PREF_LAST_URL = "Last URL";
	public static final String PREF_CELL_HEIGHT = "Cell Height";
	public static final String PREF_CELL_WIDTH = "Cell Width";
	public static final String PREF_LAST_PUZZLE = "Last Puzzle";
	public static final String PREF_LAST_PUZZLE_TYPE = "Last Puzzle Type";

	private long mPauseTime = 0L;

	Toast mToast;
	ProgressDialog mProgressDialog;

	private boolean mShowingPuzzleList = false;
	private boolean mShowPuzzleInfoOnLoad = false;

	private EditText mCustomURLInput;

	private boolean mDownloadingPuzzle;

	public static String PUZZLE_PATH;

	private PuzzleDownloader m_downloader = null;

	public void onCreate(Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);

			// no window title to save screen real estate for pre-3.x builds
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
				requestWindowFeature(Window.FEATURE_NO_TITLE);

			setContentView(R.layout.main);
		} catch (Exception e) {
			Log.e("ERROR", "ERROR IN CODE:" + e.toString());

			e.printStackTrace();
		}

		mGrid = (AndGrid) findViewById(R.id.grid);

		mGrid.setPuzzleListener(this);
		mGrid.setParent(this);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		prefs.registerOnSharedPreferenceChangeListener(this);

		String screenLayout = prefs.getString(getString(R.string.screen_orientation), getString(R.string.screen_orientation_default_value));

		mShowPuzzleInfoOnLoad = prefs.getBoolean(getString(R.string.showpuzzleinfo), false);

		// force the screen layout if requested to do so
		if (screenLayout.equals("1"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		else if (screenLayout.equals("2"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		PUZZLE_PATH = getExternalFilesDir(null).getAbsolutePath();

		String strLastPuzzle = prefs.getString(PREF_LAST_PUZZLE, "");
		int lastPuzzleType = prefs.getInt(PREF_LAST_PUZZLE_TYPE, ACROSS_LITE);

		// we are downloading a puzzle or the grid already has puzzle info
		// no need to do anything here
		if (mDownloadingPuzzle || (mGrid != null && mGrid.getInfo() != null))
			return;


		// check to see if we were killed off - if so, reload the last puzzle
		try {
			long puzTimer = prefs.getLong(PREF_TIMER, 0);

			try {
				Editor e = prefs.edit();
				e.remove(PREF_TIMER);
				e.commit();
			} catch (Exception e) {
				Log.e(TAG, e.getLocalizedMessage());
			}

			File f = new File(strLastPuzzle);


			long time = System.currentTimeMillis() - puzTimer;

			if (f.exists()) {
				PuzzleDownloader d = new PuzzleDownloader(this, true, lastPuzzleType, time, strLastPuzzle, "", "");
				d.execute("");

				mProgressDialog = ProgressDialog.show(this, "", "Resuming puzzle ...", true);

				return;
			}
		} catch (Exception e) {
			Log.e(TAG, e.getLocalizedMessage());
		}

		Intent i = new Intent(LoadPuzzle2.LOAD_PUZZLE);
		startActivityForResult(i, PUZZLE_LOADED);

		mShowingPuzzleList = true;
	}

	private void loadPuzzle(HttpURLConnection uc) {
		if (mGrid != null)
			mGrid.setInfo(null);

		removePuzzleInfo();

		if (m_downloader != null)
			m_downloader = null;

		m_downloader = new PuzzleDownloader(this, uc);
		m_downloader.execute("");

		mDownloadingPuzzle = true;

		mProgressDialog = ProgressDialog.show(this, "", "Downloading puzzle", true);

	}

	private void loadPuzzle(int puzID, int type, String strURL, String strName, String strPath, String strUsername, String strPassword) {
		if (mGrid != null)
			mGrid.setInfo(null);

		removePuzzleInfo();

		if (m_downloader != null)
			m_downloader = null;

		if (DEBUG)
			Log.v(TAG, "Loading URL: " + strURL + ", type: " + type + ", ID: " + puzID);

		m_downloader = new PuzzleDownloader(this, puzID, type, strName, strPath, strUsername, strPassword);
		m_downloader.execute(strURL);

		mDownloadingPuzzle = true;

		String strMessage = "Downloading puzzle";

		if (m_downloader.mFromFile)
			strMessage = "Resuming puzzle";

		mProgressDialog = ProgressDialog.show(this, "", strMessage, true);
	}

	public class PuzzleDownloader extends AsyncTask<String, Void, CrosswordInfo> {
		private void cancelDownload() {
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}

			try {
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setTitle("Unable to download puzzle");
				builder.setMessage("Puzzle Timeout");
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent i = new Intent(LoadPuzzle2.LOAD_PUZZLE);
						startActivityForResult(i, PUZZLE_LOADED);
					}
				});

				AlertDialog d = builder.create();
				d.show();
			} catch (Exception e) {
				Intent in = new Intent(LoadPuzzle2.LOAD_PUZZLE);
				startActivityForResult(in, PUZZLE_LOADED);
			}

		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			cancelDownload();
		}

		//		@Override
		protected void onCancelled(CrosswordInfo result) {
			super.onCancelled(result);
			cancelDownload();
		}

		int mPuzID;
		int mType;
		Context mContext;
		HttpURLConnection mUC;
		boolean mFromFile;
		long mPuzTime;
		String mName;
		String mPuzzlePath;
		String mUsername;
		String mPassword;

		private Handler mHandler = new Handler();

		private Runnable mTimeoutEvent = new Runnable() {
			public void run() {
				if (getStatus() != Status.FINISHED) {
					if (!DEBUG)
						cancel(true);
				}
			}
		};

		public PuzzleDownloader(Context context, boolean fromFile, int type, long time, String path, String username, String password) {
			mContext = context;
			mFromFile = fromFile;
			mType = type;
			mPuzTime = time;
			mPuzzlePath = path;
			mUsername = username;
			mPassword = password;
		}

		public PuzzleDownloader(Context context, HttpURLConnection uc) {
			mUC = uc;
			mContext = context;
			mType = ACROSS_LITE;
		}

		public PuzzleDownloader(Context context, int puzID, int type, String name, String path, String username, String password) {
			mPuzID = puzID;
			mType = type;
			mContext = context;
			mName = name;
			mPuzzlePath = path;
			mUsername = username;
			mPassword = password;

			File f = new File(path);

			if (f.exists())
				mFromFile = true;
			else {
				String s = DownloadManager.getDonePath(path);
				f = new File(s);
				if (f.exists()) {
					mPuzzlePath = s;
					mFromFile = true;
				}
			}
		}

		@Override
		protected CrosswordInfo doInBackground(String... params) {
			CrosswordInfo i = new CrosswordInfo();
			i.mPuzzleID = mPuzID;
			i.mPuzzleName = mName;
			i.mPuzzlePath = mPuzzlePath;

			// set up a timeout callback to interrupt the download in 20 seconds
			mHandler.removeCallbacks(mTimeoutEvent);
			mHandler.postDelayed(mTimeoutEvent, 20000);

			if (mType == ACROSS_LITE) {
				if (!mFromFile) {
					AcrossLiteReader.read(i, params[0], mUC, mUsername, mPassword);
				} else {
					AcrossLiteReader.read(i);
				}
			} else if (mType == KFS) {
				if (!mFromFile) {
					KingFeaturesReader.read(i, params[0]);

					if (params[0].contains("sheffer"))
						i.strAuthor = "Eugene Shefferd";
					else if (params[0].contains("joseph"))
						i.strAuthor = "Thomas Joseph";
				} else {
					KingFeaturesReader.read(i);
				}

				i.strCopyright = "King Features Syndicate";
			} else if (mType == BRAINSONLY_COM) {
				if (!mFromFile)
					ThinksComReader.read(i, params[0]);
				else
					ThinksComReader.readFromFile(i);
			} else if (mType == UCLICK) {
				if (!mFromFile) {
					UCLickReader r = new UCLickReader(i, params[0]);
				} else {
					UCLickReader r = new UCLickReader(i, new File(mPuzzlePath));
				}

			} else if (mType == ONLINE_CROSSWORDS) {
				if (!mFromFile) {
					OnlinePuzzlesReader r = new OnlinePuzzlesReader(i, params[0]);
				} else {
					OnlinePuzzlesReader r = new OnlinePuzzlesReader(i, new File(mPuzzlePath));
				}
			} else if (mType == NYT) {
				if (!mFromFile)
					NYTReader.read(i, params[0]);
				else
					NYTReader.read(i);
			} else if (mType == GNY_JSON) {
				if (!mFromFile)
					GNYReader.read(i, params[0]);
				else
					GNYReader.read(i);
			} else if (mType == WSJ) {
				if (!mFromFile)
					WSJReader.read(i, params[0]);
				else
					WSJReader.readFromFile(i);
			}

			if (i.mLoadComplete) {
				if ((getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES)
						|| (getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_UNDEFINED)) {
					mGrid.showKeyboard(true);
				}
			}


			mDownloadingPuzzle = false;

			for (int l = 0; l < i.nHeight; l++) {
				for (int k = 0; k < i.nWidth; k++) {
					if (i.mDiagram[l][k] != i.mSolution[l][k])
						return i;
				}
			}

			i.mPuzzleComplete = true;


			return i;
		}

		@Override
		protected void onPostExecute(CrosswordInfo i) {
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}

			// cancel the timeout callback
			mHandler.removeCallbacks(mTimeoutEvent);

			if (i != null && !i.mLoadComplete) {
				try {
					Log.e("ANDWORDS", "ERROR: " + i.mErrorString);

					AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
					builder.setTitle("Problem with puzzle");
					builder.setMessage(i.mErrorString);
					builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Intent i = new Intent(LoadPuzzle2.LOAD_PUZZLE);
							startActivityForResult(i, PUZZLE_LOADED);
						}
					});

					AlertDialog d = builder.create();
					d.show();
				} catch (Exception e) {
					Intent in = new Intent(LoadPuzzle2.LOAD_PUZZLE);
					startActivityForResult(in, PUZZLE_LOADED);
				}
			} else if (mGrid != null) {
				mGrid.setInfo(i);

				if (mPuzTime != 0L)
					mGrid.setTimer(System.currentTimeMillis() - mPuzTime);

				mGrid.invalidate();

				// store this as the last puzzle loaded
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				Editor e = prefs.edit();

				e.putLong(PREF_TIMER, System.currentTimeMillis() - mGrid.getTimer());

				e.putString(PREF_LAST_PUZZLE, i.mPuzzlePath);
				e.putInt(PREF_LAST_PUZZLE_TYPE, i.mPuzzleType);

				e.commit();

				if (mShowPuzzleInfoOnLoad)
					showPuzzleInfo();
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if ((newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES)
				|| (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_UNDEFINED)) {
			if (mGrid != null)
				mGrid.showKeyboard(true);
		} else {
			if (mGrid != null)
				mGrid.showKeyboard(false);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();

		inflater.inflate(R.menu.options_menu, menu);

		return true;
	}

	private void reveal(CharSequence which) {
		if (which == null || mGrid == null)
			return;


		String[] revealValues = getResources().getStringArray(R.array.reveal_values);

		if (which.equals(revealValues[0])) {
			mGrid.revealLetter();
		} else if (which.equals(revealValues[1])) {
			mGrid.revealWord();
		} else if (which.equals(revealValues[2])) {
			mGrid.revealSolution();
		}
	}

	private void doExit() {
		if (mGrid != null && mGrid.isModified()) {
			savePuzzle();
		}

		finish();
	}

	private void checkAnswers() {
		if (mGrid != null) {
			if (!mGrid.getInfo().mScrambled)
				mGrid.checkAnswers();
			else {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Unable to check answers");
				builder.setMessage("The solution to this puzzle is encrypted and cannot be checked.");
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

				AlertDialog d = builder.create();
				d.show();

			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		AlertDialog.Builder builder;

		switch (item.getItemId()) {

			case R.id.check:
				checkAnswers();
				break;

			case R.id.help:
				Uri u = Uri.parse("http://www.roadkillapps.com/AndWords/");
				Intent helpIntent = new Intent(Intent.ACTION_VIEW, u);
				startActivity(helpIntent);
				break;

			case R.id.exit:
				doExit();
				break;

			case R.id.load:
				if (mGrid != null) {
					if (mGrid.isModified()) {
						savePuzzle();
					}

					mGrid.setInfo(null);
				}

				Intent i = new Intent(LoadPuzzle2.LOAD_PUZZLE);
				startActivityForResult(i, PUZZLE_LOADED);
				break;

			case R.id.preferences:
				startActivity(new Intent(this, Preferences.class));
				break;

			case R.id.reveal:
				if (mGrid.getInfo().mScrambled) {
					builder = new AlertDialog.Builder(this);
					builder.setTitle("Unable to reveal answers");
					builder.setMessage("The solution to this puzzle is encrypted and clues cannot be revealed.");
					builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});

					AlertDialog d = builder.create();
					d.show();
					break;
				}

				final CharSequence[] reveals = getResources().getStringArray(R.array.reveal_values);

				builder = new AlertDialog.Builder(this);
				builder.setTitle("Reveal ...");
				builder.setItems(reveals, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						reveal(reveals[item]);
					}
				});
				builder.setCancelable(true);
				builder.setNegativeButton("Cancel", null);

				AlertDialog d = builder.create();
				d.show();

				break;

			case R.id.info:
				showPuzzleInfo();
				break;
		}

		return true;
	}

	private void showPuzzleInfo() {
		if (mGrid == null)
			return;

		CrosswordInfo info = mGrid.getInfo();

		if (info == null)
			return;

		AlertDialog.Builder b = new AlertDialog.Builder(this);
		String strMessage = "";

		if (info.mPuzzleName != null && !info.mPuzzleName.equalsIgnoreCase("")) {
			b.setTitle(info.mPuzzleName);

			if (info.strTitle != null && !info.strTitle.equalsIgnoreCase("null"))
				strMessage += "Title: " + info.strTitle + "\n";
		} else {
			b.setTitle(info.strTitle);
		}

		if (info.strAuthor != null && !info.strAuthor.equalsIgnoreCase("null"))
			strMessage += "Author: " + info.strAuthor + "\n";

		if (info.strCopyright != null && !info.strCopyright.equalsIgnoreCase("null"))
			strMessage += "Copyright: " + info.strCopyright + "\n";

		if (info.strNotes != null && !info.strNotes.equalsIgnoreCase("null"))
			strMessage += info.strNotes;

		b.setMessage(strMessage);
		b.setPositiveButton("OK", null);
		AlertDialog dlg = b.create();
		dlg.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_CANCELED) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

			String strLastPuzzle = prefs.getString(PREF_LAST_PUZZLE, "");
			int lastPuzzleType = prefs.getInt(PREF_LAST_PUZZLE_TYPE, ACROSS_LITE);

			long puzTimer = prefs.getLong(PREF_TIMER, 0);

			try {
				Editor e = prefs.edit();
				e.remove(PREF_TIMER);
				e.commit();
			} catch (Exception e) {
				Log.e(TAG, e.getLocalizedMessage());
			}

			File f = new File(strLastPuzzle);
			long time = System.currentTimeMillis() - puzTimer;

			if (f.exists()) {
				PuzzleDownloader d = new PuzzleDownloader(this, true, lastPuzzleType, time, strLastPuzzle, "", "");
				d.execute("");

				mProgressDialog = ProgressDialog.show(this, "", "Resuming puzzle ...", true);

				return;
			} else {
				finish();
			}

			return;
		}

		if (requestCode == PUZZLE_LOADED) {
			// Exit menu called from LoadPuzzle Activity
			if (resultCode == EXIT_ID) {
				finish();
				return;
			}

			mShowingPuzzleList = false;
			loadRequestedPuzzle(resultCode, data);
		}

	}

	private void loadRequestedPuzzle(int resultCode, Intent data) {
		String strUsername = "";
		String strPassword = "";

		Calendar c;
		c = Calendar.getInstance();

		int nDays;
		SimpleDateFormat dateFormat;

		String strURL;
		int nType;

		long puzzleDate = -1L;
		String puzzleName = "";
		String puzzlePath = "";

		if (data != null) {
			puzzleDate = data.getLongExtra("Date", -1L);
			puzzleName = data.getStringExtra("Name");
		}

		Date loadDate;

		if (puzzleDate != -1L)
			loadDate = new Date(puzzleDate);
		else
			loadDate = new Date();

		String roadkilldate;

		switch (resultCode) {
			case LA_TIMES:
				dateFormat = new SimpleDateFormat("yyMMdd", Locale.US);

				strURL = "http://cdn.games.arkadiumhosted.com/latimes/assets/DailyCrossword/la";
				strURL += dateFormat.format(loadDate);
				strURL += ".xml";

				nType = UCLICK;

				break;

			case OCP_1:
				strURL = "http://www.onlinecrosswords.net/en/puzzle.php?p=1";

				nType = ONLINE_CROSSWORDS;
				break;
			case OCP_2:
				strURL = "http://www.onlinecrosswords.net/en/puzzle.php?p=2";

				nType = ONLINE_CROSSWORDS;
				break;
			case OCP_3:
				strURL = "http://www.onlinecrosswords.net/en/puzzle.php?p=3";

				nType = ONLINE_CROSSWORDS;
				break;
			case OCP_4:
				strURL = "http://www.onlinecrosswords.net/en/puzzle.php?p=4";

				nType = ONLINE_CROSSWORDS;
				break;
			case OCP_5:
				strURL = "http://www.onlinecrosswords.net/en/puzzle.php?p=5";

				nType = ONLINE_CROSSWORDS;
				break;
			case OCP_6:
				strURL = "http://www.onlinecrosswords.net/en/puzzle.php?p=6";

				nType = ONLINE_CROSSWORDS;
				break;
			case OCP_7:
				strURL = "http://www.onlinecrosswords.net/en/puzzle.php?p=7";

				nType = ONLINE_CROSSWORDS;
				break;

			case DAILY_AMERICAN:
				SimpleDateFormat dir = new SimpleDateFormat("yyyy-MM", Locale.US);
				dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

				strURL = "http://bestforpuzzles.com/daily-crossword/puzzles/";

				strURL += dir.format(loadDate);
				strURL += "/dc1-";
				strURL += dateFormat.format(loadDate);
				strURL += ".js";
				nType = UCLICK;
				break;

			case BOATLOAD:
				dateFormat = new SimpleDateFormat("yyMMdd", Locale.US);

				strURL = "http://cdn.arenaconnect.arkadiumhosted.com/games-storage/daily-crossword/game/data/new_puzzles/puzzle_";

				strURL += dateFormat.format(loadDate);
				strURL += ".xml";
				nType = UCLICK;
				break;

			case WALL_STREET_JOURNAL:
				dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
				strURL = "http://blogs.wsj.com/applets/wsjxwd";
				strURL += dateFormat.format(loadDate);

				strURL += ".dat";
				nType = WSJ;
				break;


			case JONESIN:
				dateFormat = new SimpleDateFormat("yyMMdd");
				strURL = "http://herbach.dnsalias.com/Jonesin/jz";
				strURL += dateFormat.format(loadDate);

				strURL += ".puz";
				nType = ACROSS_LITE;
				break;

			case CHRONICLE_OF_HIGHER_EDUCATION:
				dateFormat = new SimpleDateFormat("yyyyMMdd");
				strURL = "http://chronicle.com/items/biz/puzzles/";
				strURL += dateFormat.format(loadDate);

				strURL += ".puz";
				nType = ACROSS_LITE;
				break;


			case BRAINS_ONLY:
				nType = BRAINSONLY_COM;
				dateFormat = new SimpleDateFormat("yyMMdd", Locale.US);

				strURL = "http://www.brainsonly.com/servlets-newsday-crossword/newsdaycrossword?date=" + dateFormat.format(loadDate);

				break;

			case GNY:
				nType = GNY_JSON;

				dateFormat = new SimpleDateFormat("yyMMdd", Locale.US);
				roadkilldate = dateFormat.format(loadDate);
				strURL = "http://puzzles.roadkillapps.com/getpuzzle.cgi?puzzle=GNY&date=" + roadkilldate;
				break;

			case NYT_DAILY:
				nType = ACROSS_LITE;
				dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

				strUsername = preferences.getString(getString(R.string.NYT_Username), "");
				strPassword = preferences.getString(getString(R.string.NYT_Password), "");
				strURL = "http://www.nytimes.com/svc/crosswords/v2/puzzle/daily-" + dateFormat.format(loadDate) + ".puz";
				break;

			case NYTCLASSIC:
				nType = NYT;
				dateFormat = new SimpleDateFormat("yyMMdd", Locale.US);
				roadkilldate = dateFormat.format(loadDate);
				strURL = "http://puzzles.roadkillapps.com/getpuzzle.cgi?puzzle=NYT1&date=" + roadkilldate;
				break;

			case NYTCLASSIC_TWO:
				nType = NYT;
				dateFormat = new SimpleDateFormat("yyMMdd", Locale.US);
				roadkilldate = dateFormat.format(loadDate);
				strURL = "http://puzzles.roadkillapps.com/getpuzzle.cgi?puzzle=NYT2&date=" + roadkilldate;
				break;

			case KFSSHEFFER:
				nType = KFS;
				dateFormat = new SimpleDateFormat("yyyyMMdd");
				strURL = "http://puzzles.kingdigital.com/javacontent/clues/sheffer/";
				strURL += dateFormat.format(loadDate);
				strURL += ".txt";
				break;

			case KFSJOSEPH:
				nType = KFS;
				dateFormat = new SimpleDateFormat("yyyyMMdd");
				strURL = "http://puzzles.kingdigital.com/javacontent/clues/joseph/";
				strURL += dateFormat.format(loadDate);
				strURL += ".txt";
				break;

			case KFSPREMIER:
				dateFormat = new SimpleDateFormat("yyyyMMdd");
				strURL = "http://puzzles.kingdigital.com/javacontent/clues/premier/";
				strURL += dateFormat.format(loadDate);

				strURL += ".txt";
				nType = KFS;
				break;

			case USATODAY:
				dateFormat = new SimpleDateFormat("yyMMdd");
				strURL = "http://www.uclick.com/puzzles/usaon/data/usaon";
				strURL += dateFormat.format(loadDate);
				strURL += "-data.xml";
				nType = UCLICK;
				break;

			case UNIVERSAL:
				dateFormat = new SimpleDateFormat("yyMMdd");
				strURL = "http://picayune.uclick.com/comics/fcx/data/fcx";
				strURL += dateFormat.format(loadDate);
				strURL += "-data.xml";
				nType = UCLICK;
				break;

			case BOSTON_GLOBE:
				strURL = "http://puzzles.roadkillapps.com/convertBG.cgi";
				nType = WSJ;
				break;

			case CRUCIVERBALIST:
				nType = ACROSS_LITE;
				dateFormat = new SimpleDateFormat("yyMMdd", Locale.US);
				roadkilldate = dateFormat.format(loadDate);
				strURL = "http://puzzles.roadkillapps.com/getpuzzle.cgi?puzzle=CVB&date=" + roadkilldate;
				break;

			case LOAD_CUSTOM_URL:
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

				String strLastURL = prefs.getString(PREF_LAST_URL, "");

				AlertDialog.Builder alert = new AlertDialog.Builder(this);

				alert.setTitle(getString(R.string.load_custom_title));
				alert.setMessage(getString(R.string.load_custom_message));

				// Set an EditText view to get user input
				mCustomURLInput = new EditText(this);
				alert.setView(mCustomURLInput);
				mCustomURLInput.setText(strLastURL);

				alert.setPositiveButton(getString(R.string.load_custom_ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = mCustomURLInput.getText().toString();

						if (value == null || value.equals("")) {
							return;
						}

						if (value.equals("1"))
							value = "http://crosswords.washingtonpost.com/wp-srv/style/crosswords/util/csserve.cgi";
						else if (value.equals("2"))
							value = "http://www.nytimes.com/premium/xword/today.puz";

						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
						Editor e = prefs.edit();
						e.putString(PREF_LAST_URL, value);
						e.commit();

						if (mGrid != null)
							mGrid.invalidate();

						if (value.endsWith(".puz"))
							loadPuzzle(LOAD_CUSTOM_URL, ACROSS_LITE, value, "", PUZZLE_PATH + "/custom.puz", "", "");
						else if (value.endsWith(".xml"))
							loadPuzzle(LOAD_CUSTOM_URL, UCLICK, value, "", PUZZLE_PATH + "/custom.xml", "", "");
						else {
							try {
								URL u = new URL(value);

								HttpURLConnection uc = (HttpURLConnection) u.openConnection();

								uc.connect();

								if (uc.getResponseCode() == HttpURLConnection.HTTP_OK) {
									if (uc.getContentType().equals("application/x-crossword")) {
										loadPuzzle(uc);
									}
								}
							} catch (Exception ex) {
								Context c = getBaseContext();

								mToast = Toast.makeText(c, "Can't download puzzle", Toast.LENGTH_LONG);
								mToast.setGravity(Gravity.CENTER, mToast.getXOffset() / 2, mToast.getYOffset() / 2);

								mToast.show();
							}

						}
					}
				});

				alert.setNegativeButton(getString(R.string.load_custom_cancel), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Intent i = new Intent(LoadPuzzle2.LOAD_PUZZLE);
						startActivityForResult(i, PUZZLE_LOADED);
					}
				});

				alert.show();

				mDownloadingPuzzle = true;

				return;

			case BRENDAN_EMMETT_QUIGLY_TUESDAY:
				nType = ACROSS_LITE;
				dateFormat = new SimpleDateFormat("yyMMdd", Locale.US);
				roadkilldate = dateFormat.format(loadDate);
				strURL = "http://puzzles.roadkillapps.com/getpuzzle.cgi?puzzle=BEQT&date=" + roadkilldate;
				break;

			case BRENDAN_EMMETT_QUIGLY_FRIDAY:
				nType = ACROSS_LITE;
				dateFormat = new SimpleDateFormat("yyMMdd", Locale.US);
				roadkilldate = dateFormat.format(loadDate);
				strURL = "http://puzzles.roadkillapps.com/getpuzzle.cgi?puzzle=BEQF&date=" + roadkilldate;
				break;

			case PUZZLES_BY_FRED:
				nType = ACROSS_LITE;
				dateFormat = new SimpleDateFormat("yyMMdd", Locale.US);
				roadkilldate = dateFormat.format(loadDate);
				strURL = "http://puzzles.roadkillapps.com/getpuzzle.cgi?puzzle=PBF&date=" + roadkilldate;
				break;

			default:
				return;
		}

		puzzlePath = DownloadManager.buildPuzzlePath(puzzleName, loadDate, nType);


		if (mGrid != null)
			mGrid.invalidate();

		loadPuzzle(resultCode, nType, strURL, puzzleName, puzzlePath, strUsername, strPassword);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();

		if (mGrid != null && mGrid.isModified())
			savePuzzle();

		finish();
	}

	void savePuzzle() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor e = prefs.edit();

		e.putLong(PREF_TIMER, System.currentTimeMillis() - mGrid.getTimer());

		if (mGrid != null) {
			CrosswordInfo i = mGrid.getInfo();

			if (i != null) {
				e.putString(PREF_LAST_PUZZLE, i.mPuzzlePath);
				e.putInt(PREF_LAST_PUZZLE_TYPE, i.mPuzzleType);

				if (i.mPuzzleType == ACROSS_LITE) {
					AcrossLiteReader.save(i);
				} else if (i.mPuzzleType == BRAINSONLY_COM) {
					ThinksComReader.save(i);
				} else if (i.mPuzzleType == UCLICK) {
					UCLickReader.save(i);
				} else if (i.mPuzzleType == KFS) {
					KingFeaturesReader.save(i);
				} else if (i.mPuzzleType == NYT) {
					NYTReader.save(i);
				} else if (i.mPuzzleType == GNY_JSON) {
					GNYReader.save(i);
				} else if (i.mPuzzleType == WSJ) {
					WSJReader.save(i);
				} else if (i.mPuzzleType == ONLINE_CROSSWORDS) {
					OnlinePuzzlesReader.save(i);
				}
			}
		}

		e.commit();
	}

	private void removePuzzleInfo() {
		try {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			Editor e = prefs.edit();

			e.remove(PREF_TIMER);

			e.commit();
		} catch (Exception e) {

		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		if (mGrid != null && mGrid.isModified()) {
			// save the current puzzle info
			if (!isFinishing()) {
				savePuzzle();
			}

			long l = System.currentTimeMillis() - mGrid.getTimer();

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			Editor e = prefs.edit();
			e.putLong(PREF_TIMER, l);
			e.commit();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mPauseTime = System.currentTimeMillis();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (mGrid != null && mShowingPuzzleList == false && mPauseTime != 0l) {
			mGrid.addTimeToTimer(System.currentTimeMillis() - mPauseTime);
		}

		// check to see if the user changed the layout ...
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		String screenLayout = prefs.getString(getString(R.string.screen_orientation), getString(R.string.screen_orientation_default_value));

		// force the screen layout if requested to do so
		if (screenLayout.equals("1"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		else if (screenLayout.equals("2"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		else if (screenLayout.equals("0"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);

		mShowPuzzleInfoOnLoad = prefs.getBoolean(getString(R.string.showpuzzleinfo), false);

		mPauseTime = 0l;

/*
		if (mIgnoreResume == true)
		{
			mIgnoreResume = false;
			return;
		}
		
		if (mShowingPuzzleList == true)
		{
			String strLastPuzzle = prefs.getString(PREF_LAST_PUZZLE, "");
			int lastPuzzleType = prefs.getInt(PREF_LAST_PUZZLE_TYPE, ACROSS_LITE);
			
			// check to see if we were killed off - if so, reload the last puzzle
			try
			{
				long puzTimer = prefs.getLong(PREF_TIMER, 0);
				
				try
				{
					Editor e = prefs.edit();
					e.remove(PREF_TIMER);
					e.commit();
				}
				catch (Exception e)
				{
				}
				
				File f = new File(strLastPuzzle);
			
				
				long time = System.currentTimeMillis() - puzTimer;
				
				if (f.exists())
				{
			    	PuzzleDownloader d = new PuzzleDownloader(this, true, lastPuzzleType, time, strLastPuzzle, "", "");
			    	d.execute("");
			    	
			    	mProgressDialog = ProgressDialog.show(this, "", "Resuming puzzle ...", true);
			    	
					return;
				}
			}
			catch (Exception e)
			{
			}
			
			mShowingPuzzleList = false;
		}	
		
		// back hit from puzzle list
		if (mShowingPuzzleList)
			finish();
		
		Intent i = new Intent(LoadPuzzle2.LOAD_PUZZLE);
		startActivityForResult(i, PUZZLE_LOADED);
		
		mShowingPuzzleList = true; */
	}

	public void puzzleCompleted() {
		if (mGrid != null) {
			CrosswordInfo i = mGrid.getInfo();

			if (i != null) {
				File f = new File(i.mPuzzlePath);
				String s = DownloadManager.getDonePath(i.mPuzzlePath);
				f.renameTo(new File(s));
				i.mPuzzlePath = s;
			}
		}

		savePuzzle();
	}

	public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
		if (mGrid != null)
			mGrid.updatePrefs();
	}

}
