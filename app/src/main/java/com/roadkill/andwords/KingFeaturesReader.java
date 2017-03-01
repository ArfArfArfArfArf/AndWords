package com.roadkill.andwords;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import android.util.Log;

public class KingFeaturesReader 
{
	public static void save(CrosswordInfo info)
	{
		try
		{
			// read in the puzzle from disk, and add in the user input into the diagram section
			File outFile = new File(AndWords.PUZZLE_PATH, "tmp");
			
			InputStreamReader streamIn = new InputStreamReader(new FileInputStream(info.mPuzzlePath), "MacRoman");
			outFile.createNewFile();
			FileWriter os = new FileWriter(outFile);
		
			int nRead = streamIn.read();
			
			while (nRead != -1 && nRead != '}')
			{
				os.write(nRead);
				nRead = streamIn.read();
			}
			
			streamIn.close();
			
			os.write('}');
			
			for (int i = 0; i < info.nHeight; i++)
			{
				for (int j = 0; j < info.nWidth; j++)
				{
					os.write(info.mDiagram[i][j]);
				}
			}
			
			os.write('\r');
			
			os.write("}");
			
			if (info.strAuthor != null)
				os.write(info.strAuthor);
			else
				os.write("");
			
			os.write('\r');
			
			os.close();
			
			File f = new File(info.mPuzzlePath);
			f.delete();
			
			outFile.renameTo(f);
		}
		catch (Exception e)
		{
			Log.v("KFS", "Can't save file: " + e.getLocalizedMessage());
		}
		
	}
	
	public static void read(CrosswordInfo info, String strSource)
	{
		HttpURLConnection uc = null;
		
		try
		{
			URL u = new URL(strSource);

			uc = (HttpURLConnection) u.openConnection();
			
			uc.connect();
			
			if (uc.getResponseCode() != HttpURLConnection.HTTP_OK)
			{
				//cantLoadPuzzle(c, strSource);
				info.mLoadComplete = false;
				info.mErrorString = uc.getResponseMessage();
				uc.disconnect();
				return;
			}
			
			InputStreamReader br = new InputStreamReader(uc.getInputStream(), "MacRoman");
			File f = new File(info.mPuzzlePath);
			
			f.createNewFile();
			FileWriter fw = new FileWriter(f);
			
			int i;
			
			char buf[] = new char[1024];

			while ((i = br.read(buf)) != -1)
				fw.write(buf, 0, i);
			
			fw.flush();
			br.close();
			fw.close();
			
			read(info);
		}
		catch (Exception e)
		{
			info.mLoadComplete = false;
			info.mErrorString = e.getLocalizedMessage();
			return;
		}
	}
	
	public static void read(CrosswordInfo info)
	{
		info.mPuzzleType = AndWords.KFS;
		
		try
		{
			Scanner s = new Scanner(new InputStreamReader(new FileInputStream(info.mPuzzlePath), "MacRoman"));
			
			if (!s.hasNextLine())
			{
				info.mErrorString = "Empty puzzle";
				info.mLoadComplete = false;
				return;
			}
			
			String line = s.nextLine();
			
			if (!line.startsWith("{") || !s.hasNextLine())
			{
				info.mErrorString = "Error in puzzle file";
				info.mLoadComplete = false;
				return;
			}
			
			line = s.nextLine();
			
			while (!line.startsWith("{"))
			{
				if (!s.hasNextLine())
				{
					info.mErrorString = "Error in puzzle file";
					info.mLoadComplete = false;
					return;
				}
				
				line = s.nextLine();
			}
			
            // Process solution grid.
            List<char[]> solGrid = new ArrayList<char[]>();
            line = line.substring(1, line.length()-2);
            String[] rowString = line.split(" ");
            int width = rowString.length;
            
            do 
            {
                if (line.endsWith(" |")) 
                {
                   line = line.substring(0, line.length()-2);
                }
                rowString = line.split(" ");
                if (rowString.length != width) 
                {
                    info.mErrorString = "Error in puzzle file.";
                    info.mLoadComplete = false;
                    return;
                }

                char[] row = new char[width];
                for (int x = 0; x < width; x++) {
                        row[x] = rowString[x].charAt(0);
                }
                solGrid.add(row);
                
                if (!s.hasNextLine()) 
                {
                    info.mErrorString = "Error in puzzle file.";
                    info.mLoadComplete = false;
                    return;
                }
                line = s.nextLine();
            } while (!line.startsWith("{"));
			
            int height = solGrid.size();
            info.nHeight = height;
            info.nWidth = width;
            info.mDiagram = new char[height][width];
            info.mSolution = new char[height][width];
            
            for (int i = 0; i < height; i++)
            {
            	char[] row = solGrid.get(i);
            	for (int j = 0; j < width; j++)
            	{
            		info.mSolution[i][j] = Character.toUpperCase(row[j]);
            		info.mDiagram[i][j] = ' ';
            		
            		if (info.mSolution[i][j] == '#')
            			info.mSolution[i][j] = '.';
            		
            		if (info.mSolution[i][j] == '.')
            			info.mDiagram[i][j] = '.';
            	}
            }
            Map<Integer, String> acrossNumToClueMap = new HashMap<Integer, String>();
            line = line.substring(1);
            int clueNum;
            do {
                if (line.endsWith(" |")) 
                {
                    line = line.substring(0, line.length()-2);
                }
                clueNum = 0;
                int i = 0;
                
                while (line.charAt(i) != '.') 
                {
                    if (clueNum != 0) {
                            clueNum *= 10;
                    }
                    clueNum += line.charAt(i) - '0';
                    i++;
                }
                
                String clue = line.substring(i+2).trim();
                acrossNumToClueMap.put(clueNum, clue);
                if (!s.hasNextLine()) 
                {
                    info.mErrorString = "Error in puzzle file.";
                    info.mLoadComplete = false;
                    return;
                }
                line = s.nextLine();
            } while (!line.startsWith("{"));
            
            int maxClueNum = clueNum;
            
            Map<Integer, String> downNumToClueMap = new HashMap<Integer, String>();
            line = line.substring(1);
            boolean finished = false;
            do {
                if (line.endsWith(" |")) 
                {
                    line = line.substring(0, line.length()-2);
                } 
                else 
                {
                    finished = true;
                }
                
                if (line.length() != 0)
                {
	                clueNum = 0;
	                int i = 0;
	                while (line.charAt(i) != '.') 
	                {
	                    if (clueNum != 0) 
	                    {
	                        clueNum *= 10;
	                    }
	                    clueNum += line.charAt(i) - '0';
	                    i++;
	                }
	                String clue = line.substring(i+2).trim();
	                downNumToClueMap.put(clueNum, clue);
                }
                
                if(!finished) 
                {
                    if (!s.hasNextLine())
                    {
                    	info.mErrorString = "Error in puzzle file.";
                        info.mLoadComplete = false;
                        return;
                    }
                    line = s.nextLine();
                }
            } while (!finished);
            
            maxClueNum = clueNum > maxClueNum ? clueNum : maxClueNum;
            
			//build the cell number array
			info.mCellNumbers = new int[info.nHeight][info.nWidth];
			
			int cellNumber = 1;
			
			for (int i = 0; i < info.nHeight; i++)
			{
				for (int j = 0; j < info.nWidth; j++)
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
			
			for (int i = 0; i < info.nHeight; i++)
			{
				for (int j = 0; j < info.nWidth; j++)
				{
					boolean needsAcross = ReaderUtils.cellNeedsAcrossNumber(info, i, j);
					boolean needsDown = ReaderUtils.cellNeedsDownNumber(info, i, j);
					
					if (needsAcross || needsDown)
					{
						if (needsAcross)
							info.strAcrossClues[cellNumber] = acrossNumToClueMap.get(cellNumber);
						
						if (needsDown)
							info.strDownClues[cellNumber] = downNumToClueMap.get(cellNumber);
						
						cellNumber++;
					}					
				}
			}
			
			// user input at the end of the file
			if (s.hasNextLine())
			{
				line = s.nextLine();
				
				while (!line.startsWith("}"))
					line = s.nextLine();
				
				line = line.substring(1);
				
				boolean bBadChar = false;
				
				for (int i = 0; i < height; i++)
				{
					for (int j = 0; j < width; j++)
					{
						info.mDiagram[i][j] = line.charAt(i * width + j);
						if (info.mDiagram[i][j] != info.mSolution[i][j])
							bBadChar = true;
					}
				}
				
				if (bBadChar == false)
					info.mPuzzleComplete = true;
			}
			
			// author info
			if (s.hasNextLine())
			{
				line = s.nextLine();
				
				while (!line.startsWith("}"))
					line = s.nextLine();
				
				line = line.substring(1, line.length());
				
				info.strAuthor = line;
			}
			
			s.close();
			
			info.mLoadComplete = true;
		}
		catch (Exception e)
		{
			info.mLoadComplete = false;
			info.mErrorString = e.getLocalizedMessage();
		}
	}
}
