package com.roadkill.andwords;

public class CrosswordInfo
{
	public int nWidth;
	public int nHeight;

	public int mPuzzleType;
	
	public String[] strAcrossClues;
	public String[] strDownClues;
	public char[][] mDiagram;
	
	public int[][] mCellNumbers;
	
	public char[][] mSolution;
	
	public char[][] mCircledSquares;
	
	public String strTitle;
	public String strAuthor;
	public String strCopyright;
	public String strNotes;
	
	public int mLastCellNumber;
	
	public boolean mLoadComplete;
	
	public boolean mPuzzleComplete;
	
	public int mPuzzleID;
	
	public String mErrorString;
	
	public boolean mScrambled;
	
	public String mPuzzlePath;
	
	public String mPuzzleName;
	
	public CrosswordInfo()
	{
		mLoadComplete = false;
	}
}
