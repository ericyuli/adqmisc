# -*- coding: utf-8 -*-

import struct
import ResourceChunk
import StringPoolResourceChunk
import xml.dom.minidom

class TableResourceChunk:

    def __init__(self, rawChunk):

        (packageCount, ) = struct.unpack("<I", rawChunk.Header)

        self.chunks = ()
        for chunk in ResourceChunk.ResourceChunkStream(rawChunk.Data).readChunks():
            self.chunks += (chunk, )
            
            if isinstance(chunk, StringPoolResourceChunk.StringPoolResourceChunk):
                globalStringPool = chunk
            elif isinstance(chunk, TablePackageChunk):
                chunk.resolveStrings(globalStringPool)

        self.XmlDoc = xml.dom.minidom.Document()
        rootNode = self.XmlDoc.createElement("packages")
        self.XmlDoc.appendChild(rootNode)
        for chunk in self.chunks:
            if isinstance(chunk, TablePackageChunk):
                curPackageNode = self.XmlDoc.createElement("package")
                curPackageNode.setAttribute("id", "0x%x" % chunk.basePackageId)
                curPackageNode.setAttribute("name", chunk.packageName)
                rootNode.appendChild(curPackageNode)

                for subChunk in chunk.chunks:
                    
                    if isinstance(subChunk, TableTypeSpecChunk):
                        curRestypeNode = self.XmlDoc.createElement("resourcetype")
                        curRestypeNode.setAttribute("id", "0x%x" % subChunk.typeId)
                        curRestypeNode.setAttribute("name", subChunk.name)
                        curPackageNode.appendChild(curRestypeNode)

                        isAttribute = False
                        if subChunk.name == 'attr': isAttribute = True
                                                
                        # The fastItemConfigTable is a table for fast lookup for each resource item. They indicate which SPEC_CONFIG_XXX flags appear in
                        # more than one resourceconfig (i.e. have > 1 choice), and also which are public. We don't care since we're just 
                        # dumping everything anyway.

                    elif isinstance(subChunk, TableTypeChunk):
                        curResourceNode = self.XmlDoc.createElement("configuration")
                        if subChunk.mcc: curResourceNode.setAttribute("mcc", "%i" % subChunk.mcc)
                        if subChunk.mnc: curResourceNode.setAttribute("mnc", "%i" % subChunk.mnc)
                        if subChunk.language: curResourceNode.setAttribute("language", subChunk.language)
                        if subChunk.country: curResourceNode.setAttribute("country", subChunk.country)
                        if subChunk.orientation: curResourceNode.setAttribute("orientation", "%i" % subChunk.orientation)
                        if subChunk.touchscreen: curResourceNode.setAttribute("touchscreen", "%i" % subChunk.touchscreen)
                        if subChunk.density: curResourceNode.setAttribute("density", "%i" % subChunk.density)
                        if subChunk.keyboard: curResourceNode.setAttribute("keyboard", "%i" % subChunk.keyboard)
                        if subChunk.navigation: curResourceNode.setAttribute("navigation", "%i" % subChunk.navigation)
                        if subChunk.inputFlags: curResourceNode.setAttribute("inputflags", "0x%x" % subChunk.inputFlags)
                        if subChunk.screenWidth: curResourceNode.setAttribute("screenwidth", "%i" % subChunk.screenWidth)
                        if subChunk.screenHeight: curResourceNode.setAttribute("screenheight", "%i" % subChunk.screenHeight)
                        if subChunk.sdkVersion: curResourceNode.setAttribute("sdkversion", "%i" % subChunk.sdkVersion)
                        if subChunk.minorVersion: curResourceNode.setAttribute("minorversion", "%i" % subChunk.minorVersion)
                        curRestypeNode.appendChild(curResourceNode)

                        entryId = 0
                        for entry in subChunk.entries:
                            if entry == None:
                                entryId += 1
                                continue
                            curEntryNode = self.XmlDoc.createElement("item")
                            curEntryNode.setAttribute("id", "0x%x" % entryId)
                            curEntryNode.setAttribute("name", entry[0])
                            if entry[1] & TableTypeChunk.FLAG_PUBLIC_ENTRY: curEntryNode.setAttribute("ispublic", "true")
                            if entry[3]: curEntryNode.setAttribute("parentref", "@0x%08x" % entry[3])
                            curResourceNode.appendChild(curEntryNode)

                            if type(entry[2]) != tuple:
                                curEntryNode.setAttribute("value", entry[2])
                            else:
                                for value in entry[2]:
                                    if isAttribute:
                                        valueNode = self.XmlDoc.createElement("value")
                                        
                                        if value[0] ==   0x01000000:
                                            valueNode.setAttribute("allowedtypes", "0x%x" % int(value[1]))
                                        elif value[0] == 0x01000001:
                                            valueNode.setAttribute("minvalue", "%i" % int(value[1]))
                                        elif value[0] == 0x01000002:
                                            valueNode.setAttribute("maxvalue", "%i" % int(value[1]))
                                        elif value[0] == 0x01000003:
                                            valueNode.setAttribute("localisation", "suggested" if int(value[1]) != 0 else "notrequired")
                                        elif value[0] == 0x01000004:
                                            valueNode.setAttribute("quantity", "%i" % int(value[1]))
                                        elif value[0] == 0x01000005:
                                            valueNode.setAttribute("quantity", "zero")
                                        elif value[0] == 0x01000006:
                                            valueNode.setAttribute("quantity", "one")
                                        elif value[0] == 0x01000007:
                                            valueNode.setAttribute("quantity", "two")
                                        elif value[0] == 0x01000008:
                                            valueNode.setAttribute("quantity", "few")
                                        elif value[0] == 0x01000009:
                                            valueNode.setAttribute("quantity", "many")
                                        else:
                                            valueNode.setAttribute("name", "@0x%08x" % value[0])
                                            valueNode.setAttribute("value", value[1])
                                        curEntryNode.appendChild(valueNode)

                                    else:
                                        valueNode = self.XmlDoc.createElement("value")
                                        valueNode.setAttribute("name", "@0x%08x" % value[0])
                                        valueNode.setAttribute("value", value[1])
                                        curEntryNode.appendChild(valueNode)

                            entryId += 1


class TablePackageChunk:

    def __init__(self, rawChunk):

        (self.basePackageId, rawPackageName, typeStringsPos, lastPublicType, keyStringsPos, lastPublicKey) = struct.unpack("<I256sIIII", rawChunk.Header)
        self.packageName = unicode(rawPackageName, "utf16").replace('\0', '')
        typeStringsPos -= len(rawChunk.Header) + 8
        keyStringsPos -= len(rawChunk.Header) + 8

        # Process all the chunks inside this package
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
                self.keyStrings = subChunk
            subStreamPos = subStream.tell()

            if isinstance(subChunk, TableTypeSpecChunk):
                subChunk.setName(typeStrings.getString(typeSpecIdx))
                typeSpecIdx += 1


    def resolveStrings(self, globalStringPool):
        
        for chunk in self.chunks:
            if isinstance(chunk, TableTypeChunk):
                chunk.resolveStrings(globalStringPool, self.keyStrings)

    
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

        self.fastItemConfigTable = ()
        for idx in xrange(0, entryCount * 4, 4):
            self.fastItemConfigTable += struct.unpack("<I", rawChunk.Data[idx:idx+4])

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
                parentRef = None
                value = ResourceChunk.ParseValue(rawChunk.Data[curOffset:curOffset + 8])
            else: # a "complex" entry
                (parentRef, mapCount) = struct.unpack("<II", rawChunk.Data[curOffset:curOffset + 8])
                curOffset += 8
        
                if parentRef == 0: parentRef = None

                value = ()
                for mapIdx in xrange(0, mapCount):
                    (nameRef, ) = struct.unpack("<I", rawChunk.Data[curOffset:curOffset + 4])
                    mapValue = ResourceChunk.ParseValue(rawChunk.Data[curOffset + 4:curOffset + 12])
                    value += ([nameRef, mapValue], )
                    curOffset += 12

            self.entries += ([keyStringIdx, flags, value, parentRef], )

    def resolveStrings(self, globalStringPool, keyStringPool):

        for entry in self.entries:
            if entry == None:
                continue
            
            entry[0] = keyStringPool.getString(entry[0])
            if (entry[1] & TableTypeChunk.FLAG_COMPLEX_ENTRY) == 0: # a "simple" entry
                if type(entry[2]) == int:
                    entry[2] = globalStringPool.getString(entry[2])

            else:
                for mapValue in entry[2]:
                    if type(mapValue[1]) == int:
                        mapValue[1] = globalStringPool.getString(mapValue[1])

