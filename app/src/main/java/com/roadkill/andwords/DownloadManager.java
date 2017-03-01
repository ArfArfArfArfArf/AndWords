package com.roadkill.andwords;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.os.Environment;
import android.util.Log;

public class DownloadManager 
{
	public static class FileInfo
	{
		long mFileDate;
		boolean mCompleted;
		String mFilePath;
		
		public FileInfo()
		{
			
		}
	}

	public static boolean isPuzzleDone(String strPath)
	{
		if (strPath.contains("-done"))
			return true;
		return false;
	}
	
	public static String buildPuzzlePath(String puzzleName, Date date, int nType)
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd", Locale.US);
		
		String puzzlePath = AndWords.PUZZLE_PATH + "/" + puzzleName + " " + dateFormat.format(date);

		String strExt = "";

		switch (nType)
		{
		case AndWords.UCLICK:
			strExt = ".xml";
			break;
		case AndWords.ACROSS_LITE:
			strExt = ".puz";
			break;
		case AndWords.BRAINSONLY_COM:
			strExt = ".txt";
			break;
		case AndWords.JSZ:
			strExt = ".jsz";
			break;
		case AndWords.KFS:
			strExt = ".kfs";
			break;
		case AndWords.NYT:
			strExt = ".nyt";
			break;
		case AndWords.WSJ:
			strExt = ".wsj";
			break;
		case AndWords.GNY_JSON:
			strExt = ".jsn";
			break;
		}

		File f = new File(puzzlePath + strExt);
		
		if (!f.exists())
		{
			String strDonePath = getDonePath(puzzlePath + strExt);
			
			f = new File(strDonePath);
			
			if (f.exists())
				return strDonePath;

			// check the old /andwords path
			String strOldPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/andwords/" + puzzleName + " " + dateFormat.format(date);

			f = new File(strOldPath + strExt);

			if (f.exists())
				return strOldPath + strExt;

			strDonePath = getDonePath(strOldPath + strExt);

			f = new File(strDonePath);

			if (f.exists())
				return strDonePath;

		}
		return puzzlePath + strExt;
	}
	
	public static String getDonePath(String strPath)
	{
		int len = strPath.length();
		
		if (strPath.contains("-done"))
			return strPath;
		
		String s = strPath.substring(0,  len - 4);
		s += "-done" + strPath.substring(len - 4);
		
		return s;
	}

	public static FileInfo[] loadFileInfo()
	{
		return loadFileInfo(null);
	}

	public static FileInfo[] loadFileInfo(String strDir)
	{
		File dir;

		if (strDir == null)
			dir = new File(AndWords.PUZZLE_PATH);
		else
			dir = new File(strDir);
		
		File[] puzzles = dir.listFiles();

		if (puzzles == null)
			return null;

		FileInfo[] fileInfo = new FileInfo[puzzles.length];
		
		for (int i = 0; i < puzzles.length; i++)
		{
			File f = new File(puzzles[i].getAbsolutePath());
			
			try
			{
				fileInfo[i] = new FileInfo();
				fileInfo[i].mFileDate = f.lastModified();
				fileInfo[i].mFilePath = f.getAbsolutePath();
				fileInfo[i].mCompleted = fileInfo[i].mFilePath.contains("done");
			}
			catch (Exception e)
			{
				Log.e("ANDWORDS", e.getLocalizedMessage());
			}
		}
		
		return fileInfo;
	}

	private static FileInfo[] mergeFileLists(FileInfo[] f1, FileInfo[] f2)
	{
		if (f1 == null)
			return f2;

		if (f2 == null)
			return f1;

		int len1 = f1.length;
		int len2 = f2.length;

		FileInfo[] f3 = new FileInfo[len1 + len2];

		System.arraycopy(f1, 0, f3, 0, len1);
		System.arraycopy(f2, 0, f3, len1, len2);

		return f3;
	}

	public static void deleteOldPuzzles()
	{
		Date d = new Date();
		long time = d.getTime();

		FileInfo newFiles[] = loadFileInfo();
		FileInfo oldFiles[] = loadFileInfo(Environment.getExternalStorageDirectory() + "/andwords");

		FileInfo[] files = mergeFileLists(newFiles, oldFiles);

		if (files == null)
			return;

		for (FileInfo f : files)
		{
			if ((time - f.mFileDate) > 1209600000l)
			{
				try
				{
					File filePath = new File(f.mFilePath);
					filePath.delete();
				}
				catch (Exception e)
				{
					if (AndWords.DEBUG)
						Log.e("ANDWORDS", "Unable to remove file: " + e.getLocalizedMessage());
				}
			}
		}
	}
}
