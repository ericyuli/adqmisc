#!/usr/bin/python 
# -*- coding: utf-8 -*-

import EFrameProtocol
from EFrameRSSFeedList import *
import sys

if len(sys.argv) != 3:
    print >>sys.stderr, "Syntax: updaterss <source file> <destination file>"
    sys.exit(1)
srcfile = sys.argv[1]
dstfile = sys.argv[2]

rssenc = EFrameRSSFeedList()
src = open(srcfile)

while True:
    name = src.readline()
    orig_url = src.readline()
    feed_url = src.readline()
    if len(name) == 0:
	break

    rssenc.AddFeed(name.strip(), orig_url.strip(), feed_url.strip())

dst = open(dstfile, "w")
dst.write(rssenc.Encode())
dst.close()
