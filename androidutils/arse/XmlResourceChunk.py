# -*- coding: utf-8 -*-

import xml.dom.minidom
import struct
import ResourceChunk
import StringPoolResourceChunk

class XmlResourceChunk:

    def __init__(self, rawChunk):

        # Load in the sub-chunks which are the xml document
        self.chunks = ()
        for subChunk in ResourceChunk.ResourceChunkStream(rawChunk.Data).readChunks():
            self.chunks += (subChunk, )

        # Now, rebuild a DOM document from the chunks loaded above
        curNode = self.XmlDoc = xml.dom.minidom.Document()
        namespaces = []
        for chunk in self.chunks:
            if isinstance(chunk, StringPoolResourceChunk.StringPoolResourceChunk):
                stringPool = chunk

            elif isinstance(chunk, XmlNodeStartNamespaceChunk):
                namespaces.insert(0, (stringPool.getString(chunk.prefixIdx), stringPool.getString(chunk.uriIdx)))

            elif isinstance(chunk, XmlNodeEndNamespaceChunk):
                namespaces.remove((stringPool.getString(chunk.prefixIdx), stringPool.getString(chunk.uriIdx)))

            elif isinstance(chunk, XmlNodeStartElementChunk):
                
                elementNamespace = stringPool.getString(chunk.namespaceIdx)
                elementName = stringPool.getString(chunk.nameIdx)

                if elementNamespace:
                    tmpNode = self.XmlDoc.createElementNS(elementNamespace, self.addNsPrefix(elementName, elementNamespace, namespaces))
                else:
                    tmpNode = self.XmlDoc.createElement(elementName)
                curNode.appendChild(tmpNode)
                curNode = tmpNode

                for attribute in chunk.attributes:
                    attrNamespace = stringPool.getString(attribute[0])
                    attrName = stringPool.getString(attribute[1])
                    attrValue = attribute[2]
                    if type(attrValue) == int:
                        attrValue = stringPool.getString(attrValue)

                    if attrNamespace:
                        curNode.setAttributeNS(attrNamespace, self.addNsPrefix(attrName, attrNamespace, namespaces), attrValue)
                    else:
                        curNode.setAttribute(attrName, attrValue)
 
            elif isinstance(chunk, XmlNodeEndElementChunk):
                curNode = curNode.parentNode 

            elif isinstance(chunk, XmlNodeCDATAChunk):
                curNode.appendChild(self.XmlDoc.createTextNode(stringPool.getString(chunk.cdataIdx)))

    def addNsPrefix(self, name, namespace, namespaces):
        prefix = None
        for ns in namespaces:
            if ns[1] == namespace:
                prefix = ns[0]
                break
        if prefix == None:
            return name
        else:
            return prefix + ':' + name



class XmlNodeChunk:

    def __init__(self, rawChunk):
        (self.lineNumber, self.commentIdx) = struct.unpack("<II", rawChunk.Header[0:8])

class XmlNodeStartNamespaceChunk(XmlNodeChunk):

    def __init__(self, rawChunk):
        XmlNodeChunk.__init__(self, rawChunk)

        (self.prefixIdx, self.uriIdx) = struct.unpack("<II", rawChunk.Data[0:8])

class XmlNodeEndNamespaceChunk(XmlNodeChunk):

    def __init__(self, rawChunk):
        XmlNodeChunk.__init__(self, rawChunk)

        (self.prefixIdx, self.uriIdx) = struct.unpack("<II", rawChunk.Data[0:8])

class XmlNodeStartElementChunk(XmlNodeChunk):

    def __init__(self, rawChunk):
        XmlNodeChunk.__init__(self, rawChunk)

        (self.namespaceIdx, self.nameIdx) = struct.unpack("<II", rawChunk.Data[0:8])

        (attributeIdx, attributeSize, attributeCount, idIndex, classIndex, styleIndex) = struct.unpack("<HHHHHH", rawChunk.Data[8:20])

        self.attributes = ()
        while attributeCount > 0:
            (attrNamespaceIdx, attrNameIdx, attrRawIdx) = struct.unpack("<III", rawChunk.Data[attributeIdx:attributeIdx + 12])

            value = ResourceChunk.ParseValue(rawChunk.Data[attributeIdx+12:attributeIdx + 20])

            self.attributes += ( (attrNamespaceIdx, attrNameIdx, value), )

            attributeIdx += 20
            attributeCount -= 1

class XmlNodeEndElementChunk(XmlNodeChunk):

    def __init__(self, rawChunk):
        XmlNodeChunk.__init__(self, rawChunk)

        (self.namespaceIdx, self.nameIdx) = struct.unpack("<II", rawChunk.Data[0:8])

class XmlNodeCDATAChunk(XmlNodeChunk):

    def __init__(self, rawChunk):
        XmlNodeChunk.__init__(self, rawChunk)

        (self.cdataIdx, ) = struct.unpack("<I", rawChunk.Data[0:4])

class XmlResourceMapChunk:

    def __init__(self, rawChunk):
        self.resourceIdStringIdxMap = {}
        for idx in xrange(0, len(rawChunk.Data), 4):
            self.resourceIdStringIdxMap[struct.unpack("<I", rawChunk.Data[idx:idx+4])] = idx / 4
