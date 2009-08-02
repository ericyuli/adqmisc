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
        typeStringsPos -= len(rawChunk.Header) + 8
        keyStringsPos -= len(rawChunk.Header) + 8

        typeStrings = None
        keyStrings = None
        subStream = ResourceChunk.ResourceChunkStream(rawChunk.Data)
        subStreamPos = subStream.tell()
        typeSpecIdx = 0
        self.chunks = ()
        for subChunk in subStream.readChunks():
            self.chunks += (subChunk, )

            if subStreamPos == typeStringsPos:
                typeStrings = subChunk
            elif subStreamPos == keyStringsPos:
                keyStrings = subChunk

            if isinstance(subChunk, TableTypeSpecChunk):
                subChunk.setName(typeStrings.getString(typeSpecIdx))
                typeSpecIdx += 1
            elif isinstance(subChunk, TableTypeChunk):
                subChunk.resolveStrings(keyStrings)

            subStreamPos = subStream.tell()

    
class TableTypeSpecChunk:

    SPEC_CONFIG_MCC             = 0x00000001
    SPEC_CONFIG_MNC             = 0x00000002
    SPEC_CONFIG_LOCALE          = 0x00000004
    SPEC_CONFIG_TOUCHSCREEN     = 0x00000008
    SPEC_CONFIG_KEYBOARD        = 0x00000010
    SPEC_CONFIG_KEYBOARD_HIDDEN = 0x00000020
    SPEC_CONFIG_NAVIGATION      = 0x00000040
    SPEC_CONFIG_ORIENTATION     = 0x00000080
    SPEC_CONFIG_DENSITY         = 0x00000100
    SPEC_CONFIG_SCREEN_SIZE     = 0x00000200
    SPEC_CONFIG_VERSION         = 0x00000400
    SPEC_PUBLIC                 = 0x40000000

    def __init__(self, rawChunk):

        (self.typeId, res0, res1, entryCount) = struct.unpack("<BBHI", rawChunk.Header)

        self.configChangeFlags = ()
        for idx in xrange(0, entryCount * 4, 4):
            self.configChangeFlags += struct.unpack("<I", rawChunk.Data[idx:idx+4])

    def setName(self, name):
        
        self.name = name


class TableTypeChunk:

    FLAG_COMPLEX_ENTRY          = 0x00000001
    FLAG_PUBLIC_ENTRY           = 0x00000002

    def __init__(self, rawChunk):

        (self.typeId, res0, res1, entryCount, entriesStart) = struct.unpack("<BBHII", rawChunk.Header[0:12])
        entriesStart -= len(rawChunk.Header) + 8

        (size, self.mcc, self.mnc, self.language, self.country, self.orientation, self.touchscreen, 
         self.density, self.keyboard, self.navigation, self.inputFlags, pad0, self.screenWidth, 
         self.screenHeight, self.sdkVersion, self.minorVersion) = struct.unpack("<IHH2s2sBBHBBBBHHHH", rawChunk.Header[12:])
        self.language = self.language.replace('\0', '')
        self.country = self.country.replace('\0', '')

        self.entries = ()
        for idx in xrange(0, entryCount * 4, 4):
            curOffset = struct.unpack("<I", rawChunk.Data[idx:idx + 4])[0]
            if curOffset == 0xffffffff:
                self.entries += (None, )
                continue
            curOffset += entriesStart

            (size, flags, keyStringIdx) = struct.unpack("<HHI", rawChunk.Data[curOffset:curOffset + 8])
            curOffset += 8

            if (flags & TableTypeChunk.FLAG_COMPLEX_ENTRY) == 0: # a "simple" entry
                simpleValue = ResourceChunk.ParseValue(rawChunk.Data[curOffset:curOffset + 8])
            else: # a "complex" entry
                (parentRef, mapCount) = struct.unpack("<II", rawChunk.Data[curOffset:curOffset + 8])
                curOffset += 8
                # FIXME

        # FIXME: the restable_entry data

    def resolveStrings(self, keyStrings):
        pass
        # FIXME: the key strings
