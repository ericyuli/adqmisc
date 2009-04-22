#!/usr/bin/python 
# -*- coding: utf-8 -*-

import EFrameProtocol
from EFrameRSSFeedList import *
import sys

if len(sys.argv) != 3:
    print >>sys.stderr, "Syntax: encoderss <source file> <destination file>"
    sys.exit(1)
srcfile = sys.argv[1]
dstfile = sys.argv[2]

rssenc = EFrameRSSFeedList()
src = open(srcfile)

while True:
    name = src.readline()
    rss_url = src.readline()
    orig_url = src.readline()
    if len(name) == 0 or len(rss_url) == 0 or len(orig_url) == 0:
	break

    rssenc.AddFeed(name.strip(), rss_url.strip(), orig_url.strip())

dst = open(dstfile, "w")
dst.write(rssenc.Encode())
dst.close()
