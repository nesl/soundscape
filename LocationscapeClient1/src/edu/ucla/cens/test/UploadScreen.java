package edu.ucla.cens.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

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
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordListener;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotOpenException;

public class UploadScreen implements CommandListener, RecordListener {

	private class UploadScreenHelper implements Runnable, RecordListener {
		private UploadScreen parent = null;

		private boolean running = true;

		/**
		 * Constructor for UploadScreen
		 * 
		 * @param parent
		 *            pointer to parent object.
		 */
		UploadScreenHelper(UploadScreen parent) {
			this.parent = parent;
			synchronized (this.parent.recordStore) {
				this.parent.recordStore.addRecordListener(this);
			}
		}

		public void run() {
			try {
				while (this.running
						&& (this.parent.state != UploadScreen.STOPPED)) {
					// dispatch based on parent's state.
					switch (this.parent.state) {
					case UploadScreen.UPLOADING:
						doUploading();
						break;
					case UploadScreen.WAITING:
						doWaiting();
						break;
					case UploadScreen.SLEEPING:
						doSleeping();
						break;
					default:
						this.updateParentState(UploadScreen.STOPPED);
						break;
					}
				}
			} catch (Exception e) {
				this.parent.alertError("UploadScreenHelper.run()"
						+ e.getMessage());
				// e.printStackTrace();
			} finally {
				this.updateParentState(UploadScreen.STOPPED);
				this.parent.updateView();
			}
		}

		/**
		 * @throws RecordStoreNotOpenException
		 * @throws InvalidRecordIDException
		 * @throws RecordStoreException
		 */
		private void doUploading() throws RecordStoreNotOpenException,
				InvalidRecordIDException, RecordStoreException {
			synchronized (this.parent.recordStore) {
				this.parent.int_recordsRemaining = this.parent.recordStore
						.getNumRecords();
			}
			if (this.parent.int_recordsRemaining > 0) {
				int result = -1;
				try {
					result = this.parent.uploadRecord();
					if (result != 200) { // result is not 200
						this.updateParentState(UploadScreen.SLEEPING);
						this.parent.midlet
								.alertError("Got a non-200 response! Sleeping...:"
										+ String.valueOf(result));
					}
				} catch (RuntimeException e) {
					this.updateParentState(UploadScreen.SLEEPING);
					this.parent.midlet.alertError("Some exception was thrown:"
							+ e.getMessage());
				}
			} else { // No more records
				this.updateParentState(UploadScreen.WAITING);
			} // else
			this.parent.updateView();
		} // while

		private void doSleeping() {
			mySleep();
			this.updateParentState(UploadScreen.UPLOADING);
		}

		private void updateParentState(int newState) {
			if (this.parent.state != UploadScreen.STOPPED) {
				this.parent.state = newState;
			}
		}

		/**
		 * Sometimes I want to sleep. :P
		 */
		private void mySleep() {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException ie) {
				this.parent.midlet
						.alertError("InterruptedException while sleeping:"
								+ ie.getMessage());
			}
		}

		private void doWaiting() {
			mySleep();
		}

		public void recordAdded(RecordStore recordStore, int recordId) {
			if (this.parent.state == UploadScreen.WAITING) {
				this.updateParentState(UploadScreen.UPLOADING);
			}
		}

		public void recordChanged(RecordStore recordStore, int recordId) {
			return;
		}

		public void recordDeleted(RecordStore recordStore, int recordId) {
			return;
		}

	}

	public Form form = null;

	public int int_recordsSent = 0;

	public int int_recordsRemaining = 0;

	// public Gauge gauge = null;

	public StringItem strItem_recordsRemaining = null;

	public StringItem strItem_recordsSent = null;

	public StringItem status = null;

	public StringItem debug = null;

	public SimpleTest midlet = null;

	public RecordStore recordStore = null;

	public Command recordScreenCommand = new Command("-> Record Screen",
			Command.SCREEN, 1);

	public Command meterScreenCommand = new Command("-> Meter Screen",
			Command.SCREEN, 1);

	public Command enableUploadCommand = new Command("* Enable Upload",
			Command.SCREEN, 1);

	public Command disableUploadCommand = new Command("* Disable Upload",
			Command.SCREEN, 1);

	public Command exitCommand = new Command("Exit", Command.EXIT, 1);

	public Thread thread = null;

	/**
	 * We're idle.
	 */
	static final int STOPPED = 0;

	/**
	 * We're in the process of uploading.
	 */
	static final int UPLOADING = 1;

	/**
	 * We're waiting for more records to be entered.
	 */
	static final int WAITING = 2;

	/**
	 * We're waiting for a possibly transit error to go away, like a non-200
	 * HTTP response.
	 */
	static final int SLEEPING = 3;

	int state = UploadScreen.STOPPED;

	public UploadScreen(SimpleTest midlet, RecordStore recordStore)
			throws RecordStoreNotOpenException {

		this.midlet = midlet;

		// Open the record store.
		// try {
		this.recordStore = recordStore;
		// RecordStore.openRecordStore("data", true);
		// } catch (RecordStoreNotFoundException e) {
		// this.alertError("Error: RecordStore not found:" + e.getMessage());
		// } catch (RecordStoreFullException e) {
		// this.alertError("Error: RecordStore full:" + e.getMessage());
		// } catch (RecordStoreException e) {
		// this.alertError("Error: RecordStore Exception:" + e.getMessage());
		// }

		// UI Form - string items
		this.form = new Form("Upload Info");
		synchronized (this.recordStore) {
			this.int_recordsRemaining = this.recordStore.getNumRecords();
		}
		this.strItem_recordsRemaining = new StringItem("Records Queued", String
				.valueOf(this.int_recordsRemaining), Item.PLAIN);
		this.strItem_recordsSent = new StringItem("Records Sent", String
				.valueOf(this.int_recordsSent), Item.PLAIN);
		this.status = new StringItem("Status", "Idle", Item.PLAIN);
		this.debug = new StringItem("Debug", "Idle", Item.PLAIN);
		// UI Form - Gauge
		// this.gauge = new Gauge("Percentage Sent", false, 100, 0);

		// UI Form - Assemble Elements
		//this.form.append(this.debug);
		this.form.append(this.status);
		// this.form.append(this.gauge);
		this.form.append(this.strItem_recordsRemaining);
		this.form.append(this.strItem_recordsSent);
		this.form.addCommand(this.recordScreenCommand);
		this.form.addCommand(this.enableUploadCommand);
		this.form.addCommand(this.disableUploadCommand);
		this.form.addCommand(this.exitCommand);

		// UI Form - Set myself as a listener
		this.form.setCommandListener(this);

		// Set myself as a record listener
		synchronized (this.recordStore) {
			this.recordStore.addRecordListener(this);
		}
	}

	/*
	 * Warning. You need to synchronize on the record store before calling me!
	 */
	public void popRecord(int recID) throws RecordStoreNotOpenException,
			InvalidRecordIDException, RecordStoreException {

		try {
			this.recordStore.deleteRecord(recID);
		} catch (RecordStoreNotOpenException e) {
			this.alertError("popRecord:deleteRecord RecordStoreNotOpen");
			e.printStackTrace();
			throw e;
		} catch (InvalidRecordIDException e) {
			// this.alertError("popRecord:deleteRecord InvalidRecordID");
			e.printStackTrace();
			throw e;
		} catch (RecordStoreException e) {
			this.alertError("popRecord:deleteRecord RecordStoreException");
			e.printStackTrace();
			throw e;
		}

	}

	public void log(String message) {
		// this.debug.setText(message);
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
		try {
			if (c == this.recordScreenCommand) {
				Display.getDisplay(this.midlet).setCurrent(this.midlet.myForm);
			} else if (c == this.enableUploadCommand) {
				this.enableUploadCommandCB();
			} else if (c == this.disableUploadCommand) {
				this.disableUploadCommandCB();
			} else if (c == this.exitCommand) {
				this.midlet.notifyDestroyed();
			}
		} catch (RuntimeException e) {
			this.midlet.alertError("commandAction Exception:" + e.getMessage());
		}

	}

	public void recordAdded(RecordStore recordStore, int recordID) {
		this.updateRecordsRemaining(recordStore);
	}

	public void recordChanged(RecordStore recordStore, int recordID) {
		return;
	}

	public void recordDeleted(RecordStore recordStore, int recordID) {
		this.updateRecordsRemaining(recordStore);
	}

	public void updateView() {
		// update status text box.
		switch (this.state) {
		case UploadScreen.STOPPED:
			this.status.setText("STOPPED");
			break;
		case UploadScreen.SLEEPING:
			this.status.setText("SLEEPING");
			break;
		case UploadScreen.WAITING:
			this.status.setText("WAITING");
			break;
		case UploadScreen.UPLOADING:
			this.status.setText("UPLOADING");
			break;
		default:
			this.status.setText("UNKNOWN");
			break;
		}

		// update records remaining
		this.strItem_recordsRemaining.setText(String
				.valueOf(this.int_recordsRemaining));

		// update records sent
		this.strItem_recordsSent.setText(String.valueOf(this.int_recordsSent));

		// update gauge
		// int percentage = 0;
		// int recordsTotal = int_recordsRemaining + this.int_recordsSent;
		// if (recordsTotal > 0) {
		// float ratio = this.int_recordsSent
		// percentage = (int) java.lang.Math
		// .floor((this.int_recordsSent * 1.0)
		// / (1.0 * (this.int_recordsRemaining + this.int_recordsSent)));
		// }
		// this.gauge.setValue(percentage);
	}

	public int postViaHttpConnection(String url) { // throws IOException,
		// RecordStoreException {

		HttpConnection c = null;
		InputStream is = null;
		OutputStream os = null;
		int rc = -1;
		int recID = 0;
		RecordEnumeration recIter = null;
		// SigSeg sigSeg = null;
		Vector sigSegV = new Vector();
		int recID_offset = 0;
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
			postBuf.append("&project_id=84");
			postBuf.append("&tableName=gps");
			postBuf.append("&data_string=");

			synchronized (this.recordStore) {
				try {
					recIter = this.recordStore.enumerateRecords(null, null,
							true);
				} catch (RecordStoreNotOpenException e) {
					this
							.alertError("post:enumrateRecords RecordStoreNotOpenException"
									+ e.getMessage());
					throw e;
				}
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
				int numRecords = 10;
				synchronized(this.recordStore) {
					numRecords = java.lang.Math.min(numRecords, this.recordStore.getNumRecords());
				}
				for (int i = 0; i < numRecords; ++i) {
					recID_offset = i;
					sigSegV.addElement(new SigSeg(this.recordStore, recID + i));
				}
			} catch (RecordStoreNotOpenException e) {
				alertError("post:SigSeg RecordStoreNotOpen ");// +
				// e.getMessage());
				throw e;
			} catch (InvalidRecordIDException e) {
				// alertError("post:SigSeg InvalidIDException ");// +
				// e.getMessage());
				// throw e;
			} catch (RecordStoreException e) {
				alertError("post:SigSeg RecordStoreException"); // ";//+
				// e.getMessage());;
				throw e;
			} catch (IOException e) {
				alertError("post:SigSeg IOException " + e.getMessage());
				throw e;
			}

			this.log("Test8");

			postBuf.append(URLEncode.encode("<table>"));

			for (int i = 0; i < sigSegV.size(); ++i) {
				SigSeg ss = (SigSeg) sigSegV.elementAt(i);
				postBuf.append(URLEncode.encode("<row>"));
				// <field name="user">ASDFASDF</field>
				postBuf.append(URLEncode.encode("<field name=\"user\">"));
				String _userName = this.midlet.strItem_userName.getString();
				postBuf.append(URLEncode.encode(_userName));
				postBuf.append(URLEncode.encode("</field>"));
				postBuf.append(URLEncode.encode(ss.toXML()));
				// postBuf.append(sigSeg.toXML());
				// sigSeg.toXML();
				postBuf.append(URLEncode.encode("</row>"));
			}
			postBuf.append(URLEncode.encode("</table>"));

			try {
				// String urlenc = URLEncode.encode(postBuf.toString());
				// String urlenc = postBuf.toString();
				os.write(postBuf.toString().getBytes());
			} catch (IOException e) {
				alertError("post:os.write IOException " + e.getMessage());
				throw e;
			}
			this.log("Test9");

			// The reason why i'm commenting this out is because the
			// documentation says the OutputStream version of flush does
			// nothing. And further, I occasionally get IO exceptions here. :P

			// try {
			// os.flush();
			// } catch (IOException e) {
			// alertError("post:os.flush IOException " + e.getMessage());
			// throw e;
			// }
			this.log("Test10");

			try {
				rc = c.getResponseCode();
			} catch (IOException e) {
				//alertError("post:c.getResponseCode IOException"
				//		+ e.getMessage());
				//throw e;
			}
			this.log("Test11");
			if (rc != HttpConnection.HTTP_OK) {
				this.alertError("HTTP response code: " + String.valueOf(rc));
			} else {
				this.int_recordsSent += sigSegV.size();
			}
		} catch (Exception e) {
		} finally {
			for (int i = 0; i <= recID_offset; ++i) {
				synchronized (this.recordStore) {
					try {
						this.popRecord(recID + i);
					} catch (RecordStoreNotOpenException e1) {
						e1.printStackTrace();
					} catch (InvalidRecordIDException e1) {
						e1.printStackTrace();
					} catch (RecordStoreException e1) {
						e1.printStackTrace();
					}
				}
			}

			this.updateView();

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
	public int uploadRecord() {
		int result = -1;
		try {
			// create a connection, do the HTTP POST
			String url = "http://sensorbase.org/alpha/upload.php";
			result = this.postViaHttpConnection(url);

			// url = "http://kestrel.lecs.cs.ucla.edu/alpha/upload.php";
			// this.postViaHttpConnection(url);
		} catch (Exception e) {
			this.alertError("UploadScreen::uploadRecord() Exception"
					+ e.getMessage());
		}
		return result;
	}

	private void disableUploadCommandCB() {
		this.state = UploadScreen.STOPPED;
		this.updateView();
	}

	/**
	 * @param recordStore
	 */
	private void updateRecordsRemaining(RecordStore recordStore) {
		try {
			synchronized (recordStore) {
				this.int_recordsRemaining = recordStore.getNumRecords();
			}
		} catch (RecordStoreNotOpenException e) {
			this.alertError("RecordStoreNotOpenException: " + e.getMessage());
		}
		this.strItem_recordsRemaining.setText(String
				.valueOf(this.int_recordsRemaining));

	}

	/**
	 * 
	 */
	private void enableUploadCommandCB() {
		try {
			if (this.state == UploadScreen.STOPPED) {
				this.state = UploadScreen.UPLOADING;
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
