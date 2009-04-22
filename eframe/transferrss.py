#!/usr/bin/python 
# -*- coding: utf-8 -*-

import EFrameProtocol
from EFrameRSSFeedList import *
import sys

# Create an eframe instance
eframe = EFrameProtocol.EFrame()

def progress(arg):
    print arg

def copy_action(arg):
    print arg
    return "REPLACEALL"

# do the transfer
eframe.TransferRss(progress, copy_action)

# Shutdown
eframe.SendByeBye()
