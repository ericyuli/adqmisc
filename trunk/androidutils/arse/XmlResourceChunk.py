# -*- coding: utf-8 -*-

import xml.dom.minidom
import struct
import ResourceChunk

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
            if isinstance(chunk, XmlNodeStartNamespaceChunk):
                namespaces.insert(0, (chunk.prefix, chunk.uri))

            elif isinstance(chunk, XmlNodeEndNamespaceChunk):
                namespaces.remove((chunk.prefix, chunk.uri))

            elif isinstance(chunk, XmlNodeStartElementChunk):

                if chunk.namespace:
                    tmpNode = self.XmlDoc.createElementNS(chunk.namespace, self.addNsPrefix(chunk.name, chunk.namespace, namespaces))
                else:
                    tmpNode = self.XmlDoc.createElement(chunk.name)
                curNode.appendChild(tmpNode)
                curNode= tmpNode

                for attribute in chunk.attributes:
                    if attribute[0]:
                        curNode.setAttributeNS(attribute[0], self.addNsPrefix(attribute[1], attribute[0], namespaces), attribute[2])
                    else:
                        curNode.setAttribute(attribute[1], attribute[2])
                
            elif isinstance(chunk, XmlNodeEndElementChunk):
                curNode = curNode.parentNode 

            elif isinstance(chunk, XmlNodeCDATAChunk):
                curNode.appendChild(self.XmlDoc.createTextNode(chunk.value))

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

    def __init__(self, rawChunk, stringPool):
        (self.lineNumber, commentIdx) = struct.unpack("<II", rawChunk.Header[0:8])
        self.comment = stringPool.getString(commentIdx)

class XmlNodeStartNamespaceChunk(XmlNodeChunk):

    def __init__(self, rawChunk, stringPool):
        XmlNodeChunk.__init__(self, rawChunk, stringPool)

        (prefixIdx, uriIdx) = struct.unpack("<II", rawChunk.Data[0:8])
        self.prefix = stringPool.getString(prefixIdx)
        self.uri = stringPool.getString(uriIdx)

class XmlNodeEndNamespaceChunk(XmlNodeChunk):

    def __init__(self, rawChunk, stringPool):
        XmlNodeChunk.__init__(self, rawChunk, stringPool)

        (prefixIdx, uriIdx) = struct.unpack("<II", rawChunk.Data[0:8])
        self.prefix = stringPool.getString(prefixIdx)
        self.uri = stringPool.getString(uriIdx)

class XmlNodeStartElementChunk(XmlNodeChunk):

    def __init__(self, rawChunk, stringPool):
        XmlNodeChunk.__init__(self, rawChunk, stringPool)

        (namespaceIdx, nameIdx) = struct.unpack("<II", rawChunk.Data[0:8])
        self.namespace = stringPool.getString(namespaceIdx)
        self.name = stringPool.getString(nameIdx)

        (attributeIdx, attributeSize, attributeCount, idIndex, classIndex, styleIndex) = struct.unpack("<HHHHHH", rawChunk.Data[8:20])

        self.attributes = ()
        while attributeCount > 0:
            (namespaceIdx, nameIdx, rawIdx) = struct.unpack("<III", rawChunk.Data[attributeIdx:attributeIdx + 12])

            value = stringPool.getString(rawIdx)
            if value == None:
                value = ResourceChunk.ParseValue(rawChunk.Data[attributeIdx+12:attributeIdx + 20], stringPool)

            self.attributes += ( (stringPool.getString(namespaceIdx), stringPool.getString(nameIdx), value), )

            attributeIdx += 20
            attributeCount -= 1

class XmlNodeEndElementChunk(XmlNodeChunk):

    def __init__(self, rawChunk, stringPool):
        XmlNodeChunk.__init__(self, rawChunk, stringPool)

        (namespaceIdx, nameIdx) = struct.unpack("<II", rawChunk.Data[0:8])
        self.namespace = stringPool.getString(namespaceIdx)
        self.name = stringPool.getString(nameIdx)

class XmlNodeCDATAChunk(XmlNodeChunk):

    def __init__(self, rawChunk, stringPool):
        XmlNodeChunk.__init__(self, rawChunk, stringPool)

        (rawDataIdx, ) = struct.unpack("<I", rawChunk.Data[0:4])
        self.value = stringPool.getString(rawDataIdx)

class XmlResourceMapChunk:

    def __init__(self, rawChunk, stringPool):
        self.resourceIdStringMap = {}
        for idx in xrange(0, len(rawChunk.Data), 4):
            self.resourceIdStringMap[struct.unpack("<I", rawChunk.Data[idx:idx+4])] = stringPool.getString(idx / 4)

