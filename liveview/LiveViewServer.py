#!/usr/bin/python2

import bluetooth
import LiveViewProtocol

serverSocket = bluetooth.BluetoothSocket( bluetooth.RFCOMM )
serverSocket.bind(("",1))
serverSocket.listen(1)

bluetooth.advertise_service(serverSocket, "LiveView", 
			    service_classes=[ bluetooth.SERIAL_PORT_CLASS ],
			    profiles=[ bluetooth.SERIAL_PORT_PROFILE ]			    
			    )


clientSocket, address = serverSocket.accept()

clientSocket.send(LiveViewProtocol.EncodeCapsReq())
print LiveViewProtocol.Decode(clientSocket.recv(1024))

clientSocket.close()
serverSocket.close()

