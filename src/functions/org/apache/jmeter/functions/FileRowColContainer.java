// $Header$
/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
*/

package org.apache.jmeter.functions;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.jmeter.junit.JMeterTestCase;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * File data container for CSV (and similar delimited) files
 * Data is accessible via row and column number
 *  
 * @author sebb AT apache DOT org (multiple file version)
 * @version $Revision$
 */
public class FileRowColContainer
{
    
    transient private static Logger log = LoggingManager.getLoggerForClass();
    

    private ArrayList fileData; // Lines in the file, split into columns

    private String fileName; // name of the file
    
    public static final String DELIMITER = ","; // Default delimiter
    
    /** Keeping track of which row is next to be read. */
    private int nextRow;

    /** Delimiter for this file */
	private String delimiter;

    private FileRowColContainer()// Not intended to be called directly
    {
    }

	public FileRowColContainer(String file,String delim)
	throws IOException,FileNotFoundException
	{
		log.debug("FDC("+file+","+delim+")");
		fileName = file;
		delimiter = delim;
		nextRow = 0;
		load();
	}

	public FileRowColContainer(String file)
	throws IOException,FileNotFoundException
	{
		log.debug("FDC("+file+")");
		fileName = file;
		delimiter = DELIMITER;
		nextRow = 0;
		load();
	}


	private void load() 
	throws IOException,FileNotFoundException
	{
		fileData = new ArrayList();

		BufferedReader myBread=null;
		try
		{
			FileReader fis = new FileReader(fileName);
			myBread = new BufferedReader(fis);
			String line = myBread.readLine();
			/* N.B. Stop reading the file if we get a blank line:
			 * This allows for trailing comments in the file
			 */
			while (line != null && line.length() > 0)
			{
				fileData.add(splitLine(line,delimiter));
				line = myBread.readLine();
			}
		} 
		catch (FileNotFoundException e)
        {
			fileData = null;
        	log.warn(e.toString());
        	throw e;
        } 
        catch (IOException e)
        {
        	fileData = null;
			log.warn(e.toString());
			myBread.close();
            throw e;
        }
	}

    /**
     * Get the string for the column from the current row
     * 
     * @param row row number (from 0)
     * @param col column number (from 0)
     * @return the string (empty if out of bounds)
     * @throws IndexOutOfBoundsException if the column number is out of bounds
     */
    public String getColumn(int row,int col) throws IndexOutOfBoundsException
    {
    	String colData;
		colData = (String) ((ArrayList) fileData.get(row)).get(col);
    	log.debug(fileName+"("+row+","+col+"): "+colData);
    	return colData;
    }
    
    /**
     * Returns the next row to the caller, and updates it,
     * allowing for wrap round
     * 
     * @return the first free (unread) row
     * 
     */
    public int nextRow()
    {
    	int row = nextRow;
        nextRow++;
        if (nextRow >= fileData.size())// 0-based
        {
            nextRow = 0;
        }
		log.debug ("Row: "+ row);
		return row;
    }


    /**
     * Splits the line according to the specified delimiter
     * 
     * @return an ArrayList of Strings containing one element for each
     *          value in the line
     */
    private static ArrayList splitLine(String theLine,String delim)
    {
        ArrayList result = new ArrayList();
        StringTokenizer tokener = new StringTokenizer(theLine,delim);
        while(tokener.hasMoreTokens())
        {
            String token = tokener.nextToken();
            result.add(token);
        }
        return result;
    }
    public static class Test extends JMeterTestCase
    {

		static{
//			LoggingManager.setPriority("DEBUG","jmeter");
//			LoggingManager.setTarget(new PrintWriter(System.out));
		}


    	public Test(String a)
    	{
    		super(a);
    	}
    	
    	public void testNull() throws Exception
    	{
    		try
    		{
    			new FileRowColContainer("testfiles/xyzxyz");
    			fail("Should not find the file");
    		}
    		catch (FileNotFoundException e)
    		{
    		}
    	}
    	
		public void testrowNum() throws Exception
		{
			FileRowColContainer f = new FileRowColContainer("testfiles/test.csv");
			assertNotNull(f);
			assertEquals("Expected 4 lines",4,f.fileData.size());

			int myRow=f.nextRow();
			assertEquals(0,myRow);
			assertEquals(1,f.nextRow);

			myRow = f.nextRow();
			assertEquals(1,myRow);
			assertEquals(2,f.nextRow);

			myRow = f.nextRow();
			assertEquals(2,myRow);
			assertEquals(3,f.nextRow);

			myRow = f.nextRow();
			assertEquals(3,myRow);
			assertEquals(0,f.nextRow);
			
			myRow = f.nextRow();
			assertEquals(0,myRow);
			assertEquals(1,f.nextRow);

		}
		
		public void testColumns() throws Exception
		{
			FileRowColContainer f = new FileRowColContainer("testfiles/test.csv");
			assertNotNull(f);
			assertTrue("Not empty",f.fileData.size() > 0);

			int myRow=f.nextRow();
			assertEquals(0,myRow);
			assertEquals("a1",f.getColumn(myRow,0));
			assertEquals("d1",f.getColumn(myRow,3));

			try {
				f.getColumn(myRow,4);
				fail("Expected out of bounds");
			}
			catch (IndexOutOfBoundsException e)
			{
			}
			myRow=f.nextRow();
			assertEquals(1,myRow);
			assertEquals("b2",f.getColumn(myRow,1));
			assertEquals("c2",f.getColumn(myRow,2));
		}
    }
    /**
     * @return the file name for this class
     */
    public String getFileName()
    {
        return fileName;
    }

}