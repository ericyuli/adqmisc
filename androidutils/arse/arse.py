#!/usr/bin/python
# -*- coding: utf-8 -*-

import ResourceChunk
import sys
import xml.dom.ext

test = ResourceChunk.ResourceChunkStream(open(sys.argv[1]))
print >>sys.stderr, sys.argv[1]
for chunk in test.ReadChunks():

    print xml.dom.ext.PrettyPrint(chunk.XmlDoc)
