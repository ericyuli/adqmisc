# -*- coding: utf-8 -*-
from socket import *
import struct
import time



def FormatPacket(data):

    data = [x.replace(",", "_") for x in data]
    data = ",".join(data)
    data += "\r\n"
    return data


def WaitForResponse(tcp_server, timeout = None):

    # Wait for something to connect to us
    mgr_in_socket = None
    tcp_server.settimeout(timeout)
    try:
	mgr_in_socket = tcp_server.accept()[0]
    except:
	return None

    # Read the response line and parse it
    mgr_in_socket.setblocking(1)
    mgr_in_socket.settimeout(timeout)
    try:
	pkt = mgr_in_socket.recv(1024).strip().split(",")
	if pkt[1] != "PF110-DEV" or pkt[2] != "PF110-PC":
	    raise Exception("Receieved bad packet")
	return pkt
    except:
	return None
    finally:
	mgr_in_socket.close()







class EFrameLocator:

    def __init__(self, 
		 local_ip, 
		 broadcast_ip = "255.255.255.255",
		 broadcast_port = 21900,
		 serial_number = gethostname(),
		 local_name = gethostname()):

	self.local_ip = local_ip
	self.broadcast_address = (broadcast_ip, broadcast_port)
	self.serial_number = serial_number
	self.local_name = local_name

	# Create a udp socket to broadcast on
	self.udp_socket = socket(AF_INET, SOCK_DGRAM)
	self.udp_socket.setsockopt(SOL_SOCKET, SO_BROADCAST, 1)

	# Create a TCP server socket to listen for connects on
	self.tcp_server = socket(AF_INET, SOCK_STREAM)
	self.tcp_server.listen(10)
	self.tcp_server.setsockopt(SOL_SOCKET, SO_LINGER, struct.pack('ii', 1, 0))
	self.local_manager_port = self.tcp_server.getsockname()[1]


    def __del__(self):

	self.udp_socket.close()
	self.tcp_server.close()


    def SendBroadcastPacket(self, packet_type, data):

	data = (packet_type, "PF110-PC", "PF110-DEV") + data
	self.udp_socket.sendto(FormatPacket(data), self.broadcast_address)


    def FindEFrames(self, timeout):

	# Build the Search packet
	# FIXME: unsure what the '1' is for
	searchpkt = ("", str(self.local_manager_port), self.serial_number, self.local_name, "", "", str(1), self.local_ip)

	# Loop, looking for eframes
	start = time.time()
	while time.time() < start + timeout:

	    # broadcast the search packet
	    self.SendBroadcastPacket("Search", searchpkt)
	    
	    # Deal with anything which has connected to us
	    while True:
		tmp = WaitForResponse(self.tcp_server, 1.0)
		if tmp == None:
		    break
		if tmp[0] == 'Register' and tmp[4] == 'RegisterStatus':
		    return ((tmp[12], int(tmp[3])), tmp[7], tmp[8].strip('"'))

	# FIXME: allow >1 eframe

    def SendByeBye(self):

	self.SendBroadcastPacket("ByeBye", (self.serial_number, self.local_name))






class EFrame:

    def __init__(self, 
		 local_ip, 
		 frame_address,
		 ftp_port = 20021,
		 ftp_username = "PF110",
		 ftp_password = "QmitwPF",
		 serial_number = gethostname(),
		 local_name = gethostname(),
		 timeout = 10.0 ):

	self.local_ip = local_ip
	self.frame_address = frame_address
	self.ftp_port = ftp_port
	self.ftp_username = ftp_username
	self.ftp_password = ftp_password
	self.serial_number = serial_number
	self.local_name = local_name
	self.timeout = timeout

	# Create a TCP server socket to listen for connects on
	self.tcp_server = socket(AF_INET, SOCK_STREAM)
	self.tcp_server.listen(1)
	self.tcp_server.setsockopt(SOL_SOCKET, SO_LINGER, struct.pack('ii', 1, 0))
	self.local_manager_port = self.tcp_server.getsockname()[1]

	# Re-register the supplied frame with /this/ instance
	self.RegisterFrame()
	tmp = self.ReadSystemStatus()
	self.serial_number = tmp[0]
	self.frame_name = tmp[1].strip('"')


    def __del__(self):
	self.tcp_server.close()


    def __str__(self):

	return "%s(%s) @%s" % (self.frame_name, self.frame_serial, self.frame_address)


    def SendManagerPacket(self, packet_type, data):

	# seems to need a completely new socket for every one!
	mgr_out_socket = socket(AF_INET, SOCK_STREAM)
	mgr_out_socket.connect(self.frame_address)

	data = (packet_type, "PF110-PC", "PF110-DEV", str(self.local_manager_port)) + data
	mgr_out_socket.send(FormatPacket(data))
	mgr_out_socket.close()


    def DoCommand(self, action, data):

	self.SendManagerPacket(action, data)
	tmp = WaitForResponse(self.tcp_server, self.timeout)
	if tmp == None or (tmp[0] != action + "-Resp") or (tmp[4] != data[0]):
	    raise Exception("Received unexpected reply")
	if int(tmp[5]) != 0:
	    print "Warning: Value expected to be zero was actually %s" % tmp[5]
	return tmp[6:]


    def DoTransfer(self, file_type_id, start_action, stop_action, progress_func, copy_action_func):

	self.DoCommand("Post", (start_action, str(1), str(file_type_id))) # FIXME: not sure what the '1' here is for
	while True:
	    # wait for the next response
	    tmp = WaitForResponse(self.tcp_server, None)
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
		    self.SendManagerPacket("Post", ("CopyPause", str(0)))
		elif action == "REPLACE":
		    self.SendManagerPacket("Post", ("CopyPause", str(1)))
		elif action == "NOREPLACE":
		    self.SendManagerPacket("Post", ("CopyPause", str(2)))
		elif action == "ABORT":
		    self.SendManagerPacket("Post", ("CopyPause", str(3)))
		else:
		    raise Exception("Unknown action %s from copy_action_func()" % action)

	    elif token == stop_action:
		break

	    else:
		raise Exception("Unexpected protocol %s token during file transfer" % str(tmp))

	# FIXME: send final response packet








    def RegisterFrame(self):

	registerpkt = ("RegisterStatus", str(self.ftp_port), str(self.local_manager_port), self.serial_number, 
		       self.local_name, self.ftp_username, self.ftp_password, str(1), self.local_ip)

	self.DoCommand("Register", registerpkt)


    def ReadStorageStatus(self):

	return self.DoCommand("Read", ("StorageStatus", ))


    def ReadSystemStatus(self):
	
	return self.DoCommand("Read", ("SystemStatus", ))


    def TransferPhotos(self, progress_func, copy_action_func):
	
	self.DoTransfer(0, "CopyStart", "CopyStop", progress_func, copy_action_func)


    def TransferMusic(self, progress_func, copy_action_func):
	
	self.DoTransfer(1, "CopyStart", "CopyStop", progress_func, copy_action_func)


    def TransferRss(self, progress_func, copy_action_func):
	
	self.DoTransfer(2, "RssFileStart", "RssFileStop", progress_func, copy_action_func)

