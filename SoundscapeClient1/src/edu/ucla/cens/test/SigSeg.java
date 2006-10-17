package edu.ucla.cens.test;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.midp.io.Base64;

import edu.ucla.cens.test.SensorBaseInterface;

public class SigSeg implements SensorBaseInterface {
	int id = -1;
	long timeMS = 0;
	public byte[] data = null;
	
	SigSeg (long timeMS, byte[] data)
	{
		this.timeMS = timeMS;
		this.data = data;
		return;
	}
	
	// TODO need to get passed in the record id, and assign it.
	SigSeg (int id, byte[] record) throws IOException
	{
		ByteArrayInputStream byteIn = new ByteArrayInputStream(record);
		DataInputStream dataIn = new DataInputStream(byteIn);
		try
		{
			this.id = id;
			this.timeMS = dataIn.readLong();
			dataIn.read(this.data);
			dataIn.close();
			byteIn.close();
		}
		catch (IOException e)
		{
			throw e;
		}
		return;
	}
	
	public byte[] toByteArray () throws IOException
	{
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(byteOut);
		try
		{
			dataOut.writeLong(this.timeMS);
			dataOut.write(this.data);
			byte[] result = byteOut.toByteArray();
			dataOut.close();
			byteOut.close();
			return result;
		}
		catch (IOException e)
		{
			dataOut.close();
			byteOut.close();
			throw e;
		}
	}
	
	public String toXML() 
	{
		String result = "";
		// <row>\n
		result += "<row>\n";
		// <field name="id">3434</field>\n
		result += createFieldString("id", String.valueOf(this.id));
		// <field name="date">10235135</date>\n
		result += this.createFieldString("date", String.valueOf(this.timeMS));
		// <field name="data">124af353d33c341....</data>\n
		String b64enc = Base64.encode(this.data, 0, this.data.length);
		// TODO implement URLEncode.encode
		String urlenc = URLEncode.encode(b64enc); 
		result += this.createFieldString("data", urlenc);
		// </row>\n
		result += "</row>\n";
		return result;
	}
	
	private String createFieldString(String name, String fieldVal)
	{
		String result = "";
		result += "<field name=\"" + name + "\">" + fieldVal + "</field>\n";
		return result;
	}
}
