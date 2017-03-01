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


public class UCLickReader extends DefaultHandler 
{
	private boolean bAcross = false;
	private boolean bDown = false;
	private boolean bClues = false;
	
	private CrosswordInfo mInfo = null;
	
	private int mWidth, mHeight;
	private int numClues = 0;
	
	private String mChars = new String();
	
	private int mCurClueNum = 0;
	
	private Map<Integer, String> acrossClues = new HashMap<Integer, String>();
	private Map<Integer, String> downClues = new HashMap<Integer, String>();
	
	public UCLickReader(CrosswordInfo info, File f)
	{
		mInfo = info;
		readFromFile(f);
	}
	
	public UCLickReader(CrosswordInfo info, String strSource)
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
		else if (strElement.equalsIgnoreCase("span") || strElement.equalsIgnoreCase("i"))
		{
			return;
		}
		else if (strElement.equalsIgnoreCase("clues"))
		{
			bClues = false;
			bDown = false;
			bAcross = false;
		}
		else if (strElement.equalsIgnoreCase("clue"))
		{
			if (bAcross)
			{
				if (mCurClueNum > numClues)
					numClues = mCurClueNum;
				
				try
				{
					acrossClues.put(mCurClueNum, URLDecoder.decode(mChars, "UTF-8"));
					if (AndWords.DEBUG)
						Log.v("ANDWORDS", "Chars: " + mChars);
				}
				catch (Exception e)
				{
					acrossClues.put(mCurClueNum, mChars);
				}
			}
			else
			{
				if (mCurClueNum > numClues)
					numClues = mCurClueNum;
				
				try
				{
					downClues.put(mCurClueNum, URLDecoder.decode(mChars, "UTF-8"));
					if (AndWords.DEBUG)
						Log.v("ANDWORDS", "Chars: " + mChars);
				}
				catch (Exception e)
				{
					downClues.put(mCurClueNum, mChars);
					if (AndWords.DEBUG)
						Log.v("ANDWORDS", "Chars: " + mChars);
				}
				
			}
		}
		else if (strElement.equalsIgnoreCase("title"))
		{
			if (bClues)
			{
				if (mChars != null && mChars.length() != 0 && mChars.trim().length() != 0)
				{
					if (mChars.toString().equalsIgnoreCase("Down"))
						bDown = true;
					else if (mChars.toString().equalsIgnoreCase("Across"))
						bAcross = true;
				}
			}
			else if (mChars != null && mChars.length() != 0 && mChars.trim().length() != 0)
				mInfo.strTitle = mChars;
		}
		else if (strElement.equalsIgnoreCase("creator"))
		{
			if (mChars != null && mChars.length() != 0 && mChars.trim().length() != 0)
				mInfo.strAuthor = mChars;
		}
		else if (strElement.equalsIgnoreCase("copyright"))
		{
			if (mChars != null && mChars.length() != 0 && mChars.trim().length() != 0)
			{
				try
				{
					mInfo.strCopyright = URLDecoder.decode(mChars, "UTF-8");
				}
				catch (Exception e)
				{
					mInfo.strCopyright = mChars;
				}
			}
		}
		else if (strElement.equalsIgnoreCase("b"))
		{
			if (mChars.equalsIgnoreCase("Across"))
			{
				bAcross = true;
				bDown = false;
			}
			else if (mChars.equalsIgnoreCase("Down"))
			{
				bAcross = false;
				bDown = true;
			}
		}
		else if (strElement.equalsIgnoreCase("down"))
		{
			bDown = false;
		}
		else if (strElement.equalsIgnoreCase("crossword"))
		{
			mInfo.strAcrossClues = new String[numClues + 1];
			mInfo.strDownClues = new String[numClues + 1];
			mInfo.mLastCellNumber = numClues;
			
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
			
			mInfo.mPuzzleType = AndWords.UCLICK;
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
		
		if (strElement.equals("clue"))
		{
			try
			{
				mCurClueNum = Integer.parseInt(attributes.getValue("number"));
			}
			catch (Exception e)
			{
				return;
			}
		}
		else if (strElement.equals("clues"))
		{
			bClues = true;
		}
		else if (strElement.equals("b") || strElement.equals("span") || strElement.equals("i"))
		{
			// nothing to do
		}
		else if (strElement.equalsIgnoreCase("grid"))
		{
			mHeight = Integer.parseInt(attributes.getValue("height"));
			mInfo.nHeight = mHeight;
			
			mWidth = Integer.parseInt(attributes.getValue("width"));
			mInfo.nWidth = mWidth;
			
			mInfo.mSolution = new char[mHeight][mWidth];
			mInfo.mDiagram = new char[mHeight][mWidth];
		}
		else if (strElement.equalsIgnoreCase("cell"))
		{
			int x, y;
			
			try
			{
				x = Integer.parseInt(attributes.getValue("x"));
				y = Integer.parseInt(attributes.getValue("y"));
			}
			catch (Exception e)
			{
				x = 0;
				y = 0;
			}
			if (attributes.getValue("type") != null)
			{
				mInfo.mSolution[y - 1][x - 1] = '.';
				mInfo.mDiagram[y - 1][x - 1] = '.';
			}
			else
			{
				mInfo.mSolution[y - 1][x - 1] = Character.toUpperCase(attributes.getValue("solution").charAt(0));
				mInfo.mDiagram[y - 1][x - 1] = ' ';
			}
			
			try
			{
				String strCircle = attributes.getValue("background-shape");
				
				if (strCircle != null && strCircle.equalsIgnoreCase("circle"))
				{
					if (mInfo.mCircledSquares == null)
						mInfo.mCircledSquares = new char[mInfo.nHeight][mInfo.nWidth];
					
					mInfo.mCircledSquares[y - 1][x - 1] = 1;
				}
			}
			catch (Exception e)
			{
				
			}
		}
		else if (strElement.equalsIgnoreCase("title"))
		{
			if (bClues != true)
				mInfo.strTitle = attributes.getValue("v");
		}
		else if (strElement.equalsIgnoreCase("author"))
		{
			mInfo.strAuthor = attributes.getValue("v");
		}
		else if (strElement.equalsIgnoreCase("width"))
		{
			mWidth = Integer.parseInt(attributes.getValue("v"));
			mInfo.nWidth = mWidth;
		}
		else if (strElement.equalsIgnoreCase("height"))
		{
			mHeight = Integer.parseInt(attributes.getValue("v"));
			mInfo.nHeight = mHeight;
		}
		else if (strElement.equalsIgnoreCase("allanswer"))
		{
			mInfo.mSolution = new char[mHeight][mWidth];
			mInfo.mDiagram = new char[mHeight][mWidth];
			
			char[][] solution = mInfo.mSolution;
			char[][] diagram = mInfo.mDiagram;
			
			String strSolution = attributes.getValue("v");
			strSolution.toUpperCase(Locale.US);
			
			int i, j;
			int spot = 0;
			
			for (i = 0; i < mHeight; i++)
			{
				for (j = 0; j < mWidth; j++)
				{
					// I denote blank squares by a '.', Uclick uses a '-', convert it
					solution[i][j] = strSolution.charAt(spot++);
					if (solution[i][j] == '-')
						solution[i][j] = '.';
					
					if (solution[i][j] == '.')
						diagram[i][j] = '.';
					else
						diagram[i][j] = ' ';
				}
			}
		}
		else if (strElement.equalsIgnoreCase("userinput"))
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
		else if (bDown)
		{
			int clueNumber;
			
			try
			{
				clueNumber = Integer.parseInt(attributes.getValue("cn"));
			}
			catch (Exception e)
			{
				clueNumber = 0;
			}
			
			if (clueNumber > numClues)
				numClues = clueNumber;
			
			try
			{
				downClues.put(clueNumber, URLDecoder.decode(attributes.getValue("c"), "UTF-8"));
			}
			catch (Exception e)
			{
				downClues.put(clueNumber, attributes.getValue("c"));
			}
		}
		else if (bAcross)
		{
			int clueNumber = 0;
			
			if (strElement.equalsIgnoreCase("clue"))
			{
				try
				{
					clueNumber = Integer.parseInt(attributes.getValue("number"));
				}
				catch (Exception e)
				{
					clueNumber = 0;
				}
				
				mCurClueNum = clueNumber;
				return;
			}
			else
			{
				try
				{
					clueNumber = Integer.parseInt(attributes.getValue("cn"));
				}
				catch (Exception e)
				{
					clueNumber = 0;
				}
			}
			
			if (clueNumber > numClues)
				numClues = clueNumber;
			
			try
			{
				acrossClues.put(clueNumber, URLDecoder.decode(attributes.getValue("c"), "UTF-8"));
			}
			catch (Exception e)
			{
				acrossClues.put(clueNumber, attributes.getValue("c"));
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
				else if (strLine.contains("</crossword"))
				{
					String strOut;
					
					int index = strLine.indexOf("</crossword");
					
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
						
						strOut = "\"/></crossword>";
						os.write(strOut.getBytes());
						//os.write(10);
					}
					else
					{
						strOut = "</crossword>";
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
