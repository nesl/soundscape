/**
 * Simply a data structure representing a Signal Segment.
 */
package edu.ucla.cens.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import edu.ucla.cens.test.Base64Coder;

public class SigSegRep {
	/** The id of the record. -1 means it's not been set. */
	public int id = -1;

	/** The time stamp of the record, in miliseconds. -1 means it's not been set. */
	public long timeMS = -1;

	/** The data associated with the record. */
	public byte[] data = null;

	public byte[] cameraData = null;
	
	public int density = 0;

	public int speed = 0;

	public int ratio = 0;

	public int proximity = 0;

	public String inOutCar = "";

	public String people = "";

	public String radio = "";

	public String roadType = "";

	public double lat = 0;
	public double lon = 0;
	
	/**
	 * This constructor doesn't do anything.
	 */
	public SigSegRep() {
	}

	/**
	 * Constructor that just takes an id.
	 * 
	 * @param id -
	 *            ID of corresponding record ID in some record store.
	 */
	public SigSegRep(int id) {
		this(id, (long) -1, (byte[]) null, (byte[]) null, 0, 0);
	}

	/**
	 * Constructor that uses specified id, time-stamp, and data.
	 * 
	 * @param id -
	 *            the record id. -1 means not initialized.
	 * @param timeMS -
	 *            the timestamp. -1 means not initialized.
	 * @param data -
	 *            the data. NULL means not initialized.
	 */
	public SigSegRep(int id, long timeMS, byte[] data, byte[] cameraData, double lat, double lon) {
		this.id = id;
		this.timeMS = timeMS;
		this.data = data;
		if (this.data == null) {
			this.data = new byte[0];
		}
		this.cameraData = cameraData;
		if (this.cameraData == null) {
			this.cameraData = new byte[0];
		}
		this.lat = lat;
		this.lon = lon;
	}


	/**
	 * Constructor that uses specified id and byte[] rep. of SigSegRep.
	 * 
	 * @param id -
	 *            the record id. -1 means not initialized.
	 * @param record -
	 *            a byte[] representing the information held in a record store.
	 * @throws IOException -
	 *             if unable to unmarshal from the record.
	 */
	public SigSegRep(int id, byte[] record) throws IOException {
		this.id = id;
		this.fromByteArray(record);
	}

	/**
	 * Modifies this to take on values of the fields contained in record.
	 * 
	 * @param record -
	 *            The byte[] representation of the sig seg.
	 * @throws IOException -
	 *             If there's a problem with unmarshalling the data.
	 * @return void
	 */
	public void fromByteArray(byte[] record) throws IOException {
		ByteArrayInputStream byteIn = new ByteArrayInputStream(record);
		DataInputStream dataIn = new DataInputStream(byteIn);
		this.timeMS = dataIn.readLong();
		
		int dataLength = dataIn.readInt();
		this.data = new byte[dataLength];
		dataIn.read(data, 0, dataLength);
		
		int cameraDataLength = dataIn.readInt();
		this.cameraData = new byte[cameraDataLength];
		dataIn.read(cameraData, 0, cameraDataLength);
		
		this.lat = dataIn.readDouble();
		this.lon = dataIn.readDouble();
		
		dataIn.close();
		byteIn.close();
	}

	/**
	 * Returns an XML representation of this, appropriate for upload to
	 * SensorBase. Note that it needs to be URL encoded as part of upload.
	 * 
	 * @return String - XML representation of SigSegRep.
	 */
	public String toXML() {
		StringBuffer result = new StringBuffer();
		// result.append("<row>");
		result.append(this.createField("id", String.valueOf(this.id)));
		// <field name="date">10235135</field>\n
		result.append(this.createField("date", String.valueOf(this.timeMS)));
		// <field name="data">124af353d33c341....</field>\n
		
		char[] b64charar = Base64Coder.encode(this.data);
		String b64enc = String.valueOf(b64charar);
		// String urlenc = URLEncode.encode(b64enc);
		result.append(this.createField("data", b64enc));
		// </row>\n
		// result.append("</row>");
		
		b64charar = Base64Coder.encode(this.cameraData);
		b64enc = String.valueOf(b64charar);
		result.append(this.createField("cameraData", b64enc));
		
		result.append(this.createField("lat", String.valueOf(this.lat)));
		result.append(this.createField("lon", String.valueOf(this.lon)));
		
		
		return result.toString();
	}

	/**
	 * Returns a byte[] representation of this.
	 * 
	 * @return A byte[] representation of this.
	 * @throws IOException
	 */
	public byte[] toByteArray() throws IOException {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(byteOut);
		try {
			dataOut.writeLong(this.timeMS);
			dataOut.writeInt(this.data.length);
			dataOut.write(this.data);
			dataOut.writeInt(this.cameraData.length);
			dataOut.write(this.cameraData);
			dataOut.writeDouble(this.lat);
			dataOut.writeDouble(this.lon);
			
			
			byte[] result = byteOut.toByteArray();
			dataOut.close();
			byteOut.close();
			return result;
		} catch (IOException e) {
			dataOut.close();
			byteOut.close();
			throw e;
		}
	}

	/**
	 * Helper function to create field strings, used by toXML().
	 * 
	 * @param name
	 *            The name of the field.
	 * @param val
	 *            The value of the field.
	 * @return A string with the field row filled out.
	 */
	private String createField(String name, String val) {
		// <field name="name">VAL</field>
		String result = "<field name=\"" + name + "\">" + val + "</field>\n";
		return result;
	}

}