# -*- coding: utf-8 -*-

import struct
import StringIO
import NullResourceChunk
import StringPoolResourceChunk
import XmlResourceChunk
import TableResourceChunk

class ResourceChunkStream:
    
    def __init__(self, src):
        if type(src) == file:
            self.stream = src
        elif type(src) == str:
            self.stream = StringIO.StringIO(src)

    def tell(self):
        return self.stream.tell()

    def readChunks(self):
        
        while True:
            chunkHeaderData = self.stream.read(8)
            if len(chunkHeaderData) != 8:
                return

            rawChunk = ResourceChunk(chunkHeaderData, self.stream)
            if rawChunk.TypeCode == ResourceChunk.RES_NULL_TYPE:
                yield NullResourceChunk.NullResourceChunk(rawChunk)
            elif rawChunk.TypeCode == ResourceChunk.RES_STRING_POOL_TYPE:
                self.stringPool = StringPoolResourceChunk.StringPoolResourceChunk(rawChunk)
                yield self.stringPool
            elif rawChunk.TypeCode == ResourceChunk.RES_TABLE_TYPE:
                yield TableResourceChunk.TableResourceChunk(rawChunk)
            elif rawChunk.TypeCode == ResourceChunk.RES_XML_TYPE:
                yield XmlResourceChunk.XmlResourceChunk(rawChunk)

            elif rawChunk.TypeCode == ResourceChunk.RES_XML_RESOURCE_MAP_TYPE:
                yield XmlResourceChunk.XmlResourceMapChunk(rawChunk, self.stringPool)

            elif rawChunk.TypeCode == ResourceChunk.RES_XML_START_NAMESPACE_TYPE:
                yield XmlResourceChunk.XmlNodeStartNamespaceChunk(rawChunk, self.stringPool)
            elif rawChunk.TypeCode == ResourceChunk.RES_XML_END_NAMESPACE_TYPE:
                yield XmlResourceChunk.XmlNodeEndNamespaceChunk(rawChunk, self.stringPool)
            elif rawChunk.TypeCode == ResourceChunk.RES_XML_START_ELEMENT_TYPE:
                yield XmlResourceChunk.XmlNodeStartElementChunk(rawChunk, self.stringPool)
            elif rawChunk.TypeCode == ResourceChunk.RES_XML_END_ELEMENT_TYPE:
                yield XmlResourceChunk.XmlNodeEndElementChunk(rawChunk, self.stringPool)
            elif rawChunk.TypeCode == ResourceChunk.RES_XML_CDATA_TYPE:
                yield XmlResourceChunk.XmlNodeCDATAChunk(rawChunk, self.stringPool)

            elif rawChunk.TypeCode == ResourceChunk.RES_TABLE_PACKAGE_TYPE:
                yield TableResourceChunk.TablePackageChunk(rawChunk)
            elif rawChunk.TypeCode == ResourceChunk.RES_TABLE_TYPE_TYPE:
                yield TableResourceChunk.TableTypeChunk(rawChunk)
            elif rawChunk.TypeCode == ResourceChunk.RES_TABLE_TYPE_SPEC_TYPE:
                yield TableResourceChunk.TableTypeSpecChunk(rawChunk)


            else:
                raise Exception("Unknown chunk code 0x%04x" % rawChunk.TypeCode)




def ParseValue(data, stringPool):
    (typedValueSize, zero, dataType, data) = struct.unpack("<HBBI", data)

    if dataType == ResourceChunk.VALUE_TYPE_NULL:
        return None
    elif dataType == ResourceChunk.VALUE_TYPE_REFERENCE:
        return "REFERENCE:0x%x" % data
    elif dataType == ResourceChunk.VALUE_TYPE_ATTRIBUTE:
        return "ATTRIBUTE:0x%x" % data
    elif dataType == ResourceChunk.VALUE_TYPE_STRING:
        return stringPool.getString(data)
    elif dataType == ResourceChunk.VALUE_TYPE_FLOAT:
        return "FLOAT: %f" % struct.unpack("<f", data)
    elif dataType == ResourceChunk.VALUE_TYPE_DIMENSION:
        return "DIMENSION: %x" % data                           # FIXME: format properly
    elif dataType == ResourceChunk.VALUE_TYPE_FRACTION:
        return "FRACTION: %x" % data                            # FIXME: format properly

    elif dataType == ResourceChunk.VALUE_TYPE_INT_DEC:
        return "%i" % data
    elif dataType == ResourceChunk.VALUE_TYPE_INT_HEX:
        return "0x%x" % data
    elif dataType == ResourceChunk.VALUE_TYPE_INT_BOOLEAN:
        return "false" if (data == 0) else "true"

    elif dataType == ResourceChunk.VALUE_TYPE_INT_COLOR_ARGB8:
        return "#%08x" % data
    elif dataType == ResourceChunk.VALUE_TYPE_INT_COLOR_RGB8:
        return "#%06x" % data
    elif dataType == ResourceChunk.VALUE_TYPE_INT_COLOR_ARGB4:
        return "#%04x" % data
    elif dataType == ResourceChunk.VALUE_TYPE_INT_COLOR_RGB4:
        return "#%03x" % data

    else:
        raise Exception("Unsupported data type 0x%x" % dataType)




class ResourceChunk:
    
    RES_NULL_TYPE               = 0x0000
    RES_STRING_POOL_TYPE        = 0x0001
    RES_TABLE_TYPE              = 0x0002
    RES_XML_TYPE                = 0x0003

    RES_XML_START_NAMESPACE_TYPE= 0x0100
    RES_XML_END_NAMESPACE_TYPE  = 0x0101
    RES_XML_START_ELEMENT_TYPE  = 0x0102
    RES_XML_END_ELEMENT_TYPE    = 0x0103
    RES_XML_CDATA_TYPE          = 0x0104
    RES_XML_RESOURCE_MAP_TYPE   = 0x0180

    RES_TABLE_PACKAGE_TYPE      = 0x0200
    RES_TABLE_TYPE_TYPE         = 0x0201
    RES_TABLE_TYPE_SPEC_TYPE    = 0x0202


    VALUE_TYPE_NULL             = 0x00
    VALUE_TYPE_REFERENCE        = 0x01
    VALUE_TYPE_ATTRIBUTE        = 0x02
    VALUE_TYPE_STRING           = 0x03
    VALUE_TYPE_FLOAT            = 0x04
    VALUE_TYPE_DIMENSION        = 0x05
    VALUE_TYPE_FRACTION         = 0x06

    VALUE_TYPE_INT_DEC          = 0x10
    VALUE_TYPE_INT_HEX          = 0x11
    VALUE_TYPE_INT_BOOLEAN      = 0x12

    VALUE_TYPE_INT_COLOR_ARGB8  = 0x1c
    VALUE_TYPE_INT_COLOR_RGB8   = 0x1d
    VALUE_TYPE_INT_COLOR_ARGB4  = 0x1e
    VALUE_TYPE_INT_COLOR_RGB4   = 0x1f


    def __init__(self, chunkHeader, stream):
        
        (self.TypeCode, headerSize, dataSize) = struct.unpack("<HHI", chunkHeader)
        self.Header = stream.read(headerSize - 8)
        self.Data = stream.read(dataSize - headerSize)

