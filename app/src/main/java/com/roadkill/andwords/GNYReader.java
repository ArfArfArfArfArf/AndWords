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

public class GNYReader 
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
			Log.v("GNY", "Can't save file: " + e.getLocalizedMessage());
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
		info.mPuzzleType = AndWords.GNY_JSON;

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
			JSONObject data = jsonresults.getJSONObject("data");
			JSONObject copy = data.getJSONObject("copy");
			
			info.strAuthor = copy.getString("byline");
			info.strTitle = copy.getString("title");
			
            JSONObject gridSize = copy.getJSONObject("gridsize");
            
            info.nWidth = gridSize.getInt("cols");
            info.nHeight = gridSize.getInt("rows");

			int i, j, len, x = 0, y = 0;

			info.mDiagram = new char[info.nHeight][info.nWidth];
			info.mSolution = new char[info.nHeight][info.nWidth];

			JSONArray grid = data.getJSONArray("grid");

			len = grid.length();

			for (i = 0; i < len; i++)
			{
				JSONArray row = grid.getJSONArray(i);

				for (j = 0; j < row.length(); j++) {
					JSONObject letter = row.getJSONObject(j);

					info.mDiagram[i][j] = ' ';

					if (letter.getString("Blank").equalsIgnoreCase("blank")) {
						info.mDiagram[i][j] = '.';
						info.mSolution[i][j] = '.';
					} else {
						info.mSolution[i][j] = letter.getString("Letter").charAt(0);
					}
				}
			}

			JSONArray clues = copy.getJSONArray("clues");

			String strAcrossClues[] = null;
			String strDownClues[] = null;

			for (i = 0; i < clues.length(); i++	)
			{
				JSONObject cluesObj = clues.getJSONObject(i);

				boolean bAcross = cluesObj.getString("title").equalsIgnoreCase("across");

				JSONArray cluesArray = cluesObj.getJSONArray("clues");

				len = cluesArray.length();

				if (bAcross)
					strAcrossClues = new String[len + 1];
				else
				    strDownClues = new String[len + 1];

				for (j = 0; j < len; j++)
				{
					JSONObject clue = cluesArray.getJSONObject(j);

					if (bAcross)
						strAcrossClues[j] = Html.fromHtml(clue.getString("clue")).toString();
					else
						strDownClues[j] = Html.fromHtml(clue.getString("clue")).toString();
				}
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

		info.mLoadComplete = true;
	}
}

/*
{
    "data": {
        "competitioncrossword": 0, 
        "copy": {
            "byline": "By Alice Long/Edited by Mike Shenk", 
            "clues": [
                {
                    "clues": [
                        {
                            "answer": "TUG", 
                            "clue": "Barge mover", 
                            "format": "3", 
                            "length": 3, 
                            "number": "1", 
                            "word": 1
                        }, 
                        {
                            "answer": "THAIS", 
                            "clue": "Title courtesan of opera", 
                            "format": "5", 
                            "length": 5, 
                            "number": "4", 
                            "word": 2
                        }, 
                        {
                            "answer": "SPAT", 
                            "clue": "Exchange of cross words", 
                            "format": "4", 
                            "length": 4, 
                            "number": "9", 
                            "word": 3
                        }, 
                        {
                            "answer": "STU", 
                            "clue": "Cook of Creedence Clearwater Revival", 
                            "format": "3", 
                            "length": 3, 
                            "number": "13", 
                            "word": 4
                        }, 
                        {
                            "answer": "WETMOP", 
                            "clue": "Custodian's tool", 
                            "format": "6", 
                            "length": 6, 
                            "number": "14", 
                            "word": 5
                        }, 
                        {
                            "answer": "TASE", 
                            "clue": "Charge a criminal?", 
                            "format": "4", 
                            "length": 4, 
                            "number": "16", 
                            "word": 6
                        }, 
                        {
                            "answer": "PENCILMUST", 
                            "clue": "Errol Flynn trademark", 
                            "format": "10", 
                            "length": 10, 
                            "number": "17", 
                            "word": 7
                        }, 
                        {
                            "answer": "ACHE", 
                            "clue": "Long", 
                            "format": "4", 
                            "length": 4, 
                            "number": "19", 
                            "word": 8
                        }, 
                        {
                            "answer": "PROCESS", 
                            "clue": "Make sense of", 
                            "format": "7", 
                            "length": 7, 
                            "number": "20", 
                            "word": 9
                        }, 
                        {
                            "answer": "BOTTOM", 
                            "clue": "Seat", 
                            "format": "6", 
                            "length": 6, 
                            "number": "21", 
                            "word": 10
                        }, 
                        {
                            "answer": "AMEN", 
                            "clue": "Word of agreement", 
                            "format": "4", 
                            "length": 4, 
                            "number": "23", 
                            "word": 11
                        }, 
                        {
                            "answer": "GOWEST", 
                            "clue": "Advice for a young man", 
                            "format": "6", 
                            "length": 6, 
                            "number": "24", 
                            "word": 12
                        }, 
                        {
                            "answer": "PINE", 
                            "clue": "Long", 
                            "format": "4", 
                            "length": 4, 
                            "number": "25", 
                            "word": 13
                        }, 
                        {
                            "answer": "ALGLAND", 
                            "clue": "Melatonin source", 
                            "format": "7", 
                            "length": 7, 
                            "number": "27", 
                            "word": 14
                        }, 
                        {
                            "answer": "EDGAR", 
                            "clue": "Item for Martha Grimes's mantelpiece", 
                            "format": "5", 
                            "length": 5, 
                            "number": "30", 
                            "word": 15
                        }, 
                        {
                            "answer": "ORATE", 
                            "clue": "Stand and deliver, perhaps", 
                            "format": "5", 
                            "length": 5, 
                            "number": "32", 
                            "word": 16
                        }, 
                        {
                            "answer": "PEN", 
                            "clue": "Stock holder", 
                            "format": "3", 
                            "length": 3, 
                            "number": "33", 
                            "word": 17
                        }, 
                        {
                            "answer": "CLETE", 
                            "clue": "Boyer who played with Berra", 
                            "format": "5", 
                            "length": 5, 
                            "number": "36", 
                            "word": 18
                        }, 
                        {
                            "answer": "AID", 
                            "clue": "Backing", 
                            "format": "3", 
                            "length": 3, 
                            "number": "37", 
                            "word": 19
                        }, 
                        {
                            "answer": "REEVE", 
                            "clue": "Four-time Kent portrayer", 
                            "format": "5", 
                            "length": 5, 
                            "number": "38", 
                            "word": 20
                        }, 
                        {
                            "answer": "SYS", 
                            "clue": "Method: Abbr.", 
                            "format": "3", 
                            "length": 3, 
                            "number": "40", 
                            "word": 21
                        }, 
                        {
                            "answer": "PATSY", 
                            "clue": "Fall guy", 
                            "format": "5", 
                            "length": 5, 
                            "number": "41", 
                            "word": 22
                        }, 
                        {
                            "answer": "SPRAT", 
                            "clue": "Insignificant person", 
                            "format": "5", 
                            "length": 5, 
                            "number": "43", 
                            "word": 23
                        }, 
                        {
                            "answer": "LIGHTSW", 
                            "clue": "Juice maker?", 
                            "format": "7", 
                            "length": 7, 
                            "number": "44", 
                            "word": 24
                        }, 
                        {
                            "answer": "ITCH", 
                            "clue": "Long", 
                            "format": "4", 
                            "length": 4, 
                            "number": "46", 
                            "word": 25
                        }, 
                        {
                            "answer": "BRULEE", 
                            "clue": "Creme ___ (custard dessert)", 
                            "format": "6", 
                            "length": 6, 
                            "number": "47", 
                            "word": 26
                        }, 
                        {
                            "answer": "ACLU", 
                            "clue": "\"Because freedom can't protect itself\" org.", 
                            "format": "4", 
                            "length": 4, 
                            "number": "49", 
                            "word": 27
                        }, 
                        {
                            "answer": "ARISEN", 
                            "clue": "Up", 
                            "format": "6", 
                            "length": 6, 
                            "number": "51", 
                            "word": 28
                        }, 
                        {
                            "answer": "INDOORS", 
                            "clue": "Suffering from cabin fever, say", 
                            "format": "7", 
                            "length": 7, 
                            "number": "52", 
                            "word": 29
                        }, 
                        {
                            "answer": "SIGH", 
                            "clue": "Long", 
                            "format": "4", 
                            "length": 4, 
                            "number": "55", 
                            "word": 30
                        }, 
                        {
                            "answer": "TSEEINGBUS", 
                            "clue": "Many shots are taken from it", 
                            "format": "10", 
                            "length": 10, 
                            "number": "56", 
                            "word": 31
                        }, 
                        {
                            "answer": "ABIE", 
                            "clue": "\"___ Baby\" (\"Hair\" song)", 
                            "format": "4", 
                            "length": 4, 
                            "number": "59", 
                            "word": 32
                        }, 
                        {
                            "answer": "SERENE", 
                            "clue": "Unruffled", 
                            "format": "6", 
                            "length": 6, 
                            "number": "60", 
                            "word": 33
                        }, 
                        {
                            "answer": "EMU", 
                            "clue": "Hatchling from a blue-green egg", 
                            "format": "3", 
                            "length": 3, 
                            "number": "61", 
                            "word": 34
                        }, 
                        {
                            "answer": "PEDS", 
                            "clue": "Users of some Xings", 
                            "format": "4", 
                            "length": 4, 
                            "number": "62", 
                            "word": 35
                        }, 
                        {
                            "answer": "WEDGY", 
                            "clue": "Resembling a DeLorean", 
                            "format": "5", 
                            "length": 5, 
                            "number": "63", 
                            "word": 36
                        }, 
                        {
                            "answer": "DON", 
                            "clue": "With 7-Down, National Radio Hall of Fame inductee of 1989", 
                            "format": "3", 
                            "length": 3, 
                            "number": "64", 
                            "word": 37
                        }
                    ], 
                    "name": "Description", 
                    "title": "Across"
                }, 
                {
                    "clues": [
                        {
                            "answer": "TSP", 
                            "clue": "Sixth of a fl. oz.", 
                            "format": "3", 
                            "length": 3, 
                            "number": "1", 
                            "word": 38
                        }, 
                        {
                            "answer": "UTEP", 
                            "clue": "Home of the Lady Miners", 
                            "format": "4", 
                            "length": 4, 
                            "number": "2", 
                            "word": 39
                        }, 
                        {
                            "answer": "GUNRANGES", 
                            "clue": "Many shots are taken at them", 
                            "format": "9", 
                            "length": 9, 
                            "number": "3", 
                            "word": 40
                        }, 
                        {
                            "answer": "TWICE", 
                            "clue": "Cautious way to look", 
                            "format": "5", 
                            "length": 5, 
                            "number": "4", 
                            "word": 41
                        }, 
                        {
                            "answer": "HELENA", 
                            "clue": "Capital in Lewis and Clark County", 
                            "format": "6", 
                            "length": 6, 
                            "number": "5", 
                            "word": 42
                        }, 
                        {
                            "answer": "ATMS", 
                            "clue": "Mall conveniences", 
                            "format": "4", 
                            "length": 4, 
                            "number": "6", 
                            "word": 43
                        }, 
                        {
                            "answer": "IMUS", 
                            "clue": "See 64-Across", 
                            "format": "4", 
                            "length": 4, 
                            "number": "7", 
                            "word": 44
                        }, 
                        {
                            "answer": "SOS", 
                            "clue": "Sinking signal", 
                            "format": "3", 
                            "length": 3, 
                            "number": "8", 
                            "word": 45
                        }, 
                        {
                            "answer": "STATED", 
                            "clue": "Clearly specified", 
                            "format": "6", 
                            "length": 6, 
                            "number": "9", 
                            "word": 46
                        }, 
                        {
                            "answer": "PACTS", 
                            "clue": "Diplomatic accomplishments", 
                            "format": "5", 
                            "length": 5, 
                            "number": "10", 
                            "word": 47
                        }, 
                        {
                            "answer": "ASHOT", 
                            "clue": "Comparable to hell?", 
                            "format": "5", 
                            "length": 5, 
                            "number": "11", 
                            "word": 48
                        }, 
                        {
                            "answer": "TEEM", 
                            "clue": "Be rife", 
                            "format": "4", 
                            "length": 4, 
                            "number": "12", 
                            "word": 49
                        }, 
                        {
                            "answer": "PTBOAT", 
                            "clue": "109, famously", 
                            "format": "6", 
                            "length": 6, 
                            "number": "15", 
                            "word": 50
                        }, 
                        {
                            "answer": "COMEAT", 
                            "clue": "Charge", 
                            "format": "6", 
                            "length": 6, 
                            "number": "18", 
                            "word": 51
                        }, 
                        {
                            "answer": "OWNERS", 
                            "clue": "They have titles", 
                            "format": "6", 
                            "length": 6, 
                            "number": "22", 
                            "word": 52
                        }, 
                        {
                            "answer": "GLADYS", 
                            "clue": "Knight from Georgia", 
                            "format": "6", 
                            "length": 6, 
                            "number": "24", 
                            "word": 53
                        }, 
                        {
                            "answer": "PECS", 
                            "clue": "Bench presser's pride", 
                            "format": "4", 
                            "length": 4, 
                            "number": "25", 
                            "word": 54
                        }, 
                        {
                            "answer": "IDLY", 
                            "clue": "How doodles are drawn", 
                            "format": "4", 
                            "length": 4, 
                            "number": "26", 
                            "word": 55
                        }, 
                        {
                            "answer": "LOATHE", 
                            "clue": "Can't stand", 
                            "format": "6", 
                            "length": 6, 
                            "number": "28", 
                            "word": 56
                        }, 
                        {
                            "answer": "GRIST", 
                            "clue": "Grain for grinding", 
                            "format": "5", 
                            "length": 5, 
                            "number": "29", 
                            "word": 57
                        }, 
                        {
                            "answer": "REPILE", 
                            "clue": "Put into a new heap", 
                            "format": "6", 
                            "length": 6, 
                            "number": "31", 
                            "word": 58
                        }, 
                        {
                            "answer": "PERTURBED", 
                            "clue": "Unsettled", 
                            "format": "9", 
                            "length": 9, 
                            "number": "33", 
                            "word": 59
                        }, 
                        {
                            "answer": "EVAC", 
                            "clue": "Emergency copter trip", 
                            "format": "4", 
                            "length": 4, 
                            "number": "34", 
                            "word": 60
                        }, 
                        {
                            "answer": "NETH", 
                            "clue": "Belg. neighbor", 
                            "format": "4", 
                            "length": 4, 
                            "number": "35", 
                            "word": 61
                        }, 
                        {
                            "answer": "EPILOG", 
                            "clue": "Literary coda", 
                            "format": "6", 
                            "length": 6, 
                            "number": "39", 
                            "word": 62
                        }, 
                        {
                            "answer": "AGENTS", 
                            "clue": "Negotiation pros", 
                            "format": "6", 
                            "length": 6, 
                            "number": "42", 
                            "word": 63
                        }, 
                        {
                            "answer": "LUSHES", 
                            "clue": "Boozehounds", 
                            "format": "6", 
                            "length": 6, 
                            "number": "44", 
                            "word": 64
                        }, 
                        {
                            "answer": "WADING", 
                            "clue": "Ibis activity", 
                            "format": "6", 
                            "length": 6, 
                            "number": "45", 
                            "word": 65
                        }, 
                        {
                            "answer": "BRIBE", 
                            "clue": "Buy off", 
                            "format": "5", 
                            "length": 5, 
                            "number": "47", 
                            "word": 66
                        }, 
                        {
                            "answer": "RIGID", 
                            "clue": "Far from flexible", 
                            "format": "5", 
                            "length": 5, 
                            "number": "48", 
                            "word": 67
                        }, 
                        {
                            "answer": "CONEY", 
                            "clue": "New York's ___ Island", 
                            "format": "5", 
                            "length": 5, 
                            "number": "50", 
                            "word": 68
                        }, 
                        {
                            "answer": "ASAP", 
                            "clue": "Indication of urgency", 
                            "format": "4", 
                            "length": 4, 
                            "number": "51", 
                            "word": 69
                        }, 
                        {
                            "answer": "IERE", 
                            "clue": "\"Able was ___...\"", 
                            "format": "4", 
                            "length": 4, 
                            "number": "52", 
                            "word": 70
                        }, 
                        {
                            "answer": "NEED", 
                            "clue": "Scholarship factor", 
                            "format": "4", 
                            "length": 4, 
                            "number": "53", 
                            "word": 71
                        }, 
                        {
                            "answer": "SUMO", 
                            "clue": "Sport whose highest rank is yokozuna", 
                            "format": "4", 
                            "length": 4, 
                            "number": "54", 
                            "word": 72
                        }, 
                        {
                            "answer": "SEW", 
                            "clue": "Hem, say", 
                            "format": "3", 
                            "length": 3, 
                            "number": "57", 
                            "word": 73
                        }, 
                        {
                            "answer": "SUN", 
                            "clue": "Taiwanese flag feature", 
                            "format": "3", 
                            "length": 3, 
                            "number": "58", 
                            "word": 74
                        }
                    ], 
                    "name": "Description", 
                    "title": "Down"
                }
            ], 
            "correctsolutionmessagetext": "Well done!", 
            "crosswordtype": "American", 
            "date-publish": "Thursday, 16 February 2017", 
            "date-publish-analytics": "2017/02/16 00:00 thursday", 
            "date-publish-email": "16 February 2017", 
            "date-release": "2017-02-13 21:00:00", 
            "date-solution": "", 
            "description": "", 
            "gridsize": {
                "cols": "15", 
                "rows": "15", 
                "type": "Standard"
            }, 
            "hints": {
                "Ask A Friend": "0", 
                "Mark Errors": "0", 
                "Solve Letter": "0", 
                "Solve Word": "0"
            }, 
            "id": "22332", 
            "previoussolutionlink": "http://nu-puzzles-prod-s3.s3-website-eu-west-1.amazonaws.com/partners/WSJ/puzzles/crossword/20170215/22331/", 
            "previoussolutiontext": "Previous crossword solution", 
            "publisher": "The Wall Street Journal", 
            "setter": "By Alice Long/Edited by Mike Shenk", 
            "settings": {
                "solution": "TUG THAIS  SPATSTU WETMOP TASEPENCILMUST ACHE PROCESS BOTTOM  AMEN  GOWEST PINE ALGLAND   EDGAR ORATE PENCLETE AID REEVESYS PATSY SPRAT   LIGHTSW ITCH BRULEE  ACLU  ARISEN INDOORS SIGH TSEEINGBUSABIE SERENE EMUPEDS  WEDGY DON", 
                "solution_hashed": "8d4f39f9b31439382e8cc689cfc2f0ca"
            }, 
            "title": "Long Jumps", 
            "type": "block", 
            "words": [
                {
                    "id": 1, 
                    "solution": "TUG", 
                    "x": "1-3", 
                    "y": "1"
                }, 
                {
                    "id": 2, 
                    "solution": "THAIS", 
                    "x": "5-9", 
                    "y": "1"
                }, 
                {
                    "id": 3, 
                    "solution": "SPAT", 
                    "x": "12-15", 
                    "y": "1"
                }, 
                {
                    "id": 4, 
                    "solution": "STU", 
                    "x": "1-3", 
                    "y": "2"
                }, 
                {
                    "id": 5, 
                    "solution": "WETMOP", 
                    "x": "5-10", 
                    "y": "2"
                }, 
                {
                    "id": 6, 
                    "solution": "TASE", 
                    "x": "12-15", 
                    "y": "2"
                }, 
                {
                    "id": 7, 
                    "solution": "PENCILMUST", 
                    "x": "1-10", 
                    "y": "3"
                }, 
                {
                    "id": 8, 
                    "solution": "ACHE", 
                    "x": "12-15", 
                    "y": "3"
                }, 
                {
                    "id": 9, 
                    "solution": "PROCESS", 
                    "x": "2-8", 
                    "y": "4"
                }, 
                {
                    "id": 10, 
                    "solution": "BOTTOM", 
                    "x": "10-15", 
                    "y": "4"
                }, 
                {
                    "id": 11, 
                    "solution": "AMEN", 
                    "x": "3-6", 
                    "y": "5"
                }, 
                {
                    "id": 12, 
                    "solution": "GOWEST", 
                    "x": "9-14", 
                    "y": "5"
                }, 
                {
                    "id": 13, 
                    "solution": "PINE", 
                    "x": "1-4", 
                    "y": "6"
                }, 
                {
                    "id": 14, 
                    "solution": "ALGLAND", 
                    "x": "6-12", 
                    "y": "6"
                }, 
                {
                    "id": 15, 
                    "solution": "EDGAR", 
                    "x": "1-5", 
                    "y": "7"
                }, 
                {
                    "id": 16, 
                    "solution": "ORATE", 
                    "x": "7-11", 
                    "y": "7"
                }, 
                {
                    "id": 17, 
                    "solution": "PEN", 
                    "x": "13-15", 
                    "y": "7"
                }, 
                {
                    "id": 18, 
                    "solution": "CLETE", 
                    "x": "1-5", 
                    "y": "8"
                }, 
                {
                    "id": 19, 
                    "solution": "AID", 
                    "x": "7-9", 
                    "y": "8"
                }, 
                {
                    "id": 20, 
                    "solution": "REEVE", 
                    "x": "11-15", 
                    "y": "8"
                }, 
                {
                    "id": 21, 
                    "solution": "SYS", 
                    "x": "1-3", 
                    "y": "9"
                }, 
                {
                    "id": 22, 
                    "solution": "PATSY", 
                    "x": "5-9", 
                    "y": "9"
                }, 
                {
                    "id": 23, 
                    "solution": "SPRAT", 
                    "x": "11-15", 
                    "y": "9"
                }, 
                {
                    "id": 24, 
                    "solution": "LIGHTSW", 
                    "x": "4-10", 
                    "y": "10"
                }, 
                {
                    "id": 25, 
                    "solution": "ITCH", 
                    "x": "12-15", 
                    "y": "10"
                }, 
                {
                    "id": 26, 
                    "solution": "BRULEE", 
                    "x": "2-7", 
                    "y": "11"
                }, 
                {
                    "id": 27, 
                    "solution": "ACLU", 
                    "x": "10-13", 
                    "y": "11"
                }, 
                {
                    "id": 28, 
                    "solution": "ARISEN", 
                    "x": "1-6", 
                    "y": "12"
                }, 
                {
                    "id": 29, 
                    "solution": "INDOORS", 
                    "x": "8-14", 
                    "y": "12"
                }, 
                {
                    "id": 30, 
                    "solution": "SIGH", 
                    "x": "1-4", 
                    "y": "13"
                }, 
                {
                    "id": 31, 
                    "solution": "TSEEINGBUS", 
                    "x": "6-15", 
                    "y": "13"
                }, 
                {
                    "id": 32, 
                    "solution": "ABIE", 
                    "x": "1-4", 
                    "y": "14"
                }, 
                {
                    "id": 33, 
                    "solution": "SERENE", 
                    "x": "6-11", 
                    "y": "14"
                }, 
                {
                    "id": 34, 
                    "solution": "EMU", 
                    "x": "13-15", 
                    "y": "14"
                }, 
                {
                    "id": 35, 
                    "solution": "PEDS", 
                    "x": "1-4", 
                    "y": "15"
                }, 
                {
                    "id": 36, 
                    "solution": "WEDGY", 
                    "x": "7-11", 
                    "y": "15"
                }, 
                {
                    "id": 37, 
                    "solution": "DON", 
                    "x": "13-15", 
                    "y": "15"
                }, 
                {
                    "id": 38, 
                    "solution": "TSP", 
                    "x": "1", 
                    "y": "1-3"
                }, 
                {
                    "id": 39, 
                    "solution": "UTEP", 
                    "x": "2", 
                    "y": "1-4"
                }, 
                {
                    "id": 40, 
                    "solution": "GUNRANGES", 
                    "x": "3", 
                    "y": "1-9"
                }, 
                {
                    "id": 41, 
                    "solution": "TWICE", 
                    "x": "5", 
                    "y": "1-5"
                }, 
                {
                    "id": 42, 
                    "solution": "HELENA", 
                    "x": "6", 
                    "y": "1-6"
                }, 
                {
                    "id": 43, 
                    "solution": "ATMS", 
                    "x": "7", 
                    "y": "1-4"
                }, 
                {
                    "id": 44, 
                    "solution": "IMUS", 
                    "x": "8", 
                    "y": "1-4"
                }, 
                {
                    "id": 45, 
                    "solution": "SOS", 
                    "x": "9", 
                    "y": "1-3"
                }, 
                {
                    "id": 46, 
                    "solution": "STATED", 
                    "x": "12", 
                    "y": "1-6"
                }, 
                {
                    "id": 47, 
                    "solution": "PACTS", 
                    "x": "13", 
                    "y": "1-5"
                }, 
                {
                    "id": 48, 
                    "solution": "ASHOT", 
                    "x": "14", 
                    "y": "1-5"
                }, 
                {
                    "id": 49, 
                    "solution": "TEEM", 
                    "x": "15", 
                    "y": "1-4"
                }, 
                {
                    "id": 50, 
                    "solution": "PTBOAT", 
                    "x": "10", 
                    "y": "2-7"
                }, 
                {
                    "id": 51, 
                    "solution": "COMEAT", 
                    "x": "4", 
                    "y": "3-8"
                }, 
                {
                    "id": 52, 
                    "solution": "OWNERS", 
                    "x": "11", 
                    "y": "4-9"
                }, 
                {
                    "id": 53, 
                    "solution": "GLADYS", 
                    "x": "9", 
                    "y": "5-10"
                }, 
                {
                    "id": 54, 
                    "solution": "PECS", 
                    "x": "1", 
                    "y": "6-9"
                }, 
                {
                    "id": 55, 
                    "solution": "IDLY", 
                    "x": "2", 
                    "y": "6-9"
                }, 
                {
                    "id": 56, 
                    "solution": "LOATHE", 
                    "x": "7", 
                    "y": "6-11"
                }, 
                {
                    "id": 57, 
                    "solution": "GRIST", 
                    "x": "8", 
                    "y": "6-10"
                }, 
                {
                    "id": 58, 
                    "solution": "REPILE", 
                    "x": "5", 
                    "y": "7-12"
                }, 
                {
                    "id": 59, 
                    "solution": "PERTURBED", 
                    "x": "13", 
                    "y": "7-15"
                }, 
                {
                    "id": 60, 
                    "solution": "EVAC", 
                    "x": "14", 
                    "y": "7-10"
                }, 
                {
                    "id": 61, 
                    "solution": "NETH", 
                    "x": "15", 
                    "y": "7-10"
                }, 
                {
                    "id": 62, 
                    "solution": "EPILOG", 
                    "x": "12", 
                    "y": "8-13"
                }, 
                {
                    "id": 63, 
                    "solution": "AGENTS", 
                    "x": "6", 
                    "y": "9-14"
                }, 
                {
                    "id": 64, 
                    "solution": "LUSHES", 
                    "x": "4", 
                    "y": "10-15"
                }, 
                {
                    "id": 65, 
                    "solution": "WADING", 
                    "x": "10", 
                    "y": "10-15"
                }, 
                {
                    "id": 66, 
                    "solution": "BRIBE", 
                    "x": "2", 
                    "y": "11-15"
                }, 
                {
                    "id": 67, 
                    "solution": "RIGID", 
                    "x": "3", 
                    "y": "11-15"
                }, 
                {
                    "id": 68, 
                    "solution": "CONEY", 
                    "x": "11", 
                    "y": "11-15"
                }, 
                {
                    "id": 69, 
                    "solution": "ASAP", 
                    "x": "1", 
                    "y": "12-15"
                }, 
                {
                    "id": 70, 
                    "solution": "IERE", 
                    "x": "8", 
                    "y": "12-15"
                }, 
                {
                    "id": 71, 
                    "solution": "NEED", 
                    "x": "9", 
                    "y": "12-15"
                }, 
                {
                    "id": 72, 
                    "solution": "SUMO", 
                    "x": "14", 
                    "y": "12-15"
                }, 
                {
                    "id": 73, 
                    "solution": "SEW", 
                    "x": "7", 
                    "y": "13-15"
                }, 
                {
                    "id": 74, 
                    "solution": "SUN", 
                    "x": "15", 
                    "y": "13-15"
                }
            ]
        }, 
        "created": "2017-02-15 18:47:58", 
        "grid": [
            [
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "1", 
                    "SquareID": 1, 
                    "WordAcrossID": 1, 
                    "WordDownID": 38
                }, 
                {
                    "Blank": "", 
                    "Letter": "U", 
                    "Number": "2", 
                    "SquareID": 2, 
                    "WordAcrossID": 1, 
                    "WordDownID": 39
                }, 
                {
                    "Blank": "", 
                    "Letter": "G", 
                    "Number": "3", 
                    "SquareID": 3, 
                    "WordAcrossID": 1, 
                    "WordDownID": 40
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 4, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "4", 
                    "SquareID": 5, 
                    "WordAcrossID": 2, 
                    "WordDownID": 41
                }, 
                {
                    "Blank": "", 
                    "Letter": "H", 
                    "Number": "5", 
                    "SquareID": 6, 
                    "WordAcrossID": 2, 
                    "WordDownID": 42
                }, 
                {
                    "Blank": "", 
                    "Letter": "A", 
                    "Number": "6", 
                    "SquareID": 7, 
                    "WordAcrossID": 2, 
                    "WordDownID": 43
                }, 
                {
                    "Blank": "", 
                    "Letter": "I", 
                    "Number": "7", 
                    "SquareID": 8, 
                    "WordAcrossID": 2, 
                    "WordDownID": 44
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "8", 
                    "SquareID": 9, 
                    "WordAcrossID": 2, 
                    "WordDownID": 45
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 10, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 11, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "9", 
                    "SquareID": 12, 
                    "WordAcrossID": 3, 
                    "WordDownID": 46
                }, 
                {
                    "Blank": "", 
                    "Letter": "P", 
                    "Number": "10", 
                    "SquareID": 13, 
                    "WordAcrossID": 3, 
                    "WordDownID": 47
                }, 
                {
                    "Blank": "", 
                    "Letter": "A", 
                    "Number": "11", 
                    "SquareID": 14, 
                    "WordAcrossID": 3, 
                    "WordDownID": 48
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "12", 
                    "SquareID": 15, 
                    "WordAcrossID": 3, 
                    "WordDownID": 49
                }
            ], 
            [
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "13", 
                    "SquareID": 16, 
                    "WordAcrossID": 4, 
                    "WordDownID": 38
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "", 
                    "SquareID": 17, 
                    "WordAcrossID": 4, 
                    "WordDownID": 39
                }, 
                {
                    "Blank": "", 
                    "Letter": "U", 
                    "Number": "", 
                    "SquareID": 18, 
                    "WordAcrossID": 4, 
                    "WordDownID": 40
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 19, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "W", 
                    "Number": "14", 
                    "SquareID": 20, 
                    "WordAcrossID": 5, 
                    "WordDownID": 41
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 21, 
                    "WordAcrossID": 5, 
                    "WordDownID": 42
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "", 
                    "SquareID": 22, 
                    "WordAcrossID": 5, 
                    "WordDownID": 43
                }, 
                {
                    "Blank": "", 
                    "Letter": "M", 
                    "Number": "", 
                    "SquareID": 23, 
                    "WordAcrossID": 5, 
                    "WordDownID": 44
                }, 
                {
                    "Blank": "", 
                    "Letter": "O", 
                    "Number": "", 
                    "SquareID": 24, 
                    "WordAcrossID": 5, 
                    "WordDownID": 45
                }, 
                {
                    "Blank": "", 
                    "Letter": "P", 
                    "Number": "15", 
                    "SquareID": 25, 
                    "WordAcrossID": 5, 
                    "WordDownID": 50
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 26, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "16", 
                    "SquareID": 27, 
                    "WordAcrossID": 6, 
                    "WordDownID": 46
                }, 
                {
                    "Blank": "", 
                    "Letter": "A", 
                    "Number": "", 
                    "SquareID": 28, 
                    "WordAcrossID": 6, 
                    "WordDownID": 47
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "", 
                    "SquareID": 29, 
                    "WordAcrossID": 6, 
                    "WordDownID": 48
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 30, 
                    "WordAcrossID": 6, 
                    "WordDownID": 49
                }
            ], 
            [
                {
                    "Blank": "", 
                    "Letter": "P", 
                    "Number": "17", 
                    "SquareID": 31, 
                    "WordAcrossID": 7, 
                    "WordDownID": 38
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 32, 
                    "WordAcrossID": 7, 
                    "WordDownID": 39
                }, 
                {
                    "Blank": "", 
                    "Letter": "N", 
                    "Number": "", 
                    "SquareID": 33, 
                    "WordAcrossID": 7, 
                    "WordDownID": 40
                }, 
                {
                    "Blank": "", 
                    "Letter": "C", 
                    "Number": "18", 
                    "SquareID": 34, 
                    "WordAcrossID": 7, 
                    "WordDownID": 51
                }, 
                {
                    "Blank": "", 
                    "Letter": "I", 
                    "Number": "", 
                    "SquareID": 35, 
                    "WordAcrossID": 7, 
                    "WordDownID": 41
                }, 
                {
                    "Blank": "", 
                    "Letter": "L", 
                    "Number": "", 
                    "SquareID": 36, 
                    "WordAcrossID": 7, 
                    "WordDownID": 42
                }, 
                {
                    "Blank": "", 
                    "Letter": "M", 
                    "Number": "", 
                    "SquareID": 37, 
                    "WordAcrossID": 7, 
                    "WordDownID": 43
                }, 
                {
                    "Blank": "", 
                    "Letter": "U", 
                    "Number": "", 
                    "SquareID": 38, 
                    "WordAcrossID": 7, 
                    "WordDownID": 44
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "", 
                    "SquareID": 39, 
                    "WordAcrossID": 7, 
                    "WordDownID": 45
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "", 
                    "SquareID": 40, 
                    "WordAcrossID": 7, 
                    "WordDownID": 50
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 41, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "A", 
                    "Number": "19", 
                    "SquareID": 42, 
                    "WordAcrossID": 8, 
                    "WordDownID": 46
                }, 
                {
                    "Blank": "", 
                    "Letter": "C", 
                    "Number": "", 
                    "SquareID": 43, 
                    "WordAcrossID": 8, 
                    "WordDownID": 47
                }, 
                {
                    "Blank": "", 
                    "Letter": "H", 
                    "Number": "", 
                    "SquareID": 44, 
                    "WordAcrossID": 8, 
                    "WordDownID": 48
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 45, 
                    "WordAcrossID": 8, 
                    "WordDownID": 49
                }
            ], 
            [
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 46, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "P", 
                    "Number": "20", 
                    "SquareID": 47, 
                    "WordAcrossID": 9, 
                    "WordDownID": 39
                }, 
                {
                    "Blank": "", 
                    "Letter": "R", 
                    "Number": "", 
                    "SquareID": 48, 
                    "WordAcrossID": 9, 
                    "WordDownID": 40
                }, 
                {
                    "Blank": "", 
                    "Letter": "O", 
                    "Number": "", 
                    "SquareID": 49, 
                    "WordAcrossID": 9, 
                    "WordDownID": 51
                }, 
                {
                    "Blank": "", 
                    "Letter": "C", 
                    "Number": "", 
                    "SquareID": 50, 
                    "WordAcrossID": 9, 
                    "WordDownID": 41
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 51, 
                    "WordAcrossID": 9, 
                    "WordDownID": 42
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "", 
                    "SquareID": 52, 
                    "WordAcrossID": 9, 
                    "WordDownID": 43
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "", 
                    "SquareID": 53, 
                    "WordAcrossID": 9, 
                    "WordDownID": 44
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 54, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "B", 
                    "Number": "21", 
                    "SquareID": 55, 
                    "WordAcrossID": 10, 
                    "WordDownID": 50
                }, 
                {
                    "Blank": "", 
                    "Letter": "O", 
                    "Number": "22", 
                    "SquareID": 56, 
                    "WordAcrossID": 10, 
                    "WordDownID": 52
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "", 
                    "SquareID": 57, 
                    "WordAcrossID": 10, 
                    "WordDownID": 46
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "", 
                    "SquareID": 58, 
                    "WordAcrossID": 10, 
                    "WordDownID": 47
                }, 
                {
                    "Blank": "", 
                    "Letter": "O", 
                    "Number": "", 
                    "SquareID": 59, 
                    "WordAcrossID": 10, 
                    "WordDownID": 48
                }, 
                {
                    "Blank": "", 
                    "Letter": "M", 
                    "Number": "", 
                    "SquareID": 60, 
                    "WordAcrossID": 10, 
                    "WordDownID": 49
                }
            ], 
            [
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 61, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 62, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "A", 
                    "Number": "23", 
                    "SquareID": 63, 
                    "WordAcrossID": 11, 
                    "WordDownID": 40
                }, 
                {
                    "Blank": "", 
                    "Letter": "M", 
                    "Number": "", 
                    "SquareID": 64, 
                    "WordAcrossID": 11, 
                    "WordDownID": 51
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 65, 
                    "WordAcrossID": 11, 
                    "WordDownID": 41
                }, 
                {
                    "Blank": "", 
                    "Letter": "N", 
                    "Number": "", 
                    "SquareID": 66, 
                    "WordAcrossID": 11, 
                    "WordDownID": 42
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 67, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 68, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "G", 
                    "Number": "24", 
                    "SquareID": 69, 
                    "WordAcrossID": 12, 
                    "WordDownID": 53
                }, 
                {
                    "Blank": "", 
                    "Letter": "O", 
                    "Number": "", 
                    "SquareID": 70, 
                    "WordAcrossID": 12, 
                    "WordDownID": 50
                }, 
                {
                    "Blank": "", 
                    "Letter": "W", 
                    "Number": "", 
                    "SquareID": 71, 
                    "WordAcrossID": 12, 
                    "WordDownID": 52
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 72, 
                    "WordAcrossID": 12, 
                    "WordDownID": 46
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "", 
                    "SquareID": 73, 
                    "WordAcrossID": 12, 
                    "WordDownID": 47
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "", 
                    "SquareID": 74, 
                    "WordAcrossID": 12, 
                    "WordDownID": 48
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 75, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }
            ], 
            [
                {
                    "Blank": "", 
                    "Letter": "P", 
                    "Number": "25", 
                    "SquareID": 76, 
                    "WordAcrossID": 13, 
                    "WordDownID": 54
                }, 
                {
                    "Blank": "", 
                    "Letter": "I", 
                    "Number": "26", 
                    "SquareID": 77, 
                    "WordAcrossID": 13, 
                    "WordDownID": 55
                }, 
                {
                    "Blank": "", 
                    "Letter": "N", 
                    "Number": "", 
                    "SquareID": 78, 
                    "WordAcrossID": 13, 
                    "WordDownID": 40
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 79, 
                    "WordAcrossID": 13, 
                    "WordDownID": 51
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 80, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "A", 
                    "Number": "27", 
                    "SquareID": 81, 
                    "WordAcrossID": 14, 
                    "WordDownID": 42
                }, 
                {
                    "Blank": "", 
                    "Letter": "L", 
                    "Number": "28", 
                    "SquareID": 82, 
                    "WordAcrossID": 14, 
                    "WordDownID": 56
                }, 
                {
                    "Blank": "", 
                    "Letter": "G", 
                    "Number": "29", 
                    "SquareID": 83, 
                    "WordAcrossID": 14, 
                    "WordDownID": 57
                }, 
                {
                    "Blank": "", 
                    "Letter": "L", 
                    "Number": "", 
                    "SquareID": 84, 
                    "WordAcrossID": 14, 
                    "WordDownID": 53
                }, 
                {
                    "Blank": "", 
                    "Letter": "A", 
                    "Number": "", 
                    "SquareID": 85, 
                    "WordAcrossID": 14, 
                    "WordDownID": 50
                }, 
                {
                    "Blank": "", 
                    "Letter": "N", 
                    "Number": "", 
                    "SquareID": 86, 
                    "WordAcrossID": 14, 
                    "WordDownID": 52
                }, 
                {
                    "Blank": "", 
                    "Letter": "D", 
                    "Number": "", 
                    "SquareID": 87, 
                    "WordAcrossID": 14, 
                    "WordDownID": 46
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 88, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 89, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 90, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }
            ], 
            [
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "30", 
                    "SquareID": 91, 
                    "WordAcrossID": 15, 
                    "WordDownID": 54
                }, 
                {
                    "Blank": "", 
                    "Letter": "D", 
                    "Number": "", 
                    "SquareID": 92, 
                    "WordAcrossID": 15, 
                    "WordDownID": 55
                }, 
                {
                    "Blank": "", 
                    "Letter": "G", 
                    "Number": "", 
                    "SquareID": 93, 
                    "WordAcrossID": 15, 
                    "WordDownID": 40
                }, 
                {
                    "Blank": "", 
                    "Letter": "A", 
                    "Number": "", 
                    "SquareID": 94, 
                    "WordAcrossID": 15, 
                    "WordDownID": 51
                }, 
                {
                    "Blank": "", 
                    "Letter": "R", 
                    "Number": "31", 
                    "SquareID": 95, 
                    "WordAcrossID": 15, 
                    "WordDownID": 58
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 96, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "O", 
                    "Number": "32", 
                    "SquareID": 97, 
                    "WordAcrossID": 16, 
                    "WordDownID": 56
                }, 
                {
                    "Blank": "", 
                    "Letter": "R", 
                    "Number": "", 
                    "SquareID": 98, 
                    "WordAcrossID": 16, 
                    "WordDownID": 57
                }, 
                {
                    "Blank": "", 
                    "Letter": "A", 
                    "Number": "", 
                    "SquareID": 99, 
                    "WordAcrossID": 16, 
                    "WordDownID": 53
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "", 
                    "SquareID": 100, 
                    "WordAcrossID": 16, 
                    "WordDownID": 50
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 101, 
                    "WordAcrossID": 16, 
                    "WordDownID": 52
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 102, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "P", 
                    "Number": "33", 
                    "SquareID": 103, 
                    "WordAcrossID": 17, 
                    "WordDownID": 59
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "34", 
                    "SquareID": 104, 
                    "WordAcrossID": 17, 
                    "WordDownID": 60
                }, 
                {
                    "Blank": "", 
                    "Letter": "N", 
                    "Number": "35", 
                    "SquareID": 105, 
                    "WordAcrossID": 17, 
                    "WordDownID": 61
                }
            ], 
            [
                {
                    "Blank": "", 
                    "Letter": "C", 
                    "Number": "36", 
                    "SquareID": 106, 
                    "WordAcrossID": 18, 
                    "WordDownID": 54
                }, 
                {
                    "Blank": "", 
                    "Letter": "L", 
                    "Number": "", 
                    "SquareID": 107, 
                    "WordAcrossID": 18, 
                    "WordDownID": 55
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 108, 
                    "WordAcrossID": 18, 
                    "WordDownID": 40
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "", 
                    "SquareID": 109, 
                    "WordAcrossID": 18, 
                    "WordDownID": 51
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 110, 
                    "WordAcrossID": 18, 
                    "WordDownID": 58
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 111, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "A", 
                    "Number": "37", 
                    "SquareID": 112, 
                    "WordAcrossID": 19, 
                    "WordDownID": 56
                }, 
                {
                    "Blank": "", 
                    "Letter": "I", 
                    "Number": "", 
                    "SquareID": 113, 
                    "WordAcrossID": 19, 
                    "WordDownID": 57
                }, 
                {
                    "Blank": "", 
                    "Letter": "D", 
                    "Number": "", 
                    "SquareID": 114, 
                    "WordAcrossID": 19, 
                    "WordDownID": 53
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 115, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "R", 
                    "Number": "38", 
                    "SquareID": 116, 
                    "WordAcrossID": 20, 
                    "WordDownID": 52
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "39", 
                    "SquareID": 117, 
                    "WordAcrossID": 20, 
                    "WordDownID": 62
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 118, 
                    "WordAcrossID": 20, 
                    "WordDownID": 59
                }, 
                {
                    "Blank": "", 
                    "Letter": "V", 
                    "Number": "", 
                    "SquareID": 119, 
                    "WordAcrossID": 20, 
                    "WordDownID": 60
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 120, 
                    "WordAcrossID": 20, 
                    "WordDownID": 61
                }
            ], 
            [
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "40", 
                    "SquareID": 121, 
                    "WordAcrossID": 21, 
                    "WordDownID": 54
                }, 
                {
                    "Blank": "", 
                    "Letter": "Y", 
                    "Number": "", 
                    "SquareID": 122, 
                    "WordAcrossID": 21, 
                    "WordDownID": 55
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "", 
                    "SquareID": 123, 
                    "WordAcrossID": 21, 
                    "WordDownID": 40
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 124, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "P", 
                    "Number": "41", 
                    "SquareID": 125, 
                    "WordAcrossID": 22, 
                    "WordDownID": 58
                }, 
                {
                    "Blank": "", 
                    "Letter": "A", 
                    "Number": "42", 
                    "SquareID": 126, 
                    "WordAcrossID": 22, 
                    "WordDownID": 63
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "", 
                    "SquareID": 127, 
                    "WordAcrossID": 22, 
                    "WordDownID": 56
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "", 
                    "SquareID": 128, 
                    "WordAcrossID": 22, 
                    "WordDownID": 57
                }, 
                {
                    "Blank": "", 
                    "Letter": "Y", 
                    "Number": "", 
                    "SquareID": 129, 
                    "WordAcrossID": 22, 
                    "WordDownID": 53
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 130, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "43", 
                    "SquareID": 131, 
                    "WordAcrossID": 23, 
                    "WordDownID": 52
                }, 
                {
                    "Blank": "", 
                    "Letter": "P", 
                    "Number": "", 
                    "SquareID": 132, 
                    "WordAcrossID": 23, 
                    "WordDownID": 62
                }, 
                {
                    "Blank": "", 
                    "Letter": "R", 
                    "Number": "", 
                    "SquareID": 133, 
                    "WordAcrossID": 23, 
                    "WordDownID": 59
                }, 
                {
                    "Blank": "", 
                    "Letter": "A", 
                    "Number": "", 
                    "SquareID": 134, 
                    "WordAcrossID": 23, 
                    "WordDownID": 60
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "", 
                    "SquareID": 135, 
                    "WordAcrossID": 23, 
                    "WordDownID": 61
                }
            ], 
            [
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 136, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 137, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 138, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "L", 
                    "Number": "44", 
                    "SquareID": 139, 
                    "WordAcrossID": 24, 
                    "WordDownID": 64
                }, 
                {
                    "Blank": "", 
                    "Letter": "I", 
                    "Number": "", 
                    "SquareID": 140, 
                    "WordAcrossID": 24, 
                    "WordDownID": 58
                }, 
                {
                    "Blank": "", 
                    "Letter": "G", 
                    "Number": "", 
                    "SquareID": 141, 
                    "WordAcrossID": 24, 
                    "WordDownID": 63
                }, 
                {
                    "Blank": "", 
                    "Letter": "H", 
                    "Number": "", 
                    "SquareID": 142, 
                    "WordAcrossID": 24, 
                    "WordDownID": 56
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "", 
                    "SquareID": 143, 
                    "WordAcrossID": 24, 
                    "WordDownID": 57
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "", 
                    "SquareID": 144, 
                    "WordAcrossID": 24, 
                    "WordDownID": 53
                }, 
                {
                    "Blank": "", 
                    "Letter": "W", 
                    "Number": "45", 
                    "SquareID": 145, 
                    "WordAcrossID": 24, 
                    "WordDownID": 65
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 146, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "I", 
                    "Number": "46", 
                    "SquareID": 147, 
                    "WordAcrossID": 25, 
                    "WordDownID": 62
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "", 
                    "SquareID": 148, 
                    "WordAcrossID": 25, 
                    "WordDownID": 59
                }, 
                {
                    "Blank": "", 
                    "Letter": "C", 
                    "Number": "", 
                    "SquareID": 149, 
                    "WordAcrossID": 25, 
                    "WordDownID": 60
                }, 
                {
                    "Blank": "", 
                    "Letter": "H", 
                    "Number": "", 
                    "SquareID": 150, 
                    "WordAcrossID": 25, 
                    "WordDownID": 61
                }
            ], 
            [
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 151, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "B", 
                    "Number": "47", 
                    "SquareID": 152, 
                    "WordAcrossID": 26, 
                    "WordDownID": 66
                }, 
                {
                    "Blank": "", 
                    "Letter": "R", 
                    "Number": "48", 
                    "SquareID": 153, 
                    "WordAcrossID": 26, 
                    "WordDownID": 67
                }, 
                {
                    "Blank": "", 
                    "Letter": "U", 
                    "Number": "", 
                    "SquareID": 154, 
                    "WordAcrossID": 26, 
                    "WordDownID": 64
                }, 
                {
                    "Blank": "", 
                    "Letter": "L", 
                    "Number": "", 
                    "SquareID": 155, 
                    "WordAcrossID": 26, 
                    "WordDownID": 58
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 156, 
                    "WordAcrossID": 26, 
                    "WordDownID": 63
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 157, 
                    "WordAcrossID": 26, 
                    "WordDownID": 56
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 158, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 159, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "A", 
                    "Number": "49", 
                    "SquareID": 160, 
                    "WordAcrossID": 27, 
                    "WordDownID": 65
                }, 
                {
                    "Blank": "", 
                    "Letter": "C", 
                    "Number": "50", 
                    "SquareID": 161, 
                    "WordAcrossID": 27, 
                    "WordDownID": 68
                }, 
                {
                    "Blank": "", 
                    "Letter": "L", 
                    "Number": "", 
                    "SquareID": 162, 
                    "WordAcrossID": 27, 
                    "WordDownID": 62
                }, 
                {
                    "Blank": "", 
                    "Letter": "U", 
                    "Number": "", 
                    "SquareID": 163, 
                    "WordAcrossID": 27, 
                    "WordDownID": 59
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 164, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 165, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }
            ], 
            [
                {
                    "Blank": "", 
                    "Letter": "A", 
                    "Number": "51", 
                    "SquareID": 166, 
                    "WordAcrossID": 28, 
                    "WordDownID": 69
                }, 
                {
                    "Blank": "", 
                    "Letter": "R", 
                    "Number": "", 
                    "SquareID": 167, 
                    "WordAcrossID": 28, 
                    "WordDownID": 66
                }, 
                {
                    "Blank": "", 
                    "Letter": "I", 
                    "Number": "", 
                    "SquareID": 168, 
                    "WordAcrossID": 28, 
                    "WordDownID": 67
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "", 
                    "SquareID": 169, 
                    "WordAcrossID": 28, 
                    "WordDownID": 64
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 170, 
                    "WordAcrossID": 28, 
                    "WordDownID": 58
                }, 
                {
                    "Blank": "", 
                    "Letter": "N", 
                    "Number": "", 
                    "SquareID": 171, 
                    "WordAcrossID": 28, 
                    "WordDownID": 63
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 172, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "I", 
                    "Number": "52", 
                    "SquareID": 173, 
                    "WordAcrossID": 29, 
                    "WordDownID": 70
                }, 
                {
                    "Blank": "", 
                    "Letter": "N", 
                    "Number": "53", 
                    "SquareID": 174, 
                    "WordAcrossID": 29, 
                    "WordDownID": 71
                }, 
                {
                    "Blank": "", 
                    "Letter": "D", 
                    "Number": "", 
                    "SquareID": 175, 
                    "WordAcrossID": 29, 
                    "WordDownID": 65
                }, 
                {
                    "Blank": "", 
                    "Letter": "O", 
                    "Number": "", 
                    "SquareID": 176, 
                    "WordAcrossID": 29, 
                    "WordDownID": 68
                }, 
                {
                    "Blank": "", 
                    "Letter": "O", 
                    "Number": "", 
                    "SquareID": 177, 
                    "WordAcrossID": 29, 
                    "WordDownID": 62
                }, 
                {
                    "Blank": "", 
                    "Letter": "R", 
                    "Number": "", 
                    "SquareID": 178, 
                    "WordAcrossID": 29, 
                    "WordDownID": 59
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "54", 
                    "SquareID": 179, 
                    "WordAcrossID": 29, 
                    "WordDownID": 72
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 180, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }
            ], 
            [
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "55", 
                    "SquareID": 181, 
                    "WordAcrossID": 30, 
                    "WordDownID": 69
                }, 
                {
                    "Blank": "", 
                    "Letter": "I", 
                    "Number": "", 
                    "SquareID": 182, 
                    "WordAcrossID": 30, 
                    "WordDownID": 66
                }, 
                {
                    "Blank": "", 
                    "Letter": "G", 
                    "Number": "", 
                    "SquareID": 183, 
                    "WordAcrossID": 30, 
                    "WordDownID": 67
                }, 
                {
                    "Blank": "", 
                    "Letter": "H", 
                    "Number": "", 
                    "SquareID": 184, 
                    "WordAcrossID": 30, 
                    "WordDownID": 64
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 185, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "T", 
                    "Number": "56", 
                    "SquareID": 186, 
                    "WordAcrossID": 31, 
                    "WordDownID": 63
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "57", 
                    "SquareID": 187, 
                    "WordAcrossID": 31, 
                    "WordDownID": 73
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 188, 
                    "WordAcrossID": 31, 
                    "WordDownID": 70
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 189, 
                    "WordAcrossID": 31, 
                    "WordDownID": 71
                }, 
                {
                    "Blank": "", 
                    "Letter": "I", 
                    "Number": "", 
                    "SquareID": 190, 
                    "WordAcrossID": 31, 
                    "WordDownID": 65
                }, 
                {
                    "Blank": "", 
                    "Letter": "N", 
                    "Number": "", 
                    "SquareID": 191, 
                    "WordAcrossID": 31, 
                    "WordDownID": 68
                }, 
                {
                    "Blank": "", 
                    "Letter": "G", 
                    "Number": "", 
                    "SquareID": 192, 
                    "WordAcrossID": 31, 
                    "WordDownID": 62
                }, 
                {
                    "Blank": "", 
                    "Letter": "B", 
                    "Number": "", 
                    "SquareID": 193, 
                    "WordAcrossID": 31, 
                    "WordDownID": 59
                }, 
                {
                    "Blank": "", 
                    "Letter": "U", 
                    "Number": "", 
                    "SquareID": 194, 
                    "WordAcrossID": 31, 
                    "WordDownID": 72
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "58", 
                    "SquareID": 195, 
                    "WordAcrossID": 31, 
                    "WordDownID": 74
                }
            ], 
            [
                {
                    "Blank": "", 
                    "Letter": "A", 
                    "Number": "59", 
                    "SquareID": 196, 
                    "WordAcrossID": 32, 
                    "WordDownID": 69
                }, 
                {
                    "Blank": "", 
                    "Letter": "B", 
                    "Number": "", 
                    "SquareID": 197, 
                    "WordAcrossID": 32, 
                    "WordDownID": 66
                }, 
                {
                    "Blank": "", 
                    "Letter": "I", 
                    "Number": "", 
                    "SquareID": 198, 
                    "WordAcrossID": 32, 
                    "WordDownID": 67
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 199, 
                    "WordAcrossID": 32, 
                    "WordDownID": 64
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 200, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "60", 
                    "SquareID": 201, 
                    "WordAcrossID": 33, 
                    "WordDownID": 63
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 202, 
                    "WordAcrossID": 33, 
                    "WordDownID": 73
                }, 
                {
                    "Blank": "", 
                    "Letter": "R", 
                    "Number": "", 
                    "SquareID": 203, 
                    "WordAcrossID": 33, 
                    "WordDownID": 70
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 204, 
                    "WordAcrossID": 33, 
                    "WordDownID": 71
                }, 
                {
                    "Blank": "", 
                    "Letter": "N", 
                    "Number": "", 
                    "SquareID": 205, 
                    "WordAcrossID": 33, 
                    "WordDownID": 65
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 206, 
                    "WordAcrossID": 33, 
                    "WordDownID": 68
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 207, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "61", 
                    "SquareID": 208, 
                    "WordAcrossID": 34, 
                    "WordDownID": 59
                }, 
                {
                    "Blank": "", 
                    "Letter": "M", 
                    "Number": "", 
                    "SquareID": 209, 
                    "WordAcrossID": 34, 
                    "WordDownID": 72
                }, 
                {
                    "Blank": "", 
                    "Letter": "U", 
                    "Number": "", 
                    "SquareID": 210, 
                    "WordAcrossID": 34, 
                    "WordDownID": 74
                }
            ], 
            [
                {
                    "Blank": "", 
                    "Letter": "P", 
                    "Number": "62", 
                    "SquareID": 211, 
                    "WordAcrossID": 35, 
                    "WordDownID": 69
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 212, 
                    "WordAcrossID": 35, 
                    "WordDownID": 66
                }, 
                {
                    "Blank": "", 
                    "Letter": "D", 
                    "Number": "", 
                    "SquareID": 213, 
                    "WordAcrossID": 35, 
                    "WordDownID": 67
                }, 
                {
                    "Blank": "", 
                    "Letter": "S", 
                    "Number": "", 
                    "SquareID": 214, 
                    "WordAcrossID": 35, 
                    "WordDownID": 64
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 215, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 216, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "W", 
                    "Number": "63", 
                    "SquareID": 217, 
                    "WordAcrossID": 36, 
                    "WordDownID": 73
                }, 
                {
                    "Blank": "", 
                    "Letter": "E", 
                    "Number": "", 
                    "SquareID": 218, 
                    "WordAcrossID": 36, 
                    "WordDownID": 70
                }, 
                {
                    "Blank": "", 
                    "Letter": "D", 
                    "Number": "", 
                    "SquareID": 219, 
                    "WordAcrossID": 36, 
                    "WordDownID": 71
                }, 
                {
                    "Blank": "", 
                    "Letter": "G", 
                    "Number": "", 
                    "SquareID": 220, 
                    "WordAcrossID": 36, 
                    "WordDownID": 65
                }, 
                {
                    "Blank": "", 
                    "Letter": "Y", 
                    "Number": "", 
                    "SquareID": 221, 
                    "WordAcrossID": 36, 
                    "WordDownID": 68
                }, 
                {
                    "Blank": "blank", 
                    "Letter": "", 
                    "Number": "", 
                    "SquareID": 222, 
                    "WordAcrossID": "", 
                    "WordDownID": ""
                }, 
                {
                    "Blank": "", 
                    "Letter": "D", 
                    "Number": "64", 
                    "SquareID": 223, 
                    "WordAcrossID": 37, 
                    "WordDownID": 59
                }, 
                {
                    "Blank": "", 
                    "Letter": "O", 
                    "Number": "", 
                    "SquareID": 224, 
                    "WordAcrossID": 37, 
                    "WordDownID": 72
                }, 
                {
                    "Blank": "", 
                    "Letter": "N", 
                    "Number": "", 
                    "SquareID": 225, 
                    "WordAcrossID": 37, 
                    "WordDownID": 74
                }
            ]
        ], 
        "headline": "Long Jumps", 
        "meta": {
            "pdf": "crossword-20170216-22332.pdf", 
            "print_index": "jumbo-print.html"
        }, 
        "options": [], 
        "type": "games"
    }
}
*/
