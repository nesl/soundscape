package edu.ucla.cens.test;

import java.io.IOException;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotOpenException;

/**
 * @author adparker
 * 
 */
public class SigSeg implements SensorBaseInterface {
	/** A handle to the RecordStore object. */
	private RecordStore recordStore = null;

	/** A Signal Segment structure. */
	private SigSegRep rep = null;

	/**
	 * Constructor that creates a default SigSeg.
	 * 
	 * @param recordStore -
	 *            Handle to the RecordStore object.
	 * @return a SigSeg object.
	 */
	SigSeg(RecordStore recordStore) throws RecordStoreNotOpenException,
			RecordStoreException, RecordStoreFullException {
		this.recordStore = recordStore;
		int id = this.recordStore.addRecord(null, 0, 0);
		this.rep = new SigSegRep(id);
	}

	/**
	 * Constructor that creates a new record using the specified time-stamp and
	 * data.
	 * 
	 * @param recordStore
	 * @param timeMS
	 * @param data
	 * @throws IOException
	 * @throws RecordStoreException
	 * @throws RecordStoreFullException
	 * @throws RecordStoreNotOpenException
	 */
	SigSeg(RecordStore recordStore, long timeMS, byte[] data, byte[] cameraData, double lat, double lon)
			throws IOException, RecordStoreNotOpenException,
			RecordStoreFullException, RecordStoreException {
		this.recordStore = recordStore;
		int id = this.recordStore.addRecord(null, 0, 0);
		this.rep = new SigSegRep(id, timeMS, data, cameraData, lat, lon);
		this.setRecord();
	}

//	SigSeg(RecordStore recordStore, long timeMS, int density, int speed,
//			int ratio, int proximity, String inOutCar, String people,
//			String radio, String roadType, byte[] data, byte[] cameraData,
//			double lat, double lon)
//			throws IOException, RecordStoreNotOpenException,
//			RecordStoreFullException, RecordStoreException {
//		this.recordStore = recordStore;
//		int id = this.recordStore.addRecord(null, 0, 0);
//		this.rep = new SigSegRep(id, timeMS, density, speed, ratio, proximity,
//				inOutCar, people, radio, roadType, data, cameraData, lat, lon);
//		this.setRecord();
//	}

	/**
	 * Constructor that links into an existing record entry.
	 * 
	 * @param recordStore
	 * @param id
	 * @throws RecordStoreException
	 * @throws InvalidRecordIDException
	 * @throws RecordStoreNotOpenException
	 * @throws IOException -
	 *             if there's a problem unmarshaling from the record store.
	 */
	SigSeg(RecordStore recordStore, int id) throws RecordStoreNotOpenException,
			InvalidRecordIDException, RecordStoreException, IOException {
		this.recordStore = recordStore;
		byte[] result = this.recordStore.getRecord(id);
		this.rep = new SigSegRep(id, result);
	}

	/**
	 * Accessor function for time-stamp.
	 * 
	 * @return the time-stamp
	 */
	public long getTimeMS() {
		return this.rep.timeMS;
	}

	/**
	 * Sets the time-stamp (in miliseconds). Updates the data structure, and
	 * writes to the record store.
	 * 
	 * @param timeMS
	 *            the time-stamp in miliseconds.
	 * @throws RecordStoreException
	 * @throws RecordStoreFullException
	 * @throws InvalidRecordIDException
	 * @throws RecordStoreNotOpenException
	 * @throws IOException
	 */
	public void setTimeMS(long timeMS) throws RecordStoreNotOpenException,
			InvalidRecordIDException, RecordStoreFullException,
			RecordStoreException, IOException {
		this.rep.timeMS = timeMS;
		this.setRecord();
	}

	/**
	 * Accessor function for the ID.
	 * 
	 * @return the id
	 */
	public int getId() {
		return this.rep.id;
	}

	/**
	 * Accessor function for the data.
	 * 
	 * @return the data
	 */
	public byte[] getData() {
		return this.rep.data;
	}

	/**
	 * Accessor function for recordStore.
	 * 
	 * @return the recordStore
	 */
	public RecordStore getRecordStore() {
		return recordStore;
	}

	/**
	 * Updates the RecordStore with the current state of rep.
	 * 
	 * @throws RecordStoreNotOpenException
	 * @throws InvalidRecordIDException
	 * @throws RecordStoreFullException
	 * @throws RecordStoreException
	 * @throws IOException
	 */
	private void setRecord() throws RecordStoreNotOpenException,
			InvalidRecordIDException, RecordStoreFullException,
			RecordStoreException, IOException {
		byte[] record = this.rep.toByteArray();
		this.recordStore.setRecord(this.rep.id, record, 0, record.length);
	}

	/**
	 * Returns an XML string deliminated by "<row>" and "</row>".
	 */
	public String toXML() {
		return this.rep.toXML();
	}
}
