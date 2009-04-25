#!/usr/bin/python 
# -*- coding: utf-8 -*-

import EFrameProtocol
from EFrameRSSFeedList import *
import sys
import EFrameFTPServer

# Check args
if len(sys.argv) != 2:
    print >>sys.stderr, "Syntax: copyrss <ftp transfer directory>"
    sys.exit(1)
ftp_dir = sys.argv[1]

# Create an eframe instance
eframe = EFrameProtocol.EFrame()

# Create and start an ftp server instance
eframeftpd = EFrameFTPServer.EFrameFTPServer(eframe, ftp_dir)

# Functions to handle transfer events
def progress(arg):
    print "EFrame reports progress: " + str(arg)

def copy_action(arg):
    return "REPLACEALL"

# Do the transfer
eframe.TransferRss(progress, copy_action)

# Shutdown
eframeftpd.Stop()
eframe.SendByeBye()
