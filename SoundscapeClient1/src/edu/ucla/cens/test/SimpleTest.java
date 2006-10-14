package edu.ucla.cens.test;

//import javax.microedition.lcdui.Command;
//import javax.microedition.lcdui.CommandListener;
//import javax.microedition.lcdui.Displayable;
//import javax.microedition.midlet.MIDlet;
//import javax.microedition.midlet.MIDletStateChangeException;

import java.lang.Thread;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
//import java.io.DataOutputStream;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import javax.microedition.media.*;
import javax.microedition.media.control.*;
import javax.microedition.rms.*;

import edu.ucla.cens.test.SigSeg;

public class SimpleTest extends MIDlet implements CommandListener, 
												  PlayerListener, 
												  ItemStateListener, 
												  RecordListener {
	
	private class SimpleTestHelper implements Runnable {
		private SimpleTest midlet = null;
		private int sleepMS = 0;
		String msg = null;
		SimpleTestHelper(SimpleTest midlet, int sleepMS, String msg) {
			this.midlet = midlet;
			this.sleepMS = sleepMS;
			this.msg = msg;
		}
		public void run() {
			try {
				Thread.sleep(this.sleepMS);
			}
			catch (InterruptedException ie) {
				midlet.alertError("InterruptedException while sleeping:"+ie.getMessage());
			}
			this.midlet.playerUpdate(null, msg, null);
		}
	}

	////////////////////////////
	// Acoustic Data
	public byte[] output = null; 
	public Vector power = new Vector();
	public RecordStore recordStore = null;
	public int samplesTaken = 0;

	///////////////////////////
	// Recording
	private Thread myThread = null;
	private Player p = null;
	private ByteArrayOutputStream tempoutput = null;
	private RecordControl rc = null;
	private boolean stopPlayer = true;

	///////////////////////////
	// UI Canvas: Sound Meter
	private HelloCanvas myCanvas;
		
	//////////////////////////
	// UI Form: Records
	private Form myForm;
	private Gauge myGaugeTotal;
	private Gauge myGaugeShared;
	private ChoiceGroup myChoiceGroupActions;
	private StringItem myStringItem;
	
	///////////////////////////
	// UI Menu Commands
	private Command backCommand = new Command("Back", Command.BACK, 1);
	private Command displayCommand = new Command("Meter", Command.SCREEN,1);
	private Command exitCommand = new Command("Exit", Command.EXIT, 1);
	private Command showCommand = new Command("Show Levels", Command.SCREEN, 1);
	private Command recordCommand = new Command("Record", Command.SCREEN, 1);
	private Command playCommand = new Command("Play", Command.SCREEN,1);

		
	// This is the constructor method.
	// It creates a Canvas and a Form object.
	public SimpleTest(){
		
		//////////////////////////////////////
		// Open the record store.
		try
		{
			this.recordStore = RecordStore.openRecordStore("data", true);
			this.recordStore.addRecordListener(this);
		}
		catch (RecordStoreNotFoundException e) {
			this.alertError("Error: RecordStore not found:" + e.getMessage());
		}
		catch (RecordStoreFullException e) {
			this.alertError("Error: RecordStore full:" + e.getMessage());
		}
		catch (RecordStoreException e) {
			this.alertError("Error: RecordStore Exception:" + e.getMessage());
		}
		
		///////////////////////////////////////////////////
		// UI Record Form - Record Info
		myForm = new Form("Record Info");
		// StringItem: # of Saved Samples 
		myStringItem = new StringItem ("Taken/Saved/Total Saved:", 
										String.valueOf(-1), 
										Item.PLAIN);
		this.updateStringItem(this.recordStore, -1);
		myForm.append(myStringItem);
		// ChoiceGroup: Actions
		myChoiceGroupActions = new ChoiceGroup("Actions:", ChoiceGroup.EXCLUSIVE);
		myChoiceGroupActions.append("Stop", null);
		myChoiceGroupActions.append("Record", null);
		myChoiceGroupActions.append("Clear", null);
		myForm.append(myChoiceGroupActions);
		// Gauges - Total Window Size | Shared Window Size
		myGaugeTotal = new Gauge("Total Window Size (ms):", true, 5000, 5000);
		myForm.append(myGaugeTotal);
		myGaugeShared = new Gauge("Shared Window Size (ms):", true, 5000, 4000);
		myForm.append(myGaugeShared);
		// Add commands.
		myForm.addCommand(showCommand);
		myForm.addCommand(displayCommand);
		myForm.addCommand(exitCommand);
		// Install Command and Item Listeners for Form.
		myForm.setCommandListener(this);
		myForm.setItemStateListener(this);
		
		///////////////////////////////////////////////////
		// UI Canvas (for the sound meter)
		myCanvas = new HelloCanvas(this);
		// Add commands.
		myCanvas.addCommand(backCommand);
		myCanvas.addCommand(playCommand);
		myCanvas.addCommand(recordCommand);
		// Install a Command Listener for the Canvas.
		myCanvas.setCommandListener(this);
		
	}
		
	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
	}

	protected void pauseApp() {
	}

	protected void startApp() throws MIDletStateChangeException {
		Display.getDisplay(this).setCurrent(myCanvas);
		myCanvas.start();
	}

	// Callback for the softkey menu
	public void commandAction(Command c, Displayable d) {
		if (c == exitCommand){
			notifyDestroyed();
		}
		if (c == backCommand){	
			Display.getDisplay(this).setCurrent(myForm);
		}	
		if (c == displayCommand){
			Display.getDisplay(this).setCurrent(myCanvas);
			myCanvas.start();
		}
		if (c == recordCommand) {
			recordCallback2(); 
		}
		if (c == playCommand) {
			playCallback2();
		}
	}
	
	// Callback for PlayerListener
	public void playerUpdate(Player p, String event, Object eventData)
	{
		try
		{
			if (event.compareTo("PAUSE") == 0 ) 
			{
				// Update application stats.
				++this.samplesTaken;
				// Close down the recorder.
				this.rc.commit();
				this.p.close();
				this.output = this.tempoutput.toByteArray();
				this.tempoutput.close();
				
				// Calculate the Noise level, save to this.power, and repaint.
				double noiseLevel = this.getNoiseLevel();
				if (this.power.size() >= 30)
				{
					this.power.removeElementAt(0);
				} 
				this.power.addElement(new Double(noiseLevel));
				this.myCanvas.repaint();
				
				// If the noiseLevel is above the threshold (myCanvas.ave), 
				// then save this.output to this.recordStore.
				if (noiseLevel > this.myCanvas.ave)
				{
					long timeMS = java.util.Calendar.getInstance().getTime().getTime();
					SigSeg sigseg = new SigSeg(timeMS, this.output);
					byte[] sigsegBA = sigseg.toByteArray();
					this.recordStore.addRecord(sigsegBA, 0, sigsegBA.length);
//					long time = java.util.Calendar.getInstance().getTime().getTime();
					// create an E:/soundscape/ directory
					// Writes to a file that looks like: "e:/soundscape/123213124124.wav
					// String fname = "file:///E:/soundscape/" + String.valueOf(time) + ".wav";
					// FileConnection fconn = this.createFC(fname, true);
//					
//					if (fconn == null)
//					{
//						this.alertError("fconn was null");
//					}
//					else
//					{
//						DataOutputStream dataOutputStream = fconn.openDataOutputStream();
//						dataOutputStream.write(this.output);
//						dataOutputStream.close();
//						fconn.close();
//					}
				}
				
				if (!this.stopPlayer)
				{
					// Set a timer callback.
					int sleepMS = this.myGaugeTotal.getValue() - this.myGaugeShared.getValue();
					if (sleepMS < 0) { sleepMS = 0; }
					myThread = new Thread(new SimpleTestHelper(this, sleepMS , "START"));
					myThread.start();
				}
			}
			else if (event.compareTo("START") == 0)
			{
				this.recordCallback2();
			}
		} catch (Exception e) {
			this.alertError("Exception in handing event:" + event + ":" +e.getMessage());
		}
	}

	// Callback for ChoiceGroup.
	public void itemStateChanged(Item item)
	{
		if (item.equals(this.myChoiceGroupActions))
		{
			this.choiceGroupChanged();
		}
	}
		
	private void choiceGroupChanged()
	{
		int selectedIndex = this.myChoiceGroupActions.getSelectedIndex();
		String selectedStr = this.myChoiceGroupActions.getString(selectedIndex);
		if (selectedStr.equals("Record"))
		{
			this.recordCallback2();
		}
		else if (selectedStr.equals("Stop"))
		{
			this.stopPlayer = true;
			this.playerUpdate(null, "PAUSE", null);
		}
		else if (selectedStr.equals("Clear"))
		{
			try 
			{
				RecordEnumeration recIter = this.recordStore.enumerateRecords(null, null, false);
				while (recIter.hasNextElement())
				{
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
	
	// Callback for RecordStore
	public void recordAdded(RecordStore recordStore, int recordID)
	{
		this.updateStringItem(recordStore, recordID);
	}
	
	// Callback for RecordStore
	public void recordChanged(RecordStore recordStore, int recordID)
	{
		return;
	}
	
	// Callback for RecordStore
	public void recordDeleted(RecordStore recordStore, int recordID)
	{
		this.updateStringItem(recordStore, recordID);
	}
	
	// This is a helper for RecordListener callbacks.
	private void updateStringItem(RecordStore recordStore, int recordID)
	{
		try 
		{ 
			int totalSaved = this.recordStore.getNumRecords();
			String numStr = String.valueOf(totalSaved);
			this.myStringItem.setText(numStr);
		}
		catch (RecordStoreNotOpenException e){}
		return;		
	}
	
	// All this code is to create a FileConnection. :P
	private FileConnection createFC(String arg, boolean write)
	{
		FileConnection fconn = null;
		try {
			fconn = (FileConnection)Connector.open(arg);
		} catch (IOException e) {
			this.alertError("Can't open file (bad URL):" + arg + "/n" + e);
			return null;
		}
		if(write) {
			if(fconn.exists()) {
				if(! fconn.canWrite()) {
					this.alertError("Can't write to existing file:" + arg);
				}
			} else {
				try {
					fconn.create();
				} catch (IOException e) {
					this.alertError("Can't create file:" + arg + "/n" + e.getMessage());
					return null;				
				}				
			}
		}
		return fconn;
	}
	
	// This is the callback for when the user selects "Record".
	// It creates a player...
	// starts recording...
	// and creates a thread that will later stop the recording.
	private void recordCallback2()
	{
		//this.alertError("recordCallback2");
		try
		{
			stopPlayer = false;
			p = Manager.createPlayer("capture://audio?encoding=pcm");
			tempoutput = new ByteArrayOutputStream();
			p.realize();
			p.addPlayerListener(this);
			rc = (RecordControl)p.getControl("RecordControl");
			rc.setRecordStream(tempoutput);
			rc.startRecord();
			p.start();
			myThread = new Thread(new SimpleTestHelper(this, this.myGaugeShared.getValue(), "PAUSE"));
			myThread.start();
		} catch (IOException ioe) {
			this.alertError("IOException in recordCallback2:" + ioe.getMessage());
		} catch (MediaException me) {
			this.alertError("MediaException in recordCallback2:" + me.getMessage());
		} 
	}

	private void playCallback2()
	{
		try
		{
			ByteArrayInputStream is = new ByteArrayInputStream(this.output);
			Player p = Manager.createPlayer(is, "audio/x-wav");
			p.start();
		}
		catch (MediaException me) {}
		catch (IOException io) {}
	}
	
	public void alertError(String message)
    {
        Alert alert = new Alert("Error", message, null, AlertType.ERROR);
        Display display = Display.getDisplay(this);
        Displayable current = display.getCurrent();
        if (!(current instanceof Alert))
        {
            // This next call can't be done when current is an Alert
            display.setCurrent(alert, current);
        }
    }

	// Skipping the first 44 bytes (WAV header)
	// Loop through every byte-pair (Short) in this.output.
	// For each Short, accumulate the sum of val^2.
	// Return sum / (this.output.length/2)
	public double getNoiseLevel()
	{
		long sum = 0;
		try {
			for (int i = 44; i < this.output.length; i += 2)
			{
				short val = SimpleTest.byteArrayToShort(this.output, i);
				sum += val * val;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (2.0 * sum)/this.output.length;
	}
	 /**
     * Convert the byte array to an int starting from the given offset.
     *
     * @param b The byte array
     * @param offset The array offset
     * @return The integer
     */
    public static int byteArrayToInt(byte[] b, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            //int shift = (bytewidth - 1 - i) * 8;
        	int shift = i * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }
    public static short byteArrayToShort(byte[] b, int offset) {
        short value = 0;
        for (int i = 0; i < 2; i++) {
        	int shift = i * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

    ////////////////////////////////////////////////////////////////////////
	// This UNUSED function is an example of how to playback an audio file.
    ////////////////////////////////////////////////////////////////////////
	public void playCallback()
	{
		String SNDFILE = "file:///E:/audio.wav";
		FileConnection fconn = null;
		InputStream inStream = null;
		Player p = null;		
		fconn = this.createFC(SNDFILE, false);
		if (fconn == null)
		{
			this.alertError("fconn was null");
			return;
		}		
		try {
			inStream = fconn.openDataInputStream();
		} 
		catch (IOException e) {
			this.alertError("Can't open input stream for:" + SNDFILE + "/n" + e);
			return;				
		}
		try
		{
			// create a datasource that captures live audio
			p = Manager.createPlayer(inStream, "audio/X-wav");
			p.start();
		} catch (IOException e) {
			this.alertError("IOException in createPlayer:" + e.getMessage());
		} catch (MediaException e) {
			this.alertError("MediaException in createPlayer:" + e.getMessage());
		}
	}
}