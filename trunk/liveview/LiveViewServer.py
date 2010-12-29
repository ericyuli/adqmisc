#!/usr/bin/python2

import bluetooth
import LiveViewProtocol
import sys

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
clientSocket.send(LiveViewProtocol.EncodeAck(LiveViewProtocol.LV_MSG_GETCAPS))
print deviceCaps

clientSocket.send(LiveViewProtocol.EncodeSetMenuSize(5))
clientSocket.send(LiveViewProtocol.EncodeSetSettings(0, 12, 4))

while True:
	tmp = LiveViewProtocol.Decode(clientSocket.recv(1024))
	if isinstance(tmp, LiveViewProtocol.GetMenuItems):
		print tmp
	elif isinstance(tmp, LiveViewProtocol.DisplayCapabilities):
		print tmp
	elif isinstance(tmp, LiveViewProtocol.Result):
		print tmp
	else:
		print "UNKNOWN"
		print tmp

clientSocket.close()
serverSocket.close()

