package edu.ucla.cens.test;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import com.sun.midp.io.Base64;
import javax.microedition.rms.*;

import edu.ucla.cens.test.SensorBaseInterface;
import edu.ucla.cens.test.URLEncode;
import edu.ucla.cens.test.SigSegRep;

public class SigSeg implements SensorBaseInterface {
	/** A handle to the RecordStore object. */
	private RecordStore recordStore = null;
	
	/** A Signal Segment structure. */
	private SigSegRep rep = null;
	
	/**
	 * Constructor for SigSeg. Creates a new record in the record store.
	 * 
	 * @param recordStore - Handle to the RecordStore object.
	 * @return a SigSeg object.
	 */
	SigSeg(RecordStore recordStore) throws RecordStoreNotOpenException,
			RecordStoreException, RecordStoreFullException {
		this.recordStore = recordStore;
		int id = this.recordStore.addRecord(null, 0, 0);
		this.rep = new SigSegRep(id, -1, null);
	}

	/**
	 * Constructor for SigSeg. Creates link to the record with the matching ID.
	 * 
	 * @param recordStore
	 * @param id
	 * @throws RecordStoreException
	 * @throws InvalidRecordIDException
	 * @throws RecordStoreNotOpenException
	 */
	SigSeg(RecordStore recordStore, int id) throws RecordStoreNotOpenException,
			InvalidRecordIDException, RecordStoreException {
		this.recordStore = recordStore;
		byte[] result = this.recordStore.getRecord(this.id);
		
	}

	/**
	 * @return the timeMS
	 */
	public long getTimeMS() {
		return timeMS;
	}


	/**
	 * @param timeMS the timeMS to set
	 */
	public void setTimeMS(long timeMS) {
		this.timeMS = timeMS;
	}


	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}


	/**
	 * @return the recordStore
	 */
	public RecordStore getRecordStore() {
		return recordStore;
	}


	SigSeg(long timeMS, byte[] data) {
		this.timeMS = timeMS;
		this.data = data;
		return;
	}

	// TODO need to get passed in the record id, and assign it.
	SigSeg(int id, byte[] record) throws IOException {
		ByteArrayInputStream byteIn = new ByteArrayInputStream(record);
		DataInputStream dataIn = new DataInputStream(byteIn);
		try {
			this.id = id;
			this.timeMS = dataIn.readLong();
			dataIn.read(this.data);
			dataIn.close();
			byteIn.close();
		} catch (IOException e) {
			throw e;
		}
		return;
	}

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

	public String toXML() {
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

	private String createFieldString(String name, String fieldVal) {
		String result = "";
		result += "<field name=\"" + name + "\">" + fieldVal + "</field>\n";
		return result;
	}
}
