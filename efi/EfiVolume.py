# -*- coding: utf-8 -*-
import struct
import Guid


class EfiVolume:

  def __init__(self, base, headerLength, dataLength, signature, attributes):
    self.Base = base
    self.HeaderLength = headerLength
    self.DataLength = dataLength
    self.Signature = signature
    self.Attributes = attributes

  def __str__(self):
    result = "EFI_FIRMWARE_VOLUME:\n"
    result += "\tBase Offset: 0x%08x\n" % self.Base
    result += "\tHeader Length: 0x%x\n" % self.HeaderLength
    result += "\tData Length: 0x%08x\n" % self.DataLength
    result += "\tSignature: %s\n" % self.Signature
    result += "\tAttributes: 0x%04x\n" % self.Attributes
    return result


def find(instream, streamlength):
  volumes = []

  base = instream.tell()
  while(base < streamlength):
    
    # align to 8 bytes
    if instream.tell() % 8:
      tmp = 8 - (instream.tell() % 8)
      instream.seek(tmp, 1)
      base += tmp

    (zero, guid, length, sig, attrib, headerlength, checksum, reserved, revision) = struct.unpack("<16s16sQ4sIHH3sB", instream.read(16 + 16 + 8 + 4 + 4 + 2 + 2 + 3 + 1))
    if Guid.strguid(guid) != "7a9354d9-0468-444a-81ce-0bf617d890df":
      return volumes

    volumes.append(EfiVolume(base, headerlength, length - headerlength, sig, attrib))

    # Skip the blockmap - sample I have just has one big block in it
    while True:
      (numBlocks, blockLength) = struct.unpack("<II", instream.read(8))
      if numBlocks == 0 and blockLength == 0:
	break;

    instream.seek(length - headerlength, 1)
    base += length

  return volumes
