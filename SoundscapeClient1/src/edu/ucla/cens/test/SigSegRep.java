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
		this(id, (long) -1, (byte[]) null);
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
	public SigSegRep(int id, long timeMS, byte[] data) {
		this.id = id;
		this.timeMS = timeMS;
		this.data = data;
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
		this.data = new byte[(int)record.length];
		dataIn.read(this.data);
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
		result.append("<row>");
		result.append(this.createField("id", String.valueOf(this.id)));
		// <field name="date">10235135</date>\n
		result.append(this.createField("date", String.valueOf(this.timeMS)));
		// <field name="data">124af353d33c341....</data>\n
		char[] b64charar = Base64Coder.encode(this.data);
		String b64enc = String.valueOf(b64charar);
		//String urlenc = URLEncode.encode(b64enc);
		result.append(this.createField("data", b64enc));
		// </row>\n
		result.append("</row>");
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
			dataOut.write(this.data);
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
		return URLEncode.encode(result);
	}

}