package com.roadkill.andwords;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import android.util.Log;

public class ThinksComReader 
{
	public static void read(CrosswordInfo info, String strURL)
	{
		InputStreamReader br = null;
		
		try
		{
			URL u = new URL(strURL);
			URLConnection uc = u.openConnection();
			
			br = new InputStreamReader(uc.getInputStream(), "ISO-8859-1");
			
			File f = new File(info.mPuzzlePath);
			f.createNewFile();
			FileOutputStream os = new FileOutputStream(f);
			
			int i;
			
			while ((i = br.read()) != -1)
				os.write(i);
			
			br.close();
			os.close();
		}
		catch (Exception e)
		{
			info.mLoadComplete = false;
			info.mErrorString = e.getLocalizedMessage();

			return;
		}
		
		
		readFromFile(info);
	}
	
	public static void readFromFile(CrosswordInfo info)
	{
		try
		{
			InputStreamReader br = new InputStreamReader(new FileInputStream(info.mPuzzlePath), "ISO-8859-1");
			String strHeader = ReaderUtils.readStringFromTextFile(br);
			
			// ARCHIVE should be the first line
			if (!strHeader.equals("ARCHIVE"))
			{
				
			}
			
			// skip a blank line
			ReaderUtils.readStringFromTextFile(br);
			// date
			ReaderUtils.readStringFromTextFile(br);
			// blank line
			ReaderUtils.readStringFromTextFile(br);
			// title
			info.strTitle = ReaderUtils.readStringFromTextFile(br);
			// blank line
			ReaderUtils.readStringFromTextFile(br);
			// Author
			info.strAuthor = ReaderUtils.readStringFromTextFile(br);
			// blank line
			ReaderUtils.readStringFromTextFile(br);
			// width
			info.nWidth = Integer.parseInt(ReaderUtils.readStringFromTextFile(br));
			// blank line
			ReaderUtils.readStringFromTextFile(br);
			// height
			info.nHeight = Integer.parseInt(ReaderUtils.readStringFromTextFile(br));
			// blank line
			ReaderUtils.readStringFromTextFile(br);
			// num across clues
			int numAcrossClues = Integer.parseInt(ReaderUtils.readStringFromTextFile(br));
			// blank line
			ReaderUtils.readStringFromTextFile(br);
			// num down clues
			int numDownClues = Integer.parseInt(ReaderUtils.readStringFromTextFile(br));
			// blank line
			ReaderUtils.readStringFromTextFile(br);

			info.mSolution = new char[info.nHeight][info.nWidth];
			info.mDiagram = new char[info.nHeight][info.nWidth];
			
			int i, j;
			
			for (i = 0; i < info.nHeight; i++)
			{
				String s = ReaderUtils.readStringFromTextFile(br);
				
				s.toUpperCase();
				
				for (j = 0; j < s.length(); j++)
				{
					info.mSolution[i][j] = s.charAt(j);
					info.mDiagram[i][j] = ' ';
					
					if (info.mSolution[i][j] == '#')
					{
						info.mSolution[i][j] = '.';
						info.mDiagram[i][j] = '.';
					}
				}
			}
			
			// blank line
			ReaderUtils.readStringFromTextFile(br);
			
			String[] strAcrossClues = new String[numAcrossClues];
			
			for (i = 0; i < numAcrossClues; i++)
				strAcrossClues[i] = ReaderUtils.readStringFromTextFile(br);
			
			// Blank line
			ReaderUtils.readStringFromTextFile(br);
			
			String[] strDownClues = new String[numDownClues];
			for (i = 0; i < numDownClues; i++)
				strDownClues[i] = ReaderUtils.readStringFromTextFile(br);
			
			// either there will be a \r char signifying the end of the file, or
			// there will be user input here
			if (br.read() != '\r')
			{
				boolean badChar = false;
				
				for (i = 0; i < info.nHeight; i++)
				{
					String s = ReaderUtils.readStringFromTextFile(br);
					
					for (j = 0; j < s.length(); j++)
					{
						info.mDiagram[i][j] = s.charAt(j);
						
						if (info.mDiagram[i][j] != info.mSolution[i][j] && info.mSolution[i][j] != '.')
							badChar = true;
					}
				}
				
				if (badChar == false)
					info.mPuzzleComplete = true;
				
			}
			
			//build the cell number array
			info.mCellNumbers = new int[info.nHeight][info.nWidth];
			
			int cellNumber = 1;
			
			int curAcrossClueNum = 0;
			int curDownClueNum = 0;
			
			for (i = 0; i < info.nHeight; i++)
			{
				for (j = 0; j < info.nWidth; j++)
				{
					if (ReaderUtils.cellNeedsAcrossNumber(info, i, j) || ReaderUtils.cellNeedsDownNumber(info, i, j))
						info.mCellNumbers[i][j] = cellNumber++;
					else
						info.mCellNumbers[i][j] = 0;
				}
			}
			
			info.strAcrossClues = new String[cellNumber];
			info.strDownClues = new String[cellNumber];

			info.mLastCellNumber = cellNumber;
			
			cellNumber = 1;
			
			for (i = 0; i < info.nHeight; i++)
			{
				for (j = 0; j < info.nWidth; j++)
				{
					boolean needsAcross = ReaderUtils.cellNeedsAcrossNumber(info, i, j);
					boolean needsDown = ReaderUtils.cellNeedsDownNumber(info, i, j);
					
					if (needsAcross || needsDown)
					{
						if (needsAcross)
							info.strAcrossClues[cellNumber] = strAcrossClues[curAcrossClueNum++];
						
						if (needsDown)
							info.strDownClues[cellNumber] = strDownClues[curDownClueNum++];
						
						cellNumber++;
					}					
				}
			}
			
			
			br.close();
		}
		catch (Exception e)
		{
			info.mLoadComplete = false;
		}
		
		info.mPuzzleType = AndWords.BRAINSONLY_COM;
		info.mLoadComplete = true;
	}
	
	public static boolean save(CrosswordInfo i)
	{
		try
		{
			// read in the puzzle from disk, and add in the user input into the diagram section
			File outFile = new File(AndWords.PUZZLE_PATH, "tmp");
			
			InputStreamReader streamIn = new InputStreamReader(new FileInputStream(i.mPuzzlePath), "ISO-8859-1");
			outFile.createNewFile();
			FileWriter os = new FileWriter(outFile);
			
			int numAcrossClues = 0;
			int numDownClues = 0;
			
			int len = i.strAcrossClues.length;
			
			int j;
			
			for (j = 0; j < len; j++)
				if (i.strAcrossClues[j] != null)
					++numAcrossClues;
			
			len = i.strDownClues.length;
			
			for (j = 0; j < len; j++)
				if (i.strDownClues[j] != null)
					++numDownClues;
			
			int linesToRead = 16 + i.nHeight + 1 + numAcrossClues + 1 + numDownClues;
			
			for (j = 0; j < linesToRead; j++)
			{
				String s = ReaderUtils.readStringFromTextFile(streamIn);
				os.write(s);
				os.write("\n");
			}
			
			streamIn.close();
			
			os.write("\n");
			
			// write out the user input
			int k;
			
			for (j = 0; j < i.nHeight; j++)
			{
				for (k = 0; k < i.nWidth; k++)
				{
					os.write((int)i.mDiagram[j][k]);
				}
				os.write("\n");
			}
			
			// terminate the file
			os.write("\r\n");
			os.close();
			
			File f = new File(i.mPuzzlePath);
			f.delete();
			
			outFile.renameTo(f);
			
			return true;
		}
		catch (Exception e)
		{
			Log.v("ERROR", "Unable to save puzzle" + e.getLocalizedMessage());
		}
		
		return false;
	}
}

