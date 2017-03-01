package com.roadkill.andwords;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class OnlinePuzzlesReader extends DefaultHandler
{
	private boolean bAcross = false;
	private boolean bDown = false;
	private boolean bClues = false;
	
	private CrosswordInfo mInfo = null;
	
	private int mWidth = 15, mHeight = 15;
	private int numClues = 0;
	
	private String mChars = new String();
	
	private int mCurClueNum = 0;
	
	private Map<Integer, String> acrossClues = new HashMap<Integer, String>();
	private Map<Integer, String> downClues = new HashMap<Integer, String>();
	
	public OnlinePuzzlesReader(CrosswordInfo info, File f)
	{
		mInfo = info;
		readFromFile(f);
	}
	
	public OnlinePuzzlesReader(CrosswordInfo info, String strSource)
	{
		InputStreamReader br = null;
		
		mInfo = info;

		try
		{
			URL u = new URL(strSource);
			
			HttpURLConnection uc = (HttpURLConnection) u.openConnection();
			
			uc.connect();
			
			if (uc.getResponseCode() != HttpURLConnection.HTTP_OK)
			{
				info.mErrorString = "Invalid HTTP code for URL: " + uc.getResponseCode();
				info.mLoadComplete = false;
				uc.disconnect();
				return;
			}
			
			br = new InputStreamReader(uc.getInputStream(), "UTF-8");
			
			File f = new File(info.mPuzzlePath);
			f.createNewFile();
			FileOutputStream os = new FileOutputStream(f);

			char[] buf = new char[2048];
			int i = br.read(buf, 0, 2048);

			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
			
			while (i != -1)
			{
				int iStart = 0;
				int iEnd = 0;
				
				String strBuf = new String(buf, 0, i);
				strBuf = strBuf.replace("\\", "");
				
				i = strBuf.length();
				iEnd = i;
				
				if (strBuf.startsWith("var CrosswordPuzzleData = "))
				{
					iStart += 27;
				}

				if (strBuf.charAt(i-2) == '"' && strBuf.charAt(i - 1) == ';')
				{
					iEnd = i - 2;
				}
				
				osw.write(strBuf, iStart, iEnd - iStart);
				
				i = br.read(buf, 0, 2048);
			}
			
			br.close();
			osw.close();
			
			readFromFile(f);
		}
		catch (Exception e)
		{
			info.mErrorString = e.getLocalizedMessage();
			info.mLoadComplete = false;
			return;
		}
	}
	
	public void readFromFile(File f)
	{
		try
		{
			readFromStream(new InputStreamReader(new FileInputStream(f), "UTF-8"));
		}
		catch (Exception e)
		{
			Log.v("ERROR", "readFromFile: " + e.getLocalizedMessage());
		}
	}
	
	public void readFromStream(InputStreamReader br)
	{
        SAXParserFactory f = SAXParserFactory.newInstance();
        
        try 
        {
            SAXParser parser = f.newSAXParser();

            XMLReader x = parser.getXMLReader();
            x.setContentHandler(this);
            InputSource is = new InputSource(br);
            is.setEncoding("UTF-8");
            
            x.parse(is);
        } 
        catch (Exception e) 
        {
			mInfo.mErrorString = e.getLocalizedMessage();
			mInfo.mLoadComplete = false;
			Log.v("ERROR", mInfo.mErrorString);
        }

	}

	@Override
	public void endElement(String URI, String shortName, String tagName) throws SAXException 
	{
		shortName = shortName.trim();
		String strElement = shortName.length() == 0 ? tagName.trim() : shortName;
		
		if (AndWords.DEBUG)
			Log.v("ANDWORDS", "End Element: " + strElement);
		
		if (strElement.equalsIgnoreCase("across"))
		{
			bAcross = false;
		}
		else if (strElement.equalsIgnoreCase("down"))
		{
			bDown = false;
		}
		else if (strElement.equalsIgnoreCase("puzzle"))
		{
			mInfo.strAcrossClues = new String[numClues + 1];
			mInfo.strDownClues = new String[numClues + 1];
			mInfo.mLastCellNumber = numClues;
			
			mInfo.nHeight = 15;
			mInfo.nWidth = 15;
			
			String[] strAcrossClues = mInfo.strAcrossClues;
			String[] strDownClues = mInfo.strDownClues;
			
			int i, j;
			
			for (i = 1; i <= numClues; i++)
			{
				if (acrossClues.containsKey(i))
				{
					strAcrossClues[i] = acrossClues.get(i);
				}
				if (downClues.containsKey(i))
				{
					strDownClues[i] = downClues.get(i);
				}
			}
			
			int cellNumber = 1;
			mInfo.mCellNumbers = new int[mHeight][mWidth];
			
			for (i = 0; i < mHeight; i++)
			{
				for (j = 0; j < mWidth; j++)
				{
					if (ReaderUtils.cellNeedsAcrossNumber(mInfo, i, j) || ReaderUtils.cellNeedsDownNumber(mInfo, i, j))
						mInfo.mCellNumbers[i][j] = cellNumber++;
					else
						mInfo.mCellNumbers[i][j] = 0;
				}
			}
			
			mInfo.mPuzzleType = AndWords.ONLINE_CROSSWORDS;
			mInfo.mLoadComplete = true;
		}

		mChars = "";
	}

	@Override
	public void startElement(String URI, String shortName, String tagName, Attributes attributes) throws SAXException 
	{
		shortName = shortName.trim();
		String strElement = shortName.length() == 0 ? tagName.trim() : shortName;

		if (AndWords.DEBUG)
			Log.v("ANDWORDS", "Start Element: " + strElement);
		
		if (strElement.equalsIgnoreCase("userinput"))
		{
			String strInput = attributes.getValue("v");
			char[][] diagram = mInfo.mDiagram;
			
			int i, j;
			
			boolean badChar = false;
			
			for (i = 0; i < mHeight; i++)
			{
				for (j = 0; j < mWidth; j++)
				{
					diagram[i][j] = strInput.charAt(i * mHeight + j);
					
					if (diagram[i][j] != mInfo.mSolution[i][j] && mInfo.mSolution[i][j] != '.')
						badChar = true;
					
				}
			}
			
			if (badChar == false)
				mInfo.mPuzzleComplete = true;
		}
		else if (strElement.equalsIgnoreCase("across"))
		{
			bAcross = true;
		}
		else if (strElement.equalsIgnoreCase("down"))
		{
			bDown = true;
		}
		else if (strElement.equalsIgnoreCase("puzzle"))
		{
			mInfo.mDiagram = new char[15][15];
			mInfo.mSolution = new char[15][15];
			
			for (int i = 0; i < 15; i++)
				for (int j = 0; j < 15; j++)
				{
					mInfo.mDiagram[i][j] = '.';
					mInfo.mSolution[i][j] = '.';
				}
		}
		else if (bDown)
		{
			int clueNumber = Integer.parseInt(attributes.getValue("cn"));
//			int position = Integer.parseInt(attributes.getValue("n"));
			if (clueNumber > numClues)
				numClues = clueNumber;
			
			String strClue;
			
//			String strAnswer;
			
			try
			{
				strClue = URLDecoder.decode(attributes.getValue("c"), "UTF-8");
//				strAnswer = URLDecoder.decode(attributes.getValue("a"), "UTF-8");
				strClue.replace('+', ' ');
				strClue = strClue.replace("&quot;", "\"");
				
				downClues.put(clueNumber, strClue);
			}
			catch (Exception e)
			{
//				strAnswer = attributes.getValue("a");
				strClue = attributes.getValue("c");
				strClue.replace('+', ' ');
				strClue = strClue.replace("&quot;", "\"");
				
				downClues.put(clueNumber, attributes.getValue("c"));
			}
/*			int c = clueNumber / 15;
			int r = clueNumber % 15;
			
			for (int i = 0; i < strAnswer.length(); i++)
			{
				mInfo.mSolution[r + i][c] = strAnswer.charAt(i);
				mInfo.mDiagram[r + i][c] = ' ';
			}*/
		}
		else if (bAcross)
		{
			int clueNumber = 0;
			int position = 0;
			
			clueNumber = Integer.parseInt(attributes.getValue("cn"));
			position = Integer.parseInt(attributes.getValue("n"));
			
			if (clueNumber > numClues)
				numClues = clueNumber;
			
			String strClue;
			String strAnswer;
			
			try
			{
				strAnswer = URLDecoder.decode(attributes.getValue("a"), "UTF-8");
				strClue = URLDecoder.decode(attributes.getValue("c"), "UTF-8");
				strClue.replace('+', ' ');
				strClue = strClue.replace("&quot;", "\"");
				acrossClues.put(clueNumber, strClue);
			}
			catch (Exception e)
			{
				strAnswer = attributes.getValue("a");
				strClue = attributes.getValue("c");
				strClue.replace('+', ' ');
				strClue = strClue.replace("&quot;", "\"");
				acrossClues.put(clueNumber, strClue);
			}
			
			int c = position / 15;
			int r = position % 15;
			
			for (int i = 0; i < strAnswer.length(); i++)
			{
				mInfo.mSolution[c][r + i] = strAnswer.charAt(i);
				mInfo.mDiagram[c][r + i] = ' ';
			}
		}
	}
	
	public static void save(CrosswordInfo info)
	{
		try
		{
			boolean bWroteInput = false;
			
			InputStreamReader br = new InputStreamReader(new FileInputStream(info.mPuzzlePath), "UTF-8");
			
			File outFile = new File(AndWords.PUZZLE_PATH, "tmp");
			outFile.createNewFile();
			FileOutputStream os = new FileOutputStream(outFile);
			
			String strLine = ReaderUtils.readStringFromTextFile(br);
			
			while (strLine != null && !strLine.equals(""))
			{
				if (strLine.contains("<UserInput"))
				{
					int index = strLine.indexOf("<UserInput");
					
					if (index != 0)
					{
						String s = strLine.substring(0, index);
						os.write(s.getBytes());
					}
					
					String strOut = "<UserInput v=\"";
					
					os.write(strOut.getBytes());
					
					int i,j;
					for (i = 0; i < info.nHeight; i++)
					{
						for (j = 0; j < info.nWidth; j++)
						{
							os.write(info.mDiagram[i][j]);
						}
					}
					
					strOut = "\" />";
					os.write(strOut.getBytes());
					//os.write(10);

					index = strLine.indexOf("/>", index + 1);

					if (index < strLine.length() - 2)
						os.write(strLine.substring(index + 2).getBytes());
					
					bWroteInput = true;
				}
				else if (strLine.contains("</puzzle"))
				{
					String strOut;
					
					int index = strLine.indexOf("</puzzle");
					
					// make sure and write out any content before the closing tag
					if (index != 0)
					{
						String s = strLine.substring(0, index);
						os.write(s.getBytes());
					}
					
					if (bWroteInput == false)
					{
						strOut = "<UserInput v=\"";
						
						os.write(strOut.getBytes());
						
						int i,j;
						for (i = 0; i < info.nHeight; i++)
						{
							for (j = 0; j < info.nWidth; j++)
							{
								os.write(info.mDiagram[i][j]);
							}
						}
						
						strOut = "\"/></puzzle>";
						os.write(strOut.getBytes());
						//os.write(10);
					}
					else
					{
						strOut = "</puzzle>";
						os.write(strOut.getBytes());
						//os.write(10);
					}
					
					if (index + 12 < strLine.length())
						os.write(strLine.substring(index + 12).getBytes());
				}
				else
				{
					os.write(strLine.getBytes());
					//os.write(10);
				}
				
				strLine = ReaderUtils.readStringFromTextFile(br);
			}
			br.close();
			os.close();
			
			File f = new File(info.mPuzzlePath);
			f.delete();
			outFile.renameTo(f);
		}
		catch (Exception e)
		{
			return;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException 
	{
		super.characters(ch, start, length);
		try
		{
			mChars += URLDecoder.decode(String.valueOf(ch, start, length), "UTF-8");
		}
		catch (Exception e)
		{
			mChars = String.valueOf(ch, start, length);
		}
	}

}
