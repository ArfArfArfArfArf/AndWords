package com.roadkill.andwords;

import java.io.InputStreamReader;

public class ReaderUtils 
{
	private static final int BUFFER_SIZE = 4096;
	
	public static String readStringFromTextFile(InputStreamReader br)
	{
		return readStringFromTextFile(br, false);
	}
	
	public static String readStringFromTextFile(InputStreamReader br, boolean bCarriageReturn)
	{
		StringBuffer b = new StringBuffer(BUFFER_SIZE);
		
		int c;
		int count = 0;
		
		try
		{
			while (count < BUFFER_SIZE && (c = br.read()) != '\n')
			{
				if (bCarriageReturn && c == '\r')
					return b.toString();
				
				if (c == -1)
					return b.toString();
				
				b.append((char)c);
				count++;
			}
		}
		catch (Exception e)
		{
		}
		
		return b.toString();
	}
	
	public static String readStringFromFile(InputStreamReader br)
	{
		StringBuffer b = new StringBuffer(BUFFER_SIZE);
		
		int c;
		int count = 0;
		
		try
		{
			while ((c = br.read()) != 0 && count < BUFFER_SIZE)
			{
				if (c == -1)
					break;
				
				b.append((char)c);
				++count;
			}
		}
		catch (Exception e)
		{
		}
		
		return b.toString();
	}
	
	public static boolean cellNeedsAcrossNumber(CrosswordInfo i, int height, int width)
	{
		if (i.mDiagram[height][width] == '.')
			return false;
		
		if (width == 0 || i.mDiagram[height][width - 1] == '.')
		{
			if (width + 1 < i.nWidth && i.mDiagram[height][width + 1] != '.')
				return true;
		}
			
		return false;
	}
	
	public static boolean cellNeedsDownNumber(CrosswordInfo i, int height, int width)
	{
		if (i.mDiagram[height][width] == '.')
			return false;
		
		if (height == 0 || i.mDiagram[height - 1][width] == '.')
		{
			if (height + 1 < i.nHeight && i.mDiagram[height + 1][width] != '.')
				return true;
		}
		
		return false;
	}
	

}
