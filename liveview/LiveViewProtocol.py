import struct
import time
import datetime
import sys

MSG_GETCAPS		= 1
MSG_GETCAPS_ACK 	= 2

MSG_DISPLAYTEXT		= 3
MSG_DISPLAYTEXT_ACK	= 4

MSG_DISPLAYPANEL	= 5
MSG_DISPLAYPANEL_ACK	= 6

MSG_DEVICESTATUS	= 7
MSG_DEVICESTATUS_ACK 	= 8

MSG_DISPLAYBITMAP	= 19
MSG_DISPLAYBITMAP_ACK	= 20

MSG_CLEARDISPLAY  	= 21
MSG_CLEARDISPLAY_ACK 	= 22

MSG_SETMENUSIZE		= 23
MSG_SETMENUSIZE_ACK	= 24

MSG_GETMENUITEM		= 25
MSG_GETMENUITEM_ACK	= 26

MSG_GETALERT		= 27

MSG_NAVIGATION		= 29
MSG_NAVIGATION_ACK	= 30

MSG_SETSTATUSBAR	= 33
MSG_SETSTATUSBAR_ACK	= 34

MSG_GETMENUITEMS	= 35

MSG_SETMENUSETTINGS  	= 36
MSG_SETMENUSETTINGS_ACK = 37

MSG_GETTIME		= 38
MSG_GETTIME_ACK		= 39

MSG_SETLED 		= 40
MSG_SETLED_ACK 		= 41

MSG_SETVIBRATE 		= 42
MSG_SETVIBRATE_ACK 	= 43

MSG_ACK			= 44

MSG_SETSCREENMODE	= 64
MSG_SETSCREENMODE_ACK	= 65

MSG_GETSCREENMODE	= 66
MSG_GETSCREENMODE_ACK	= 67

DEVICESTATUS_OFF	= 0
DEVICESTATUS_ON		= 1
DEVICESTATUS_MENU	= 2

RESULT_OK		= 0
RESULT_ERROR		= 1
RESULT_OOM		= 2
RESULT_EXIT		= 3
RESULT_CANCEL		= 4

NAVACTION_PRESS		= 0
NAVACTION_LONGPRESS	= 1
NAVACTION_DOUBLEPRESS	= 2

NAVTYPE_UP		= 0
NAVTYPE_DOWN		= 1
NAVTYPE_LEFT		= 2
NAVTYPE_RIGHT		= 3
NAVTYPE_SELECT		= 4
NAVTYPE_MENUSELECT	= 5

BRIGHTNESS_OFF		= 48
BRIGHTNESS_DIM		= 49
BRIGHTNESS_MAX		= 50

def DecodeLVMessage(msg):
	(messageId, headerLen, payloadLen) = struct.unpack(">BBL", msg[0:6])
	payload = msg[2 + headerLen:]

	if headerLen != 4:
		raise Exception("Unexpected header length %i" % headerLen)
	if payloadLen != len(payload):
		i = 0
		for x in msg:
			print >>sys.stderr, "\t%02x: %02x" % (i, ord(x))
			i += 1
		raise Exception("Payload length is not as expected %i != %i", (payloadLen, len(payload)))
	
	return (messageId, payload)

def Decode(msg):
	(messageId, payload) = DecodeLVMessage(msg)
	if messageId == MSG_GETCAPS_ACK:
		return DisplayCapabilities(messageId, payload)
	elif messageId == MSG_SETLED_ACK:
		return Result(messageId, payload)
	elif messageId == MSG_SETVIBRATE_ACK:
		return Result(messageId, payload)
	elif messageId == MSG_DEVICESTATUS_ACK:
		return Result(messageId, payload)
	elif messageId == MSG_SETSCREENMODE_ACK:
		return Result(messageId, payload)
	elif messageId == MSG_CLEARDISPLAY_ACK:
		return Result(messageId, payload)
	elif messageId == MSG_SETSTATUSBAR_ACK:
		return Result(messageId, payload)
	elif messageId == MSG_DISPLAYTEXT_ACK:
		return Result(messageId, payload)
	elif messageId == MSG_DISPLAYBITMAP_ACK:
		return Result(messageId, payload)
	elif messageId == MSG_DISPLAYPANEL_ACK:
		return Result(messageId, payload)
	elif messageId == MSG_GETMENUITEMS:
		return GetMenuItems(messageId, payload)
	elif messageId == MSG_GETMENUITEM:
		return GetMenuItem(messageId, payload)
	elif messageId == MSG_GETTIME:
		return GetTime(messageId, payload)
	elif messageId == MSG_GETALERT:
		return GetAlert(messageId, payload)
	elif messageId == MSG_DEVICESTATUS:
		return DeviceStatus(messageId, payload)
	elif messageId == MSG_NAVIGATION:
		return Navigation(messageId, payload)
	elif messageId == MSG_GETSCREENMODE_ACK:
		return GetScreenMode(messageId, payload)
	else:
		print >>sys.stderr, "Unknown message id %i" % messageId
		i = 0
		for x in payload:
			print >>sys.stderr, "\t%02x: %02x" % (i, ord(x))
			i += 1

def EncodeLVMessage(messageId, data):
	return struct.pack(">BBL", messageId, 4, len(data)) + data

def EncodeGetCaps():
	return EncodeLVMessage(MSG_GETCAPS, struct.pack(">B5s", 5, "0.0.3"))

def EncodeSetVibrate(delayTime, onTime):
	return EncodeLVMessage(MSG_SETVIBRATE, struct.pack(">HH", delayTime, onTime))

def EncodeSetLED(r, g, b, delayTime, onTime):
	return EncodeLVMessage(MSG_SETLED, struct.pack(">HHH", ((r & 0x31) << 10) | ((g & 0x31) << 5) | (b & 0x31), delayTime, onTime))

def EncodeSetMenuSize(menuSize):
	return EncodeLVMessage(MSG_SETMENUSIZE, struct.pack(">B", menuSize))

def EncodeAck(ackMessageId):
	return EncodeLVMessage(MSG_ACK, struct.pack(">B", ackMessageId))

def EncodeGetMenuItemAck(menuItemId, isAlertItem, unreadCount, itemDescription, itemBitmap):
	payload = struct.pack(">BHHHBB", not isAlertItem, 
					 0,			# unused total alert count 
					 unreadCount, 
					 0, 			# unused current alert index
					 menuItemId + 3,	# this needs +3 added on here for some reason.
					 0)			# final 0 is for plaintext vs bitmapimage (1) strings
	payload += struct.pack(">H", 0) 			# unused string
	payload += struct.pack(">H", 0) 			# unused string
	payload += struct.pack(">H", len(itemDescription)) + itemDescription
	payload += itemBitmap

	return EncodeLVMessage(MSG_GETMENUITEM_ACK, payload)

def EncodeDisplayPanel(topText, bottomText, bitmap, alertUser):

	id = 80
	if not alertUser:
		id |= 1

	payload = struct.pack(">BHHHBB", 0, 0, 0, 0, id, 0)	# final 0 is for plaintext vs bitmapimage (1) strings
	payload += struct.pack(">H", len(topText)) + topText
	payload += struct.pack(">H", 0) 			# unused string
	payload += struct.pack(">H", len(bottomText)) + bottomText
	payload += bitmap

	return EncodeLVMessage(MSG_DISPLAYPANEL, payload)

def EncodeDisplayBitmap(x, y, bitmap):
	# Only works if you have sent SetMenuItems(0)
	# Meaning of byte 2 is unknown, but /is/ important!
	return EncodeLVMessage(MSG_DISPLAYBITMAP, struct.pack(">BBB", x, y, 1) + bitmap)

def EncodeSetStatusBar(menuItemId, unreadAlerts, itemBitmap):
	# Note that menu item#0 is treated specially if you have non-zero unreadAlerts...
	# Its value will be automatically updated from the other menu items... e.g. if item #3 currently has 20, and is changed to 200 with this call, item#0 will automatically be set to 180 (200-20). Slightly annoying!

	payload = struct.pack(">BHHHBB", 0, 0, unreadAlerts, 0, menuItemId + 3, 0)
	payload += struct.pack(">H", 0)
	payload += struct.pack(">H", 0)
	payload += struct.pack(">H", 0)
	payload += itemBitmap
	
	return EncodeLVMessage(MSG_SETSTATUSBAR, payload)

def EncodeGetTimeAck(time, is24HourDisplay):
	return EncodeLVMessage(MSG_GETTIME_ACK, struct.pack(">LB", time, not is24HourDisplay))

def EncodeDeviceStatus(deviceStatus):
	return EncodeLVMessage(MSG_DEVICESTATUS, struct.pack(">B", deviceStatus))

def EncodeDeviceStatusAck():
	return EncodeLVMessage(MSG_DEVICESTATUS_ACK, struct.pack(">B", RESULT_OK))

def EncodeNavigationAck(result):
	return EncodeLVMessage(MSG_NAVIGATION_ACK, struct.pack(">B", result))

def EncodeSetScreenMode(brightness, auto):
	# Only works if you have sent SetMenuItems(0)
	v = brightness << 1
	if auto:
		v |= 1
	return EncodeLVMessage(MSG_SETSCREENMODE, struct.pack(">B", v))

def EncodeGetScreenMode():
	# Only works if you have sent SetMenuItems(0)
	return EncodeLVMessage(MSG_GETSCREENMODE, "")

def EncodeClearDisplay():
	# Only works if you have sent SetMenuItems(0)
	return EncodeLVMessage(MSG_CLEARDISPLAY, "")

def EncodeSetMenuSettings(vibrationTime, fontSize, initialMenuItemId):
	# vibrationTime is in units of approximately 100ms
	# This message is never acked for some reason. 

	# what does fontSize control? changing it doesn't seem to have any effect...

	return EncodeLVMessage(MSG_SETMENUSETTINGS, struct.pack(">BBB", vibrationTime, fontSize, initialMenuItemId))





def EncodeUIPayload(isAlertItem, totalAlerts, unreadAlerts, curAlert, menuItemId, top, mid, body, itemBitmap):
	# FIXME: not quite sure of all this yet
	# byte 00: flag set to 1 if icon is a normal menu item

	# byte 01: } total alerts
	# byte 02: }

	# byte 03: } unread alerts count
	# byte 04: }

	# byte 05: } alert index ???
	# byte 06: }

	# byte 07: itemId (for menu item, set to 0 for alert msg 28)

	# byte 08: flag set to 0 if string data is plain text, or 1 if they're "Simple Image Format" images (if they require non iso-8859-1 characters)
	
	# byte: length of timestamp string
	# byte:
	# <timestamp string>

	# byte: length of header string
	# byte:
	# <header string>

	# byte : length of body string
	# byte : 
	# <body string>	
	
	# message type 28 (get alert ack) has some extra fields here
	# byte: ? unknown
	# byte: ? part of PNG length?
	# byte: ? part of PNG length?
	# byte: } definitely length of following PNG.
	# byte: }


	# <PNG data for menu item>
	
	payload = struct.pack(">BHHHBB", not isAlertItem, totalAlerts, unreadAlerts, curAlert, menuItemId, 0)
	payload += struct.pack(">H", len(top)) + top
	payload += struct.pack(">H", len(mid)) + mid
	payload += struct.pack(">H", len(body)) + body
	payload += itemBitmap
	
	return payload

def EncodeDisplayText(s):
	# Only works if you have sent SetMenuItems(0)
	# FIXME: doesn't seem to do anything
	# meaning of 0 byte is unknown...
	return EncodeLVMessage(MSG_DISPLAYTEXT, struct.pack(">B", 0) + s)








class DisplayCapabilities:
	
	def __init__(self, messageId, msg):
		self.messageId = messageId
		(self.width, self.height, self.statusBarWidth, self.statusBarHeight, self.viewWidth, self.viewHeight, self.announceWidth, self.announceHeight, self.textChunkSize, self.idleTimer) = struct.unpack(">BBBBBBBBBB", msg[0:10])
		self.softwareVersion = msg[10:]
	
	def __str__(self):
		return "<DisplayCapabilities>\nWidth %i\nHeight %i\nStatusBarWidth %i\nStatusBarHeight %i\nViewWidth %i\nViewHeight %i\nAnnounceWidth %i\nAnnounceHeight %i\nTextChunkSize %i\nIdleTimer %i\nSoftware Version: %s" % (self.width, self.height, self.statusBarWidth, self.statusBarHeight, self.viewWidth, self.viewHeight, self.announceWidth, self.announceHeight, self.textChunkSize, self.idleTimer, self.softwareVersion)

class Result:

	def __init__(self, messageId, msg):
		self.messageId = messageId
		(self.code, ) = struct.unpack(">B", msg)
		if self.code > RESULT_CANCEL:
			print >>sys.stderr, "Result with unknown code %i" % self.code

	def __str__(self):
		s = "UNKNOWN"
		if self.code == RESULT_OK:
			s = "OK"
		elif self.code == RESULT_ERROR:
			s = "ERROR"
		elif self.code == RESULT_OOM:
			s = "OOM"
		elif self.code == RESULT_EXIT:
			s = "EXIT"
		elif self.code == RESULT_CANCEL:
			s = "CANCEL"

		return "<Result>\nMessageId: %i\nCode: %i (%s)" % (self.messageId, self.code, s)

class GetMenuItem:

	def __init__(self, messageId, msg):
		self.messageId = messageId
		(self.menuItemId, ) = struct.unpack(">B", msg)
		
		# FIXME: subtract 3 from menu item id?

	def __str__(self):
		return "<GetMenuItem>\nMenuItemId: %i" % (self.menuItemId)

class GetMenuItems:

	def __init__(self, messageId, msg):
		self.messageId = messageId
		(self.unknown, ) = struct.unpack(">B", msg)
		if self.unknown != 0:
			print >>sys.stderr, "GetMenuItems with non-zero unknown byte %i" % self.unknown

	def __str__(self):
		return "<GetMenuItems>\nUnknown: %i" % (self.unknown)

class GetTime:
	
	def __init__(self, messageId, msg):
		self.messageId = messageId
		(self.unknown, ) = struct.unpack(">B", msg)
		if self.unknown != 0:
			print >>sys.stderr, "GetTime with non-zero unknown byte %i" % self.unknown

	def __str__(self):
		return "<GetTime>\nUnknown: %i" % (self.unknown)

class DeviceStatus:
	
	def __init__(self, messageId, msg):
		self.messageId = messageId
		(self.deviceStatus, ) = struct.unpack(">B", msg)
		if self.deviceStatus > 2:
			print >>sys.stderr, "DeviceStatus with unknown value %i" % self.deviceStatus

	def __str__(self):
		s = "UNKNOWN"
		if self.deviceStatus == DEVICESTATUS_OFF:
			s = "Off"
		elif self.deviceStatus == DEVICESTATUS_ON:
			s = "On"
		elif self.deviceStatus == DEVICESTATUS_MENU:
			s = "Menu"

		return "<DeviceStatus>\nStatus: %i (%s)" % (self.deviceStatus, s)

class GetAlert:
	
	def __init__(self, messageId, msg):
		self.messageId = messageId
		(self.menuItemId, self.textLength, self.maxLineBreakSize, self.fontSize, self.textImageWidth, self.textImageHeight) = struct.unpack(">BHBBBB", msg)
		if self.maxLineBreakSize != 0 or self.fontSize != 0  or self.textImageWidth != 0  or self.textImageHeight != 0:
			print >>sys.stderr, "GetAlert with non zero text values! %i %i %i %i" % (self.maxLineBreakSize, self.fontSize, self.textImageWidth, self.textImageHeight)
		
	def __str__(self):
		return "<GetAlert>\nMenuItemId %i\nTextLength %i\nMaxLineBreakSize %i\nFontSize %i\nTextImageWidth %i\nTextImageHeight %i" % (self.menuItemId, self.textLength, self.maxLineBreakSize, self.fontSize, self.textImageWidth, self.textImageHeight)

class Navigation:

	def __init__(self, messageId, msg):
		self.messageId = messageId
		(byte0, byte1, navigation, self.menuItemId, menuId) = struct.unpack(">BBBBB", msg)
		if byte0 != 0:
			print >>sys.stderr, "Navigation with unknown byte0 value %i" % byte0
		if byte1 != 3:
			print >>sys.stderr, "Navigation with unknown byte1 value %i" % byte1
		if menuId != 10:
			print >>sys.stderr, "Navigation with unexpected menuId value %i" % menuId
		if (navigation != 32) and ((navigation < 1) or (navigation > 15)):
			print >>sys.stderr, "Navigation with out of range value %i" % navigation

		if navigation != 32:
			self.navAction = (navigation - 1) % 3
			self.navType = int((navigation - 1) / 3)
		else:
			self.navAction = NAVACTION_PRESS
			self.navType = NAVTYPE_MENUSELECT

	def __str__(self):
		
		sA = "UNKNOWN"
		if self.navAction == NAVACTION_PRESS:
			sA = "Press"
		elif self.navAction == NAVACTION_DOUBLEPRESS:
			sA = "DoublePress"
		elif self.navAction == NAVACTION_LONGPRESS:
			sA = "LongPress"

		sT = "UNKNOWN"
		if self.navType == NAVTYPE_UP:
			sT = "Up"
		elif self.navType == NAVTYPE_DOWN:
			sT = "Down"
		elif self.navType == NAVTYPE_LEFT:
			sT = "Left"
		elif self.navType == NAVTYPE_RIGHT:
			sT = "Right"
		elif self.navType == NAVTYPE_SELECT:
			sT = "Select"
		elif self.navType == NAVTYPE_MENUSELECT:
			sT = "MenuSelect"

		return "<Navigation>\nAction %s\nType %s\nMenuItemId %i" % (sA, sT, self.menuItemId)

class GetScreenMode:

	def __init__(self, messageId, msg):
		self.messageId = messageId
		(raw, ) = struct.unpack(">B", msg)
		self.auto = raw & 1
		self.brightness = raw >> 1

	def __str__(self):
		return "<GetScreenMode>\nAuto: %i\nBrightness: %i" % (self.auto, self.brightness)
