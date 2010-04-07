# -*- coding: utf-8 -*-
import struct
import Guid
import zlib
import sys
import EfiDecompressor

class EfiSection:
  
  EFI_SECTION_COMPRESSION		= 0x01
  EFI_SECTION_GUID_DEFINED		= 0x02

  EFI_SECTION_PE32			= 0x10
  EFI_SECTION_PIC			= 0x11
  EFI_SECTION_TE			= 0x12
  EFI_SECTION_DXE_DEPEX			= 0x13
  EFI_SECTION_VERSION			= 0x14
  EFI_SECTION_USER_INTERFACE		= 0x15
  EFI_SECTION_COMPATABILITY16		= 0x16
  EFI_SECTION_FIRMWARE_VOLUME_IMAGE	= 0x17
  EFI_SECTION_FREEFORM_SUBTYPE_GUID	= 0x18
  EFI_SECTION_RAW			= 0x19
  EFI_SECTION_PEI_DEPEX			= 0x1b

  def __init__(self, filetype):
    self.Type = filetype
    self.Data = None
    self.Guid = None
    self.String = None
    self.BuildNumber = None
    self.Attributes = None
    self.HeaderData = None
    self.SubSections = None

  def __str__(self):
    result = "EFI_SECTION:\n"
    result += "\tType: 0x%02x (%s)\n" % (self.Type, self.strsectiontype())
    if self.Guid:
      result += "\tGuid: %s\n" % self.Guid
    if self.String:
      result += "\tString: %s\n" % self.String
    if self.BuildNumber:
      result += "\tBuildNumber: %s\n" % self.BuildNumber
    if self.Attributes:
      result += "\tAttributes: %s\n" % self.Attributes
    if self.HeaderData:
      result += "\tHeader Data Length: 0x%08x\n" % len(self.HeaderData)
    if self.Data:
      result += "\tBinary Data Length: 0x%08x\n" % len(self.Data)
    return result

  def strsectiontype(self):
    if self.Type == self.EFI_SECTION_COMPRESSION:
      return "COMPRESSION"
    elif self.Type == self.EFI_SECTION_GUID_DEFINED:
      return "GUID_DEFINED"
    elif self.Type == self.EFI_SECTION_PE32:
      return "PE32"
    elif self.Type == self.EFI_SECTION_PIC:
      return "PIC"
    elif self.Type == self.EFI_SECTION_TE:
      return "TE"
    elif self.Type == self.EFI_SECTION_DXE_DEPEX:
      return "DEPEX"
    elif self.Type == self.EFI_SECTION_VERSION:
      return "VERSION"
    elif self.Type == self.EFI_SECTION_USER_INTERFACE:
      return "USER_INTERFACE"
    elif self.Type == self.EFI_SECTION_COMPATABILITY16:
      return "COMPATABILITY16"
    elif self.Type == self.EFI_SECTION_FIRMWARE_VOLUME_IMAGE:
      return "FIRMWARE_VOLUME_IMAGE"
    elif self.Type == self.EFI_SECTION_FREEFORM_SUBTYPE_GUID:
      return "FREEFORM_SUBTYPE_GUID"
    elif self.Type == self.EFI_SECTION_RAW:
      return "RAW"
    elif self.Type == self.EFI_SECTION_PEI_DEPEX:
      return "PEI_DEPEX"
    return "UNKNOWN"

def find(rawdata):
  sections = []

  base = 0
  while base < len(rawdata) :

    # align to 4 bytes
    if base % 4:
      base += 4 - (base % 4)
    if base >= len(rawdata):
      break

    (length, efitype) = struct.unpack("<3sB", rawdata[base:base+4])
    length = struct.unpack("<I", length + '\0')[0]
    base += 4
    length -= 4

    section = EfiSection(efitype)
    if efitype == EfiSection.EFI_SECTION_COMPRESSION:
      (uncomp_length, comp_type) = struct.unpack("<IB", rawdata[base:base+4+1])
      data = rawdata[base+5:base+length]
      if comp_type == 0:
	pass # uncompressed
      elif comp_type == 1:
	data = EfiDecompressor.Decompress(data)
      else:
	print "Warning: Unknown compression type %i" % comp_type

      section.SubSections = find(data)

    elif efitype == EfiSection.EFI_SECTION_GUID_DEFINED:
      (guid, data_offset, attributes) = struct.unpack("<16sHH", rawdata[base:base+16+2+2])
      data_offset -= 24

      section.Guid = Guid.strguid(guid)
      section.HeaderData = rawdata[base+20:base+20+data_offset]
      section.Attributes = attributes
      section.Data = rawdata[base+20+data_offset:base+length]

    elif efitype == EfiSection.EFI_SECTION_VERSION:
      (build_number, ) = struct.unpack("<H", rawdata[base:base+2])
      section.BuildNumber = build_number
      section.String = unicode(rawdata[base+2:base+length-2], "utf-16")

    elif efitype == EfiSection.EFI_SECTION_USER_INTERFACE:
      section.String = unicode(rawdata[base:base+length-2], "utf-16")

    elif efitype == EfiSection.EFI_SECTION_FREEFORM_SUBTYPE_GUID:
      section.Guid = Guid.strguid(rawdata[base:base+16])
      section.Data = rawdata[base+16:]

    else:
      if length:
	section.Data = rawdata[base:base+length]

    sections.append(section)

    base += length

  return sections
