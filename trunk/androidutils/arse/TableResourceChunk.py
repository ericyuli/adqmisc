# -*- coding: utf-8 -*-

import struct
import ResourceChunk

class TableResourceChunk:

    def __init__(self, rawChunk):

        (self.packageCount, ) = struct.unpack("<I", rawChunk.Header)

        self.chunks = ()
        for subChunk in ResourceChunk.ResourceChunkStream(rawChunk.Data).readChunks():
            self.chunks += (subChunk, )


class TablePackageChunk:

    def __init__(self, rawChunk):

        (self.basePackageId, rawPackageName, typeStringsPos, lastPublicType, keyStringsPos, lastPublicKey) = struct.unpack("<I256sIIII", rawChunk.Header)
        self.packageName = unicode(rawPackageName, "utf16").replace('\0', '')
        typeStringsPos -= 284
        keyStringsPos -= 284

        typeStrings = None
        keyStrings = None
        subStream = ResourceChunk.ResourceChunkStream(rawChunk.Data)
        subStreamPos = subStream.tell()
        self.chunks = ()
        for subChunk in subStream.readChunks():
            self.chunks += (subChunk, )

            if subStreamPos == typeStringsPos:
                typeStrings = subChunk
            elif subStreamPos == keyStringsPos:
                keyStrings = subChunk

            subStreamPos = subStream.tell()


class TableTypeChunk:

    def __init__(self, rawChunk):

        (self.typeId, res0, res1, entryCount, entriesStart) = struct.unpack("<BBHII", rawChunk.Header[0:12])
        # FIXME: implement the remaining bit


class TableTypeSpecChunk:

    def __init__(self, rawChunk):

        (self.typeId, res0, res1, entryCount) = struct.unpack("<BBHI", rawChunk.Header)

        self.configChangeFlags = ()
        for idx in xrange(0, entryCount * 4, 4):
            self.configChangeFlags += struct.unpack("<I", rawChunk.Data[idx:idx+4])
