# Sasank Reddy
# sasank@ee.ucla.edu
# -------------------

# This program is designed to get a sample of sound data from the phone.
# Then the program will get the power value from the sample.  Finally, this information
# will be logged to SensorBase.

import appuifw, e32, sys, os, struct, math, audio, time, sysinfo, urllib, httplib, thread, camera, graphics

#photoCaptured = camera.take_photo('RGB', (1280, 960), 'auto', 1, 'auto', 'auto', 0)
photoCaptured = camera.take_photo('RGB', (1280, 960), 0, 'auto', 'auto', 'auto', 0)

print camera.image_modes()
print camera.image_sizes()
print camera.flash_modes()
print camera.max_zoom()
print camera.exposure_modes()
print camera.white_balance_modes() 
#photoCaptured = camera.take_photo()

photoName = unicode("E:\\Others\\")
photoName += unicode(time.strftime("%Y-%m-%d %H-%M-%S", time.localtime()))
photoName += unicode(".jpg")
photoCaptured.save(photoName, None, 'JPEG', 100, 24, 'no')

