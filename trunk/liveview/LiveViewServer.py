#!/usr/bin/python2

import bluetooth
import LiveViewMessages
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

clientSocket.send(LiveViewMessages.EncodeGetCaps())

while True:
	for msg in LiveViewMessages.Decode(clientSocket.recv(4096)):
		if isinstance(msg, LiveViewMessages.GetMenuItems):
			clientSocket.send(LiveViewMessages.EncodeAck(LiveViewMessages.MSG_GETMENUITEMS))

			clientSocket.send(LiveViewMessages.EncodeGetMenuItemAck(0, True, 0, "Moo", testPng))
			clientSocket.send(LiveViewMessages.EncodeGetMenuItemAck(1, False, 20, "Hi1", testPng))
			clientSocket.send(LiveViewMessages.EncodeGetMenuItemAck(2, False, 0, "Hi2", testPng))
			clientSocket.send(LiveViewMessages.EncodeGetMenuItemAck(3, True, 0, "Hi3", testPng))

		elif isinstance(msg, LiveViewMessages.GetMenuItem):
			clientSocket.send(LiveViewMessages.EncodeAck(LiveViewMessages.MSG_GETMENUITEM))
			
			print "---------------------------- GETMENUITEM RECEIVED ----------------------------------"
			# FIXME: do something!

		elif isinstance(msg, LiveViewMessages.DisplayCapabilities):
			clientSocket.send(LiveViewMessages.EncodeAck(LiveViewMessages.MSG_GETCAPS_ACK))
			deviceCapabilities = msg
			
			clientSocket.send(LiveViewMessages.EncodeSetMenuSize(4))
			clientSocket.send(LiveViewMessages.EncodeSetMenuSettings(5, 12, 0))
			
		elif isinstance(msg, LiveViewMessages.Result):
			pass
		elif isinstance(msg, LiveViewMessages.GetTime):
			clientSocket.send(LiveViewMessages.EncodeAck(LiveViewMessages.MSG_GETTIME))

			clientSocket.send(LiveViewMessages.EncodeGetTimeAck(time.time(), True))
		elif isinstance(msg, LiveViewMessages.DeviceStatus):
			clientSocket.send(LiveViewMessages.EncodeAck(LiveViewMessages.MSG_DEVICESTATUS))
			
			clientSocket.send(LiveViewMessages.EncodeDeviceStatusAck())

		elif isinstance(msg, LiveViewMessages.GetAlert):
			clientSocket.send(LiveViewMessages.EncodeAck(LiveViewMessages.MSG_GETALERT))

			# FIXME: do summat

		elif isinstance(msg, LiveViewMessages.Navigation):
			clientSocket.send(LiveViewMessages.EncodeAck(LiveViewMessages.MSG_NAVIGATION))		
			clientSocket.send(LiveViewMessages.EncodeNavigationAck(LiveViewMessages.RESULT_OK))

	#		clientSocket.send(LiveViewMessages.EncodeSetMenuSize(0))
	#		clientSocket.send(LiveViewMessages.EncodeClearDisplay())
	#		clientSocket.send(LiveViewMessages.EncodeDisplayBitmap(100, 100, testPng))
	#		clientSocket.send(LiveViewMessages.EncodeSetScreenMode(50, False))
	#		clientSocket.send(LiveViewMessages.EncodeDisplayText("WOOOOOOOOOOOO"))

	#		clientSocket.send(LiveViewMessages.EncodeLVMessage(31, ""))


	#		clientSocket.send(LiveViewMessages.EncodeSetScreenMode(0, False))
	#		clientSocket.send(LiveViewMessages.EncodeClearDisplay())
	#		clientSocket.send(LiveViewMessages.EncodeLVMessage(48, struct.pack(">B", 38) + "moo"))

	#		tmpxxx = "MOOO"
	#		clientSocket.send(LiveViewMessages.EncodeSetMenuSize(4))
	#		clientSocket.send(LiveViewMessages.EncodeDisplayText("moo"))

	#		clientSocket.send(LiveViewMessages.EncodeSetStatusBar(tmp.menuItemId, 200, testPng))
			
	#		clientSocket.send(EncodeLVMessage(5, LiveViewMessages.EncodeUIPayload(isAlertItem, totalAlerts, unreadAlerts, curAlert, menuItemId, top, mid, body, itemBitmap)))

			if msg.navType == LiveViewMessages.NAVTYPE_DOWN:
				clientSocket.send(LiveViewMessages.EncodeDisplayPanel("TOOOOOOOOOOOOOOOOOP", "BOTTTTTTTTTTTTTTTTTOM", testPng, False))
	#			clientSocket.send(LiveViewMessages.EncodeNavigationAck(LiveViewMessages.RESULT_OK))
	#			clientSocket.send(LiveViewMessages.EncodeDisplayText("ADQ WOS HERE"))
	#		elif tmp.navType == LiveViewMessages.NAVTYPE_SELECT:
	#			clientSocket.send(LiveViewMessages.EncodeNavigationAck(LiveViewMessages.RESULT_EXIT))
			
	#		clientSocket.send(LiveViewMessages.EncodeSetVibrate(1, 1000))

		else:
			print "------------------- UNKNOWN -----------------"
		print msg

clientSocket.close()
serverSocket.close()
