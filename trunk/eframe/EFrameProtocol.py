# -*- coding: utf-8 -*-
from socket import *
import struct
import time
import threading



class EFrame:

    def __init__(self, 
		 local_ip = gethostbyname(gethostname()), 
		 broadcast_ip = "255.255.255.255",
		 broadcast_port = 21900,
		 ftp_port = 20021,
		 ftp_username = "PF110",
		 ftp_password = "QmitwPF",
		 serial_number = gethostname(),
		 local_name = gethostname(),
		 timeout = 10.0 ):

	self.local_ip = local_ip
	self.broadcast_address = (broadcast_ip, broadcast_port)
	self.ftp_port = ftp_port
	self.ftp_username = ftp_username
	self.ftp_password = ftp_password
	self.serial_number = serial_number
	self.local_name = local_name
	self.timeout = timeout

	# Create a udp socket to broadcast on
	self.udp_socket = socket(AF_INET, SOCK_DGRAM)
	self.udp_socket.setsockopt(SOL_SOCKET, SO_BROADCAST, 1)

	# Create a TCP server socket to listen for connects on
	self.tcp_server = socket(AF_INET, SOCK_STREAM)
	self.tcp_server.listen(1)
	self.tcp_server.setsockopt(SOL_SOCKET, SO_LINGER, struct.pack('ii', 1, 0))
	self.local_manager_port = self.tcp_server.getsockname()[1]

	# Create a thread to listen for connections and data on the above tcp_server socket
	self.rx_queue = []
	self.rx_queue_lock = threading.Lock()
	self.tcp_server_thread = threading.Thread(target=self.TcpListener)
	self.tcp_server_thread.setDaemon(True)
	self.tcp_server_thread.start()

	# Originally I tried not sending a broadcast here, and just using the RegisterFrame() call below. However I found
	# that the frame then doesn't work properly after a complete powerdown-powerup cycle; e.g. the ftp port is 
	# completely wrong. Switching back to using a broadcast seemed to fix this :(
	searchpkt = (str(self.ftp_port), str(self.local_manager_port), self.serial_number, self.local_name, self.ftp_username, self.ftp_password, str(1), self.local_ip)
	self.SendBroadcastPacket("Search", searchpkt)
	while True:
	    tmp = self.WaitForResponse(5.0)
	    if tmp == None:
		break
	    if tmp[0] == 'Register' and tmp[4] == 'RegisterStatus':
		self.serial_number = tmp[0]
		self.frame_name = tmp[1].strip('"')
		self.frame_address = (tmp[12], int(tmp[6]))
		return

	raise Exception("Failed to contact eframe")


    def __del__(self):

	self.udp_socket.close()
	self.tcp_server.close()

    def TcpListener(self):
    
	while True:
	    try:
		mgr_in_socket = self.tcp_server.accept()[0]

		self.rx_queue_lock.acquire()
		self.rx_queue.insert(0, mgr_in_socket.recv(1024).strip())
		mgr_in_socket.close()
		self.rx_queue_lock.release()
	    except:
		pass


    def FormatPacket(self, data):

	data = [x.replace(",", "_") for x in data]
	data = ",".join(data)
	data += "\r\n"
	return data


    def WaitForResponse(self, timeout = None):

	# Wait for the tcp listener to acquire some data
	rx_data = None
	start = time.time()
	while rx_data == None:

	    # handle timeout
	    if timeout != None:
		if start + timeout < time.time():
		    break
	    
	    # check if we have a connection
	    self.rx_queue_lock.acquire()
	    if len(self.rx_queue) > 0:
		rx_data = self.rx_queue.pop()
	    self.rx_queue_lock.release()

	    # delay if no data left
	    if rx_data == None:
		time.sleep(0.001)

	# still don't have data? => timed out
	if rx_data == None:
	    raise Exception("Timed out waiting for response")

	# Read the response line and parse it
	pkt = rx_data.split(",")
	if pkt[1] != "PF110-DEV" or pkt[2] != "PF110-PC":
	    raise Exception("Receieved bad packet: " + str(pkt))
	return pkt


    def SendBroadcastPacket(self, packet_type, data):

	data = (packet_type, "PF110-PC", "PF110-DEV") + data
	self.udp_socket.sendto(self.FormatPacket(data), self.broadcast_address)


    def SendByeBye(self):

	self.SendBroadcastPacket("ByeBye", (self.serial_number, self.local_name))


    def SendManagerPacket(self, packet_type, data):

	# seems to need a completely new socket for every one!
	mgr_out_socket = socket(AF_INET, SOCK_STREAM)
	mgr_out_socket.connect(self.frame_address)

	data = (packet_type, "PF110-PC", "PF110-DEV", str(self.local_manager_port)) + data
	mgr_out_socket.send(self.FormatPacket(data))
	mgr_out_socket.close()


    def DoCommand(self, action, data):

	self.SendManagerPacket(action, data)
	tmp = self.WaitForResponse(self.timeout)
	if tmp == None or (tmp[0] != action + "-Resp") or (tmp[4] != data[0]):
	    raise Exception("Received unexpected reply: " + str(tmp))
	if int(tmp[5]) != 0:
	    print "Warning: Value expected to be zero was actually %s" % tmp[5]
	return tmp[6:]


    def DoTransfer(self, file_type_id, start_action, stop_action, progress_func, copy_action_func):

	self.DoCommand("Post", (start_action, str(1), str(file_type_id))) # FIXME: not sure what the '1' here is for
	while True:

	    # FIXME: FFS! the bloody thing doesn't always send a response!!!! need to check if a file has been ftped.

	    # wait for the next response
	    tmp = self.WaitForResponse(None)
	    if tmp == None:
		raise Exception("Transfer timed out")
	    if tmp[0] != 'Post':
		raise Exception("Received unexpected reply:" + str(tmp))

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
	self.SendManagerPacket("Post-Resp", ("CopyStop", str(0))) # FIXME: unsure what the 0 is for


    def TransferMusic(self, progress_func, copy_action_func):
	
	self.DoTransfer(1, "CopyStart", "CopyStop", progress_func, copy_action_func)
	self.SendManagerPacket("Post-Resp", ("CopyStop", str(0))) # FIXME: unsure what the 0 is for


    def TransferRss(self, progress_func, copy_action_func):
	
	self.DoTransfer(2, "RssFileStart", "RssFileStop", progress_func, copy_action_func)
	self.SendManagerPacket("Post-Resp", ("RssFileStop", str(0), str(3))) # FIXME: unsure what the 0 or 3 are for
