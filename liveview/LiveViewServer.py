#!/usr/bin/python2

import bluetooth
import LiveViewProtocol
import sys
import time

testPngFd = open("test36.png")
testPng = testPngFd.read()
testPngFd.close()
print len(testPng)

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

while True:
	tmp = LiveViewProtocol.Decode(clientSocket.recv(1024))
	if isinstance(tmp, LiveViewProtocol.GetMenuItems):
		clientSocket.send(LiveViewProtocol.EncodeAck(LiveViewProtocol.MSG_GETMENUITEMS))
		
		clientSocket.send(LiveViewProtocol.EncodeGetMenuItemAck(False, 10, 5, 4, 0, "Hi0", testPng))
		clientSocket.send(LiveViewProtocol.EncodeGetMenuItemAck(False, 5, 2, 2, 1, "Hi1", testPng))
		clientSocket.send(LiveViewProtocol.EncodeGetMenuItemAck(False, 5, 3, 3, 2, "Hi2", testPng))
		clientSocket.send(LiveViewProtocol.EncodeGetMenuItemAck(True, 5, 4, 4, 3, "Hi3", testPng))
		
		clientSocket.send(LiveViewProtocol.EncodeSetSettings(0, 12, 1))

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
	else:
		print "UNKNOWN"
	print tmp

clientSocket.close()
serverSocket.close()
