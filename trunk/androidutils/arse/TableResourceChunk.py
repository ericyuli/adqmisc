# -*- coding: utf-8 -*-

import struct
import ResourceChunk

class TableResourceChunk:

    def __init__(self, rawChunk):

        (self.packageCount, ) = struct.unpack("<I", rawChunk.Header)

        self.chunks = ()
        for subChunk in ResourceChunk.ResourceChunkStream(rawChunk.Data).ReadChunks():
            self.chunks += (subChunk, )
