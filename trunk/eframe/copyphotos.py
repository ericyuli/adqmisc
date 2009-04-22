#!/usr/bin/python 
# -*- coding: utf-8 -*-

import EFrameProtocol
from EFrameRSSFeedList import *
import sys

# Argument handling
if len(sys.argv) < 1 or len(sys.argv) > 2:
    print >>sys.stderr, "Syntax: copyphotos <local ip>"
    sys.exit(1)
local_ip = sys.argv[1]


# Create an eframe instance
eframe = EFrameProtocol.EFrame(local_ip)

def progress(arg):
    print arg

def copy_action(arg):
    print arg

# do the transfer
eframe.TransferPhotos(progress, copy_action)

# Shutdown
locator = EFrameProtocol.EFrameLocator(local_ip)
locator.SendByeBye()
