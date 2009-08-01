# -*- coding: utf-8 -*-

import struct

class StringPoolResourceChunk:

    def __init__(self, rawChunk):
        (stringCount, styleCount, flags, stringsStart, stylesStart) = struct.unpack("<IIIII", rawChunk.Header)
        stringsStart -= 28
        stylesStart -= 28

        dataIdx = 0
        self._strings = ()
        while stringCount > 0:
            stringIdx = stringsStart + struct.unpack("<I", rawChunk.Data[dataIdx:dataIdx+4])[0]

            (stringLen, ) = struct.unpack("<H", rawChunk.Data[stringIdx:stringIdx+2])
            stringIdx += 2

            if stringLen & 0x8000:
                stringLen = ((stringLen & 0x7fff) << 16) | struct.unpack("<H", rawChunk.Data[stringIdx:stringIdx+2])
                stringIdx += 2

            self._strings += (unicode(rawChunk.Data[stringIdx:stringIdx + (stringLen*2)], 'utf16').replace('\0', ''), )

            dataIdx += 4
            stringCount -= 1

        # FIXME: handle styles

    def getString(self, idx): 
        if idx == -1:
            return None
        if idx >= len(self._strings):
            return None
        return self._strings[idx]

