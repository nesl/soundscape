package edu.ucla.cens.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordListener;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

public class UploadScreen implements CommandListener, RecordListener {

	private class UploadScreenHelper implements Runnable {
		private UploadScreen parent = null;

		UploadScreenHelper(UploadScreen parent) {
			this.parent = parent;
		}

		public void run() {
			while (this.parent.state == this.parent.UPLOADING) {
				if (this.parent.int_recordsRemaining > 0) {
					this.parent.uploadRecord();
				} else {
					break;
				}
			}
		}
	}

	public Form form;

	public int int_recordsSent;

	public int int_recordsRemaining;

	public Gauge gauge;

	public StringItem recordsRemaining;

	public StringItem recordsSent;

	public StringItem status;

	public SimpleTest midlet;

	public RecordStore recordStore;

	public Command backCommand = new Command("Back", Command.BACK, 1);

	public Command uploadCommand = new Command("Upload", Command.SCREEN, 1);

	public Command stopCommand = new Command("Stop", Command.STOP, 1);

	public Thread thread;

	final int STOPPED = 0;

	final int UPLOADING = 1;

	int state = 0;

	public UploadScreen(SimpleTest midlet) throws RecordStoreNotOpenException {
		this.midlet = midlet;

		// Open the record store.
		try {
			this.recordStore = RecordStore.openRecordStore("data", true);
		} catch (RecordStoreNotFoundException e) {
			this.alertError("Error: RecordStore not found:" + e.getMessage());
		} catch (RecordStoreFullException e) {
			this.alertError("Error: RecordStore full:" + e.getMessage());
		} catch (RecordStoreException e) {
			this.alertError("Error: RecordStore Exception:" + e.getMessage());
		}

		// UI Form - string items
		this.form = new Form("Upload Info");
		this.int_recordsRemaining = this.recordStore.getNumRecords();
		this.recordsRemaining = new StringItem("Records Remaining", String
				.valueOf(this.int_recordsRemaining), Item.PLAIN);
		this.recordsSent = new StringItem("Records Sent", String
				.valueOf(this.int_recordsSent), Item.PLAIN);
		this.status = new StringItem("Status", "Idle", Item.PLAIN);

		// UI Form - Gauge
		this.gauge = new Gauge("Percentage Sent", false, 0, 100);

		// UI Form - Assemble Elements
		this.form.append(this.status);
		this.form.append(this.gauge);
		this.form.append(this.recordsRemaining);
		this.form.append(this.recordsSent);
		this.form.addCommand(this.backCommand);
		this.form.addCommand(this.uploadCommand);
		this.form.addCommand(this.stopCommand);

	}

	/**
	 * Creates an alert message on the phone.
	 * 
	 * @param message
	 *            The message to display.
	 */
	public void alertError(String message) {
		Alert alert = new Alert("Error", message, null, AlertType.ERROR);
		Display display = Display.getDisplay(this.midlet);
		Displayable current = display.getCurrent();
		if (!(current instanceof Alert)) {
			// This next call can't be done when current is an Alert
			display.setCurrent(alert, current);
		}
	}

	public void commandAction(Command c, Displayable d) {
		if (c == this.backCommand) {
			this.backCommandCB();
		} else if (c == this.uploadCommand) {
			this.uploadCommandCB();
		} else if (c == this.stopCommand) {
			this.stopCommandCB();
		}
	}

	public void recordAdded(RecordStore recordStore, int recordID) {
		this.updateRecordsRemaining(recordStore);
		this.updateView();
	}

	public void recordChanged(RecordStore recordStore, int recordID) {
		return;
	}

	public void recordDeleted(RecordStore recordStore, int recordID) {
		this.updateRecordsRemaining(recordStore);
	}

	public void updateView() {
		// update status text box.
		if (this.state == this.STOPPED) {
			this.status.setText("STOPPED");
		} else if (this.state == this.UPLOADING) {
			this.status.setText("UPLOADING)");
		}
		// update gauge
		int percentage = (int) (this.int_recordsSent * 1.0 / (1.0 + this.int_recordsRemaining + this.int_recordsSent));
		this.gauge.setValue(percentage);
		// update records remaining
		this.recordsRemaining
				.setText(String.valueOf(this.int_recordsRemaining));
		// update records sent
		this.recordsSent.setText(String.valueOf(this.int_recordsSent));
	}

	public int postViaHttpConnection(String url) throws IOException,
			RecordStoreException {
		HttpConnection c = null;
		InputStream is = null;
		OutputStream os = null;
		int rc = -1;
		try {
			c = (HttpConnection) Connector.open(url, Connector.READ_WRITE);
			c.setRequestMethod(HttpConnection.POST);
			c.setRequestProperty("Content-type",
					"application/x-www-form-urlencoded");
			c.setRequestProperty("User-Agent",
					"Profile/MIDP-2.0 Configuration/CLDC-1.0");
			c.setRequestProperty("Accept", "text/plain");
			os = c.openOutputStream();
			StringBuffer postBuf = new StringBuffer();
			postBuf.append("email=adparker@gmail.com");
			postBuf.append("&pwd=ecopda");
			postBuf.append("&type=xml");
			postBuf.append("&project_id=43");
			postBuf.append("&tableName=test1");
			postBuf.append("&data_string=");
			RecordEnumeration recIter = this.recordStore.enumerateRecords(null,
					null, true);
			int recID = recIter.nextRecordId();
			SigSeg sigSeg = new SigSeg(this.recordStore, recID);
			postBuf.append("<table>\n");
			postBuf.append(sigSeg.toXML());
			postBuf.append("</table>\n");
			os.write(postBuf.toString().getBytes());
			os.flush();
			rc = c.getResponseCode();
			if (rc != HttpConnection.HTTP_OK) {
				this.alertError("HTTP response code: " + String.valueOf(rc));
			} else {
				++this.int_recordsSent;
			}
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Not an HTTP URL");
		} finally {
			if (is != null)
				is.close();
			if (os != null)
				os.close();
			if (c != null)
				c.close();
		}
		return rc;
	}

	

	// This gets called from the helper thread
	public void uploadRecord() {
		try {
			// create a connection, do the HTTP POST
			String url = "http://sensorbase.org/alpha/upload.php";
			this.postViaHttpConnection(url);
			this.updateView();
		} catch (IOException e) {
			this.alertError(e.getMessage());
		} catch (RecordStoreException e) {
			this.alertError(e.getMessage());
		}
	}

	private void backCommandCB() {
		Display.getDisplay(this.midlet).setCurrent(this.midlet.myForm);
		this.updateView();
	}

	private void stopCommandCB() {
		this.state = this.STOPPED;
		this.updateView();
	}

	/**
	 * @param recordStore
	 */
	private void updateRecordsRemaining(RecordStore recordStore) {
		try {
			this.int_recordsRemaining = recordStore.getNumRecords();
		} catch (RecordStoreNotOpenException e) {
			this.alertError("RecordStoreNotOpenException: " + e.getMessage());
		}
	}

	/**
	 * 
	 */
	private void uploadCommandCB() {
		if (this.state != this.UPLOADING) {
			this.state = this.UPLOADING;
			this.thread = new Thread(new UploadScreenHelper(this));
			this.thread.start();
		}
		this.updateView();
	}

}
