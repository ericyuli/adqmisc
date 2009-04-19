#!/usr/bin/python 
# -*- coding: utf-8 -*-

import EFrameProtocol
from EFrameRSSFeedList import *
import sys

# Argument handling
if len(sys.argv) < 3 or len(sys.argv) > 4:
    print >>sys.stderr, "Syntax: copyphotos <local ip> <eframe ip> [<eframe port>]"
    sys.exit(1)
local_ip = sys.argv[1]
eframe_ip = sys.argv[2]
if len(sys.argv) == 3:
    eframe_port = 21902
else:
    eframe_port = int(sys.argv[3])


# Create an eframe instance
eframe = EFrameProtocol.EFrame(local_ip, (eframe_ip, eframe_port))

def progress(arg):
    print arg

def copy_action(arg):
    print arg

# do the transfer
eframe.TransferPhotos(progress, copy_action)

# Shutdown
locator = EFrameProtocol.EFrameLocator(local_ip)
locator.SendByeBye()
