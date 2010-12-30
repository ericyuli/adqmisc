import struct
import time
import datetime
import sys

MSG_GETCAPS		= 1
MSG_GETCAPS_ACK 	= 2

MSG_DEVICESTATUS	= 7
MSG_DEVICESTATUS_ACK 	= 8

			# 15 - Old time/date request unused in 0.0.5 protocol
			# 16 - Old time/date response unused in 0.0.5 protocol

MSG_CLEARDISPLAY  	= 21
MSG_CLEARDISPLAY_ACK 	= 22

MSG_SETMENUSIZE		= 23
MSG_SETMENUSIZE_ACK	= 24

MSG_GETMENUITEM		= 25
MSG_GETMENUITEM_ACK	= 26

MSG_GETALERT		= 27

MSG_NAVIGATION		= 29
MSG_NAVIGATION_ACK	= 30

MSG_GETMENUITEMS	= 35

MSG_SETSETTINGS  	= 36
MSG_SETSETTINGS_ACK 	= 37

MSG_GETTIME		= 38
MSG_GETTIME_ACK		= 39

MSG_SETLED 		= 40
MSG_SETLED_ACK 		= 41

MSG_SETVIBRATE 		= 42
MSG_SETVIBRATE_ACK 	= 43

MSG_ACK			= 44

DEVICESTATUS_OFF	= 0
DEVICESTATUS_CLOCK	= 1
DEVICESTATUS_MENU	= 2

RESULT_OK		= 0
RESULT_ERROR		= 1
RESULT_OOM		= 2
RESULT_EXIT		= 3
RESULT_CANCEL		= 4

NAVACTION_NORMAL	= 0
NAVACTION_LONG		= 1
NAVACTION_DOUBLE	= 2

NAVTYPE_UP		= 0
NAVTYPE_DOWN		= 1
NAVTYPE_LEFT		= 2
NAVTYPE_RIGHT		= 3
NAVTYPE_SELECT		= 4
NAVTYPE_MENUSELECT	= 5

def DecodeLVMessage(msg):
	(messageId, headerLen, payloadLen) = struct.unpack(">BBL", msg[0:6])
	payload = msg[2 + headerLen:]

	if headerLen != 4:
		raise Exception("Unexpected header length %i" % headerLen)
	if payloadLen != len(payload):
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
	elif messageId == MSG_GETMENUITEMS:
		return GetMenuItems(messageId, payload)
	elif messageId == MSG_GETTIME:
		return GetTime(messageId, payload)
	elif messageId == MSG_GETALERT:
		return GetAlert(messageId, payload)
	elif messageId == MSG_DEVICESTATUS:
		return DeviceStatus(messageId, payload)
	elif messageId == MSG_NAVIGATION:
		return Navigation(messageId, payload)
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

def EncodeSetSettings(flags, fontSize, selectedMenuItem):
	# FIXME: dunno quite wtf this is all doing
	# flags 01:
	# flags 02:
	# flags 04: vibrator on/off
	# flags 08:
	# flags 10:
	# flags 20:
	# flags 40:
	# flags 80:
	return EncodeLVMessage(MSG_SETSETTINGS, struct.pack(">BBB", flags, fontSize, selectedMenuItem))

def EncodeGetMenuItemAck(isAlertItem, totalAlerts, unreadAlerts, curAlert, menuItemId, itemDescription, itemBitmap):
	# FIXME: not quite sure of all this yet
	# byte 00: bit 0: icon is a normal menu item

	# byte 01: } total ??
	# byte 02: }

	# byte 03: } unread count
	# byte 04: }

	# byte 05: } index ???
	# byte 06: }

	# byte 07: itemId + 3 (why +3 I do not know)

	# byte 08: ?? depends on whether menu item text is a text icon is a latin string (0 or 1). Setting it to 1 disables the menu item texts
	
	# byte 09: length of renderShowUi() string
	# byte 0a:
	# <message string?>

	# byte 0b: length of relative time span string
	# byte 0c:
	# <relative time span string>

	# byte 0d: string size hi
	# byte 0e: string size lo-
	# byte 0f: <menu item string>

	# byte n: PNG data
	
	payload = struct.pack(">BHHHBBHHH", not isAlertItem, totalAlerts, unreadAlerts, curAlert, menuItemId + 3, 0, 0, 0, len(itemDescription)) + itemDescription + itemBitmap
	return EncodeLVMessage(MSG_GETMENUITEM_ACK, payload)

def EncodeGetTimeAck(time, is24HourDisplay):
	return EncodeLVMessage(MSG_GETTIME_ACK, struct.pack(">LB", time, not is24HourDisplay))

def EncodeDeviceStatus(deviceStatus):
	return EncodeLVMessage(MSG_DEVICESTATUS, struct.pack(">B", deviceStatus))

def EncodeDeviceStatusAck():
	return EncodeLVMessage(MSG_DEVICESTATUS_ACK, struct.pack(">B", RESULT_OK))

def EncodeNavigationAck(result):
	return EncodeLVMessage(MSG_NAVIGATION_ACK, struct.pack(">B", result))








def EncodeClearDisplay():
	# FIXME: device does not respond!
	return EncodeLVMessage(MSG_CLEARDISPLAY, "")









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
		s = "??"
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
		elif self.deviceStatus == DEVICESTATUS_CLOCK:
			s = "Clock"
		elif self.deviceStatus == DEVICESTATUS_MENU:
			s = "Menu"

		return "<DeviceStatus>\nStatus: %i (%s)" % (self.deviceStatus, s)

class GetAlert:
	
	def __init__(self, messageId, msg):
		self.messageId = messageId
		(self.menuItemId, self.textLength, self.maxLineBreakSize, self.fontSize, self.textImageWidth, self.textImageHeight) = struct.unpack(">BHBBBB", msg)
		
	def __str__(self):
		return "<GetAlert>\nMenuItemId %i\nTextLength %i\nMaxLineBreakSize %i\nFontSize %i\nTextImageWidth %i\nTextImageHeight %i" % (self.menuItemId, self.textLength, self.maxLineBreakSize, self.fontSize, self.textImageWidth, self.textImageHeight)

class Navigation:

	def __init__(self, messageId, msg):
		self.messageId = messageId
		(byte0, byte1, navigation, self.x, self.y) = struct.unpack(">BBBBB", msg)
		if byte0 != 0:
			print >>sys.stderr, "Navigation with unknown byte0 value %i" % byte0
		if byte1 != 3:
			print >>sys.stderr, "Navigation with unknown byte1 value %i" % byte1
		if (navigation != 32) and ((navigation < 1) or (navigation > 15)):
			print >>sys.stderr, "Navigation with out of range value %i" % navigation

		if navigation != 32:
			self.navAction = (navigation - 1) % 3
			self.navType = int((navigation - 1) / 3)
		else:
			self.navAction = NAVACTION_NORMAL
			self.navType = NAVTYPE_MENUSELECT

	def __str__(self):
		
		sA = "UNKNOWN"
		if self.navAction == NAVACTION_NORMAL:
			sA = "Normal"
		elif self.navAction == NAVACTION_DOUBLE:
			sA = "Double"
		elif self.navAction == NAVACTION_LONG:
			sA = "Long"

		sT = "UNKNOWN"
		if self.navType == NAVTYPE_UP:
			sT = "UP"
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

		return "<Navigation>\nAction %s\nType %s\nX %i\nY %i" % (sA, sT, self.x, self.y)
