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

clientSocket.send(LiveViewProtocol.EncodeCapsReq())
deviceCaps = LiveViewProtocol.Decode(clientSocket.recv(1024))

#clientSocket.send(LiveViewProtocol.EncodeClearDisplayReq())
#print LiveViewProtocol.Decode(clientSocket.recv(1024))

#clientSocket.send(LiveViewProtocol.EncodeStandbyReq(2))
#print LiveViewProtocol.Decode(clientSocket.recv(1024))

#clientSocket.send(LiveViewProtocol.EncodeLEDReq(0, 0, 0x1f, 10, 1000))
#print LiveViewProtocol.Decode(clientSocket.recv(1024))

#clientSocket.send(LiveViewProtocol.EncodeVibrateReq(10, 1000))
#print LiveViewProtocol.Decode(clientSocket.recv(1024))

clientSocket.close()
serverSocket.close()

