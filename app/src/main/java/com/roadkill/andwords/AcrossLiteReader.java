package com.roadkill.andwords;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import android.util.Log;


public class AcrossLiteReader 
{
	private static int GEXT_CIRCLED_SQUARE = 0x80;
	
	public static void read(CrosswordInfo info, String strSource, HttpURLConnection uc, String userName, String password)
	{
		try
		{
		//	System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
			if (uc == null)
			{
				URL u = new URL(strSource);
				
				while (true)
				{
					if (userName != null && !userName.equalsIgnoreCase(""))
					{
						URL login = new URL("https://myaccount.nytimes.com/auth/login?URI=http://www.nytimes.com/crosswords/archive/index.html");
						HttpURLConnection loginUC = (HttpURLConnection) login.openConnection();
						loginUC.setRequestMethod("GET");
						loginUC.setRequestProperty ("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:35.0) Gecko/20100101 Firefox/35.0");
						loginUC.setRequestProperty ("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9");
						loginUC.setRequestProperty ("Accept-Language", "en-us,en;q=0.5");
						loginUC.setRequestProperty ("Accept-Charset",  "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
						//loginUC.setRequestProperty ("Accept-Encoding", "gzip;q=0,deflate,sdch");
						loginUC.connect();
						
						InputStream is = loginUC.getInputStream();
						
						String strResponse = new String();
						
						byte[] b = new byte[2048]; 
						
						while (is.read(b) != -1)
						{
							String s = new String(b);
							strResponse += s;
						}
						

				        String content = "is_continue=true&SAVEOPTION=YES&URI=http://select.nytimes.com/premium/xword/puzzles.html&OQ=&OP=&userid=";
				        content += URLEncoder.encode(userName, "UTF-8");
				        content += "&password=";
				        content += URLEncoder.encode(password, "UTF-8");
				        
		                int tokenIndex = strResponse.indexOf("name=\"token\" value=\"");
		                int expiresIndex = strResponse.indexOf("name=\"expires\" value=\"");

		                content += "&token=" + strResponse.substring(tokenIndex + 20, strResponse.indexOf("\"", tokenIndex + 20));
		                content += "&expires=" + strResponse.substring(expiresIndex + 22, strResponse.indexOf("\"", expiresIndex + 22));
		                
						loginUC.disconnect();
						
						loginUC = (HttpURLConnection) login.openConnection();
						loginUC.setRequestMethod("POST");
						loginUC.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
						loginUC.setDoOutput(true);
						loginUC.connect();
						DataOutputStream out = new DataOutputStream(loginUC.getOutputStream());
						out.writeBytes(content);
						out.flush();
						
						if (loginUC.getResponseCode() != 302)
						{
							info.mErrorString = "Unable to log in to NYT website";
							return;
						}
						
						String cookies = new String();
						String headerName=null;
						for (int i=1; (headerName = loginUC.getHeaderFieldKey(i))!=null; i++) 
						{
						 	if (headerName.equalsIgnoreCase("Set-Cookie")) 
						 	{                  
						 		String cookie = loginUC.getHeaderField(i);
						 		cookies += cookie.substring(0, cookie.indexOf(";"));
						 		cookies += ";";
						 	}
						}
						 		
						out.close();
						loginUC.disconnect();
						
						uc = (HttpURLConnection) u.openConnection();
						uc.addRequestProperty("Cookie", cookies);
						uc.setRequestProperty ("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:35.0) Gecko/20100101 Firefox/35.0");
						uc.setRequestProperty ("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
						uc.setRequestProperty ("Accept-Language", "en-us,en;q=0.5");
						uc.setRequestProperty ("Referer", "http://www.nytimes.com/crosswords/archive/index.html");
					}
					else
					{
						uc = (HttpURLConnection) u.openConnection();
						uc.setRequestProperty ("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:35.0) Gecko/20100101 Firefox/35.0");
						uc.setRequestProperty ("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
						uc.setRequestProperty ("Accept-Language", "en-us,en;q=0.5");
					}

					uc.connect();
					
					switch (uc.getResponseCode())
					{
					case HttpURLConnection.HTTP_MOVED_PERM:
					case HttpURLConnection.HTTP_MOVED_TEMP:
						u = new URL(uc.getHeaderField("Location"));
						continue;
					}
					
					break;
				}
				
				if (uc.getResponseCode() != HttpURLConnection.HTTP_OK)
				{
					//cantLoadPuzzle(c, strSource);
					info.mLoadComplete = false;
					info.mErrorString = uc.getResponseMessage();
					uc.disconnect();
					return;
				}
			}
			
			InputStream s = uc.getInputStream();
			
			//br = new InputStreamReader(uc.getInputStream(), "ISO-8859-1");
			File f = new File(info.mPuzzlePath);
			f.createNewFile();
			FileOutputStream os = new FileOutputStream(f);
			
			int i;
			byte buf[] = new byte[1024];
			
			while ((i = s.read(buf)) != -1)
				os.write(buf, 0, i);
			
			s.close();
			os.close();
			
			read(info);
		}
		catch (Exception e)
		{
			//cantLoadPuzzle(c, e.getLocalizedMessage());
			
			info.mLoadComplete = false;
			return;
		}
	}
	
	public static void read(CrosswordInfo info)
	{
		try
		{
			InputStreamReader br = new InputStreamReader(new FileInputStream(info.mPuzzlePath), "ISO-8859-1");
			int count = 0;
			
			while (!br.ready())
			{
				if (count > 10)
					break;
				
				Thread.sleep(1000);
				++count;
			}
			
			br.skip(2);
			
			String strMagic = ReaderUtils.readStringFromFile(br);
			
			if (!strMagic.equals("ACROSS&DOWN"))
			{
				br.close();
				return;
			}
			
			// skip checksums in the file
			br.skip(30);
			
			info.nWidth = br.read();
			info.nHeight = br.read();
			
			char[] clues = new char[2];
			
			br.read(clues);
			
			int numClues = (short) ((clues[1] & 0xff) << 8 | ((clues[0] & 0xff)));
			numClues &= 0x00ff;
			
			info.mSolution = new char[info.nHeight][info.nWidth];
			info.mDiagram = new char[info.nHeight][info.nWidth];
			
			br.skip(2);
			
			char[] scrambled = new char[2];
			br.read(scrambled);
			int scram = (short) ((scrambled[1] & 0xff) << 8 | ((scrambled[0] & 0xff)));
			
			info.mScrambled = (scram != 0);
			
			int i = 0;
			int j = 0;
			
			// read in the solution
			for (i = 0; i < info.nHeight; i++)
			{
				char b[] = new char[info.nWidth];
				br.read(b);
				
				for (j = 0; j < info.nWidth; j++)
					info.mSolution[i][j] = Character.toUpperCase((char) b[j]);
			}
			
			// read in the diagram/user input
			boolean bBadChar = false;
			
			for (i = 0; i < info.nHeight; i++)
			{
				char b[] = new char[info.nWidth];
				br.read(b);
				
				for (j = 0; j < info.nWidth; j++)
				{
					info.mDiagram[i][j] = (char) b[j];
					if (info.mDiagram[i][j] == '-')
						info.mDiagram[i][j] = ' ';
					
					if (info.mDiagram[i][j] != info.mSolution[i][j] && info.mSolution[i][j] != '.')
						bBadChar = true;
				}
			}
			
			if (bBadChar == false)
				info.mPuzzleComplete = true;
			
			info.strTitle = ReaderUtils.readStringFromFile(br);
			info.strAuthor = ReaderUtils.readStringFromFile(br);
			info.strCopyright = ReaderUtils.readStringFromFile(br);
			
			String[] strClues = new String[numClues];
			
			for (i = 0; i < numClues; i++)
				strClues[i] = ReaderUtils.readStringFromFile(br);
			
			info.strNotes = ReaderUtils.readStringFromFile(br);
			
			//build the cell number array
			info.mCellNumbers = new int[info.nHeight][info.nWidth];
			
			int cellNumber = 1;
			
			int curClueNum = 0;
			
			
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
							info.strAcrossClues[cellNumber] = strClues[curClueNum++];
						
						if (needsDown)
							info.strDownClues[cellNumber] = strClues[curClueNum++];
						
						cellNumber++;
					}					
				}
			}
				
			boolean keepReading = true;
			
			while (keepReading)
			{
				try
				{
					char[] inp = new char[4];
					if (br.read(inp) < 0)
						break;
					
					if (inp[0] == 'G' && inp[1] == 'E' && inp[2] == 'X' && inp[3] == 'T')
					{
						br.skip(4);  // skip length & checksum
						
						info.mCircledSquares = new char[info.nHeight][info.nWidth];
						                                              
						// read in the diagram
						for (i = 0; i < info.nHeight; i++)
						{
							char b[] = new char[info.nWidth];
							br.read(b);
							
							for (j = 0; j < info.nWidth; j++)
							{
								if ((b[j] & GEXT_CIRCLED_SQUARE) != 0)
									info.mCircledSquares[i][j] = (char) b[j];
							}
						}
					}
					else
					{
						char c1 = (char)br.read();
						char c2 = (char)br.read();
						
						short numBytes = (short) (((c2 & 0xff) << 8) | (c1 & 0xff));
						if (br.skip(numBytes + 2) < 0)
							break;
					}
					
					if (br.skip(1) < 0)
						break;
				}
				catch (Exception e)
				{
					keepReading = false;
				}
			}
			
			br.close();
		}
		catch (Exception e)
		{
			info.mLoadComplete = false;
			return;
		}
		
		info.mPuzzleType = AndWords.ACROSS_LITE;
		info.mLoadComplete = true;
	}
	
	public static boolean save(CrosswordInfo info)
	{
		try
		{
			// read in the puzzle from disk, and add in the user input into the diagram section
			File outFile = new File(AndWords.PUZZLE_PATH, "tmp");
			
			InputStreamReader streamIn = new InputStreamReader(new FileInputStream(info.mPuzzlePath), "ISO-8859-1");
			outFile.createNewFile();
			FileOutputStream os = new FileOutputStream(outFile);
			
			int i = 0;
			int count = 0;
			int diagramSpot = 52 + info.nHeight * info.nWidth;
		
			while ((i = streamIn.read()) != -1)
			{
				if (count == diagramSpot)
				{
					int j,k;
					
					for (j = 0; j < info.nHeight; j++)
					{
						for (k = 0; k < info.nWidth; k++)
						{
							os.write((int)info.mDiagram[j][k]);
						}
					}
					
					streamIn.skip(info.nWidth * info.nHeight - 1);
				}
				else
				{
					os.write(i);
				}
				
				count++;
			}
				
			os.close();
			streamIn.close();
			
			File f = new File(info.mPuzzlePath);
			f.delete();
			
			outFile.renameTo(f);
			
			return true;
		}
		catch (Exception e)
		{
			Log.v("ERROR", "Unable to save puzzle: " + e.getLocalizedMessage());
		}
		
		return false;
	}
}
