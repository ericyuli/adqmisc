import struct
import time
import sys

LV_MSG_GETCAPS		= 1
LV_MSG_GETCAPS_ACK 	= 2

LV_MSG_STANDBY		= 7
LV_MSG_STANDBY_ACK 	= 8

LV_MSG_CLEARDISPLAY  	= 21
LV_MSG_CLEARDISPLAY_ACK = 22

LV_MSG_SETMENUSIZE	= 23
LV_MSG_SETMENUSIZE_ACK	= 24

LV_MSG_GETMENUITEMS	= 35

LV_MSG_SETSETTINGS  	= 36
LV_MSG_SETSETTINGS_ACK 	= 37

LV_MSG_SETLED 		= 40
LV_MSG_SETLED_ACK 	= 41

LV_MSG_SETVIBRATE 	= 42
LV_MSG_SETVIBRATE_ACK 	= 43

LV_MSG_ACK		= 44


LV_RESULT_OK		= 0
LV_RESULT_UNKNOWN	= 1
LV_RESULT_OOM		= 2
LV_RESULT_EXIT		= 3
LV_RESULT_CANCEL	= 4


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
	if messageId == LV_MSG_GETCAPS_ACK:
		return DisplayCapabilities(messageId, payload)
	elif messageId == LV_MSG_SETLED_ACK:
		return Result(messageId, payload)
	elif messageId == LV_MSG_SETVIBRATE_ACK:
		return Result(messageId, payload)
	elif messageId == LV_MSG_STANDBY_ACK:
		return Result(messageId, payload)
	elif messageId == LV_MSG_GETMENUITEMS:
		return GetMenuItems(messageId, payload)
	else:
		print >>sys.stderr, "Unknown message id %i", messageId

def EncodeLVMessage(messageId, data):
	return struct.pack(">BBL", messageId, 4, len(data)) + data

def EncodeGetCaps():
	return EncodeLVMessage(LV_MSG_GETCAPS, struct.pack(">B5s", 5, "0.0.3"))

def EncodeSetVibrate(delayTime, onTime):
	return EncodeLVMessage(LV_MSG_SETVIBRATE, struct.pack(">HH", delayTime, onTime))

def EncodeSetLED(r, g, b, delayTime, onTime):
	return EncodeLVMessage(LV_MSG_SETLED, struct.pack(">HHH", ((r & 0x31) << 10) | ((g & 0x31) << 5) | (b & 0x31), delayTime, onTime))

def EncodeSetMenuSize(menuSize):
	return EncodeLVMessage(LV_MSG_SETMENUSIZE, struct.pack(">B", menuSize))

def EncodeAck(ackMessageId):
	return EncodeLVMessage(LV_MSG_ACK, struct.pack(">B", ackMessageId))

def EncodeSetSettings(flags, fontSize, selectedMenuItem):
	# FIXME:
	# flags 01:
	# flags 02:
	# flags 04: vibrator on/off
	# flags 08:
	# flags 10:
	# flags 20:
	# flags 40:
	# flags 80:
	return EncodeLVMessage(LV_MSG_SETSETTINGS, struct.pack(">BBB", flags, fontSize, selectedMenuItem))










def EncodeClearDisplay():
	# FIXME: device does not respond!
	return EncodeLVMessage(LV_MSG_CLEARDISPLAY, "")

def EncodeStandby(unknown):
	# FIXME: what is "unknown" value
	return EncodeLVMessage(LV_MSG_STANDBY, struct.pack(">B", unknown))










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
	
	def __str__(self):
		s = "??"
		if self.code == LV_RESULT_OK:
			s = "OK"
		elif self.code == LV_RESULT_UNKNOWN:
			s = "UNKNOWN"
		elif self.code == LV_RESULT_OOM:
			s = "OOM"
		elif self.code == LV_RESULT_EXIT:
			s = "EXIT"
		elif self.code == LV_RESULT_CANCEL:
			s = "CANCEL"

		return "<Result>\nMessageId: %i\nCode: %i (%s)" % (self.messageId, self.code, s)

class GetMenuItems:

	def __init__(self, messageId, msg):
		self.messageId = messageId
		(self.unknown, ) = struct.unpack(">B", msg)

	def __str__(self):
		return "<GetMenuItems>\nUnknown: %i" % (self.unknown)
