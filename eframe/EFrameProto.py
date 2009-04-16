# -*- coding: utf-8 -*-
from socket import *
import struct

class EFrameProto:

    def __init__(self, local_ip, 
		 broadcast_address = "255.255.255.255",
		 broadcast_port = 21900, 
		 pc_manager_port = 21901,
		 ftp_port = 20021,
		 ftp_username = "PF110",
		 ftp_password = "QmitwPF"):
	
	self.local_ip = local_ip
	self.broadcast_address = broadcast_address
	self.broadcast_port = broadcast_port
	self.pc_manager_port = pc_manager_port
	self.ftp_port = ftp_port
	self.ftp_username = ftp_username
	self.ftp_password = ftp_password

	# Create a udp socket to broadcast on
	self.udp_socket = socket(AF_INET, SOCK_DGRAM)
	self.udp_socket.bind(("0.0.0.0", self.broadcast_port))
	self.udp_socket.setsockopt(SOL_SOCKET, SO_BROADCAST, 1)

	# Create a TCP server socket to listen for connects on
	self.tcp_server = socket(AF_INET, SOCK_STREAM)
	self.tcp_server.bind(("0.0.0.0", self.pc_manager_port))
	self.tcp_server.listen(1)
	self.tcp_server.settimeout(2.0)
	self.tcp_server.setsockopt(SOL_SOCKET, SO_LINGER, struct.pack('ii', 1, 0))

    def FormatPacket(self, data):

	data = [x.replace(",", "_") for x in data]
	data = ",".join(data)
	data += "\r\n"
	return data

    def SendBroadcastPacket(self, packet_type, data):

	data = (packet_type, "PF110-PC", "PF110-DEV") + data
	self.udp_socket.sendto(self.FormatPacket(data), (self.broadcast_address, self.broadcast_port))

    def SendManagerPacket(self, frame_address, packet_type, data):


	# seems to need a completely new socket for every one!
	mgr_out_socket = socket(AF_INET, SOCK_STREAM)
	mgr_out_socket.connect(frame_address)

	data = (packet_type, "PF110-PC", "PF110-DEV", str(self.pc_manager_port)) + data
	mgr_out_socket.send(self.FormatPacket(data))
	mgr_out_socket.close()

    def ParseReceivedPacket(self, raw):

	tmp = raw.strip().split(",")
	if tmp[1] != 'PF110-DEV' or tmp[2] != 'PF110-PC':
	    raise Exception("Receieved bad packet")
	return tmp

    def WaitForConnection(self, timeout):

	self.tcp_server.settimeout(timeout)
	while True:
	    try:
		return self.tcp_server.accept()
	    except:
		return    

    def GetResponsePacket(self, timeout, mgr_in_socket):

	mgr_in_socket.setblocking(1)
	mgr_in_socket.settimeout(timeout)
	try:
	    return self.ParseReceivedPacket(mgr_in_socket.recv(1024))
	except:
	    pass
	finally:
	    mgr_in_socket.close()

    def WaitForResponse(self, timeout):

	tmp = self.WaitForConnection(timeout)
	if tmp == None:
	    return
	return self.GetResponsePacket(timeout, tmp[0])

    def ReadProperty(self, frame_address, property_name):

	self.SendManagerPacket(frame_address, "Read", (property_name, ))
	tmp = self.WaitForResponse(None)
	if tmp == None or tmp[0] != 'Read-Resp' or tmp[4] != property_name:
	    raise Exception("Received unexpected reply")
	return tmp[5:]






    def SearchForFrame(self):
	data = (str(self.ftp_port),
		str(self.pc_manager_port),
		gethostname(),		 	# frame serial number; we just use the hostname from the pc side
		gethostname(), 			# frame name; aagin, we just use the hostname from the pc side 
		self.ftp_username,
		self.ftp_password,
		str(1),	  			# FIXME: not sure what this is
		self.local_ip)

	# Wait for a frame to connect to our TCP server
	self.tcp_server.settimeout(2.0)
	while True:
	    self.SendBroadcastPacket("Search", data)
	    try:
		mgr_in_socket, addr = self.tcp_server.accept()
		break
	    except:
		pass

	# Process and check the response
	tmp = self.GetResponsePacket(None, mgr_in_socket)
	if tmp == None:
	    return
	if tmp[0] != 'Register' or tmp[4] != 'RegisterStatus':
	    raise Exception("Received unexpected reply")
	return tmp[5:]

    def SendByeBye(self):
	data = (gethostname(), 			# frame serial number; we just use the hostname from the pc side
		gethostname()) 			# frame name; aagin, we just use the hostname from the pc side

	self.SendBroadcastPacket("ByeBye", data)

    def ReadStorageStatus(self, frame_address):

	return self.ReadProperty(frame_address, "StorageStatus")

    def ReadSystemStatus(self, frame_address):
	
	return self.ReadProperty(frame_address, "SystemStatus")
