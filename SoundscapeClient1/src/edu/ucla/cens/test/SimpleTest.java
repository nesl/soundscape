package edu.ucla.cens.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.StringItem;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.RecordControl;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordListener;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

/**
 * @author adparker
 * 
 */
public class SimpleTest extends MIDlet implements CommandListener,
		PlayerListener, ItemStateListener, RecordListener {

	private class SimpleTestHelper implements Runnable {
		/**
		 * Handler to our parent midlet.
		 */
		private SimpleTest midlet = null;

		/**
		 * Number of miliseconds to sleep.
		 */
		private int sleepMS = 0;

		/**
		 * The string to deliver to the listener.
		 */
		String msg = null;

		/**
		 * Constructor.
		 * 
		 * @param midlet
		 *            Handle to our parent midlet.
		 * @param sleepMS
		 *            Number of seconds to sleep.
		 * @param msg
		 *            Message to deliver.
		 */
		SimpleTestHelper(SimpleTest midlet, int sleepMS, String msg) {
			this.midlet = midlet;
			this.sleepMS = sleepMS;
			this.msg = msg;
		}

		/*
		 * Sleeps for a specified amount of time before triggering
		 * this.midlet.playerUpdate callback.
		 */
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			try {
				Thread.sleep(this.sleepMS);
			} catch (InterruptedException ie) {
				this.midlet.alertError("InterruptedException while sleeping:"
						+ ie.getMessage());
			}
			this.midlet.playerUpdate(null, this.msg, null);
		}
	}

	/**
	 * Convert the byte array to an int starting from the given offset.
	 * 
	 * @param b
	 *            The byte array
	 * @param offset
	 *            The array offset
	 * @return The integer
	 */
	public static int byteArrayToInt(byte[] b, int offset) {
		int value = 0;
		for (int i = 0; i < 4; i++) {
			// int shift = (bytewidth - 1 - i) * 8;
			int shift = i * 8;
			value += (b[i + offset] & 0x000000FF) << shift;
		}
		return value;
	}

	/**
	 * Convert a byte[] to a Short.
	 * 
	 * @param b
	 *            The byte array.
	 * @param offset
	 *            The offset into the byte array.
	 * @return The value of the Short located at the specified offset in the
	 *         byte array.
	 */
	public static short byteArrayToShort(byte[] b, int offset) {
		short value = 0;
		for (int i = 0; i < 2; i++) {
			int shift = i * 8;
			value += (b[i + offset] & 0x000000FF) << shift;
		}
		return value;
	}

	// //////////////////////////
	// Acoustic Data

	/**
	 * The byte[] representation of the acoustic data recorded.
	 */
	public byte[] output = null;

	/**
	 * A vector containing power levels of the last several acoustic readings,
	 * in order from oldest to most recent.
	 */
	public Vector power = new Vector();

	/**
	 * This is the storage used by this object's SigSeg.
	 */
	public RecordStore recordStore = null;

	/**
	 * A count of the number of acoust samples taken since application start-up.
	 */
	public int samplesTaken = 0;

	// /////////////////////////
	// Recording

	/**
	 * The thread that implements the timer callback.
	 */
	private Thread myThread = null;

	/**
	 * The player for the Recording.
	 */
	private Player p = null;

	/**
	 * Where the player streams the audio data.
	 */
	private ByteArrayOutputStream tempoutput = null;

	/**
	 * The controller for this.p;
	 */
	private RecordControl rc = null;

	/**
	 * A flag that tells this whether or not stop the player, or to start the
	 * player.
	 */
	private boolean stopPlayer = true;

	// /////////////////////////
	// UI Canvas: Sound Meter
	/**
	 * The Canvas object that draws the graph.
	 */
	private HelloCanvas myCanvas;

	// ////////////////////////
	// UI Form: Records
	/**
	 * The Form object that contains information about records and EOS.
	 */
	private Form myForm;

	/**
	 * Belongs to this.myForm. Gauge controlling the base window size.
	 */
	private Gauge myGaugeTotal;

	/**
	 * Belongs to this.myForm. Gauge controlling the recording window size.
	 */
	private Gauge myGaugeShared;

	/**
	 * Belongs to this.myForm. A combo box for recording/stopping/deleting.
	 */
	private ChoiceGroup myChoiceGroupActions;

	/**
	 * Belongs to this.myForm. A text box that shows some stats on the number of
	 * recordings made and kept.
	 */
	private StringItem myStringItem;

	// /////////////////////////
	// UI Menu Commands
	private Command backCommand = new Command("Back", Command.BACK, 1);

	private Command displayCommand = new Command("Meter", Command.SCREEN, 1);

	private Command exitCommand = new Command("Exit", Command.EXIT, 1);

	private Command showCommand = new Command("Show Levels", Command.SCREEN, 1);

	private Command recordCommand = new Command("Record", Command.SCREEN, 1);

	private Command playCommand = new Command("Play", Command.SCREEN, 1);

	/**
	 * Default constructor. It creates a Canvas and a Form object.
	 */
	public SimpleTest() {
		// ////////////////////////////////////
		// Open the record store.
		try {
			this.recordStore = RecordStore.openRecordStore("data", true);
			this.recordStore.addRecordListener(this);
		} catch (RecordStoreNotFoundException e) {
			this.alertError("Error: RecordStore not found:" + e.getMessage());
		} catch (RecordStoreFullException e) {
			this.alertError("Error: RecordStore full:" + e.getMessage());
		} catch (RecordStoreException e) {
			this.alertError("Error: RecordStore Exception:" + e.getMessage());
		}

		// /////////////////////////////////////////////////
		// UI Record Form - Record Info
		this.myForm = new Form("Record Info");
		// StringItem: # of Saved Samples
		this.myStringItem = new StringItem("Taken/Saved/Total Saved:", String
				.valueOf(-1), Item.PLAIN);
		this.updateStringItem(this.recordStore, -1);
		this.myForm.append(this.myStringItem);
		// ChoiceGroup: Actions
		this.myChoiceGroupActions = new ChoiceGroup("Actions:",
				Choice.EXCLUSIVE);
		this.myChoiceGroupActions.append("Stop", null);
		this.myChoiceGroupActions.append("Record", null);
		this.myChoiceGroupActions.append("Clear", null);
		this.myForm.append(this.myChoiceGroupActions);
		// Gauges - Total Window Size | Shared Window Size
		this.myGaugeTotal = new Gauge("Total Window Size (ms):", true, 5000,
				5000);
		this.myForm.append(this.myGaugeTotal);
		this.myGaugeShared = new Gauge("Shared Window Size (ms):", true, 5000,
				4000);
		this.myForm.append(this.myGaugeShared);
		// Add commands.
		this.myForm.addCommand(this.showCommand);
		this.myForm.addCommand(this.displayCommand);
		this.myForm.addCommand(this.exitCommand);
		// Install Command and Item Listeners for Form.
		this.myForm.setCommandListener(this);
		this.myForm.setItemStateListener(this);

		// /////////////////////////////////////////////////
		// UI Canvas (for the sound meter)
		this.myCanvas = new HelloCanvas(this);
		// Add commands.
		this.myCanvas.addCommand(this.backCommand);
		this.myCanvas.addCommand(this.playCommand);
		this.myCanvas.addCommand(this.recordCommand);
		// Install a Command Listener for the Canvas.
		this.myCanvas.setCommandListener(this);

	}

	/**
	 * Creates an alert message on the phone.
	 * 
	 * @param message
	 *            The message to display.
	 */
	public void alertError(String message) {
		Alert alert = new Alert("Error", message, null, AlertType.ERROR);
		Display display = Display.getDisplay(this);
		Displayable current = display.getCurrent();
		if (!(current instanceof Alert)) {
			// This next call can't be done when current is an Alert
			display.setCurrent(alert, current);
		}
	}

	/*
	 * Callback for the softkey menu
	 * 
	 * @see javax.microedition.lcdui.CommandListener#commandAction(javax.microedition.lcdui.Command,
	 *      javax.microedition.lcdui.Displayable)
	 */
	public void commandAction(Command c, Displayable d) {
		if (c == this.exitCommand) {
			this.notifyDestroyed();
		}
		if (c == this.backCommand) {
			Display.getDisplay(this).setCurrent(this.myForm);
		}
		if (c == this.displayCommand) {
			Display.getDisplay(this).setCurrent(this.myCanvas);
			this.myCanvas.start();
		}
		if (c == this.recordCommand) {
			this.recordCallback2();
		}
		if (c == this.playCommand) {
			this.playCallback2();
		}
	}

	/**
	 * Skip the first 44 bytes of the (WAV header), Loop through every byte-pair
	 * (Short) in this.output. For each Short, accumulate the sum of val^2.
	 * Return sum/(this.output.length/2).
	 * 
	 * @return The noise level.
	 */
	public double getNoiseLevel() {
		long sum = 0;
		try {
			for (int i = 44; i < this.output.length; i += 2) {
				short val = SimpleTest.byteArrayToShort(this.output, i);
				sum += val * val;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (2.0 * sum) / this.output.length;
	}

	/**
	 * Callback for ChoiceGroup when it has a state change.
	 * 
	 * @see javax.microedition.lcdui.ItemStateListener#itemStateChanged(javax.microedition.lcdui.Item)
	 */
	public void itemStateChanged(Item item) {
		if (item.equals(this.myChoiceGroupActions)) {
			this.choiceGroupChanged();
		}
	}

	/**
	 * This UNUSED function is an example of how to playback an audio file.
	 */
	public void playCallback() {
		String SNDFILE = "file:///E:/audio.wav";
		FileConnection fconn = null;
		InputStream inStream = null;
		Player p = null;
		fconn = this.createFC(SNDFILE, false);
		if (fconn == null) {
			this.alertError("fconn was null");
			return;
		}
		try {
			inStream = fconn.openDataInputStream();
		} catch (IOException e) {
			this
					.alertError("Can't open input stream for:" + SNDFILE + "/n"
							+ e);
			return;
		}
		try {
			// create a datasource that captures live audio
			p = Manager.createPlayer(inStream, "audio/X-wav");
			p.start();
		} catch (IOException e) {
			this.alertError("IOException in createPlayer:" + e.getMessage());
		} catch (MediaException e) {
			this.alertError("MediaException in createPlayer:" + e.getMessage());
		}
	}

	/**
	 * Callback for PlayerListener.
	 * 
	 * @see javax.microedition.media.PlayerListener#playerUpdate(javax.microedition.media.Player,
	 *      java.lang.String, java.lang.Object)
	 */
	public void playerUpdate(Player p, String event, Object eventData) {
		try {
			if (event.compareTo("PAUSE") == 0) {
				playerUpdatePause();
			} else if (event.compareTo("START") == 0) {
				this.recordCallback2();
			}
		} catch (Exception e) {
			this.alertError("Exception in handing event:" + event + ":"
					+ e.getMessage());
		}
	}

	/**
	 * Helper function for playerUpdate, dispatched to if the event is "PAUSE".
	 * 
	 * @throws IOException
	 * @throws RecordStoreNotOpenException
	 * @throws RecordStoreException
	 * @throws RecordStoreFullException
	 */
	private void playerUpdatePause() throws IOException,
			RecordStoreNotOpenException, RecordStoreException,
			RecordStoreFullException {
		++this.samplesTaken;
		playerUpdateCommitAndClose();
		double noiseLevel = this.getNoiseLevel();
		playerUpdateCanvas(noiseLevel);
		playerUpdateFilter(noiseLevel);
		playerUpdateMaybeRecordAgain();
	}

	/**
	 * If the stopPlayer flag is not set, then set another time to record again.
	 */
	private void playerUpdateMaybeRecordAgain() {
		if (!this.stopPlayer) {
			// Set a timer callback.
			int sleepMS = this.myGaugeTotal.getValue()
					- this.myGaugeShared.getValue();
			if (sleepMS < 0) {
				sleepMS = 0;
			}
			this.myThread = new Thread(new SimpleTestHelper(this, sleepMS,
					"START"));
			this.myThread.start();
		}
	}

	/**
	 * Based on the noiseLevel, decide to save or drop the sample.
	 * 
	 * @param noiseLevel
	 * @throws RecordStoreNotOpenException
	 * @throws RecordStoreException
	 * @throws RecordStoreFullException
	 * @throws IOException
	 */
	private void playerUpdateFilter(double noiseLevel)
			throws RecordStoreNotOpenException, RecordStoreException,
			RecordStoreFullException, IOException {
		// If the noiseLevel is above the threshold (myCanvas.ave),
		// then save this.output to this.recordStore.
		if (noiseLevel > this.myCanvas.ave) {
			long timeMS = java.util.Calendar.getInstance().getTime().getTime();
			// This has the side-effect of writing to the recordStore.
			new SigSeg(this.recordStore, timeMS, this.output);
		}
	}

	/**
	 * Given a noiseLevel, update the canvas-relate state and repaint.
	 * 
	 * @param noiseLevel
	 */
	private void playerUpdateCanvas(double noiseLevel) {
		if (this.power.size() >= 30) {
			this.power.removeElementAt(0);
		}
		this.power.addElement(new Double(noiseLevel));
		this.myCanvas.repaint();
	}

	/**
	 * Commit and close the player.
	 * 
	 * @throws IOException
	 */
	private void playerUpdateCommitAndClose() throws IOException {
		// Close down the recorder.
		this.rc.commit();
		this.p.close();
		this.output = this.tempoutput.toByteArray();
		this.tempoutput.close();
	}

	/**
	 * Callback for RecordStore. It updates the text box with new stats.
	 * 
	 * @see javax.microedition.rms.RecordListener#recordAdded(javax.microedition.rms.RecordStore,
	 *      int)
	 */
	public void recordAdded(RecordStore recordStore, int recordID) {
		this.updateStringItem(recordStore, recordID);
	}

	/**
	 * Callback for RecordStore
	 * 
	 * @see javax.microedition.rms.RecordListener#recordChanged(javax.microedition.rms.RecordStore,
	 *      int)
	 */
	public void recordChanged(RecordStore recordStore, int recordID) {
		return;
	}

	/**
	 * Callback for RecordStore
	 * 
	 * @see javax.microedition.rms.RecordListener#recordDeleted(javax.microedition.rms.RecordStore,
	 *      int)
	 */
	public void recordDeleted(RecordStore recordStore, int recordID) {
		this.updateStringItem(recordStore, recordID);
	}

	/**
	 * Called from this.itemStatechanged, this is triggered when
	 * this.myChoiceGroupActions is the item that changed. Updates the stats.
	 */
	private void choiceGroupChanged() {
		int selectedIndex = this.myChoiceGroupActions.getSelectedIndex();
		String selectedStr = this.myChoiceGroupActions.getString(selectedIndex);
		if (selectedStr.equals("Record")) {
			this.recordCallback2();
		} else if (selectedStr.equals("Stop")) {
			this.stopPlayer = true;
			this.playerUpdate(null, "PAUSE", null);
		} else if (selectedStr.equals("Clear")) {
			try {
				RecordEnumeration recIter = this.recordStore.enumerateRecords(
						null, null, false);
				while (recIter.hasNextElement()) {
					int recId = recIter.nextRecordId();
					this.recordStore.deleteRecord(recId);
				}
			} catch (RecordStoreNotOpenException e) {
				e.printStackTrace();
			} catch (InvalidRecordIDException e) {
				e.printStackTrace();
			} catch (RecordStoreException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * All this code is to create a FileConnection. :P
	 * 
	 * @param arg
	 *            The name of the file to open.
	 * @param write
	 *            A boolean that indicates whether or not to open the file for
	 *            writing.
	 * @return A FileConnection object tied to the specified file.
	 */
	private FileConnection createFC(String arg, boolean write) {
		FileConnection fconn = null;
		try {
			fconn = (FileConnection) Connector.open(arg);
		} catch (IOException e) {
			this.alertError("Can't open file (bad URL):" + arg + "/n" + e);
			return null;
		}
		if (write) {
			if (fconn.exists()) {
				if (!fconn.canWrite()) {
					this.alertError("Can't write to existing file:" + arg);
				}
			} else {
				try {
					fconn.create();
				} catch (IOException e) {
					this.alertError("Can't create file:" + arg + "/n"
							+ e.getMessage());
					return null;
				}
			}
		}
		return fconn;
	}

	/**
	 * A helper of this.commandAction. This plays the data contained in
	 * this.output.
	 */
	private void playCallback2() {
		try {
			ByteArrayInputStream is = new ByteArrayInputStream(this.output);
			Player p = Manager.createPlayer(is, "audio/x-wav");
			p.start();
		} catch (MediaException me) {
		} catch (IOException io) {
		}
	}

	/**
	 * This is the callback for when the user selects "Record", or when the
	 * timer goes off and it's time to record again. It creates a player...
	 * starts recording... and creates a thread that will later stop the
	 * recording.
	 */
	private void recordCallback2() {
		// this.alertError("recordCallback2");
		try {
			this.stopPlayer = false;
			this.p = Manager.createPlayer("capture://audio?encoding=pcm");
			this.tempoutput = new ByteArrayOutputStream();
			this.p.realize();
			this.p.addPlayerListener(this);
			this.rc = (RecordControl) this.p.getControl("RecordControl");
			this.rc.setRecordStream(this.tempoutput);
			this.rc.startRecord();
			this.p.start();
			this.myThread = new Thread(new SimpleTestHelper(this,
					this.myGaugeShared.getValue(), "PAUSE"));
			this.myThread.start();
		} catch (IOException ioe) {
			this.alertError("IOException in recordCallback2:"
					+ ioe.getMessage());
		} catch (MediaException me) {
			this.alertError("MediaException in recordCallback2:"
					+ me.getMessage());
		}
	}

	// 
	/**
	 * This is a helper for RecordListener callbacks. It updates the state of
	 * this.myStringItem.
	 * 
	 * @param recordStore
	 *            This is the record store to use for the update.
	 * @param recordID
	 *            This is ignored.
	 */
	private void updateStringItem(RecordStore recordStore, int recordID) {
		try {
			int totalSaved = this.recordStore.getNumRecords();
			String numStr = String.valueOf(totalSaved);
			this.myStringItem.setText(numStr);
		} catch (RecordStoreNotOpenException e) {
		}
		return;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.midlet.MIDlet#destroyApp(boolean)
	 */
	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.midlet.MIDlet#pauseApp()
	 */
	protected void pauseApp() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.midlet.MIDlet#startApp()
	 */
	protected void startApp() throws MIDletStateChangeException {
		Display.getDisplay(this).setCurrent(this.myCanvas);
		this.myCanvas.start();
	}
}