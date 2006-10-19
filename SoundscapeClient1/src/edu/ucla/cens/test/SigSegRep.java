/**
 * Simply a data structure representing a Signal Segment.
 */
package edu.ucla.cens.test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import com.sun.midp.io.Base64;

public class SigSegRep {
	/** The id of the record. -1 means it's not been set. */
	public int id = -1;
	/** The time stamp of the record. -1 means it's not been set. */
	public long timeMS = -1;
	/** The data associated with the record. */
	public byte[] data = null;
	
	/**
	 * This constructor doesn't do anything.
	 *
	 */
	public SigSegRep() {
	}
	
	/**
	 * @param id - the record id. -1 means not initialized.
	 * @param timeMS - the timestamp. -1 means not initialized.
	 * @param data - the data. NULL means not initialized.
	 */
	public SigSegRep(int id, long timeMS, byte[] data) {
		this.id = id;
		this.timeMS = timeMS;
		this.data = data;
	}
	
	/**
	 * @param id - the record id. -1 means not initialized.
	 * @param record - a byte[] representing the information held in a record store.
	 * @throws IOException - if unable to unmarshal from the record.
	 */
	public SigSegRep(int id, byte[] record) throws IOException {
		this.id = id;
		this.fromByteArray(record);
	}
	
	/**
	 * Modifies this to take on values of the fields contained in record.
	 * 
	 * @param record - The byte[] representation of the sig seg.
	 * @throws IOException - If there's a problem with unmarshalling the data.
	 */
	public void fromByteArray(byte[] record) throws IOException {
		ByteArrayInputStream byteIn = new ByteArrayInputStream(record);
		DataInputStream dataIn = new DataInputStream(byteIn);
		this.timeMS = dataIn.readLong();
		dataIn.read(this.data);
		dataIn.close();
		byteIn.close();
	}
	
	public String toXML() {
		StringBuffer result = new StringBuffer();
		result.append("<row>\n");
		result.append(this.createField("id", String.valueOf(this.id)));
//		 <field name="date">10235135</date>\n
		result.append(this.createField("date", String.valueOf(this.timeMS)));
		// <field name="data">124af353d33c341....</data>\n
		String b64enc = Base64.encode(this.data, 0, this.data.length);
		// TODO implement URLEncode.encode
		String urlenc = URLEncode.encode(b64enc);
		result.append(this.createField("data", urlenc));
		// </row>\n
		result.append("</row>\n");
		return result.toString();
	}
	
	private String createField(String name, String val) {
		return "<field name=\"" + name + "\">" + val + "</field>\n";
	}
	
}