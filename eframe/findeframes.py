#!/usr/bin/python 
# -*- coding: utf-8 -*-

import EFrameProtocol
import sys

# Create an eframe instance
locator = EFrameProtocol.EFrameLocator("172.16.1.17")
tmp = locator.FindEFrames(20.0)
eframe = EFrameProtocol.EFrame("172.16.1.17", tmp[0])

print "Found eframe %s(%s) at %s" % (eframe.frame_name, eframe.serial_number, eframe.frame_address)
print "Storage Status:\t" + str(eframe.ReadStorageStatus())
print "System Status:\t" + str(eframe.ReadSystemStatus())
locator.SendByeBye()
