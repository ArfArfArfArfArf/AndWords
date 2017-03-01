package com.roadkill.andwords;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class WSJReader 
{
	public static void save(CrosswordInfo info)
	{
		try
		{
			File outFile = new File(AndWords.PUZZLE_PATH, "tmp");
			
			InputStreamReader streamIn = new InputStreamReader(new FileInputStream(info.mPuzzlePath), "ISO-8859-1");
			outFile.createNewFile();
			FileWriter os = new FileWriter(outFile);
		
			String str = ReaderUtils.readStringFromTextFile(streamIn, true);
			
			os.write(str);
			os.write("\n");
			str = ReaderUtils.readStringFromTextFile(streamIn, true);
			
			os.write(str);
			os.write("\n");
			str = ReaderUtils.readStringFromTextFile(streamIn, true);
			
			os.write(str);
			os.write("\n");
			str = ReaderUtils.readStringFromTextFile(streamIn, true);
			
			os.write(str);
			os.write("\n");
			
			int i, j;
			
			for (i = 0; i < info.nHeight; i++)
			{
				for (j = 0; j < info.nWidth; j++)
				{
					os.write(info.mDiagram[i][j]);
				}
			}
			
			os.write("\n");
			
			os.close();
			
			File f = new File(info.mPuzzlePath);
			f.delete();
			
			outFile.renameTo(f);
			
		}
		catch (Exception e)
		{
			
		}
	}
	
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
			
			String strSize = ReaderUtils.readStringFromTextFile(br, true);
			
			int pos = strSize.indexOf('|');
			
			String strWidth = strSize.substring(0, pos);
			String strHeight = strSize.substring(pos + 1);
			
			info.nHeight = Integer.parseInt(strHeight);
			info.nWidth = Integer.parseInt(strWidth);
			
			String strSolution = ReaderUtils.readStringFromTextFile(br, true);
			
			strSolution.toUpperCase();
			
			int i, j;
			
			int index = 0;
			
			info.mSolution = new char[info.nHeight][info.nWidth];
			info.mDiagram = new char[info.nHeight][info.nWidth]
					;
			for (i = 0; i < info.nHeight; i++)
			{
				for (j = 0; j < info.nWidth; j++)
				{
					info.mSolution[i][j] = strSolution.charAt(index);
					
					if (info.mSolution[i][j] == '+')
					{
						info.mSolution[i][j] = '.';
						info.mDiagram[i][j] = '.';
					}
					else
						info.mDiagram[i][j] = ' ';
					
					++index;
				}
			}

			//build the cell number array
			info.mCellNumbers = new int[info.nHeight][info.nWidth];
			
			int cellNumber = 1;
			
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
			
			String strClues = ReaderUtils.readStringFromTextFile(br, true);
			
			index = 0;
			
			while (index >= 0)
			{
				int nClueNumPos;
				
				try
				{
					nClueNumPos = strClues.indexOf('|', index);
				}
				catch (Exception e)
				{
					nClueNumPos = -1;
				}
				
				if (nClueNumPos < 0)
					break;
				
				int nClueNum = Integer.parseInt(strClues.substring(index, nClueNumPos));
				
				int nAcrossClueEnd = strClues.indexOf('|', nClueNumPos + 1);
				int nDownClueEnd = strClues.indexOf('|', nAcrossClueEnd + 1);
				
				String strAcrossClue = strClues.substring(nClueNumPos + 1, nAcrossClueEnd);
				
				if (strAcrossClue.length() > 0)
					info.strAcrossClues[nClueNum] = strAcrossClue;
				
				String strDownClue = strClues.substring(nAcrossClueEnd + 1, nDownClueEnd);
				
				if (strDownClue.length() > 0)
					info.strDownClues[nClueNum] = strDownClue;
				
				index = nDownClueEnd + 1;
			}
			
			String strInfo = ReaderUtils.readStringFromTextFile(br, true);
			
			int nNameEnd = strInfo.indexOf('|');
			
			if (nNameEnd > 0)
			{
				info.strTitle = strInfo.substring(0, nNameEnd);
				info.strAuthor = strInfo.substring(nNameEnd + 1);
			}
			
			try
			{
				String strUserInput = ReaderUtils.readStringFromTextFile(br, true);
				index = 0;
				
				for (i = 0; i < info.nHeight; i++)
				{
					for (j = 0; j < info.nWidth; j++)
					{
						char c = strUserInput.charAt(index);
						
						++index;
						
						info.mDiagram[i][j] = c;
						
					}
				}
			}
			catch (Exception e)
			{
				
			}
			
			info.mLoadComplete = true;
			info.mPuzzleType = AndWords.WSJ;
		}
		catch (Exception e)
		{
			info.mLoadComplete = false;
		}
	}
}
