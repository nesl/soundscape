package edu.ucla.cens.test;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.location.LocationProvider;

// Has a handle to the parent.
// There's a menu command that calls the parent's getCoordinates 
// and then displays all the info in a TextBox.
public class GPSScreen implements CommandListener {

//	public Command updateDisplayCommand = new Command("* Update Display",
//			Command.SCREEN, 1);

	public Command recordScreenCommand = new Command("-> Record Screen",
			Command.SCREEN, 1);

	public Command uploadScreenCommand = new Command("->Upload Screen",
			Command.SCREEN, 1);

	public Command exitCommand = new Command("Exit", Command.EXIT, 1);

	public TextBox textBox = new TextBox("GPS Details",
			"Please go to Record Screen and select \"Start Recording\"  to see GPS Data.", 9999,
			TextField.UNEDITABLE);

	private SimpleTest midlet = null;

	public GPSScreen(SimpleTest midlet) {
		this.midlet = midlet;
		this.textBox.addCommand(recordScreenCommand);
		this.textBox.addCommand(uploadScreenCommand);
		//this.textBox.addCommand(updateDisplayCommand);
		this.textBox.addCommand(exitCommand);
		this.textBox.setCommandListener(this);
	}

	public void commandAction(Command c, Displayable d) {
//		if (c == this.updateDisplayCommand) {
//			this.updateDisplayCB();
//		} else 
		if (c == this.recordScreenCommand) {
			Display.getDisplay(this.midlet).setCurrent(this.midlet.myForm);
		} else if (c == this.uploadScreenCommand) {
			Display.getDisplay(this.midlet).setCurrent(
					this.midlet.myUpload.form);
		} else if (c == this.exitCommand) {
			this.midlet.notifyDestroyed();
		}
	}

	public void updateDisplayCB() {
		// call my parent's getCoordinates command.
		// Unpack it and update textBox.
		// TODO how do I make sure the screen gets refreshed?
		Boolean isValid = new Boolean(false);
		Integer lpstate = new Integer(LocationProvider.OUT_OF_SERVICE);
		Float lat = new Float(Float.NaN);
		Float lon = new Float(Float.NaN);
		Float alt = new Float(Float.NaN);
		Float horizontal_accuracy = new Float(Float.NaN);
		Float vertical_accuracy = new Float(Float.NaN);
		Float course = new Float(Float.NaN);
		Float speed = new Float(Float.NaN);
		Long timestamp = new Long(0);
		StringBuffer buf = new StringBuffer();
		
		//this.midlet.getCoordinates();
		
		// Gather information.
		if (this.midlet.lp != null) {
			lpstate = new Integer(this.midlet.lp.getState());
		}
		if (this.midlet.location != null) {
			isValid = new Boolean(this.midlet.location.isValid());
			course = new Float(this.midlet.location.getCourse());
			speed = new Float(this.midlet.location.getSpeed());
			timestamp = new Long(this.midlet.location.getTimestamp());
		}
		if (this.midlet.coordinates != null) {
			lat = new Float(this.midlet.coordinates.getLatitude());
			lon = new Float(this.midlet.coordinates.getLongitude());
			alt = new Float(this.midlet.coordinates.getAltitude());
			horizontal_accuracy = new Float(this.midlet.coordinates.getHorizontalAccuracy());
			vertical_accuracy = new Float(this.midlet.coordinates.getVerticalAccuracy());
		}
		
		// Convert everything to a string...
		buf.append("timestamp: " + this.toDateStr(timestamp.longValue()));
		buf.append("\nisValid:\t" + isValid.toString());
		buf.append("\nState:\t");
		
		switch(lpstate.intValue()) {
			case LocationProvider.AVAILABLE:
				buf.append("AVAILABLE");
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				buf.append("TEMPORARILY_UNAVAILABLE");
				break;
			case LocationProvider.OUT_OF_SERVICE:
				buf.append("OUT_OF_SERVICE");
				break;
			}
		
		buf.append("\nlat:\t" + lat.toString());
		buf.append("\nlon:\t" + lon.toString());
		buf.append("\nalt(m):\t" + alt.toString());
		buf.append("\nh_acc(m): " + horizontal_accuracy.toString());
		buf.append("  v_acc(m): " + vertical_accuracy.toString());
		buf.append("\ncourse: " + course.toString());
		buf.append("\tspeed(m/s): " + speed.toString());
		
		// Update the display.
		this.textBox.setString(buf.toString());
		return;
	}
	
	private String toDateStr(long timeMS) {
		java.util.Date date = new java.util.Date(timeMS);
		java.util.Calendar cal = java.util.Calendar.getInstance();
		cal.setTime(date);
		String year = String.valueOf(cal.get(java.util.Calendar.YEAR));
		String month = String.valueOf(cal.get(java.util.Calendar.MONTH));
		month = this.maybePrependZero(month);
		String day = String.valueOf(cal.get(java.util.Calendar.DAY_OF_MONTH));
		day = this.maybePrependZero(day);
		String hour = String.valueOf(cal.get(java.util.Calendar.HOUR_OF_DAY));
		hour = this.maybePrependZero(hour);
		String minute = String.valueOf(cal.get(java.util.Calendar.MINUTE));
		minute = this.maybePrependZero(minute);
		String second = String.valueOf(cal.get(java.util.Calendar.SECOND));
		second = this.maybePrependZero(second);
		String str_date = year + "-" + month + "-" + day + " " + hour + ":"
				+ minute + ":" + second;
		return str_date;
	}
	
	private String maybePrependZero(String s) {
		if (s.length() == 1) {
			s = "0" + s;
		}
		return s;
	}
}
