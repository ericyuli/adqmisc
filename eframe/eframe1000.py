#!/usr/bin/python 
# -*- coding: utf-8 -*-
from socket import *
from EFrameProto import *
import time
import struct
import sys

# sent by photoframe on shutdown (to 255.255.255.255 port 21900)
# Byebye,PF110-DEV,PF110-PC,0829003416,"000829003416"
# token ,FROM,     TO,      serialnum, frame name

# sent by photoframe on startup (to 255.255.255.255 port 21900)
# Search,PF110-DEV,PF110-PC,20021,21902,0829003416,"000829003416",PhotoFrame,PhotoFrame,1,172.16.1.213
# token ,FROM,     TO,      serialnum, frame name

# Undecoded strings:
# Updata
# Write, Write-Resp, Register, Register-Resp, Post, Post-Resp
# WifiStatus
# RegisterStatus, CopyStart, CopyStop, CopyPause, PushStart, PushStop, RssFileStart, RssFileStop, BtCaceroRss, ProgressStatus, KeepAlive



# Decoded strings:
# Search, Byebye, 
# Read, Read-Resp, 
# SystemStatus, 
# StorageStatus, 


# Create an eframe instance
eframe = EFrameProto("172.16.1.17")
tmp = eframe.SearchForFrame()
if tmp == None:
    print "Could not find eframe"
    sys.exit(1)
print tmp
frame_address = (tmp[7], int(tmp[1]))

print eframe.ReadStorageStatus(frame_address)
print eframe.ReadSystemStatus(frame_address)
eframe.SendByeBye()
