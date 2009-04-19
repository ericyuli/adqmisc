#!/usr/bin/python 
# -*- coding: utf-8 -*-

import EFrameProtocol
from EFrameRSSFeedList import *
import sys

# sent by photoframe on startup (to 255.255.255.255 port 21900)
# Search,PF110-DEV,PF110-PC,20021,21902,0829003416,"000829003416",PhotoFrame,PhotoFrame,1,172.16.1.213
# token ,FROM,     TO,      serialnum, frame name

# sent by photoframe on shutdown (to 255.255.255.255 port 21900)
# Byebye,PF110-DEV,PF110-PC,0829003416,"000829003416"
# token ,FROM,     TO,      serialnum, frame name

# >> Post,PF110-PC,PF110-DEV,21901,CopyStart,1,0					--LAST NUMBER IS FILE TYPE: 0 == IMAGE, 1 == MP3
# << Post-Resp,PF110-DEV,PF110-PC,21902,CopyStart,0
# device transfers files from the ftp server from /vCopyFolder

# optional: << Post,PF110-DEV,PF110-PC,21902,CopyPause,filename,file size on photo frame
# if replaceall: Post,PF110-PC,PF110-DEV,21901,CopyPause,0
# if replace: Post,PF110-PC,PF110-DEV,21901,CopyPause,1
# if noreplace: Post,PF110-PC,PF110-DEV,21901,CopyPause,2
# if cancel: Post,PF110-PC,PF110-DEV,21901,CopyPause,3

# << Post,PF110-DEV,PF110-PC,21902,ProgressStatus,100,130990080,128040960
# >> Post,PF110-DEV,PF110-PC,21902,CopyStop,0
# << Post-Resp,PF110-PC,PF110-DEV,21901,CopyStop,0

# >> Post,PF110-PC,PF110-DEV,21901,RssFileStart,1,2
# << Post-Resp,PF110-DEV,PF110-PC,21902,RssFileStart,0
# << Post,PF110-DEV,PF110-PC,21902,ProgressStatus,100,130990080,128040960
# >> Post,PF110-DEV,PF110-PC,21902,RssFileStop,0
# << Post-Resp,PF110-PC,PF110-DEV,21901,RssFileStop,0,3
# device transfers a PF110.RSS from the ftp server from /vRssFolder/PF110.RSS


# Undecoded strings: Updata, Write, Write-Resp, WifiStatus, PushStart, PushStop, BtCaceroRss, KeepAlive

# Decoded strings: Search, Byebye, Read, Read-Resp, Register, Register-Resp, Post, Post-Resp, 
# SystemStatus, RssFileStart, RssFileStop, ProgressStatus, StorageStatus, RegisterStatus, CopyStart, CopyStop, CopyPause


# Create an eframe instance
locator = EFrameProtocol.EFrameLocator("172.16.1.17")
tmp = locator.FindEFrames(20.0)
eframe = EFrameProtocol.EFrame("172.16.1.17", tmp[0], tmp[1], tmp[2])

print "Storage Status:\t" + str(eframe.ReadStorageStatus())
print "System Status:\t" + str(eframe.ReadSystemStatus())
locator.SendByeBye()
