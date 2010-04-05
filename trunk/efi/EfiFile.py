# -*- coding: utf-8 -*-
import struct
import Guid


class EfiFile:
  
  EFI_FV_FILETYPE_RAW		= 0x01
  EFI_FV_FILETYPE_FREEFORM	= 0x02
  EFI_FV_FILETYPE_SECURITY_CORE	= 0x03
  EFI_FV_FILETYPE_PEI_CORE	= 0x04
  EFI_FV_FILETYPE_PXE_CORE	= 0x05
  EFI_FV_FILETYPE_PEIM		= 0x06
  EFI_FV_FILETYPE_DRIVER	= 0x07
  EFI_FV_FILETYPE_COMBINED_PEIM_DRIVER	= 0x08
  EFI_FV_FILETYPE_APPLICATION	= 0x09
  EFI_FV_FILETYPE_FIRMWARE_VOLUME_IMAGE	= 0x01b
  EFI_FV_FILETYPE_FFS_PAD	= 0xf0

  def __init__(self, base, length, guid, filetype, attributes, state):
    self.Base = base
    self.HeaderLength = 24
    self.DataLength = length
    self.Guid = guid
    self.Type = filetype
    self.Attributes = attributes
    self.State = state

  def __str__(self):
    result = "EFI_FIRMWARE_FILE:\n"
    result += "\tBase Offset: 0x%08x\n" % self.Base
    result += "\tData Length: 0x%08x\n" % self.DataLength
    result += "\tGuid: %s\n" % self.Guid
    result += "\tType: 0x%02x (%s)\n" % (self.Type, self.strfiletype())
    result += "\tAttributes: 0x%02x\n" % self.Attributes
    result += "\tState: 0x%x\n" % self.State
    return result

  def strfiletype(self):
    if self.Type == self.EFI_FV_FILETYPE_RAW:
      return "RAW"
    elif self.Type == self.EFI_FV_FILETYPE_FREEFORM:
      return "FREEFORM"
    elif self.Type == self.EFI_FV_FILETYPE_SECURITY_CORE:
      return "SECURITY_CORE"
    elif self.Type == self.EFI_FV_FILETYPE_PEI_CORE:
      return "PEI_CORE"
    elif self.Type == self.EFI_FV_FILETYPE_PXE_CORE:
      return "PXE_CORE"
    elif self.Type == self.EFI_FV_FILETYPE_PEIM:
      return "PEIM"
    elif self.Type == self.EFI_FV_FILETYPE_DRIVER:
      return "DRIVER"
    elif self.Type == self.EFI_FV_FILETYPE_COMBINED_PEIM_DRIVER:
      return "COMBINED_PEIM_DRIVER"
    elif self.Type == self.EFI_FV_FILETYPE_APPLICATION:
      return "APPLICATION"
    elif self.Type == self.EFI_FV_FILETYPE_FIRMWARE_VOLUME_IMAGE:
      return "FIRMWARE_VOLUME_IMAGE"
    elif self.Type == self.EFI_FV_FILETYPE_FFS_PAD:
      return "PAD"
    return "UNKNOWN"

def find(instream, volume):
  files = []

  instream.seek(volume.Base + volume.HeaderLength, 0)
  base = 0
  while(base < volume.DataLength):
    
    # align to 8 bytes
    if instream.tell() % 8:
      tmp = 8 - (instream.tell() % 8)
      instream.seek(tmp, 1)
      base += tmp
    if base >= volume.DataLength:
      break
    
    (guid, checksum, type, attrib, length, state) = struct.unpack("<16sHBB3sB", instream.read(16 + 2 + 1 + 1 + 3 + 1))
    length = struct.unpack("<I", length + '\0')[0]

    files.append(EfiFile(base, length - 24, Guid.strguid(guid), type, attrib, state))

    instream.seek(length - 24, 1)
    base += length

  return files
