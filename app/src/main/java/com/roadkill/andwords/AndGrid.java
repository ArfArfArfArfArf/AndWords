package com.roadkill.andwords;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

public class AndGrid extends View implements OnLongClickListener
{
	private enum Direction { ACROSS, DOWN }

	Direction mDirection = Direction.ACROSS;
	
	private final static String ROCKER_WORD = "Word";
	
	public int mWidth;
	public int mHeight;
	private char[][] mLayout;
	
	private float mCellHeight = 30.0f;
	private float mCellWidth = 30.0f;
	
	private static final float CELL_BORDER = 1.0f;
	private static final float DOUBLE_CELL_BORDER = 2.0f;
	
	private static float ONE_CLUE_OFFSET = 40.0f;
	private static float TWO_CLUES_OFFSET = 72.0f;
	
	private String mRocker = ROCKER_WORD;
	
	private float mClueOffset = ONE_CLUE_OFFSET;
	
	// used when pinching
	private float mLastZoomSpacing = 0.0f;
	
	private boolean mShiftDown = false;
	
	// used to indicate the user is scrolling/zooming the grid
	private boolean mMoving = false;
	
	// used for scrolling
	private float mXOffset = 0.0f;
	private float mYOffset = 0.0f;
	
	// used for scrolling the clue
	private float mClueXOffset = 0.0f;
	
	private float mClueWidth = 0.0f;
	private String mStrLastAcrossClue = "";
	private String mStrLastDownClue = "";
	
	// font sizes for cell numbers/text
	final float mClueNumberSize = 10.0f;
	final float mGridCharSize = 20.0f;
	private float mClueTextSize = 30.0f;
	
	// various paints for drawing lines/cells/text
	private Paint mGridPaint;
	private Paint mFillPaint;
	private Paint mNumberPaint;
	private Paint mCharacterPaint;
	private Paint mWrongCharacterPaint;
	private Paint mSelectedRowPaint;
	private Paint mSelectedCellPaint;
	private Paint mClueTextPaint;
	private Paint mSeparatorPaint;
	private Paint mCirclePaint;
	private Paint mSecondCluePaint;
	
	private int mFillPaintColor = Color.BLACK;
	private int mNumberPaintColor = Color.BLACK;
	private int mCharacterPaintColor = Color.BLACK;
	private int mWrongCharacterPaintColor = Color.RED;
	private int mSelectedRowPaintColor = Color.LTGRAY;
	private int mSelectedCellPaintColor = Color.parseColor("#add8e6");
	private int mClueTextPaintColor = Color.BLACK;
	private int mColorSeparatorPaintColor = Color.DKGRAY;
	private int mSecondCluePainColor = Color.LTGRAY;
	private int mCirclePaintColor = Color.BLACK;
	private int mBackgroundColor = Color.WHITE;
	private int mGridPaintColor = Color.BLACK;

	
	// spots for up/down touch events
	private float mDownY;
	private float mDownX;
	private float mLastMoveX;
	private float mLastMoveY;
	
	private int mSelectedX = 0;
	private int mSelectedY = 0;
	
	private CrosswordInfo mInfo;
	
	private Context mContext;

	private boolean mSpaceSwitchesDirections = false;
	private boolean mEnterSwitchesDirections = false;
	private boolean mShowClueLength = false;
	private boolean mAdvanceToNextWord = false;
	
	private boolean mShowKeyboard = false;
	
	private long mPuzzleStartTime = 0L;
	private boolean mFirstChar = false;
	
	private boolean mSkipExistingLetters;
	private boolean mShowWrongAnswers;
	private boolean mInkMode;
	private boolean mShowBothClues;
	private boolean mTimePuzzle;
	private boolean mGoogleSearch;
	
	private PuzzleCompletedListener mListener = null;
	
	private AndWords mAndWords;


	
	public void resetScrollOffsets()
	{
		mXOffset = 0.0f;
		mYOffset = 0.0f;
		
		invalidate();
	}
	
	public void checkAnswers()
	{
		int i, j;
		
		for (i = 0; i < mHeight; i++)
		{
			for (j = 0; j < mWidth; j++)
			{
				if (mLayout[i][j] != mInfo.mSolution[i][j])
					mLayout[i][j] = ' ';
			}
		}
		
		invalidate();
	}
	
	public interface PuzzleCompletedListener
	{
		public void puzzleCompleted();
	}
	
	public void setPuzzleListener(PuzzleCompletedListener listener)
	{
		mListener = listener;
	}
	
	public void fitToScreen()
	{
		if (mShowKeyboard)
		{
			InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(this, 0);
		}

		DisplayMetrics metrics = this.getResources().getDisplayMetrics();

		int width = metrics.widthPixels;
		int height = metrics.heightPixels;

		mCellHeight = (height - (2.0f * CELL_BORDER))/mHeight;
		mCellWidth = (width - (2.0f * CELL_BORDER))/mWidth;
		
		if (mCellHeight > mCellWidth)
			mCellHeight = mCellWidth;
		else if (mCellWidth > mCellHeight)
			mCellWidth = mCellHeight;
	}
	
	public void updatePrefs()
	{
		Context c = getContext();
		
		if (c == null)
			return;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		
		if (prefs == null)
			return;
		
		boolean bFitToScreen = prefs.getBoolean(mContext.getString(R.string.fit_puzzle_to_screen), false);
		mShowWrongAnswers = prefs.getBoolean(mContext.getString(R.string.show_wrong_answers), false);
		mSpaceSwitchesDirections = prefs.getBoolean(mContext.getString(R.string.space_changes_direction), false);
		mEnterSwitchesDirections = prefs.getBoolean(mContext.getString(R.string.enter_changes_direction), false);
		mShowClueLength = prefs.getBoolean(mContext.getString(R.string.show_clue_length), false);
		mSkipExistingLetters = prefs.getBoolean(mContext.getString(R.string.skip_existing_letters), false);
		mInkMode = prefs.getBoolean(mContext.getString(R.string.do_in_ink), false);
		mShowBothClues = prefs.getBoolean(mContext.getString(R.string.show_both_clues), false);
		mTimePuzzle = prefs.getBoolean(mContext.getString(R.string.time_puzzle), false);
		mGoogleSearch = prefs.getBoolean(mContext.getString(R.string.google_search), false);
		
		mRocker = prefs.getString(mContext.getString(R.string.volume_rocker), ROCKER_WORD);
		
		mCellWidth = prefs.getFloat(AndWords.PREF_CELL_WIDTH, 30.0f);
		mCellHeight = prefs.getFloat(AndWords.PREF_CELL_HEIGHT, 30.0f);

		if (bFitToScreen)
		{
			fitToScreen();
		}
		
		mNumberPaint.setTextSize(mCellHeight/3.0f);
		mCharacterPaint.setTextSize(mCellWidth * 0.8f);
		mWrongCharacterPaint.setTextSize(mCellWidth * 0.8f);

		mClueTextSize = Float.parseFloat(prefs.getString(mContext.getString(R.string.clue_font_size), String.valueOf(mClueTextSize)));
		
		mClueTextPaint.setTextSize(mClueTextSize);
		mSecondCluePaint.setTextSize(mClueTextSize);
		
		ONE_CLUE_OFFSET = mClueTextSize + 10.0f;
		TWO_CLUES_OFFSET = mClueTextSize * 2.0f + 10.0f;
		
		String s = prefs.getString(mContext.getString(R.string.end_of_word), "Stop");
		mAdvanceToNextWord = !s.equals("Stop");
		
		int nTheme = Integer.parseInt(prefs.getString(mContext.getString(R.string.screen_theme), "0"));
		
		if (nTheme == 0)
		{
			mFillPaintColor = Color.BLACK;
			mNumberPaintColor = Color.BLACK;
			mCharacterPaintColor = Color.BLACK;
			mWrongCharacterPaintColor = Color.RED;
			mSelectedRowPaintColor = Color.LTGRAY;
			mSelectedCellPaintColor = Color.parseColor("#add8e6");
			mClueTextPaintColor = Color.BLACK;
			mColorSeparatorPaintColor = Color.DKGRAY;
			mSecondCluePainColor = Color.LTGRAY;
			mCirclePaintColor = Color.BLACK;
			mBackgroundColor = Color.WHITE;
			mGridPaintColor = Color.BLACK;
		}
		else
		{
			mFillPaintColor = Color.GRAY;
			mNumberPaintColor = Color.WHITE;
			mCharacterPaintColor = Color.WHITE;
			mWrongCharacterPaintColor = Color.RED;
			mSelectedRowPaintColor = Color.LTGRAY;
			mSelectedCellPaintColor = Color.parseColor("#add8e6");
			mClueTextPaintColor = Color.WHITE;
			mColorSeparatorPaintColor = Color.DKGRAY;
			mSecondCluePainColor = Color.LTGRAY;
			mCirclePaintColor = Color.WHITE;
			mBackgroundColor = Color.BLACK;
			mGridPaintColor = Color.WHITE;
		}
		
		mGridPaint.setColor(mGridPaintColor );
		mFillPaint.setColor(mFillPaintColor);
		mNumberPaint.setColor(mNumberPaintColor);
		mCharacterPaint.setColor(mCharacterPaintColor);
		mWrongCharacterPaint.setColor(mWrongCharacterPaintColor);
		mSelectedRowPaint.setColor(mSelectedRowPaintColor);
		mSelectedCellPaint.setColor(mSelectedCellPaintColor);
		mClueTextPaint.setColor(mClueTextPaintColor);
		mSeparatorPaint.setColor(mColorSeparatorPaintColor);
		mSecondCluePaint.setColor(mSecondCluePainColor);
		mCirclePaint.setColor(mCirclePaintColor);
		setBackgroundColor(mBackgroundColor);
	}
	
	public boolean isModified()
	{
		if (mInfo == null)
			return false;
		
		if (mInfo.mPuzzleComplete)
			return false;
		
		int i, j;
		
		for (i = 0; i < mInfo.nHeight; i++)
		{
			for (j = 0; j < mInfo.nWidth; j++)
			{
				if (mLayout[i][j] != '.' && mLayout[i][j] != ' ')
					return true;
			}
		}
		
		return false;
	}
	
	public void setParent(AndWords parent)
	{
		mAndWords = parent;
	}

	public CrosswordInfo getInfo()
	{
		return mInfo;
	}
	
	public void setInfo(CrosswordInfo info)
	{
		mInfo = info;
		
		mSelectedY = 0;
		
		if (mShowKeyboard)
		{
			InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(this, 0);
		}
		
		// move over to the first char in the puzzle
		int h = 0;
		int w = 0;
		boolean bFound = false;
		
		if (info != null)
		{
			while (!bFound)
			{
				w = 0;
				
				while (w < info.nWidth && info.mDiagram[h][w] == '.')
					++w;
				
				if (w == info.nWidth)
				{
					++h;
					
					if (h > info.nHeight)
						break;
					
					w = 0;
				}
				else
				{
					bFound = true;
				}
			}
		}
		
		mSelectedX = w;
		mSelectedY = h;
		
		mXOffset = 0.0f;
		mYOffset = 0.0f;
		mLastMoveX = 0.0f;
		mLastMoveY = 0.0f;
		
		mDirection = Direction.ACROSS;
		
		
		mPuzzleStartTime = 0L;
		mFirstChar = false;

		if (info == null)
			return;
		
		mWidth = info.nWidth;
		mHeight = info.nHeight;
		mLayout = info.mDiagram;

		updatePrefs();
	}
	
	public void setTimer(long t)
	{
		mPuzzleStartTime = t;
		mFirstChar = true;
	}
	
	public void addTimeToTimer(long t)
	{
		mPuzzleStartTime += t;
	}
	
	public long getTimer()
	{
		return mPuzzleStartTime;
	}
	
	public int getPuzzleID()
	{
		return mInfo.mPuzzleID;
	}
	
	public char[][] getUserInput()
	{
		return mLayout;
	}
	
	public void showKeyboard(boolean bShow)
	{
		mShowKeyboard = bShow;
		
		InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(this, 0);
	}
	
	public AndGrid(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		mContext = context;
		init();
	}
	
	public AndGrid(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		
		mContext = context;
		init();
	}
	
	public AndGrid(Context context)
	{
		super(context);
		
		mContext = context;
		init();
	}

	private void init()
	{
		setFocusable(true);
		setFocusableInTouchMode(true);
		
		
		mGridPaint = new Paint();
		mGridPaint.setColor(mGridPaintColor);
		
		mFillPaint = new Paint();
		mFillPaint.setStyle(Paint.Style.FILL);
		mFillPaint.setColor(mFillPaintColor);
		
		mNumberPaint = new Paint();
		mNumberPaint.setColor(mNumberPaintColor);
		mNumberPaint.setTextSize(mClueNumberSize);
		mNumberPaint.setAntiAlias(true);
		
		mCharacterPaint = new Paint();
		mCharacterPaint.setColor(mCharacterPaintColor);
		mCharacterPaint.setTextSize(mGridCharSize);
		mCharacterPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
		mCharacterPaint.setAntiAlias(true);

		mWrongCharacterPaint = new Paint();
		mWrongCharacterPaint.setColor(mWrongCharacterPaintColor);
		mWrongCharacterPaint.setTextSize(mGridCharSize);
		mWrongCharacterPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
		mWrongCharacterPaint.setAntiAlias(true);
		
		mSelectedRowPaint = new Paint();
		mSelectedRowPaint.setColor(mSelectedRowPaintColor);
		
		// light blue
		mSelectedCellPaint = new Paint();
		mSelectedCellPaint.setColor(mSelectedCellPaintColor);
		
		mClueTextPaint = new Paint();
		mClueTextPaint.setColor(mClueTextPaintColor);
		mClueTextPaint.setTextSize(mClueTextSize);
		mClueTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
		mClueTextPaint.setAntiAlias(true);
		
		mSeparatorPaint = new Paint();
		mSeparatorPaint.setColor(mColorSeparatorPaintColor);
		
		mSecondCluePaint = new Paint();
		mSecondCluePaint.setColor(mSecondCluePainColor);
		mSecondCluePaint.setTextSize(mClueTextSize);
		mSecondCluePaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
		mSecondCluePaint.setAntiAlias(true);
		
		mCirclePaint = new Paint();
		mCirclePaint.setColor(mCirclePaintColor);
		mCirclePaint.setStyle(Style.STROKE);
		
		setBackgroundColor(mBackgroundColor);
		
		mInfo = null;
		
		setOnLongClickListener(this);
		
		requestFocus();
		
		updatePrefs();
	}

	private int numCharsInClue(Direction d)
	{
		int i = mSelectedY, j = mSelectedX;
		int count = 0;
		
		char[][] diagram = mInfo.mDiagram;
		
		if (d == Direction.ACROSS)
		{
			while (j > 0 && diagram[i][j - 1] != '.')
				--j;
			
			while (j < mWidth && diagram[i][j] != '.')
			{
				++count;
				++j;
			}
		}
		else
		{
			while (i > 0 && diagram[i - 1][j] != '.')
				--i;
			
			while (i < mHeight && diagram[i][j] != '.')
			{
				++count;
				++i;
			}
		}
		
		return count;
	}
	
	private int clueAcross(int height, int width)
	{
		if (mLayout == null)
			return -1;
		
		while (width >= 0 && mLayout[height][width] != '.')
		{
			if (mInfo.mCellNumbers[height][width] != 0 && (width == 0 || mLayout[height][width - 1] == '.'))
				return mInfo.mCellNumbers[height][width];
			
			width--;
		}

		return -1;
	}
	
	private int clueDown(int height, int width)
	{
		if (mLayout == null)
			return -1;
		
		while (height >= 0 && height < mHeight && mLayout[height][width] != '.')
		{
			if (mInfo.mCellNumbers[height][width] != 0 && (height == 0 || mLayout[height - 1][width] == '.'))
				return mInfo.mCellNumbers[height][width];
			
			height--;
		}

		return -1;
		
	}
	
	private boolean isRowSelected(int height, int width)
	{
		if (mDirection == Direction.ACROSS)
		{
			if (height != mSelectedY)
				return false;
			
			int selectedClue = clueAcross(mSelectedY, mSelectedX);
			
			if (selectedClue == clueAcross(height, width))
				return true;
			
		}
		else
		{
			if (width != mSelectedX)
				return false;
			
			int selectedRow = clueDown(mSelectedY, mSelectedX);
			
			if (selectedRow == clueDown(height, width))
				return true;
		}
		
		return false;
	}
	
	private String getAcrossClue()
	{
		int nClue = clueAcross(mSelectedY, mSelectedX);
		
		if (nClue == -1)
			return "";
		
		return mInfo.strAcrossClues[nClue];
	}
	
	private String getDownClue()
	{
		int nClue = clueDown(mSelectedY, mSelectedX);
		
		if (nClue == -1)
			return "";
		
		return mInfo.strDownClues[nClue];
	}
	
	private void invalidateClues()
	{
		invalidate(0, 0, getWidth(), (int)mClueOffset);
	}
	
	private void drawClue(Canvas mCanvas, boolean bDrawBoth)
	{
		String strAcrossClue = getAcrossClue();
		String strDownClue = getDownClue();
		
		if (!bDrawBoth)
			mClueOffset = ONE_CLUE_OFFSET;
		else
			mClueOffset = TWO_CLUES_OFFSET;
		
		if (mShowClueLength)
		{
			if (strAcrossClue != null && !strAcrossClue.equals(""))
				strAcrossClue = "(" + numCharsInClue(Direction.ACROSS) + ") " + strAcrossClue;
			
			if (strDownClue != null && !strDownClue.equals(""))
				strDownClue = "(" + numCharsInClue(Direction.DOWN) + ") " + strDownClue;
		}
		
		if (strAcrossClue != null && !strAcrossClue.equals(""))
			strAcrossClue = clueAcross(mSelectedY, mSelectedX) + "a. " + strAcrossClue;
		if (strDownClue != null && !strDownClue.equals(""))
			strDownClue = clueDown(mSelectedY, mSelectedX) + "d. " + strDownClue;
		
		if ((strAcrossClue != null && !strAcrossClue.equals(mStrLastAcrossClue)) || (strDownClue != null && !strDownClue.equals(mStrLastDownClue)))
			mClueXOffset = 0.0f;
			
		mStrLastAcrossClue = strAcrossClue;
		mStrLastDownClue = strDownClue;
		
		int downLen = 0;
		int acrossLen = 0;
		
		if (strDownClue != null)
			downLen = strDownClue.length();
		
		if (strAcrossClue != null)
			acrossLen = strAcrossClue.length();
		
		int length = Math.max(downLen, acrossLen);
		
		float widths[] = new float[length];
		
		if (downLen > acrossLen)
			mClueTextPaint.getTextWidths(strDownClue, widths);
		else
		{
			if (strAcrossClue != null)
				mClueTextPaint.getTextWidths(strAcrossClue, widths);
		}
		
		int i;
		float len = 0.0f;
		int startPos = 0;
		
		for (i = 0; i < length; i++)
		{
			len += widths[i];
			
			if (len <= mClueXOffset)
				startPos = i;
		}
		
		// fudge
		mClueWidth = len + ((float)length * 2.0f);
		
		mCanvas.drawLine(0.0f, 0.0f, getWidth(), 0.0f, mGridPaint);
		
		if (mDirection == Direction.ACROSS)
		{
			if (startPos < acrossLen && strAcrossClue != null)
				mCanvas.drawText(strAcrossClue, startPos, acrossLen, 0.0f, mClueTextSize + 2.0f, mClueTextPaint);
			
			if (startPos < downLen && bDrawBoth && strDownClue != null)
			{
				mCanvas.drawText(strDownClue, startPos, downLen, 0.0f, mClueOffset - 6, mSecondCluePaint);
			}
		}
		else
		{
			if (startPos < downLen && strDownClue != null)
				mCanvas.drawText(strDownClue, startPos, downLen, 0.0f, mClueTextSize, mClueTextPaint);
			if (startPos < acrossLen && bDrawBoth && strAcrossClue != null)
			{
				mCanvas.drawText(strAcrossClue, startPos, acrossLen, 0.0f, mClueOffset - 6, mSecondCluePaint);
			}
		}
		
		mCanvas.drawLine(0.0f, mClueOffset - 1, getWidth(), mClueOffset - 1, mSeparatorPaint);
	}
	
	private void makeSelectedCellVisible()
	{
		float xPos = (mSelectedX * (mCellWidth + 2.0f * CELL_BORDER));
		float yPos = (mSelectedY * (mCellHeight + 2.0f * CELL_BORDER));
		
		float height = getHeight() - mClueOffset;
		float width = getWidth();
		
		// Cell is off the screen to the left
		if (xPos - mXOffset < 0)
		{
			mXOffset = xPos;
		}
		else if (xPos - mXOffset > width)
		{
			mXOffset += (xPos - mXOffset - width + mCellWidth + 2.0f * CELL_BORDER);
		}
		
		if (yPos - mYOffset < 0)
		{
			mYOffset = yPos;
		}
		else if (yPos - mYOffset > height)
		{
			mYOffset += (yPos - mYOffset - height + mCellHeight + 2.0f * CELL_BORDER);
		}
		else
		{
			float yGap = height - (mHeight * (mCellHeight + 2.0f * CELL_BORDER) - mYOffset);
			
			if (yGap > 0.0f)
				mYOffset -= yGap;
			
			if (mYOffset < 0.0f)
				mYOffset = 0.0f;
		}
	}

	private int offsetFromX(int x)
	{
		return (int)(x * (mCellWidth + DOUBLE_CELL_BORDER - mXOffset));
	}
	
	private int offsetFromY(int y) {
		return (int) ((y * (mCellHeight + DOUBLE_CELL_BORDER) + mClueOffset + 1) - mYOffset) ;
	}
	
	private void invalidateCell(int x, int y)
	{
		int xoff = offsetFromX(x);
		int yoff = offsetFromY(y);
		
		invalidate(xoff, yoff, xoff + (int)(mCellWidth + DOUBLE_CELL_BORDER), yoff + (int)(mCellHeight + DOUBLE_CELL_BORDER));
	}
	
	private void invalidateWord(int x, int y)
	{
		if (mDirection == Direction.ACROSS)
			invalidateWordAcross(x, y);
		else
			invalidateWordDown(x, y);
		
	}
	
	private void invalidateWordAcross(int x, int y)
	{
		int initX = x;
		
		while (initX > 0 && mInfo.mDiagram[y][initX] != '.')
			--initX;
		
		int lastX = x;
		
		while (lastX < mWidth - 1 && mInfo.mDiagram[y][lastX] != '.')
			++lastX;
		
		invalidate(offsetFromX(initX), offsetFromY(y), (int)(offsetFromX(lastX) + mCellWidth + DOUBLE_CELL_BORDER), (int)(offsetFromY(y) + mCellHeight + DOUBLE_CELL_BORDER));
		invalidateClues();
	}
	
	private void invalidateWordDown(int x, int y)
	{
		int initY = y;
		
		while (initY > 0 && mInfo.mDiagram[initY][x] != '.')
			--initY;
		
		int lastY = y;
		
		while (lastY < mHeight - 1 && mInfo.mDiagram[lastY][x] != '.')
			++lastY;
		
		invalidate(offsetFromX(x), offsetFromY(initY), (int) (offsetFromX(x) + mCellWidth + DOUBLE_CELL_BORDER), (int)(offsetFromY(lastY) + mCellHeight + DOUBLE_CELL_BORDER));
		invalidateClues();
	}
	
	private boolean drawCell(Canvas canvas, int x, int y)
	{
		float border = 2.0f * CELL_BORDER;
		boolean bBadAnswer = false;
		
		float xoffset = x * mCellWidth + (x * border) - mXOffset;
		float yoffset = y * mCellHeight + (y * border) + mClueOffset - mYOffset;

		if (yoffset < mClueOffset || xoffset < 0)
			canvas.clipRect(0, mClueOffset, getWidth(), getHeight());

		if (mLayout[y][x] == '.')
		{
			canvas.drawRect(xoffset, yoffset, xoffset + mCellWidth + border, yoffset + mCellHeight + border, mFillPaint);
		}
		else
		{
			// left vertical line
			canvas.drawLine(xoffset, yoffset, xoffset, yoffset + mCellHeight, mGridPaint);
			
			// right vertical line
			canvas.drawLine(xoffset + mCellWidth + border, yoffset, xoffset + mCellWidth + border, yoffset + mCellHeight, mGridPaint);
			
			// top horizontal line
			canvas.drawLine(xoffset, yoffset, xoffset + mCellWidth, yoffset, mGridPaint);
			
			// bottom horizontal line
			canvas.drawLine(xoffset, yoffset + mCellHeight + border, xoffset + mCellWidth, yoffset + mCellHeight + border, mGridPaint);
			
			// highlight the selected row
			if (isRowSelected(y, x))
			{
				canvas.drawRect(xoffset + CELL_BORDER, yoffset + CELL_BORDER, xoffset + mCellWidth + CELL_BORDER, yoffset + mCellHeight + CELL_BORDER, mSelectedRowPaint);
			}
			
			// selected cell is highlighted
			if (mSelectedX == x && mSelectedY == y)
			{
				// if we aren't scrolling, make sure that the selected cell
				// is visible
				if (!mMoving)
				{
					if (xoffset + mCellWidth > getWidth())
					{
						mXOffset += (xoffset + mCellWidth - getWidth());
						invalidate();
						return false;
					}
					
					if (yoffset + mCellHeight > getHeight())
					{
						mYOffset += (yoffset + mCellHeight - getHeight());
						invalidate();
						return false;
					}
				}
				
				canvas.drawRect(xoffset + CELL_BORDER, yoffset + CELL_BORDER, xoffset + mCellWidth + CELL_BORDER, yoffset + mCellHeight + CELL_BORDER, mSelectedCellPaint);
			}
			
			
			// draw the cell numbers
			if (mInfo.mCellNumbers[y][x] != 0)
			{
				canvas.drawText(String.valueOf(mInfo.mCellNumbers[y][x]), xoffset + CELL_BORDER + 2, yoffset + CELL_BORDER + mCellHeight/3.0f + 2, mNumberPaint);
			}
			
			// put any user input in the cell
			if (mLayout[y][x] != 0)
			{
				String strText = String.valueOf(mLayout[y][x]);
				
				// only hilit wrong answers if the pref is set and we aren't in 'do it in ink' mode
				if (mLayout[y][x] != mInfo.mSolution[y][x] && mShowWrongAnswers && !mInkMode)
				{
					canvas.drawText(strText, xoffset + CELL_BORDER + ((mCellWidth - (int)mWrongCharacterPaint.measureText(strText, 0, 1))/2), yoffset  + mCellHeight - CELL_BORDER, mWrongCharacterPaint);
					bBadAnswer = true;
				}
				else
					canvas.drawText(strText, xoffset + CELL_BORDER + ((mCellWidth - (int)mCharacterPaint.measureText(strText, 0, 1))/2), yoffset + mCellHeight - CELL_BORDER, mCharacterPaint);	
			}
			else
			{
				// make sure this isn't a blank box cell
				if (mLayout[y][x] != '.')
					bBadAnswer = true;
			}
			
			// circled cell
			if (mInfo.mCircledSquares != null && mInfo.mCircledSquares[y][x] != 0)
			{
				canvas.drawCircle(xoffset + CELL_BORDER + (mCellWidth/2.0f) , yoffset + CELL_BORDER + (mCellHeight/2.0f), mCellWidth / 2.0f, mCirclePaint);
			}
		}	
		
		return bBadAnswer;
	}
	
	private int xFromOffset(float offset)
	{
		return (int)(offset / ( mCellWidth + DOUBLE_CELL_BORDER));
	}
	
	private int yFromOffset(float offset)
	{
		return (int)((offset - mClueOffset)/( mCellHeight + DOUBLE_CELL_BORDER));
	}
	
	@Override
	protected void onDraw(Canvas canvas) 
	{
		if (mInfo == null)
			return;
		
		int width = getWidth();
		int height = getHeight();
		boolean bBadAnswer = false;

		Rect clipRect = new Rect(0,0,0,0);
		
		boolean bClipped = canvas.getClipBounds(clipRect);
		
		if (bClipped)
		{
			int x = xFromOffset(clipRect.left);
			int y = yFromOffset(clipRect.top);

			drawClue(canvas, mShowBothClues);

			int xlen = (int)Math.ceil(((clipRect.width() + mXOffset) / (mCellWidth + DOUBLE_CELL_BORDER)));
			int ylen = (int)Math.ceil(((clipRect.height() + mYOffset)/ (mCellHeight + DOUBLE_CELL_BORDER)));
			
			if (x < 0) x = 0;
			if (y < 0) y = 0;
			if (xlen > mWidth) xlen = mWidth;
			if (ylen > mHeight) ylen = mHeight;

			bBadAnswer = false;
			
			for (int i = 0; i < xlen; i++)
			{
				for (int j = 0; j < ylen; j++)
				{
					if (drawCell(canvas, x + i, y + j))
						bBadAnswer = true;
				}
			}

			if (!bBadAnswer && !mInfo.mPuzzleComplete)
				checkIfPuzzleIsCopmlete();

			return;
		}
		
		int i = 0;
		int j = 0;
		
		float x, y;

		try
		{
			drawClue(canvas, mShowBothClues);
		}
		catch (Exception e)
		{
    		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    		builder.setTitle("Problem with puzzle");
    		builder.setMessage("Unable to parse this puzzle - please reload");
    		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) 
				{
					Intent i = new Intent(LoadPuzzle2.LOAD_PUZZLE);
					mAndWords.startActivityForResult(i, AndWords.PUZZLE_LOADED);
				}
			});
    		
    		AlertDialog d = builder.create();
    		d.show();
    		
    		return;
		}
		

		y = mClueOffset;
	
		if (!mMoving)
			makeSelectedCellVisible();
		
		int startingVertCell = (int) (mYOffset/(mCellHeight + (2.0f * CELL_BORDER)));
		
		int lastVertCell;
		
		if ((mHeight * mCellHeight + (2.0f * CELL_BORDER)) > height)
		{
			lastVertCell = (int) ((mYOffset + height - mClueOffset )/(mCellHeight + (2.0f * CELL_BORDER)));
			if (lastVertCell < mHeight)
				++lastVertCell;
			
			if (lastVertCell > mHeight)
			{
				Log.v("ERROR!", "lastVertCell > mHeight");
				lastVertCell = mHeight;
			}
		}
		else
			lastVertCell = mHeight;

		float yDiff = mYOffset - ((float)startingVertCell * (mCellHeight + (2.0f * CELL_BORDER)));
		float yCellHeight = mCellHeight - yDiff;
		float yTopBorder = CELL_BORDER;
		float yBottomBorder = CELL_BORDER;
		
		if (yDiff != 0.0f)
			yTopBorder = 0.0f;
		
		for (i = startingVertCell; i < lastVertCell; i++)
		{
			x = 0.0f;
			
			// figure out which cell we need to start drawing due to scrolling
			int startingHorCell = (int) (mXOffset/(mCellWidth + (2.0f * CELL_BORDER)));
			int lastHorCell;
			
			if (mWidth * (mCellWidth + 2.0f * CELL_BORDER) > width)
			{
				lastHorCell = (int) ((mXOffset + width)/(mCellWidth + (2.0f * CELL_BORDER)));
				if (lastHorCell < mWidth)
					++lastHorCell;
				
				if (lastHorCell > mWidth)
				{
					Log.v("ERROR!", "lastHorCell > mWidth");
					lastHorCell = mWidth;
				}
			}
			else
				lastHorCell = mWidth;
			
			
			float xDiff = mXOffset - ((float)startingHorCell * (mCellWidth + (2.0f * CELL_BORDER)));
			float xCellWidth = mCellWidth - xDiff;
			float xLeftBorder = CELL_BORDER;
			float xRightBorder = CELL_BORDER;
			
			if (xDiff != 0.0f)
				xLeftBorder = 0.0f;

			for (j = startingHorCell; j < lastHorCell; j++)
			{
				if (drawCell(canvas, j, i))
					bBadAnswer = true;
				x += xCellWidth + xLeftBorder + xRightBorder;
				xLeftBorder = CELL_BORDER;
				xDiff = 0.0f;
				xCellWidth = mCellWidth;
			}
			// top of the cells for this row
			y += yCellHeight + yTopBorder + yBottomBorder;
			yDiff = 0.0f;
			yTopBorder = CELL_BORDER;
			yCellHeight = mCellHeight;
		}	
		
		// nothing drawn was incorrect - check to see if the puzzle is complete
		if (!bBadAnswer && !mInfo.mPuzzleComplete)
		{
			checkIfPuzzleIsCopmlete();
		}
	}

	private void checkIfPuzzleIsCopmlete()
	{
		for (int i = 0; i < mHeight; i++)
		{
			for (int j = 0; j < mWidth; j++)
			{
				if (mLayout[i][j] != mInfo.mSolution[i][j])
					return;
			}
		}

		// puzzle is complete!
		// alert the user once
		if (!mInfo.mPuzzleComplete)
		{
			AlertDialog.Builder ad = new AlertDialog.Builder(mContext);
			ad.setTitle(mContext.getString(R.string.puzzle_finished_title));

			String strMessage = mContext.getString(R.string.puzzle_finished_message);

			if (mTimePuzzle)
			{
				long len = System.currentTimeMillis() - mPuzzleStartTime;

				int seconds = (int) ((len / 1000) % 60);
				int minutes = (int) ((len / 1000) / 60);

				strMessage += " " + mContext.getString(R.string.puzzle_finished_in) + " " + minutes + " " + mContext.getString(R.string.puzzle_finished_min) + ", " + seconds + " " + mContext.getString(R.string.puzzle_finished_sec);
			}
			else
				strMessage += "!";

			ad.setMessage(strMessage);
			ad.setPositiveButton(mContext.getString(R.string.puzzle_finished_ok), null );
			ad.show();

			if (mListener != null)
				mListener.puzzleCompleted();
		}
		mInfo.mPuzzleComplete = true;
	}

	private void setSelectedCell(float nX, float nY)
	{
		int cellX = (int) (nX / (mCellWidth + DOUBLE_CELL_BORDER));
		int cellY = (int) (nY / (mCellHeight + DOUBLE_CELL_BORDER));
		
		if (cellX >= mWidth)
			cellX = mWidth - 1;
		if (cellY >= mHeight)
			cellY = mHeight - 1;
		
		if (cellX > mWidth || cellY > mHeight || cellX < 0 || cellY < 0)
			return;
		
		// click on blank square
		if (mLayout[cellY][cellX] == '.')
			return;
		
		// same cell selected, change direction
		if (mSelectedX == cellX && mSelectedY == cellY)
		{
			invalidateWord(mSelectedX, mSelectedY);

			if (mDirection == Direction.ACROSS)
			{
				if (cellY == 0)
				{
					if (mLayout[cellY + 1][cellX] != '.')
						mDirection = Direction.DOWN;
				}
				else if (cellY == mHeight)
				{
					if (mLayout[cellY - 1][cellX] != '.')
						mDirection = Direction.DOWN;
				}
				else if (mLayout[cellY - 1][cellX] != '.' || mLayout[cellY + 1][cellX] != '.')
					mDirection = Direction.DOWN;
			}
			else
			{
				if (cellX == 0)
				{
					if (mLayout[cellY][cellX + 1] != '.')
						mDirection = Direction.ACROSS;
				}
				else if (cellX == mWidth)
				{
					if (mLayout[cellY][cellX - 1] != '.')
						mDirection = Direction.ACROSS;
				}
				else if (mLayout[cellY][cellX - 1] != '.' || mLayout[cellY ][cellX + 1] != '.')
					mDirection = Direction.ACROSS;
			}
			
			invalidateWord(mSelectedX, mSelectedY);
			
			return;
		}
		else
		{
			// we are going down, check to see if there's a down word here.
			// if not, change direction.
			if (mDirection == Direction.DOWN)
			{
				if (cellY == 0)
				{
					if (mLayout[cellY + 1][cellX] == '.')
						mDirection = Direction.ACROSS;
				}
				else if (cellY == mHeight -1)
				{
					if (mLayout[cellY - 1][cellX] == '.')
						mDirection = Direction.ACROSS;
				}
				else
				{
					if (mLayout[cellY - 1][cellX] == '.' && mLayout[cellY + 1][cellX] == '.')
						mDirection = Direction.ACROSS;
				}
			}
			else
			{
				if (cellX == 0)
				{
					if (mLayout[cellY][cellX + 1] == '.')
						mDirection = Direction.DOWN;
				}
				else if (cellX == mWidth - 1)
				{
					if (mLayout[cellY][cellX - 1] == '.')
						mDirection = Direction.DOWN;
				}
				else
				{
					if (mLayout[cellY][cellX - 1] == '.' && mLayout[cellY][cellX + 1] == '.')
						mDirection = Direction.DOWN;
				}
			}
				
		}
		
		int oldX = mSelectedX;
		int oldY = mSelectedY;
		
		mSelectedX = cellX;
		mSelectedY = cellY;
		
		if (inSameWord(oldX, oldY, mSelectedX, mSelectedY))
		{
			invalidateCell(oldX, oldY);
			invalidateCell(mSelectedX, mSelectedY);
			invalidateClues();
		}
		else
		{
			invalidateWord(oldX, oldY);
			invalidateWord(mSelectedX, mSelectedY);
			invalidateClues();
		}
	}
	
	private boolean inSameWord(int orgX, int orgY, int newX, int newY)
	{
		return ((orgX == newX && mDirection == Direction.DOWN) || (orgY == newY && mDirection == Direction.ACROSS));
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		
		if (mShowKeyboard)
		{
			InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(this, 0);
		}
        
		int action = event.getAction();
		
		switch (action)
		{
		case MotionEvent.ACTION_MOVE:
			// only 1 pointer down, scroll the grid/clue
			if (event.getPointerCount() == 1)
			{
				float x = event.getX();
				float y = event.getY();
				
				mMoving = true;
//				Log.v("MOVE", "LastX: " + mLastMoveX + ", LastY: " + mLastMoveY);
				
				// scroll the clue if we are in the top
				if (mDownY < mClueOffset)
				{
					float xDiff = mLastMoveX - x;
					float width = getWidth();
					
					if (mClueWidth > width && Math.abs(xDiff) > 10)
					{
						mClueXOffset += xDiff;
						
						if (mClueXOffset > (mClueWidth - width))
							mClueXOffset = mClueWidth - width;
						
						if (mClueXOffset < 0.0f)
							mClueXOffset = 0.0f;
					}
					else
					{
						mMoving = false;
					}
					
					mLastMoveX = x;
					mLastMoveY = y;
					
					invalidate();
					return true;
				}
				
				// scroll the grid
				if (x != mDownX || y != mDownY)
				{
					float xDiff = mLastMoveX - x;
					float yDiff = mLastMoveY - y;
					
					float width = getWidth();
					float height = getHeight() - mClueOffset;
					
					float puzWidth = mWidth * (mCellWidth + DOUBLE_CELL_BORDER);
					float puzHeight = mHeight * (mCellHeight + DOUBLE_CELL_BORDER);
					
					if (puzWidth > width)
					{
						mXOffset += xDiff;
						
						if (mXOffset > (puzWidth - width))
							mXOffset = puzWidth - width;
						
						if (mXOffset < 0.0f)
							mXOffset = 0.0f;
					}
					
					
					if (puzHeight > height)
					{
						mYOffset += yDiff;
						
						if (mYOffset > (puzHeight - height))
							mYOffset = puzHeight - height;
						
						if (mYOffset < 0.0f)
							mYOffset = 0.0f;
					}
					
					mLastMoveX = x;
					mLastMoveY = y;
				}
			}
			else
			{
				// otherwise, we are pinch zooming
				float newSpacing = spacing(event);
				
				if (Math.abs(newSpacing - mLastZoomSpacing) < 20.0f)
					return true;
				
				if (mLastZoomSpacing == 0.0f)
				{
					mLastZoomSpacing = newSpacing;
					return true;
				}
				
				float scale = (newSpacing / mLastZoomSpacing);
				
				if (mCellWidth * scale < 20 || mCellWidth * scale > 150)
					break;
				
				mCellWidth *= scale;
				mCellHeight *= scale;
				
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
				
				SharedPreferences.Editor e = prefs.edit();
				e.putFloat(AndWords.PREF_CELL_WIDTH, mCellWidth);
				e.putFloat(AndWords.PREF_CELL_HEIGHT, mCellHeight);
				e.commit();
				
				// font sizes for cell numbers/text
				mNumberPaint.setTextSize(mCellHeight/3.0f);
				mCharacterPaint.setTextSize(mCellWidth * 0.8f);
				mWrongCharacterPaint.setTextSize(mCellWidth* 0.8f);

				mXOffset *= scale;
				mYOffset *= scale;
				
				mLastZoomSpacing = newSpacing;
			}

			invalidate();
			break;
			
		case MotionEvent.ACTION_UP:
			float upX = event.getX();
			float upY = event.getY();
			
			float gridHeight = (mHeight * (mCellHeight + 2.0f * CELL_BORDER)) + mClueOffset;
			float gridWidth = (mWidth * (mCellWidth + 2.0f * CELL_BORDER));
			
			// TODO - use the cell contained at the X,Y coords rather than
			// the coords themselves to improve granularity when clickin the
			// same cell to change directions
			if (upY > mClueOffset && upY < gridHeight && upX < gridWidth && cellsAreEqual(upY, upX, mDownY, mDownX))
				setSelectedCell(upX + mXOffset, upY - mClueOffset + mYOffset);
			
			mMoving = false;
			break;
			
		case MotionEvent.ACTION_DOWN:
			mDownX = event.getX();
			mDownY = event.getY();

			mLastMoveY = mDownY;
			mLastMoveX = mDownX;
			
			mLastZoomSpacing = 0.0f;
			
			break;
		}
		
		return true;
	}

	private boolean cellsAreEqual(float y1, float x1, float y2, float x2)
	{
		int ycoord1 = (int) ((y1 + mYOffset - mClueOffset)/(mCellHeight + 2.0f* CELL_BORDER));
		int ycoord2 = (int) ((y2 + mYOffset - mClueOffset)/(mCellHeight + 2.0f* CELL_BORDER));
		int xcoord1 = (int) ((y1 + mXOffset - mClueOffset)/(mCellWidth + 2.0f* CELL_BORDER));
		int xcoord2 = (int) ((y2 + mXOffset - mClueOffset)/(mCellWidth + 2.0f* CELL_BORDER));
		
		return (ycoord1 == ycoord2 && xcoord1 == xcoord2);
	}
	
	private float spacing(MotionEvent e)
	{
		float x = e.getX(0) - e.getX(1);
		float y = e.getY(0) - e.getY(1);
		
		return FloatMath.sqrt(x * x + y * y);
	}
	
	private void moveToPreviousClueAcross()
	{
		// move til we hit a non-clue square
		while (mSelectedX > 0 && mLayout[mSelectedY][mSelectedX] != '.')
			--mSelectedX;
		
		// now we are either at the beginning of the row, or on a blank cell
		// skip past any contiguous blank cells
		if (mSelectedX > 0)
		{
			while (mSelectedX > 0 && mLayout[mSelectedY][mSelectedX] == '.')
				--mSelectedX;
		}
		
		// hit the beginning of the line - bump up - or reset to the end and switch directions
		if (mSelectedX == 0)
		{
			if (mSelectedY == 0)
			{
				mSelectedY = mHeight - 1;
				mSelectedX = mWidth - 1;
				
				mDirection = Direction.DOWN;
				
			}
			else
			{
				mSelectedY--;
				mSelectedX = mWidth - 1;
			}
			
			// make sure we're on a clue in the row
			while (mSelectedX > 0 && mLayout[mSelectedY][mSelectedX] == '.')
				--mSelectedX;
			
			if (mSkipExistingLetters)
				skipToNextOpenLetter();
			
			return;
		}
		
		if (mSkipExistingLetters)
			skipToNextOpenLetter();
	
	}
	
	private void moveToNextClueAcross()
	{
		invalidateWord(mSelectedX, mSelectedY);
		
		// move til we hit a non-clue square
		while (mSelectedX < mWidth && mLayout[mSelectedY][mSelectedX] != '.')
			++mSelectedX;
		
		// now we are either at the end of the row, or on a blank cell
		// skip past any contiguous blank cells
		if (mSelectedX < mWidth)
		{
			while (mSelectedX < mWidth && mLayout[mSelectedY][mSelectedX] == '.')
				++mSelectedX;
		}
		
		// hit the end of the line - bump down - or reset to the beginning and switch directions
		if (mSelectedX == mWidth)
		{
			if (mSelectedY == mHeight - 1)
			{
				mSelectedY = 0;
				mSelectedX = 0;
				
				mDirection = Direction.DOWN;
				
				invalidateWord(mSelectedX, mSelectedY);
				
				return;
			}
			else
			{
				mSelectedY++;
				mSelectedX = 0;
			}
			
			// make sure we're on a clue in the row
			while (mSelectedX < mWidth && mLayout[mSelectedY][mSelectedX] == '.')
				++mSelectedX;
			
			if (mSkipExistingLetters)
				skipToNextOpenLetter();
			
			invalidateWord(mSelectedX, mSelectedY);
			return;
		}
		
		if (mSkipExistingLetters)
			skipToNextOpenLetter();
		
		invalidateWord(mSelectedX, mSelectedY);
	}
	
	private void moveToNextClueDown()
	{
		int lastCellNumber = mInfo.mLastCellNumber;
	
		invalidateWord(mSelectedX, mSelectedY);

		// move up til we hit a non-clue square
		while (mSelectedY > 0 && mLayout[mSelectedY - 1][mSelectedX] != '.')
			--mSelectedY;
		
		// Now we are at the top of the current clue - get the number
		int clueNum = mInfo.mCellNumbers[mSelectedY][mSelectedX];
		
		clueNum++;
		
		while (clueNum < lastCellNumber && mInfo.strDownClues[clueNum] == null)
		{
			++clueNum;
		}
		
		
		// skip looking for the next down clue if we don't actually have a clue for this number
		if (clueNum < lastCellNumber)
		{
			int y = mSelectedY;
			int x;
			
			for (; y < mHeight; y++)
			{
				for (x = 0; x < mWidth; x++)
				{
					if (mInfo.mCellNumbers[y][x] == clueNum)
					{
						mSelectedX = x;
						mSelectedY = y;
						
						if (mSkipExistingLetters)
							skipToNextOpenLetter();
						
						invalidateWord(mSelectedX, mSelectedY);
						
						return;
					}
				}
			}
		}
		
		// no clue - swap directions and move to the first clue
		mDirection = Direction.ACROSS;
		
		mSelectedX = 0;
		mSelectedY = 0;
		
		while (mSelectedX < mWidth && mLayout[mSelectedY][mSelectedX] == '.')
			++mSelectedX;
		
		if (mSkipExistingLetters)
			skipToNextOpenLetter();
		
		invalidateWord(mSelectedX, mSelectedY);
	}
	
	public void moveToPreviousClueDown()
	{
		invalidateWord(mSelectedX, mSelectedY);
		
		// move til we hit a non-clue square
		while (mSelectedY > 0 && mLayout[mSelectedY - 1][mSelectedX] != '.')
			--mSelectedY;
		
		// Now we are at the top of the current clue - get the number
		int clueNum = mInfo.mCellNumbers[mSelectedY][mSelectedX];
		
		clueNum--;
		
		while (clueNum >= 0 && mInfo.strDownClues[clueNum] == null)
		{
			--clueNum;
		}
		
		int y = mSelectedY;
		int x;
		
		for (; y >= 0; y--)
		{
			for (x = 0; x < mWidth; x++)
			{
				if (mInfo.mCellNumbers[y][x] == clueNum)
				{
					mSelectedX = x;
					mSelectedY = y;
					
					if (mSkipExistingLetters)
						skipToNextOpenLetter();
					
					invalidateWord(mSelectedX, mSelectedY);
					return;
				}
			}
		}
		
		mDirection = Direction.ACROSS;
		
		mSelectedX = mWidth - 1;
		mSelectedY = mHeight - 1;
		
		while (mSelectedX > 0 && mLayout[mSelectedY][mSelectedX] == '.')
			--mSelectedX;
		
		if (mSkipExistingLetters)
			skipToNextOpenLetter();

		invalidateWord(mSelectedX, mSelectedY);
	}
	
	// move a space to the left
	private void moveLeft()
	{
		invalidateCell(mSelectedX, mSelectedY);
		
		if (mSelectedX > 0)
		{
			if (mLayout[mSelectedY][mSelectedX - 1] != '.')
			{
				// if the shift is down, move to the beginning of the current clue
				if (mShiftDown)
				{
					int x = mSelectedX;
					
					while (x > 0 && mLayout[mSelectedY][x - 1] != '.')
						--x;
					
					if (x >= 0)
						mSelectedX = x;
				}
				else
					--mSelectedX;
			}
			else
			{
				int tempX = mSelectedX;
				
				while (tempX > 0 && mLayout[mSelectedY][tempX - 1] == '.')
				{
					--tempX;
				}
				
				if (tempX > 0)
					mSelectedX = tempX - 1;
			}
		}
		
		invalidateCell(mSelectedX, mSelectedY);
		invalidateClues();
	}
	
	// move a space to the right
	private void moveRight()
	{
		invalidateCell(mSelectedX, mSelectedY);
		
		if (mSelectedX < mWidth - 1)
		{
			if (mLayout[mSelectedY][mSelectedX + 1] != '.')
			{
				// move to the end of the word if the shift key is down
				if (mShiftDown)
				{
					int x = mSelectedX;
					
					while (x < (mWidth - 1) && mLayout[mSelectedY][x + 1] != '.')
						++x;
					
					if (x < mWidth)
						mSelectedX = x;
				}
				else
					++mSelectedX;
			}
			else
			{
				int tempX = mSelectedX;
				
				while (tempX < (mWidth - 1) && mLayout[mSelectedY][tempX + 1] == '.')
				{
					++tempX;
				}
				
				if (tempX < mWidth - 1)
					mSelectedX = tempX + 1;
			}
		}
		invalidateCell(mSelectedX, mSelectedY);
		invalidateClues();
	}
	
	// move up one space
	private void moveUp()
	{
		invalidateCell(mSelectedX, mSelectedY);

		if (mSelectedY > 0)
		{
			if (mLayout[mSelectedY - 1][mSelectedX] != '.')
			{
				// move to the top of the word if the shift key is down
				if (mShiftDown)
				{
					int y = mSelectedY;
					
					while (y > 0 && mLayout[y - 1][mSelectedX] != '.')
						--y;
					
					if (y >= 0)
						mSelectedY = y;
				}
				else
					--mSelectedY;
			}
			else
			{
				int tempY = mSelectedY;
				
				while (tempY > 0 && mLayout[tempY - 1][mSelectedX] == '.')
				{
					--tempY;
				}
				
				if (tempY > 0)
					mSelectedY = tempY - 1;
			}
		}

		invalidateCell(mSelectedX, mSelectedY);
		invalidateClues();
	}
	
	// move down one space
	private void moveDown()
	{
		invalidateCell(mSelectedX, mSelectedY);

		if (mSelectedY < mHeight - 1)
		{
			if (mLayout[mSelectedY + 1][mSelectedX] != '.')
			{
				if (mShiftDown)
				{
					int y = mSelectedY;
					
					while (y < (mHeight - 1) && mLayout[y + 1][mSelectedX] != '.')
						++y;
					
					if (y < mHeight - 1)
						mSelectedY = y;
					else
						mSelectedY = mHeight - 1;
				}
				else
					++mSelectedY;
			}
			else
			{
				int tempY = mSelectedY;
				
				while (tempY < (mHeight - 1) && mLayout[tempY + 1][mSelectedX] == '.')
				{
					++tempY;
				}
				
				if (tempY < mHeight - 1)
					mSelectedY = tempY + 1;
			}
		}

		invalidateCell(mSelectedX, mSelectedY);
		invalidateClues();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{
		// start timing on the first character input
		if (mFirstChar == false)
		{
			mFirstChar = true;
			mPuzzleStartTime = System.currentTimeMillis();
		}
		
		switch (keyCode)
		{
		case KeyEvent.KEYCODE_UNKNOWN:
			String s = event.getCharacters();
			
			if (s.length() != 0)
			{
				for (int i = 0; i < s.length(); i++)
					addChar(s.charAt(i));
				return true;
			}
			
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			
			if (mRocker.equals(ROCKER_WORD))
			{
				if (mDirection == Direction.ACROSS)
					moveToNextClueAcross();
				else
					moveToNextClueDown();
			}
			else
			{
				if (mDirection == Direction.ACROSS)
					moveRight();
				else
					moveDown();
			}
			
			return true;
			
		case KeyEvent.KEYCODE_VOLUME_UP:
			
			if (mRocker.equals(ROCKER_WORD))
			{
				if (mDirection == Direction.ACROSS)
					moveToPreviousClueAcross();
				else
					moveToPreviousClueDown();
			}
			else
			{
				if (mDirection == Direction.ACROSS)
					moveLeft();
				else
					moveUp();
			}
			return true;
			
		case KeyEvent.KEYCODE_PERIOD:
			if (mDirection == Direction.ACROSS)
				moveRight();
			else
				moveDown();
			
			return true;
			
		case KeyEvent.KEYCODE_COMMA:
			if (mDirection == Direction.ACROSS)
				moveLeft();
			else
				moveUp();
			
			return true;
			
		case KeyEvent.KEYCODE_ENTER:
			
			boolean bShift = mShiftDown;
			
			if (event.isShiftPressed())
				bShift = true;
			
			if (mEnterSwitchesDirections)
			{
				if (mDirection == Direction.ACROSS)
				{
					mDirection = Direction.DOWN;
					invalidateWordAcross(mSelectedX, mSelectedY);
					invalidateWordDown(mSelectedX, mSelectedY);
				}
				else
				{
					mDirection = Direction.ACROSS;
					invalidateWordDown(mSelectedX, mSelectedY);
					invalidateWordAcross(mSelectedX, mSelectedY);
				}
			}
			else
			{
				int curX = mSelectedX;
				int curY = mSelectedY;
				
				if (mDirection == Direction.ACROSS)
				{
					if (bShift)
						moveToPreviousClueAcross();
					else
						moveToNextClueAcross();
					
					invalidateWordAcross(curX, curY);
					invalidateWordAcross(mSelectedX, mSelectedY);
				}
				else
				{
					if (bShift)
						moveToPreviousClueDown();
					else
						moveToNextClueDown();
					
					invalidateWordDown(curX, curY);
					invalidateWordDown(mSelectedX, mSelectedY);
				}
			}
			
			return true;
			
			// catch the shift key
		case KeyEvent.KEYCODE_SHIFT_LEFT:
		case KeyEvent.KEYCODE_SHIFT_RIGHT:
			mShiftDown = true;
			return true;
			
		case KeyEvent.KEYCODE_SPACE:
			if (mSpaceSwitchesDirections == false)
			{
				addChar(' ');
				return true;
			}
			
			// fall through
		case KeyEvent.KEYCODE_CAMERA:
		case KeyEvent.KEYCODE_DPAD_CENTER:
			invalidateWord(mSelectedX, mSelectedY);
			
			if (mDirection == Direction.ACROSS)
				mDirection = Direction.DOWN;
			else
				mDirection = Direction.ACROSS; 
			
			invalidateWord(mSelectedX, mSelectedY);
			return true;
			
		case KeyEvent.KEYCODE_DPAD_LEFT:
			moveLeft();

			return true;
			
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			moveRight();
			return true;
			
		case KeyEvent.KEYCODE_DPAD_UP:
			moveUp();
			return true;
			
			
		case KeyEvent.KEYCODE_DPAD_DOWN:
			moveDown();
			return true;
			
		case KeyEvent.KEYCODE_DEL:
			// only allow the user to delete a char if we aren't in 'do in ink' mode
			if (mInkMode == false)
			{
				mLayout[mSelectedY][mSelectedX] = ' ';
				
				invalidateCell(mSelectedX, mSelectedY);

				if (mDirection == Direction.ACROSS)
				{
					if (mSelectedX != 0 && mLayout[mSelectedY][mSelectedX - 1] != '.')
					{
						--mSelectedX;
					}
				}
				else
				{
					if (mSelectedY != 0 && mLayout[mSelectedY - 1][mSelectedX] != '.')
						--mSelectedY;
				}

				invalidateCell(mSelectedX, mSelectedY);
				invalidateClues();
			}
			return true;
			
		default:
			if ((keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z))
			{
				addChar((char)(keyCode + 36));
				return true;
			}
			else if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9)
			{
				addChar((char)(keyCode + 41));
				return true;
			}
		}
		
		return false;
	}

	private void addChar(char c)
	{
		if (mInkMode == false || mLayout[mSelectedY][mSelectedX] == ' ')
			mLayout[mSelectedY][mSelectedX] = c;
		
		if (mDirection == Direction.ACROSS)
		{
			if (mAdvanceToNextWord && (mSelectedX == mWidth - 1 || mLayout[mSelectedY][mSelectedX + 1] == '.'))
			{
				moveToNextClueAcross();
				return;
			}
			
			invalidateCell(mSelectedX, mSelectedY);
			
			if (mSelectedX != mWidth - 1 && mLayout[mSelectedY][mSelectedX + 1] != '.')
			{
				++mSelectedX;

				invalidateCell(mSelectedX, mSelectedY);
				invalidateClues();
				
				if (mSkipExistingLetters)
				{
					skipToNextOpenLetter();
				}
			}
		}
		else
		{
			if (mAdvanceToNextWord && (mSelectedY == mHeight - 1 || mLayout[mSelectedY + 1][mSelectedX] == '.'))
			{
				moveToNextClueDown();
				return;
			}
			
			invalidateCell(mSelectedX, mSelectedY);

			if (mSelectedY != mHeight - 1 && mLayout[mSelectedY + 1][mSelectedX] != '.')
			{
				++mSelectedY;
				invalidateCell(mSelectedX, mSelectedY);
				invalidateClues();
			
				if (mSkipExistingLetters)
				{
					skipToNextOpenLetter();
				}
			}
		}

	}
	
	private void skipToNextOpenLetter()
	{
		if (mDirection == Direction.ACROSS)
		{
			int tmpX = mSelectedX;
			
			while (mLayout[mSelectedY][tmpX] != ' ' && tmpX < mWidth - 1)
			{
				if (mLayout[mSelectedY][tmpX + 1] == '.')
					return;
				
				++tmpX;
			}
			
			if (tmpX != mWidth)
				mSelectedX = tmpX;
		}
		else
		{
			int tmpY = mSelectedY;
			
			while (mLayout[tmpY][mSelectedX] != ' ' && tmpY < mHeight - 1)
			{
				if (mLayout[tmpY + 1][mSelectedX] == '.')
					return;
				
				++tmpY;
			}
			
			if (tmpY != mHeight)
				mSelectedY = tmpY;
		}
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) 
	{
		if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)
		{
			mShiftDown = false;
			return true;
		}
		
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
			return true;
		
	    if (keyCode == KeyEvent.KEYCODE_UNKNOWN)
	    {
			String s = event.getCharacters();
			
			if (s.length() != 0)
			{
				for (int i = 0; i < s.length(); i++)
					addChar(s.charAt(i));
				return true;
			}
	    }
	    
		return super.onKeyUp(keyCode, event);
	}

	public void revealLetter() 
	{
		mLayout[mSelectedY][mSelectedX] = mInfo.mSolution[mSelectedY][mSelectedX];
		invalidateCell(mSelectedX, mSelectedY);
	}

	public void revealWord() 
	{
		int i = mSelectedY, j = mSelectedX;
		
		char[][] diagram = mInfo.mDiagram;
		char[][] solution = mInfo.mSolution;
		
		if (mDirection == Direction.ACROSS)
		{
			while (j > 0 && diagram[i][j - 1] != '.')
				--j;
			
			while (j < mWidth && diagram[i][j] != '.')
			{
				mLayout[i][j] = solution[i][j];
				++j;
			}
		}
		else
		{
			while (i > 0 && diagram[i - 1][j] != '.')
				--i;
			
			while (i < mHeight && diagram[i][j] != '.')
			{
				mLayout[i][j] = solution[i][j];
				++i;
			}
		}
		
		invalidateWord(mSelectedX, mSelectedY);
	}

	public void revealSolution() 
	{
		int i, j;
		char[][] solution = mInfo.mSolution;
		
		for (i = 0; i < mHeight; i++)
		{
			for (j = 0; j < mWidth; j++)
			{
				mLayout[i][j] = solution[i][j];
			}
		}
		invalidate();
	}

	@Override
	public boolean onCheckIsTextEditor() 
	{
		return true;
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) 
	{
		outAttrs.imeOptions |= EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_NONE;
		outAttrs.actionLabel = null;
		outAttrs.hintText = "Insert the test text";
		outAttrs.initialCapsMode = 0;
		outAttrs.initialSelEnd = outAttrs.initialSelStart = -1;
		outAttrs.label = "Test text";
		return new BaseInputConnection(this, false);
	}

	public boolean onLongClick(View view) 
	{
		if (!view.equals(this))
			return false;
		
		if (mInfo == null)
			return false;
		
		// if we are scrolling - don't do anything
		if (mMoving == true)
			return false;
		
		// google prefence isn't set - no need to do anything
		if (!mGoogleSearch)
			return false;
		
		// long click in the clue - figure out which clue and google it
		if (mDownY < mClueOffset)
		{
			String strAcrossClue = getAcrossClue();
			String strDownClue = getDownClue();
			
			
			String strClue = "";
			
			if (mShowBothClues == false)
			{
				if (mDirection == Direction.ACROSS)
					strClue = strAcrossClue;
				else
					strClue = strDownClue;
			}
			else
			{
				// long click on the bottom clue which is the opposite clue of the direction
				if (mDownY > ONE_CLUE_OFFSET)
				{
					if (mDirection == Direction.ACROSS)
						strClue = strDownClue;
					else
						strClue = strAcrossClue;
				}
				else
				{
					if (mDirection == Direction.ACROSS)
						strClue = strAcrossClue;
					else
						strClue = strDownClue;
				}
			}
			
			// fire off a google search
    		Uri u = Uri.parse("http://www.google.com/search?q=" + strClue);
    		Intent googleIntent = new Intent(Intent.ACTION_VIEW, u);
    		getContext().startActivity(googleIntent);
 		}
		
		return false;
	}
}
