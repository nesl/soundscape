# Sasank Reddy
# sasank@ee.ucla.edu
# -------------------

# This program is designed to get a sample of sound data from the phone.
# Then the program will get the power value from the sample.  Finally, this information
# will be logged to SensorBase.

import appuifw, e32, sys, os, struct, math, audio, time, sysinfo, urllib, httplib, thread, camera, graphics
from key_codes import EKeySelect
sys.path.append("E:\\Python")

appLock = e32.Ao_lock()

class imageTagger:

	def initTabs(self):
		self.imagerOn = 0
		contextTab = u'Context'
		imageTab = u'Image'
		tabList = [contextTab, imageTab]

		appuifw.app.set_tabs(tabList, self.handleTabs)
	
		# Some Extra Inits
		self.displayGUI()

	def handleTabs(self, tabIndex):
		if tabIndex == 0:
			if(self.imagerOn == 1):
				camera.stop_finder()
				self.imagerOn = 0		
			self.displayGUI()
		elif tabIndex == 1:
			self.initImager()
		else:
			self.displayGUI()

	def initImager(self):

		if(self.captureImage == 1):
	                textScreen = appuifw.Text()
                	textScreen.font = 'normal'
			msgText = u'You are currently capturing images.'
			msgText += u'The view finder cannot be shown until you stop capturing'
			msgText += u' and go back to the image screen.'
                	textScreen.add(msgText)
                	appuifw.app.body = textScreen
		else:
			self.imagerOn = 1
			def cameraBuffer(image):
				appuifw.app.body.blit(image)

			appuifw.app.body = appuifw.Canvas()
			camera.start_finder(cameraBuffer) 

	def captureImageLoop(self):
		if self.captureImage == 1:
			if ((self.userName != u'None') and (self.userName != u'') and (self.locationTag != u'None') and (self.activityTag != u'None') and (self.imagerOn == 0)):
				photoCaptured = camera.take_photo('RGB', (1280, 960), 0, 'auto', 'auto', 'auto', 0)
				photoName = unicode("E:\\Images\\")
				photoName += unicode(self.userName) + unicode("_")
				photoName += unicode(self.locationTag) + unicode("_")
				photoName += unicode(self.activityTag) + unicode("_")
				photoName += unicode(time.strftime("%Y-%m-%d_%H-%M-%S", time.localtime()))
				photoName += unicode(".jpg")
				photoCaptured.save(photoName, None, 'JPEG', 100, 24, 'no')
			e32.ao_sleep(0, self.captureImageLoop)
		
	def startImageCapture(self):
		self.captureImage = 1
		appuifw.app.menu = [(u"Stop Capture", self.stopImageCapture)]
		self.captureImageLoop()

	def stopImageCapture(self):
		self.captureImage = 0
                appuifw.app.menu = [(u"Start Capture", self.startImageCapture)]
		
	def initGUI(self):

		title = u'UCLA ImageTagger'
		intro = u'Welcome.  Today we will be documenting your day.'
		intro += u'We will take pictures and tag them with your location and activity.'
		cont = u'Press the center joystick button to start.'
			
		textScreen = appuifw.Text()
		plainStyle = textScreen.style
		textScreen.font = 'title'
		textScreen.style = appuifw.STYLE_BOLD
		textScreen.add(title + '\n\n')
		textScreen.font = 'normal'
		textScreen.style = plainStyle
		textScreen.add(intro + '\n\n')
		textScreen.add(cont)
		textScreen.bind(EKeySelect, self.initTabs)
		appuifw.app.body = textScreen
	
		self.captureImage = 0
                self.userName = u'None'
                self.locationTag = u'None'
                self.activityTag = u'None'

		appuifw.app.menu = [(u"Start Capture", self.startImageCapture)]

	def displayGUI(self):

	        qName = u'Name : ' + self.userName
        	qLocation = u'Location : ' + self.locationTag
	        qActivity = u'Activity : ' + self.activityTag

	        qList = [qName, qLocation, qActivity]

	        def displayQuestion():
		        currentIndex = qListBox.current()
                	currentItem = qList[currentIndex]
                	if currentItem[0] == qName[0]:
	                	self.userName = appuifw.query(u'Please tell us your name.', 'text')
				qList[currentIndex] = u'Name : ' + self.userName
				qListBox.set_list(qList)
                	elif currentItem[0] == qLocation[0]:
                        	locationTags = [u'Home', u'CENS', u'UCLA', u'Santa Monica', u'Westwood', u'Brentwood']
                        	locationTags.sort()
                        	locationTagIndex = appuifw.popup_menu(locationTags, u"Select your location and press ok.")
                        	if(locationTagIndex in range(0, 6)):
                        		self.locationTag = locationTags[locationTagIndex]
					qList[currentIndex] = u'Location : ' + self.locationTag
					qListBox.set_list(qList)
                	elif currentItem[0] == qActivity[0]:
                        	activityTags = [u'Working', u'Playing', u'Walking', u'Shopping', u'Eating', u'Relaxing', u'Reading', u'Sleeping', u'Talking', u'Watching']
                        	activityTags.sort()
                        	activityTagIndex = appuifw.popup_menu(activityTags, u"Select your activity and press ok.")
                        	if(activityTagIndex in range(0, 7)):
                        		self.activityTag = activityTags[activityTagIndex]
					qList[currentIndex] = u'Activity : ' + self.activityTag
					qListBox.set_list(qList)

		qListBox = appuifw.Listbox(qList)
        	qListBox.bind(EKeySelect, displayQuestion)
	        appuifw.app.body = qListBox
		

	def slog(self):
		date_time = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())

	        xml = '<?xml version="1.0" encoding="UTF-8"?>'
        	xml += '<table>'
        	xml += '<row>'
        	xml += '<field name="Audio">' + str(audioDec) + '</field>'
        	xml += '<field name="DateTime">' + date_time + '</field>'
        	xml += '</row>'
        	xml += '</table>'

	        params = {}
        	params['email']='sasank@ee.ucla.edu'
	        params['pw']='intel'
        	params['data_string']=xml
        	params['type']='xml'
       		params['project_id']="64"
        	params['tableName']='testAudioLife'

	        params = urllib.urlencode(params)

	        print "Encoded Data"

	        headers = {}
        	headers['Content-type']='application/x-www-form-urlencoded'
	        headers['Accept']='text/plain'

	        conn = httplib.HTTPConnection("sensorbase.org")
        	conn.request("POST", "/alpha/upload.php", params, headers)
	        response = conn.getresponse()
        	responseText = response.read()
        	conn.close()
                
        	print "Posted Data"


# Main loop

it = imageTagger()                
it.initGUI()
appLock.wait()
