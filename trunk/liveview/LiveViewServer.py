#!/usr/bin/python2

import bluetooth
import LiveViewProtocol
import sys
import time
import struct

testPngFd = open("test36.png")
testPng = testPngFd.read()
testPngFd.close()

testPngFd = open("test128.png")
testPng128 = testPngFd.read()
testPngFd.close()

serverSocket = bluetooth.BluetoothSocket( bluetooth.RFCOMM )
serverSocket.bind(("",1))
serverSocket.listen(1)

bluetooth.advertise_service(serverSocket, "LiveView", 
			    service_classes=[ bluetooth.SERIAL_PORT_CLASS ],
			    profiles=[ bluetooth.SERIAL_PORT_PROFILE ]			    
			    )
clientSocket, address = serverSocket.accept()

clientSocket.send(LiveViewProtocol.EncodeGetCaps())
deviceCaps = LiveViewProtocol.Decode(clientSocket.recv(1024))
clientSocket.send(LiveViewProtocol.EncodeAck(LiveViewProtocol.MSG_GETCAPS_ACK))
print deviceCaps

clientSocket.send(LiveViewProtocol.EncodeSetMenuSize(4))
clientSocket.send(LiveViewProtocol.EncodeSetMenuSettings(5, 12, 0))

tmpxxx = "Hi0"

while True:
	tmp = LiveViewProtocol.Decode(clientSocket.recv(1024))
	if isinstance(tmp, LiveViewProtocol.GetMenuItems):
		clientSocket.send(LiveViewProtocol.EncodeAck(LiveViewProtocol.MSG_GETMENUITEMS))

		clientSocket.send(LiveViewProtocol.EncodeGetMenuItemAck(0, True, 0, "Moo", testPng))
		clientSocket.send(LiveViewProtocol.EncodeGetMenuItemAck(1, False, 20, "Hi1", testPng))
		clientSocket.send(LiveViewProtocol.EncodeGetMenuItemAck(2, False, 0, "Hi2", testPng))
		clientSocket.send(LiveViewProtocol.EncodeGetMenuItemAck(3, True, 0, "Hi3", testPng))

	elif isinstance(tmp, LiveViewProtocol.GetMenuItem):
		clientSocket.send(LiveViewProtocol.EncodeAck(LiveViewProtocol.MSG_GETMENUITEM))
		
		print "---------------------------- GETMENUITEM RECEIVED ----------------------------------"
		# FIXME: do something!

	elif isinstance(tmp, LiveViewProtocol.DisplayCapabilities):
		pass
	elif isinstance(tmp, LiveViewProtocol.Result):
		pass
	elif isinstance(tmp, LiveViewProtocol.GetTime):
		clientSocket.send(LiveViewProtocol.EncodeAck(LiveViewProtocol.MSG_GETTIME))
		
		clientSocket.send(LiveViewProtocol.EncodeGetTimeAck(time.time(), True))
	elif isinstance(tmp, LiveViewProtocol.DeviceStatus):
		clientSocket.send(LiveViewProtocol.EncodeAck(LiveViewProtocol.MSG_DEVICESTATUS))
		
		clientSocket.send(LiveViewProtocol.EncodeDeviceStatusAck())

	elif isinstance(tmp, LiveViewProtocol.GetAlert):
		clientSocket.send(LiveViewProtocol.EncodeAck(LiveViewProtocol.MSG_GETALERT))

		# FIXME: do summat

	elif isinstance(tmp, LiveViewProtocol.Navigation):
		clientSocket.send(LiveViewProtocol.EncodeAck(LiveViewProtocol.MSG_NAVIGATION))		
		clientSocket.send(LiveViewProtocol.EncodeNavigationAck(LiveViewProtocol.RESULT_CANCEL))

#		clientSocket.send(LiveViewProtocol.EncodeSetMenuSize(0))
#		clientSocket.send(LiveViewProtocol.EncodeClearDisplay())
#		clientSocket.send(LiveViewProtocol.EncodeDisplayBitmap(100, 100, testPng))
#		clientSocket.send(LiveViewProtocol.EncodeSetScreenMode(50, False))
#		clientSocket.send(LiveViewProtocol.EncodeDisplayText("WOOOOOOOOOOOO"))

#		clientSocket.send(LiveViewProtocol.EncodeLVMessage(31, ""))


#		clientSocket.send(LiveViewProtocol.EncodeSetScreenMode(0, False))
#		clientSocket.send(LiveViewProtocol.EncodeClearDisplay())
#		clientSocket.send(LiveViewProtocol.EncodeLVMessage(48, struct.pack(">B", 38) + "moo"))

#		tmpxxx = "MOOO"
#		clientSocket.send(LiveViewProtocol.EncodeSetMenuSize(4))
#		clientSocket.send(LiveViewProtocol.EncodeDisplayText("moo"))

		clientSocket.send(LiveViewProtocol.EncodeSetStatusBar(tmp.menuItemId, 200, testPng))
		
#		clientSocket.send(EncodeLVMessage(5, LiveViewProtocol.EncodeUIPayload(isAlertItem, totalAlerts, unreadAlerts, curAlert, menuItemId, top, mid, body, itemBitmap)))

#		if tmp.navType == LiveViewProtocol.NAVTYPE_DOWN:
#			clientSocket.send(LiveViewProtocol.EncodeNavigationAck(LiveViewProtocol.RESULT_OK))
#			clientSocket.send(LiveViewProtocol.EncodeDisplayPanel("TOOOOOOOOOOOOOOOOOP", "BOTTTTTTTTTTTTTTTTTOM", testPng, False))
#		elif tmp.navType == LiveViewProtocol.NAVTYPE_UP:
#			clientSocket.send(LiveViewProtocol.EncodeNavigationAck(LiveViewProtocol.RESULT_OK))
#			clientSocket.send(LiveViewProtocol.EncodeDisplayText("ADQ WOS HERE"))
#		elif tmp.navType == LiveViewProtocol.NAVTYPE_SELECT:
#			clientSocket.send(LiveViewProtocol.EncodeNavigationAck(LiveViewProtocol.RESULT_EXIT))
		
#		clientSocket.send(LiveViewProtocol.EncodeSetVibrate(1, 1000))

	else:
		print "UNKNOWN"
	print tmp

clientSocket.close()
serverSocket.close()
