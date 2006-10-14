package edu.ucla.cens.test;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SigSeg {
	long timeMS = 0;
	public byte[] data = null;
	
	SigSeg (long timeMS, byte[] data)
	{
		this.timeMS = timeMS;
		this.data = data;
		return;
	}
	
	SigSeg (byte[] record) throws IOException
	{
		ByteArrayInputStream byteIn = new ByteArrayInputStream(record);
		DataInputStream dataIn = new DataInputStream(byteIn);
		try
		{
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
	
}
