# -*- coding: utf-8 -*-
from socket import *
import struct

class EFrameProto:

    def __init__(self, 
		 local_ip, 
		 broadcast_address = "255.255.255.255",
		 broadcast_port = 21900, 
		 pc_manager_port = 21901,
		 ftp_port = 20021,
		 ftp_username = "PF110",
		 ftp_password = "QmitwPF",
		 serial_number = gethostname(),
		 local_name = gethostname() ):
	
	self.local_ip = local_ip
	self.broadcast_address = broadcast_address
	self.broadcast_port = broadcast_port
	self.pc_manager_port = pc_manager_port
	self.ftp_port = ftp_port
	self.ftp_username = ftp_username
	self.ftp_password = ftp_password
	self.serial_number = serial_number
	self.local_name = local_name

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


    def BuildRegisterPacket(self):

	return (str(self.ftp_port),
		str(self.pc_manager_port),
		self.serial_number,
		self.local_name,
		self.ftp_username,
		self.ftp_password,
		str(1),	  			# FIXME: not sure what this '1' is for
		self.local_ip)


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
	if tmp[1] != "PF110-DEV" or tmp[2] != "PF110-PC"=:
	    raise Exception("Receieved bad packet")
	return tmp


    def WaitForConnection(self, timeout = None, retries = 1, func = None):

	self.tcp_server.settimeout(timeout)
	while retries > 0:
	    retries -= 1

	    if func:
		func()

	    try:
		return self.tcp_server.accept()
	    except:
		pass


    def GetResponsePacket(self, timeout, mgr_in_socket):

	mgr_in_socket.setblocking(1)
	mgr_in_socket.settimeout(timeout)
	try:
	    return self.ParseReceivedPacket(mgr_in_socket.recv(1024))
	except:
	    pass
	finally:
	    mgr_in_socket.close()


    def WaitForResponse(self, timeout = None, retries = 1, func = None):

	tmp = self.WaitForConnection(timeout, retries, func)
	if tmp == None:
	    return
	return self.GetResponsePacket(timeout, tmp[0])


    def DoCommand(self, frame_address, action, data):

	self.SendManagerPacket(frame_address, action, data)
	tmp = self.WaitForResponse()
	if tmp == None or (tmp[0] != action + "-Resp") or (tmp[4] != data[0]):
	    raise Exception("Received unexpected reply")
	return tmp[5:]


    def DoTransfer(self, frame_address, file_type_id, start_action, stop_action, progress_func, copy_action_func):

	self.DoCommand(frame_address, "Post", (start_action, str(1), str(file_type_id))) # FIXME: not sure what the '1' here is for
	while True:
	    # wait for the next response
	    tmp = self.WaitForResponse()
	    if tmp == None:
		raise Exception("Transfer timed out")
	    if tmp[0] != 'Post':
		raise Exception("Received unexpected reply")
	    
	    # Deal with the response
	    token = tmp[4]
	    if token == "ProgressStatus":
		progress_func(tmp[5:])
	    elif token == "CopyPause":
		action = copy_action_func(tmp[5:])
		if action == "REPLACEALL":
		    self.SendManagerPacket(frame_address, "Post", ("CopyPause", str(0)))
		elif action == "REPLACE":
		    self.SendManagerPacket(frame_address, "Post", ("CopyPause", str(1)))
		elif action == "NOREPLACE":
		    self.SendManagerPacket(frame_address, "Post", ("CopyPause", str(2)))
		elif action == "ABORT":
		    self.SendManagerPacket(frame_address, "Post", ("CopyPause", str(3)))
	    elif token == stop_action:
		break
	    else:
		raise Exception("Unexepected protocol %s token during file transfer" % str(tmp))

	# FIXME: send final response packet






    def SearchForFrame(self):
	data = self.BuildRegisterPacket()

	def bcast():
	    self.SendBroadcastPacket("Search", data)

	tmp = self.WaitForResponse(2.0, 20, bcast)
	if tmp == None:
	    return
	if tmp[0] != 'Register' or tmp[4] != 'RegisterStatus':
	    raise Exception("Received unexpected reply")
	return tmp[5:]


    def SendByeBye(self):

	self.SendBroadcastPacket("ByeBye", (self.serial_number, self.local_name))


    def ReadStorageStatus(self, frame_address):

	return self.DoCommand(frame_address, "Read", ("StorageStatus", ))


    def ReadSystemStatus(self, frame_address):
	
	return self.DoCommand(frame_address, "Read", ("SystemStatus", ))


    def ReadRegisterStatus(self, frame_address):

	return self.DoCommand(frame_address, "Register", ("RegisterStatus", ) + self.BuildRegisterPacket())


    def TransferPhotos(self, frame_address, progress_func, copy_action_func):
	
	self.DoTransfer(frame_address, 0, "CopyStart", "CopyStop", progress_func, copy_action_func)


    def TransferMusic(self, frame_address, progress_func, copy_action_func):
	
	self.DoTransfer(frame_address, 1, "CopyStart", "CopyStop", progress_func, copy_action_func)


    def TransferRss(self, frame_address, progress_func, copy_action_func):
	
	self.DoTransfer(frame_address, 2, "RssFileStart", "RssFileStop", progress_func, copy_action_func)
