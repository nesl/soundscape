package edu.ucla.cens.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.ConnectionNotFoundException;
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
import javax.microedition.rms.InvalidRecordIDException;
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
			try {
				while (this.parent.state == this.parent.UPLOADING) {
					if (this.parent.int_recordsRemaining > 0) {
						this.parent.uploadRecord();
					} else {
						break;
					}
					// TODO debugging only
					this.parent.state = this.parent.STOPPED;
					break;
				}
			} catch (Exception e) {
				this.parent.alertError("UploadScreenHelper.run()"
						+ e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public Form form = null;

	public int int_recordsSent = 0;

	public int int_recordsRemaining = 0;

	public Gauge gauge = null;

	public StringItem recordsRemaining = null;

	public StringItem recordsSent = null;

	public StringItem status = null;

	public StringItem debug = null;
	
	public SimpleTest midlet = null;

	public RecordStore recordStore = null;

	public Command backCommand = new Command("Record View", Command.SCREEN, 1);

	public Command uploadCommand = new Command("Upload", Command.SCREEN, 1);

	public Command stopCommand = new Command("Stop", Command.SCREEN, 1);

	public Thread thread = null;

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
		this.debug = new StringItem("Debug", "Idle", Item.PLAIN);
		// UI Form - Gauge
		this.gauge = new Gauge("Percentage Sent", false, 100, 0);

		// UI Form - Assemble Elements
		this.form.append(this.debug);
		this.form.append(this.status);
		this.form.append(this.gauge);
		this.form.append(this.recordsRemaining);
		this.form.append(this.recordsSent);
		this.form.addCommand(this.backCommand);
		this.form.addCommand(this.uploadCommand);
		this.form.addCommand(this.stopCommand);

		// UI Form - Set myself as a listener
		this.form.setCommandListener(this);
	}

	
	public void log(String message) {
		this.debug.setText(message);
	}
	/**
	 * Creates an alert message on the phone.
	 * 
	 * @param message
	 *            The message to display.
	 */
	public void alertError(String message) {
		Alert alert = new Alert("Alert", message, null, AlertType.CONFIRMATION);
		Display display = Display.getDisplay(this.midlet);
		Displayable current = display.getCurrent();
		if (!(current instanceof Alert)) {
			// This next call can't be done when current is an Alert
			display.setCurrent(alert, current);
		}
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void commandAction(Command c, Displayable d) {
		if (c == this.backCommand) {
			Display.getDisplay(this.midlet).setCurrent(this.midlet.myForm);
			// this.backCommandCB();
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
			this.status.setText("UPLOADING");
		}
		// update gauge
		int percentage = 0;
		if (this.int_recordsRemaining > 0) {
			percentage = (int) java.lang.Math
					.floor((this.int_recordsSent * 1.0)
							/ (1.0 + this.int_recordsRemaining + this.int_recordsSent));
		}
		this.gauge.setValue(percentage);
		// update records remaining
		this.recordsRemaining
				.setText(String.valueOf(this.int_recordsRemaining));
		// update records sent
		this.recordsSent.setText(String.valueOf(this.int_recordsSent));
	}

	public int postViaHttpConnection(String url) { // throws IOException,
		// RecordStoreException {

		HttpConnection c = null;
		InputStream is = null;
		OutputStream os = null;
		int rc = -1;
		int recID = 0;
		RecordEnumeration recIter = null;
		SigSeg sigSeg = null;
		this.log("Test1");
		try {
			try {
				c = (HttpConnection) Connector.open(url, Connector.READ_WRITE);
			} catch (IllegalArgumentException e) {
				this
						.alertError("post:open IllegalArgument: parameter is invalid. "
								+ e.getMessage());
				throw e;
			} catch (ConnectionNotFoundException e) {
				this
						.alertError("post:open ConnectionNotFound: target not found, or protocol not supported. "
								+ e.getMessage());
				throw e;
			} catch (IOException e) {
				this.alertError("post:open IOException: " + e.getMessage());
				throw e;
			} catch (SecurityException e) {
				this.alertError("post:open SecurityException: "
						+ e.getMessage());
				throw e;
			}
			this.log("Test2");

			try {
				c.setRequestMethod(HttpConnection.POST);
			} catch (IOException e) {
				this
						.alertError("post:setReqMethod IOException: the method cannot be reset or if the requested method isn't valid for HTTP:"
								+ e.getMessage());
				throw e;
			}
			this.log("Test3");

			try {
				c.setRequestProperty("Content-type",
						"application/x-www-form-urlencoded");
				c.setRequestProperty("User-Agent",
						"Profile/MIDP-2.0 Configuration/CLDC-1.0");
				c.setRequestProperty("Accept", "text/plain");
				c.setRequestProperty("Accept-Encoding", "identity");
			} catch (IOException e) {
				this
						.alertError("post:setReqProperty IOException: connection is in connected state."
								+ e.getMessage());
				throw e;
			}
			this.log("Test4");

			try {
				os = c.openOutputStream();
			} catch (IOException e) {
				this
						.alertError("post:openOutputStream IOException: maybe output stream has been closed?"
								+ e.getMessage());
				throw e;
			}
			this.log("Test5");

			StringBuffer postBuf = new StringBuffer();
			postBuf.append("email=adparker%40gmail.com");
			postBuf.append("&pw=ecopda");
			postBuf.append("&type=xml");
			postBuf.append("&project_id=43");
			postBuf.append("&tableName=test1");
			postBuf.append("&data_string=");

			try {
				recIter = this.recordStore.enumerateRecords(null, null, true);
			} catch (RecordStoreNotOpenException e) {
				this
						.alertError("post:enumrateRecords RecordStoreNotOpenException"
								+ e.getMessage());
				throw e;
			}
			this.log("Test6");

			try {
				recID = recIter.nextRecordId();
			} catch (InvalidRecordIDException e) {
				this.alertError("post:nextRecordId: no more records."
						+ e.getMessage());
				throw e;
			}
			this.log("Test7");

			try {
				sigSeg = new SigSeg(this.recordStore, recID);
			} catch (RecordStoreNotOpenException e) {
				alertError("post:SigSeg RecordStoreNotOpen ");// + e.getMessage());
				throw e;
			} catch (InvalidRecordIDException e) {
				alertError("post:SigSeg InvalidIDException ");// + e.getMessage());
				throw e;
			} catch (RecordStoreException e) {
				alertError("post:SigSeg RecordStoreException");  //";//+ e.getMessage());;
				throw e;
			} catch (IOException e) {
				alertError("post:SigSeg IOException " + e.getMessage());
				throw e;
			}
			this.log("Test8");

			postBuf.append(URLEncode.encode("<table>"));
			postBuf.append(URLEncode.encode(sigSeg.toXML()));
			//postBuf.append(sigSeg.toXML());
			//sigSeg.toXML();
			postBuf.append(URLEncode.encode("</table>"));

			// URL encode!
			
			
			try {
				//String urlenc = URLEncode.encode(postBuf.toString());
				//String urlenc = postBuf.toString();
				os.write(postBuf.toString().getBytes());
			} catch (IOException e) {
				alertError("post:os.write IOException " + e.getMessage());
				throw e;
			}
			this.log("Test9");

			try {
				os.flush();
			} catch (IOException e) {
				alertError("post:os.flush IOException " + e.getMessage());
				throw e;
			}
			this.log("Test10");

			try {
				rc = c.getResponseCode();
			} catch (IOException e) {
				alertError("post:c.getResponseCode IOException"
						+ e.getMessage());
				throw e;
			}
			this.log("Test11");
			this.alertError("HTTP response code: " + String.valueOf(rc));
			if (rc != HttpConnection.HTTP_OK) {
			} else {
				++this.int_recordsSent;
				this.updateView();
			}

		} catch (Exception e) {
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (os != null)
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (c != null)
				try {
					c.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return rc;
	}

	// This gets called from the helper thread
	public void uploadRecord() {
		try {
			// create a connection, do the HTTP POST
			String url = "http://sensorbase.org/alpha/upload.php";
			this.postViaHttpConnection(url);
			
			url = "http://kestrel.lecs.cs.ucla.edu/alpha/upload.php";
			this.postViaHttpConnection(url);
			this.updateView();
		} catch (Exception e) {
			this.alertError("UploadScreen::uploadRecord() Exception"
					+ e.getMessage());
		}
	}

	public void backCommandCB() {
		this.alertError("Stop.");
		Display.getDisplay(this.midlet).setCurrent(this.midlet.myForm);
		// this.updateView();
	}

	private void stopCommandCB() {
		this.state = this.STOPPED;
		this.updateView();
		this.midlet.alertError("Stop.");
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
		try {
			if (this.state != this.UPLOADING) {
				this.state = this.UPLOADING;
				this.thread = new Thread(new UploadScreenHelper(this));
				this.thread.start();
			}
			this.updateView();
		} catch (Exception e) {
			this.alertError("uploadCommand:" + e.getMessage());
			e.printStackTrace();
		}
	}

}
