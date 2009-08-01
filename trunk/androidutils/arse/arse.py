#!/usr/bin/python
# -*- coding: utf-8 -*-

import XmlResourceChunk
import ResourceChunk
import sys
import xml.dom.ext

test = ResourceChunk.ResourceChunkStream(open(sys.argv[1]))
print >>sys.stderr, sys.argv[1]
for chunk in test.ReadChunks():
    
    if isinstance(chunk, XmlResourceChunk.XmlResourceChunk):
        print xml.dom.ext.PrettyPrint(chunk.XmlDoc)
