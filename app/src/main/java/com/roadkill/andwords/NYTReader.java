package com.roadkill.andwords;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import android.text.Html;
import android.util.Log;

public class NYTReader 
{
	public static void save(CrosswordInfo info)
	{
		try
		{
			// read in the puzzle from disk, and add in the user input into the diagram section
			File outFile = new File(AndWords.PUZZLE_PATH, "tmp");
			
			InputStreamReader streamIn = new InputStreamReader(new FileInputStream(info.mPuzzlePath), "UTF-8");
			outFile.createNewFile();
			FileWriter os = new FileWriter(outFile);
			
			int nRead = streamIn.read();
			
			int brackets = 0;
			
			boolean bQuote = false;
			String strQuote = "";
			
			while (nRead != -1)
			{
				if ((char) nRead == '{')
					++brackets;
				
				if ((char) nRead == '}')
					--brackets;

				if ((char) nRead == '\"')
				{
					bQuote = !bQuote;
					
					if (bQuote == false)
					{
						if (strQuote.equals("user_input"))
						{
							os.write("\":[");
							for (int i = 0; i < info.nHeight; i++)
							{
								for (int j = 0; j < info.nWidth; j++)
								{
									if (i == 0 && j == 0)
										os.write("\"");
									else
										os.write(",\"");
									
									os.write(info.mDiagram[i][j]);
									os.write("\"");
								}
							}
							
							os.write("]};");
							
							os.close();
							
							File f = new File(info.mPuzzlePath);
							f.delete();
							
							outFile.renameTo(f);
							
							return;
						}
						
						strQuote = "";
					}
					os.write(nRead);
					nRead = streamIn.read();
					continue;
				}
				
				if (bQuote)
					strQuote += (char) nRead;
				
				if ((char) nRead == '}')
				{
					if (brackets != 0)
						os.write(nRead);
				}
				else
					os.write(nRead);
				
				nRead = streamIn.read();
			}
			
			streamIn.close();
			
			os.write(",\"user_input\":[");
			
			for (int i = 0; i < info.nHeight; i++)
			{
				for (int j = 0; j < info.nWidth; j++)
				{
					if (i == 0 && j == 0)
						os.write("\"");
					else
						os.write(",\"");
					
					os.write(info.mDiagram[i][j]);
					os.write("\"");
				}
			}
			
			os.write("]};");
			
			os.close();
			
			File f = new File(info.mPuzzlePath);
			f.delete();
			
			outFile.renameTo(f);
		}
		catch (Exception e)
		{
			Log.v("NYT", "Can't save file: " + e.getLocalizedMessage());
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
			
			InputStreamReader br = new InputStreamReader(uc.getInputStream(), "UTF-8");
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
		info.mPuzzleType = AndWords.NYT;

		String text;
		
		try
		{
			text = new Scanner( new File(info.mPuzzlePath) ).useDelimiter("\\A").next();
		}
		catch (Exception e)
		{
			info.mLoadComplete = false;
			info.mErrorString = e.getLocalizedMessage();
			return;
		}
		
		JSONObject jsonresults;
		
		try
		{
			jsonresults = new JSONObject(text);
			
			JSONArray json = jsonresults.getJSONArray("results");
			JSONObject results = json.getJSONObject(0);
			
			JSONObject meta = results.getJSONObject("puzzle_meta");
			
			info.nHeight = meta.getInt("height");
			info.nWidth = meta.getInt("width");
			
			info.mDiagram = new char[info.nHeight][info.nWidth];
			info.mSolution = new char[info.nHeight][info.nWidth];
			
			info.strAuthor = meta.getString("author");
			info.strCopyright = Html.fromHtml(meta.getString("copyright")).toString();
			info.strTitle = meta.getString("title");
			
			if (info.strTitle == null || info.strTitle.equals("") || info.strTitle.equals("null"))
				info.strTitle = "NYT Classic Puzzle";
			
			JSONObject puzzleData = results.getJSONObject("puzzle_data");
			JSONArray layout = puzzleData.getJSONArray("layout");
			
			int x = 0, y = 0;
			
			int i;
			
			int len = layout.length();
			
			for (i = 0; i < len; i++)
			{
				if (x >= info.nWidth)
				{
					x = 0; ++y;
				}
				
				int l = layout.getInt(i);
				
				if (l != 0)
				{
					info.mDiagram[y][x] = ' ';
					
					// TODO
					// TODO l == 2?  NO idea what 2 means
					// TODO 
					
					if (l == 3)
					{
						if (info.mCircledSquares == null)
							info.mCircledSquares = new char[info.nHeight][info.nWidth];
						
						info.mCircledSquares[y][x] = 1;
					}
				}
				else
				{
					info.mDiagram[y][x] = '.';
				}
				
				++x;
			}
			
			JSONObject clues = puzzleData.getJSONObject("clues");
			
			JSONArray across = clues.getJSONArray("A");
			
			len = across.length();
			
			String strAcrossClues[] = new String[len + 1];
			
			for (i = 0; i < len; i++)
			{
				JSONObject clue = across.getJSONObject(i);
				strAcrossClues[i] = Html.fromHtml(clue.getString("value")).toString();
			}
			
			JSONArray down = clues.getJSONArray("D");
			
			len = down.length();
			
			String strDownClues[] = new String[len + 1];
			
			for (i = 0; i < len; i++)
			{
				JSONObject clue = down.getJSONObject(i);
				strDownClues[i] = Html.fromHtml(clue.getString("value")).toString();
			}
			
			JSONArray answers = puzzleData.getJSONArray("answers");
			
			len = answers.length();

			x = 0;
			y = 0;
			
			for (i = 0; i < len; i++)
			{
				if (x >= info.nWidth)
				{
					x = 0; ++y;
				}
				
				String str = answers.getString(i);
				
				if (str != null && str.length() >= 1  && !str.equalsIgnoreCase("null"))
				{
					info.mSolution[y][x] = Character.toUpperCase(str.charAt(0));
				}
				else
					info.mSolution[y][x] = '.';
				
				++x;
			}

			try
			{
				JSONArray user_input = jsonresults.getJSONArray("user_input");
				
				len = user_input.length();
				
				x = 0;
				y = 0;
	
				for (i = 0; i < len; i++)
				{
					if (x >= info.nWidth)
					{
						x = 0; ++y;
					}
					
					String str = user_input.getString(i);
					
					if (str != null && str.length() >= 1)
					{
						info.mDiagram[y][x] = str.charAt(0);
					}
					else
						info.mDiagram[y][x] = '.';
					
					++x;
				}
			}
			catch (Exception e)
			{
				
			}
			//build the cell number array
			info.mCellNumbers = new int[info.nHeight][info.nWidth];
			
			int cellNumber = 1;
			
			int curAcrossClueNum = 0;
			int curDownClueNum = 0;
			int j;
			
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
			
		}
		catch (Exception e)
		{
			info.mLoadComplete = false;
			info.mErrorString = e.getLocalizedMessage();
			return;
		}
		
		try
		{
			info.mLoadComplete = true;
		}
		catch (Exception e)
		{
			info.mLoadComplete = false;
			info.mErrorString = e.getLocalizedMessage();
		}
	}
}
