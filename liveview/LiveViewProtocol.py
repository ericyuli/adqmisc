import struct
import time
import sys

LV_MSG_CAPS_REQ 	= 1
LV_MSG_CAPS_RESP 	= 2

LV_MSG_STANDBY_REQ 	= 7
LV_MSG_STANDBY_RESP 	= 8

LV_MSG_CLEARDISPLAY_REQ  = 21
LV_MSG_CLEARDISPLAY_RESP = 22

LV_MSG_LED_REQ 		= 40
LV_MSG_LED_RESP 	= 41

LV_MSG_VIBRATE_REQ 	= 42
LV_MSG_VIBRATE_RESP 	= 43

LV_RESULT_OK		= 0
LV_RESULT_UNKNOWN	= 1
LV_RESULT_OOM		= 2
LV_RESULT_EXIT		= 3
LV_RESULT_CANCEL	= 4


def EncodeLVMessage(messageId, data):
	return struct.pack(">BBL", messageId, 4, len(data)) + data

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
	if messageId == LV_MSG_CAPS_RESP:
		return DisplayCapabilities(payload)
	elif messageId == LV_MSG_LED_RESP:
		return Result(payload)
	else:
		print >>sys.stderr, "Unknown message id %i", messageId

def EncodeCapsReq():
	return EncodeLVMessage(LV_MSG_CAPS_REQ, struct.pack(">B5s", 5, "0.0.3"))

def EncodeClearDisplayReq():
	return EncodeLVMessage(LV_MSG_CLEARDISPLAY_REQ, "")

def EncodeStandbyReq(unknown):
	return EncodeLVMessage(LV_MSG_STANDBY_REQ, struct.pack(">B", unknown))

def EncodeVibrateReq(delayTime, onTime):
	return EncodeLVMessage(LV_MSG_VIBRATE_REQ, struct.pack(">HH", delayTime, onTime))

def EncodeLEDReq(r, g, b, delayTime, onTime):
	return EncodeLVMessage(LV_MSG_LED_REQ, struct.pack(">HHH", ((r & 0x31) << 10) | ((g & 0x31) << 5) | (b & 0x31), delayTime, onTime))



class DisplayCapabilities:
	
	def __init__(self, msg):
		(self.width, self.height, self.statusBarWidth, self.statusBarHeight, self.viewWidth, self.viewHeight, self.announceWidth, self.announceHeight, self.textChunkSize, self.idleTimer) = struct.unpack(">BBBBBBBBBB", msg[0:10])
		self.softwareVersion = msg[10:]
	
	def __str__(self):
		return "<DisplayCapabilities>\nWidth %i\nHeight %i\nStatusBarWidth %i\nStatusBarHeight %i\nViewWidth %i\nViewHeight %i\nAnnounceWidth %i\nAnnounceHeight %i\nTextChunkSize %i\nIdleTimer %i\nSoftware Version: %s" % (self.width, self.height, self.statusBarWidth, self.statusBarHeight, self.viewWidth, self.viewHeight, self.announceWidth, self.announceHeight, self.textChunkSize, self.idleTimer, self.softwareVersion)

class Result:

	def __init__(self, msg):
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
		return "<Result>\n%s (%i)" % (s, self.code)
